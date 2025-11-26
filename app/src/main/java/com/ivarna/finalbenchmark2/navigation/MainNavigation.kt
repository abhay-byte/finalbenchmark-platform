package com.ivarna.finalbenchmark2.navigation

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ivarna.finalbenchmark2.ui.screens.*
import com.ivarna.finalbenchmark2.navigation.FrostedGlassNavigationBar
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun MainNavigation(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route
    val context = LocalContext.current
    
    // Create HazeState for blur effect
    val hazeState = remember { HazeState() }

    // Define the bottom navigation items with custom icons
    val bottomNavigationItems = listOf(
        BottomNavigationItem(
            route = "home",
            icon = Icons.Default.Home,
            label = "Home"
        ),
        BottomNavigationItem(
            route = "device",
            icon = Icons.Default.Phone,
            label = "Device"
        ),
        BottomNavigationItem(
            route = "history",
            icon = Icons.Default.List,
            label = "History"
        ),
        BottomNavigationItem(
            route = "settings",
            icon = Icons.Default.Settings,
            label = "Settings"
        )
    )

    // Check if current route should show bottom navigation
    val showBottomBar = currentRoute in bottomNavigationItems.map { it.route }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                FrostedGlassNavigationBar(
                    items = bottomNavigationItems,
                    navController = navController,
                    hazeState = hazeState // Pass the HazeState
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState) // Apply haze to content that should be blurred
        ) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("home") {
                    HomeScreen(
                        onStartBenchmark = {
                            navController.navigate("benchmark")
                        }
                    )
                }
                composable("device") {
                    DeviceScreen()
                }
                composable("history") {
                    HistoryScreen()
                }
                composable("settings") {
                    SettingsScreen()
                }
                // Keep the existing benchmark flow
                composable("benchmark") {
                    BenchmarkScreen(
                        onBenchmarkComplete = { summaryJson ->
                            navController.navigate("result/$summaryJson")
                        }
                    )
                }
                composable("result/{summaryJson}") { backStackEntry ->
                    val summaryJson = backStackEntry.arguments?.getString("summaryJson") ?: "{}"
                    ResultScreen(
                        summaryJson = summaryJson,
                        onRunAgain = {
                            navController.popBackStack()
                            navController.navigate("benchmark")
                        },
                        onBackToHome = {
                            navController.popBackStack()
                            navController.navigate("home")
                        }
                    )
                }
            }
        }
    }
}

// Data class for bottom navigation items
data class BottomNavigationItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)