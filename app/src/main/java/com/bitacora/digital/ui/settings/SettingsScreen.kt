package com.bitacora.digital.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bitacora.digital.util.Config

/**
 * Settings screen with API key, interview settings, and storage management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onApiKeySetupClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearApiKeyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header
        TopAppBar(
            title = { Text("Settings") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black,
                titleContentColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // AI Configuration Section
            SettingsSection(title = "AI Configuration") {
                ApiKeyRow(
                    isConfigured = viewModel.apiKeyConfigured,
                    onAddClick = onApiKeySetupClick,
                    onClearClick = { showClearApiKeyDialog = true }
                )
            }

            // Interview Settings Section
            SettingsSection(title = "Interview Settings") {
                // Auto Questions Toggle
                SettingsToggleRow(
                    icon = Icons.Default.QuestionAnswer,
                    title = "Auto Questions",
                    description = "Automatically ask follow-up questions",
                    isChecked = viewModel.autoQuestionsEnabled,
                    onCheckedChange = { viewModel.updateAutoQuestions(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Question Interval
                SettingsSliderRow(
                    icon = Icons.Default.Timer,
                    title = "Question Interval",
                    value = viewModel.questionInterval,
                    valueRange = 30f..120f,
                    steps = 5,
                    valueLabel = "${viewModel.questionInterval}s",
                    onValueChange = { viewModel.updateQuestionInterval(it.toInt()) },
                    enabled = viewModel.autoQuestionsEnabled
                )
            }

            // Storage Section
            SettingsSection(title = "Storage") {
                StorageInfoRow(
                    storageUsed = viewModel.storageUsed,
                    sessionCount = viewModel.sessionCount
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showClearCacheDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Clear Cache")
                }
            }

            // About Section
            SettingsSection(title = "About") {
                AboutRow(
                    appName = Config.APP_NAME,
                    version = Config.APP_VERSION
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Clear Cache Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache?") },
            text = { Text("This will remove temporary files. Your recordings will not be affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCache()
                        showClearCacheDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear API Key Dialog
    if (showClearApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showClearApiKeyDialog = false },
            title = { Text("Remove API Key?") },
            text = { Text("AI-guided interviews will be disabled until you add a new key.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearApiKey()
                        showClearApiKeyDialog = false
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Settings section with title.
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .padding(16.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

/**
 * API key configuration row.
 */
@Composable
private fun ApiKeyRow(
    isConfigured: Boolean,
    onAddClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = if (isConfigured) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(24.dp)
            )

            Column {
                Text(
                    text = "Gemini API Key",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Text(
                    text = if (isConfigured) "Configured" else "Not configured",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConfigured) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }
        }

        if (isConfigured) {
            TextButton(onClick = onClearClick) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        } else {
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text("Add")
            }
        }
    }
}

/**
 * Toggle setting row.
 */
@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Slider setting row.
 */
@Composable
private fun SettingsSliderRow(
    icon: ImageVector,
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) Color.Gray else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) Color.White else Color.Gray
                )
            }

            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }

        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Storage info row.
 */
@Composable
private fun StorageInfoRow(
    storageUsed: String,
    sessionCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )

            Column {
                Text(
                    text = "Storage Used",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Text(
                    text = "$sessionCount sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        Text(
            text = storageUsed,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

/**
 * About row.
 */
@Composable
private fun AboutRow(
    appName: String,
    version: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = appName,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Text(
                text = "Version $version",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}
