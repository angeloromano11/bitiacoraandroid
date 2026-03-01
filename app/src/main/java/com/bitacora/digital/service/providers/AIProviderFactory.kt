package com.bitacora.digital.service.providers

/**
 * Factory for creating AI provider instances.
 */
object AIProviderFactory {

    /**
     * Create a provider instance for the given type and API key.
     */
    fun create(type: AIProviderType, apiKey: String): AIProvider {
        return when (type) {
            AIProviderType.GEMINI -> GeminiProvider(apiKey)
            AIProviderType.OPENAI -> OpenAIProvider(apiKey)
            AIProviderType.ANTHROPIC -> AnthropicProvider(apiKey)
            AIProviderType.MISTRAL -> MistralProvider(apiKey)
        }
    }
}
