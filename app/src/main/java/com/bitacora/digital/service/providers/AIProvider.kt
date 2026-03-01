package com.bitacora.digital.service.providers

/**
 * Interface for AI providers that generate interview questions.
 */
interface AIProvider {
    /**
     * The type of this provider.
     */
    val providerType: AIProviderType

    /**
     * Whether this provider supports native audio input.
     */
    val supportsAudio: Boolean

    /**
     * Validate an API key with a minimal request.
     */
    suspend fun validateApiKey(key: String): Boolean

    /**
     * Generate a response from text input.
     */
    suspend fun generateResponse(
        systemPrompt: String,
        conversationHistory: List<AIMessage>,
        userMessage: String
    ): String

    /**
     * Generate a response from audio input (base64 encoded).
     * Throws UnsupportedOperationException if provider doesn't support audio.
     */
    suspend fun generateResponseFromAudio(
        systemPrompt: String,
        conversationHistory: List<AIMessage>,
        audioBase64: String
    ): String
}

/**
 * Message in conversation history.
 */
data class AIMessage(
    val role: AIMessageRole,
    val content: String
)

/**
 * Role of a message in conversation.
 */
enum class AIMessageRole {
    USER,
    ASSISTANT
}

/**
 * Exception thrown when provider doesn't support audio input.
 */
class AudioNotSupportedException(
    provider: AIProviderType
) : Exception("${provider.displayName} does not support native audio input")

/**
 * Exception thrown when API key is invalid.
 */
class InvalidApiKeyException(
    provider: AIProviderType
) : Exception("Invalid API key for ${provider.displayName}")

/**
 * Exception thrown when rate limited.
 */
class RateLimitedException(
    provider: AIProviderType
) : Exception("Rate limit exceeded for ${provider.displayName}")
