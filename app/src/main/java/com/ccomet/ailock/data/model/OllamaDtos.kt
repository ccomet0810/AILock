package com.ccomet.ailock.data.model

data class JudgePreRequest(
    val appName: String,
    val preInput: String,
    val requestUseTime: Int,
    val todayAppUsageMinutes: Int = 0,
    val dailyLimitMinutes: Int = 120,
    val previousRequest: PreviousUnlockRequest? = null,
    val forceNewSession: Boolean = false,
)

data class JudgePostRequest(
    val sessionId: String = "",
    val appName: String,
    val previousReason: String,
    val postInput: String,
    val requestCount: Int = 1,
    val todayAppUsageMinutes: Int = 0,
    val dailyLimitMinutes: Int = 120,
    val backendDeviceId: String? = null,
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
    val backendDeviceId: String? = null,
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
