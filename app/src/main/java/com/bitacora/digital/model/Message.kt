package com.bitacora.digital.model

import java.util.UUID

/**
 * Message role in conversation.
 */
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * Chat message for AI conversation.
 */
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: String,
    val audioDuration: Double? = null
)

/**
 * Interview conversation context.
 */
data class InterviewContext(
    val sessionId: String,
    val userName: String,
    val interviewType: InterviewType,
    val subcategory: PracticeSubcategory? = null,
    val questionsAsked: MutableList<String> = mutableListOf(),
    val messages: MutableList<Message> = mutableListOf(),
    var isActive: Boolean = true,
    val startedAt: String
)

/**
 * Chat message for memory assistant.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: String,
    val sourceCount: Int? = null
)

/**
 * Suggested question for memory assistant.
 */
data class SuggestedQuestion(
    val id: String = UUID.randomUUID().toString(),
    val text: String
)
