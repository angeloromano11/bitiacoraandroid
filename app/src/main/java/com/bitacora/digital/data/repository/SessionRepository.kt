package com.bitacora.digital.data.repository

import com.bitacora.digital.data.database.MemoryEntryDao
import com.bitacora.digital.data.database.SessionDao
import com.bitacora.digital.data.database.SessionEntity
import com.bitacora.digital.model.InterviewType
import com.bitacora.digital.model.PracticeSubcategory
import com.bitacora.digital.model.Session
import com.bitacora.digital.service.StorageService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for session data operations.
 */
@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val memoryEntryDao: MemoryEntryDao,
    private val storageService: StorageService
) {
    private val gson = Gson()

    /**
     * Get all sessions as a Flow.
     */
    fun getAllSessions(): Flow<List<Session>> {
        return sessionDao.getAllSessions().map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Get a single session by ID.
     */
    suspend fun getSession(id: String): Session? {
        return sessionDao.getSession(id)?.toModel()
    }

    /**
     * Get sessions filtered by type.
     */
    fun getSessionsByType(type: InterviewType): Flow<List<Session>> {
        return sessionDao.getSessionsByType(type.value).map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Save a new session.
     */
    suspend fun saveSession(session: Session) {
        sessionDao.insert(session.toEntity())
    }

    /**
     * Update session notes.
     */
    suspend fun updateNotes(id: String, notes: String) {
        sessionDao.updateNotes(id, notes)
    }

    /**
     * Update session tags.
     */
    suspend fun updateTags(id: String, tags: List<String>) {
        sessionDao.updateTags(id, gson.toJson(tags))
    }

    /**
     * Delete a session and its associated files.
     */
    suspend fun deleteSession(session: Session) {
        // Delete video file
        storageService.deleteSession(session.filename)

        // Delete memory entries
        memoryEntryDao.deleteBySession(session.id)

        // Delete database entry
        sessionDao.deleteById(session.id)
    }

    /**
     * Search sessions by query.
     */
    fun searchSessions(query: String): Flow<List<Session>> {
        return sessionDao.searchSessions(query).map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Get all collections.
     */
    fun getCollections(): Flow<List<String>> {
        return sessionDao.getCollections()
    }

    /**
     * Get session count.
     */
    suspend fun getCount(): Int {
        return sessionDao.getCount()
    }

    // MARK: - Entity <-> Model Mapping

    private fun SessionEntity.toModel(): Session {
        val type = object : TypeToken<List<String>>() {}.type
        val tagsList: List<String> = try {
            gson.fromJson(tags, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val keyTopicsList: List<String>? = keyTopics?.let {
            try {
                gson.fromJson(it, type)
            } catch (e: Exception) {
                null
            }
        }

        return Session(
            id = id,
            filename = filename,
            createdAt = createdAt,
            durationSeconds = durationSeconds,
            interviewType = InterviewType.entries.find { it.value == interviewType }
                ?: InterviewType.MEMORY,
            subcategory = subcategory?.let { sub ->
                PracticeSubcategory.entries.find { it.value == sub }
            },
            userName = userName,
            questionsCount = questionsCount,
            tags = tagsList,
            collection = collection,
            notes = notes,
            encrypted = encrypted,
            thumbnailPath = thumbnailPath,
            summary = summary,
            keyTopics = keyTopicsList
        )
    }

    private fun Session.toEntity(): SessionEntity {
        return SessionEntity(
            id = id,
            filename = filename,
            createdAt = createdAt,
            durationSeconds = durationSeconds,
            interviewType = interviewType.value,
            subcategory = subcategory?.value,
            userName = userName,
            questionsCount = questionsCount,
            tags = gson.toJson(tags),
            collection = collection,
            notes = notes,
            encrypted = encrypted,
            thumbnailPath = thumbnailPath,
            summary = summary,
            keyTopics = keyTopics?.let { gson.toJson(it) }
        )
    }
}
