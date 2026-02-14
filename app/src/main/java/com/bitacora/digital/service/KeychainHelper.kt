package com.bitacora.digital.service

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.bitacora.digital.util.Config
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for API keys and sensitive data using EncryptedSharedPreferences.
 */
@Singleton
class KeychainHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            Config.SECURE_PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Save a value securely.
     */
    fun save(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    /**
     * Load a value securely.
     */
    fun load(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    /**
     * Delete a value.
     */
    fun delete(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    /**
     * Check if a key exists.
     */
    fun exists(key: String): Boolean {
        return sharedPreferences.contains(key)
    }
}
