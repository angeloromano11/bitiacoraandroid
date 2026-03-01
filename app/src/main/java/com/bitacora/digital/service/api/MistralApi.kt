package com.bitacora.digital.service.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for Mistral API (OpenAI-compatible).
 */
interface MistralApi {
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: MistralChatRequest
    ): MistralChatResponse
}

/**
 * Request body for Mistral chat completions.
 */
data class MistralChatRequest(
    val model: String,
    val messages: List<MistralChatMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 200,
    val top_p: Double = 0.9
)

/**
 * Message in Mistral chat format.
 */
data class MistralChatMessage(
    val role: String,
    val content: String
)

/**
 * Response from Mistral chat completions.
 */
data class MistralChatResponse(
    val choices: List<MistralChatChoice>?
)

/**
 * Choice in Mistral response.
 */
data class MistralChatChoice(
    val message: MistralChatMessage?
)
