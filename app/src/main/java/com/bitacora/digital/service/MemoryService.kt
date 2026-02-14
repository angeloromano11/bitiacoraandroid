package com.bitacora.digital.service

import com.bitacora.digital.data.repository.MemoryRepository
import com.bitacora.digital.model.EntryType
import com.bitacora.digital.model.MemoryEntry
import com.bitacora.digital.util.currentTimestamp
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for extracting and storing memory entries from interview sessions.
 * Performs keyword-based extraction of topics, emotions, people, and places.
 */
@Singleton
class MemoryService @Inject constructor(
    private val memoryRepository: MemoryRepository
) {
    // Topic keywords for extraction
    private val topicKeywords = mapOf(
        "family" to listOf("family", "mother", "father", "parent", "sibling", "brother", "sister", "child", "children", "son", "daughter", "grandparent", "grandmother", "grandfather", "aunt", "uncle", "cousin"),
        "childhood" to listOf("childhood", "growing up", "young", "kid", "school", "playground", "toy", "game"),
        "career" to listOf("job", "work", "career", "office", "business", "company", "boss", "colleague", "profession", "employee"),
        "education" to listOf("school", "university", "college", "teacher", "student", "learn", "study", "graduate", "degree", "class"),
        "travel" to listOf("travel", "trip", "vacation", "journey", "visit", "explore", "adventure", "flight", "destination"),
        "health" to listOf("health", "hospital", "doctor", "sick", "illness", "medicine", "recovery", "surgery", "treatment"),
        "relationships" to listOf("friend", "friendship", "love", "relationship", "partner", "spouse", "husband", "wife", "dating", "marriage", "wedding"),
        "achievements" to listOf("achieve", "success", "accomplish", "award", "win", "proud", "milestone", "goal", "dream"),
        "challenges" to listOf("challenge", "difficult", "struggle", "overcome", "problem", "obstacle", "hardship", "tough"),
        "hobbies" to listOf("hobby", "passion", "interest", "enjoy", "fun", "sport", "music", "art", "reading", "cooking"),
        "home" to listOf("home", "house", "apartment", "room", "neighborhood", "move", "live", "living"),
        "holidays" to listOf("holiday", "christmas", "thanksgiving", "birthday", "celebration", "party", "tradition", "festival"),
        "pets" to listOf("pet", "dog", "cat", "animal", "puppy", "kitten"),
        "food" to listOf("food", "meal", "dinner", "lunch", "breakfast", "cook", "recipe", "restaurant", "favorite dish")
    )

    // Emotion keywords
    private val emotionKeywords = mapOf(
        "happy" to listOf("happy", "joy", "joyful", "excited", "thrilled", "delighted", "pleased", "wonderful", "amazing", "great"),
        "sad" to listOf("sad", "unhappy", "depressed", "down", "upset", "disappointed", "heartbroken", "grief", "loss", "miss"),
        "grateful" to listOf("grateful", "thankful", "appreciate", "blessed", "fortunate", "lucky"),
        "proud" to listOf("proud", "accomplished", "achievement", "success", "confident"),
        "nostalgic" to listOf("nostalgic", "remember", "memories", "back then", "those days", "used to"),
        "anxious" to listOf("anxious", "nervous", "worried", "stress", "fear", "scared", "afraid"),
        "loved" to listOf("love", "loved", "care", "cherish", "adore", "affection"),
        "hopeful" to listOf("hope", "hopeful", "optimistic", "looking forward", "future", "dream"),
        "peaceful" to listOf("peace", "peaceful", "calm", "relaxed", "serene", "tranquil")
    )

    // People patterns
    private val peoplePatterns = listOf(
        Regex("\\b(my|our)\\s+(mother|father|mom|dad|brother|sister|son|daughter|grandmother|grandfather|grandma|grandpa|aunt|uncle|cousin|wife|husband|partner|friend|boss|teacher|coach)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b[A-Z][a-z]+\\s+[A-Z][a-z]+\\b"), // Full names (capitalized)
        Regex("\\b(Mr|Mrs|Ms|Dr|Prof)\\.?\\s+[A-Z][a-z]+\\b") // Titles with names
    )

    // Place patterns
    private val placePatterns = listOf(
        Regex("\\b(in|at|from|to|near)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)\\b"),
        Regex("\\b[A-Z][a-z]+,\\s*[A-Z]{2}\\b"), // City, State
        Regex("\\b(home|school|university|college|hospital|church|office|park|beach|mountain|city|town|village|country)\\b", RegexOption.IGNORE_CASE)
    )

    /**
     * Save a question as a memory entry.
     */
    suspend fun saveQuestion(sessionId: String, question: String) {
        val entry = MemoryEntry(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            entryType = EntryType.QUESTION,
            content = question,
            timestamp = currentTimestamp(),
            topics = emptyList(),
            peopleMentioned = emptyList(),
            placesMentioned = emptyList(),
            emotions = emptyList()
        )
        memoryRepository.saveEntry(entry)
    }

    /**
     * Save a response as a memory entry with extracted metadata.
     */
    suspend fun saveResponse(sessionId: String, response: String) {
        val topics = extractTopics(response)
        val emotions = extractEmotions(response)
        val people = extractPeople(response)
        val places = extractPlaces(response)

        val entry = MemoryEntry(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            entryType = EntryType.RESPONSE,
            content = response,
            timestamp = currentTimestamp(),
            topics = topics,
            peopleMentioned = people,
            placesMentioned = places,
            emotions = emotions
        )
        memoryRepository.saveEntry(entry)
    }

    /**
     * Save a session summary.
     */
    suspend fun saveSessionSummary(sessionId: String, summary: String, topics: List<String>) {
        val entry = MemoryEntry(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            entryType = EntryType.SUMMARY,
            content = summary,
            timestamp = currentTimestamp(),
            topics = topics,
            peopleMentioned = emptyList(),
            placesMentioned = emptyList(),
            emotions = emptyList()
        )
        memoryRepository.saveEntry(entry)
    }

    /**
     * Get all entries for a session.
     */
    suspend fun getSessionEntries(sessionId: String): List<MemoryEntry> {
        return memoryRepository.getSessionEntries(sessionId)
    }

    /**
     * Generate topics from session entries.
     */
    suspend fun generateSessionTopics(sessionId: String): List<String> {
        val entries = memoryRepository.getSessionEntries(sessionId)
        val allTopics = entries.flatMap { it.topics }
        return allTopics.groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }

    // MARK: - Private Extraction Methods

    private fun extractTopics(text: String): List<String> {
        val lowerText = text.lowercase()
        val foundTopics = mutableSetOf<String>()

        topicKeywords.forEach { (topic, keywords) ->
            if (keywords.any { keyword -> lowerText.contains(keyword) }) {
                foundTopics.add(topic)
            }
        }

        return foundTopics.toList()
    }

    private fun extractEmotions(text: String): List<String> {
        val lowerText = text.lowercase()
        val foundEmotions = mutableSetOf<String>()

        emotionKeywords.forEach { (emotion, keywords) ->
            if (keywords.any { keyword -> lowerText.contains(keyword) }) {
                foundEmotions.add(emotion)
            }
        }

        return foundEmotions.toList()
    }

    private fun extractPeople(text: String): List<String> {
        val foundPeople = mutableSetOf<String>()

        peoplePatterns.forEach { pattern ->
            pattern.findAll(text).forEach { match ->
                val person = match.value.trim()
                if (person.length > 2) {
                    foundPeople.add(person)
                }
            }
        }

        return foundPeople.toList().take(10)
    }

    private fun extractPlaces(text: String): List<String> {
        val foundPlaces = mutableSetOf<String>()

        placePatterns.forEach { pattern ->
            pattern.findAll(text).forEach { match ->
                val place = match.groupValues.lastOrNull { it.isNotEmpty() } ?: match.value
                if (place.length > 2) {
                    foundPlaces.add(place.trim())
                }
            }
        }

        return foundPlaces.toList().take(10)
    }
}
