package com.ccomet.ailock.data.repository

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ccomet.ailock.BuildConfig
import com.ccomet.ailock.data.local.ailockDataStore
import com.ccomet.ailock.data.model.ActiveUseSession
import com.ccomet.ailock.data.model.JudgePostRequest
import com.ccomet.ailock.data.model.JudgePostResponse
import com.ccomet.ailock.data.model.JudgePreRequest
import com.ccomet.ailock.data.model.JudgePreResponse
import com.ccomet.ailock.data.remote.AiLockBackendApi
import com.ccomet.ailock.data.remote.DeviceRegisterRequest
import com.ccomet.ailock.data.remote.EvaluateRequest
import com.ccomet.ailock.data.remote.EvaluateResponse
import com.ccomet.ailock.data.remote.StartSessionRequest
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class OllamaDecisionRepository(context: Context) {
    private val appContext = context.applicationContext
    private val apiCache = mutableMapOf<String, AiLockBackendApi>()
    private val registeredDeviceKeys = mutableSetOf<String>()
    private val gson = Gson()
    private val deviceId: String
        get() = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "ailock-android-device"

    suspend fun judgePre(request: JudgePreRequest): JudgePreResponse = withContext(Dispatchers.IO) {
        val backendDeviceId = if (request.forceNewSession) freshBackendDeviceId() else deviceId
        val (session, decision) = runBackendCatching {
            val api = backendApi()
            ensureDeviceRegistered(api, backendDeviceId)
            val session = api.start(
                StartSessionRequest(
                    deviceId = backendDeviceId,
                    appName = request.appName,
                    lockTime = request.requestUseTime.coerceAtLeast(1),
                ),
            )
            val decision = api.evaluate(
                EvaluateRequest(
                    sessionId = session.sessionId,
                    deviceId = backendDeviceId,
                    userInput = request.preInput,
                    appName = request.appName,
                    targetUsage = request.dailyLimitMinutes.coerceAtLeast(1),
                    todayUsage = request.todayAppUsageMinutes,
                ),
            )
            session to decision
        }

        JudgePreResponse(
            sessionId = session.sessionId,
            status = decision.normalizedStatus,
            allowedTime = decision.safeAllowedTime,
            supportMessage = decision.safeMessage,
            reason = decision.safeMessage,
            summary = decision.normalizedStatus,
            source = SOURCE_BACKEND,
            finalDecision = finalDecision(decision.status, decision.allowedTime),
            backendDeviceId = backendDeviceId,
        )
    }

    suspend fun judgePost(request: JudgePostRequest): JudgePostResponse = withContext(Dispatchers.IO) {
        val backendDeviceId = request.backendDeviceId?.takeIf { it.isNotBlank() } ?: deviceId
        val decision = runBackendCatching {
            val api = backendApi()
            ensureDeviceRegistered(api, backendDeviceId)
            val sessionId = request.sessionId.ifBlank {
                api.start(
                    StartSessionRequest(
                        deviceId = backendDeviceId,
                        appName = request.appName,
                        lockTime = DEFAULT_POST_LOCK_MINUTES,
                    ),
                ).sessionId
            }
            api.evaluate(
                EvaluateRequest(
                    sessionId = sessionId,
                    deviceId = backendDeviceId,
                    userInput = request.postInput,
                    appName = request.appName,
                    targetUsage = request.dailyLimitMinutes.coerceAtLeast(1),
                    todayUsage = request.todayAppUsageMinutes,
                ),
            )
        }

        JudgePostResponse(
            status = decision.normalizedStatus,
            allowedTime = decision.safeAllowedTime,
            supportMessage = decision.safeMessage,
            source = SOURCE_BACKEND,
            finalDecision = finalDecision(decision.status, decision.allowedTime),
        )
    }

    suspend fun testBackendConnection(baseUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedBaseUrl = normalizeBackendBaseUrl(baseUrl)
            val api = backendApi(normalizedBaseUrl)
            runBackendCatching {
                ensureDeviceRegistered(api, deviceId, normalizedBaseUrl)
            }
        }
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

    private suspend fun backendApi(): AiLockBackendApi =
        backendApi(backendBaseUrl())

    private fun backendApi(baseUrl: String): AiLockBackendApi {
        apiCache[baseUrl]?.let { return it }
        return synchronized(apiCache) {
            apiCache[baseUrl] ?: Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AiLockBackendApi::class.java)
                .also { apiCache[baseUrl] = it }
        }
    }

    private suspend fun ensureDeviceRegistered(api: AiLockBackendApi) {
        ensureDeviceRegistered(api, deviceId, backendBaseUrl())
    }

    private suspend fun ensureDeviceRegistered(api: AiLockBackendApi, targetDeviceId: String) {
        ensureDeviceRegistered(api, targetDeviceId, backendBaseUrl())
    }

    private suspend fun ensureDeviceRegistered(api: AiLockBackendApi, targetDeviceId: String, baseUrl: String) {
        val key = "$baseUrl#$targetDeviceId"
        synchronized(registeredDeviceKeys) {
            if (key in registeredDeviceKeys) return
        }

        api.registerDevice(DeviceRegisterRequest(deviceId = targetDeviceId))

        synchronized(registeredDeviceKeys) {
            registeredDeviceKeys += key
        }
    }

    private fun freshBackendDeviceId(): String =
        "$deviceId-retry-${UUID.randomUUID()}"

    private suspend inline fun <T> runBackendCatching(block: () -> T): T =
        try {
            block()
        } catch (exception: HttpException) {
            throw BackendRequestException(userFriendlyHttpMessage(exception), exception)
        } catch (exception: IOException) {
            throw BackendRequestException(
                "서버에 연결하지 못했어요. 인터넷 연결이나 서버 상태를 확인한 뒤 다시 시도해주세요.",
                exception,
            )
        }

    private fun userFriendlyHttpMessage(exception: HttpException): String {
        val serverMessage = extractBackendMessage(exception)
            .takeUnless { it.isTechnicalHttpText() }

        return when (exception.code()) {
            400 -> serverMessage ?: "요청 정보가 올바르지 않아요. 잠시 후 다시 시도해주세요."
            401, 403 -> serverMessage ?: "지금 이 기기에서는 요청을 처리할 수 없어요. 앱을 다시 실행한 뒤 시도해주세요."
            404 -> serverMessage ?: "서버에서 필요한 정보를 찾지 못했어요. 다시 시도해주세요."
            408 -> serverMessage ?: "요청 시간이 너무 오래 걸렸어요. 잠시 후 다시 시도해주세요."
            409 -> serverMessage ?: "이미 처리 중인 요청이 있어요. 잠시 후 다시 시도해주세요."
            429 -> serverMessage ?: "요청이 한 번에 몰려서 잠시 처리할 수 없어요. 조금 있다가 다시 시도해주세요."
            in 500..599 -> serverMessage ?: "서버에서 잠시 문제가 생겼어요. 조금 있다가 다시 시도해주세요."
            else -> serverMessage ?: "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요."
        }
    }

    private fun extractBackendMessage(exception: HttpException): String {
        val body = exception.response()?.errorBody()?.string().orEmpty()
        if (body.isBlank()) return exception.message()

        return runCatching {
            val root = JsonParser.parseString(body).asJsonObject
            listOf("message", "detail", "error")
                .firstNotNullOfOrNull { key -> root.stringValue(key)?.takeIf { it.isNotBlank() } }
                ?: body
        }.getOrDefault(body)
    }

    private fun String.isTechnicalHttpText(): Boolean {
        val normalized = trim()
        if (normalized.isBlank()) return true
        return normalized.equals("Bad Request", ignoreCase = true) ||
            normalized.equals("Unauthorized", ignoreCase = true) ||
            normalized.equals("Forbidden", ignoreCase = true) ||
            normalized.equals("Not Found", ignoreCase = true) ||
            normalized.equals("Conflict", ignoreCase = true) ||
            normalized.equals("Too Many Requests", ignoreCase = true) ||
            normalized.equals("Internal Server Error", ignoreCase = true) ||
            normalized.startsWith("<!DOCTYPE", ignoreCase = true) ||
            normalized.startsWith("<html", ignoreCase = true)
    }

    private fun JsonObject.stringValue(key: String): String? =
        get(key)?.takeIf { !it.isJsonNull }?.let { element ->
            if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
                element.asString
            } else {
                gson.toJson(element)
            }
        }

    private suspend fun backendBaseUrl(): String {
        val saved = appContext.ailockDataStore.data.first()[BACKEND_BASE_URL]
        return normalizeBackendBaseUrl(saved ?: DEFAULT_BACKEND_BASE_URL)
    }

    private fun normalizeBackendBaseUrl(url: String): String {
        val trimmed = url.trim().ifBlank { DEFAULT_BACKEND_BASE_URL }
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
        val normalized = if (withScheme.endsWith("/")) withScheme else "$withScheme/"
        return if (normalized in LEGACY_BACKEND_BASE_URLS) DEFAULT_BACKEND_BASE_URL else normalized
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
        private val BACKEND_BASE_URL = stringPreferencesKey("backend_base_url")
        private val DEFAULT_BACKEND_BASE_URL = BuildConfig.AILOCK_BACKEND_BASE_URL
        private val LEGACY_BACKEND_BASE_URLS = setOf(
            "http://210.222.240.170:8080/",
            "http://168.188.128.36:8080/",
        )
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

class BackendRequestException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
