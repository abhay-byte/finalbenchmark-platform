package com.ivarna.finalbenchmark2.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.ivarna.finalbenchmark2.ui.viewmodels.ProductivityBenchmarkUiState
import com.ivarna.finalbenchmark2.ui.viewmodels.ProductivityBenchmarkViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.ProductivityTest
import com.ivarna.finalbenchmark2.ui.viewmodels.ProductivityTestResult

// Colors per test for radial backgrounds
private val PROD_TEST_TINT = mapOf(
    ProductivityTest.CANVAS_OPS      to Color(0xFF7C4DFF),  // deep purple
    ProductivityTest.IMAGE_FILTER    to Color(0xFF00BCD4),  // cyan
    ProductivityTest.IMAGE_RESIZE    to Color(0xFF00E5FF),  // light cyan
    ProductivityTest.TEXT_OPS        to Color(0xFF69F0AE),  // green accent
    ProductivityTest.JSON_OPS        to Color(0xFFFFB300),  // amber
    ProductivityTest.COMPRESSION     to Color(0xFFFF4081),  // pink accent
    ProductivityTest.VIDEO_ENCODE    to Color(0xFFE040FB),  // purple A200
    ProductivityTest.VIDEO_DECODE    to Color(0xFFFF6D00),  // deep orange A400
    ProductivityTest.VIDEO_TRANSCODE to Color(0xFF00E676),  // green A400
)

private enum class ProdTestStatus {
    PENDING, RUNNING, COMPLETED
}

private data class ProdTestState(
    val test: ProductivityTest,
    val name: String,
    val status: ProdTestStatus,
    val score: Int = 0,
    val value: Double = 0.0,
    val unit: String = "",
    val timeText: String = ""
)

private data class ProductivityStats(
    val cpuLoad: Int,
    val temp: Float,
    val score: Int,
    val tint: Color
)

@Composable
fun ProductivityBenchmarkScreen(
    preset: String,
    historyRepository: HistoryRepository? = null,
    onBenchmarkComplete: (String) -> Unit,
    onNavBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val vmFactory = remember(historyRepository) {
        ProductivityBenchmarkViewModel.factory(historyRepository, application)
    }
    val vm: ProductivityBenchmarkViewModel = viewModel(factory = vmFactory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val previewBitmap by vm.previewBitmapFlow.collectAsStateWithLifecycle()

    BackHandler(enabled = state.isRunning || state.isWarmingUp) {
        vm.stop()
        onNavBack()
    }

    LaunchedEffect(Unit) { vm.start(preset) }
    LaunchedEffect(vm.completionEvent) {
        vm.completionEvent.collect { json -> onBenchmarkComplete(json) }
    }

    val tint = PROD_TEST_TINT[state.currentTest] ?: Color(0xFF7C4DFF)
    val scrollState = rememberLazyListState()
    var listCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    // Map 9 tests to states
    val testStates = remember(state.currentTest, state.completedTests, state.isRunning) {
        ProductivityTest.values().map { test ->
            val completed = state.completedTests.firstOrNull { it.test == test }
            val isCurrent = state.currentTest == test && state.isRunning
            
            val status = when {
                isCurrent -> ProdTestStatus.RUNNING
                completed != null -> ProdTestStatus.COMPLETED
                else -> ProdTestStatus.PENDING
            }
            
            val displayName = when (test) {
                ProductivityTest.TEXT_OPS        -> "Text Corpus Processing"
                ProductivityTest.JSON_OPS        -> "JSON Serialization"
                ProductivityTest.COMPRESSION     -> "Corpus Compression"
                ProductivityTest.CANVAS_OPS      -> "2D Canvas Rendering"
                ProductivityTest.IMAGE_FILTER    -> "4K Image Filtering"
                ProductivityTest.IMAGE_RESIZE    -> "4K Image Resizing"
                ProductivityTest.VIDEO_ENCODE    -> "1080p Video Encoding"
                ProductivityTest.VIDEO_DECODE    -> "1080p Video Decoding"
                ProductivityTest.VIDEO_TRANSCODE -> "Video Transcoding"
            }
            
            val score = completed?.score ?: 0
            val value = completed?.value ?: if (isCurrent) state.currentValue else 0.0
            val unit = completed?.unit ?: if (isCurrent) state.currentUnit else ""
            
            val timeText = if (completed != null) {
                when (unit) {
                    "Mchars/s" -> String.format("%.2f Mchars/s", value)
                    "docs/s"   -> String.format("%.0f docs/s", value)
                    "images/s" -> String.format("%.1f imgs/s", value)
                    "ops/s"    -> String.format("%.0f ops/s", value)
                    "MB/s"     -> String.format("%.1f MB/s", value)
                    "fps"      -> String.format("%.1f fps", value)
                    else       -> String.format("%.1f %s", value, unit)
                }
            } else if (isCurrent && state.currentValue > 0.0) {
                when (state.currentUnit) {
                    "Mchars/s" -> String.format("%.2f Mchars/s", state.currentValue)
                    "docs/s"   -> String.format("%.0f docs/s", state.currentValue)
                    "images/s" -> String.format("%.1f imgs/s", state.currentValue)
                    "ops/s"    -> String.format("%.0f ops/s", state.currentValue)
                    "MB/s"     -> String.format("%.1f MB/s", state.currentValue)
                    "fps"      -> String.format("%.1f fps", state.currentValue)
                    else       -> String.format("%.1f %s", state.currentValue, state.currentUnit)
                }
            } else {
                ""
            }
            
            ProdTestState(
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
    val activeIndex = testStates.indexOfFirst { it.status == ProdTestStatus.RUNNING }
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            val baseListIndex = when (activeIndex) {
                in 0..2 -> 1 + activeIndex
                in 3..5 -> 5 + (activeIndex - 3)
                else -> 9 + (activeIndex - 6)
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

    val totalEstimatedSec = 34
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

    val productivityStats = remember(cpuLoadVal, state.cpuTempC, currentScore, tint) {
        ProductivityStats(
            cpuLoad = cpuLoadVal,
            temp = state.cpuTempC,
            score = currentScore,
            tint = tint
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020407))
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
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 2.sp
                    )
                    
                    val configTitle = when (preset.lowercase()) {
                        "slow" -> "Low Accuracy - Fastest (Productivity)"
                        "mid" -> "Mid Accuracy - Fast (Productivity)"
                        "flagship" -> "High Accuracy - Slow (Productivity)"
                        else -> "${preset.replace("Workload: ", "")} (Productivity)"
                    }
                    
                    Text(
                        text = configTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
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

            // Uniform Reactor Progress
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ProdReactorProgress(progress = state.overallProgress, accentColor = tint)
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    ) {
                        Text(
                            text = "${(state.overallProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White,
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

            // Reimagined Live Preview Viewport
            AnimatedVisibility(
                visible = previewBitmap != null && !(previewBitmap!!.isRecycled) && (state.isRunning || state.isWarmingUp),
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                ) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Current frame",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Bottom shadow for readability
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                )
                            )
                    )
                    Text(
                        text = state.currentOperationDetail.ifEmpty { "Processing task..." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    // Resolution Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when (state.currentTest) {
                                ProductivityTest.IMAGE_FILTER    -> "4K FILTER"
                                ProductivityTest.IMAGE_RESIZE    -> "4K→QHD"
                                ProductivityTest.VIDEO_ENCODE    -> "1080p ENC"
                                ProductivityTest.VIDEO_DECODE    -> "1080p DEC"
                                ProductivityTest.VIDEO_TRANSCODE -> "1080p→720p"
                                else -> "PREVIEW"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = tint,
                            fontSize = 9.sp
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
                // Section 1: Office Operations
                item { ProdSectionHeader(title = "OFFICE OPERATIONS", tint = tint) }
                items(testStates.subList(0, 3)) { test ->
                    Box(modifier = Modifier.prodWheelCurve(scrollState, listCoordinates)) {
                        ProdTimelineTestRow(test = test, activeTint = tint)
                    }
                }

                // Section 2: Media Operations
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { ProdSectionHeader(title = "MEDIA OPERATIONS", tint = tint) }
                items(testStates.subList(3, 6)) { test ->
                    Box(modifier = Modifier.prodWheelCurve(scrollState, listCoordinates)) {
                        ProdTimelineTestRow(test = test, activeTint = tint)
                    }
                }

                // Section 3: Video Engine
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { ProdSectionHeader(title = "VIDEO ENGINE", tint = tint) }
                items(testStates.subList(6, 9)) { test ->
                    Box(modifier = Modifier.prodWheelCurve(scrollState, listCoordinates)) {
                        ProdTimelineTestRow(test = test, activeTint = tint)
                    }
                }
            }
        }

        // Fixed Bottom HUD Monitor
        ProdHUDMonitor(
            stats = productivityStats,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun ProdReactorProgress(progress: Float, accentColor: Color) {
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

    Canvas(modifier = Modifier.size(240.dp)) {
        val radius = size.width / 2

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
            style = Stroke(width = 16f, cap = StrokeCap.Round)
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
private fun ProdSectionHeader(title: String, tint: Color) {
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
            color = Color.White.copy(alpha = 0.7f),
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
private fun ProdTimelineTestRow(
    test: ProdTestState,
    activeTint: Color
) {
    val isRunning = test.status == ProdTestStatus.RUNNING
    val isCompleted = test.status == ProdTestStatus.COMPLETED
    
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
                    ProdRunningBenchmarkIndicator(activeTint)
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
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Test name and score/results
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = test.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isRunning) activeTint else Color.White,
                    fontWeight = if (isRunning) FontWeight.ExtraBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (test.timeText.isNotEmpty()) {
                    Text(
                        text = test.timeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isRunning) activeTint else Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProdRunningBenchmarkIndicator(tint: Color) {
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
private fun ProdHUDMonitor(
    stats: ProductivityStats,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(340.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(Color(0xCC06080D))
            .border(
                1.dp,
                Color(0x33FFFFFF),
                RoundedCornerShape(40.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProdHUDMetric(
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

            ProdHUDMetric(
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

            ProdHUDMetric(
                icon = Icons.Default.WorkOutline,
                label = "SCORE",
                value = "${stats.score}",
                accentColor = Color(0xFF00BCD4)
            )
        }
    }
}

@Composable
private fun ProdHUDMetric(
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
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp
        )
    }
}

private fun Modifier.prodWheelCurve(
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
