package com.ivarna.finalbenchmark2.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.ivarna.finalbenchmark2.ui.viewmodels.StorageBenchmarkUiState
import com.ivarna.finalbenchmark2.ui.viewmodels.StorageBenchmarkViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.StorageTest

private val STORAGE_GLASS_DARK   = Color(0xCC060B0E)
private val STORAGE_GLASS_BORDER = Color(0x33FFFFFF)

// Accent colour per test — amber/orange palette for "storage" feel
private val STORAGE_TEST_TINT = mapOf(
    StorageTest.SEQ_READ    to Color(0xFFFFCA28),  // amber
    StorageTest.SEQ_WRITE   to Color(0xFFFF7043),  // deep orange
    StorageTest.RAND_4K     to Color(0xFFFF5252),  // red accent
    StorageTest.SMALL_FILES to Color(0xFF26C6DA),  // cyan
    StorageTest.SQLITE      to Color(0xFF7C4DFF),  // deep purple
    StorageTest.MIXED       to Color(0xFFB2FF59)   // light green
)

@Composable
fun StorageBenchmarkScreen(
    preset: String,
    historyRepository: HistoryRepository? = null,
    onBenchmarkComplete: (String) -> Unit,
    onNavBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val vmFactory = remember(historyRepository) {
        StorageBenchmarkViewModel.factory(historyRepository, application)
    }
    val vm: StorageBenchmarkViewModel = viewModel(factory = vmFactory)
    val state by vm.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = state.isRunning || state.isWarmingUp) { /* swallow back during benchmark */ }

    LaunchedEffect(Unit) { vm.start(preset) }
    LaunchedEffect(vm.completionEvent) {
        vm.completionEvent.collect { json -> onBenchmarkComplete(json) }
    }

    val tint = STORAGE_TEST_TINT[state.currentTest] ?: Color(0xFFFFCA28)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF020608), Color(0xFF081014), Color(0xFF050A0E))
                )
            )
    ) {
        // Animated background glow
        StorageAnimatedBackground(tint = tint)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            StorageTopHud(state = state, tint = tint, onClose = {
                vm.stop(); onNavBack()
            })

            Spacer(Modifier.weight(1f))

            StorageLiveValueDisplay(state = state, tint = tint)

            Spacer(Modifier.weight(1f))

            StorageBottomHud(state = state)
        }
    }
}

// ── Background glow ───────────────────────────────────────────────────────────

@Composable
private fun StorageAnimatedBackground(tint: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = 0.07f), Color.Transparent),
                    radius = 750f
                )
            )
    )
}

// ── Top HUD ───────────────────────────────────────────────────────────────────

@Composable
private fun StorageTopHud(
    state: StorageBenchmarkUiState,
    tint: Color,
    onClose: () -> Unit
) {
    val progress by animateFloatAsState(targetValue = state.overallProgress, label = "prog")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(STORAGE_GLASS_DARK)
            .border(0.5.dp, STORAGE_GLASS_BORDER, RoundedCornerShape(20.dp))
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
                        text = "STORAGE BENCHMARK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = tint.copy(alpha = 0.7f),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = state.currentTestName.ifEmpty { "Preparing…" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(2.dp))
                    AnimatedVisibility(visible = state.isWarmingUp, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            text = state.statusMessage.ifEmpty { "Warming up…" },
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
                            color = tint.copy(alpha = 0.9f)
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
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, "Stop", tint = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            // Overall progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp)),
                color = tint,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            // Per-test progress bar
            LinearProgressIndicator(
                progress = { state.currentTestProgress },
                modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(2.dp)),
                color = tint.copy(alpha = 0.6f),
                trackColor = Color.White.copy(alpha = 0.05f)
            )
        }
    }
}

// ── Live value display ────────────────────────────────────────────────────────

@Composable
private fun StorageLiveValueDisplay(state: StorageBenchmarkUiState, tint: Color) {
    val valueText = when {
        state.currentValue <= 0.0 -> "—"
        state.currentUnit == "files/s" -> "${"%.0f".format(state.currentValue)} files/s"
        state.currentUnit == "txn/s"   -> "${"%.0f".format(state.currentValue)} txn/s"
        else -> "${"%.0f".format(state.currentValue)} MB/s"
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Status dot
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(
                    when {
                        state.isRunning   -> tint.copy(alpha = 0.9f)
                        state.isWarmingUp -> Color.Yellow.copy(alpha = 0.7f)
                        else              -> Color.White.copy(alpha = 0.3f)
                    }
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

        // Completed tests summary chips
        if (state.completedTests.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            StorageCompletedTestsRow(state)
        }
    }
}

@Composable
private fun StorageCompletedTestsRow(state: StorageBenchmarkUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(STORAGE_GLASS_DARK)
            .border(0.5.dp, STORAGE_GLASS_BORDER, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        state.completedTests.forEach { r ->
            val c = STORAGE_TEST_TINT[r.test] ?: Color.White
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val shortVal = when (r.unit) {
                    "files/s" -> "${"%.0f".format(r.value / 1000)}k"
                    "txn/s"   -> "${"%.0f".format(r.value / 1000)}k"
                    else      -> "${"%.0f".format(r.value)}"
                }
                Text(
                    text = shortVal,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = c
                )
                Text(
                    text = when (r.test) {
                        StorageTest.SEQ_READ    -> "RD"
                        StorageTest.SEQ_WRITE   -> "WR"
                        StorageTest.RAND_4K     -> "4K"
                        StorageTest.SMALL_FILES -> "SF"
                        StorageTest.SQLITE      -> "DB"
                        StorageTest.MIXED       -> "MX"
                    },
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
private fun StorageBottomHud(state: StorageBenchmarkUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(STORAGE_GLASS_DARK)
            .border(0.5.dp, STORAGE_GLASS_BORDER, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Free storage
            StorageHudStat(
                icon = Icons.Default.Save,
                label = "Free",
                value = "${"%.1f".format(state.storageFreeGB)} GB",
                tint = Color(0xFFFFCA28)
            )
            // CPU temperature
            StorageHudStat(
                icon = Icons.Default.Thermostat,
                label = "CPU Temp",
                value = "${"%.1f".format(state.cpuTempC)}°C",
                tint = Color(0xFFFF7043)
            )
            // Progress
            StorageHudStat(
                icon = Icons.Default.Speed,
                label = "Progress",
                value = "${state.currentTestIndex + 1} / ${state.totalTests}",
                tint = Color(0xFF26C6DA)
            )
        }
    }
}

@Composable
private fun StorageHudStat(
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
