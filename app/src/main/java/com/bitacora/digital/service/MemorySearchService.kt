package com.bitacora.digital.service

import com.bitacora.digital.data.repository.MemoryRepository
import com.bitacora.digital.model.EntryType
import com.bitacora.digital.model.MatchType
import com.bitacora.digital.model.MemoryEntry
import com.bitacora.digital.model.SearchResult
import com.bitacora.digital.util.Config
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memory search service with relevance scoring.
 * Searches across content, topics, people, places, and emotions.
 */
@Singleton
class MemorySearchService @Inject constructor(
    private val memoryRepository: MemoryRepository
) {
    // Scoring weights
    companion object {
        private const val CONTENT_WEIGHT = 1.0
        private const val TOPIC_WEIGHT = 1.5
        private const val PERSON_WEIGHT = 2.0
        private const val PLACE_WEIGHT = 1.8
        private const val EMOTION_WEIGHT = 1.3
        private const val RESPONSE_BOOST = 1.1 // Responses scored higher than questions
    }

    /**
     * Search memory entries with relevance scoring.
     */
    suspend fun search(query: String, limit: Int = Config.MEMORY_SEARCH_LIMIT): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val allEntries = memoryRepository.getAllEntries()
        val terms = query.lowercase().split(Regex("\\s+")).filter { it.length > 2 }

        if (terms.isEmpty()) return emptyList()

        return allEntries
            .map { entry -> scoreEntry(entry, terms) }
            .filter { it.relevanceScore > 0 }
            .sortedByDescending { it.relevanceScore }
            .take(limit)
    }

    /**
     * Search by specific topic.
     */
    suspend fun searchByTopic(topic: String, limit: Int = 10): List<MemoryEntry> {
        val allEntries = memoryRepository.getAllEntries()
        return allEntries
            .filter { entry -> entry.topics.any { it.equals(topic, ignoreCase = true) } }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    /**
     * Search by person mentioned.
     */
    suspend fun searchByPerson(name: String, limit: Int = 10): List<MemoryEntry> {
        val allEntries = memoryRepository.getAllEntries()
        val lowerName = name.lowercase()
        return allEntries
            .filter { entry -> entry.peopleMentioned.any { it.lowercase().contains(lowerName) } }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    /**
     * Get recent entries.
     */
    suspend fun getRecent(limit: Int = 10): List<MemoryEntry> {
        return memoryRepository.getAllEntries()
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    /**
     * Get all unique topics.
     */
    suspend fun getAllTopics(): List<String> {
        return memoryRepository.getAllEntries()
            .flatMap { it.topics }
            .distinct()
            .sorted()
    }

    /**
     * Get all unique people mentioned.
     */
    suspend fun getAllPeople(): List<String> {
        return memoryRepository.getAllEntries()
            .flatMap { it.peopleMentioned }
            .distinct()
            .sorted()
    }

    /**
     * Get memory statistics.
     */
    suspend fun getStatistics(): MemoryStats {
        val entries = memoryRepository.getAllEntries()
        val topics = entries.flatMap { it.topics }.distinct()
        val people = entries.flatMap { it.peopleMentioned }.distinct()
        val places = entries.flatMap { it.placesMentioned }.distinct()
        val sessions = entries.map { it.sessionId }.distinct()

        // Get top topics
        val topTopics = entries
            .flatMap { it.topics }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }

        // Get top emotions
        val topEmotions = entries
            .flatMap { it.emotions }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }

        return MemoryStats(
            totalEntries = entries.size,
            totalSessions = sessions.size,
            uniqueTopics = topics.size,
            uniquePeople = people.size,
            uniquePlaces = places.size,
            topTopics = topTopics,
            topEmotions = topEmotions
        )
    }

    // MARK: - Private

    private fun scoreEntry(entry: MemoryEntry, terms: List<String>): SearchResult {
        var totalScore = 0.0
        val matchedTerms = mutableListOf<String>()
        var primaryMatchType = MatchType.CONTENT

        val lowerContent = entry.content.lowercase()

        for (term in terms) {
            // Content match
            if (lowerContent.contains(term)) {
                totalScore += CONTENT_WEIGHT
                matchedTerms.add(term)
                primaryMatchType = MatchType.CONTENT
            }

            // Topic match
            if (entry.topics.any { it.lowercase().contains(term) }) {
                totalScore += TOPIC_WEIGHT
                if (!matchedTerms.contains(term)) matchedTerms.add(term)
                if (primaryMatchType == MatchType.CONTENT) primaryMatchType = MatchType.TOPIC
            }

            // Person match
            if (entry.peopleMentioned.any { it.lowercase().contains(term) }) {
                totalScore += PERSON_WEIGHT
                if (!matchedTerms.contains(term)) matchedTerms.add(term)
                primaryMatchType = MatchType.PERSON
            }

            // Place match
            if (entry.placesMentioned.any { it.lowercase().contains(term) }) {
                totalScore += PLACE_WEIGHT
                if (!matchedTerms.contains(term)) matchedTerms.add(term)
                if (primaryMatchType != MatchType.PERSON) primaryMatchType = MatchType.PLACE
            }

            // Emotion match
            if (entry.emotions.any { it.lowercase().contains(term) }) {
                totalScore += EMOTION_WEIGHT
                if (!matchedTerms.contains(term)) matchedTerms.add(term)
                if (primaryMatchType == MatchType.CONTENT) primaryMatchType = MatchType.EMOTION
            }
        }

        // Apply response boost
        if (entry.entryType == EntryType.RESPONSE) {
            totalScore *= RESPONSE_BOOST
        }

        return SearchResult(
            entry = entry,
            relevanceScore = totalScore,
            matchedTerms = matchedTerms,
            matchType = primaryMatchType
        )
    }
}

/**
 * Memory statistics.
 */
data class MemoryStats(
    val totalEntries: Int,
    val totalSessions: Int,
    val uniqueTopics: Int,
    val uniquePeople: Int,
    val uniquePlaces: Int,
    val topTopics: List<String>,
    val topEmotions: List<String>
)
