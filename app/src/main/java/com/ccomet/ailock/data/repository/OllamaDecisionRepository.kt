package com.ccomet.ailock.data.repository

import android.content.Context
import android.provider.Settings
import com.ccomet.ailock.BuildConfig
import com.ccomet.ailock.data.model.ActiveUseSession
import com.ccomet.ailock.data.model.JudgePostRequest
import com.ccomet.ailock.data.model.JudgePostResponse
import com.ccomet.ailock.data.model.JudgePreRequest
import com.ccomet.ailock.data.model.JudgePreResponse
import com.ccomet.ailock.data.remote.AiLockBackendApi
import com.ccomet.ailock.data.remote.EvaluateRequest
import com.ccomet.ailock.data.remote.EvaluateResponse
import com.ccomet.ailock.data.remote.StartSessionRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class OllamaDecisionRepository(context: Context) {
    private val appContext = context.applicationContext
    private val deviceId: String
        get() = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "ailock-android-device"

    private val api: AiLockBackendApi = Retrofit.Builder()
        .baseUrl(BACKEND_BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AiLockBackendApi::class.java)

    suspend fun judgePre(request: JudgePreRequest): JudgePreResponse = withContext(Dispatchers.IO) {
        val session = api.start(
            StartSessionRequest(
                deviceId = deviceId,
                appName = request.appName,
                lockTime = request.requestUseTime.coerceAtLeast(1),
            ),
        )
        val decision = api.evaluate(
            EvaluateRequest(
                sessionId = session.sessionId,
                deviceId = deviceId,
                userInput = request.preInput,
                appName = request.appName,
                targetUsage = request.dailyLimitMinutes.coerceAtLeast(1),
                todayUsage = request.todayAppUsageMinutes,
            ),
        )

        JudgePreResponse(
            sessionId = session.sessionId,
            status = decision.normalizedStatus,
            allowedTime = decision.safeAllowedTime,
            supportMessage = decision.safeMessage,
            reason = decision.safeMessage,
            summary = decision.normalizedStatus,
            source = SOURCE_BACKEND,
            finalDecision = finalDecision(decision.status, decision.allowedTime),
        )
    }

    suspend fun judgePost(request: JudgePostRequest): JudgePostResponse = withContext(Dispatchers.IO) {
        val sessionId = request.sessionId.ifBlank {
            api.start(
                StartSessionRequest(
                    deviceId = deviceId,
                    appName = request.appName,
                    lockTime = DEFAULT_POST_LOCK_MINUTES,
                ),
            ).sessionId
        }
        val decision = api.evaluate(
            EvaluateRequest(
                sessionId = sessionId,
                deviceId = deviceId,
                userInput = request.postInput,
                appName = request.appName,
                targetUsage = request.dailyLimitMinutes.coerceAtLeast(1),
                todayUsage = request.todayAppUsageMinutes,
            ),
        )

        JudgePostResponse(
            status = decision.normalizedStatus,
            allowedTime = decision.safeAllowedTime,
            supportMessage = decision.safeMessage,
            source = SOURCE_BACKEND,
            finalDecision = finalDecision(decision.status, decision.allowedTime),
        )
    }

    fun fallbackPostDecision(
        session: ActiveUseSession,
        postInput: String,
        requestCount: Int = 1,
    ): JudgePostResponse =
        JudgePostResponse(
            status = "FAIL",
            allowedTime = 0,
            supportMessage = "서버에 연결하지 못했어요. 이번에는 멈추는 쪽이 좋아요.",
            source = SOURCE_FALLBACK,
            finalDecision = "REJECT",
        )

    private fun finalDecision(status: String?, allowedTime: Int?): String {
        val normalized = status.orEmpty().uppercase()
        return if (normalized in ALLOW_STATUSES || (allowedTime ?: 0) > 0) "ALLOW" else "REJECT"
    }

    private val EvaluateResponse.normalizedStatus: String
        get() = status?.ifBlank { null } ?: if (safeAllowedTime > 0) "APPROVE" else "REJECT"

    private val EvaluateResponse.safeAllowedTime: Int
        get() = (allowedTime ?: 0).coerceAtLeast(0)

    private val EvaluateResponse.safeMessage: String
        get() = text?.ifBlank { null } ?: if (safeAllowedTime > 0) {
            "필요한 것만 확인하고 바로 돌아와."
        } else {
            "지금은 멈추는 쪽이 더 좋아 보여."
        }

    companion object {
        private val BACKEND_BASE_URL = BuildConfig.AILOCK_BACKEND_BASE_URL
        private const val SOURCE_BACKEND = "ailock-backend"
        private const val SOURCE_FALLBACK = "backend-fallback"
        private const val DEFAULT_POST_LOCK_MINUTES = 5
        private val ALLOW_STATUSES = setOf("APPROVE", "APPROVED", "ALLOW", "ALLOWED")

        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(110, TimeUnit.SECONDS)
            .build()
    }
}
