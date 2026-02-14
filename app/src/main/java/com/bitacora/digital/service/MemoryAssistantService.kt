package com.bitacora.digital.service

import com.bitacora.digital.model.ChatMessage
import com.bitacora.digital.model.MessageRole
import com.bitacora.digital.model.SearchResult
import com.bitacora.digital.model.SuggestedQuestion
import com.bitacora.digital.service.api.Content
import com.bitacora.digital.service.api.GenerateContentRequest
import com.bitacora.digital.service.api.GenerationConfig
import com.bitacora.digital.service.api.GeminiApi
import com.bitacora.digital.service.api.Part
import com.bitacora.digital.util.Config
import com.bitacora.digital.util.currentTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RAG-based memory assistant service.
 * Retrieves relevant memories and uses Gemini to generate contextual responses.
 */
@Singleton
class MemoryAssistantService @Inject constructor(
    private val searchService: MemorySearchService,
    private val keychainHelper: KeychainHelper
) {
    private val chatHistory = mutableListOf<ChatMessage>()
    private var apiKey: String? = null

    val isInitialized: Boolean get() = apiKey != null

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
     * Initialize the service by loading API key.
     */
    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            apiKey = keychainHelper.load(Config.KEYCHAIN_API_KEY)
        }
    }

    /**
     * Ask a question and get a response based on stored memories.
     */
    suspend fun askQuestion(question: String): ChatResponse {
        val key = apiKey ?: throw IllegalStateException("API key not configured")

        return withContext(Dispatchers.IO) {
            // Add user message to history
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = MessageRole.USER,
                content = question,
                timestamp = currentTimestamp()
            )
            chatHistory.add(userMessage)

            // Search for relevant memories
            val searchResults = searchService.search(question, limit = 10)

            // Build RAG prompt
            val prompt = buildRAGPrompt(question, searchResults)

            // Call Gemini API
            val response = callGeminiAPI(key, prompt)

            // Create assistant message
            val assistantMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = MessageRole.ASSISTANT,
                content = response,
                timestamp = currentTimestamp(),
                sourceCount = searchResults.size
            )
            chatHistory.add(assistantMessage)

            ChatResponse(
                message = assistantMessage,
                sourceSessions = searchResults.map { it.entry.sessionId }.distinct().size
            )
        }
    }

    /**
     * Get suggested questions based on stored memories.
     */
    suspend fun getSuggestedQuestions(): List<SuggestedQuestion> {
        return withContext(Dispatchers.IO) {
            val stats = searchService.getStatistics()
            generateSuggestions(stats)
        }
    }

    /**
     * Get chat history.
     */
    fun getChatHistory(): List<ChatMessage> = chatHistory.toList()

    /**
     * Clear chat history.
     */
    fun clearHistory() {
        chatHistory.clear()
    }

    // MARK: - Private

    private fun buildRAGPrompt(question: String, context: List<SearchResult>): String {
        val contextText = if (context.isNotEmpty()) {
            val memories = context.mapIndexed { index, result ->
                val entry = result.entry
                val metadata = buildList {
                    if (entry.topics.isNotEmpty()) add("Topics: ${entry.topics.joinToString(", ")}")
                    if (entry.peopleMentioned.isNotEmpty()) add("People: ${entry.peopleMentioned.joinToString(", ")}")
                    if (entry.emotions.isNotEmpty()) add("Emotions: ${entry.emotions.joinToString(", ")}")
                }.joinToString(" | ")

                """
                [Memory ${index + 1}]
                ${entry.content}
                ${if (metadata.isNotEmpty()) "($metadata)" else ""}
                """.trimIndent()
            }.joinToString("\n\n")

            """
            You have access to the following memories from the user's recorded sessions:

            $memories

            Based on these memories, answer the user's question naturally and conversationally.
            Reference specific details from the memories when relevant.
            If the memories don't contain relevant information, acknowledge that and offer to help with what you know.
            """.trimIndent()
        } else {
            """
            The user hasn't recorded any memories yet, or no memories match their question.
            Politely explain that you don't have any recorded memories to reference.
            Encourage them to record some sessions to build their memory collection.
            """.trimIndent()
        }

        val recentHistory = chatHistory.takeLast(6).joinToString("\n") { msg ->
            val role = if (msg.role == MessageRole.USER) "User" else "Assistant"
            "$role: ${msg.content}"
        }

        return """
            You are a helpful memory assistant for Bitácora Digital, an app that helps people capture and recall their life memories.

            $contextText

            ${if (recentHistory.isNotEmpty()) "Recent conversation:\n$recentHistory\n" else ""}

            User's question: $question

            Respond warmly and helpfully. Keep your response concise but meaningful.
        """.trimIndent()
    }

    private suspend fun callGeminiAPI(apiKey: String, prompt: String): String {
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                temperature = 0.7,
                topP = 0.9,
                topK = 40,
                maxOutputTokens = Config.MEMORY_ASSISTANT_MAX_TOKENS
            )
        )

        val response = geminiApi.generateContent(apiKey, request)

        return response.candidates?.firstOrNull()
            ?.content?.parts?.firstOrNull()
            ?.text?.trim()
            ?: "I'm sorry, I couldn't generate a response. Please try again."
    }

    private fun generateSuggestions(stats: MemoryStats): List<SuggestedQuestion> {
        val suggestions = mutableListOf<SuggestedQuestion>()

        // Topic-based suggestions
        stats.topTopics.take(2).forEach { topic ->
            suggestions.add(
                SuggestedQuestion(
                    id = UUID.randomUUID().toString(),
                    text = "What do I remember about $topic?"
                )
            )
        }

        // Emotion-based suggestions
        stats.topEmotions.firstOrNull()?.let { emotion ->
            suggestions.add(
                SuggestedQuestion(
                    id = UUID.randomUUID().toString(),
                    text = "When have I felt $emotion?"
                )
            )
        }

        // General suggestions if not enough specific ones
        if (suggestions.size < 3) {
            val generalSuggestions = listOf(
                "What are my earliest memories?",
                "Tell me about my family",
                "What achievements am I proud of?",
                "What life lessons have I shared?",
                "What places are meaningful to me?"
            )

            generalSuggestions.shuffled().take(3 - suggestions.size).forEach {
                suggestions.add(
                    SuggestedQuestion(
                        id = UUID.randomUUID().toString(),
                        text = it
                    )
                )
            }
        }

        return suggestions.take(4)
    }
}

/**
 * Response from the memory assistant.
 */
data class ChatResponse(
    val message: ChatMessage,
    val sourceSessions: Int
)
