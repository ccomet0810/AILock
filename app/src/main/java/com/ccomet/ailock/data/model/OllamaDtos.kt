package com.ccomet.ailock.data.model

data class JudgePreRequest(
    val appName: String,
    val preInput: String,
    val requestUseTime: Int,
    val todayAppUsageMinutes: Int = 0,
    val dailyLimitMinutes: Int = 120,
    val previousRequest: PreviousUnlockRequest? = null,
)

data class JudgePostRequest(
    val appName: String,
    val previousReason: String,
    val postInput: String,
    val requestCount: Int = 1,
)

data class JudgePreResponse(
    val sessionId: String,
    val status: String,
    val allowedTime: Int,
    val supportMessage: String,
    val reason: String,
    val summary: String,
    val source: String,
    val stateScore: Float = 0f,
    val finalDecision: String = "REJECT",
    val checks: List<JudgmentCheck> = emptyList(),
)

data class JudgePostResponse(
    val status: String,
    val allowedTime: Int,
    val supportMessage: String,
    val source: String,
    val stateScore: Float = 0f,
    val finalDecision: String = "REJECT",
    val checks: List<JudgmentCheck> = emptyList(),
)

data class PreviousUnlockRequest(
    val userUnlockReason: String,
    val previousStateScore: Float,
    val previousUserStateLevel: String,
    val finalDecision: String,
)

data class JudgmentCheck(
    val checkType: String,
    val plusScore: Int,
    val riskScore: Int,
    val weight: Float,
    val reason: String,
)

data class OllamaGenerateRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
    val format: String = "json",
)

data class OllamaGenerateResponse(
    val response: String? = null,
)

data class OllamaJudgeJson(
    val decision: String? = null,
    val allow_minutes: Int? = null,
    val message: String? = null,
    val reason: String? = null,
    val user_state_level: String? = null,
)

data class OllamaCheckJson(
    val check_type: String? = null,
    val plus_score: Int? = null,
    val risk_score: Int? = null,
    val reason: String? = null,
)
