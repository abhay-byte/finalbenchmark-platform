package com.ivarna.finalbenchmark2.ui.screens

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.ivarna.finalbenchmark2.ui.models.SystemStats
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.ui.viewmodels.BenchmarkViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.TestState
import com.ivarna.finalbenchmark2.ui.viewmodels.TestStatus

@Composable
fun BenchmarkScreen(
    preset: String,
    onBenchmarkComplete: (String) -> Unit,
    onBenchmarkStart: () -> Unit = {},
    onBenchmarkEnd: () -> Unit = {},
    onNavBack: () -> Unit = {}, // New parameter
    historyRepository: HistoryRepository
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    
    // Custom Factory to inject dependencies properly
    // Custom Factory to inject dependencies properly
    val viewModel: BenchmarkViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BenchmarkViewModel(
                    historyRepository = historyRepository,
                    application = application
                ) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val isWarmingUp by viewModel.isWarmingUp.collectAsState()
    val scrollState = rememberLazyListState()
    
    // Listen for completion events from ViewModel
    // This survives configuration changes because ViewModel survives, and we re-subscribe here
    LaunchedEffect(viewModel) {
        viewModel.completionEvent.collect { summaryJson ->
            onBenchmarkComplete(summaryJson)
        }
    }
    
    // Track list position for wheel calculations
    var listCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    // Handle back press - stop benchmark if running
    BackHandler(enabled = uiState.isRunning) {
        viewModel.stopBenchmark()
        onBenchmarkEnd()
    }

    // Initialize and start benchmark
    LaunchedEffect(Unit) {
        if (!uiState.isRunning && uiState.progress == 0f) {
            onBenchmarkStart()
            viewModel.startBenchmark(preset)
        }
    }
    
    // Ensure cleanup when leaving composition
    DisposableEffect(Unit) {
        onDispose {
            if (uiState.isRunning) {
                // If checking out while running, we consider it ended for the activity optimization
                onBenchmarkEnd()
            }
        }
    }

    // Handle Scroll to active item - Center it!
    val activeIndex = uiState.testStates.indexOfFirst { it.status == TestStatus.RUNNING }
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            // Estimate item height + header offset to center it. 
            // A more robust way is getting layout info, but a rough offset works well for fixed-ish height items.
            // TimelineTestRow is approx 60-80dp? Let's aim for center.
            // Scroll offset logic: Center of screen approx 400dp down (depending on screen).
            // We want the item to be valid in the "sweet spot" of the curve.
            
            // Note: LazyList auto-handling of indices with headers is tricky. 
            // We need to account for the header "SINGLE CORE OPERATIONS" (index 0).
            // Single core items start at index 1.
            // Using the key or finding the exact list index is safer if we had keys.
            // For now, let's just use the index mapping logic used in the LazyColumn below.
            
            val singleCoreCount = uiState.testStates.count { !it.name.startsWith("Multi-Core", ignoreCase = true) }
            
            // Calculate list index:
            // 0: Header Single
            // 1..N: Single items
            // N+1: Spacer
            // N+2: Header Multi
            // N+3..End: Multi items
            
            val listIndex = if (activeIndex < singleCoreCount) {
                // 6 dots + 1 header + itemIndex
                activeIndex + 7 
            } else {
                // 6 dots + 1 header + N single + 1 spacer + 1 header + (activeIndex - N)
                // = activeIndex + 9
                activeIndex + 9
            }

            // Animate scroll with an offset to center the item
            // scrollToItem(index, offset): offset is pixels from the start of the viewport.
            // Target exact center (50% down)
            val viewportHeight = scrollState.layoutInfo.viewportSize.height
            
            // Only scroll if layout is ready and we have a valid height
            if (viewportHeight > 0) {
                val targetOffset = (viewportHeight / 2)
                
                scrollState.animateScrollToItem(
                    index = listIndex,
                    scrollOffset = targetOffset 
                )
            }
        }
    }

    FinalBenchmark2Theme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Subtle Theme Gradient Overlay - IMPROVED for Starry Background visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f), // Reduced opacity
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            // Gradient overlay
            // Box(modifier = Modifier...) // Already moved or kept if needed.
            // Removing StarryBackground as requested.

            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Bar
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
                        
                        // Map internal keys to human-readable titles matches HomeScreen
                        val configTitle = when (preset.lowercase()) {
                            "slow" -> "Low Accuracy - Fastest"
                            "mid" -> "Mid Accuracy - Fast"
                            "flagship" -> "High Accuracy - Slow"
                            else -> preset.replace("Workload: ", "")
                        }
                        
                        Text(
                            text = configTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.isRunning) {
                        Surface(
                            onClick = { 
                                viewModel.stopBenchmark()
                                onBenchmarkEnd()
                                onNavBack() // Navigate back
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

                // Reactor Progress (Hero)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ReactorProgress(progress = uiState.progress)
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 40.dp)) {
                            // 1. Percentage Text
                            Text(
                                text = "${(uiState.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-2).sp
                            )
                            
                            // Pulsing animation for Warm-up
                            val infiniteTransition = rememberInfiniteTransition(label = "warmup_pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alpha"
                            )

                            // 2. Status Text ("PROCESSING", "WARMING UP") - "bellow text processing"
                            Text(
                                text = when {
                                    isWarmingUp -> "WARMING UP"
                                    // Show PROCESSING if running OR if we have intermediate progress (to handle state edge cases)
                                    uiState.isRunning || (uiState.progress > 0f && uiState.progress < 1f) -> "PROCESSING"
                                    else -> "READY"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isWarmingUp) 
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = pulseAlpha)
                                        else 
                                            MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Time Remaining Text (Below Dial)
                    if (uiState.isRunning || isWarmingUp) {
                        Text(
                            text = "Estimated Time Remaining: ${uiState.estimatedTimeRemaining}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }

                // Timeline List
                LazyColumn(
                    state = scrollState,
                    contentPadding = PaddingValues(bottom = 150.dp, top = 24.dp), // Added top padding
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Ensure it takes up remaining space correctly
                        .onGloballyPositioned { listCoordinates = it }
                ) {
                    val singleCoreTests = uiState.testStates.filter { !it.name.startsWith("Multi-Core", ignoreCase = true) }
                    val multiCoreTests = uiState.testStates.filter { it.name.startsWith("Multi-Core", ignoreCase = true) }

                    // Dots at start
                    items(6) { 
                        Box(modifier = Modifier.wheelCurve(scrollState, listCoordinates)) {
                            WheelSpacerDot() 
                        }
                    }

                    // Single Core Section
                    if (singleCoreTests.isNotEmpty()) {
                        item { 
                            // Header also curves
                            Box(modifier = Modifier.wheelCurve(scrollState, listCoordinates)) {
                                SectionHeader("SINGLE CORE OPERATIONS") 
                            }
                        }
                        items(singleCoreTests) { test ->
                            Box(modifier = Modifier.wheelCurve(scrollState, listCoordinates)) {
                                TimelineTestRow(test = test)
                            }
                        }
                    }

                    // Multi Core Section
                    if (multiCoreTests.isNotEmpty()) {
                        item { 
                            Box(modifier = Modifier.wheelCurve(scrollState, listCoordinates)) {
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                        item {
                           Box(modifier = Modifier.wheelCurve(scrollState, listCoordinates)) {
                               SectionHeader("MULTI CORE OPERATIONS")
                           }
                        }
                        items(multiCoreTests) { test ->
                            Box(modifier = Modifier.wheelCurve(scrollState, listCoordinates)) {
                                TimelineTestRow(test = test)
                            }
                        }
                    }

                    // Dots at end
                    items(6) { 
                        Box(modifier = Modifier.wheelCurve(scrollState, listCoordinates)) {
                            WheelSpacerDot() 
                        }
                    }
                }
            }

            // HUD Monitor (Bottom Fixed)
            HUDMonitor(
                stats = uiState.systemStats,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
            )
        }
    }
}

@Composable
fun ReactorProgress(progress: Float) {
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

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = Modifier.size(260.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2

        // Outer Glow Ring (Static) using Surface Variant color for subtle look
        drawCircle(
            color = outlineColor.copy(alpha = 0.1f),
            radius = radius,
            style = Stroke(width = 30f)
        )

        // Progress Arc
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    primaryColor,
                    secondaryColor,
                    tertiaryColor,
                    primaryColor
                )
            ),
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = 20f, cap = StrokeCap.Round)
        )

        // Spinner Ring (Animated)
        if (progress > 0 && progress < 1f) {
            rotate(degrees = rotation) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryColor
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
fun WheelSpacerDot() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .padding(start = 2.dp), // Touch left edge
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), CircleShape)
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
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
        
        // Horizontal Line
        Box(
            modifier = Modifier
                .height(1.dp)
                .width(100.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.outlineVariant,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
fun TimelineTestRow(test: TestState) {
    val isRunning = test.status == TestStatus.RUNNING
    val isCompleted = test.status == TestStatus.COMPLETED
    
    val rowBackgroundColor = if (isRunning) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    // Animation for running state
    val infiniteTransition = rememberInfiniteTransition(label = "running_text")
    val scale by if (isRunning) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val textAlpha by if (isRunning) {
        infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackgroundColor)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp) // Reduced start padding
            .padding(end = 16.dp), // Reduced extra end padding as translation is smaller now
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Icon
        when {
            isRunning -> {
                RunningBenchmarkIndicator()
            }
            isCompleted -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Done",
                    tint = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(20.dp)
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Pending,
                    contentDescription = "Pending",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text and Time grouped together
        Row(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (isRunning) {
                        Modifier.scale(scale)
                    } else Modifier
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = test.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isRunning) 
                            MaterialTheme.colorScheme.primary.copy(alpha = textAlpha) 
                        else if (isCompleted) 
                            MaterialTheme.colorScheme.onBackground 
                        else 
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontWeight = if (isRunning) FontWeight.ExtraBold else if (isCompleted) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false) // Don't force fill, take what's needed for name
            )

            // Duration or Score next to title
            if (test.timeText.isNotEmpty()) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = test.timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RunningBenchmarkIndicator() {
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

    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.size(20.dp)) {
        rotate(rotation) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor
                    )
                ),
                startAngle = 0f,
                sweepAngle = 270f, // "Semi" circle feel (3/4 actually, but fits implied intent of arc)
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun HUDMonitor(
    stats: SystemStats,
    modifier: Modifier = Modifier
) {
    // Glass Pill Layout
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
            HUDMetric(
                icon = Icons.Default.Memory,
                label = "CPU",
                value = "${stats.cpuLoad.toInt()}%",
                accentColor = MaterialTheme.colorScheme.primary
            )
            
            Divider(
                modifier = Modifier
                    .height(30.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            HUDMetric(
                icon = Icons.Default.Bolt,
                label = "TEMP",
                value = "${stats.temp.toInt()}°C",
                accentColor = if (stats.temp > 40) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
            )

            Divider(
                modifier = Modifier
                    .height(30.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            HUDMetric(
                icon = Icons.Default.Bolt,
                label = "PWR",
                value = String.format("%.1fW", stats.power),
                accentColor = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun HUDMetric(
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}


// Wheel Curve Modifier
fun Modifier.wheelCurve(
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
            
            // Calculate accurate center based on list position in window
            // If listCoordinates is not yet available, fallback to simple view height center
            val listY = listCoordinates?.positionInWindow()?.y ?: 0f
            val centerY = listY + (viewportHeight / 2f)
            
            // If listY is 0 (first frame), usage of just positionInWindow might be offset?
            // itemY is also global.
            // distance = itemGlobalY - listCenterGlobalY
            val distanceFromCenter = itemY - centerY
            
            // Normalized distance [-1, 1] relative to half height
            val normalizedDist = (distanceFromCenter / (viewportHeight / 1.6f)).coerceIn(-1f, 1f)
            
            val maxTranslationX = 60.dp.toPx() // Further reduced from 80dp to 60dp to reduce left gap
            
            // Apply translation
            // 1 - dist^2 creates a nice bell curve
            translationX = maxTranslationX * (1f - (normalizedDist * normalizedDist)) 
            
            // scaleX = 1f + 0.1f * (1f - Math.abs(normalizedDist)) // subtle scale up center?
            
            alpha = 1f - 0.7f * Math.abs(normalizedDist) // Fade out edges
        }
}
