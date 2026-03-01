package com.bitacora.digital.service.providers

import com.bitacora.digital.service.api.MistralApi
import com.bitacora.digital.service.api.MistralChatMessage
import com.bitacora.digital.service.api.MistralChatRequest
import com.bitacora.digital.util.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Mistral AI provider.
 */
class MistralProvider(
    private val apiKey: String
) : AIProvider {

    override val providerType: AIProviderType = AIProviderType.MISTRAL
    override val supportsAudio: Boolean = false

    companion object {
        private const val BASE_URL = "https://api.mistral.ai/"
        private const val MODEL = "mistral-large-latest"
    }

    private val api: MistralApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(Config.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Config.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MistralApi::class.java)
    }

    override suspend fun validateApiKey(key: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = MistralChatRequest(
                    model = "mistral-small-latest",
                    messages = listOf(MistralChatMessage(role = "user", content = "Hi")),
                    max_tokens = 5
                )
                api.chatCompletion("Bearer $key", request)
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
            val messages = buildMessages(systemPrompt, conversationHistory, userMessage)
            callApi(messages)
        }
    }

    override suspend fun generateResponseFromAudio(
        systemPrompt: String,
        conversationHistory: List<AIMessage>,
        audioBase64: String
    ): String {
        throw AudioNotSupportedException(providerType)
    }

    private fun buildMessages(
        systemPrompt: String,
        conversationHistory: List<AIMessage>,
        userMessage: String
    ): List<MistralChatMessage> {
        val messages = mutableListOf<MistralChatMessage>()

        // System message
        messages.add(MistralChatMessage(role = "system", content = systemPrompt))

        // Conversation history
        for (msg in conversationHistory) {
            val role = if (msg.role == AIMessageRole.ASSISTANT) "assistant" else "user"
            messages.add(MistralChatMessage(role = role, content = msg.content))
        }

        // Current user message
        messages.add(MistralChatMessage(role = "user", content = userMessage))

        return messages
    }

    private suspend fun callApi(messages: List<MistralChatMessage>): String {
        val request = MistralChatRequest(
            model = MODEL,
            messages = messages,
            temperature = Config.GENERATION_TEMPERATURE,
            max_tokens = Config.GENERATION_MAX_OUTPUT_TOKENS,
            top_p = Config.GENERATION_TOP_P
        )

        val response = api.chatCompletion("Bearer $apiKey", request)

        return response.choices?.firstOrNull()
            ?.message?.content?.trim()
            ?: throw Exception("No content in response")
    }
}
