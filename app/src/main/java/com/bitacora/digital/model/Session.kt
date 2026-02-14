package com.bitacora.digital.model

import com.bitacora.digital.util.formatDuration
import com.bitacora.digital.util.formatRelativeDate

/**
 * Recorded interview session.
 */
data class Session(
    val id: String,
    val filename: String,
    val createdAt: String,
    val durationSeconds: Int,
    val interviewType: InterviewType,
    val subcategory: PracticeSubcategory? = null,
    val userName: String = "",
    val questionsCount: Int = 0,
    val tags: List<String> = emptyList(),
    val collection: String = "",
    val notes: String = "",
    val encrypted: Boolean = false,
    val thumbnailPath: String? = null,
    val summary: String? = null,
    val keyTopics: List<String>? = null
) {
    /**
     * Formatted duration string (MM:SS or HH:MM:SS).
     */
    val formattedDuration: String
        get() = durationSeconds.formatDuration()

    /**
     * Formatted relative date (Today, Yesterday, day name, or date).
     */
    val formattedDate: String
        get() = createdAt.formatRelativeDate()

    /**
     * Get display name for the interview type.
     */
    val typeDisplayName: String
        get() = getInterviewTypeInfo(interviewType, subcategory)?.label
            ?: interviewType.value.replaceFirstChar { it.uppercase() }

    companion object {
        /**
         * Create a new session with generated timestamp.
         */
        fun create(
            id: String,
            filename: String,
            durationSeconds: Int,
            interviewType: InterviewType,
            subcategory: PracticeSubcategory? = null,
            userName: String = "",
            questionsCount: Int = 0
        ): Session {
            val now = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                java.util.Locale.US
            ).format(java.util.Date())

            return Session(
                id = id,
                filename = filename,
                createdAt = now,
                durationSeconds = durationSeconds,
                interviewType = interviewType,
                subcategory = subcategory,
                userName = userName,
                questionsCount = questionsCount
            )
        }
    }
}
