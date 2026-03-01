package com.bitacora.digital.service.providers

import com.bitacora.digital.service.api.AnthropicApi
import com.bitacora.digital.service.api.AnthropicMessage
import com.bitacora.digital.service.api.AnthropicMessageRequest
import com.bitacora.digital.util.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Anthropic Claude AI provider.
 */
class AnthropicProvider(
    private val apiKey: String
) : AIProvider {

    override val providerType: AIProviderType = AIProviderType.ANTHROPIC
    override val supportsAudio: Boolean = false

    companion object {
        private const val BASE_URL = "https://api.anthropic.com/"
        private const val MODEL = "claude-sonnet-4-20250514"
    }

    private val api: AnthropicApi by lazy {
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
            .create(AnthropicApi::class.java)
    }

    override suspend fun validateApiKey(key: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = AnthropicMessageRequest(
                    model = "claude-3-5-haiku-20241022",
                    max_tokens = 5,
                    messages = listOf(AnthropicMessage(role = "user", content = "Hi"))
                )
                api.createMessage(key, request = request)
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
            val messages = buildMessages(conversationHistory, userMessage)
            callApi(systemPrompt, messages)
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
        conversationHistory: List<AIMessage>,
        userMessage: String
    ): List<AnthropicMessage> {
        val messages = mutableListOf<AnthropicMessage>()

        // Anthropic requires messages to start with user role
        // If first message is assistant, prepend a user message
        if (conversationHistory.isNotEmpty() && conversationHistory.first().role == AIMessageRole.ASSISTANT) {
            messages.add(AnthropicMessage(role = "user", content = "Please begin the interview."))
        }

        // Conversation history
        for (msg in conversationHistory) {
            val role = if (msg.role == AIMessageRole.ASSISTANT) "assistant" else "user"
            messages.add(AnthropicMessage(role = role, content = msg.content))
        }

        // Current user message
        messages.add(AnthropicMessage(role = "user", content = userMessage))

        return messages
    }

    private suspend fun callApi(
        systemPrompt: String,
        messages: List<AnthropicMessage>
    ): String {
        val request = AnthropicMessageRequest(
            model = MODEL,
            max_tokens = Config.GENERATION_MAX_OUTPUT_TOKENS,
            system = systemPrompt,
            messages = messages
        )

        val response = api.createMessage(apiKey, request = request)

        return response.content?.firstOrNull()
            ?.text?.trim()
            ?: throw Exception("No content in response")
    }
}
