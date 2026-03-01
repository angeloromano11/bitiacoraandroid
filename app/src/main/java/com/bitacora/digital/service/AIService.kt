package com.bitacora.digital.service

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bitacora.digital.model.InterviewContext
import com.bitacora.digital.model.InterviewType
import com.bitacora.digital.model.Message
import com.bitacora.digital.model.MessageRole
import com.bitacora.digital.model.PracticeSubcategory
import com.bitacora.digital.service.providers.AIMessage
import com.bitacora.digital.service.providers.AIMessageRole
import com.bitacora.digital.service.providers.AIProvider
import com.bitacora.digital.service.providers.AIProviderFactory
import com.bitacora.digital.service.providers.AIProviderType
import com.bitacora.digital.service.providers.AudioNotSupportedException
import com.bitacora.digital.util.Config
import com.bitacora.digital.util.currentTimestamp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = Config.PREFERENCES_NAME)

/**
 * AI Service that orchestrates interview question generation using pluggable providers.
 */
@Singleton
class AIService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keychainHelper: KeychainHelper
) {
    companion object {
        private val ACTIVE_PROVIDER_PREF = stringPreferencesKey(Config.ACTIVE_PROVIDER_KEY)
    }

    private var currentProvider: AIProvider? = null
    private var activeProviderType: AIProviderType? = null
    private var interviewContext: InterviewContext? = null

    val isInitialized: Boolean get() = currentProvider != null
    val isReady: Boolean get() = currentProvider != null
    val currentProviderType: AIProviderType? get() = activeProviderType

    /**
     * Check if the current provider supports native audio input.
     */
    val supportsAudio: Boolean get() = currentProvider?.supportsAudio == true

    /**
     * Load the active provider and its API key from storage.
     */
    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            // Load active provider type from preferences
            val prefs = context.dataStore.data.first()
            val providerString = prefs[ACTIVE_PROVIDER_PREF] ?: Config.DEFAULT_PROVIDER
            val providerType = AIProviderType.fromString(providerString) ?: AIProviderType.GEMINI

            // Try to load API key for this provider
            val key = keychainHelper.load(providerType.keychainKey)
            if (key != null) {
                setProvider(providerType, key)
            } else {
                // Try legacy key location for Gemini
                if (providerType == AIProviderType.GEMINI) {
                    val legacyKey = keychainHelper.load(Config.KEYCHAIN_API_KEY)
                    if (legacyKey != null) {
                        setProvider(AIProviderType.GEMINI, legacyKey)
                    }
                }
            }
        }
    }

    // MARK: - Provider Management

    /**
     * Get list of all available providers with their configuration status.
     */
    fun getProviderStatuses(): List<Pair<AIProviderType, Boolean>> {
        return AIProviderType.entries.map { type ->
            val hasKey = keychainHelper.load(type.keychainKey) != null
            type to hasKey
        }
    }

    /**
     * Check if a specific provider is configured.
     */
    fun isProviderConfigured(type: AIProviderType): Boolean {
        return keychainHelper.load(type.keychainKey) != null
    }

    /**
     * Set the active provider with its API key.
     */
    private fun setProvider(type: AIProviderType, apiKey: String) {
        activeProviderType = type
        currentProvider = AIProviderFactory.create(type, apiKey)
    }

    /**
     * Switch to a different configured provider.
     */
    suspend fun switchProvider(type: AIProviderType): Boolean {
        return withContext(Dispatchers.IO) {
            val apiKey = keychainHelper.load(type.keychainKey) ?: return@withContext false
            setProvider(type, apiKey)

            // Save active provider preference
            context.dataStore.edit { prefs ->
                prefs[ACTIVE_PROVIDER_PREF] = type.name
            }
            true
        }
    }

    /**
     * Validate and save API key for a provider.
     */
    suspend fun setApiKey(key: String, type: AIProviderType): Boolean {
        return withContext(Dispatchers.IO) {
            val tempProvider = AIProviderFactory.create(type, key)
            if (tempProvider.validateApiKey(key)) {
                keychainHelper.save(type.keychainKey, key)

                // If this is the first configured provider or we're updating current, make it active
                if (currentProvider == null || activeProviderType == type) {
                    setProvider(type, key)
                    context.dataStore.edit { prefs ->
                        prefs[ACTIVE_PROVIDER_PREF] = type.name
                    }
                }
                true
            } else {
                false
            }
        }
    }

    /**
     * Clear API key for a specific provider.
     */
    suspend fun clearApiKey(type: AIProviderType) {
        withContext(Dispatchers.IO) {
            keychainHelper.delete(type.keychainKey)

            // If we cleared the active provider, try to switch to another
            if (activeProviderType == type) {
                currentProvider = null
                activeProviderType = null

                // Try to find another configured provider
                for (otherType in AIProviderType.entries) {
                    if (otherType != type && switchProvider(otherType)) {
                        break
                    }
                }
            }
        }
    }

    /**
     * Validate an API key for a specific provider.
     */
    suspend fun validateApiKey(key: String, type: AIProviderType): Boolean {
        return withContext(Dispatchers.IO) {
            val tempProvider = AIProviderFactory.create(type, key)
            tempProvider.validateApiKey(key)
        }
    }

    // Legacy compatibility
    suspend fun setApiKey(key: String): Boolean = setApiKey(key, AIProviderType.GEMINI)
    fun clearApiKey() {
        keychainHelper.delete(Config.KEYCHAIN_API_KEY)
        AIProviderType.entries.forEach { keychainHelper.delete(it.keychainKey) }
        currentProvider = null
        activeProviderType = null
    }

    // MARK: - Interview Lifecycle

    /**
     * Start a new interview session and return the opening question.
     */
    fun startInterview(
        sessionId: String,
        type: InterviewType,
        userName: String = "",
        subcategory: PracticeSubcategory? = null
    ): String {
        interviewContext = InterviewContext(
            sessionId = sessionId,
            userName = userName,
            interviewType = type,
            subcategory = subcategory,
            startedAt = currentTimestamp()
        )

        val opening = getOpeningQuestion(type, subcategory, userName)
        interviewContext?.questionsAsked?.add(opening)
        interviewContext?.messages?.add(
            Message(
                role = MessageRole.ASSISTANT,
                content = opening,
                timestamp = currentTimestamp()
            )
        )

        return opening
    }

    /**
     * Generate a follow-up question from the user's text response.
     */
    suspend fun generateFollowUp(text: String): String {
        val provider = currentProvider ?: return fallbackQuestion()
        val ctx = interviewContext ?: return fallbackQuestion()

        return withContext(Dispatchers.IO) {
            try {
                val systemPrompt = getSystemPrompt(ctx.interviewType, ctx.subcategory)
                val history = buildConversationHistory()

                val question = provider.generateResponse(systemPrompt, history, text)

                interviewContext?.messages?.add(
                    Message(role = MessageRole.USER, content = text, timestamp = currentTimestamp())
                )
                interviewContext?.messages?.add(
                    Message(role = MessageRole.ASSISTANT, content = question, timestamp = currentTimestamp())
                )
                interviewContext?.questionsAsked?.add(question)

                question
            } catch (e: Exception) {
                fallbackQuestion()
            }
        }
    }

    /**
     * Generate a follow-up question from audio data (base64 WAV).
     */
    suspend fun generateFollowUpFromAudio(audioBase64: String): String {
        val provider = currentProvider ?: return fallbackQuestion()
        val ctx = interviewContext ?: return fallbackQuestion()

        return withContext(Dispatchers.IO) {
            try {
                if (!provider.supportsAudio) {
                    throw AudioNotSupportedException(provider.providerType)
                }

                val systemPrompt = getSystemPrompt(ctx.interviewType, ctx.subcategory)
                val history = buildConversationHistory()

                val question = provider.generateResponseFromAudio(systemPrompt, history, audioBase64)

                interviewContext?.messages?.add(
                    Message(role = MessageRole.USER, content = "[Audio response]", timestamp = currentTimestamp())
                )
                interviewContext?.messages?.add(
                    Message(role = MessageRole.ASSISTANT, content = question, timestamp = currentTimestamp())
                )
                interviewContext?.questionsAsked?.add(question)

                question
            } catch (e: AudioNotSupportedException) {
                // For non-audio providers, caller should use speech-to-text first
                throw e
            } catch (e: Exception) {
                fallbackQuestion()
            }
        }
    }

    /**
     * Generate a follow-up using text fallback for non-audio providers.
     */
    suspend fun generateFollowUpWithTextFallback(transcribedText: String): String {
        return generateFollowUp(transcribedText)
    }

    /**
     * End the current interview and return a closing message.
     */
    fun endInterview(): String {
        val ctx = interviewContext ?: return "Thank you for sharing."
        val closing = getClosingMessage(ctx.interviewType)
        interviewContext = null
        return closing
    }

    /**
     * Get the number of questions asked in the current session.
     */
    val questionsCount: Int get() = interviewContext?.questionsAsked?.size ?: 0

    // MARK: - Private: Conversation History

    private fun buildConversationHistory(): List<AIMessage> {
        val ctx = interviewContext ?: return emptyList()
        return ctx.messages.map { msg ->
            AIMessage(
                role = if (msg.role == MessageRole.ASSISTANT) AIMessageRole.ASSISTANT else AIMessageRole.USER,
                content = msg.content
            )
        }
    }

    // MARK: - Private: Prompts

    private fun getSystemPrompt(type: InterviewType, subcategory: PracticeSubcategory?): String {
        if (type == InterviewType.PRACTICE && subcategory != null) {
            return when (subcategory) {
                PracticeSubcategory.JOB_INTERVIEW ->
                    "You are a professional interview coach conducting realistic mock interviews. You ask behavioral, situational, and role-specific questions, adapting difficulty based on responses. Provide brief feedback when appropriate. Ask one question at a time."
                PracticeSubcategory.PUBLIC_SPEAKING ->
                    "You are an expert speech coach helping refine presentations. You prompt practice of key segments, assess clarity and structure, and offer actionable feedback on delivery and engagement. Give one prompt or question at a time."
            }
        }

        return when (type) {
            InterviewType.MEMORY ->
                "You are an expert memory interviewer skilled at asking open-ended, insightful questions. You follow emotional cues, clarify unclear moments, and draw out sensory details—sights, sounds, smells, and feelings—to capture the full richness of the memory. Ask one short question at a time."
            InterviewType.WILL ->
                "You are a compassionate legacy guide helping someone leave meaningful messages for loved ones. You gently prompt for heartfelt words, life lessons, and unspoken feelings with dignity and patience. Ask one short, gentle question at a time."
            InterviewType.EXPERIENCE ->
                "You are an expert story interviewer skilled at asking open-ended, insightful questions. You follow emotional cues, clarify unclear moments, and draw out depth, meaning, and authenticity from the narrator's experience. Ask one short question at a time."
            InterviewType.PRACTICE ->
                "You are a professional interview coach conducting realistic mock interviews. You ask behavioral, situational, and role-specific questions, adapting difficulty based on responses. Provide brief feedback when appropriate. Ask one question at a time."
        }
    }

    private fun getOpeningQuestion(
        type: InterviewType,
        subcategory: PracticeSubcategory?,
        userName: String
    ): String {
        val prefix = if (userName.isNotEmpty()) "$userName, " else ""

        if (type == InterviewType.PRACTICE && subcategory != null) {
            return when (subcategory) {
                PracticeSubcategory.JOB_INTERVIEW -> "${prefix}What position are you interviewing for?"
                PracticeSubcategory.PUBLIC_SPEAKING -> "${prefix}What's your presentation topic?"
            }
        }

        return when (type) {
            InterviewType.MEMORY -> "${prefix}What memory would you like to capture today?"
            InterviewType.WILL -> "${prefix}Who would you like to record a message for?"
            InterviewType.EXPERIENCE -> "${prefix}What experience would you like to share?"
            InterviewType.PRACTICE -> "${prefix}What position are you interviewing for?"
        }
    }

    private fun getClosingMessage(type: InterviewType): String {
        return when (type) {
            InterviewType.MEMORY -> "Thank you for sharing this precious memory. It has been beautifully captured."
            InterviewType.WILL -> "Thank you for these heartfelt messages. They will be treasured."
            InterviewType.EXPERIENCE -> "Thank you for sharing your story. It was a pleasure to hear."
            InterviewType.PRACTICE -> "Great practice session! Keep up the good work."
        }
    }

    private fun fallbackQuestion(): String {
        val fallbacks = listOf(
            "Can you tell me more about that?",
            "How did that make you feel?",
            "What happened next?",
            "Why was that significant to you?",
            "Who else was involved in that moment?"
        )
        return fallbacks.random()
    }
}
