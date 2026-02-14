package com.bitacora.digital.di

import android.content.Context
import androidx.room.Room
import com.bitacora.digital.data.database.AppDatabase
import com.bitacora.digital.data.database.MemoryEntryDao
import com.bitacora.digital.data.database.SessionDao
import com.bitacora.digital.util.Config
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Room database dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Config.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao {
        return database.sessionDao()
    }

    @Provides
    fun provideMemoryEntryDao(database: AppDatabase): MemoryEntryDao {
        return database.memoryEntryDao()
    }
}
