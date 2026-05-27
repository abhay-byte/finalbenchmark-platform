package com.ivarna.finalbenchmark2.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.opengl.GLSurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.ivarna.finalbenchmark2.gpu.GpuBenchmarkRenderer
import com.ivarna.finalbenchmark2.gpu.GpuScene
import com.ivarna.finalbenchmark2.ui.viewmodels.GpuBenchmarkUiState
import com.ivarna.finalbenchmark2.ui.viewmodels.GpuBenchmarkViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.GpuTestResult

// HSL Accent Colors mapped to GPU scenes
private val GPU_TEST_TINT = mapOf(
    GpuScene.TRIANGLE_RENDERING to Color(0xFF4FC3F7),  // light blue
    GpuScene.COMPUTE_MATRIX     to Color(0xFF81C784),  // green
    GpuScene.PARTICLE_SYSTEM    to Color(0xFFFF8A65),  // orange
    GpuScene.TEXTURE_SAMPLING   to Color(0xFFCE93D8),  // purple
    GpuScene.WIREFRAME_MESH     to Color(0xFFFFD54F),  // amber
    GpuScene.MANDELBROT_DEEP    to Color(0xFF4DB6AC),  // teal
    GpuScene.PHONG_MULTI_LIGHT  to Color(0xFF7986CB),  // indigo
    GpuScene.RAY_MARCH_SDF      to Color(0xFFE57373),  // coral red
    GpuScene.DOMAIN_WARP        to Color(0xFF90A4AE),  // blue-grey
    GpuScene.SUPER_SAMPLE       to Color(0xFFAED581)   // light green
)

@Composable
fun GpuBenchmarkScreen(
    preset: String,
    historyRepository: HistoryRepository? = null,
    onBenchmarkComplete: (String) -> Unit,
    onNavBack: () -> Unit,
    isFullBenchmark: Boolean = false
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val application = context.applicationContext as android.app.Application
    val vmFactory = remember(historyRepository) {
        GpuBenchmarkViewModel.factory(historyRepository, application)
    }
    val viewModel: GpuBenchmarkViewModel = viewModel(factory = vmFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Lock to landscape while GPU benchmark runs; restore on exit
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            viewModel.stop()
        }
    }

    // Create renderer once — also wire GL info callback so ViewModel gets real GPU name
    val renderer = remember {
        GpuBenchmarkRenderer(
            onFrameMetrics = { fps, ft -> viewModel.onFrameMetrics(fps, ft) },
            onGpuInfo      = { r, v  -> viewModel.onGpuInfo(r, v) }
        )
    }

    // Sync scene change from VM → renderer
    LaunchedEffect(uiState.currentScene) {
        renderer.currentScene = uiState.currentScene
    }

    // Completion navigation
    LaunchedEffect(Unit) {
        viewModel.completionEvent.collect { json -> onBenchmarkComplete(json) }
    }

    // Start benchmark on first composition
    LaunchedEffect(preset) {
        viewModel.start(preset)
    }

    BackHandler {
        viewModel.stop()
        onNavBack()
    }

    // Build GLSurfaceView once
    val glView = rememberGlSurfaceView(renderer)
    val tint = GPU_TEST_TINT[uiState.currentScene] ?: Color(0xFF4F8EFF)

    // Elapsed & remaining timers calculation (Estimated: 10 scenes x 8s = 80s)
    var elapsedTimeSec by remember { mutableStateOf(0) }
    LaunchedEffect(uiState.isRunning || uiState.isWarmingUp) {
        if (uiState.isRunning || uiState.isWarmingUp) {
            elapsedTimeSec = 0
            while (uiState.isRunning || uiState.isWarmingUp) {
                kotlinx.coroutines.delay(1000L)
                elapsedTimeSec++
            }
        }
    }

    val minutes = elapsedTimeSec / 60
    val seconds = elapsedTimeSec % 60
    val elapsedTimeText = String.format("%02d:%02d", minutes, seconds)

    val totalEstimatedSec = 80
    val remainingSec = maxOf(0, totalEstimatedSec - elapsedTimeSec)
    val remMin = remainingSec / 60
    val remSec = remainingSec % 60
    val remainingTimeText = String.format("%02d:%02d", remMin, remSec)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full-screen OpenGL surface in the background
        AndroidView(
            factory = { glView },
            modifier = Modifier.fillMaxSize()
        )

        // Radial Background Glow Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(tint.copy(alpha = 0.12f), Color.Transparent),
                        radius = 1200f
                    )
                )
        )

        // Landscape split-screen overlays (translucent glass panels)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT PANEL: Metrics Dashboard
            AnimatedVisibility(
                visible = uiState.isRunning || uiState.isWarmingUp,
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                modifier = Modifier.fillMaxHeight()
            ) {
                GpuLeftPanel(uiState = uiState, tint = tint)
            }

            Spacer(modifier = Modifier.weight(1f))

            // RIGHT PANEL: Status, Progress, and Timers
            AnimatedVisibility(
                visible = uiState.isRunning || uiState.isWarmingUp,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier.fillMaxHeight()
            ) {
                GpuRightPanel(
                    uiState = uiState,
                    tint = tint,
                    elapsedTimeText = elapsedTimeText,
                    remainingTimeText = remainingTimeText,
                    isFullBenchmark = isFullBenchmark,
                    onStop = {
                        viewModel.stop()
                        onNavBack()
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Left Panel – Prominent FPS, Frametime line-chart, Hardware Metrics
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun GpuLeftPanel(
    uiState: GpuBenchmarkUiState,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xCC06080D))
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Title Header
            Column {
                Text(
                    text = "LIVE PERFORMANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(1.dp)
                        .width(60.dp)
                        .background(tint)
                )
            }

            // Prominent Live FPS Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LIVE FPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (uiState.currentFps > 0f) "%.0f".format(uiState.currentFps) else "—",
                        color = fpsColor(uiState.currentFps),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 44.sp
                    )
                }

                Divider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp),
                    color = Color.White.copy(alpha = 0.2f)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = "AVERAGE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (uiState.avgFps > 0f) "%.0f".format(uiState.avgFps) else "—",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Frametime details + Live sparkline
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FRAMETIME TIMELINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "${"%.1f".format(uiState.currentFrametimeMs)} ms",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = tint
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                GpuFrametimeSparkline(
                    history = uiState.frametimeHistory,
                    tintColor = tint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                )
            }

            // Hardware Metrics Vertical Column
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GpuHUDMetric(
                    icon = Icons.Default.Speed,
                    label = "GPU FREQ",
                    value = if (uiState.gpuFreqMhz > 0) "${uiState.gpuFreqMhz} MHz" else "— MHz",
                    accentColor = Color(0xFF4FC3F7)
                )
                GpuHUDMetric(
                    icon = Icons.Default.Thermostat,
                    label = "GPU TEMP",
                    value = "${"%.0f".format(uiState.gpuTempC)}°C",
                    accentColor = tempColor(uiState.gpuTempC)
                )
                GpuHUDMetric(
                    icon = Icons.Default.Memory,
                    label = "GPU LOAD",
                    value = "${"%.0f".format(uiState.gpuLoadPercent)}%",
                    accentColor = Color(0xFFB0FF70)
                )
                GpuHUDMetric(
                    icon = Icons.Default.Bolt,
                    label = "WATTAGE",
                    value = if (uiState.powerWatts > 0f) "${"%.1f".format(uiState.powerWatts)} W" else "— W",
                    accentColor = Color(0xFFFFD740)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Right Panel – Test Name, Status indicators, Timer Pill & Stop Button
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun GpuRightPanel(
    uiState: GpuBenchmarkUiState,
    tint: Color,
    elapsedTimeText: String,
    remainingTimeText: String,
    isFullBenchmark: Boolean,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xCC06080D))
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Badge + Scene Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val badgeText = when {
                    uiState.isWarmingUp -> "WARMING UP"
                    uiState.isCompleted -> "COMPLETE"
                    uiState.isRunning   -> "RUNNING"
                    else                -> "READY"
                }
                
                val badgeColor = when {
                    uiState.isWarmingUp -> Color(0xFFFFA040)
                    uiState.isCompleted -> Color(0xFF40FF80)
                    uiState.isRunning   -> tint
                    else                -> Color.Gray
                }

                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val badgeAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.15f * if (uiState.isRunning || uiState.isWarmingUp) badgeAlpha else 1f))
                        .border(0.5.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "${uiState.currentTestIndex + 1} / ${uiState.totalTests}",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Test Title, Description & API chip
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "CURRENT SCENE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.currentTestName.ifEmpty { "Initializing..." },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                // GL API indicator chip
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ApiChip(label = uiState.glApiLabel, color = Color(0xFF4FC3F7))
                    if (uiState.gpuName.isNotEmpty()) {
                        ApiChip(label = uiState.gpuName, color = Color(0xFFB0FF70))
                    }
                }
            }

            // Progress Tracking
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "OVERALL PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${(uiState.overallProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = tint,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                // Glowing Overall Progress
                LinearProgressIndicator(
                    progress = { uiState.overallProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp)),
                    color = tint,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )

                if (uiState.isRunning || uiState.isWarmingUp) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SCENE ENGINE LOAD",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "${(uiState.currentTestProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { uiState.currentTestProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                        color = if (uiState.isWarmingUp) Color(0xFFFFA040) else tint.copy(alpha = 0.7f),
                        trackColor = Color.Transparent
                    )
                }
            }

            // Timers & Close Button Unified row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFullBenchmark) {
                    GpuTimerPill(
                        timeText = remainingTimeText,
                        elapsedTime = elapsedTimeText,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    GpuTimerPill(
                        timeText = remainingTimeText,
                        elapsedTime = elapsedTimeText,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    )

                    // Translucent Close Button
                    Surface(
                        onClick = onStop,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GpuTimerPill(
    timeText: String,
    elapsedTime: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Layer 1: Blurred Background
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .blur(30.dp)
        )
        
        // Layer 2: Border
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
                    RoundedCornerShape(12.dp)
                )
        )

        // Layer 3: Content
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxHeight()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Elapsed: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Text(
                        text = elapsedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rem: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    AnimatedContent(
                        targetState = timeText,
                        transitionSpec = {
                            (slideInVertically { height -> height } + fadeIn())
                                .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                        },
                        label = "GpuTimerAnimation"
                    ) { targetTime ->
                        Text(
                            text = targetTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// UI Helpers & Reusable Composables
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun GpuHUDMetric(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun GpuFrametimeSparkline(
    history: List<Float>,
    tintColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (history.size < 2) return@Canvas

        val target60  = 16.67f
        val maxFt     = history.maxOrNull()?.coerceAtLeast(33.3f) ?: 33.3f
        val w         = size.width
        val h         = size.height
        val stepX     = w / (history.size - 1).toFloat()

        // 60 fps reference line (dashed horizontal reference line)
        val refY = h - (target60 / maxFt * h)
        drawLine(
            color = Color.White.copy(alpha = 0.1f),
            start = Offset(0f, refY),
            end   = Offset(w, refY),
            strokeWidth = 1.dp.toPx()
        )

        // Fill Path
        val fillPath = Path()
        fillPath.moveTo(0f, h)
        history.forEachIndexed { i, ft ->
            val x = i * stepX
            val y = h - (ft / maxFt * h).coerceIn(0f, h)
            if (i == 0) fillPath.lineTo(x, y) else fillPath.lineTo(x, y)
        }
        fillPath.lineTo((history.size - 1) * stepX, h)
        fillPath.close()

        drawPath(
            path  = fillPath,
            brush = Brush.verticalGradient(
                listOf(tintColor.copy(alpha = 0.25f), Color.Transparent)
            )
        )

        // Line Path
        val linePath = Path()
        history.forEachIndexed { i, ft ->
            val x = i * stepX
            val y = h - (ft / maxFt * h).coerceIn(0f, h)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        drawPath(
            path        = linePath,
            color       = tintColor,
            style       = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// API Chip — small pill badge for graphics API / GPU name
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun ApiChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.10f))
            .border(0.5.dp, color.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun fpsColor(fps: Float) = when {

    fps >= 55f -> Color(0xFF40FF80) // Green
    fps >= 30f -> Color(0xFFFFD840) // Yellow
    fps > 0f   -> Color(0xFFFF5050) // Red
    else       -> Color.White.copy(alpha = 0.5f)
}

private fun tempColor(temp: Float) = when {
    temp >= 80f -> Color(0xFFFF4040)
    temp >= 65f -> Color(0xFFFFAA30)
    else        -> Color(0xFF80DDFF)
}

@Composable
private fun rememberGlSurfaceView(renderer: GpuBenchmarkRenderer): GLSurfaceView {
    val context   = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val glView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            preserveEGLContextOnPause = true
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE  -> glView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    return glView
}

