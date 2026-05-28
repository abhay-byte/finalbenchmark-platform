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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
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
import com.ivarna.finalbenchmark2.ui.screens.ModelDownloadDialog
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

    // AI model download dialog state — shown before Full or AI Benchmark starts
    var showModelDownloadDialog by remember { mutableStateOf(false) }
    var pendingFullBenchmarkPreset by remember { mutableStateOf("") }
    var pendingBenchmarkType by remember { mutableStateOf("FULL") }

    // Show model download dialog if needed
    if (showModelDownloadDialog) {
        ModelDownloadDialog(
            onDismiss = {
                showModelDownloadDialog = false
                pendingFullBenchmarkPreset = ""
                pendingBenchmarkType = "FULL"
            },
            onProceed = {
                showModelDownloadDialog = false
                val preset = pendingFullBenchmarkPreset
                val benchType = pendingBenchmarkType
                pendingFullBenchmarkPreset = ""
                pendingBenchmarkType = "FULL"
                navController.navigate("benchmark/$preset/$benchType")
            }
        )
    }

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
            bottomBar = {} // Empty bottomBar to prevent reservation of space
    ) { innerPadding ->
        Box(
                modifier =
                        Modifier.fillMaxSize()
        ) {
            NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier
                        .padding(innerPadding)
                        .haze(state = hazeState)
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

                // Unified Transitions - Optimized Slide
                val commonSpec = androidx.compose.animation.core.tween<androidx.compose.ui.unit.IntOffset>(
                    durationMillis = 350,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )

                // Helper to determine screen index for sliding direction
                fun getRouteIndex(route: String?): Int {
                    return when (route) {
                        "home" -> 0
                        "device" -> 1
                        "rankings" -> 2
                        "history" -> 3
                        else -> -1
                    }
                }

                val enterTransition: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition = {
                    val initialIndex = getRouteIndex(initialState.destination.route)
                    val targetIndex = getRouteIndex(targetState.destination.route)
                    
                    if (initialIndex != -1 && targetIndex != -1) {
                        // Tab to Tab
                        if (targetIndex > initialIndex) {
                            androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }, animationSpec = commonSpec)
                        } else {
                            androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it }, animationSpec = commonSpec)
                        }
                    } else {
                        // To Detail or others
                        androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }, animationSpec = commonSpec)
                    }
                }
                
                val exitTransition: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition = {
                    val initialIndex = getRouteIndex(initialState.destination.route)
                    val targetIndex = getRouteIndex(targetState.destination.route)
                    
                    if (initialIndex != -1 && targetIndex != -1) {
                         // Tab to Tab
                        if (targetIndex > initialIndex) {
                            androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it }, animationSpec = commonSpec)
                        } else {
                            androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }, animationSpec = commonSpec)
                        }
                    } else {
                        // From Detail or others
                        androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it }, animationSpec = commonSpec)

                    }
                }
                
                val popEnterTransition: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition = {
                    val initialIndex = getRouteIndex(initialState.destination.route)
                    val targetIndex = getRouteIndex(targetState.destination.route)
                    
                    if (initialIndex != -1 && targetIndex != -1) {
                         // Back within Tabs (if applicable)
                        if (targetIndex > initialIndex) {
                             androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }, animationSpec = commonSpec)
                        } else {
                             androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it }, animationSpec = commonSpec)
                        }
                    } else {
                         // Back from Detail
                        androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it }, animationSpec = commonSpec)
                    }
                }
                
                val popExitTransition: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition = {
                    val initialIndex = getRouteIndex(initialState.destination.route)
                    val targetIndex = getRouteIndex(targetState.destination.route)
                    
                    if (initialIndex != -1 && targetIndex != -1) {
                         // Back within Tabs
                        if (targetIndex > initialIndex) {
                             androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it }, animationSpec = commonSpec)
                        } else {
                             androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }, animationSpec = commonSpec)
                        }
                    } else {
                        // Back from Detail (Detail leaving)
                        androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }, animationSpec = commonSpec)
                    }
                }

                composable(
                    route = "home",
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    popEnterTransition = popEnterTransition,
                    popExitTransition = popExitTransition
                ) {
                    val historyRepository = remember {
                        HistoryRepository(
                            AppDatabase.getDatabase(context).benchmarkDao()
                        )
                    }
                    HomeScreen(
                            onStartBenchmark = { preset, type ->
                                if (type == "FULL" || type == "AI") {
                                    // Both FULL and standalone AI benchmarks require models
                                    val areModelsReady = com.ivarna.finalbenchmark2.aiBenchmark.ModelDownloader.areAllModelsDownloaded(context)
                                    if (areModelsReady) {
                                        navController.navigate("benchmark/$preset/$type")
                                    } else {
                                        // Show AI model download dialog before starting
                                        pendingFullBenchmarkPreset = preset
                                        pendingBenchmarkType = type
                                        showModelDownloadDialog = true
                                    }
                                } else {
                                    navController.navigate("benchmark/$preset/$type")
                                }
                            },
                            onNavigateToSettings = { navController.navigate("settings") },
                            historyRepository = historyRepository,
                            hazeState = hazeState // Pass hazeState
                    )
                }
                composable(
                    route = "device",
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    popEnterTransition = popEnterTransition,
                    popExitTransition = popExitTransition
                ) { DeviceScreen() }
                
                composable(
                    route = "rankings",
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    popEnterTransition = popEnterTransition,
                    popExitTransition = popExitTransition
                ) {
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
                composable(
                    route = "cpu-comparison/{deviceData}",
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    popEnterTransition = popEnterTransition,
                    popExitTransition = popExitTransition
                ) { backStackEntry ->
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
                composable(
                    route = "history",
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    popEnterTransition = popEnterTransition,
                    popExitTransition = popExitTransition
                ) {
                    val historyViewModel: com.ivarna.finalbenchmark2.ui.viewmodels.HistoryViewModel =
                            viewModel(
                                    factory =
                                            com.ivarna.finalbenchmark2.di.DatabaseInitializer
                                                    .HistoryViewModelFactory(context)
                            )
                    HistoryScreen(viewModel = historyViewModel, navController = navController)
                }
                composable(
                    route = "settings",
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    popEnterTransition = popEnterTransition,
                    popExitTransition = popExitTransition
                ) {
                    SettingsScreen(
                            rootStatus = rootStatus,
                            onBackClick = { navController.popBackStack() },
                            onNavigateToOnboarding = {
                                navController.navigate("welcome") {
                                    // Clear the back stack up to home
                                    popUpTo("home") { inclusive = false }
                                }
                            },
                            onNavigateToLastResult = {
                                navController.navigate("last-result")
                            }
                    )
                }
                composable(
                    route = "last-result",
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    popEnterTransition = popEnterTransition,
                    popExitTransition = popExitTransition
                ) {
                    val historyRepository = remember {
                        HistoryRepository(
                            AppDatabase.getDatabase(context).benchmarkDao()
                        )
                    }
                    com.ivarna.finalbenchmark2.ui.screens.FullBenchmarkLastResultScreen(
                        historyRepository = historyRepository,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                // Update benchmark route to accept type
                composable(
                    route = "benchmark/{preset}/{type}",
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    popEnterTransition = popEnterTransition,
                    popExitTransition = popExitTransition
                ) { backStackEntry ->
                    val preset = backStackEntry.arguments?.getString("preset") ?: "Auto"
                    val typeString = backStackEntry.arguments?.getString("type") ?: "CPU"
                    val category = try {
                        com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory.valueOf(typeString)
                    } catch (e: Exception) {
                        com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory.CPU
                    }
                    
                    // GPU benchmark gets its own dedicated rendering screen
                    if (category == com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory.GPU) {
                        val gpuHistoryRepository = remember {
                            com.ivarna.finalbenchmark2.data.repository.HistoryRepository(
                                com.ivarna.finalbenchmark2.data.database.AppDatabase
                                    .getDatabase(context)
                                    .benchmarkDao()
                            )
                        }
                        GpuBenchmarkScreen(
                            preset = preset,
                            historyRepository = gpuHistoryRepository,
                            onBenchmarkComplete = { summaryJson ->
                                val encodedJson = java.net.URLEncoder.encode(summaryJson, "UTF-8")
                                navController.navigate("result/$encodedJson") {
                                    popUpTo("home") { inclusive = false }
                                }
                            },
                            onNavBack = { navController.popBackStack() }
                        )
                    } else if (category == com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory.FULL) {
                        // ── Full Benchmark ─ runs all categories sequentially ──────────
                        val fullHistoryRepository = remember {
                            com.ivarna.finalbenchmark2.data.repository.HistoryRepository(
                                com.ivarna.finalbenchmark2.data.database.AppDatabase
                                    .getDatabase(context)
                                    .benchmarkDao()
                            )
                        }
                        com.ivarna.finalbenchmark2.ui.screens.FullBenchmarkScreen(
                            preset = preset,
                            historyRepository = fullHistoryRepository,
                            onBenchmarkComplete = { summaryJson ->
                                val encodedJson = java.net.URLEncoder.encode(summaryJson, "UTF-8")
                                navController.navigate("result/$encodedJson") {
                                    popUpTo("home") { inclusive = false }
                                }
                            },
                            onNavBack = { navController.popBackStack() }
                        )
                    } else if (category == com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory.STORAGE) {
                        // Storage benchmark — dedicated screen with I/O throughput tests
                        val storageHistoryRepository = remember {
                            com.ivarna.finalbenchmark2.data.repository.HistoryRepository(
                                com.ivarna.finalbenchmark2.data.database.AppDatabase
                                    .getDatabase(context)
                                    .benchmarkDao()
                            )
                        }
                        com.ivarna.finalbenchmark2.ui.screens.StorageBenchmarkScreen(
                            preset = preset,
                            historyRepository = storageHistoryRepository,
                            onBenchmarkComplete = { summaryJson ->
                                val encodedJson = java.net.URLEncoder.encode(summaryJson, "UTF-8")
                                navController.navigate("result/$encodedJson") {
                                    popUpTo("home") { inclusive = false }
                                }
                            },
                            onNavBack = { navController.popBackStack() }
                        )
                    } else if (category == com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory.RAM) {
                        // RAM benchmark — dedicated screen with memory bandwidth / latency tests
                        val ramHistoryRepository = remember {
                            com.ivarna.finalbenchmark2.data.repository.HistoryRepository(
                                com.ivarna.finalbenchmark2.data.database.AppDatabase
                                    .getDatabase(context)
                                    .benchmarkDao()
                            )
                        }
                        RamBenchmarkScreen(
                            preset = preset,
                            historyRepository = ramHistoryRepository,
                            onBenchmarkComplete = { summaryJson ->
                                val encodedJson = java.net.URLEncoder.encode(summaryJson, "UTF-8")
                                navController.navigate("result/$encodedJson") {
                                    popUpTo("home") { inclusive = false }
                                }
                            },
                            onNavBack = { navController.popBackStack() }
                        )
                    } else if (category == com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory.PRODUCTIVITY) {
                        // Productivity benchmark — CPU-bound tasks: canvas, image, text, JSON, compression
                        val productivityHistoryRepository = remember {
                            com.ivarna.finalbenchmark2.data.repository.HistoryRepository(
                                com.ivarna.finalbenchmark2.data.database.AppDatabase
                                    .getDatabase(context)
                                    .benchmarkDao()
                            )
                        }
                        com.ivarna.finalbenchmark2.ui.screens.ProductivityBenchmarkScreen(
                            preset = preset,
                            historyRepository = productivityHistoryRepository,
                            onBenchmarkComplete = { summaryJson ->
                                val encodedJson = java.net.URLEncoder.encode(summaryJson, "UTF-8")
                                navController.navigate("result/$encodedJson") {
                                    popUpTo("home") { inclusive = false }
                                }
                            },
                            onNavBack = { navController.popBackStack() }
                        )
                    } else {
                        val historyRepository =
                                com.ivarna.finalbenchmark2.data.repository.HistoryRepository(
                                        com.ivarna.finalbenchmark2.data.database.AppDatabase
                                                .getDatabase(context)
                                                .benchmarkDao()
                                )
                        val activity = context as? com.ivarna.finalbenchmark2.MainActivity
                        BenchmarkScreen(
                                preset = preset,
                                benchmarkCategory = category,
                                onBenchmarkComplete = { summaryJson ->
                                    // URL-encode the JSON to handle special characters properly
                                    val encodedJson = java.net.URLEncoder.encode(summaryJson, "UTF-8")
                                    navController.navigate("result/$encodedJson") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                },
                                onBenchmarkStart = { activity?.startAllOptimizations() },
                                onBenchmarkEnd = { activity?.stopAllOptimizations() },
                                onNavBack = { navController.popBackStack() },
                                historyRepository = historyRepository
                        )
                    }
                }
                composable(
                    route = "result/{summaryJson}",
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    popEnterTransition = popEnterTransition,
                    popExitTransition = popExitTransition
                ) { backStackEntry ->
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
                                // Determine type from summaryJson to restart correct benchmark
                                val isAi = summaryJson.contains("\"type\":\"AI\"") || summaryJson.contains("\"type\": \"AI\"")
                                val type = if (isAi) "AI" else "CPU"
                                navController.popBackStack()
                                navController.navigate("benchmark/Auto/$type")
                            },
                            onBackToHome = {
                                navController.popBackStack()
                            },
                            onShowDetailedResults = { detailedResults ->
                                // URL-encode the JSON to handle special characters properly
                                val encodedJson = java.net.URLEncoder.encode(summaryJson, "UTF-8")
                                navController.navigate("detailed-results/$encodedJson")
                            },
                            historyRepository = historyRepository,
                            hazeState = hazeState
                    )
                }
                composable(
                    route = "detailed-results/{summaryJson}",
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    popEnterTransition = popEnterTransition,
                    popExitTransition = popExitTransition
                ) { backStackEntry ->
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

            // Floating Navigation Bar Overlay with Elegant Animation
            androidx.compose.animation.AnimatedVisibility(
                visible = showBottomBar,
                enter = androidx.compose.animation.slideInVertically(
                    initialOffsetY = { it }, // Slide in from bottom
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 600,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                ) + androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 600)
                ),
                exit = androidx.compose.animation.slideOutVertically(
                    targetOffsetY = { it }, // Slide out to bottom
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 600,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                ) + androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 600)
                ),
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(1f)
            ) {
                FrostedGlassNavigationBar(
                    items = bottomNavigationItems,
                    navController = navController,
                    hazeState = hazeState,
                    modifier = Modifier
                        .padding(bottom = innerPadding.calculateBottomPadding())
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
