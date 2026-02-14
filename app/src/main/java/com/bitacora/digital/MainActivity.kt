package com.bitacora.digital

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.bitacora.digital.ui.navigation.BitacoraNavHost
import com.bitacora.digital.ui.onboarding.OnboardingScreen
import com.bitacora.digital.ui.onboarding.OnboardingViewModel
import com.bitacora.digital.ui.theme.BitacoraTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity - single activity host for all Compose screens.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BitacoraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RootContent()
                }
            }
        }
    }
}

/**
 * Root content that decides between onboarding and main app.
 */
@Composable
private fun RootContent(
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    when (viewModel.isOnboardingComplete) {
        null -> {
            // Loading state while checking DataStore
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        false -> {
            // First-time user: show onboarding
            OnboardingScreen(viewModel = viewModel)
        }
        true -> {
            // Returning user: show main app
            BitacoraNavHost()
        }
    }
}
