package com.ivarna.finalbenchmark2.ui.screens

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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.ivarna.finalbenchmark2.R
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
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
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.ui.viewmodels.FullBenchmarkPhase
import com.ivarna.finalbenchmark2.ui.viewmodels.FullBenchmarkState
import com.ivarna.finalbenchmark2.ui.viewmodels.FullBenchmarkViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.PhaseStatus
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

                val entity = com.ivarna.finalbenchmark2.data.database.entities.BenchmarkResultEntity(
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

    FinalBenchmark2Theme {
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
private fun FullBenchmarkResultScreen(
    state: FullBenchmarkState,
    preset: String,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Big score ring ────────────────────────────────────────────────
            ScoreRing(score = state.overallScore)

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(20.dp))

            // ── Category breakdown ────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.phases.forEach { phase ->
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
                ExpandableCategoryRow(
                        displayName   = phase.displayName,
                        rawScore      = rawScore,
                        categoryColor = categoryColor,
                        phaseJson     = phaseJson,
                        categoryKey   = phase.category.name
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Save & Close button ───────────────────────────────────────────
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation  = 8.dp
                )
            ) {
                Text(
                    text        = "Save & Close",
                    fontWeight  = FontWeight.Bold,
                    fontSize    = 16.sp,
                    color       = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// ── Score ring (simplified – score number only) ───────────────────────────────

@Composable
private fun ScoreRing(score: Int) {
    val progressAnim by animateFloatAsState(
        targetValue    = (score / 1000f).coerceIn(0f, 1f),
        animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label          = "score_progress"
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor   = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)

    Box(
        modifier           = Modifier.size(230.dp),
        contentAlignment   = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val radius      = (size.minDimension - strokeWidth - 36.dp.toPx()) / 2f
            val center      = Offset(size.width / 2f, size.height / 2f)

            // Tick marks
            val numTicks = 40
            for (i in 0..numTicks) {
                val angle      = 135f + (270f * i / numTicks)
                val isGlow     = i <= (progressAnim * numTicks)
                val tickColor  = if (isGlow) primaryColor.copy(alpha = 0.8f) else trackColor.copy(alpha = 0.25f)
                val tickLength = if (i % 5 == 0) 8.dp.toPx() else 4.dp.toPx()
                val tickWidth  = if (i % 5 == 0) 2.5.dp.toPx() else 1.2f.dp.toPx()
                val rad        = Math.toRadians(angle.toDouble())
                val r0         = radius + 12.dp.toPx()
                drawLine(
                    color       = tickColor,
                    start       = Offset(center.x + r0 * Math.cos(rad).toFloat(), center.y + r0 * Math.sin(rad).toFloat()),
                    end         = Offset(center.x + (r0 + tickLength) * Math.cos(rad).toFloat(), center.y + (r0 + tickLength) * Math.sin(rad).toFloat()),
                    strokeWidth = tickWidth,
                    cap         = StrokeCap.Round
                )
            }

            // Track arc
            drawArc(
                color       = trackColor,
                startAngle  = 135f,
                sweepAngle  = 270f,
                useCenter   = false,
                style       = Stroke(strokeWidth, cap = StrokeCap.Round),
                topLeft     = Offset(center.x - radius, center.y - radius),
                size        = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )

            // Progress arc
            drawArc(
                brush       = Brush.sweepGradient(listOf(primaryColor.copy(alpha = 0.5f), primaryColor), center = center),
                startAngle  = 135f,
                sweepAngle  = 270f * progressAnim,
                useCenter   = false,
                style       = Stroke(strokeWidth, cap = StrokeCap.Round),
                topLeft     = Offset(center.x - radius, center.y - radius),
                size        = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }

        // Center typography: score + "/1000"
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text  = "$score",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize      = 58.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = (-2).sp
                ),
                color      = MaterialTheme.colorScheme.onBackground,
                lineHeight = 58.sp
            )
            Text(
                text         = "/ 1000",
                style        = MaterialTheme.typography.labelSmall,
                color        = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                letterSpacing = 1.sp,
                fontWeight   = FontWeight.Bold
            )
        }
    }
}

// ── Expandable category row ───────────────────────────────────────────────────

@Composable
private fun ExpandableCategoryRow(
    displayName   : String,
    rawScore      : Double,
    categoryColor : Color,
    phaseJson     : String?,
    categoryKey   : String = ""
) {
    var expanded by remember { mutableStateOf(false) }

    // Parse sub-test list from the phase JSON
    val subTests: List<Pair<String, String>> = remember(phaseJson, categoryKey) {
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

                // Try metricsJson score first (GPU/RAM/STORAGE/AI), else compute for CPU
                val score = try {
                    val metricsObj = org.json.JSONObject(metricsStr)
                    val fromMetrics = metricsObj.optDouble("score", -1.0)
                    if (fromMetrics >= 0) {
                        fromMetrics
                    } else when (categoryKey) {
                        "CPU" -> {
                            val benchmarkName = com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkName.fromString(rawName)
                            val factor = benchmarkName?.let {
                                com.ivarna.finalbenchmark2.cpuBenchmark.KotlinBenchmarkManager.SCORING_FACTORS[it]
                            } ?: 0.0
                            if (factor > 0) opsPerSecond * factor else -1.0
                        }
                        "AI" -> {
                            val benchmarkName = com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkName.fromString(rawName)
                            val factor = benchmarkName?.let {
                                com.ivarna.finalbenchmark2.cpuBenchmark.KotlinBenchmarkManager.AI_PER_TEST_SCORING_FACTORS[it]
                            } ?: 0.0
                            if (factor > 0) opsPerSecond * factor else -1.0
                        }
                        else -> -1.0
                    }
                } catch (_: Exception) { -1.0 }
                val scoreText = if (score >= 0) score.roundToInt().toString() else "—"
                name to scoreText
            }
        } catch (_: Exception) { emptyList() }
    }

    val hasSubTests = subTests.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (hasSubTests) Modifier.clickable { expanded = !expanded } else Modifier),
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.10f)
        ),
        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.20f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                    .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text      = displayName,
                    modifier  = Modifier.weight(1f),
                    style     = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color     = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text          = rawScore.roundToInt().toString(),
                    style         = MaterialTheme.typography.titleLarge,
                    fontWeight    = FontWeight.Black,
                    color         = categoryColor,
                    letterSpacing = (-0.5).sp
                )
                if (hasSubTests) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text     = if (expanded) "▲" else "▼",
                        fontSize = 10.sp,
                        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                    )
                }
            }

            // Sub-test rows (animated dropdown)
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
                    )
                    subTests.forEachIndexed { idx, (name, scoreText) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (idx % 2 == 0) Color.Transparent
                                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.025f)
                                )
                                .padding(start = 20.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                text      = name,
                                modifier  = Modifier.weight(1f),
                                style     = MaterialTheme.typography.bodySmall,
                                color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                                maxLines  = 2,
                                overflow  = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text       = scoreText,
                                style      = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color      = categoryColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
