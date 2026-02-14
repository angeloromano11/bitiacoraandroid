package com.bitacora.digital.ui.recording

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bitacora.digital.ui.theme.RecordingRed
import com.bitacora.digital.util.formatDuration

/**
 * Recording control bar with record/stop button, interview controls, and auto-question timer.
 */
@Composable
fun RecordingControls(
    isRecording: Boolean,
    isSetup: Boolean,
    isInterviewActive: Boolean,
    isGenerating: Boolean,
    aiReady: Boolean,
    durationSeconds: Int,
    autoQuestionsEnabled: Boolean,
    countdown: Int,
    error: String?,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onNextQuestionClick: () -> Unit,
    onToggleAutoQuestions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Auto-question countdown
        if (isInterviewActive && autoQuestionsEnabled && countdown > 0) {
            Text(
                text = "Next question in ${countdown}s",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

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

        // Control buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Auto-question toggle (during interview)
            if (isInterviewActive) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(50.dp)
                ) {
                    IconButton(onClick = onToggleAutoQuestions) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Auto Questions",
                            tint = if (autoQuestionsEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.White
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "Auto",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            // Main action button
            if (!isRecording && !isInterviewActive) {
                // Start button
                Button(
                    onClick = onStartClick,
                    enabled = isSetup,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (aiReady) Icons.Default.Mic else Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start")
                }
            } else {
                // Record / Stop button
                IconButton(
                    onClick = {
                        if (isRecording) onStopClick() else onStartClick()
                    },
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Outer ring
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(Color.Transparent)
                            )
                        }

                        // Inner button
                        if (isRecording) {
                            // Stop button (square)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RecordingRed)
                            )
                        } else {
                            // Record button (circle)
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(RecordingRed)
                            )
                        }
                    }
                }
            }

            // Next Question button (during interview)
            if (isInterviewActive) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(50.dp)
                ) {
                    IconButton(
                        onClick = onNextQuestionClick,
                        enabled = !isGenerating
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Forward,
                                contentDescription = "Next Question",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = "Next",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
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
        if (!aiReady && isSetup && !isInterviewActive && !isRecording) {
            Text(
                text = "Recording only mode. Add API key in Settings for AI-guided interviews.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF9800),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
