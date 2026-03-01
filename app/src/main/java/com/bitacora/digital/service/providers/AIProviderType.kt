package com.bitacora.digital.service.providers

/**
 * Available AI provider types.
 */
enum class AIProviderType(
    val displayName: String,
    val description: String,
    val supportsAudio: Boolean,
    val apiKeyUrl: String,
    val keychainKey: String,
    val icon: String
) {
    GEMINI(
        displayName = "Google Gemini",
        description = "Native audio support",
        supportsAudio = true,
        apiKeyUrl = "https://aistudio.google.com/apikey",
        keychainKey = "gemini_api_key",
        icon = "sparkles"
    ),
    OPENAI(
        displayName = "OpenAI",
        description = "GPT-4o with Whisper",
        supportsAudio = false,
        apiKeyUrl = "https://platform.openai.com/api-keys",
        keychainKey = "openai_api_key",
        icon = "robot"
    ),
    ANTHROPIC(
        displayName = "Anthropic Claude",
        description = "Thoughtful responses",
        supportsAudio = false,
        apiKeyUrl = "https://console.anthropic.com/settings/keys",
        keychainKey = "anthropic_api_key",
        icon = "brain"
    ),
    MISTRAL(
        displayName = "Mistral AI",
        description = "European hosting",
        supportsAudio = false,
        apiKeyUrl = "https://console.mistral.ai/api-keys",
        keychainKey = "mistral_api_key",
        icon = "wind"
    );

    companion object {
        fun fromString(value: String): AIProviderType? {
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }
    }
}
