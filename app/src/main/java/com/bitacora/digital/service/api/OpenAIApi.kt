package com.bitacora.digital.service.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for OpenAI API.
 */
interface OpenAIApi {
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIChatRequest
    ): OpenAIChatResponse
}

/**
 * Request body for OpenAI chat completions.
 */
data class OpenAIChatRequest(
    val model: String,
    val messages: List<OpenAIChatMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 200,
    val top_p: Double = 0.9
)

/**
 * Message in OpenAI chat format.
 */
data class OpenAIChatMessage(
    val role: String,
    val content: String
)

/**
 * Response from OpenAI chat completions.
 */
data class OpenAIChatResponse(
    val choices: List<OpenAIChatChoice>?
)

/**
 * Choice in OpenAI response.
 */
data class OpenAIChatChoice(
    val message: OpenAIChatMessage?
)
