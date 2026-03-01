package com.bitacora.digital.service.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for Anthropic API.
 */
interface AnthropicApi {
    @POST("v1/messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: AnthropicMessageRequest
    ): AnthropicMessageResponse
}

/**
 * Request body for Anthropic messages API.
 * Note: System prompt is a top-level parameter, not in messages.
 */
data class AnthropicMessageRequest(
    val model: String,
    val max_tokens: Int,
    val system: String? = null,
    val messages: List<AnthropicMessage>
)

/**
 * Message in Anthropic format.
 */
data class AnthropicMessage(
    val role: String,
    val content: String
)

/**
 * Response from Anthropic messages API.
 */
data class AnthropicMessageResponse(
    val content: List<AnthropicContentBlock>?
)

/**
 * Content block in Anthropic response.
 */
data class AnthropicContentBlock(
    val type: String,
    val text: String?
)
