package com.ccomet.ailock.data.remote

import com.ccomet.ailock.data.model.AiDecisionRequest
import com.ccomet.ailock.data.model.AiDecisionResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AiDecisionApi {
    @POST("testFinal")
    suspend fun decide(@Body request: AiDecisionRequest): AiDecisionResponse
}
