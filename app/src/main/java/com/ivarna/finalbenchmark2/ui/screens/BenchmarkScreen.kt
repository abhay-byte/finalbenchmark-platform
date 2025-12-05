package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkEvent
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkManager
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.ui.viewmodels.BenchmarkProgress
import com.ivarna.finalbenchmark2.ui.viewmodels.BenchmarkState
import com.ivarna.finalbenchmark2.ui.viewmodels.BenchmarkViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.SystemMonitorViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.rememberLazyListState
import com.ivarna.finalbenchmark2.ui.models.SystemStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(
    preset: String = "Auto",
    onBenchmarkComplete: (String) -> Unit,
    viewModel: BenchmarkViewModel = viewModel(),
    systemMonitorViewModel: SystemMonitorViewModel = viewModel()
) {
    val benchmarkState by viewModel.benchmarkState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val systemStats by systemMonitorViewModel.systemStats.collectAsState()
    val benchmarkManager = remember { BenchmarkManager() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Collect benchmark events
    LaunchedEffect(Unit) {
        benchmarkManager.benchmarkEvents.collectLatest { event ->
            // We're now using the new uiState instead of individual events
        }
    }
    
    // Start monitoring when the screen is launched
    LaunchedEffect(Unit) {
        systemMonitorViewModel.startMonitoring()
    }
    
    // Stop monitoring when the screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            systemMonitorViewModel.stopMonitoring()
        }
    }
    
    // Handle benchmark state changes
    LaunchedEffect(Unit) {
        viewModel.benchmarkState.collectLatest { state ->
            when (state) {
                is BenchmarkState.Completed -> {
                    // Convert BenchmarkResults to JSON string for navigation
                    // Include detailed results as well
                    val detailedResultsJson = state.results.individualScores.map { result ->
                        // Properly escape the metrics JSON for inclusion in the parent JSON
                        val escapedMetricsJson = result.metricsJson
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r")
                            .replace("\t", "\\t")
                        
                        """{
                            "name": "${result.name.replace("\"", "\\\"")}",
                            "executionTimeMs": ${result.executionTimeMs},
                            "opsPerSecond": ${result.opsPerSecond},
                            "isValid": ${result.isValid},
                            "metricsJson": "$escapedMetricsJson"
                        }"""
                    }.joinToString(",\n")
                    
                    val summaryJson = """{
                        "single_core_score": ${state.results.singleCoreScore},
                        "multi_core_score": ${state.results.multiCoreScore},
                        "final_score": ${state.results.finalWeightedScore},
                        "normalized_score": ${state.results.normalizedScore},
                        "rating": "${determineRating(state.results.normalizedScore)}",
                        "detailed_results": [
                            $detailedResultsJson
                        ]
                    }"""
                    onBenchmarkComplete(summaryJson)
                }
                is BenchmarkState.Error -> {
                    // In case of error, still navigate to results with default data
                    onBenchmarkComplete("""{
                        "single_core_score": 0.0,
                        "multi_core_score": 0.0,
                        "final_score": 0.0,
                        "normalized_score": 0.0,
                        "rating": "★"
                    }""")
                }
                else -> {
                    // Handle other states if needed
                }
            }
        }
    }
    
    // Start benchmark when the screen is launched
    LaunchedEffect(Unit) {
        viewModel.startBenchmark(preset)
    }
    
    // Auto-scroll to the bottom when new items are added
    LaunchedEffect(uiState.completedTests.size) {
        if (uiState.completedTests.isNotEmpty()) {
            listState.animateScrollToItem(uiState.completedTests.size - 1)
        }
    }
    
    FinalBenchmark2Theme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = WindowInsets.statusBars.getTop(LocalDensity.current).dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
            ) {
                // Top Section (Overall Progress)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header
                    Text(
                        text = "Running Benchmarks",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    // Circular progress indicator
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        CircularProgressIndicator(
                            progress = { uiState.progress },
                            strokeWidth = 8.dp,
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${(uiState.progress * 100).toInt()}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Current test name
                    Text(
                        text = "Current: ${uiState.currentTestName}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
                
                // Middle Section (Test Log / Console)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    items(uiState.completedTests) { test ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Completed",
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = test.name,
                                modifier = Modifier.weight(1f),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${test.executionTimeMs.toInt()}ms",
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bottom Section (System Dashboard)
                SystemMonitorCard(stats = systemStats)
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SystemMonitorCard(stats: SystemStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MonitorItem(
                icon = Icons.Rounded.Memory,
                value = "${stats.cpuLoad.toInt()}%",
                label = "CPU"
            )
            MonitorItem(
                icon = Icons.Rounded.Bolt,
                value = "${String.format("%.2f", stats.power)}W",
                label = "Power"
            )
            MonitorItem(
                icon = Icons.Rounded.Thermostat,
                value = "${stats.temp.toInt()}°C",
                label = "Temp"
            )
        }
    }
}

@Composable
fun MonitorItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
* Determines the rating based on the normalized score
*/
fun determineRating(normalizedScore: Double): String {
  return when {
      normalizedScore >= 80000 -> "★★★★★"
      normalizedScore >= 60000 -> "★★☆"
      normalizedScore >= 40000 -> "★★★☆☆"
      normalizedScore >= 20000 -> "★★☆☆☆"
      normalizedScore >= 10000 -> "★☆☆☆"
      else -> "☆☆☆☆☆"
  }
}

@Composable
fun BenchmarkEventCard(event: BenchmarkEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.testName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusColor = when (event.state) {
                        "PENDING" -> MaterialTheme.colorScheme.onSurfaceVariant
                        "RUNNING" -> MaterialTheme.colorScheme.primary
                        "COMPLETED" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    
                    val statusText = when (event.state) {
                        "PENDING" -> "Pending"
                        "RUNNING" -> "Running"
                        "COMPLETED" -> "Completed"
                        else -> event.state
                    }
                    
                    Text(
                        text = "$statusText | ${event.mode.lowercase().capitalize()}",
                        color = statusColor,
                        fontSize = 14.sp
                    )
                    
                    if (event.state == "COMPLETED") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${event.timeMs}ms | Score: ${String.format("%.2f", event.score)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            // Status indicator
            when (event.state) {
                "PENDING" -> {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                "RUNNING" -> {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                "COMPLETED" -> {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> {
                    Text(text = event.state)
                }
            }
        }
    }
}