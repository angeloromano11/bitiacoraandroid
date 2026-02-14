package com.bitacora.digital.service

import com.bitacora.digital.model.InterviewContext
import com.bitacora.digital.model.InterviewType
import com.bitacora.digital.model.Message
import com.bitacora.digital.model.MessageRole
import com.bitacora.digital.model.PracticeSubcategory
import com.bitacora.digital.service.api.Content
import com.bitacora.digital.service.api.GenerateContentRequest
import com.bitacora.digital.service.api.GenerationConfig
import com.bitacora.digital.service.api.GeminiApi
import com.bitacora.digital.service.api.InlineData
import com.bitacora.digital.service.api.Part
import com.bitacora.digital.util.Config
import com.bitacora.digital.util.currentTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini API integration for AI-guided interview question generation.
 */
@Singleton
class AIService @Inject constructor(
    private val keychainHelper: KeychainHelper
) {
    private var apiKey: String? = null
    private var context: InterviewContext? = null

    val isInitialized: Boolean get() = apiKey != null
    val isReady: Boolean get() = apiKey != null

    private val geminiApi: GeminiApi by lazy {
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

    /**
     * Load API key from secure storage.
     */
    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            apiKey = keychainHelper.load(Config.KEYCHAIN_API_KEY)
        }
    }

    /**
     * Validate and save API key.
     */
    suspend fun setApiKey(key: String): Boolean {
        return withContext(Dispatchers.IO) {
            if (validateApiKey(key)) {
                keychainHelper.save(Config.KEYCHAIN_API_KEY, key)
                apiKey = key
                true
            } else {
                false
            }
        }
    }

    /**
     * Clear API key from storage.
     */
    fun clearApiKey() {
        keychainHelper.delete(Config.KEYCHAIN_API_KEY)
        apiKey = null
    }

    /**
     * Test an API key with a minimal request.
     */
    suspend fun validateApiKey(key: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = "Hi")))),
                    generationConfig = GenerationConfig(maxOutputTokens = 5)
                )
                geminiApi.generateContent(key, request)
                true
            } catch (e: Exception) {
                // Check if it's a rate limit (key is valid but rate limited)
                e.message?.contains("429") == true
            }
        }
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
        context = InterviewContext(
            sessionId = sessionId,
            userName = userName,
            interviewType = type,
            subcategory = subcategory,
            startedAt = currentTimestamp()
        )

        val opening = getOpeningQuestion(type, subcategory, userName)
        context?.questionsAsked?.add(opening)
        context?.messages?.add(
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
        val key = apiKey ?: return fallbackQuestion()
        val ctx = context ?: return fallbackQuestion()

        return withContext(Dispatchers.IO) {
            try {
                val systemPrompt = getSystemPrompt(ctx.interviewType, ctx.subcategory)
                val history = formatConversationHistory()
                val prompt = """
                    $systemPrompt

                    Conversation so far:
                    $history

                    User's response: $text

                    Generate the next question:
                """.trimIndent()

                val question = callGeminiAPI(key, prompt)

                context?.messages?.add(
                    Message(role = MessageRole.USER, content = text, timestamp = currentTimestamp())
                )
                context?.messages?.add(
                    Message(role = MessageRole.ASSISTANT, content = question, timestamp = currentTimestamp())
                )
                context?.questionsAsked?.add(question)

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
        val key = apiKey ?: return fallbackQuestion()
        val ctx = context ?: return fallbackQuestion()

        return withContext(Dispatchers.IO) {
            try {
                val systemPrompt = getSystemPrompt(ctx.interviewType, ctx.subcategory)
                val history = formatConversationHistory()
                val prompt = """
                    $systemPrompt

                    Conversation so far:
                    $history

                    Generate the next question based on the user's audio response:
                """.trimIndent()

                val question = callGeminiAPI(key, prompt, audioBase64)

                context?.messages?.add(
                    Message(role = MessageRole.USER, content = "[Audio response]", timestamp = currentTimestamp())
                )
                context?.messages?.add(
                    Message(role = MessageRole.ASSISTANT, content = question, timestamp = currentTimestamp())
                )
                context?.questionsAsked?.add(question)

                question
            } catch (e: Exception) {
                fallbackQuestion()
            }
        }
    }

    /**
     * End the current interview and return a closing message.
     */
    fun endInterview(): String {
        val ctx = context ?: return "Thank you for sharing."
        val closing = getClosingMessage(ctx.interviewType)
        context = null
        return closing
    }

    /**
     * Get the number of questions asked in the current session.
     */
    val questionsCount: Int get() = context?.questionsAsked?.size ?: 0

    // MARK: - Private: API Call

    private suspend fun callGeminiAPI(
        apiKey: String,
        prompt: String,
        audioBase64: String? = null
    ): String {
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

        val response = geminiApi.generateContent(apiKey, request)

        val text = response.candidates?.firstOrNull()
            ?.content?.parts?.firstOrNull()
            ?.text

        return text?.trim() ?: throw Exception("No content in response")
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

    private fun formatConversationHistory(): String {
        val ctx = context ?: return ""
        return ctx.messages.joinToString("\n") { msg ->
            val role = if (msg.role == MessageRole.ASSISTANT) "Interviewer" else "User"
            "$role: ${msg.content}"
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
