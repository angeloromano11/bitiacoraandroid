package com.bitacora.digital.service.providers

import com.bitacora.digital.service.api.Content
import com.bitacora.digital.service.api.GenerateContentRequest
import com.bitacora.digital.service.api.GenerationConfig
import com.bitacora.digital.service.api.GeminiApi
import com.bitacora.digital.service.api.InlineData
import com.bitacora.digital.service.api.Part
import com.bitacora.digital.util.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Google Gemini AI provider with native audio support.
 */
class GeminiProvider(
    private val apiKey: String
) : AIProvider {

    override val providerType: AIProviderType = AIProviderType.GEMINI
    override val supportsAudio: Boolean = true

    private val api: GeminiApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(Config.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Config.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(Config.GEMINI_API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }

    override suspend fun validateApiKey(key: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = "Hi")))),
                    generationConfig = GenerationConfig(maxOutputTokens = 5)
                )
                api.generateContent(key, request)
                true
            } catch (e: Exception) {
                // Rate limit means key is valid
                e.message?.contains("429") == true
            }
        }
    }

    override suspend fun generateResponse(
        systemPrompt: String,
        conversationHistory: List<AIMessage>,
        userMessage: String
    ): String {
        return withContext(Dispatchers.IO) {
            val prompt = buildPrompt(systemPrompt, conversationHistory, userMessage)
            callApi(prompt)
        }
    }

    override suspend fun generateResponseFromAudio(
        systemPrompt: String,
        conversationHistory: List<AIMessage>,
        audioBase64: String
    ): String {
        return withContext(Dispatchers.IO) {
            val prompt = buildPrompt(
                systemPrompt,
                conversationHistory,
                "Generate the next question based on the user's audio response:"
            )
            callApi(prompt, audioBase64)
        }
    }

    private fun buildPrompt(
        systemPrompt: String,
        conversationHistory: List<AIMessage>,
        userMessage: String
    ): String {
        val history = conversationHistory.joinToString("\n") { msg ->
            val role = if (msg.role == AIMessageRole.ASSISTANT) "Interviewer" else "User"
            "$role: ${msg.content}"
        }

        return """
            $systemPrompt

            Conversation so far:
            $history

            User's response: $userMessage

            Generate the next question:
        """.trimIndent()
    }

    private suspend fun callApi(prompt: String, audioBase64: String? = null): String {
        val parts = mutableListOf<Part>()
        parts.add(Part(text = prompt))

        audioBase64?.let {
            parts.add(Part(inlineData = InlineData(mimeType = "audio/wav", data = it)))
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(
                temperature = Config.GENERATION_TEMPERATURE,
                topP = Config.GENERATION_TOP_P,
                topK = Config.GENERATION_TOP_K,
                maxOutputTokens = Config.GENERATION_MAX_OUTPUT_TOKENS
            )
        )

        val response = api.generateContent(apiKey, request)

        return response.candidates?.firstOrNull()
            ?.content?.parts?.firstOrNull()
            ?.text?.trim()
            ?: throw Exception("No content in response")
    }
}
