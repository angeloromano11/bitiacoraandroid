package com.bitacora.digital.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Extension functions for common formatting operations.
 */

/**
 * Format duration in seconds to "MM:SS" or "HH:MM:SS" string.
 */
fun Int.formatDuration(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60

    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

/**
 * Format bytes to human-readable string (e.g., "1.2 MB").
 */
fun Long.formatBytes(): String {
    if (this < 1024) return "$this B"
    val kb = this / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}

/**
 * Format ISO 8601 date string to relative date (Today, Yesterday, day name, or date).
 */
fun String.formatRelativeDate(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    val date = try {
        dateFormat.parse(this)
    } catch (e: Exception) {
        return this
    } ?: return this

    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { time = date }

    val isToday = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)

    val isYesterday = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR) == 1

    val daysDiff = TimeUnit.MILLISECONDS.toDays(now.timeInMillis - then.timeInMillis)

    return when {
        isToday -> "Today"
        isYesterday -> "Yesterday"
        daysDiff < 7 -> SimpleDateFormat("EEEE", Locale.US).format(date)
        else -> SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
    }
}

/**
 * Get current ISO 8601 timestamp.
 */
fun currentTimestamp(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    return dateFormat.format(Date())
}

/**
 * Generate a session ID based on current timestamp.
 */
fun generateSessionId(): String {
    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    return dateFormat.format(Date())
}
