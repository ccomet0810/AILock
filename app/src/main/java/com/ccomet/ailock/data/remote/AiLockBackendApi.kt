package com.ccomet.ailock.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface AiLockBackendApi {
    @POST("start")
    suspend fun start(@Body request: StartSessionRequest): StartSessionResponse

    @POST("evaluate")
    suspend fun evaluate(@Body request: EvaluateRequest): EvaluateResponse
}

data class StartSessionRequest(
    val deviceId: String,
    val appName: String,
    val lockTime: Int,
)

data class StartSessionResponse(
    val sessionId: String = "",
)

data class EvaluateRequest(
    val sessionId: String,
    val deviceId: String,
    val userInput: String,
    val appName: String,
    val targetUsage: Int,
    val todayUsage: Int,
)

data class EvaluateResponse(
    val text: String? = null,
    val allowedTime: Int? = null,
    val status: String? = null,
)
