package com.bitacora.digital.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for sessions.
 */
@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY created_at DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE interview_type = :type ORDER BY created_at DESC")
    fun getSessionsByType(type: String): Flow<List<SessionEntity>>

    @Query("SELECT DISTINCT collection FROM sessions WHERE collection != '' ORDER BY collection")
    fun getCollections(): Flow<List<String>>

    @Query("UPDATE sessions SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: String, notes: String)

    @Query("UPDATE sessions SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: String, tags: String)

    @Query("UPDATE sessions SET summary = :summary, key_topics = :keyTopics WHERE id = :id")
    suspend fun updateSummary(id: String, summary: String, keyTopics: String)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("""
        SELECT * FROM sessions
        WHERE user_name LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
        ORDER BY created_at DESC
    """)
    fun searchSessions(query: String): Flow<List<SessionEntity>>

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getCount(): Int
}
