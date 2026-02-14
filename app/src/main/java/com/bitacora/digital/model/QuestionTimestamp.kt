package com.bitacora.digital.model

/**
 * Timestamp for when a question was asked during recording.
 */
data class QuestionTimestamp(
    val question: String,
    val timestamp: Double,
    val questionNumber: Int
)
