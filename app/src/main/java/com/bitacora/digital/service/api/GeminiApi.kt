package com.bitacora.digital.service.api

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface for Gemini API.
 */
interface GeminiApi {
    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

/**
 * Request body for Gemini API.
 */
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig
)

/**
 * Content object containing parts.
 */
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

/**
 * Part can be text or inline data (audio/image).
 */
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

/**
 * Inline data for audio/image content.
 */
data class InlineData(
    val mimeType: String,
    val data: String
)

/**
 * Generation configuration for Gemini.
 */
data class GenerationConfig(
    val temperature: Double = 0.7,
    val topP: Double = 0.9,
    val topK: Int = 40,
    val maxOutputTokens: Int = 200
)

/**
 * Response from Gemini API.
 */
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

/**
 * Candidate response containing content.
 */
data class Candidate(
    val content: Content?
)
