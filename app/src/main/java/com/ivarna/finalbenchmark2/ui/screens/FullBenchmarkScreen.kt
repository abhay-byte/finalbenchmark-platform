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

    // Start the overall performance monitor when this screen is first composed
    LaunchedEffect(Unit) { viewModel.startMonitoring() }

    // ── Auto-save to Room DB the moment all phases complete ──────────────────
    // This ensures results persist even if the user closes the app before pressing the button.
    LaunchedEffect(state.isComplete) {
        if (state.isComplete && !savedToDb) {
            try {
                val metricsJson = viewModel.stopAndGetMetrics()
                val summaryJson = viewModel.buildFinalSummaryJson(metricsJson)

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
                    // Build the final JSON for the result screen navigation.
                    // DB save already happened via LaunchedEffect above.
                    val metricsJson = viewModel.stopAndGetMetrics()
                    val summaryJson = viewModel.buildFinalSummaryJson(metricsJson)
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
    val gradeColor = when (state.grade) {
        "A+" -> Color(0xFFFFD700)
        "A"  -> Color(0xFF4CAF50)
        "B+" -> Color(0xFF8BC34A)
        "B"  -> Color(0xFF03A9F4)
        "C"  -> Color(0xFFFF9800)
        "D"  -> Color(0xFFFF5722)
        else -> Color(0xFFF44336)
    }

    val ratingText = when {
        state.overallScore >= 850 -> "ELITE EXTREME"
        state.overallScore >= 700 -> "FLAGSHIP POWER"
        state.overallScore >= 550 -> "HIGH PERFORMANCE"
        state.overallScore >= 400 -> "MID-RANGE GEAR"
        state.overallScore >= 250 -> "BUDGET PERFORMER"
        else -> "LOW PERFORMANCE"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding() // Pushes everything safely completely below the physical camera cutout/notch
    ) {
        // Subtle Theme Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Top Header ────────────────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "BENCHMARK REPORT",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Full Performance Analysis",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ── Big score ring dial ───────────────────────────────────────────────
            ScoreRing(
                score      = state.overallScore,
                grade      = state.grade,
                gradeColor = gradeColor,
                ratingText = ratingText
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

            // ── Category breakdown ────────────────────────────────────────────
            Text(
                text = "Detailed Category Analysis",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.phases.forEach { phase ->
                    val rawScore = state.scores[phase.category] ?: 0.0
                    val pct = ((rawScore / phase.maxScore) * 100.0).coerceIn(0.0, 100.0)
                    
                    val barColor = when (phase.category) {
                        BenchmarkCategory.CPU         -> Color(0xFF6C63FF)
                        BenchmarkCategory.AI          -> Color(0xFFE91E63)
                        BenchmarkCategory.RAM         -> Color(0xFF00BCD4)
                        BenchmarkCategory.STORAGE     -> Color(0xFF8BC34A)
                        BenchmarkCategory.GPU         -> Color(0xFFFF5722)
                        BenchmarkCategory.PRODUCTIVITY -> Color(0xFFFF9800)
                        else                          -> Color(0xFF9E9E9E)
                    }

                    CategoryScoreCard(
                        phase        = phase,
                        scorePercent = pct.toFloat(),
                        categoryColor = barColor
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

            // ── Environment specs log ─────────────────────────────────────────
            DeviceReportCard(preset = preset)

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

            // ── Scoring methodology note ──────────────────────────────────────
            ScoringNoteCard()

            Spacer(modifier = Modifier.height(16.dp))

            // ── Save/Done button ──────────────────────────────────────────────
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
                    pressedElevation = 8.dp
                )
            ) {
                Text(
                    text = "Save & Close Report",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun ScoreRing(
    score: Int,
    grade: String,
    gradeColor: Color,
    ratingText: String
) {
    val progressAnim by animateFloatAsState(
        targetValue = score / 1000f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "score_progress"
    )
    val primaryColor = gradeColor
    val trackColor   = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)

    Box(
        modifier = Modifier.size(230.dp),
        contentAlignment = Alignment.Center
    ) {
        // High-tech dial background & reference ticks
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val radius = (size.minDimension - strokeWidth - 36.dp.toPx()) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // 1. Ticks indicator glow ring
            val numTicks = 40
            for (i in 0..numTicks) {
                val angle = 135f + (270f * i / numTicks)
                val isGlow = i <= (progressAnim * numTicks)
                val tickColor = if (isGlow) primaryColor.copy(alpha = 0.8f) else trackColor.copy(alpha = 0.25f)
                val tickLength = if (i % 5 == 0) 8.dp.toPx() else 4.dp.toPx()
                val tickWidth = if (i % 5 == 0) 2.5.dp.toPx() else 1.2f.dp.toPx()

                val rad = Math.toRadians(angle.toDouble())
                val startX = center.x + (radius + 12.dp.toPx()) * Math.cos(rad).toFloat()
                val startY = center.y + (radius + 12.dp.toPx()) * Math.sin(rad).toFloat()
                val endX = center.x + (radius + 12.dp.toPx() + tickLength) * Math.cos(rad).toFloat()
                val endY = center.y + (radius + 12.dp.toPx() + tickLength) * Math.sin(rad).toFloat()

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round
                )
            }

            // 2. Track background
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )

            // 3. Glowing Progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(primaryColor.copy(alpha = 0.5f), primaryColor),
                    center = center
                ),
                startAngle = 135f,
                sweepAngle = 270f * progressAnim,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }

        // Center typography and pill grade
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text  = "$score",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 52.sp
            )
            Text(
                text  = "PTS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Grade pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(gradeColor.copy(alpha = 0.15f))
                    .border(1.dp, gradeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "GRADE $grade",
                    color = gradeColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = ratingText,
                style = MaterialTheme.typography.labelSmall,
                color = gradeColor.copy(alpha = 0.8f),
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun CategoryScoreCard(
    phase: FullBenchmarkPhase,
    scorePercent: Float,
    categoryColor: Color
) {
    val barProgress by animateFloatAsState(
        targetValue = scorePercent / 100f,
        animationSpec = tween(800, easing = EaseInOutCubic),
        label = "card_bar_${phase.category}"
    )

    val actualContributionPts = ((scorePercent / 100f) * phase.weight * 1000).roundToInt()
    val maxContributionPts    = (phase.weight * 1000).roundToInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Left color vertical strip
                    drawRect(
                        color = categoryColor,
                        size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)
                    )
                }
                .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (phase.category) {
                            BenchmarkCategory.CPU         -> Icons.Rounded.Speed
                            BenchmarkCategory.RAM         -> Icons.Rounded.Memory
                            BenchmarkCategory.STORAGE     -> Icons.Rounded.Storage
                            BenchmarkCategory.GPU         -> Icons.Rounded.Speed
                            BenchmarkCategory.PRODUCTIVITY -> Icons.Rounded.Speed
                            else                          -> Icons.Rounded.Speed
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = phase.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "${scorePercent.roundToInt()}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                        color = categoryColor
                    )
                }

                // Progress Bar
                LinearProgressIndicator(
                    progress = { barProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = categoryColor,
                    trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    strokeCap = StrokeCap.Round
                )

                // Contribution details row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Weight: ${(phase.weight * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "Contribution: +$actualContributionPts / $maxContributionPts pts",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceReportCard(preset: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "TEST ENVIRONMENT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportRow(label = "Device Model", value = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                ReportRow(label = "Android SDK", value = "API ${android.os.Build.VERSION.SDK_INT}")
                ReportRow(
                    label = "Test Preset", 
                    value = when (preset.lowercase()) {
                        "slow" -> "Low Workload (Fast)"
                        "mid" -> "Medium Workload (Standard)"
                        "flagship" -> "Flagship Workload (Extensive)"
                        else -> preset
                    }
                )
                ReportRow(
                    label = "Completion Time", 
                    value = java.text.SimpleDateFormat("MMM dd, yyyy  •  HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                )
            }
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ScoringNoteCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "SCORING METHODOLOGY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Text(
                text = "The overall index is calculated dynamically by normalizing each category's score (0-100%) and multiplying it by its respective weight, aggregating to a final 0-1000 score scale.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                lineHeight = 16.sp
            )

            // Visual stacked weights bar graph
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxHeight().weight(20f).background(Color(0xFF6C63FF))) // CPU
                    Box(modifier = Modifier.fillMaxHeight().weight(15f).background(Color(0xFFE91E63))) // AI/ML
                    Box(modifier = Modifier.fillMaxHeight().weight(10f).background(Color(0xFF00BCD4))) // RAM
                    Box(modifier = Modifier.fillMaxHeight().weight(10f).background(Color(0xFF8BC34A))) // Storage
                    Box(modifier = Modifier.fillMaxHeight().weight(20f).background(Color(0xFFFF5722))) // GPU
                    Box(modifier = Modifier.fillMaxHeight().weight(25f).background(Color(0xFFFF9800))) // Productivity
                }
            }

            // Weights Legend Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LegendItem(color = Color(0xFF6C63FF), text = "CPU Performance (20%)")
                    LegendItem(color = Color(0xFFE91E63), text = "AI / ML Engine (15%)")
                    LegendItem(color = Color(0xFF00BCD4), text = "RAM Bandwidth (10%)")
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LegendItem(color = Color(0xFF8BC34A), text = "Storage I/O (10%)")
                    LegendItem(color = Color(0xFFFF5722), text = "GPU Compute (20%)")
                    LegendItem(color = Color(0xFFFF9800), text = "Productivity (25%)")
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

