package com.ccomet.ailock.data.model

data class ActiveUseSession(
    val packageName: String,
    val appName: String,
    val sessionId: String,
    val preInput: String,
    val plannedMinutes: Int,
    val startedAt: Long,
    val expectedEndAt: Long,
    val state: String = "IN_USE",
)

data class PendingFinalDecision(
    val sessionId: String,
    val status: String? = null,
    val allowedTime: Int = 0,
    val supportMessage: String? = null,
    val userInput: String = "",
    val stateScore: Float = 0f,
    val finalDecision: String = "REJECT",
    val reasonForDecision: String = "",
    val requestedAt: Long = System.currentTimeMillis(),
)

enum class InterventionState {
    START_INPUT,
    PRE_LOADING,
    IN_USE,
    TIME_EXPIRED,
    ADDITIONAL_REASON_INPUT,
    GRACE_PERIOD,
    WAITING_FINAL_DECISION,
    FINAL_DECISION,
    UPDATE_LOADING,
    CLOSED,
    ERROR,
}

