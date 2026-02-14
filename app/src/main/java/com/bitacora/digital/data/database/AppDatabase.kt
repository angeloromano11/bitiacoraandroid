package com.bitacora.digital.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database for Bitacora Digital.
 */
@Database(
    entities = [SessionEntity::class, MemoryEntryEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun memoryEntryDao(): MemoryEntryDao
}
