package com.bitacora.digital.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

private const val GOOGLE_AI_STUDIO_URL = "https://aistudio.google.com/apikey"

/**
 * Onboarding data for each page.
 */
private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Default.Videocam,
        title = "Capture Your Story",
        description = "Record meaningful moments, memories, and life experiences with AI-guided interviews."
    ),
    OnboardingPage(
        icon = Icons.Default.Mic,
        title = "AI-Powered Interviews",
        description = "Our AI asks thoughtful follow-up questions to help you share richer, deeper stories."
    ),
    OnboardingPage(
        icon = Icons.AutoMirrored.Filled.List,
        title = "Multiple Session Types",
        description = "Choose from guided memories, wills, life experiences, or practice sessions."
    ),
    OnboardingPage(
        icon = Icons.Default.Star,
        title = "Powered by Gemini",
        description = "Uses Google's Gemini AI. You'll need a free API key to enable AI features."
    )
)

/**
 * Onboarding screen with carousel and API key setup.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { OnboardingViewModel.TOTAL_PAGES })
    val coroutineScope = rememberCoroutineScope()

    // Sync pager with viewModel
    LaunchedEffect(viewModel.currentPage) {
        if (pagerState.currentPage != viewModel.currentPage) {
            pagerState.animateScrollToPage(viewModel.currentPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (viewModel.currentPage != pagerState.currentPage) {
            viewModel.goToPage(pagerState.currentPage)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (viewModel.showApiKeySetup) {
            // API Key Setup
            ApiKeySetupContent(
                apiKeyInput = viewModel.apiKeyInput,
                onApiKeyChange = { viewModel.apiKeyInput = it },
                isValidating = viewModel.isValidatingKey,
                error = viewModel.validationError,
                onValidate = { viewModel.validateAndSaveApiKey() },
                onSkip = { viewModel.skipApiKey() },
                onClearError = { viewModel.clearError() }
            )
        } else {
            // Carousel
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Skip button
                if (viewModel.currentPage < OnboardingViewModel.TOTAL_PAGES - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(onClick = { viewModel.skip() }) {
                            Text("Skip", color = Color.Gray)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(64.dp))
                }

                // Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    OnboardingPageContent(
                        page = onboardingPages[page]
                    )
                }

                // Page indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(OnboardingViewModel.TOTAL_PAGES) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (index == pagerState.currentPage) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == pagerState.currentPage) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Gray.copy(alpha = 0.5f)
                                    }
                                )
                        )
                    }
                }

                // Navigation buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (viewModel.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(viewModel.currentPage - 1)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }
                    }

                    Button(
                        onClick = {
                            if (viewModel.currentPage == OnboardingViewModel.TOTAL_PAGES - 1) {
                                viewModel.nextPage()
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(viewModel.currentPage + 1)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (viewModel.currentPage == OnboardingViewModel.TOTAL_PAGES - 1) {
                                "Set Up API Key"
                            } else {
                                "Next"
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single onboarding page content.
 */
@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * API Key setup content.
 */
@Composable
private fun ApiKeySetupContent(
    apiKeyInput: String,
    onApiKeyChange: (String) -> Unit,
    isValidating: Boolean,
    error: String?,
    onValidate: () -> Unit,
    onSkip: () -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    var showKey by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Set Up API Key",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Get a free API key from Google AI Studio to enable AI-powered interviews.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Open AI Studio button
        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_AI_STUDIO_URL))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Open Google AI Studio")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // API Key input
        BasicTextField(
            value = apiKeyInput,
            onValueChange = {
                onApiKeyChange(it)
                onClearError()
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            visualTransformation = if (showKey) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        if (apiKeyInput.isEmpty()) {
                            Text(
                                text = "Paste your API key",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    }

                    IconButton(
                        onClick = { showKey = !showKey },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (showKey) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (showKey) "Hide" else "Show",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Error message
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Validate button
        Button(
            onClick = onValidate,
            enabled = apiKeyInput.isNotBlank() && !isValidating,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isValidating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Validating...")
            } else {
                Text("Continue")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Skip button
        TextButton(onClick = onSkip) {
            Text("Skip for now", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "You can add your API key later in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}
