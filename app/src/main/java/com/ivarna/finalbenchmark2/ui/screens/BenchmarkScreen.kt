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
    viewModel: BenchmarkViewModel = viewModel()
) {
    val benchmarkState by viewModel.benchmarkState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    // Use the throttled system stats from the BenchmarkViewModel instead of SystemMonitorViewModel
    val systemStats by viewModel.throttledSystemStats.collectAsState()
    val benchmarkManager = remember { BenchmarkManager() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Removed unnecessary LaunchedEffect that was collecting benchmarkManager.benchmarkEvents
    // The ViewModel now handles event accumulation properly
    
    // Handle benchmark state changes
    LaunchedEffect(Unit) {
        viewModel.benchmarkState.collect { state ->
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
    
    // Auto-scroll to the currently running test
    LaunchedEffect(uiState.allTestStates) { // Watch the entire list for changes
        val runningIndex = uiState.allTestStates.indexOfFirst {
            it.status == com.ivarna.finalbenchmark2.ui.viewmodels.TestStatus.RUNNING
        }
        if (runningIndex >= 0) {
            listState.animateScrollToItem(runningIndex)
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
                    items(uiState.allTestStates, key = { "${it.name}_${it.status}" }) { testState ->
                        TestStateItem(testState = testState)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bottom Section (System Dashboard) - now using throttled system stats
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
      normalizedScore >= 800 -> "★★★"
      normalizedScore >= 60000 -> "★★☆"
      normalizedScore >= 40000 -> "★★★☆☆"
      normalizedScore >= 2000 -> "★★☆☆☆"
      normalizedScore >= 10000 -> "★☆☆☆"
      else -> "☆☆☆☆☆"
  }
}

@Composable
fun TestStateItem(testState: com.ivarna.finalbenchmark2.ui.viewmodels.TestState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status icon
        when (testState.status) {
            com.ivarna.finalbenchmark2.ui.viewmodels.TestStatus.PENDING -> {
                Icon(
                    imageVector = Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = "Pending",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            com.ivarna.finalbenchmark2.ui.viewmodels.TestStatus.RUNNING -> {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            com.ivarna.finalbenchmark2.ui.viewmodels.TestStatus.COMPLETED -> {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Test name
        Text(
            text = testState.name,
            modifier = Modifier.weight(1f),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = when (testState.status) {
                com.ivarna.finalbenchmark2.ui.viewmodels.TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                com.ivarna.finalbenchmark2.ui.viewmodels.TestStatus.RUNNING -> MaterialTheme.colorScheme.primary
                com.ivarna.finalbenchmark2.ui.viewmodels.TestStatus.COMPLETED -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (testState.status == com.ivarna.finalbenchmark2.ui.viewmodels.TestStatus.RUNNING) FontWeight.Bold else FontWeight.Normal
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Execution time (only for completed tests)
        if (testState.status == com.ivarna.finalbenchmark2.ui.viewmodels.TestStatus.COMPLETED && testState.result != null) {
            Text(
                text = "${testState.result.executionTimeMs.toInt()}ms",
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}