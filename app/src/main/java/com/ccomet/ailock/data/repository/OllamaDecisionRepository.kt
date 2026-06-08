package com.ccomet.ailock.data.repository

import android.util.Log
import com.ccomet.ailock.data.model.ActiveUseSession
import com.ccomet.ailock.data.model.JudgePostRequest
import com.ccomet.ailock.data.model.JudgePostResponse
import com.ccomet.ailock.data.model.JudgePreRequest
import com.ccomet.ailock.data.model.JudgePreResponse
import com.ccomet.ailock.data.model.JudgmentCheck
import com.ccomet.ailock.data.model.OllamaCheckJson
import com.ccomet.ailock.data.model.OllamaGenerateRequest
import com.ccomet.ailock.data.model.PreviousUnlockRequest
import com.ccomet.ailock.data.remote.OllamaApi
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class OllamaDecisionRepository {
    private val gson = Gson()
    private val api: OllamaApi = Retrofit.Builder()
        .baseUrl(OLLAMA_BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OllamaApi::class.java)

    suspend fun judgePre(request: JudgePreRequest): JudgePreResponse = withContext(Dispatchers.IO) {
        val checks = judgeChecks(request)
        val usageCheck = usageAmountCheck(request.todayAppUsageMinutes, request.dailyLimitMinutes)
        val final = aggregateJudgment(
            checks = checks + usageCheck,
            requestedMinutes = request.requestUseTime,
            previous = request.previousRequest,
        )

        JudgePreResponse(
            sessionId = "ollama-session-${UUID.randomUUID()}",
            status = final.userStateLevel,
            allowedTime = final.allowMinutes,
            supportMessage = final.parentalMessage,
            reason = final.reasonForDecision,
            summary = final.summary,
            source = final.source,
            stateScore = final.stateScore,
            finalDecision = final.finalDecision,
            checks = final.checks,
        )
    }

    suspend fun judgePost(request: JudgePostRequest): JudgePostResponse = withContext(Dispatchers.IO) {
        val preLikeRequest = JudgePreRequest(
            appName = request.appName,
            preInput = request.postInput,
            requestUseTime = 3,
            previousRequest = PreviousUnlockRequest(
                userUnlockReason = request.previousReason,
                previousStateScore = BASE_STATE_SCORE,
                previousUserStateLevel = "CAUTION",
                finalDecision = "REJECT",
            ),
        )
        val pre = judgePre(preLikeRequest)
        JudgePostResponse(
            status = pre.status,
            allowedTime = pre.allowedTime,
            supportMessage = pre.supportMessage,
            source = pre.source,
            stateScore = pre.stateScore,
            finalDecision = pre.finalDecision,
            checks = pre.checks,
        )
    }

    fun fallbackPostDecision(
        session: ActiveUseSession,
        postInput: String,
        requestCount: Int = 1,
    ): JudgePostResponse {
        val request = JudgePreRequest(
            appName = session.appName,
            preInput = postInput,
            requestUseTime = 3,
            previousRequest = PreviousUnlockRequest(
                userUnlockReason = session.preInput,
                previousStateScore = BASE_STATE_SCORE,
                previousUserStateLevel = if (requestCount >= 4) "DANGER" else "CAUTION",
                finalDecision = "REJECT",
            ),
        )
        val checks = LocalUnlockJudgmentEngine.evaluateAll(request) + usageAmountCheck(0, 120)
        val final = aggregateJudgment(checks, request.requestUseTime, request.previousRequest, source = SOURCE_FALLBACK)
        return JudgePostResponse(
            status = final.userStateLevel,
            allowedTime = final.allowMinutes,
            supportMessage = final.parentalMessage,
            source = final.source,
            stateScore = final.stateScore,
            finalDecision = final.finalDecision,
            checks = final.checks,
        )
    }

    private suspend fun judgeChecks(request: JudgePreRequest): List<JudgmentCheck> = coroutineScope {
        CheckSpec.entries.map { spec ->
            async {
                val fallback = LocalUnlockJudgmentEngine.evaluate(request, spec)
                askOllama(checkPrompt(spec, request), fallback).getOrNull() ?: fallback
            }
        }.awaitAll()
    }

    private suspend fun askOllama(prompt: String, fallback: JudgmentCheck): Result<JudgmentCheck> =
        runCatching {
            val response = api.generate(
                OllamaGenerateRequest(
                    model = OLLAMA_MODEL,
                    prompt = prompt,
                ),
            ).response.orEmpty()
            val parsed = gson.fromJson(response, OllamaCheckJson::class.java)
            JudgmentCheck(
                checkType = parsed.check_type?.ifBlank { fallback.checkType } ?: fallback.checkType,
                plusScore = (parsed.plus_score ?: fallback.plusScore).coerceIn(0, 2),
                riskScore = (parsed.risk_score ?: fallback.riskScore).coerceIn(0, 2),
                weight = fallback.weight,
                reason = parsed.reason?.takeIf { it.isNotBlank() } ?: fallback.reason,
            )
        }.onFailure {
            Log.w(TAG, "Ollama check failed for ${fallback.checkType}; using local fallback.", it)
        }

    private fun usageAmountCheck(todayMinutes: Int, dailyLimitMinutes: Int): JudgmentCheck {
        val limit = dailyLimitMinutes.coerceAtLeast(1)
        val section = limit / 4f
        val usage = todayMinutes.coerceAtLeast(0)
        val scores = when {
            usage < section -> 2 to 0
            usage < section * 2 -> 1 to 0
            usage < section * 3 -> 0 to 1
            usage <= limit -> 0 to 2
            else -> 0 to 2
        }
        val reason = when (scores) {
            2 to 0 -> "오늘 사용 시간이 하루 기준 시간의 첫 번째 구간이라 아직 사용량이 적어요."
            1 to 0 -> "오늘 사용 시간이 하루 기준 시간의 두 번째 구간이라 아직 여유가 있어요."
            0 to 1 -> "오늘 사용 시간이 하루 기준 시간의 세 번째 구간이라 사용량이 많아지기 시작했어요."
            else -> "오늘 사용 시간이 하루 기준 시간의 마지막 구간이거나 기준을 넘었어요."
        }
        return JudgmentCheck(
            checkType = "USAGE_AMOUNT",
            plusScore = scores.first,
            riskScore = scores.second,
            weight = USAGE_WEIGHT,
            reason = reason,
        )
    }

    private fun aggregateJudgment(
        checks: List<JudgmentCheck>,
        requestedMinutes: Int,
        previous: PreviousUnlockRequest?,
        source: String = SOURCE_OLLAMA,
    ): FinalUnlockJudgment {
        val sortedChecks = CHECK_ORDER.mapNotNull { type -> checks.firstOrNull { it.checkType == type } }
        val weightedPlus = sortedChecks.sumOf { (it.plusScore * it.weight).toDouble() }.toFloat() / TOTAL_WEIGHT
        val weightedRisk = sortedChecks.sumOf { (it.riskScore * it.weight).toDouble() }.toFloat() / TOTAL_WEIGHT
        val forcedDanger = previous?.previousUserStateLevel == "DANGER"
        val previousScore = previous?.previousStateScore ?: BASE_STATE_SCORE
        val stateScore = if (forcedDanger) {
            0.25f
        } else {
            (previousScore + weightedPlus - weightedRisk).coerceIn(0f, 4f)
        }
        val level = userStateLevel(stateScore)
        val allow = !forcedDanger && stateScore >= ALLOW_THRESHOLD
        val finalDecision = if (allow) "ALLOW" else "REJECT"
        val allowMinutes = if (allow) requestedMinutes.coerceIn(1, 5) else 0
        val reason = decisionReason(sortedChecks, level, finalDecision, forcedDanger)
        val message = parentalMessage(finalDecision, level, allowMinutes, sortedChecks, forcedDanger)

        return FinalUnlockJudgment(
            checks = sortedChecks,
            previousStateScore = previousScore,
            weightedPlusScore = weightedPlus,
            weightedRiskScore = weightedRisk,
            stateScore = stateScore,
            userStateLevel = level,
            finalDecision = finalDecision,
            allowMinutes = allowMinutes,
            reasonForDecision = reason,
            parentalMessage = message,
            source = source,
        )
    }

    private fun userStateLevel(score: Float): String = when {
        score >= 3.5f -> "GOOD"
        score >= 2.5f -> "SOSO"
        score >= 1.5f -> "CAUTION"
        score >= 0.5f -> "WARNING"
        else -> "DANGER"
    }

    private fun decisionReason(
        checks: List<JudgmentCheck>,
        level: String,
        decision: String,
        forcedDanger: Boolean,
    ): String {
        if (forcedDanger) return "이전 요청이 DANGER 상태라 이번 잠금 상황에서는 계속 거절해요."
        if (decision == "ALLOW") return "요청 이유, 맥락, 필요성, 사용량, 말투가 모두 안정적이라 GOOD 상태에 해당해요."
        val strongestRisk = checks.maxWithOrNull(compareBy<JudgmentCheck> { it.riskScore }.thenBy { it.weight })
        return "${level} 상태예요. ${strongestRisk?.reason ?: "허용 기준인 GOOD까지 충분히 올라오지 못했어요."}"
    }

    private fun parentalMessage(
        decision: String,
        level: String,
        allowMinutes: Int,
        checks: List<JudgmentCheck>,
        forcedDanger: Boolean,
    ): String {
        if (decision == "ALLOW") {
            return "필요한 일이면 ${allowMinutes}분만 확인해. 다른 화면으로 새지 말고 바로 돌아와."
        }
        if (forcedDanger || level == "DANGER") {
            return "지금은 계속 앱을 열고 싶은 마음이 커진 상태로 보여. 오늘은 더 열어주지 않을게."
        }
        val riskReason = checks.firstOrNull { it.riskScore == 2 }?.reason
        return when (level) {
            "SOSO" -> "조금만 더 명확하게 말해줘. ${riskReason ?: "아직 GOOD까지는 부족해."}"
            "CAUTION" -> "요청이 아직 애매해. 정말 필요한 일인지 더 구체적으로 설명해줘."
            "WARNING" -> "지금은 거절 신호가 강해. 차분하게 필요한 이유를 다시 말해줘."
            else -> riskReason ?: "지금은 허용하기 어려워."
        }
    }

    private fun checkPrompt(spec: CheckSpec, request: JudgePreRequest): String {
        val previousJson = request.previousRequest?.let {
            """
            {
              "exists": true,
              "user_unlock_reason": "${it.userUnlockReason}",
              "previous_state_score": ${it.previousStateScore},
              "previous_user_state_level": "${it.previousUserStateLevel}",
              "final_decision": "${it.finalDecision}"
            }
            """.trimIndent()
        } ?: """{"exists": false}"""

        return """
            너는 사용자의 스마트폰 사용을 조절하는 Strong하고 Strict한 부모님 같은 AI이다.
            이번 판단에서 너의 역할은 오직 ${spec.koreanRole} 판단이다.
            다른 기준이나 최종 허용 여부는 절대 판단하지 마라.
            plus_score와 risk_score는 각각 0부터 2까지의 숫자로만 판단한다.
            출력은 반드시 JSON 형식으로만 한다. JSON 외 설명은 쓰지 마라.

            판단 기준:
            ${spec.promptRules}

            입력:
            {
              "locked_app_name": "${request.appName}",
              "user_unlock_reason": "${request.preInput}",
              "requested_minutes": ${request.requestUseTime},
              "today_app_usage_minutes": ${request.todayAppUsageMinutes},
              "daily_limit_minutes": ${request.dailyLimitMinutes},
              "previous_request": $previousJson
            }

            출력 형식:
            {
              "check_type": "${spec.checkType}",
              "plus_score": 0,
              "risk_score": 0,
              "reason": "판단 이유를 한국어로 짧게 작성"
            }
        """.trimIndent()
    }

    companion object {
        private const val TAG = "OllamaDecisionRepo"
        private const val OLLAMA_BASE_URL = "http://10.0.2.2:11434/"
        private const val OLLAMA_MODEL = "gemma4:e2b"
        private const val SOURCE_OLLAMA = "ollama"
        private const val SOURCE_FALLBACK = "local-fallback"
        private const val BASE_STATE_SCORE = 2.0f
        private const val ALLOW_THRESHOLD = 3.5f
        private const val CONTEXT_WEIGHT = 1.18f
        private const val SPECIFICITY_WEIGHT = 1.08f
        private const val NECESSITY_WEIGHT = 0.99f
        private const val USAGE_WEIGHT = 1.00f
        private const val TONE_WEIGHT = 0.75f
        private const val TOTAL_WEIGHT = 5.0f
        private val CHECK_ORDER = listOf("CONTEXT_MATCH", "SPECIFICITY", "NECESSITY", "USAGE_AMOUNT", "TONE")

        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(75, TimeUnit.SECONDS)
            .build()
    }

    enum class CheckSpec(
        val checkType: String,
        val weight: Float,
        val koreanRole: String,
        val promptRules: String,
    ) {
        CONTEXT(
            "CONTEXT_MATCH",
            CONTEXT_WEIGHT,
            "맥락 동일",
            """
            1. 앱 기능과 요청 목적이 명확히 일치하면 plus_score = 2, risk_score = 0
            2. 대체로 일치하지만 약간 애매하면 plus_score = 1, risk_score = 0
            3. 가능할 수도 있지만 불분명하면 plus_score = 0, risk_score = 1
            4. 거의 맞지 않거나 명확히 불일치하면 plus_score = 0, risk_score = 2
            """.trimIndent(),
        ),
        SPECIFICITY(
            "SPECIFICITY",
            SPECIFICITY_WEIGHT,
            "요청 이유 구체성",
            """
            1. 사람, 목적, 행동이 명확하면 plus_score = 2, risk_score = 0
            2. 무엇을 할지는 명확하지만 짧으면 plus_score = 1, risk_score = 0
            3. 목적은 있지만 행동이 부족하면 plus_score = 0, risk_score = 1
            4. 잠깐, 그냥, 조금만 수준이면 plus_score = 0, risk_score = 2
            5. 이유 없거나 감정만 있으면 plus_score = 0, risk_score = 2
            """.trimIndent(),
        ),
        NECESSITY(
            "NECESSITY",
            NECESSITY_WEIGHT,
            "진짜 필요한 사용인지",
            """
            1. 팀플, 공지, 약속, 가족 연락, 일정 확인이면 plus_score = 2, risk_score = 0
            2. 친구 메시지 확인 또는 답장이면 plus_score = 1, risk_score = 0
            3. 필요할 수도 있지만 정보가 부족하면 plus_score = 0, risk_score = 1
            4. 피드 확인, 가벼운 둘러보기면 plus_score = 0, risk_score = 1
            5. 릴스, 쇼츠, 게임, 쇼핑, 심심함, 스트레스 해소면 plus_score = 0, risk_score = 2
            시간이 제한되어 있어도 목적이 오락이면 risk_score = 2
            """.trimIndent(),
        ),
        TONE(
            "TONE",
            TONE_WEIGHT,
            "사용자 말투",
            """
            1. 차분하게 목적을 설명하거나 사용 시간과 범위를 약속하면 plus_score = 2, risk_score = 0
            2. 비교적 차분하지만 짧으면 plus_score = 1, risk_score = 0
            3. 모호하지만 공격적이지 않으면 plus_score = 0, risk_score = 1
            4. 감정 호소 또는 짧은 명령형이면 plus_score = 0, risk_score = 1
            5. 간청, 짜증, 반발, 반복 주장이면 plus_score = 0, risk_score = 2
            재요청이면 이전보다 차분하고 구체적인지 비교한다.
            """.trimIndent(),
        ),
    }
}

private object LocalUnlockJudgmentEngine {
    fun evaluateAll(request: JudgePreRequest): List<JudgmentCheck> =
        OllamaDecisionRepository.CheckSpec.entries.map { evaluate(request, it) }

    fun evaluate(request: JudgePreRequest, spec: OllamaDecisionRepository.CheckSpec): JudgmentCheck =
        when (spec.checkType) {
            "CONTEXT_MATCH" -> contextMatch(request, spec.weight)
            "SPECIFICITY" -> specificity(request, spec.weight)
            "NECESSITY" -> necessity(request, spec.weight)
            "TONE" -> tone(request, spec.weight)
            else -> JudgmentCheck(spec.checkType, 0, 1, spec.weight, "판단 기준을 찾지 못했어요.")
        }

    private fun contextMatch(request: JudgePreRequest, weight: Float): JudgmentCheck {
        val app = request.appName.lowercase(Locale.getDefault())
        val text = normalized(request.preInput)
        val social = app.hasAny("instagram", "인스타", "카카오", "kakao", "facebook", "x", "twitter")
        val video = app.hasAny("youtube", "유튜브", "netflix", "넷플", "tving", "티빙")
        val shopping = app.hasAny("shop", "쇼핑", "쿠팡", "11번가", "번개")
        val game = app.hasAny("game", "게임")
        val score = when {
            game && text.hasAny("강의", "자료", "공지", "팀플") -> 0 to 2
            shopping && text.hasAny("팀플", "공지", "강의", "자료") -> 0 to 2
            social && text.hasAny("dm", "디엠", "메시지", "연락", "답장", "공지", "피드", "릴스") -> 2 to 0
            video && text.hasAny("강의", "수업", "영상", "영화", "드라마", "시청", "넷플") -> 2 to 0
            text.hasAny("확인", "자료", "관련", "정보") -> 1 to 0
            text.isBlank() -> 0 to 2
            else -> 0 to 1
        }
        return JudgmentCheck("CONTEXT_MATCH", score.first, score.second, weight, "앱 기능과 요청 목적의 맥락을 봤어요.")
    }

    private fun specificity(request: JudgePreRequest, weight: Float): JudgmentCheck {
        val text = normalized(request.preInput)
        val concrete = listOf("친구", "가족", "팀플", "공지", "약속", "dm", "디엠", "메시지", "답장", "강의", "수업", "자료", "일정", "릴스", "쇼츠", "게임", "쇼핑", "피드", "웹툰", "영상")
            .count { text.contains(it) }
        val action = text.hasAny("확인", "답장", "보고", "볼", "보내", "나올", "끌게", "찾")
        val score = when {
            text.isBlank() || text.hasAny("열어줘", "답답", "그냥") -> 0 to 2
            text.hasAny("잠깐", "조금만", "한번만") && concrete == 0 -> 0 to 2
            concrete >= 1 && action -> 2 to 0
            concrete >= 1 -> 1 to 0
            text.hasAny("관련", "필요", "확인") -> 0 to 1
            else -> 0 to 2
        }
        return JudgmentCheck("SPECIFICITY", score.first, score.second, weight, "요청 이유가 얼마나 구체적인지 봤어요.")
    }

    private fun necessity(request: JudgePreRequest, weight: Float): JudgmentCheck {
        val text = normalized(request.preInput)
        val important = text.hasAny("팀플", "공지", "약속", "가족", "일정", "제출", "수업", "강의", "자료", "연락", "취소", "변경")
        val message = text.hasAny("친구", "dm", "디엠", "메시지", "답장")
        val leisure = text.hasAny("릴스", "쇼츠", "게임", "쇼핑", "피드", "구경", "심심", "스트레스", "답답", "웹툰", "영상")
        val score = when {
            leisure -> 0 to 2
            important -> 2 to 0
            message -> 1 to 0
            text.hasAny("관련", "정보", "확인") -> 0 to 1
            else -> 0 to 2
        }
        return JudgmentCheck("NECESSITY", score.first, score.second, weight, "자기조절 목표에 비추어 필요한 사용인지 봤어요.")
    }

    private fun tone(request: JudgePreRequest, weight: Float): JudgmentCheck {
        val text = normalized(request.preInput)
        val previous = request.previousRequest?.userUnlockReason?.let(::normalized).orEmpty()
        val repeated = previous.isNotBlank() && previous == text
        val selfLimit = text.contains(Regex("""\d+\s*(분|개|판)""")) || text.hasAny("바로", "나올", "답장만", "확인만", "하나만")
        val calm = text.hasAny("확인", "답장", "나올게", "끌게", "할게")
        val score = when {
            repeated -> 0 to 2
            text.hasAny("제발", "아 ", "짜증", "풀어", "한번만", "이번만") -> 0 to 2
            text.hasAny("답답", "스트레스", "보고 싶") -> 0 to 1
            selfLimit -> 2 to 0
            calm -> 1 to 0
            text.hasAny("잠깐", "부탁") -> 0 to 1
            else -> 0 to 1
        }
        return JudgmentCheck("TONE", score.first, score.second, weight, "말투가 차분하고 자기조절적인지 봤어요.")
    }

    private fun normalized(text: String): String = text.trim().lowercase(Locale.getDefault())
    private fun String.hasAny(vararg needles: String): Boolean = needles.any { contains(it.lowercase(Locale.getDefault())) }
}

private data class FinalUnlockJudgment(
    val checks: List<JudgmentCheck>,
    val previousStateScore: Float,
    val weightedPlusScore: Float,
    val weightedRiskScore: Float,
    val stateScore: Float,
    val userStateLevel: String,
    val finalDecision: String,
    val allowMinutes: Int,
    val reasonForDecision: String,
    val parentalMessage: String,
    val source: String,
) {
    val summary: String
        get() = "state=${String.format(Locale.US, "%.2f", stateScore)}, level=$userStateLevel, decision=$finalDecision"
}
