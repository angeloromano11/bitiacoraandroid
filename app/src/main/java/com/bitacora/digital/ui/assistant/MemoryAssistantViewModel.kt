package com.bitacora.digital.ui.assistant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitacora.digital.model.ChatMessage
import com.bitacora.digital.model.SuggestedQuestion
import com.bitacora.digital.service.MemoryAssistantService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the memory assistant chat screen.
 */
@HiltViewModel
class MemoryAssistantViewModel @Inject constructor(
    private val assistantService: MemoryAssistantService
) : ViewModel() {

    var messages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set

    var suggestedQuestions by mutableStateOf<List<SuggestedQuestion>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isInitialized by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var inputText by mutableStateOf("")

    init {
        initialize()
    }

    /**
     * Initialize the assistant service.
     */
    private fun initialize() {
        viewModelScope.launch {
            try {
                assistantService.initialize()
                isInitialized = assistantService.isInitialized
                if (isInitialized) {
                    loadSuggestions()
                }
                messages = assistantService.getChatHistory()
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    /**
     * Load suggested questions.
     */
    fun loadSuggestions() {
        viewModelScope.launch {
            try {
                suggestedQuestions = assistantService.getSuggestedQuestions()
            } catch (e: Exception) {
                // Suggestions are optional, don't show error
            }
        }
    }

    /**
     * Send a message.
     */
    fun sendMessage() {
        val text = inputText.trim()
        if (text.isEmpty() || isLoading) return

        inputText = ""
        isLoading = true
        error = null

        viewModelScope.launch {
            try {
                val response = assistantService.askQuestion(text)
                messages = assistantService.getChatHistory()
                // Refresh suggestions after response
                loadSuggestions()
            } catch (e: Exception) {
                error = e.message ?: "Failed to get response"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Send a suggested question.
     */
    fun sendSuggestion(suggestion: SuggestedQuestion) {
        inputText = suggestion.text
        sendMessage()
    }

    /**
     * Update input text.
     */
    fun updateInput(text: String) {
        inputText = text
    }

    /**
     * Clear chat history.
     */
    fun clearChat() {
        assistantService.clearHistory()
        messages = emptyList()
        loadSuggestions()
    }

    /**
     * Clear error.
     */
    fun clearError() {
        error = null
    }
}
