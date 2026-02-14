package com.bitacora.digital.di

import android.content.Context
import com.bitacora.digital.service.KeychainHelper
import com.bitacora.digital.service.StorageService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for app-level singleton dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideKeychainHelper(
        @ApplicationContext context: Context
    ): KeychainHelper {
        return KeychainHelper(context)
    }

    @Provides
    @Singleton
    fun provideStorageService(
        @ApplicationContext context: Context
    ): StorageService {
        return StorageService(context).also { it.initialize() }
    }
}
