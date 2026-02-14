package com.bitacora.digital.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bitacora.digital.ui.assistant.MemoryAssistantScreen
import com.bitacora.digital.ui.detail.SessionDetailScreen
import com.bitacora.digital.ui.library.LibraryScreen
import com.bitacora.digital.ui.recording.RecordingScreen
import com.bitacora.digital.ui.settings.ApiKeySetupScreen
import com.bitacora.digital.ui.settings.SettingsScreen

/**
 * Navigation destinations.
 */
sealed class NavRoute(val route: String) {
    object Main : NavRoute("main")
    object SessionDetail : NavRoute("detail/{sessionId}") {
        fun createRoute(sessionId: String) = "detail/$sessionId"
    }
    object Settings : NavRoute("settings")
    object ApiKeySetup : NavRoute("apiKeySetup")
}

/**
 * Bottom navigation tabs.
 */
data class NavTab(
    val title: String,
    val icon: ImageVector
)

val navTabs = listOf(
    NavTab("Record", Icons.Default.Videocam),
    NavTab("Library", Icons.Default.Folder),
    NavTab("Ask", Icons.Default.Chat),
    NavTab("Settings", Icons.Default.Settings)
)

/**
 * Main navigation host.
 */
@Composable
fun BitacoraNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NavRoute.Main.route
    ) {
        composable(NavRoute.Main.route) {
            MainScreen(navController)
        }
        composable(
            route = NavRoute.SessionDetail.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) {
            SessionDetailScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(NavRoute.Settings.route) {
            SettingsScreen(
                onApiKeySetupClick = {
                    navController.navigate(NavRoute.ApiKeySetup.route)
                }
            )
        }
        composable(NavRoute.ApiKeySetup.route) {
            ApiKeySetupScreen(
                onBackClick = { navController.popBackStack() },
                onComplete = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Main screen with bottom navigation.
 */
@Composable
fun MainScreen(navController: NavHostController) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                navTabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> RecordingScreen()
                1 -> LibraryScreen(
                    onSessionClick = { sessionId ->
                        navController.navigate(NavRoute.SessionDetail.createRoute(sessionId))
                    },
                    onSettingsClick = {
                        navController.navigate(NavRoute.Settings.route)
                    }
                )
                2 -> MemoryAssistantScreen()
                3 -> SettingsScreen(
                    onApiKeySetupClick = {
                        navController.navigate(NavRoute.ApiKeySetup.route)
                    }
                )
            }
        }
    }
}

/**
 * Placeholder screen for unimplemented features.
 */
@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.padding(androidx.compose.foundation.layout.PaddingValues(16.dp)),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = "$title - Coming Soon",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
    }
}

private val Int.dp: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(this.toFloat())
