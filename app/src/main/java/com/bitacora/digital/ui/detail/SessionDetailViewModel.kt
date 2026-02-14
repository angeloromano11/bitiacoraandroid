package com.bitacora.digital.ui.detail

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitacora.digital.data.repository.SessionRepository
import com.bitacora.digital.model.Session
import com.bitacora.digital.service.StorageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for session detail screen.
 */
@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val storageService: StorageService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    var isEditingNotes by mutableStateOf(false)
        private set
    var editedNotes by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var isDeleted by mutableStateOf(false)
        private set

    init {
        loadSession()
    }

    /**
     * Load the session from repository.
     */
    private fun loadSession() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val loadedSession = sessionRepository.getSession(sessionId)
                _session.value = loadedSession
                editedNotes = loadedSession?.notes ?: ""
            } catch (e: Exception) {
                error = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get video URI for playback.
     */
    fun getVideoUri(): Uri? {
        val session = _session.value ?: return null
        val videoFile = File(storageService.sessionsDir, session.filename)
        return if (videoFile.exists()) {
            Uri.fromFile(videoFile)
        } else {
            null
        }
    }

    /**
     * Start editing notes.
     */
    fun startEditingNotes() {
        editedNotes = _session.value?.notes ?: ""
        isEditingNotes = true
    }

    /**
     * Update notes text while editing.
     */
    fun updateNotes(notes: String) {
        editedNotes = notes
    }

    /**
     * Cancel editing notes.
     */
    fun cancelEditingNotes() {
        editedNotes = _session.value?.notes ?: ""
        isEditingNotes = false
    }

    /**
     * Save edited notes.
     */
    fun saveNotes() {
        viewModelScope.launch {
            try {
                sessionRepository.updateNotes(sessionId, editedNotes)
                _session.value = _session.value?.copy(notes = editedNotes)
                isEditingNotes = false
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    /**
     * Delete the session.
     */
    fun deleteSession() {
        val session = _session.value ?: return

        viewModelScope.launch {
            try {
                sessionRepository.deleteSession(session)
                isDeleted = true
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    /**
     * Get share URI for the video file.
     */
    fun getShareUri(): Uri? {
        return getVideoUri()
    }

    /**
     * Clear error.
     */
    fun clearError() {
        error = null
    }
}
