package com.bitacora.digital.ui.recording

import android.Manifest
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Main recording screen with camera preview, AI interview overlays, and controls.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val isRecording by viewModel.isRecording.collectAsState()
    val isSetup by viewModel.isSetup.collectAsState()
    val transcript by viewModel.transcript.collectAsState()
    val partialTranscript by viewModel.partialTranscript.collectAsState()
    val isListening by viewModel.isListening.collectAsState()

    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Permission handling
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    // Request permissions on first launch
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // Initialize camera when permissions granted and preview ready
    LaunchedEffect(permissionsState.allPermissionsGranted, previewView) {
        if (permissionsState.allPermissionsGranted && previewView != null && !isSetup) {
            viewModel.setup(lifecycleOwner, previewView!!)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            !permissionsState.allPermissionsGranted -> {
                // Permission request UI
                PermissionRequest(
                    onRequestPermission = { permissionsState.launchMultiplePermissionRequest() }
                )
            }
            !isSetup -> {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Setting up camera...",
                            color = Color.Gray
                        )
                    }
                }
            }
            else -> {
                // Camera preview
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onPreviewViewCreated = { previewView = it }
                )

                // Type selector overlay (pre-recording setup)
                if (viewModel.showTypeSelector && !isRecording) {
                    TypeSelectorOverlay(
                        aiReady = viewModel.aiReady,
                        selectedType = viewModel.interviewType,
                        subcategory = viewModel.subcategory,
                        userName = viewModel.userName,
                        onTypeSelected = { type, sub ->
                            viewModel.interviewType = type
                            viewModel.subcategory = sub
                        },
                        onUserNameChanged = { viewModel.userName = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f))
                    )
                }

                // Interview overlays (during recording)
                if (isRecording || viewModel.isInterviewActive) {
                    // Compact type selector (top left)
                    CompactTypeSelector(
                        selectedType = viewModel.interviewType,
                        subcategory = viewModel.subcategory,
                        disabled = viewModel.isInterviewActive,
                        onTypeSelected = { type, sub ->
                            viewModel.interviewType = type
                            viewModel.subcategory = sub
                        },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    )

                    // AI question overlay (top center)
                    if (viewModel.isInterviewActive && viewModel.currentQuestion.isNotEmpty()) {
                        QuestionOverlay(
                            question = viewModel.currentQuestion,
                            questionNumber = viewModel.questionNumber,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 60.dp)
                        )
                    }

                    // Transcript display (above controls)
                    if (viewModel.isInterviewActive) {
                        TranscriptDisplay(
                            transcript = transcript,
                            partialTranscript = partialTranscript,
                            isListening = isListening,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 180.dp)
                        )
                    }
                }

                // Controls
                RecordingControls(
                    isRecording = isRecording,
                    isSetup = isSetup,
                    isInterviewActive = viewModel.isInterviewActive,
                    isGenerating = viewModel.isGenerating,
                    aiReady = viewModel.aiReady,
                    durationSeconds = viewModel.durationSeconds,
                    autoQuestionsEnabled = viewModel.autoQuestionsEnabled,
                    countdown = viewModel.countdown,
                    error = viewModel.error,
                    onStartClick = { viewModel.startInterview() },
                    onStopClick = { viewModel.stopRecording() },
                    onNextQuestionClick = { viewModel.requestNextQuestion() },
                    onToggleAutoQuestions = { viewModel.toggleAutoQuestions() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp)
                )
            }
        }
    }
}

/**
 * Recording indicator badge.
 */
@Composable
private fun RecordingIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(RecordingRed, CircleShape)
        )
        Text(
            text = "REC",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

/**
 * Recording controls bar.
 */
@Composable
private fun RecordingControls(
    isRecording: Boolean,
    isSetup: Boolean,
    aiReady: Boolean,
    durationSeconds: Int,
    error: String?,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Duration display
        if (isRecording) {
            Row(
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(RecordingRed, CircleShape)
                )
                Text(
                    text = durationSeconds.formatDuration(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }

        // Main button
        if (!isRecording) {
            Button(
                onClick = onStartClick,
                enabled = isSetup,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(
                    imageVector = if (aiReady) Icons.Default.Mic else Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text("Start")
            }
        } else {
            // Stop button
            IconButton(
                onClick = onStopClick,
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer ring
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.Transparent)
                        )
                    }
                    // Inner stop icon
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(RecordingRed)
                    )
                }
            }
        }

        // Error message
        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        // AI status message
        if (!aiReady && isSetup && !isRecording) {
            Text(
                text = "Recording only mode. Add API key in Settings for AI-guided interviews.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF9800),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

/**
 * Permission request UI.
 */
@Composable
private fun PermissionRequest(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Text(
                text = "Camera & Microphone Required",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "Bitacora needs access to your camera and microphone to record videos.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Button(onClick = onRequestPermission) {
                Text("Grant Permissions")
            }
        }
    }
}

/**
 * Type selector overlay for pre-recording setup.
 */
@Composable
private fun TypeSelectorOverlay(
    aiReady: Boolean,
    selectedType: com.bitacora.digital.model.InterviewType,
    subcategory: com.bitacora.digital.model.PracticeSubcategory?,
    userName: String,
    onTypeSelected: (com.bitacora.digital.model.InterviewType, com.bitacora.digital.model.PracticeSubcategory?) -> Unit,
    onUserNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Text(
                text = if (aiReady) "New Interview" else "New Recording",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            if (aiReady) {
                // Full interview type selector
                InterviewTypeSelector(
                    selectedType = selectedType,
                    subcategory = subcategory,
                    userName = userName,
                    onTypeSelected = onTypeSelected,
                    onUserNameChanged = onUserNameChanged,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Recording-only mode message
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Text(
                        text = "Record video without AI questions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add your API key in Settings to enable AI-guided interviews.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}
