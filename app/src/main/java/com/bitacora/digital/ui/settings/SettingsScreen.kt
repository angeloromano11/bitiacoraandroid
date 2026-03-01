package com.bitacora.digital.ui.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bitacora.digital.service.providers.AIProviderType
import com.bitacora.digital.util.Config

/**
 * Settings screen with AI provider selection, interview settings, and storage management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onApiKeySetupClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var providerToDelete by remember { mutableStateOf<AIProviderType?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            // AI Provider Section
            SettingsSection(title = "AI Provider") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AIProviderType.entries.forEach { provider ->
                        ProviderRow(
                            provider = provider,
                            isActive = viewModel.activeProvider == provider,
                            isConfigured = viewModel.providerStatuses[provider] == true,
                            onSelect = { viewModel.selectProvider(provider) },
                            onConfigure = { viewModel.selectedProviderForSetup = provider },
                            onClear = { providerToDelete = provider }
                        )
                    }
                }

                // Audio support note
                if (viewModel.activeProvider != null && viewModel.activeProvider != AIProviderType.GEMINI) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Audio-based questions use device speech recognition with this provider.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800)
                    )
                }
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

    // Provider Setup Bottom Sheet
    viewModel.selectedProviderForSetup?.let { provider ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissProviderSetup() },
            sheetState = sheetState,
            containerColor = Color(0xFF1A1A1A)
        ) {
            ProviderSetupSheet(
                provider = provider,
                isValidating = viewModel.isValidatingKey,
                error = viewModel.validationError,
                onSave = { key -> viewModel.saveApiKey(key, provider) },
                onDismiss = { viewModel.dismissProviderSetup() }
            )
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
    providerToDelete?.let { provider ->
        AlertDialog(
            onDismissRequest = { providerToDelete = null },
            title = { Text("Remove API Key?") },
            text = { Text("Remove the API key for ${provider.displayName}? You can add it again later.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearApiKey(provider)
                        providerToDelete = null
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { providerToDelete = null }) {
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
 * Provider selection row.
 */
@Composable
private fun ProviderRow(
    provider: AIProviderType,
    isActive: Boolean,
    isConfigured: Boolean,
    onSelect: () -> Unit,
    onConfigure: () -> Unit,
    onClear: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onSelect() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            RadioButton(
                selected = isActive,
                onClick = { onSelect() }
            )

            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = provider.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isConfigured) "Configured" else "Not configured",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isConfigured) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )

                    if (provider.supportsAudio) {
                        Text(
                            text = "Audio",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        if (isConfigured) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = Color.Gray
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Update Key") },
                        onClick = {
                            showMenu = false
                            onConfigure()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove Key", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onClear()
                        }
                    )
                }
            }
        } else {
            TextButton(onClick = onConfigure) {
                Text("Setup")
            }
        }
    }
}

/**
 * Provider API key setup sheet.
 */
@Composable
private fun ProviderSetupSheet(
    provider: AIProviderType,
    isValidating: Boolean,
    error: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Setup ${provider.displayName}",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Gray
                )
            }
        }

        // Step 1: Get API Key
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Step 1: Get Your API Key",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )

            Text(
                text = provider.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(provider.apiKeyUrl))
                    context.startActivity(intent)
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Get API Key")
            }
        }

        // Step 2: Enter API Key
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Step 2: Enter Your API Key",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Paste your API key") },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showKey) "Hide" else "Show"
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                )
            )

            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Save Button
        Button(
            onClick = { onSave(apiKey) },
            modifier = Modifier.fillMaxWidth(),
            enabled = apiKey.isNotBlank() && !isValidating
        ) {
            if (isValidating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Validating...")
            } else {
                Text("Validate & Save")
            }
        }

        // Privacy note
        Text(
            text = "Your API key is stored securely on your device.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )
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
