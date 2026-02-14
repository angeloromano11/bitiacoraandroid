package com.bitacora.digital.ui.detail

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bitacora.digital.model.Session
import com.bitacora.digital.model.getInterviewTypeInfo

/**
 * Session detail screen with video playback and metadata.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    onBackClick: () -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Navigate back when deleted
    LaunchedEffect(viewModel.isDeleted) {
        if (viewModel.isDeleted) {
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Share button
                    IconButton(
                        onClick = {
                            viewModel.getShareUri()?.let { uri ->
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "video/mp4"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }

                    // Delete button
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            session == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Session not found",
                        color = Color.Gray
                    )
                }
            }
            else -> {
                SessionDetailContent(
                    session = session!!,
                    videoUri = viewModel.getVideoUri(),
                    isEditingNotes = viewModel.isEditingNotes,
                    editedNotes = viewModel.editedNotes,
                    onStartEditNotes = { viewModel.startEditingNotes() },
                    onNotesChange = { viewModel.updateNotes(it) },
                    onSaveNotes = { viewModel.saveNotes() },
                    onCancelEditNotes = { viewModel.cancelEditingNotes() },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Session?") },
            text = {
                Text("This will permanently delete this recording. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Session detail content.
 */
@Composable
private fun SessionDetailContent(
    session: Session,
    videoUri: android.net.Uri?,
    isEditingNotes: Boolean,
    editedNotes: String,
    onStartEditNotes: () -> Unit,
    onNotesChange: (String) -> Unit,
    onSaveNotes: () -> Unit,
    onCancelEditNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeInfo = getInterviewTypeInfo(session.interviewType, session.subcategory)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Video player
        if (videoUri != null) {
            VideoPlayer(
                uri = videoUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Video not available",
                    color = Color.Gray
                )
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Session type and duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    typeInfo?.let { info ->
                        Icon(
                            imageVector = info.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = typeInfo?.label ?: session.interviewType.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }

                Text(
                    text = session.formattedDuration,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }

            // Metadata grid
            MetadataGrid(session = session)

            // Notes section
            NotesSection(
                notes = if (isEditingNotes) editedNotes else session.notes,
                isEditing = isEditingNotes,
                onStartEdit = onStartEditNotes,
                onNotesChange = onNotesChange,
                onSave = onSaveNotes,
                onCancel = onCancelEditNotes
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Video player using ExoPlayer.
 */
@Composable
private fun VideoPlayer(
    uri: android.net.Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
            }
        },
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
    )
}

/**
 * Metadata grid showing session info.
 */
@Composable
private fun MetadataGrid(session: Session) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (session.userName.isNotEmpty()) {
            MetadataRow(label = "Name", value = session.userName)
        }

        MetadataRow(label = "Recorded", value = session.formattedDate)

        if (session.questionsCount > 0) {
            MetadataRow(label = "Questions", value = "${session.questionsCount}")
        }

        if (session.collection.isNotEmpty()) {
            MetadataRow(label = "Collection", value = session.collection)
        }

        if (session.tags.isNotEmpty()) {
            MetadataRow(label = "Tags", value = session.tags.joinToString(", "))
        }
    }
}

/**
 * Single metadata row.
 */
@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

/**
 * Notes section with edit capability.
 */
@Composable
private fun NotesSection(
    notes: String,
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            if (isEditing) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.Gray
                        )
                    }
                    IconButton(onClick = onSave, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                IconButton(onClick = onStartEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit notes",
                        tint = Color.Gray
                    )
                }
            }
        }

        if (isEditing) {
            BasicTextField(
                value = notes,
                onValueChange = onNotesChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
                    .height(100.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (notes.isEmpty()) {
                            Text(
                                text = "Add notes about this session...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    }
                }
            )
        } else {
            Text(
                text = notes.ifEmpty { "No notes yet" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (notes.isEmpty()) Color.Gray else Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            )
        }
    }
}
