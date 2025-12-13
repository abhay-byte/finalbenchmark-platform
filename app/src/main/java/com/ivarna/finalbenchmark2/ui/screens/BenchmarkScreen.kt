package com.ivarna.finalbenchmark2.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivarna.finalbenchmark2.ui.models.SystemStats
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.ui.viewmodels.BenchmarkState
import com.ivarna.finalbenchmark2.ui.viewmodels.BenchmarkViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.TestState
import com.ivarna.finalbenchmark2.ui.viewmodels.TestStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(
        preset: String = "Auto",
        onBenchmarkComplete: (String) -> Unit,
        historyRepository: com.ivarna.finalbenchmark2.data.repository.HistoryRepository? = null,
        onBenchmarkStart: (() -> Unit)? = null,
        onBenchmarkEnd: (() -> Unit)? = null
) {
        val application =
                androidx.compose.ui.platform.LocalContext.current.applicationContext as
                        android.app.Application

        val viewModel: BenchmarkViewModel =
                if (historyRepository != null) {
                        viewModel(
                                key = "BenchmarkViewModelWithHistory",
                                factory =
                                        object : androidx.lifecycle.ViewModelProvider.Factory {
                                                override fun <
                                                        T : androidx.lifecycle.ViewModel> create(
                                                        modelClass: Class<T>
                                                ): T {
                                                        return BenchmarkViewModel(
                                                                historyRepository =
                                                                        historyRepository,
                                                                application = application,
                                                                onBenchmarkCompleteCallback =
                                                                        onBenchmarkComplete
                                                        ) as
                                                                T
                                                }
                                        }
                        )
                } else {
                        viewModel(factory = BenchmarkViewModel.Factory)
                }
        val uiState by viewModel.uiState.collectAsState()
        val listState = rememberLazyListState()

        // Performance monitoring
        val context = androidx.compose.ui.platform.LocalContext.current
        val performanceMonitor = remember {
                com.ivarna.finalbenchmark2.utils.PerformanceMonitor(context)
        }

        // Drag state for the floating system monitor card
        var cardOffset by remember { mutableStateOf(IntOffset.Zero) }
        var isDragging by remember { mutableStateOf(false) }
        var cardSize by remember { mutableStateOf(IntSize.Zero) }
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current

        // Scroll to the active test automatically
        LaunchedEffect(uiState.testStates) {
                val runningIndex =
                        uiState.testStates.indexOfFirst { it.status == TestStatus.RUNNING }
                if (runningIndex >= 0) {
                        listState.animateScrollToItem(runningIndex)
                }
        }

        // Helper function to determine rating based on normalized score
        fun determineRating(normalizedScore: Double): String {
                return when {
                        normalizedScore >= 90000 -> "Excellent"
                        normalizedScore >= 70000 -> "Very Good"
                        normalizedScore >= 50000 -> "Good"
                        normalizedScore >= 30000 -> "Average"
                        normalizedScore >= 10000 -> "Below Average"
                        else -> "Poor"
                }
        }

        // Handle completion (Navigation logic) - Multiple mechanisms to ensure it works
        LaunchedEffect(viewModel.benchmarkState) {
                Log.d("BenchmarkScreen", "Starting benchmarkState collection...")
                viewModel.benchmarkState.collect { state ->
                        Log.d("BenchmarkScreen", "Received benchmark state: $state")
                        when (state) {
                                is BenchmarkState.Completed -> {
                                        Log.d(
                                                "BenchmarkScreen",
                                                "Benchmark completed! Triggering navigation..."
                                        )
                                        val results = state.results

                                        // Stop performance monitoring and get metrics
                                        val performanceMetrics = performanceMonitor.stop()
                                        Log.d(
                                                "BenchmarkScreen",
                                                "Performance metrics: ${performanceMetrics.take(100)}..."
                                        )

                                        // Sanitize values to prevent Infinity/NaN JSON
                                        // serialization errors
                                        fun sanitize(value: Double): Double =
                                                when {
                                                        value.isInfinite() -> 0.0
                                                        value.isNaN() -> 0.0
                                                        else -> value
                                                }

                                        // Create a map that matches EXACTLY what your Result Screen
                                        // expects
                                        val summaryData =
                                                mapOf(
                                                        "single_core_score" to
                                                                sanitize(results.singleCoreScore),
                                                        "multi_core_score" to
                                                                sanitize(results.multiCoreScore),
                                                        "final_score" to
                                                                sanitize(
                                                                        results.finalWeightedScore
                                                                ),
                                                        "normalized_score" to
                                                                sanitize(results.normalizedScore),
                                                        "rating" to
                                                                determineRating(
                                                                        results.normalizedScore
                                                                ),
                                                        "performance_metrics" to performanceMetrics,
                                                        "detailed_results" to
                                                                results.detailedResults.map { result
                                                                        ->
                                                                        mapOf(
                                                                                "name" to
                                                                                        result.name,
                                                                                "executionTimeMs" to
                                                                                        sanitize(
                                                                                                result.executionTimeMs
                                                                                        ),
                                                                                "opsPerSecond" to
                                                                                        sanitize(
                                                                                                result.opsPerSecond
                                                                                        ),
                                                                                "isValid" to
                                                                                        result.isValid,
                                                                                "metricsJson" to
                                                                                        result.metricsJson
                                                                        )
                                                                }
                                                )

                                        // Safe serialization using Gson
                                        val gson = com.google.gson.Gson()
                                        val summaryJson = gson.toJson(summaryData)

                                        Log.d(
                                                "BenchmarkScreen",
                                                "Calling onBenchmarkEnd and onBenchmarkComplete..."
                                        )
                                        onBenchmarkEnd?.invoke()
                                        onBenchmarkComplete(summaryJson)
                                        Log.d(
                                                "BenchmarkScreen",
                                                "Navigation triggered successfully!"
                                        )
                                }
                                is BenchmarkState.Error -> {
                                        Log.d(
                                                "BenchmarkScreen",
                                                "Benchmark error: ${state.message}"
                                        )
                                        // Stop monitoring on error
                                        performanceMonitor.stop()
                                        // Handle error state if needed
                                        onBenchmarkEnd?.invoke()
                                        onBenchmarkComplete("{\"error\": \"${state.message}\"}")
                                }
                                else -> {
                                        Log.d(
                                                "BenchmarkScreen",
                                                "Benchmark state: $state (no action needed)"
                                        )
                                        // Do nothing for other states
                                }
                        }
                }
        }

        // Start on load
        LaunchedEffect(Unit) {
                onBenchmarkStart?.invoke()
                // Start performance monitoring
                performanceMonitor.start()
                Log.d("BenchmarkScreen", "Performance monitoring started")
                viewModel.startBenchmark(preset)
        }

        FinalBenchmark2Theme {
                Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(containerColor = MaterialTheme.colorScheme.background) {
                                paddingValues ->
                                Column(
                                        modifier =
                                                Modifier.fillMaxSize()
                                                        .padding(paddingValues)
                                                        .padding(
                                                                horizontal = 16.dp
                                                        ), // Side padding for main content
                                        horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                        // 1. Top Spacing
                                        Spacer(modifier = Modifier.height(48.dp))

                                        // --- Header Section ---
                                        Text(
                                                text = "Running Benchmarks",
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Workload Preset Badge
                                        Surface(
                                                shape =
                                                        androidx.compose.foundation.shape
                                                                .CircleShape,
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                tonalElevation = 2.dp,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                                // Map backend value to user-friendly display
                                                val workloadDisplay =
                                                        when (uiState.workloadPreset) {
                                                                "slow" -> "Low Accuracy - Fastest"
                                                                "mid" -> "Mid Accuracy - Fast"
                                                                "flagship" -> "High Accuracy - Slow"
                                                                else -> uiState.workloadPreset
                                                        }
                                                Text(
                                                        text = "Workload: $workloadDisplay",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onTertiaryContainer,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier =
                                                                Modifier.padding(
                                                                        horizontal = 16.dp,
                                                                        vertical = 8.dp
                                                                )
                                                )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // 2. Circular Indicator with Bold Text in Center
                                        Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.size(160.dp)
                                        ) {
                                                // Track (Background circle)
                                                CircularProgressIndicator(
                                                        progress = { 1f },
                                                        modifier = Modifier.fillMaxSize(),
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant,
                                                        strokeWidth = 12.dp,
                                                        trackColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant,
                                                )

                                                // Progress (Foreground circle)
                                                CircularProgressIndicator(
                                                        progress = { uiState.progress },
                                                        modifier = Modifier.fillMaxSize(),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        strokeWidth = 12.dp,
                                                        strokeCap = StrokeCap.Round,
                                                )

                                                // Center Text
                                                Column(
                                                        horizontalAlignment =
                                                                Alignment.CenterHorizontally
                                                ) {
                                                        Text(
                                                                text =
                                                                        "${(uiState.progress * 100).toInt()}%",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .displaySmall,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onBackground
                                                        )
                                                        Text(
                                                                text = "Completed",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .secondary,
                                                                fontWeight = FontWeight.Medium
                                                        )
                                                }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Active Test Name Label
                                        Text(
                                                text =
                                                        uiState.currentTestName.ifEmpty {
                                                                "Initializing..."
                                                        },
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(32.dp))

                                        // 4. Modernized Test List with Separated Core Sections
                                        Card(
                                                modifier =
                                                        Modifier.weight(
                                                                        1f
                                                                ) // Fill remaining vertical space
                                                                .fillMaxWidth(),
                                                colors =
                                                        CardDefaults.cardColors(
                                                                containerColor =
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceContainerLow
                                                        ),
                                                shape =
                                                        RoundedCornerShape(
                                                                topStart = 20.dp,
                                                                topEnd = 20.dp,
                                                                bottomStart = 0.dp,
                                                                bottomEnd = 0.dp
                                                        ),
                                                elevation =
                                                        CardDefaults.cardElevation(
                                                                defaultElevation = 4.dp
                                                        )
                                        ) {
                                                // Separate tests into Single-Core and Multi-Core
                                                val singleCoreTests =
                                                        uiState.testStates.filter {
                                                                it.name.contains("Single-Core")
                                                        }
                                                val multiCoreTests =
                                                        uiState.testStates.filter {
                                                                it.name.contains("Multi-Core")
                                                        }

                                                // Calculate section progress
                                                val singleCoreCompleted =
                                                        singleCoreTests.count {
                                                                it.status == TestStatus.COMPLETED
                                                        }
                                                val multiCoreCompleted =
                                                        multiCoreTests.count {
                                                                it.status == TestStatus.COMPLETED
                                                        }
                                                val singleCoreTotal = singleCoreTests.size
                                                val multiCoreTotal = multiCoreTests.size

                                                LazyColumn(
                                                        state = listState,
                                                        contentPadding =
                                                                PaddingValues(bottom = 60.dp),
                                                        modifier = Modifier.fillMaxSize()
                                                ) {
                                                        // Single-Core Section Header
                                                        if (singleCoreTests.isNotEmpty()) {
                                                                item {
                                                                        BenchmarkSectionHeader(
                                                                                title =
                                                                                        "Single-Core Performance",
                                                                                completedCount =
                                                                                        singleCoreCompleted,
                                                                                totalCount =
                                                                                        singleCoreTotal,
                                                                                icon =
                                                                                        Icons.Rounded
                                                                                                .Memory,
                                                                                isActive =
                                                                                        singleCoreTests
                                                                                                .any {
                                                                                                        it.status ==
                                                                                                                TestStatus
                                                                                                                        .RUNNING
                                                                                                }
                                                                        )
                                                                }

                                                                items(
                                                                        singleCoreTests,
                                                                        key = { it.name }
                                                                ) { testState ->
                                                                        ModernTestRow(testState)
                                                                }
                                                        }

                                                        // Divider between sections
                                                        if (singleCoreTests.isNotEmpty() &&
                                                                        multiCoreTests.isNotEmpty()
                                                        ) {
                                                                item {
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                }
                                                        }

                                                        // Multi-Core Section Header
                                                        if (multiCoreTests.isNotEmpty()) {
                                                                item {
                                                                        BenchmarkSectionHeader(
                                                                                title =
                                                                                        "Multi-Core Performance",
                                                                                completedCount =
                                                                                        multiCoreCompleted,
                                                                                totalCount =
                                                                                        multiCoreTotal,
                                                                                icon =
                                                                                        Icons.Rounded
                                                                                                .DeveloperBoard,
                                                                                isActive =
                                                                                        multiCoreTests
                                                                                                .any {
                                                                                                        it.status ==
                                                                                                                TestStatus
                                                                                                                        .RUNNING
                                                                                                }
                                                                        )
                                                                }

                                                                items(
                                                                        multiCoreTests,
                                                                        key = { it.name }
                                                                ) { testState ->
                                                                        ModernTestRow(testState)
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }

                        // Floating System Monitor Card with drag functionality
                        SystemMonitorDock(
                                stats = uiState.systemStats,
                                modifier =
                                        Modifier.offset { cardOffset }
                                                .pointerInput(Unit) {
                                                        detectDragGestures(
                                                                onDragStart = { isDragging = true },
                                                                onDragEnd = { isDragging = false },
                                                                onDragCancel = {
                                                                        isDragging = false
                                                                }
                                                        ) { change, dragAmount ->
                                                                change.consume()
                                                                val (x, y) = dragAmount

                                                                // Update position with bounds
                                                                // checking
                                                                val newX =
                                                                        (cardOffset.x + x.toInt())
                                                                                .coerceIn(
                                                                                        16,
                                                                                        1000
                                                                                ) // Simple bounds
                                                                val newY =
                                                                        (cardOffset.y + y.toInt())
                                                                                .coerceIn(
                                                                                        16,
                                                                                        2000
                                                                                ) // Simple bounds

                                                                cardOffset = IntOffset(newX, newY)
                                                        }
                                                }
                                                .onGloballyPositioned { coordinates ->
                                                        cardSize = coordinates.size
                                                        // Initialize position at bottom-center if
                                                        // not set
                                                        if (cardOffset == IntOffset.Zero) {
                                                                val screenWidth =
                                                                        coordinates
                                                                                .parentLayoutCoordinates
                                                                                ?.size
                                                                                ?.width
                                                                                ?: 400
                                                                val screenHeight =
                                                                        coordinates
                                                                                .parentLayoutCoordinates
                                                                                ?.size
                                                                                ?.height
                                                                                ?: 800
                                                                val initialX =
                                                                        ((screenWidth -
                                                                                        cardSize.width) /
                                                                                        2)
                                                                                .coerceAtLeast(16)
                                                                val initialY =
                                                                        (screenHeight -
                                                                                        cardSize.height -
                                                                                        40)
                                                                                .coerceAtLeast(
                                                                                        16
                                                                                ) // 40px from
                                                                // bottom
                                                                cardOffset =
                                                                        IntOffset(
                                                                                initialX,
                                                                                initialY
                                                                        )
                                                        }
                                                },
                                isDragging = isDragging
                        )
                }
        }
}

// 3. System Monitor Dock (Floating & Draggable)
@Composable
fun SystemMonitorDock(
        stats: SystemStats,
        modifier: Modifier = Modifier,
        isDragging: Boolean = false
) {
        Surface(
                modifier = modifier.fillMaxWidth(),
                color =
                        if (isDragging) {
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
                        } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                tonalElevation = if (isDragging) 12.dp else 8.dp,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = if (isDragging) 12.dp else 8.dp
        ) {
                Row(
                        modifier =
                                Modifier.padding(vertical = 16.dp, horizontal = 20.dp)
                                        .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        DockMetric(
                                icon = Icons.Rounded.Memory,
                                value = "${stats.cpuLoad.toInt()}%",
                                label = "CPU"
                        )

                        // Vertical Divider
                        VerticalDivider(
                                modifier = Modifier.height(32.dp),
                                color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.2f
                                        )
                        )

                        DockMetric(
                                icon = Icons.Rounded.Bolt,
                                value = "${String.format("%.1f", stats.power)}W",
                                label = "Power"
                        )

                        VerticalDivider(
                                modifier = Modifier.height(32.dp),
                                color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.2f
                                        )
                        )

                        DockMetric(
                                icon = Icons.Rounded.Thermostat,
                                value = "${stats.temp.toInt()}°C",
                                label = "Temp"
                        )

                        VerticalDivider(
                                modifier = Modifier.height(32.dp),
                                color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.2f
                                        )
                        )

                        DockMetric(
                                icon = Icons.Rounded.Memory,
                                value = "${stats.memoryLoad.toInt()}%",
                                label = "RAM"
                        )
                }
        }
}

@Composable
fun DockMetric(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        value: String,
        label: String
) {
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                // 1. Status Column
                Box(modifier = Modifier.width(50.dp), contentAlignment = Alignment.CenterStart) {
                        when (testState.status) {
                                TestStatus.COMPLETED ->
                                        Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = "Done",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                        )
                                TestStatus.RUNNING ->
                                        CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                TestStatus.PENDING ->
                                        Icon(
                                                imageVector = Icons.Rounded.RadioButtonUnchecked,
                                                contentDescription = "Pending",
                                                tint =
                                                        MaterialTheme.colorScheme.outline.copy(
                                                                alpha = 0.5f
                                                        ),
                                                modifier = Modifier.size(20.dp)
                                        )
                        }
                }

                // 2. Name Column
                Text(
                        text =
                                testState
                                        .name
                                        .replace("Single-Core ", "")
                                        .replace("Multi-Core ", ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                                if (testState.status == TestStatus.PENDING)
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurface,
                        fontWeight =
                                if (testState.status == TestStatus.RUNNING) FontWeight.Bold
                                else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )

                // 3. Time Column (The Key Requirement!)
                if (testState.status == TestStatus.COMPLETED && testState.timeText.isNotEmpty()) {
                        Text(
                                text = testState.timeText, // e.g., "200ms" - THE SPECIFIC
                                // REQUIREMENT
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color =
                                        MaterialTheme.colorScheme
                                                .primary, // Using primary color for visibility
                                modifier = Modifier.width(80.dp),
                                textAlign = TextAlign.End
                        )
                } else {
                        // Show dash for non-completed tests
                        Text(
                                text = "-",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                modifier = Modifier.width(80.dp),
                                textAlign = TextAlign.End
                        )
                }
        }
}

// Section Header for Single-Core / Multi-Core groups
@Composable
fun BenchmarkSectionHeader(
        title: String,
        completedCount: Int,
        totalCount: Int,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        isActive: Boolean
) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(
                                        if (isActive)
                                                MaterialTheme.colorScheme.primaryContainer.copy(
                                                        alpha = 0.3f
                                                )
                                        else
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                        alpha = 0.5f
                                                )
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint =
                                        if (isActive) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                        )
                        Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color =
                                        if (isActive) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }

                // Progress badge
                Surface(
                        shape = RoundedCornerShape(12.dp),
                        color =
                                if (completedCount == totalCount && totalCount > 0)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                        Text(
                                text = "$completedCount / $totalCount",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color =
                                        if (completedCount == totalCount && totalCount > 0)
                                                MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                }
        }
}

// Modern Test Row with better visual feedback
@Composable
fun ModernTestRow(testState: TestState) {
        val isRunning = testState.status == TestStatus.RUNNING
        val isCompleted = testState.status == TestStatus.COMPLETED

        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(
                                        if (isRunning)
                                                MaterialTheme.colorScheme.primaryContainer.copy(
                                                        alpha = 0.1f
                                                )
                                        else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                // Status indicator
                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        when (testState.status) {
                                TestStatus.COMPLETED ->
                                        Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = "Completed",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                        )
                                TestStatus.RUNNING ->
                                        CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                TestStatus.PENDING ->
                                        Icon(
                                                imageVector = Icons.Rounded.RadioButtonUnchecked,
                                                contentDescription = "Pending",
                                                tint =
                                                        MaterialTheme.colorScheme.outline.copy(
                                                                alpha = 0.4f
                                                        ),
                                                modifier = Modifier.size(18.dp)
                                        )
                        }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Test name (without Single-Core/Multi-Core prefix)
                Text(
                        text =
                                testState
                                        .name
                                        .replace("Single-Core ", "")
                                        .replace("Multi-Core ", ""),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isRunning) FontWeight.SemiBold else FontWeight.Normal,
                        color =
                                when {
                                        isRunning -> MaterialTheme.colorScheme.primary
                                        isCompleted -> MaterialTheme.colorScheme.onSurface
                                        else ->
                                                MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.5f
                                                )
                                },
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )

                // Time display
                Text(
                        text =
                                if (isCompleted && testState.timeText.isNotEmpty())
                                        testState.timeText
                                else "-",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = if (isCompleted) FontWeight.Medium else FontWeight.Normal,
                        color =
                                when {
                                        isCompleted -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                },
                        modifier = Modifier.width(70.dp),
                        textAlign = TextAlign.End
                )
        }

        // Subtle divider
        if (testState.status != TestStatus.RUNNING) {
                HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        modifier = Modifier.padding(start = 54.dp, end = 16.dp)
                )
        }
}
