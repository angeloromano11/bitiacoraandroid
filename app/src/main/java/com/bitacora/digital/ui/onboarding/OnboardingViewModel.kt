package com.bitacora.digital.ui.onboarding

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitacora.digital.service.AIService
import com.bitacora.digital.util.Config
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = Config.PREFERENCES_NAME)

/**
 * ViewModel for the onboarding flow.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiService: AIService
) : ViewModel() {

    companion object {
        private val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey(Config.ONBOARDING_COMPLETE_KEY)
        const val TOTAL_PAGES = 4
    }

    // Onboarding state
    var currentPage by mutableIntStateOf(0)
        private set

    var isOnboardingComplete by mutableStateOf<Boolean?>(null)
        private set

    // API Key setup state
    var showApiKeySetup by mutableStateOf(false)
        private set
    var apiKeyInput by mutableStateOf("")
    var isValidatingKey by mutableStateOf(false)
        private set
    var validationError by mutableStateOf<String?>(null)
        private set

    init {
        checkOnboardingStatus()
    }

    /**
     * Check if onboarding is complete.
     */
    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            val isComplete = context.dataStore.data
                .map { prefs -> prefs[ONBOARDING_COMPLETE_KEY] ?: false }
                .first()
            isOnboardingComplete = isComplete
        }
    }

    /**
     * Move to the next page.
     */
    fun nextPage() {
        if (currentPage < TOTAL_PAGES - 1) {
            currentPage++
        } else {
            // On last page, show API key setup
            showApiKeySetup = true
        }
    }

    /**
     * Move to the previous page.
     */
    fun previousPage() {
        if (currentPage > 0) {
            currentPage--
        }
    }

    /**
     * Skip to the end.
     */
    fun skip() {
        showApiKeySetup = true
    }

    /**
     * Go to a specific page.
     */
    fun goToPage(page: Int) {
        currentPage = page.coerceIn(0, TOTAL_PAGES - 1)
    }

    /**
     * Validate and save API key.
     */
    fun validateAndSaveApiKey() {
        val key = apiKeyInput.trim()
        if (key.isBlank()) {
            validationError = "API key cannot be empty"
            return
        }

        isValidatingKey = true
        validationError = null

        viewModelScope.launch {
            try {
                val isValid = aiService.setApiKey(key)
                if (isValid) {
                    completeOnboarding()
                } else {
                    validationError = "Invalid API key. Please check and try again."
                }
            } catch (e: Exception) {
                validationError = e.message ?: "Failed to validate API key"
            } finally {
                isValidatingKey = false
            }
        }
    }

    /**
     * Skip API key setup.
     */
    fun skipApiKey() {
        completeOnboarding()
    }

    /**
     * Mark onboarding as complete.
     */
    private fun completeOnboarding() {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[ONBOARDING_COMPLETE_KEY] = true
            }
            isOnboardingComplete = true
        }
    }

    /**
     * Clear validation error.
     */
    fun clearError() {
        validationError = null
    }

    /**
     * Reset onboarding (for testing).
     */
    fun resetOnboarding() {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[ONBOARDING_COMPLETE_KEY] = false
            }
            currentPage = 0
            showApiKeySetup = false
            isOnboardingComplete = false
        }
    }
}
