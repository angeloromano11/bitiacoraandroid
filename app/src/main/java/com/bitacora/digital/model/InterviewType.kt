package com.bitacora.digital.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Interview session types.
 */
enum class InterviewType(val value: String, val displayName: String) {
    MEMORY("memory", "Memory"),
    WILL("will", "Will"),
    EXPERIENCE("experience", "Experience"),
    PRACTICE("practice", "Practice")
}

/**
 * Practice session subcategories.
 */
enum class PracticeSubcategory(val value: String) {
    JOB_INTERVIEW("job_interview"),
    PUBLIC_SPEAKING("public_speaking")
}

/**
 * Display information for interview types.
 */
data class InterviewTypeInfo(
    val type: InterviewType,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val subcategory: PracticeSubcategory? = null
)

/**
 * All available interview types for selection.
 */
val interviewTypes: List<InterviewTypeInfo> = listOf(
    InterviewTypeInfo(
        type = InterviewType.MEMORY,
        label = "Guided Memory",
        description = "Capture cherished memories with guided prompts",
        icon = Icons.Default.Star
    ),
    InterviewTypeInfo(
        type = InterviewType.WILL,
        label = "Will / Testament",
        description = "Record your wishes and legacy messages",
        icon = Icons.Default.Description
    ),
    InterviewTypeInfo(
        type = InterviewType.EXPERIENCE,
        label = "Life Experience",
        description = "Share significant life events and lessons",
        icon = Icons.Default.Bookmark
    ),
    InterviewTypeInfo(
        type = InterviewType.PRACTICE,
        label = "Practice - Job Interview",
        description = "Rehearse for upcoming job interviews",
        icon = Icons.Default.Work,
        subcategory = PracticeSubcategory.JOB_INTERVIEW
    ),
    InterviewTypeInfo(
        type = InterviewType.PRACTICE,
        label = "Practice - Public Speaking",
        description = "Improve your presentation skills",
        icon = Icons.Default.Campaign,
        subcategory = PracticeSubcategory.PUBLIC_SPEAKING
    )
)

/**
 * Get interview type info by type and subcategory.
 */
fun getInterviewTypeInfo(
    type: InterviewType,
    subcategory: PracticeSubcategory? = null
): InterviewTypeInfo? {
    return interviewTypes.find { it.type == type && it.subcategory == subcategory }
}
