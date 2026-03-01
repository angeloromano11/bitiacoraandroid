package com.bitacora.digital.ui.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitacora.digital.service.AIService
import com.bitacora.digital.service.StorageService
import com.bitacora.digital.service.providers.AIProviderType
import com.bitacora.digital.util.Config
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = Config.PREFERENCES_NAME)

/**
 * ViewModel for the settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiService: AIService,
    private val storageService: StorageService
) : ViewModel() {

    companion object {
        private val AUTO_QUESTIONS_KEY = booleanPreferencesKey(Config.AUTO_QUESTIONS_ENABLED_KEY)
        private val QUESTION_INTERVAL_KEY = intPreferencesKey(Config.QUESTION_INTERVAL_KEY)
    }

    // Provider state
    var activeProvider by mutableStateOf<AIProviderType?>(null)
        private set
    var providerStatuses by mutableStateOf<Map<AIProviderType, Boolean>>(emptyMap())
        private set
    var isValidatingKey by mutableStateOf(false)
        private set
    var validationError by mutableStateOf<String?>(null)
        private set
    var selectedProviderForSetup by mutableStateOf<AIProviderType?>(null)

    // Legacy compatibility
    val apiKeyConfigured: Boolean get() = providerStatuses.any { it.value }

    // Interview settings
    var autoQuestionsEnabled by mutableStateOf(false)
    var questionInterval by mutableIntStateOf(Config.AUTO_QUESTION_DEFAULT_INTERVAL)

    // Storage info
    var storageUsed by mutableStateOf("")
        private set
    var sessionCount by mutableStateOf(0)
        private set

    // General
    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadSettings()
    }

    /**
     * Load all settings.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            // Initialize AI service and load provider statuses
            aiService.initialize()
            activeProvider = aiService.currentProviderType
            loadProviderStatuses()

            // Load preferences
            val prefs = context.dataStore.data.first()
            autoQuestionsEnabled = prefs[AUTO_QUESTIONS_KEY] ?: false
            questionInterval = prefs[QUESTION_INTERVAL_KEY] ?: Config.AUTO_QUESTION_DEFAULT_INTERVAL

            // Load storage info
            refreshStorageInfo()
        }
    }

    /**
     * Load provider configuration statuses.
     */
    private fun loadProviderStatuses() {
        providerStatuses = aiService.getProviderStatuses().toMap()
    }

    /**
     * Refresh storage information.
     */
    fun refreshStorageInfo() {
        viewModelScope.launch {
            val bytes = storageService.sessionsStorageUsed()
            storageUsed = storageService.formatBytes(bytes)
            sessionCount = storageService.getSessionFiles().size
        }
    }

    /**
     * Select a provider. If configured, switches to it. If not, opens setup.
     */
    fun selectProvider(type: AIProviderType) {
        if (providerStatuses[type] == true) {
            // Provider is configured, switch to it
            viewModelScope.launch {
                if (aiService.switchProvider(type)) {
                    activeProvider = type
                }
            }
        } else {
            // Not configured, need to set up
            selectedProviderForSetup = type
        }
    }

    /**
     * Save and validate API key for a provider.
     */
    fun saveApiKey(key: String, type: AIProviderType) {
        if (key.isBlank()) {
            validationError = "API key cannot be empty"
            return
        }

        isValidatingKey = true
        validationError = null

        viewModelScope.launch {
            try {
                val isValid = aiService.setApiKey(key, type)
                if (isValid) {
                    loadProviderStatuses()
                    activeProvider = aiService.currentProviderType
                    selectedProviderForSetup = null
                    validationError = null
                } else {
                    validationError = "Invalid API key"
                }
            } catch (e: Exception) {
                validationError = e.message ?: "Failed to validate API key"
            } finally {
                isValidatingKey = false
            }
        }
    }

    /**
     * Clear API key for a provider.
     */
    fun clearApiKey(type: AIProviderType) {
        viewModelScope.launch {
            aiService.clearApiKey(type)
            loadProviderStatuses()
            activeProvider = aiService.currentProviderType
        }
    }

    /**
     * Update auto-questions setting.
     */
    fun updateAutoQuestions(enabled: Boolean) {
        autoQuestionsEnabled = enabled
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[AUTO_QUESTIONS_KEY] = enabled
            }
        }
    }

    /**
     * Update question interval.
     */
    fun updateQuestionInterval(interval: Int) {
        questionInterval = interval.coerceIn(30, 120)
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[QUESTION_INTERVAL_KEY] = questionInterval
            }
        }
    }

    /**
     * Clear cache (temp files).
     */
    fun clearCache() {
        viewModelScope.launch {
            try {
                storageService.cleanupTempFiles()
                refreshStorageInfo()
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    /**
     * Clear error.
     */
    fun clearError() {
        error = null
        validationError = null
    }

    /**
     * Dismiss provider setup sheet.
     */
    fun dismissProviderSetup() {
        selectedProviderForSetup = null
        validationError = null
    }
}
