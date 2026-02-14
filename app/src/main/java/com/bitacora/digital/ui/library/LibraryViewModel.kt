package com.bitacora.digital.ui.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitacora.digital.data.repository.SessionRepository
import com.bitacora.digital.model.InterviewType
import com.bitacora.digital.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the session library screen.
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    // Search and filter state
    var searchQuery by mutableStateOf("")
    var filterType by mutableStateOf<InterviewType?>(null)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    var error by mutableStateOf<String?>(null)
        private set

    // Sessions flow from repository
    private val allSessions = sessionRepository.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Search query as flow for combining
    private val _searchQueryFlow = MutableStateFlow("")
    private val _filterTypeFlow = MutableStateFlow<InterviewType?>(null)

    // Filtered sessions combining all sources
    val sessions: StateFlow<List<Session>> = combine(
        allSessions,
        _searchQueryFlow,
        _filterTypeFlow
    ) { sessions, query, type ->
        _isLoading.value = false
        filterSessions(sessions, query, type)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Grouped sessions by date
    val groupedSessions: StateFlow<Map<String, List<Session>>> = sessions
        .combine(_searchQueryFlow) { sessions, _ ->
            groupSessionsByDate(sessions)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    /**
     * Update search query.
     */
    fun updateSearchQuery(query: String) {
        searchQuery = query
        _searchQueryFlow.value = query
    }

    /**
     * Update filter type.
     */
    fun updateFilterType(type: InterviewType?) {
        filterType = type
        _filterTypeFlow.value = type
    }

    /**
     * Clear all filters.
     */
    fun clearFilters() {
        searchQuery = ""
        filterType = null
        _searchQueryFlow.value = ""
        _filterTypeFlow.value = null
    }

    /**
     * Delete a session.
     */
    fun deleteSession(session: Session) {
        viewModelScope.launch {
            try {
                sessionRepository.deleteSession(session)
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    /**
     * Refresh sessions.
     */
    fun refresh() {
        _isLoading.value = true
        // Flow will automatically refresh
        viewModelScope.launch {
            _isLoading.value = false
        }
    }

    /**
     * Clear error.
     */
    fun clearError() {
        error = null
    }

    // MARK: - Private

    private fun filterSessions(
        sessions: List<Session>,
        query: String,
        type: InterviewType?
    ): List<Session> {
        var filtered = sessions

        // Filter by type
        if (type != null) {
            filtered = filtered.filter { it.interviewType == type }
        }

        // Filter by search query
        if (query.isNotBlank()) {
            val lowerQuery = query.lowercase()
            filtered = filtered.filter { session ->
                session.userName.lowercase().contains(lowerQuery) ||
                session.notes.lowercase().contains(lowerQuery) ||
                session.tags.any { it.lowercase().contains(lowerQuery) } ||
                session.collection.lowercase().contains(lowerQuery)
            }
        }

        return filtered.sortedByDescending { it.createdAt }
    }

    private fun groupSessionsByDate(sessions: List<Session>): Map<String, List<Session>> {
        return sessions.groupBy { session ->
            session.formattedDate
        }
    }
}
