package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkEvent
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkManager
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.ui.viewmodels.BenchmarkProgress
import com.ivarna.finalbenchmark2.ui.viewmodels.BenchmarkState
import com.ivarna.finalbenchmark2.ui.viewmodels.BenchmarkViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(
    onBenchmarkComplete: (String) -> Unit,
    viewModel: BenchmarkViewModel = viewModel()
) {
    val benchmarkState by viewModel.benchmarkState.collectAsState()
    val benchmarkManager = remember { BenchmarkManager() }
    var benchmarkEvents by remember { mutableStateOf<Map<String, BenchmarkEvent>>(emptyMap()) }
    val coroutineScope = rememberCoroutineScope()
    
    // Collect benchmark events
    LaunchedEffect(Unit) {
        benchmarkManager.benchmarkEvents.collectLatest { event ->
            benchmarkEvents = benchmarkEvents.toMutableMap().apply {
                this[event.testName] = event
            }
        }
    }
    
    // Handle benchmark state changes
    LaunchedEffect(Unit) {
        viewModel.benchmarkState.collectLatest { state ->
            when (state) {
                is BenchmarkState.Completed -> {
                    onBenchmarkComplete(state.results)
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
        viewModel.startBenchmark()
    }
    
    FinalBenchmark2Theme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Text(
                    text = "Running Benchmarks",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
                
                // Progress indicator
                when (val state = benchmarkState) {
                    is BenchmarkState.Running -> {
                        val progress = state.progress
                        LinearProgressIndicator(
                            progress = { progress.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        
                        // Show current benchmark name
                        Text(
                            text = "Current: ${progress.currentBenchmark}",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                        
                        // Show progress percentage
                        Text(
                            text = "${progress.progress}% (${progress.completedBenchmarks}/${progress.totalBenchmarks})",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    is BenchmarkState.Completed -> {
                        LinearProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    is BenchmarkState.Error -> {
                        LinearProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Text(
                            text = "Error: ${state.message}",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                
                // Benchmark list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(benchmarkEvents.values.toList()) { event ->
                        BenchmarkEventCard(event = event)
                    }
                }
            }
        }
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