package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivarna.finalbenchmark2.ui.models.SystemStats
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.ui.viewmodels.BenchmarkState
import com.ivarna.finalbenchmark2.ui.viewmodels.BenchmarkViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.TestState
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.ivarna.finalbenchmark2.ui.viewmodels.TestStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(
    preset: String = "Auto",
    onBenchmarkComplete: (String) -> Unit,
    historyRepository: com.ivarna.finalbenchmark2.data.repository.HistoryRepository? = null,
    viewModel: BenchmarkViewModel = viewModel(factory = BenchmarkViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Scroll to the active test automatically
    LaunchedEffect(uiState.allTestStates) {
        val runningIndex = uiState.allTestStates.indexOfFirst { it.status == TestStatus.RUNNING }
        if (runningIndex >= 0) {
            listState.animateScrollToItem(runningIndex)
        }
    }

    // Handle completion (Navigation logic)
    LaunchedEffect(viewModel.benchmarkState.collectAsState().value) {
        val state = viewModel.benchmarkState.value
        if (state is BenchmarkState.Completed) {
            // ... (Your existing JSON serialization logic) ...
            // Simplified for brevity in this snippet
            onBenchmarkComplete("{\"completed\": true}")
        }
    }
    
    // Start on load
    LaunchedEffect(Unit) {
        viewModel.startBenchmark(preset)
    }

    FinalBenchmark2Theme {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content with Scaffold
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background
                // Removed bottomBar to add spacing manually
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp), // Side padding for main content
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Top Spacing
                    Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    Spacer(modifier = Modifier.height(32.dp)) // Additional top spacing for breathing room
    
                    // --- Header Section ---
                    Text(
                        text = "Running Benchmarks",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
    
                    Spacer(modifier = Modifier.height(32.dp))
    
                    // 2. Circular Indicator with Bold Text in Center
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(160.dp)
                    ) {
                        // Track (Background circle)
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            strokeWidth = 12.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        
                        // Progress (Foreground circle)
                        CircularProgressIndicator(
                            progress = { uiState.progress },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 12.dp,
                            strokeCap = StrokeCap.Round,
                        )
    
                        // Center Text with bold percentage
                        Text(
                            text = "${(uiState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Active Test Name Label
                    Text(
                        text = uiState.currentTestName.ifEmpty { "Initializing..." },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
    
                    Spacer(modifier = Modifier.height(32.dp))
    
                    // 3. Card Container for Benchmark List (Card Table)
                    Card(
                        modifier = Modifier
                            .weight(1f) // Fill remaining vertical space
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp) // Internal padding for the card content
                        ) {
                            // -- Table Header --
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(vertical = 12.dp, horizontal = 16.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Status",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.width(50.dp)
                                )
                                Text(
                                    text = "Benchmark Name",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Time",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(60.dp)
                                )
                            }
                            
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
    
                            // -- Table Rows --
                            LazyColumn(
                                state = listState
                            ) {
                                items(uiState.allTestStates, key = { it.name }) { testState ->
                                    TestTableRow(testState)
                                    if (testState != uiState.allTestStates.last()) { // Don't add divider after last item
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Add spacing above bottom card
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            
            // SystemMonitorDock positioned at the bottom
            SystemMonitorDock(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                stats = uiState.systemStats
            )
        }
    }
}

// 4. System Monitor Dock (Attached to Bottom)
@Composable
fun SystemMonitorDock(
    modifier: Modifier = Modifier,
    stats: SystemStats
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp), // Remove horizontal padding to make it span full width
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp,
        // Round top corners only, square bottom to sit flush
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(top = 24.dp, bottom = 16.dp) // Add proper bottom padding
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockMetric(icon = Icons.Rounded.Memory, value = "${stats.cpuLoad.toInt()}%", label = "CPU")
            
            // Vertical Divider
            VerticalDivider(modifier = Modifier.height(32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            
            DockMetric(icon = Icons.Rounded.Bolt, value = "${String.format("%.1f", stats.power)}W", label = "Power")
            
            VerticalDivider(modifier = Modifier.height(32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            
            DockMetric(icon = Icons.Rounded.Thermostat, value = "${stats.temp.toInt()}°C", label = "Temp")
        }
    }
}

@Composable
fun DockMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TestTableRow(testState: TestState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Column
        Box(modifier = Modifier.width(50.dp), contentAlignment = Alignment.CenterStart) {
            when (testState.status) {
                TestStatus.COMPLETED -> Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Done",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                TestStatus.RUNNING -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                TestStatus.PENDING -> Icon(
                    imageVector = Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = "Pending",
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Name Column
        Text(
            text = testState.name.replace("Single-Core ", "").replace("Multi-Core ", ""),
            style = MaterialTheme.typography.bodyMedium,
            color = if (testState.status == TestStatus.PENDING) 
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) 
            else 
                MaterialTheme.colorScheme.onSurface,
            fontWeight = if (testState.status == TestStatus.RUNNING) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Time Column
        Text(
            text = if (testState.result != null) "${testState.result.executionTimeMs.toInt()}ms" else "-",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.End
        )
    }
}