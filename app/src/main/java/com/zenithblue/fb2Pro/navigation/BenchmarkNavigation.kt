package com.zenithblue.fb2Pro.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zenithblue.fb2Pro.ui.screens.*

@Composable
fun BenchmarkNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "welcome",
        modifier = modifier
    ) {
        composable("welcome") {
            WelcomeScreen(
                onNextClicked = {
                    navController.navigate("benchmark")
                }
            )
        }
        
        composable("benchmark") {
            val context = androidx.compose.ui.platform.LocalContext.current
            val historyRepository = androidx.compose.runtime.remember {
                 com.zenithblue.fb2Pro.data.repository.HistoryRepository(
                     com.zenithblue.fb2Pro.data.database.AppDatabase.getDatabase(context).benchmarkDao()
                 )
            }
            BenchmarkScreen(
                preset = "Auto",
                onBenchmarkComplete = { summaryJson ->
                    navController.navigate("result/$summaryJson")
                },
                onNavBack = { navController.popBackStack() },
                historyRepository = historyRepository
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
                    navController.navigate("welcome")
                }
            )
        }
    }
}