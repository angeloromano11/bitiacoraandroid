package com.bitacora.digital.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bitacora.digital.model.InterviewType
import com.bitacora.digital.model.Session
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Library screen showing all recorded sessions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onSessionClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val groupedSessions by viewModel.groupedSessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val datesWithSessions by viewModel.datesWithSessions.collectAsState()

    var sessionToDelete by remember { mutableStateOf<Session?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header with view mode toggle
        LibraryHeader(
            viewMode = viewModel.viewMode,
            onViewModeToggle = { viewModel.toggleViewMode() },
            onSettingsClick = onSettingsClick
        )

        // Content based on view mode
        when (viewModel.viewMode) {
            LibraryViewMode.CALENDAR -> {
                CalendarContent(
                    viewModel = viewModel,
                    sessions = sessions,
                    datesWithSessions = datesWithSessions,
                    isLoading = isLoading,
                    onSessionClick = onSessionClick,
                    onDeleteClick = { sessionToDelete = it }
                )
            }

            LibraryViewMode.LIST -> {
                ListContent(
                    viewModel = viewModel,
                    sessions = sessions,
                    groupedSessions = groupedSessions,
                    isLoading = isLoading,
                    onSessionClick = onSessionClick,
                    onDeleteClick = { sessionToDelete = it }
                )
            }
        }
    }

    // Delete confirmation dialog
    sessionToDelete?.let { session ->
        DeleteConfirmationDialog(
            session = session,
            onConfirm = {
                viewModel.deleteSession(session)
                sessionToDelete = null
            },
            onDismiss = { sessionToDelete = null }
        )
    }
}

/**
 * Calendar view content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarContent(
    viewModel: LibraryViewModel,
    sessions: List<Session>,
    datesWithSessions: Set<LocalDate>,
    isLoading: Boolean,
    onSessionClick: (String) -> Unit,
    onDeleteClick: (Session) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Filter chips
            FilterChipsRow(
                selectedType = viewModel.filterType,
                onTypeSelected = { viewModel.updateFilterType(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Calendar header with navigation
            CalendarHeader(
                currentMonth = viewModel.currentMonth,
                onPreviousMonth = { viewModel.goToPreviousMonth() },
                onNextMonth = { viewModel.goToNextMonth() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid
            CalendarGrid(
                currentMonth = viewModel.currentMonth,
                selectedDate = viewModel.selectedDate,
                datesWithSessions = datesWithSessions,
                onDateClick = { viewModel.selectDate(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selected date sessions or instructions
            if (viewModel.selectedDate != null) {
                SelectedDateSessions(
                    date = viewModel.selectedDate!!,
                    sessions = viewModel.getSessionsForDate(viewModel.selectedDate!!),
                    onSessionClick = onSessionClick,
                    onDeleteClick = onDeleteClick,
                    onClearSelection = { viewModel.selectDate(null) }
                )
            } else if (sessions.isEmpty()) {
                EmptyState(hasFilters = viewModel.filterType != null)
            } else {
                CalendarInstructions()
            }

            Spacer(modifier = Modifier.height(80.dp)) // Bottom padding
        }
    }
}

/**
 * List view content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListContent(
    viewModel: LibraryViewModel,
    sessions: List<Session>,
    groupedSessions: Map<String, List<Session>>,
    isLoading: Boolean,
    onSessionClick: (String) -> Unit,
    onDeleteClick: (Session) -> Unit
) {
    Column {
        // Search bar
        SearchBar(
            query = viewModel.searchQuery,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onClear = { viewModel.updateSearchQuery("") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips
        FilterChipsRow(
            selectedType = viewModel.filterType,
            onTypeSelected = { viewModel.updateFilterType(it) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                isLoading && sessions.isEmpty() -> {
                    LoadingState()
                }

                sessions.isEmpty() -> {
                    EmptyState(
                        hasFilters = viewModel.searchQuery.isNotEmpty() || viewModel.filterType != null
                    )
                }

                else -> {
                    SessionList(
                        groupedSessions = groupedSessions,
                        onSessionClick = onSessionClick,
                        onDeleteClick = onDeleteClick
                    )
                }
            }
        }
    }
}

/**
 * Library header with title, view mode toggle, and settings button.
 */
@Composable
private fun LibraryHeader(
    viewMode: LibraryViewMode,
    onViewModeToggle: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onViewModeToggle) {
                Icon(
                    imageVector = if (viewMode == LibraryViewMode.CALENDAR) {
                        Icons.AutoMirrored.Filled.List
                    } else {
                        Icons.Default.CalendarMonth
                    },
                    contentDescription = if (viewMode == LibraryViewMode.CALENDAR) {
                        "Switch to list view"
                    } else {
                        "Switch to calendar view"
                    },
                    tint = Color.White
                )
            }

            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Calendar header with month navigation.
 */
@Composable
private fun CalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous month",
                tint = Color.White
            )
        }

        Text(
            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next month",
                tint = Color.White
            )
        }
    }
}

/**
 * Calendar grid showing days of the month.
 */
@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate?,
    datesWithSessions: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit
) {
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val today = LocalDate.now()

    // Generate days for the calendar grid
    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0

    val days = mutableListOf<LocalDate?>()

    // Add empty cells for days before the first day of month
    repeat(firstDayOfWeek) { days.add(null) }

    // Add all days of the month
    var day = firstDayOfMonth
    while (!day.isAfter(lastDayOfMonth)) {
        days.add(day)
        day = day.plusDays(1)
    }

    // Pad to complete the last week
    while (days.size % 7 != 0) {
        days.add(null)
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Weekday headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { dayName ->
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar days
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(((days.size / 7) * 48).dp),
            userScrollEnabled = false
        ) {
            items(days) { date ->
                if (date != null) {
                    CalendarDayCell(
                        date = date,
                        isSelected = date == selectedDate,
                        isToday = date == today,
                        hasSession = datesWithSessions.contains(date),
                        onClick = { onDateClick(date) }
                    )
                } else {
                    Box(modifier = Modifier.aspectRatio(1f))
                }
            }
        }
    }
}

/**
 * Single calendar day cell.
 */
@Composable
private fun CalendarDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasSession: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> Color.White
        isToday -> MaterialTheme.colorScheme.primary
        else -> Color.White.copy(alpha = 0.8f)
    }

    val dotColor = when {
        isSelected -> Color.White
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    }

    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )

        if (hasSession) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Sessions for the selected date.
 */
@Composable
private fun SelectedDateSessions(
    date: LocalDate,
    sessions: List<Session>,
    onSessionClick: (String) -> Unit,
    onDeleteClick: (Session) -> Unit,
    onClearSelection: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, ${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${date.dayOfMonth}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            IconButton(onClick = onClearSelection) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear selection",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (sessions.isEmpty()) {
            Text(
                text = "No sessions on this day",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = TextAlign.Center
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            sessions.forEach { session ->
                SessionCard(
                    session = session,
                    onClick = { onSessionClick(session.id) },
                    onDelete = { onDeleteClick(session) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Instructions for using the calendar.
 */
@Composable
private fun CalendarInstructions() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.TouchApp,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(32.dp)
        )

        Text(
            text = "Tap a day to view sessions",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

/**
 * Search bar component.
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = true,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search sessions...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                    innerTextField()
                }

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        },
        modifier = modifier
    )
}

/**
 * Filter chips row.
 */
@Composable
private fun FilterChipsRow(
    selectedType: InterviewType?,
    onTypeSelected: (InterviewType?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedType == null,
                onClick = { onTypeSelected(null) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        items(InterviewType.entries.toList()) { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(if (selectedType == type) null else type) },
                label = { Text(type.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

/**
 * Session list grouped by date.
 */
@Composable
private fun SessionList(
    groupedSessions: Map<String, List<Session>>,
    onSessionClick: (String) -> Unit,
    onDeleteClick: (Session) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedSessions.forEach { (date, sessions) ->
            item(key = "header_$date") {
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(
                items = sessions,
                key = { it.id }
            ) { session ->
                SessionCard(
                    session = session,
                    onClick = { onSessionClick(session.id) },
                    onDelete = { onDeleteClick(session) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // Bottom padding for nav bar
        }
    }
}

/**
 * Loading state.
 */
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Empty state.
 */
@Composable
private fun EmptyState(hasFilters: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = if (hasFilters) "No matching sessions" else "No sessions yet",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            Text(
                text = if (hasFilters) {
                    "Try adjusting your search or filters"
                } else {
                    "Record your first session to get started"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Delete confirmation dialog.
 */
@Composable
private fun DeleteConfirmationDialog(
    session: Session,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Session?") },
        text = {
            Text("This will permanently delete this ${session.formattedDuration} recording. This action cannot be undone.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
