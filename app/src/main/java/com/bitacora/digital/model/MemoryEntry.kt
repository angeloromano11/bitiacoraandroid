package com.bitacora.digital.model

import java.util.UUID

/**
 * Type of memory entry.
 */
enum class EntryType {
    QUESTION,
    RESPONSE,
    SUMMARY
}

/**
 * Extracted memory entry from recording sessions.
 */
data class MemoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val entryType: EntryType,
    val content: String,
    val timestamp: String,
    val topics: List<String> = emptyList(),
    val peopleMentioned: List<String> = emptyList(),
    val placesMentioned: List<String> = emptyList(),
    val emotions: List<String> = emptyList()
)

/**
 * Search result with relevance scoring.
 */
data class SearchResult(
    val entry: MemoryEntry,
    val relevanceScore: Double,
    val matchedTerms: List<String>,
    val matchType: MatchType
)

/**
 * Type of match in search results.
 */
enum class MatchType {
    CONTENT,
    TOPIC,
    PERSON,
    PLACE,
    EMOTION
}

/**
 * Memory statistics for the assistant.
 */
data class MemoryStatistics(
    val totalEntries: Int,
    val totalSessions: Int,
    val totalTopics: Int,
    val totalPeople: Int,
    val topTopics: List<String>,
    val topPeople: List<String>
)
