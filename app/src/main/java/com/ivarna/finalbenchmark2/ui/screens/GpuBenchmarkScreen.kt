package com.ivarna.finalbenchmark2.ui.screens

import android.opengl.GLSurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

/**
 * Dedicated full-screen GPU benchmark screen.
 *
 * Architecture:
 *  ┌─ Box (fillMaxSize) ──────────────────────────────────────────────┐
 *  │  AndroidView(GLSurfaceView)  ← full-screen live render            │
 *  │  TopHudOverlay               ← glass bar: test name + progress    │
 *  │  BottomHudOverlay            ← glass panel: FPS, frametimes, hw   │
 *  └───────────────────────────────────────────────────────────────────┘
 *
 * The [GpuBenchmarkRenderer] is created once and kept alive across recompositions.
 * [GpuBenchmarkViewModel.onFrameMetrics] bridges GL-thread metrics into UI state.
 */
@Composable
fun GpuBenchmarkScreen(
    preset: String,
    historyRepository: HistoryRepository? = null,
    onBenchmarkComplete: (String) -> Unit,
    onNavBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val vmFactory = remember(historyRepository) {
        GpuBenchmarkViewModel.factory(historyRepository, application)
    }
    val viewModel: GpuBenchmarkViewModel = viewModel(factory = vmFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ── Create renderer once ────────────────────────────────────────────
    val renderer = remember {
        GpuBenchmarkRenderer { fps, ft -> viewModel.onFrameMetrics(fps, ft) }
    }

    // ── Sync scene change from VM → renderer ────────────────────────────
    LaunchedEffect(uiState.currentScene) {
        renderer.currentScene = uiState.currentScene
    }

    // ── Completion navigation ───────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.completionEvent.collect { json -> onBenchmarkComplete(json) }
    }

    // ── Start benchmark on first composition ────────────────────────────
    LaunchedEffect(preset) {
        viewModel.start(preset)
    }

    BackHandler {
        viewModel.stop()
        onNavBack()
    }

    // ── Build GLSurfaceView once ─────────────────────────────────────────
    val glView = rememberGlSurfaceView(renderer)

    // ── Root layout ──────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Full-screen OpenGL surface ───────────────────────────────────
        AndroidView(
            factory = { glView },
            modifier = Modifier.fillMaxSize()
        )

        // ── Top HUD ──────────────────────────────────────────────────────
        TopHudOverlay(
            uiState = uiState,
            onStop = { viewModel.stop(); onNavBack() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )

        // ── Bottom HUD ────────────────────────────────────────────────────
        BottomHudOverlay(
            uiState = uiState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Top HUD  – test name, overall progress, scene chips, stop button
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun TopHudOverlay(
    uiState: GpuBenchmarkUiState,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressAnim by animateFloatAsState(
        targetValue = uiState.overallProgress,
        animationSpec = tween(300),
        label = "overall_progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A1A2E).copy(alpha = 0.92f),
                        Color(0xFF16213E).copy(alpha = 0.85f)
                    )
                )
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Scene badge
                val badge = when {
                    uiState.isWarmingUp -> "WARMING UP"
                    uiState.isCompleted -> "COMPLETE"
                    uiState.isRunning   -> "RUNNING"
                    else                -> "READY"
                }
                val badgeColor = when {
                    uiState.isWarmingUp -> Color(0xFFFFA040)
                    uiState.isCompleted -> Color(0xFF40FF80)
                    uiState.isRunning   -> Color(0xFF4080FF)
                    else                -> Color(0xFFAAAAAA)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.20f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            color = badgeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${uiState.currentTestIndex + 1} / ${uiState.totalTests}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = uiState.currentTestName.ifEmpty { "GPU Benchmark" },
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            IconButton(onClick = onStop, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Stop benchmark",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Overall progress bar
        LinearProgressIndicator(
            progress = { progressAnim },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = Color(0xFF4F8EFF),
            trackColor = Color.White.copy(alpha = 0.15f)
        )

        // Per-test mini progress
        if (uiState.isRunning || uiState.isWarmingUp) {
            Spacer(Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { uiState.currentTestProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp)),
                color = if (uiState.isWarmingUp) Color(0xFFFFA040) else Color(0xFF80CFFF),
                trackColor = Color.Transparent
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Bottom HUD – FPS (large), frametime, GPU hw metrics, sparkline
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun BottomHudOverlay(
    uiState: GpuBenchmarkUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF16213E).copy(alpha = 0.85f),
                        Color(0xFF1A1A2E).copy(alpha = 0.92f)
                    )
                )
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ── Row 1: FPS prominent + frametime ────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            // Live FPS – large
            Column {
                Text("FPS", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, letterSpacing = 1.sp)
                Text(
                    text = if (uiState.currentFps > 0f) "%.0f".format(uiState.currentFps) else "—",
                    color = fpsColor(uiState.currentFps),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 42.sp
                )
            }
            Spacer(Modifier.width(20.dp))
            // Avg FPS
            Column {
                Text("AVG", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, letterSpacing = 1.sp)
                Text(
                    text = if (uiState.avgFps > 0f) "%.0f".format(uiState.avgFps) else "—",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.weight(1f))
            // Frametime
            Column(horizontalAlignment = Alignment.End) {
                Text("FRAMETIME", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, letterSpacing = 1.sp)
                Text(
                    text = "${"%.1f".format(uiState.currentFrametimeMs)} ms",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Frametime sparkline ──────────────────────────────────────────
        FrametimeSparkline(
            history = uiState.frametimeHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        )

        Spacer(Modifier.height(10.dp))

        // ── Row 2: HW metrics row ────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HwMetricChip(
                label = "GPU FREQ",
                value = if (uiState.gpuFreqMhz > 0) "${uiState.gpuFreqMhz} MHz" else "— MHz",
                color = Color(0xFF7EB8FF)
            )
            HwMetricChip(
                label = "GPU TEMP",
                value = "${"%.0f".format(uiState.gpuTempC)}°C",
                color = tempColor(uiState.gpuTempC)
            )
            HwMetricChip(
                label = "GPU LOAD",
                value = "${"%.0f".format(uiState.gpuLoadPercent)}%",
                color = Color(0xFFB0FF70)
            )
            HwMetricChip(
                label = "CPU TEMP",
                value = "${"%.0f".format(uiState.cpuTempC)}°C",
                color = tempColor(uiState.cpuTempC)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Frametime sparkline (Canvas)
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun FrametimeSparkline(
    history: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (history.size < 2) return@Canvas

        val target60  = 16.67f
        val maxFt     = history.max().coerceAtLeast(33.3f)
        val w         = size.width
        val h         = size.height
        val stepX     = w / (history.size - 1).toFloat()

        // 60 fps reference line
        val refY = h - (target60 / maxFt * h)
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(0f, refY),
            end   = Offset(w, refY),
            strokeWidth = 1.dp.toPx(),
            cap   = StrokeCap.Round,
            pathEffect = null
        )

        // Fill path
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
                listOf(Color(0xFF4F8EFF).copy(alpha = 0.30f), Color(0xFF4F8EFF).copy(alpha = 0.03f))
            )
        )

        // Line path
        val linePath = Path()
        history.forEachIndexed { i, ft ->
            val x = i * stepX
            val y = h - (ft / maxFt * h).coerceIn(0f, h)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        drawPath(
            path        = linePath,
            color       = Color(0xFF4F8EFF),
            style       = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Small hardware metric chip
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun HwMetricChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            text  = value,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────

private fun fpsColor(fps: Float) = when {
    fps >= 55f -> Color(0xFF40FF80)
    fps >= 30f -> Color(0xFFFFD840)
    fps > 0f   -> Color(0xFFFF5050)
    else       -> Color.White.copy(alpha = 0.5f)
}

private fun tempColor(temp: Float) = when {
    temp >= 80f -> Color(0xFFFF4040)
    temp >= 65f -> Color(0xFFFFAA30)
    else        -> Color(0xFF80DDFF)
}

/**
 * Creates and remembers a [GLSurfaceView] that is properly paused/resumed
 * alongside the host lifecycle.
 */
@Composable
private fun rememberGlSurfaceView(renderer: GpuBenchmarkRenderer): GLSurfaceView {
    val context   = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val glView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
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
