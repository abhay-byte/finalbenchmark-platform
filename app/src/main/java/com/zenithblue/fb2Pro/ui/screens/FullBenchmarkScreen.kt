package com.zenithblue.fb2Pro.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.zenithblue.fb2Pro.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenithblue.fb2Pro.cpuBenchmark.BenchmarkCategory
import com.zenithblue.fb2Pro.data.repository.HistoryRepository
import com.zenithblue.fb2Pro.ui.theme.FB2ProTheme
import com.zenithblue.fb2Pro.ui.viewmodels.FullBenchmarkPhase
import com.zenithblue.fb2Pro.ui.viewmodels.FullBenchmarkState
import com.zenithblue.fb2Pro.ui.viewmodels.FullBenchmarkViewModel
import com.zenithblue.fb2Pro.ui.viewmodels.PhaseStatus
import kotlin.math.roundToInt

// ── Main Screen ───────────────────────────────────────────────────────────────

/**
 * Full Benchmark orchestration screen.
 *
 * Runs CPU → RAM → STORAGE → GPU → PRODUCTIVITY sequentially, then shows a
 * weighted overall score (0–1000) and category breakdown.
 * Category weights (docs): CPU 20%, GPU 20%, RAM 10%, Storage 10%, Productivity 25%
 * (AI 15% excluded as not yet implemented; redistributed proportionally).
 */
@Composable
fun FullBenchmarkScreen(
    preset: String,
    historyRepository: HistoryRepository,
    onBenchmarkComplete: (String) -> Unit,
    onNavBack: () -> Unit
) {
    val viewModel: FullBenchmarkViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Track whether the final result has already been auto-saved so we don't double-insert
    var savedToDb by remember { mutableStateOf(false) }
    // Cache the metricsJson and summaryJson so they can be reused in onDone without stopping monitor twice
    var cachedMetricsJson by remember { mutableStateOf("{}") }
    var cachedSummaryJson by remember { mutableStateOf("") }

    // Start the overall performance monitor when this screen is first composed
    LaunchedEffect(Unit) { viewModel.startMonitoring() }

    // ── Auto-save to Room DB the moment all phases complete ──────────────────
    // This ensures results persist even if the user closes the app before pressing the button.
    LaunchedEffect(state.isComplete) {
        if (state.isComplete && !savedToDb) {
            try {
                val metricsJson = viewModel.stopAndGetMetrics()
                cachedMetricsJson = metricsJson
                val summaryJson = viewModel.buildFinalSummaryJson(metricsJson)
                cachedSummaryJson = summaryJson

                val catScores = try {
                    org.json.JSONObject(summaryJson).optJSONObject("category_scores") ?: org.json.JSONObject()
                } catch (e: Exception) { org.json.JSONObject() }
                val perfMetrics = try {
                    org.json.JSONObject(metricsJson)
                } catch (e: Exception) { org.json.JSONObject() }
                val combinedMetrics = org.json.JSONObject().apply {
                    put("category_scores", catScores)
                    put("performance_metrics", perfMetrics)
                }.toString()

                val phaseDetailsJson = try {
                    org.json.JSONObject(summaryJson).optJSONObject("phase_details")?.toString() ?: "{}"
                } catch (e: Exception) { "{}" }

                val entity = com.zenithblue.fb2Pro.data.database.entities.BenchmarkResultEntity(
                    type                   = "FULL",
                    totalScore             = state.overallScore.toDouble(),
                    timestamp              = System.currentTimeMillis(),
                    deviceModel            = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                    singleCoreScore        = 0.0,
                    multiCoreScore         = state.overallScore.toDouble(),
                    normalizedScore        = state.overallScore.toDouble(),
                    detailedResultsJson    = phaseDetailsJson,
                    performanceMetricsJson = combinedMetrics
                )
                historyRepository.saveGenericBenchmark(entity, emptyList())
                savedToDb = true
                android.util.Log.d("FullBenchmarkScreen", "Full benchmark auto-saved to DB. Score=${state.overallScore}")
            } catch (e: Exception) {
                android.util.Log.e("FullBenchmarkScreen", "Auto-save failed: ${e.message}", e)
            }
        }
    }

    FB2ProTheme {
        // ── Final results ────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.isComplete,
            enter = fadeIn(tween(600)) + scaleIn(tween(600), initialScale = 0.92f),
            exit  = fadeOut(tween(300))
        ) {
            FullBenchmarkResultScreen(
                state = state,
                preset = preset,
                onDone = {
                    // Use cached summaryJson (with real performance_metrics) built during auto-save.
                    // Do NOT call stopAndGetMetrics() again — monitor is already stopped.
                    val summaryJson = if (cachedSummaryJson.isNotEmpty()) cachedSummaryJson
                                      else viewModel.buildFinalSummaryJson(cachedMetricsJson)
                    onBenchmarkComplete(summaryJson)
                }
            )
        }

        // ── Running phases ───────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = !state.isComplete,
            enter = fadeIn(),
            exit  = fadeOut(tween(200))
        ) {
            val currentPhase = state.currentPhase ?: return@AnimatedVisibility

            Box(modifier = Modifier.fillMaxSize()) {

                // Sub-benchmark screen (keyed by phase index so LaunchedEffects reset)
                key(state.currentPhaseIndex) {
                    PhaseSubScreen(
                        phase        = currentPhase,
                        preset       = preset,
                        historyRepo  = historyRepository,
                        onComplete   = { json ->
                            viewModel.recordPhaseScore(currentPhase.category, json)
                        },
                        onNavBack    = onNavBack
                    )
                }

                // Phase progress overlay — floats at the top of the screen
                FullBenchmarkProgressOverlay(
                    state     = state,
                    onNavBack = onNavBack,
                    modifier  = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

// ── Phase sub-screen dispatcher ───────────────────────────────────────────────

@Composable
private fun PhaseSubScreen(
    phase: FullBenchmarkPhase,
    preset: String,
    historyRepo: HistoryRepository,
    onComplete: (String) -> Unit,
    onNavBack: () -> Unit
) {
    when (phase.category) {
        BenchmarkCategory.CPU, BenchmarkCategory.AI -> BenchmarkScreen(
            preset              = preset,
            benchmarkCategory   = phase.category,
            onBenchmarkComplete = onComplete,
            onNavBack           = onNavBack,
            historyRepository   = historyRepo,
            viewModelKey        = "full_${phase.category.name}",
            isFullBenchmark     = true
        )
        BenchmarkCategory.RAM -> RamBenchmarkScreen(
            preset              = preset,
            historyRepository   = historyRepo,
            onBenchmarkComplete = onComplete,
            onNavBack           = onNavBack,
            isFullBenchmark     = true
        )
        BenchmarkCategory.STORAGE -> StorageBenchmarkScreen(
            preset              = preset,
            historyRepository   = historyRepo,
            onBenchmarkComplete = onComplete,
            onNavBack           = onNavBack,
            isFullBenchmark     = true
        )
        BenchmarkCategory.GPU -> GpuBenchmarkScreen(
            preset              = preset,
            historyRepository   = historyRepo,
            onBenchmarkComplete = onComplete,
            onNavBack           = onNavBack,
            isFullBenchmark     = true
        )
        BenchmarkCategory.PRODUCTIVITY -> ProductivityBenchmarkScreen(
            preset              = preset,
            historyRepository   = historyRepo,
            onBenchmarkComplete = onComplete,
            onNavBack           = onNavBack,
            isFullBenchmark     = true
        )
        else -> { /* AI / EXTERNAL_GPU / FULL — not handled here */ }
    }
}

// ── Progress overlay ──────────────────────────────────────────────────────────

@Composable
private fun FullBenchmarkProgressOverlay(
    state: FullBenchmarkState,
    onNavBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val overallProgressAnim by animateFloatAsState(
        targetValue = state.overallProgress,
        animationSpec = tween(600, easing = EaseInOutCubic),
        label = "overall_progress"
    )

    val isGpuPhase = state.currentPhase?.category == BenchmarkCategory.GPU

    if (isGpuPhase) {
        // Floating premium island card to accommodate GPU landscape layout without overlapping sidebars
        Column(
            modifier = modifier
                .statusBarsPadding()
                .padding(top = 8.dp) // Floats slightly below the top bezel/notch safely
                .width(360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xE606080D)) // Matches GpuLeftPanel background
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                .padding(vertical = 10.dp)
        ) {
            // Content Row (Compacted for compact HUD)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_2),
                    contentDescription = "Logo",
                    modifier = Modifier.size(24.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Phase ${state.scores.size + 1}/${state.totalPhases}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Compact Close Button
                    Surface(
                        onClick = onNavBack,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Phase Pills Row (Joined Control curved at ends, scaled nicely for 360dp width)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(28.dp)
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clip(RoundedCornerShape(14.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.phases.forEachIndexed { index, phase ->
                    PhasePill(
                        phase  = phase,
                        status = state.statusOf(phase),
                        index  = index,
                        total  = state.phases.size,
                        modifier = Modifier.weight(1f),
                        isDarkCard = true
                    )
                    if (index < state.phases.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(0.5.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Embedded Glowing progress bar with shiny light at the end
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.06f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(overallProgressAnim)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF6C63FF), // Indigo
                                    Color(0xFF9D63FF), // Violet
                                    Color(0xFFFF63B8)  // Glowing pink
                                )
                            )
                        ),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "bar_shiny")
                    val shinyGlow by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(850, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glow"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(16.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0f),
                                        Color.White.copy(alpha = 0.8f * shinyGlow),
                                        Color.White
                                    )
                                )
                            )
                    )
                }
            }
        }
    } else {
        // Standard Portrait Header bar
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)) // Themed unified bar covering status bar area
                .statusBarsPadding()
                .padding(top = 22.dp) // Pushes content safely completely below the circular camera cutout/notch
        ) {
            // Content Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_2),
                    contentDescription = "Logo",
                    modifier = Modifier.size(28.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Phase ${state.scores.size + 1} of ${state.totalPhases}",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Close button
                    Surface(
                        onClick = onNavBack,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Phase Pills Row (Joined Control curved at ends)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(30.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(15.dp)
                    )
                    .clip(RoundedCornerShape(15.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.phases.forEachIndexed { index, phase ->
                    PhasePill(
                        phase  = phase,
                        status = state.statusOf(phase),
                        index  = index,
                        total  = state.phases.size,
                        modifier = Modifier.weight(1f)
                    )
                    if (index < state.phases.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Premium thin glowing horizontal progress bar with shiny light at the very bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(overallProgressAnim)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "bar_shiny_portrait")
                    val shinyGlow by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(850, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glow"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(20.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0f),
                                        Color.White.copy(alpha = 0.8f * shinyGlow),
                                        Color.White
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PhasePill(
    phase: FullBenchmarkPhase,
    status: PhaseStatus,
    index: Int,
    total: Int,
    modifier: Modifier = Modifier,
    isDarkCard: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_pill")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Breathing scale animation for the active/running pill's content
    val textScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_scale"
    )

    // Moving sweeping shimmer position for the active/running gradient
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 260f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val abbrev = when (phase.category) {
        BenchmarkCategory.CPU          -> "CPU"
        BenchmarkCategory.AI           -> "AI"
        BenchmarkCategory.RAM          -> "RAM"
        BenchmarkCategory.STORAGE      -> "STO"
        BenchmarkCategory.GPU          -> "GPU"
        BenchmarkCategory.PRODUCTIVITY -> "PRO"
        else                           -> phase.category.name.take(3)
    }

    // Dynamic Theme-based colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    // Custom glowing linear sweeping neon gradient for active state
    val runningBrush = if (isDarkCard) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF6C63FF),
                Color(0xFF9D63FF).copy(alpha = pulseGlow),
                Color(0xFFFF63B8),
                Color(0xFF6C63FF)
            ),
            start = Offset(shimmerOffset, 0f),
            end = Offset(shimmerOffset + 120f, 120f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                primaryColor,
                secondaryColor.copy(alpha = pulseGlow),
                tertiaryColor,
                primaryColor
            ),
            start = Offset(shimmerOffset, 0f),
            end = Offset(shimmerOffset + 120f, 120f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .then(
                when (status) {
                    PhaseStatus.DONE -> Modifier
                        .background(
                            if (isDarkCard) Color(0xFF4CAF50).copy(alpha = 0.16f)
                            else Color(0xFF4CAF50).copy(alpha = 0.12f)
                        )
                    PhaseStatus.RUNNING -> Modifier
                        .background(runningBrush)
                    PhaseStatus.PENDING -> Modifier
                        .background(
                            if (isDarkCard) Color(0xFF1E293B).copy(alpha = 0.1f)
                            else onSurfaceColor.copy(alpha = 0.02f)
                        )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.then(
                if (status == PhaseStatus.RUNNING) Modifier.graphicsLayer(scaleX = textScale, scaleY = textScale)
                else Modifier
            )
        ) {
            if (status == PhaseStatus.DONE) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "${phase.displayName} done",
                    tint = if (isDarkCard) Color(0xFF81C784) else Color(0xFF4CAF50),
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Text(
                    text = abbrev,
                    color = when (status) {
                        PhaseStatus.RUNNING -> {
                            if (isDarkCard) Color.White
                            else MaterialTheme.colorScheme.onPrimary
                        }
                        else -> {
                            if (isDarkCard) Color.White.copy(alpha = 0.35f)
                            else onSurfaceColor.copy(alpha = 0.4f)
                        }
                    },
                    fontSize = 9.sp,
                    fontWeight = if (status == PhaseStatus.RUNNING) FontWeight.ExtraBold else FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ── Final results screen ──────────────────────────────────────────────────────

@Composable
fun FullBenchmarkResultScreen(
    state: FullBenchmarkState,
    preset: String,
    onDone: () -> Unit
) {
    val rankInfo = remember(state.overallScore) {
        when {
            state.overallScore >= 800 -> "ELITE PERFORMER" to Color(0xFF6C63FF)
            state.overallScore >= 600 -> "HIGH END" to Color(0xFF00BCD4)
            state.overallScore >= 400 -> "MID-RANGE" to Color(0xFFFF9800)
            else -> "BUDGET PERFORMER" to Color(0xFFE91E63)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        val screenHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Score Count-up Animation state ──
            var targetScore by remember { mutableStateOf(0) }
            var animationFinished by remember { mutableStateOf(false) }

            val animatedScore by animateIntAsState(
                targetValue = targetScore,
                animationSpec = tween(durationMillis = 1500, easing = EaseInOutCubic),
                label = "score_count_up",
                finishedListener = { _ -> animationFinished = true }
            )

            LaunchedEffect(state.overallScore) {
                targetScore = state.overallScore
            }

            // Top spacing: 20% of screen height
            Spacer(modifier = Modifier.height(screenHeight * 0.20f))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                val glowColor = MaterialTheme.colorScheme.primary
                // Background radial glow
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        glowColor.copy(alpha = 0.12f),
                                        Color.Transparent
                                    ),
                                    radius = size.minDimension * 0.5f
                                )
                            )
                        }
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // ── Stylized brand header ──
                    Text(
                        text = "FINALBENCHMARK",
                        style = TextStyle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            ),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 6.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // ── Big score number (no out of 1000) ──
                    Text(
                        text = "$animatedScore",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 120.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-4).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 120.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Gamified Performance Rank Badge ──
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, rankInfo.second.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(rankInfo.second, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = rankInfo.first,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = rankInfo.second
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // ── Category breakdown & Save button (appears after count-up completes) ──
            AnimatedVisibility(
                visible = animationFinished,
                enter = fadeIn(animationSpec = tween(durationMillis = 500)) +
                        expandVertically(animationSpec = tween(durationMillis = 500))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Unified edge-to-edge container (takes up full width, no side spacing!)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp), // flat corners for edge-to-edge look
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.06f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            state.phases.forEachIndexed { index, phase ->
                                val rawScore = state.scores[phase.category] ?: 0.0
                                val phaseJson = state.phaseJsons[phase.category]
                                val categoryColor = when (phase.category) {
                                    BenchmarkCategory.CPU          -> Color(0xFF6C63FF)
                                    BenchmarkCategory.AI           -> Color(0xFFE91E63)
                                    BenchmarkCategory.RAM          -> Color(0xFF00BCD4)
                                    BenchmarkCategory.STORAGE      -> Color(0xFF8BC34A)
                                    BenchmarkCategory.GPU          -> Color(0xFFFF5722)
                                    BenchmarkCategory.PRODUCTIVITY -> Color(0xFFFF9800)
                                    else                           -> Color(0xFF9E9E9E)
                                }

                                CategoryRow(
                                    displayName   = phase.displayName,
                                    rawScore      = rawScore,
                                    categoryColor = categoryColor,
                                    phaseJson     = phaseJson,
                                    categoryKey   = phase.category.name
                                )

                                if (index < state.phases.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.05f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // ── Save & Close button (Glass effect with proper theming) ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Button(
                            onClick = onDone,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(28.dp)
                                ),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.05f),
                                                Color.Transparent
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text        = "Save & Close",
                                    fontWeight  = FontWeight.Bold,
                                    fontSize    = 16.sp,
                                    color       = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Subcomponents for the Unified Breakdown Panel ──

private data class SubTestResult(
    val name: String,
    val scoreText: String,
    val isMultiCore: Boolean
)

@Composable
private fun SubSectionHeader(text: String, categoryColor: Color) {
    Row(
        modifier = Modifier
            .padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(categoryColor, RoundedCornerShape(1.5.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun SubTestRow(idx: Int, name: String, scoreText: String, categoryColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (idx % 2 == 0) Color.Transparent
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.015f)
            )
            .padding(start = 32.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(categoryColor.copy(alpha = 0.5f), CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text      = name,
                style     = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                ),
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text       = scoreText,
            style      = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = categoryColor
            )
        )
    }
}

@Composable
private fun CategoryRow(
    displayName   : String,
    rawScore      : Double,
    categoryColor : Color,
    phaseJson     : String?,
    categoryKey   : String = ""
) {
    var expanded by remember { mutableStateOf(false) }

    // Parse sub-test list from the phase JSON
    val subTests: List<SubTestResult> = remember(phaseJson, categoryKey) {
        if (phaseJson == null) return@remember emptyList()
        try {
            val obj = org.json.JSONObject(phaseJson)
            val dr  = obj.optJSONArray("detailed_results") ?: return@remember emptyList()
            (0 until dr.length()).mapNotNull { i ->
                val item = dr.getJSONObject(i)
                val rawName = item.optString("name", "Test ${i + 1}")
                val name = rawName.removePrefix("Single-Core ").removePrefix("Multi-Core ")
                val metricsStr = item.optString("metricsJson", "{}")
                val opsPerSecond = item.optDouble("opsPerSecond", 0.0)
                val isMultiCore = rawName.startsWith("Multi-Core")

                // Try metricsJson score first (GPU/RAM/STORAGE/AI), else compute for CPU
                val score = try {
                    val metricsObj = org.json.JSONObject(metricsStr)
                    val fromMetrics = metricsObj.optDouble("score", -1.0)
                    if (fromMetrics >= 0) {
                        fromMetrics
                    } else when (categoryKey) {
                        "CPU" -> {
                            val benchmarkName = com.zenithblue.fb2Pro.cpuBenchmark.BenchmarkName.fromString(rawName)
                            val factor = benchmarkName?.let {
                                com.zenithblue.fb2Pro.cpuBenchmark.KotlinBenchmarkManager.SCORING_FACTORS[it]
                            } ?: 0.0
                            if (factor > 0) opsPerSecond * factor else -1.0
                        }
                        "AI" -> {
                            val benchmarkName = com.zenithblue.fb2Pro.cpuBenchmark.BenchmarkName.fromString(rawName)
                            val factor = benchmarkName?.let {
                                com.zenithblue.fb2Pro.cpuBenchmark.KotlinBenchmarkManager.AI_PER_TEST_SCORING_FACTORS[it]
                            } ?: 0.0
                            if (factor > 0) opsPerSecond * factor else -1.0
                        }
                        else -> -1.0
                    }
                } catch (_: Exception) { -1.0 }
                val scoreText = if (score >= 0) score.roundToInt().toString() else "—"
                SubTestResult(name, scoreText, isMultiCore)
            }
        } catch (_: Exception) { emptyList() }
    }

    val hasSubTests = subTests.isNotEmpty()

    val categoryIcon = when (categoryKey) {
        "CPU"          -> Icons.Rounded.Memory
        "AI"           -> Icons.Rounded.AutoAwesome
        "RAM"          -> Icons.Rounded.Layers
        "STORAGE"      -> Icons.Rounded.Storage
        "GPU"          -> Icons.Rounded.Speed
        "PRODUCTIVITY" -> Icons.Rounded.FactCheck
        else           -> Icons.Rounded.Memory
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (hasSubTests) Modifier.clickable { expanded = !expanded } else Modifier)
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Left colour strip
                    drawRect(
                        color = categoryColor,
                        size  = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)
                    )
                }
                .padding(start = 20.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = categoryIcon,
                contentDescription = null,
                tint = categoryColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text      = displayName,
                modifier  = Modifier.weight(1f),
                style     = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                color     = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text          = rawScore.roundToInt().toString(),
                style         = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                ),
                color         = categoryColor,
                letterSpacing = (-0.5).sp
            )
            if (hasSubTests) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text     = if (expanded) "▲" else "▼",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                )
            }
        }

        // Sub-test rows (animated dropdown)
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
                )
                
                if (categoryKey == "CPU") {
                    val singleCoreTests = subTests.filter { !it.isMultiCore }
                    val multiCoreTests = subTests.filter { it.isMultiCore }

                    // Single-core header
                    if (singleCoreTests.isNotEmpty()) {
                        SubSectionHeader("Single-Core Tests", categoryColor)
                        singleCoreTests.forEachIndexed { idx, test ->
                            SubTestRow(idx, test.name, test.scoreText, categoryColor)
                        }
                    }

                    // Divider between single-core and multi-core
                    if (singleCoreTests.isNotEmpty() && multiCoreTests.isNotEmpty()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                        )
                    }

                    // Multi-core header
                    if (multiCoreTests.isNotEmpty()) {
                        SubSectionHeader("Multi-Core Tests", categoryColor)
                        multiCoreTests.forEachIndexed { idx, test ->
                            SubTestRow(idx, test.name, test.scoreText, categoryColor)
                        }
                    }
                } else {
                    subTests.forEachIndexed { idx, test ->
                        SubTestRow(idx, test.name, test.scoreText, categoryColor)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FullBenchmarkLastResultScreen(
    historyRepository: com.zenithblue.fb2Pro.data.repository.HistoryRepository,
    onBackClick: () -> Unit
) {
    val resultsState = historyRepository.getResultsByType("FULL").collectAsState(initial = null)
    val results = resultsState.value

    if (results == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (results.isEmpty()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Last Result",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FactCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Benchmark Run Found",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You haven't run a full benchmark yet. Start one from the Home screen to see results here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    } else {
        val lastResult = results.first().benchmarkResult
        val state = remember(lastResult) {
            val overallScore = lastResult.totalScore.toInt()
            val scores = mutableMapOf<com.zenithblue.fb2Pro.cpuBenchmark.BenchmarkCategory, Double>()
            val phaseJsons = mutableMapOf<com.zenithblue.fb2Pro.cpuBenchmark.BenchmarkCategory, String>()
            
            try {
                val pmObj = org.json.JSONObject(lastResult.performanceMetricsJson)
                val catScoresObj = pmObj.optJSONObject("category_scores")
                if (catScoresObj != null) {
                    catScoresObj.keys().forEach { key ->
                        try {
                            val category = com.zenithblue.fb2Pro.cpuBenchmark.BenchmarkCategory.valueOf(key)
                            scores[category] = catScoresObj.optDouble(key, 0.0)
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}

            try {
                val pdObj = org.json.JSONObject(lastResult.detailedResultsJson)
                pdObj.keys().forEach { key ->
                    try {
                        val category = com.zenithblue.fb2Pro.cpuBenchmark.BenchmarkCategory.valueOf(key)
                        phaseJsons[category] = pdObj.optString(key, "")
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}

            FullBenchmarkState(
                phases = com.zenithblue.fb2Pro.ui.viewmodels.FullBenchmarkViewModel.PHASES,
                scores = scores,
                phaseJsons = phaseJsons,
                isComplete = true,
                overallScore = overallScore
            )
        }
        
        FullBenchmarkResultScreen(
            state = state,
            preset = "Auto",
            onDone = onBackClick
        )
    }
}
