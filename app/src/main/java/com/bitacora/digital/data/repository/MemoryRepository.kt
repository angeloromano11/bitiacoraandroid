package com.bitacora.digital.data.repository

import com.bitacora.digital.data.database.MemoryEntryDao
import com.bitacora.digital.data.database.MemoryEntryEntity
import com.bitacora.digital.model.EntryType
import com.bitacora.digital.model.MemoryEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for memory entry data operations.
 */
@Singleton
class MemoryRepository @Inject constructor(
    private val memoryEntryDao: MemoryEntryDao
) {
    private val gson = Gson()

    /**
     * Save a memory entry.
     */
    suspend fun saveEntry(entry: MemoryEntry) {
        memoryEntryDao.insert(entry.toEntity())
    }

    /**
     * Get all entries for a session.
     */
    suspend fun getSessionEntries(sessionId: String): List<MemoryEntry> {
        return memoryEntryDao.getEntriesBySession(sessionId).map { it.toModel() }
    }

    /**
     * Get all memory entries.
     */
    suspend fun getAllEntries(): List<MemoryEntry> {
        return memoryEntryDao.getAllEntries().map { it.toModel() }
    }

    /**
     * Get entries by type.
     */
    suspend fun getEntriesByType(type: EntryType): List<MemoryEntry> {
        return memoryEntryDao.getEntriesByType(type.name).map { it.toModel() }
    }

    /**
     * Search entries by query.
     */
    suspend fun searchEntries(query: String): List<MemoryEntry> {
        return memoryEntryDao.searchEntries(query).map { it.toModel() }
    }

    /**
     * Delete all entries for a session.
     */
    suspend fun deleteBySession(sessionId: String) {
        memoryEntryDao.deleteBySession(sessionId)
    }

    /**
     * Get total entry count.
     */
    suspend fun getCount(): Int {
        return memoryEntryDao.getCount()
    }

    /**
     * Get unique session count.
     */
    suspend fun getSessionCount(): Int {
        return memoryEntryDao.getSessionCount()
    }

    // MARK: - Entity <-> Model Mapping

    private fun MemoryEntryEntity.toModel(): MemoryEntry {
        val type = object : TypeToken<List<String>>() {}.type

        fun parseList(json: String): List<String> = try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return MemoryEntry(
            id = id,
            sessionId = sessionId,
            entryType = EntryType.entries.find { it.name == entryType } ?: EntryType.RESPONSE,
            content = content,
            timestamp = timestamp,
            topics = parseList(topics),
            peopleMentioned = parseList(peopleMentioned),
            placesMentioned = parseList(placesMentioned),
            emotions = parseList(emotions)
        )
    }

    private fun MemoryEntry.toEntity(): MemoryEntryEntity {
        return MemoryEntryEntity(
            id = id,
            sessionId = sessionId,
            entryType = entryType.name,
            content = content,
            timestamp = timestamp,
            topics = gson.toJson(topics),
            peopleMentioned = gson.toJson(peopleMentioned),
            placesMentioned = gson.toJson(placesMentioned),
            emotions = gson.toJson(emotions)
        )
    }
}
