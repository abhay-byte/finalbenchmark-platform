package com.ivarna.finalbenchmark2.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.ivarna.finalbenchmark2.ui.viewmodels.RamBenchmarkUiState
import com.ivarna.finalbenchmark2.ui.viewmodels.RamBenchmarkViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.RamTest

private val GLASS_DARK = Color(0xCC0A0E1A)
private val GLASS_BORDER = Color(0x33FFFFFF)

// Accent colours keyed to each RAM test
private val TEST_TINT = mapOf(
    RamTest.SEQ_READ     to Color(0xFF4FC3F7),
    RamTest.SEQ_WRITE    to Color(0xFF81C784),
    RamTest.RAND_ACCESS  to Color(0xFFFF8A65),
    RamTest.MEM_COPY     to Color(0xFFCE93D8),
    RamTest.MULTI_THREAD to Color(0xFFFFD54F)
)

@Composable
fun RamBenchmarkScreen(
    preset: String,
    historyRepository: HistoryRepository? = null,
    onBenchmarkComplete: (String) -> Unit,
    onNavBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val vmFactory = remember(historyRepository) {
        RamBenchmarkViewModel.factory(historyRepository, application)
    }
    val vm: RamBenchmarkViewModel = viewModel(factory = vmFactory)
    val state by vm.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = state.isRunning || state.isWarmingUp) { /* swallow back */ }

    LaunchedEffect(Unit) { vm.start(preset) }
    LaunchedEffect(vm.completionEvent) {
        vm.completionEvent.collect { json -> onBenchmarkComplete(json) }
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
        // Animated background glow for current test
        val tint = TEST_TINT[state.currentTest] ?: Color(0xFF4FC3F7)
        AnimatedBackground(tint = tint)

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ─ Top HUD ─────────────────────────────────────────────────
            TopHudOverlay(state = state, onClose = {
                vm.stop(); onNavBack()
            })

            Spacer(Modifier.weight(1f))

            // ─ Live Value Display ───────────────────────────────────────
            LiveValueDisplay(state = state, tint = tint)

            Spacer(Modifier.weight(1f))

            // ─ Bottom HUD ──────────────────────────────────────────────
            BottomRamHud(state = state)
        }
    }
}

// ── Background glow blob ──────────────────────────────────────────────────────

@Composable
private fun AnimatedBackground(tint: Color) {
    val anim by animateFloatAsState(
        targetValue = tint.red,
        animationSpec = tween(600),
        label = "bg_r"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = 0.08f), Color.Transparent),
                    radius = 700f
                )
            )
    )
}

// ── Top HUD ───────────────────────────────────────────────────────────────────

@Composable
private fun TopHudOverlay(state: RamBenchmarkUiState, onClose: () -> Unit) {
    val progress by animateFloatAsState(targetValue = state.overallProgress, label = "prog")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GLASS_DARK)
            .border(0.5.dp, GLASS_BORDER, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RAM BENCHMARK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = state.currentTestName.ifEmpty { "Preparing…" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(2.dp))
                    AnimatedVisibility(
                        visible = state.isWarmingUp,
                        enter = fadeIn(), exit = fadeOut()
                    ) {
                        Text(
                            text = "Warming up…",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Yellow.copy(alpha = 0.8f)
                        )
                    }
                    AnimatedVisibility(
                        visible = state.isRunning && !state.isWarmingUp,
                        enter = fadeIn(), exit = fadeOut()
                    ) {
                        Text(
                            text = "Measuring…",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4FC3F7).copy(alpha = 0.9f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${state.currentTestIndex + 1} / ${state.totalTests}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp).background(
                            Color.White.copy(alpha = 0.08f), CircleShape
                        )
                    ) {
                        Icon(Icons.Default.Close, "Stop", tint = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            // Overall progress
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            // Per-test progress
            LinearProgressIndicator(
                progress = { state.currentTestProgress },
                modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(2.dp)),
                color = TEST_TINT[state.currentTest]?.copy(alpha = 0.8f) ?: Color.White.copy(alpha = 0.6f),
                trackColor = Color.White.copy(alpha = 0.05f)
            )
        }
    }
}

// ── Live value display ────────────────────────────────────────────────────────

@Composable
private fun LiveValueDisplay(state: RamBenchmarkUiState, tint: Color) {
    val isLatency = state.currentTest == RamTest.RAND_ACCESS
    val valueText = if (state.currentValue > 0) {
        if (isLatency) "${"%.1f".format(state.currentValue)} ns/op"
        else "${"%.0f".format(state.currentValue)} MB/s"
    } else "—"

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(
                    if (state.isRunning) tint.copy(alpha = 0.9f)
                    else if (state.isWarmingUp) Color.Yellow.copy(alpha = 0.7f)
                    else Color.White.copy(alpha = 0.3f)
                )
        )

        Text(
            text = valueText,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = tint,
            textAlign = TextAlign.Center
        )

        Text(
            text = state.currentTestName.ifEmpty { "" },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )

        // Completed tests mini-bar
        if (state.completedTests.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            CompletedTestsRow(state)
        }
    }
}

@Composable
private fun CompletedTestsRow(state: RamBenchmarkUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GLASS_DARK)
            .border(0.5.dp, GLASS_BORDER, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        state.completedTests.forEach { r ->
            val c = TEST_TINT[r.test] ?: Color.White
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (r.unit == "ns/op") "${"%.0f".format(r.value)}ns"
                    else "${"%.0f".format(r.value)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = c
                )
                Text(
                    text = r.test.name.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

// ── Bottom HUD ────────────────────────────────────────────────────────────────

@Composable
private fun BottomRamHud(state: RamBenchmarkUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GLASS_DARK)
            .border(0.5.dp, GLASS_BORDER, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Memory usage
            HudStat(
                icon = Icons.Default.Memory,
                label = "Memory",
                value = "${state.memUsageMB} MB",
                tint = Color(0xFF81C784)
            )

            // CPU temperature
            HudStat(
                icon = Icons.Default.Thermostat,
                label = "CPU Temp",
                value = "${"%.1f".format(state.cpuTempC)}°C",
                tint = Color(0xFFFF8A65)
            )

            // Current score progress (live bench index / total)
            HudStat(
                icon = Icons.Default.Speed,
                label = "Progress",
                value = "${state.currentTestIndex + 1} / ${state.totalTests}",
                tint = Color(0xFF4FC3F7)
            )
        }
    }
}

@Composable
private fun HudStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = tint.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}
