package com.ccomet.ailock.data.remote

import com.ccomet.ailock.data.model.OllamaGenerateRequest
import com.ccomet.ailock.data.model.OllamaGenerateResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OllamaApi {
    @POST("api/generate")
    suspend fun generate(@Body request: OllamaGenerateRequest): OllamaGenerateResponse
}
