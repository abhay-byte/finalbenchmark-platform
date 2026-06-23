package com.zenithblue.fb2Pro.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenithblue.fb2Pro.data.repository.HistoryRepository
import com.zenithblue.fb2Pro.ui.theme.FB2ProTheme
import com.zenithblue.fb2Pro.ui.viewmodels.StorageBenchmarkUiState
import com.zenithblue.fb2Pro.ui.viewmodels.StorageBenchmarkViewModel
import com.zenithblue.fb2Pro.ui.viewmodels.StorageTest
import com.zenithblue.fb2Pro.ui.viewmodels.StorageTestResult

// Colors per test for radial backgrounds
private val STOR_TEST_TINT = mapOf(
    StorageTest.SEQ_READ    to Color(0xFFFFCA28),  // amber
    StorageTest.SEQ_WRITE   to Color(0xFFFF7043),  // deep orange
    StorageTest.RAND_4K     to Color(0xFFFF5252),  // red accent
    StorageTest.SMALL_FILES to Color(0xFF26C6DA),  // cyan
    StorageTest.SQLITE      to Color(0xFF7C4DFF),  // deep purple
    StorageTest.MIXED       to Color(0xFFB2FF59)   // light green
)

private enum class StorTestStatus {
    PENDING, RUNNING, COMPLETED
}

private data class StorTestState(
    val test: StorageTest,
    val name: String,
    val status: StorTestStatus,
    val score: Int = 0,
    val value: Double = 0.0,
    val unit: String = "",
    val timeText: String = ""
)

private data class StorageStats(
    val cpuLoad: Int,
    val temp: Float,
    val freeSpace: Double,
    val tint: Color
)

@Composable
fun StorageBenchmarkScreen(
    preset: String,
    historyRepository: HistoryRepository? = null,
    onBenchmarkComplete: (String) -> Unit,
    onNavBack: () -> Unit,
    isFullBenchmark: Boolean = false
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val vmFactory = remember(historyRepository) {
        StorageBenchmarkViewModel.factory(historyRepository, application)
    }
    val vm: StorageBenchmarkViewModel = viewModel(factory = vmFactory)
    val state by vm.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = state.isRunning || state.isWarmingUp) {
        vm.stop()
        onNavBack()
    }

    // Ensure cleanup when leaving composition
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            if (state.isRunning) {
                vm.stop()
            }
        }
    }

    LaunchedEffect(Unit) { vm.start(preset) }
    LaunchedEffect(vm.completionEvent) {
        vm.completionEvent.collect { json -> onBenchmarkComplete(json) }
    }

    val tint = STOR_TEST_TINT[state.currentTest] ?: Color(0xFFFFCA28)
    val scrollState = rememberLazyListState()
    var listCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    // Map 6 tests to states
    val testStates = remember(state.currentTest, state.completedTests, state.isRunning) {
        StorageTest.values().map { test ->
            val completed = state.completedTests.firstOrNull { it.test == test }
            val isCurrent = state.currentTest == test && state.isRunning
            
            val status = when {
                isCurrent -> StorTestStatus.RUNNING
                completed != null -> StorTestStatus.COMPLETED
                else -> StorTestStatus.PENDING
            }
            
            val displayName = when (test) {
                StorageTest.SEQ_READ    -> "Sequential Read"
                StorageTest.SEQ_WRITE   -> "Sequential Write"
                StorageTest.RAND_4K     -> "Random 4K I/O"
                StorageTest.SMALL_FILES -> "Small Files Ops"
                StorageTest.SQLITE      -> "SQLite Transactions"
                StorageTest.MIXED       -> "Mixed Read/Write"
            }
            
            val score = completed?.score ?: 0
            val value = completed?.value ?: if (isCurrent) state.currentValue else 0.0
            val unit = completed?.unit ?: if (isCurrent) state.currentUnit else ""
            
            val timeText = if (completed != null) {
                when (unit) {
                    "MB/s"    -> String.format("%.1f MB/s", value)
                    "files/s" -> String.format("%.0f files/s", value)
                    "txns/s"  -> String.format("%.0f txns/s", value)
                    else      -> String.format("%.1f %s", value, unit)
                }
            } else if (isCurrent && state.currentValue > 0.0) {
                when (state.currentUnit) {
                    "MB/s"    -> String.format("%.1f MB/s", state.currentValue)
                    "files/s" -> String.format("%.0f files/s", state.currentValue)
                    "txns/s"  -> String.format("%.0f txns/s", state.currentValue)
                    else      -> String.format("%.1f %s", state.currentValue, state.currentUnit)
                }
            } else {
                ""
            }
            
            StorTestState(
                test = test,
                name = displayName,
                status = status,
                score = score,
                value = value,
                unit = unit,
                timeText = timeText
            )
        }
    }

    // Scroll to center active index
    val activeIndex = testStates.indexOfFirst { it.status == StorTestStatus.RUNNING }
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            val baseListIndex = when (activeIndex) {
                in 0..1 -> 1 + activeIndex
                in 2..3 -> 5 + (activeIndex - 2)
                else -> 9 + (activeIndex - 4)
            }
            val targetListIndex = maxOf(0, baseListIndex - 2)
            scrollState.animateScrollToItem(
                index = targetListIndex,
                scrollOffset = 0
            )
        }
    }

    // Timer calculation
    var elapsedTimeSec by remember { mutableStateOf(0) }
    LaunchedEffect(state.isRunning) {
        if (state.isRunning) {
            elapsedTimeSec = 0
            while (state.isRunning) {
                kotlinx.coroutines.delay(1000L)
                elapsedTimeSec++
            }
        }
    }

    val minutes = elapsedTimeSec / 60
    val seconds = elapsedTimeSec % 60
    val elapsedTimeText = String.format("%02d:%02d", minutes, seconds)

    val totalEstimatedSec = 24
    val remainingSec = maxOf(0, totalEstimatedSec - elapsedTimeSec)
    val remMin = remainingSec / 60
    val remSec = remainingSec % 60
    val remainingTimeText = String.format("%02d:%02d", remMin, remSec)

    // HUD Monitor Stats
    val currentScore = if (state.completedTests.isEmpty()) 0
                       else state.completedTests.map { it.score }.average().toInt()

    val cpuLoadVal = remember(state.isRunning, elapsedTimeSec) {
        if (state.isRunning) (75..95).random() else (4..10).random()
    }

    val storageStats = remember(cpuLoadVal, state.cpuTempC, state.storageFreeGB, currentScore, tint) {
        StorageStats(
            cpuLoad = cpuLoadVal,
            temp = state.cpuTempC,
            freeSpace = state.storageFreeGB,
            tint = tint
        )
    }

    FB2ProTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Radial Background Glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(tint.copy(alpha = 0.08f), Color.Transparent),
                            radius = 800f
                        )
                    )
            )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            if (isFullBenchmark) {
                Spacer(modifier = Modifier.height(120.dp))
            } else {
                // Uniform Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FINAL BENCHMARK",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            letterSpacing = 2.sp
                        )
                        
                        val configTitle = when (preset.lowercase()) {
                            "slow" -> "Low Accuracy - Fastest (Storage)"
                            "mid" -> "Mid Accuracy - Fast (Storage)"
                            "flagship" -> "High Accuracy - Slow (Storage)"
                            else -> "${preset.replace("Workload: ", "")} (Storage)"
                        }
                        
                        Text(
                            text = configTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (state.isRunning || state.isWarmingUp) {
                        Surface(
                            onClick = { 
                                vm.stop()
                                onNavBack()
                            },
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop",
                                modifier = Modifier.padding(8.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Uniform Reactor Progress
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StorReactorProgress(progress = state.overallProgress, accentColor = tint)
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    ) {
                        Text(
                            text = "${(state.overallProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-2).sp
                        )
                        
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alpha"
                        )

                        Text(
                            text = when {
                                state.isWarmingUp -> "WARMING UP"
                                state.isRunning -> "PROCESSING"
                                else -> "READY"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.isWarmingUp) 
                                        Color.Yellow.copy(alpha = pulseAlpha)
                                    else 
                                        tint,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Time Remaining Timer Pill
                if (state.isRunning || state.isWarmingUp) {
                    GlassTimerPill(
                        timeText = remainingTimeText,
                        elapsedTime = elapsedTimeText
                    )
                }
            }

            // Live Performance Details Badge (Speed details, e.g. "Measuring... Sequential Read at 1024 MB/s")
            AnimatedVisibility(
                visible = state.isRunning && state.currentValue > 0.0,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tint.copy(alpha = 0.08f))
                        .border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.currentTestName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (state.currentUnit) {
                                "MB/s"    -> String.format("%.1f MB/s", state.currentValue)
                                "files/s" -> String.format("%.0f files/s", state.currentValue)
                                "txns/s"  -> String.format("%.0f txns/s", state.currentValue)
                                else      -> String.format("%.1f %s", state.currentValue, state.currentUnit)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = tint
                        )
                    }
                }
            }

            // Rolling Timeline List
            LazyColumn(
                state = scrollState,
                contentPadding = PaddingValues(bottom = 150.dp, top = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onGloballyPositioned { listCoordinates = it }
            ) {
                // Section 1: Sequential Access
                item { StorSectionHeader(title = "SEQUENTIAL ACCESS", tint = tint) }
                items(testStates.subList(0, 2)) { test ->
                    Box(modifier = Modifier.storWheelCurve(scrollState, listCoordinates)) {
                        StorTimelineTestRow(test = test, activeTint = tint)
                    }
                }

                // Section 2: Random & File I/O
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { StorSectionHeader(title = "RANDOM & FILE I/O", tint = tint) }
                items(testStates.subList(2, 4)) { test ->
                    Box(modifier = Modifier.storWheelCurve(scrollState, listCoordinates)) {
                        StorTimelineTestRow(test = test, activeTint = tint)
                    }
                }

                // Section 3: Database & Combined
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { StorSectionHeader(title = "DATABASE & COMBINED", tint = tint) }
                items(testStates.subList(4, 6)) { test ->
                    Box(modifier = Modifier.storWheelCurve(scrollState, listCoordinates)) {
                        StorTimelineTestRow(test = test, activeTint = tint)
                    }
                }
            }
        }

        // Fixed Bottom HUD Monitor
        StorHUDMonitor(
            stats = storageStats,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        )
    }
}
}

@Composable
private fun StorReactorProgress(progress: Float, accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Animated glow pulsator for the shiny progress tip
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_radius"
    )

    Canvas(modifier = Modifier.size(240.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2
        val strokeWidth = 16f
        val arcRadius = (size.width - strokeWidth) / 2f

        // Outer Glow Ring
        drawCircle(
            color = Color.White.copy(alpha = 0.05f),
            radius = radius,
            style = Stroke(width = 24f)
        )

        // Progress Arc
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.5f),
                    accentColor,
                    accentColor.copy(alpha = 0.8f),
                    accentColor.copy(alpha = 0.5f)
                )
            ),
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )


        // Spinner Ring
        if (progress > 0f && progress < 1f) {
            rotate(degrees = rotation) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            accentColor
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 120f,
                    useCenter = false,
                    style = Stroke(width = 4f)
                )
            }
        }
    }
}

@Composable
private fun StorSectionHeader(title: String, tint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(tint, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        
        // Gradient Horizontal Line
        Box(
            modifier = Modifier
                .height(1.dp)
                .width(100.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            tint.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun StorTimelineTestRow(
    test: StorTestState,
    activeTint: Color
) {
    val isRunning = test.status == StorTestStatus.RUNNING
    val isCompleted = test.status == StorTestStatus.COMPLETED
    
    val rowAlpha = if (isRunning) 1f else if (isCompleted) 0.8f else 0.4f
    val targetScale = if (isRunning) 1.05f else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "rowScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = if (isRunning) 12.dp else 24.dp)
            .graphicsLayer {
                alpha = rowAlpha
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isRunning) {
                    Modifier
                        .background(activeTint.copy(alpha = 0.15f))
                        .border(1.dp, activeTint.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon / Spinner
            when {
                isRunning -> {
                    StorRunningBenchmarkIndicator(activeTint)
                }
                isCompleted -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Done",
                        tint = activeTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Pending,
                        contentDescription = "Pending",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Test name and results
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = test.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isRunning) activeTint else MaterialTheme.colorScheme.onBackground,
                    fontWeight = if (isRunning) FontWeight.ExtraBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (test.timeText.isNotEmpty()) {
                    Text(
                        text = test.timeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isRunning) activeTint else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StorRunningBenchmarkIndicator(tint: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = Modifier.size(20.dp)) {
        rotate(rotation) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        tint
                    )
                ),
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun StorHUDMonitor(
    stats: StorageStats,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(340.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(40.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StorHUDMetric(
                icon = Icons.Default.Memory,
                label = "CPU LOAD",
                value = "${stats.cpuLoad}%",
                accentColor = stats.tint
            )
            
            Divider(
                modifier = Modifier
                    .height(30.dp)
                    .width(1.dp),
                color = Color.White.copy(alpha = 0.2f)
            )

            StorHUDMetric(
                icon = Icons.Default.Thermostat,
                label = "CPU TEMP",
                value = String.format("%.1f°C", stats.temp),
                accentColor = Color(0xFFFF7043)
            )

            Divider(
                modifier = Modifier
                    .height(30.dp)
                    .width(1.dp),
                color = Color.White.copy(alpha = 0.2f)
            )

            StorHUDMetric(
                icon = Icons.Default.Save,
                label = "FREE SPACE",
                value = String.format("%.1f GB", stats.freeSpace),
                accentColor = Color(0xFF00BCD4)
            )
        }
    }
}

@Composable
private fun StorHUDMetric(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}

private fun Modifier.storWheelCurve(
    scrollState: androidx.compose.foundation.lazy.LazyListState,
    listCoordinates: androidx.compose.ui.layout.LayoutCoordinates?
): Modifier = this.composed {
    var itemY by remember { mutableStateOf(0f) }
    
    Modifier
        .onGloballyPositioned { coordinates ->
             itemY = coordinates.positionInWindow().y
        }
        .graphicsLayer {
            val viewportHeight = scrollState.layoutInfo.viewportSize.height.toFloat()
            
            val listY = listCoordinates?.positionInWindow()?.y ?: 0f
            val centerY = listY + (viewportHeight / 2f)
            
            val distanceFromCenter = itemY - centerY
            
            val normalizedDist = (distanceFromCenter / (viewportHeight / 1.6f)).coerceIn(-1f, 1f)
            
            val maxTranslationX = 60.dp.toPx()
            
            translationX = maxTranslationX * (1f - (normalizedDist * normalizedDist)) 
            alpha = 1f - 0.7f * Math.abs(normalizedDist)
        }
}
