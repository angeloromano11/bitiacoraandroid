package com.bitacora.digital.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for sessions table.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val filename: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int,
    @ColumnInfo(name = "interview_type") val interviewType: String,
    val subcategory: String?,
    @ColumnInfo(name = "user_name") val userName: String,
    @ColumnInfo(name = "questions_count") val questionsCount: Int,
    val tags: String,
    val collection: String,
    val notes: String,
    val encrypted: Boolean,
    @ColumnInfo(name = "thumbnail_path") val thumbnailPath: String?,
    val summary: String?,
    @ColumnInfo(name = "key_topics") val keyTopics: String?
)

/**
 * Room entity for memory entries table.
 */
@Entity(
    tableName = "memory_entries",
    indices = [
        Index("session_id"),
        Index("entry_type")
    ]
)
data class MemoryEntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "entry_type") val entryType: String,
    val content: String,
    val timestamp: String,
    val topics: String,
    @ColumnInfo(name = "people_mentioned") val peopleMentioned: String,
    @ColumnInfo(name = "places_mentioned") val placesMentioned: String,
    val emotions: String
)
