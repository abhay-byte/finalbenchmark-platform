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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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

    // ── Final results ────────────────────────────────────────────────────────
    AnimatedVisibility(
        visible = state.isComplete,
        enter = fadeIn(tween(600)) + scaleIn(tween(600), initialScale = 0.92f),
        exit  = fadeOut(tween(300))
    ) {
        FullBenchmarkResultScreen(
            state = state,
            onDone = { onBenchmarkComplete(viewModel.buildFinalSummaryJson()) }
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
                state    = state,
                modifier = Modifier.align(Alignment.TopCenter)
            )
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
            historyRepository   = historyRepo
        )
        BenchmarkCategory.RAM -> RamBenchmarkScreen(
            preset              = preset,
            historyRepository   = historyRepo,
            onBenchmarkComplete = onComplete,
            onNavBack           = onNavBack
        )
        BenchmarkCategory.STORAGE -> StorageBenchmarkScreen(
            preset              = preset,
            historyRepository   = historyRepo,
            onBenchmarkComplete = onComplete,
            onNavBack           = onNavBack
        )
        BenchmarkCategory.GPU -> GpuBenchmarkScreen(
            preset              = preset,
            historyRepository   = historyRepo,
            onBenchmarkComplete = onComplete,
            onNavBack           = onNavBack
        )
        BenchmarkCategory.PRODUCTIVITY -> ProductivityBenchmarkScreen(
            preset              = preset,
            historyRepository   = historyRepo,
            onBenchmarkComplete = onComplete,
            onNavBack           = onNavBack
        )
        else -> { /* AI / EXTERNAL_GPU / FULL — not handled here */ }
    }
}

// ── Progress overlay ──────────────────────────────────────────────────────────

@Composable
private fun FullBenchmarkProgressOverlay(
    state: FullBenchmarkState,
    modifier: Modifier = Modifier
) {
    val overallProgressAnim by animateFloatAsState(
        targetValue = state.overallProgress,
        animationSpec = tween(600, easing = EaseInOutCubic),
        label = "overall_progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xDD050810), Color(0x88050810), Color.Transparent)
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // "Full Benchmark · Phase X of Y" label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Full Benchmark",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = "Phase ${state.scores.size + 1} of ${state.totalPhases}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }

        // Thin overall progress bar
        LinearProgressIndicator(
            progress = { overallProgressAnim },
            modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
            color = Color(0xFF6C63FF),
            trackColor = Color.White.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )

        // Phase pills row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            state.phases.forEach { phase ->
                PhasePill(
                    phase  = phase,
                    status = state.statusOf(phase),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PhasePill(
    phase: FullBenchmarkPhase,
    status: PhaseStatus,
    modifier: Modifier = Modifier
) {
    val bgColor = when (status) {
        PhaseStatus.DONE    -> Color(0xFF4CAF50).copy(alpha = 0.85f)
        PhaseStatus.RUNNING -> Color(0xFF6C63FF).copy(alpha = 0.85f)
        PhaseStatus.PENDING -> Color.White.copy(alpha = 0.12f)
    }
    val textColor = when (status) {
        PhaseStatus.PENDING -> Color.White.copy(alpha = 0.4f)
        else                -> Color.White
    }
    val abbrev = when (phase.category) {
        BenchmarkCategory.CPU         -> "CPU"
            BenchmarkCategory.AI          -> "AI"
        BenchmarkCategory.GPU         -> "GPU"
        BenchmarkCategory.PRODUCTIVITY -> "PRO"
        else                          -> phase.category.name.take(3)
    }

    Box(
        modifier = modifier
            .height(22.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (status == PhaseStatus.DONE) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "${phase.displayName} done",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        } else {
            Text(
                text = abbrev,
                color = textColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ── Final results screen ──────────────────────────────────────────────────────

@Composable
private fun FullBenchmarkResultScreen(
    state: FullBenchmarkState,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050810), Color(0xFF0D1220), Color(0xFF080D1A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Text(
                text = "FULL BENCHMARK",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            // ── Big score ring ────────────────────────────────────────────────
            ScoreRing(
                score     = state.overallScore,
                grade     = state.grade,
                gradeColor = gradeColor
            )

            // ── Subtitle ──────────────────────────────────────────────────────
            Text(
                text = "Overall Device Score  •  0–1000 scale",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // ── Category breakdown ────────────────────────────────────────────
            Text(
                text = "Category Breakdown",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )

            state.phases.forEach { phase ->
                val rawScore = state.scores[phase.category] ?: 0.0
                val pct = ((rawScore / phase.maxScore) * 100.0).coerceIn(0.0, 100.0)
                CategoryScoreRow(phase = phase, scorePercent = pct.toFloat())
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // ── Scoring note ──────────────────────────────────────────────────
            ScoringNoteCard()

            Spacer(modifier = Modifier.height(8.dp))

            // ── Done button ───────────────────────────────────────────────────
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6C63FF)
                )
            ) {
                Text(
                    text = "Save & Continue",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ScoreRing(
    score: Int,
    grade: String,
    gradeColor: Color
) {
    val progressAnim by animateFloatAsState(
        targetValue = score / 1000f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "score_progress"
    )
    val primaryColor  = gradeColor
    val trackColor    = Color.White.copy(alpha = 0.08f)

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Arc drawn with drawBehind
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val strokeWidth = 16.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
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
                        brush       = Brush.sweepGradient(
                            listOf(primaryColor.copy(alpha = 0.7f), primaryColor),
                            center  = center
                        ),
                        startAngle  = 135f,
                        sweepAngle  = 270f * progressAnim,
                        useCenter   = false,
                        style       = Stroke(strokeWidth, cap = StrokeCap.Round),
                        topLeft     = Offset(center.x - radius, center.y - radius),
                        size        = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                    )
                }
        )

        // Centre content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "$score",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 50.sp
            )
            Text(
                text  = "pts",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Grade badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(gradeColor.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Grade $grade",
                    color = gradeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun CategoryScoreRow(
    phase: FullBenchmarkPhase,
    scorePercent: Float   // 0..100
) {
    val barProgress by animateFloatAsState(
        targetValue = scorePercent / 100f,
        animationSpec = tween(800, easing = EaseInOutCubic),
        label = "bar_${phase.category}"
    )

    val barColor = when (phase.category) {
        BenchmarkCategory.CPU         -> Color(0xFF6C63FF)
        BenchmarkCategory.AI          -> Color(0xFFE91E63)
        BenchmarkCategory.RAM         -> Color(0xFF00BCD4)
        BenchmarkCategory.STORAGE     -> Color(0xFF8BC34A)
        BenchmarkCategory.GPU         -> Color(0xFFFF5722)
        BenchmarkCategory.PRODUCTIVITY -> Color(0xFFFF9800)
        else                          -> Color(0xFF9E9E9E)
    }

    val icon: ImageVector = when (phase.category) {
        BenchmarkCategory.CPU         -> Icons.Rounded.Speed
        BenchmarkCategory.RAM         -> Icons.Rounded.Memory
        BenchmarkCategory.STORAGE     -> Icons.Rounded.Storage
        BenchmarkCategory.GPU         -> Icons.Rounded.Speed
        BenchmarkCategory.PRODUCTIVITY -> Icons.Rounded.Speed
        else                          -> Icons.Rounded.Speed
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = barColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = phase.displayName,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${scorePercent.roundToInt()}%",
                    color = barColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = "×${(phase.weight * 100).roundToInt()}%",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 11.sp
                )
            }
        }

        LinearProgressIndicator(
            progress = { barProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = barColor,
            trackColor = Color.White.copy(alpha = 0.08f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun ScoringNoteCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text  = "Scoring Methodology",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Each category normalised 0–100%, then weighted per docs: " +
                       "CPU 20% · AI/ML 15% · GPU 20% · RAM 10% · Storage 10% · Productivity 25%. " +
                       "Final score 0–1000.",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}
