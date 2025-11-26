package com.ivarna.finalbenchmark2.navigation

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ivarna.finalbenchmark2.ui.screens.*

@Composable
fun MainNavigation(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route
    val context = LocalContext.current

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
                NavigationBar {
                    bottomNavigationItems.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                when (item.route) {
                                    "device" -> Icon(
                                        painter = androidx.compose.ui.res.painterResource(id = com.ivarna.finalbenchmark2.R.drawable.mobile_24),
                                        contentDescription = item.label,
                                        tint = if (currentRoute == item.route) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    "history" -> Icon(
                                        painter = androidx.compose.ui.res.painterResource(id = com.ivarna.finalbenchmark2.R.drawable.history_24),
                                        contentDescription = item.label,
                                        tint = if (currentRoute == item.route) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    else -> Icon(item.icon, contentDescription = item.label)
                                }
                            },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    // Avoid multiple copies of the same destination
                                    launchSingleTop = true
                                    // Restore the state when navigating back
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
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

// Data class for bottom navigation items
data class BottomNavigationItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)