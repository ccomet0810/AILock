package com.ccomet.ailock.data.repository

import android.util.Log
import com.ccomet.ailock.data.model.AiDecisionRequest
import com.ccomet.ailock.data.model.AiDecisionResponse
import com.ccomet.ailock.data.model.AiDecisionStatus
import com.ccomet.ailock.data.remote.AiDecisionApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class AiDecisionRepository {
    private val apiCache = ConcurrentHashMap<String, AiDecisionApi>()

    suspend fun decide(
        request: AiDecisionRequest,
        baseUrl: String,
        useMock: Boolean,
    ): AiDecisionResponse = withContext(Dispatchers.IO) {
        if (useMock) {
            Log.d(TAG, "Using mock AI decision. request=$request")
            return@withContext mockDecision(request).copy(source = SOURCE_MOCK)
        }

        val normalized = normalizeBackendBaseUrl(baseUrl)
        runCatching {
            Log.d(TAG, "POST ${normalized.trimEnd('/')}/testFinal request=$request")
            apiFor(normalized).decide(request).copy(source = SOURCE_SERVER)
        }.onSuccess { response ->
            Log.d(TAG, "AI backend response=$response")
        }.getOrElse { error ->
            Log.w(TAG, "AI backend failed. baseUrl=$normalized", error)
            AiDecisionResponse(
                status = AiDecisionStatus.FAIL,
                text = "서버 연결에 실패했어. 요청 주소는 ${normalized.trimEnd('/')}/testFinal 이야. SSH 터널, bootRun, 앱의 Base URL, 같은 네트워크 여부를 확인해줘. (${error.javaClass.simpleName}: ${error.message})",
                allowedTime = 0,
                source = SOURCE_NETWORK_ERROR,
            )
        }
    }

    private fun apiFor(baseUrl: String): AiDecisionApi {
        val normalized = normalizeBackendBaseUrl(baseUrl)
        return apiCache.getOrPut(normalized) {
            Retrofit.Builder()
                .baseUrl(normalized)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AiDecisionApi::class.java)
        }
    }

    private fun normalizeBackendBaseUrl(url: String): String {
        val withoutEndpoint = url.trim()
            .replace(Regex("/testFinal/?$", RegexOption.IGNORE_CASE), "")
        return if (withoutEndpoint.endsWith("/")) withoutEndpoint else "$withoutEndpoint/"
    }

    private fun mockDecision(request: AiDecisionRequest): AiDecisionResponse {
        val stats = request.currentStats
        val reasonLength = request.userInput.trim().length
        val asks = stats.todayOpenAppCount
        val accumulated = stats.accumUseApp
        val willPower = stats.willPowerScore

        return when {
            willPower <= 25 || asks >= 8 || accumulated >= 120 -> AiDecisionResponse(
                status = AiDecisionStatus.CRITICAL,
                text = "지금은 멈추는 게 맞아. 오늘은 이미 많이 버텼고, 더 열면 약속이 무너질 것 같아.",
                allowedTime = 0,
            )

            accumulated >= 75 || asks >= 5 -> AiDecisionResponse(
                status = AiDecisionStatus.OVERUSE,
                text = "오늘 꽤 많이 켰어. 꼭 필요한 일이라면 5분만 보고 바로 나오자.",
                allowedTime = 5,
            )

            asks >= 3 || willPower < 55 -> AiDecisionResponse(
                status = AiDecisionStatus.WARNING,
                text = "좋아, 이유는 이해했어. 대신 10분만 보고 마무리하는 걸로 약속하자.",
                allowedTime = 10,
            )

            reasonLength >= 12 -> AiDecisionResponse(
                status = AiDecisionStatus.OPTIMAL,
                text = "목적이 분명하네. 30분 안에 필요한 것만 확인하고 돌아오자.",
                allowedTime = 30,
            )

            else -> AiDecisionResponse(
                status = AiDecisionStatus.WARNING,
                text = "이유가 조금 애매해. 15분만 허용할게. 끝나면 내가 다시 부를 거야.",
                allowedTime = 15,
            )
        }
    }

    companion object {
        private const val TAG = "AiDecisionRepository"
        private const val SOURCE_SERVER = "server"
        private const val SOURCE_MOCK = "mock"
        private const val SOURCE_NETWORK_ERROR = "network-error"

        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .callTimeout(240, TimeUnit.SECONDS)
            .build()
    }
}
