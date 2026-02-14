package com.bitacora.digital.util

/**
 * Centralized app configuration constants.
 */
object Config {
    // App
    const val APP_NAME = "Bitacora Digital"
    const val APP_VERSION = "1.0.0"

    // API
    const val GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/"
    const val GEMINI_MODEL = "gemini-2.0-flash"
    const val API_TIMEOUT_SECONDS = 30L

    // Generation Config
    const val GENERATION_TEMPERATURE = 0.7
    const val GENERATION_TOP_P = 0.9
    const val GENERATION_TOP_K = 40
    const val GENERATION_MAX_OUTPUT_TOKENS = 200

    // Recording
    const val AUDIO_SAMPLE_RATE = 44100
    const val AUDIO_CHANNELS = 1
    const val RING_BUFFER_SECONDS = 60
    const val AI_AUDIO_CONTEXT_SECONDS = 30
    const val AUTO_QUESTION_DEFAULT_INTERVAL = 60

    // Memory
    const val MEMORY_SEARCH_LIMIT = 15
    const val MEMORY_ASSISTANT_MAX_TOKENS = 500

    // Storage
    const val DATABASE_NAME = "bitacora.db"
    const val SESSIONS_DIR = "sessions"
    const val THUMBNAILS_DIR = "thumbnails"

    // Keychain
    const val SECURE_PREFS_NAME = "bitacora_secure_prefs"
    const val KEYCHAIN_API_KEY = "gemini_api_key"

    // DataStore
    const val PREFERENCES_NAME = "bitacora_preferences"
    const val ONBOARDING_COMPLETE_KEY = "has_onboarded"
    const val AUTO_QUESTIONS_ENABLED_KEY = "auto_questions_enabled"
    const val QUESTION_INTERVAL_KEY = "question_interval"
}
