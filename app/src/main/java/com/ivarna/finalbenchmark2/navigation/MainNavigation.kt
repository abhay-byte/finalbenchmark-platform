package com.ivarna.finalbenchmark2.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ivarna.finalbenchmark2.data.database.AppDatabase
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.ivarna.finalbenchmark2.ui.screens.*
import com.ivarna.finalbenchmark2.ui.screens.DetailedResultScreen
import com.ivarna.finalbenchmark2.ui.viewmodels.RootStatus
import com.ivarna.finalbenchmark2.utils.OnboardingPreferences
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun MainNavigation(
        modifier: Modifier = Modifier,
        hazeState: HazeState, // Accept hazeState as parameter, no default
        rootStatus: RootStatus = RootStatus.NO_ROOT // Root status from MainViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route
    val context = LocalContext.current

    // Check if onboarding is completed
    val onboardingPreferences: OnboardingPreferences = remember { OnboardingPreferences(context) }
    val isOnboardingCompleted: Boolean = onboardingPreferences.isOnboardingCompleted()

    // Set start destination based on onboarding status
    val startDestination = if (isOnboardingCompleted) "home" else "welcome"

    // Define the bottom navigation items with custom icons
    val bottomNavigationItems =
            listOf(
                    BottomNavigationItem(route = "home", icon = Icons.Default.Home, label = "Home"),
                    BottomNavigationItem(
                            route = "device",
                            icon = Icons.Default.Phone,
                            label = "Device"
                    ),
                    BottomNavigationItem(
                            route = "rankings",
                            icon = Icons.Rounded.Leaderboard,
                            label = "Rankings"
                    ),
                    BottomNavigationItem(
                            route = "history",
                            icon = Icons.Default.List,
                            label = "History"
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
                modifier =
                        Modifier.fillMaxSize()
                                .haze(
                                        state = hazeState
                                ) // Apply haze to content that should be blurred
        ) {
            NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.padding(innerPadding)
            ) {
                composable("welcome") {
                    OnboardingPagerScreen(
                            onOnboardingComplete = {
                                // Clear onboarding stack and go to home
                                navController.navigate("home") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            }
                    )
                }

                composable("home") {
                    val historyRepository = remember {
                        HistoryRepository(
                            AppDatabase.getDatabase(context).benchmarkDao()
                        )
                    }
                    HomeScreen(
                            onStartBenchmark = { preset ->
                                navController.navigate("benchmark/$preset")
                            },
                            onNavigateToSettings = { navController.navigate("settings") },
                            historyRepository = historyRepository
                    )
                }
                composable("device") { DeviceScreen() }
                composable("rankings") {
                    RankingsScreen(
                        onDeviceClick = { selectedItem ->
                            val encodedData = java.net.URLEncoder.encode(
                                com.google.gson.Gson().toJson(selectedItem),
                                "UTF-8"
                            )
                            navController.navigate("cpu-comparison/$encodedData")
                        }
                    )
                }
                composable("cpu-comparison/{deviceData}") { backStackEntry ->
                    val encodedData = backStackEntry.arguments?.getString("deviceData") ?: "{}"
                    val decodedData = try {
                        java.net.URLDecoder.decode(encodedData, "UTF-8")
                    } catch (e: Exception) {
                        encodedData
                    }
                    val historyRepository =
                            com.ivarna.finalbenchmark2.data.repository.HistoryRepository(
                                    com.ivarna.finalbenchmark2.data.database.AppDatabase
                                            .getDatabase(context)
                                            .benchmarkDao()
                            )
                    CpuComparisonScreen(
                        selectedDeviceJson = decodedData,
                        historyRepository = historyRepository,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable("history") {
                    val historyViewModel =
                            com.ivarna.finalbenchmark2.di.DatabaseInitializer
                                    .createHistoryViewModel(context)
                    HistoryScreen(viewModel = historyViewModel, navController = navController)
                }
                composable("settings") {
                    SettingsScreen(
                            rootStatus = rootStatus,
                            onBackClick = { navController.popBackStack() },
                            onNavigateToOnboarding = {
                                navController.navigate("welcome") {
                                    // Clear the back stack up to home
                                    popUpTo("home") { inclusive = false }
                                }
                            }
                    )
                }
                // Keep the existing benchmark flow - consolidate into one route
                composable("benchmark/{preset}") { backStackEntry ->
                    val preset = backStackEntry.arguments?.getString("preset") ?: "Auto"
                    val historyRepository =
                            com.ivarna.finalbenchmark2.data.repository.HistoryRepository(
                                    com.ivarna.finalbenchmark2.data.database.AppDatabase
                                            .getDatabase(context)
                                            .benchmarkDao()
                            )
                    val activity = context as? com.ivarna.finalbenchmark2.MainActivity
                    BenchmarkScreen(
                            preset = preset,
                            onBenchmarkComplete = { summaryJson ->
                                // URL-encode the JSON to handle special characters properly
                                val encodedJson = java.net.URLEncoder.encode(summaryJson, "UTF-8")
                                navController.navigate("result/$encodedJson")
                            },
                            onBenchmarkStart = { activity?.startAllOptimizations() },
                            onBenchmarkEnd = { activity?.stopAllOptimizations() },
                            historyRepository = historyRepository
                    )
                }
                composable("result/{summaryJson}") { backStackEntry ->
                    val encodedSummaryJson =
                            backStackEntry.arguments?.getString("summaryJson") ?: "{}"
                    // URL-decode the JSON to handle special characters properly
                    val summaryJson =
                            try {
                                java.net.URLDecoder.decode(encodedSummaryJson, "UTF-8")
                            } catch (e: Exception) {
                                // Fallback to original if decoding fails
                                encodedSummaryJson
                            }
                    val historyRepository =
                            com.ivarna.finalbenchmark2.data.repository.HistoryRepository(
                                    com.ivarna.finalbenchmark2.data.database.AppDatabase
                                            .getDatabase(context)
                                            .benchmarkDao()
                            )
                    ResultScreen(
                            summaryJson = summaryJson,
                            onRunAgain = {
                                navController.popBackStack()
                                navController.navigate("benchmark/Auto")
                            },
                            onBackToHome = {
                                navController.popBackStack()
                                navController.navigate("home")
                            },
                            onShowDetailedResults = { detailedResults ->
                                // URL-encode the JSON to handle special characters properly
                                val encodedJson = java.net.URLEncoder.encode(summaryJson, "UTF-8")
                                navController.navigate("detailed-results/$encodedJson")
                            },
                            historyRepository = historyRepository
                    )
                }
                composable("detailed-results/{summaryJson}") { backStackEntry ->
                    val encodedSummaryJson =
                            backStackEntry.arguments?.getString("summaryJson") ?: "{}"
                    // URL-decode the JSON to handle special characters properly
                    val decodedSummaryJson =
                            try {
                                java.net.URLDecoder.decode(encodedSummaryJson, "UTF-8")
                            } catch (e: Exception) {
                                // Fallback to original if decoding fails
                                encodedSummaryJson
                            }
                    DetailedResultScreen(
                            summaryJson = decodedSummaryJson,
                            onBack = { navController.popBackStack() }
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
