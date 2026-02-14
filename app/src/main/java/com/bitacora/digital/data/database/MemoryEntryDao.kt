package com.bitacora.digital.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data access object for memory entries.
 */
@Dao
interface MemoryEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MemoryEntryEntity)

    @Query("SELECT * FROM memory_entries WHERE session_id = :sessionId ORDER BY timestamp")
    suspend fun getEntriesBySession(sessionId: String): List<MemoryEntryEntity>

    @Query("SELECT * FROM memory_entries ORDER BY timestamp DESC")
    suspend fun getAllEntries(): List<MemoryEntryEntity>

    @Query("SELECT * FROM memory_entries WHERE entry_type = :type ORDER BY timestamp DESC")
    suspend fun getEntriesByType(type: String): List<MemoryEntryEntity>

    @Query("""
        SELECT * FROM memory_entries
        WHERE content LIKE '%' || :query || '%'
           OR topics LIKE '%' || :query || '%'
           OR people_mentioned LIKE '%' || :query || '%'
           OR places_mentioned LIKE '%' || :query || '%'
    """)
    suspend fun searchEntries(query: String): List<MemoryEntryEntity>

    @Query("DELETE FROM memory_entries WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: String)

    @Query("SELECT COUNT(*) FROM memory_entries")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(DISTINCT session_id) FROM memory_entries")
    suspend fun getSessionCount(): Int
}
