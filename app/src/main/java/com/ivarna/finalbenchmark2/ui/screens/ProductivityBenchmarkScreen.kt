package com.ivarna.finalbenchmark2.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WorkOutline
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

private val PROD_GLASS_DARK   = Color(0xCC06080D)
private val PROD_GLASS_BORDER = Color(0x33FFFFFF)

// Accent colour per test — purple/cyan/teal palette for "productivity" feel
private val PROD_TEST_TINT = mapOf(
    ProductivityTest.CANVAS_OPS   to Color(0xFF7C4DFF),  // deep purple
    ProductivityTest.IMAGE_FILTER to Color(0xFF00BCD4),  // cyan
    ProductivityTest.IMAGE_RESIZE to Color(0xFF00E5FF),  // light cyan
    ProductivityTest.TEXT_OPS     to Color(0xFF69F0AE),  // green accent
    ProductivityTest.JSON_OPS     to Color(0xFFFFB300),  // amber
    ProductivityTest.COMPRESSION  to Color(0xFFFF4081),  // pink accent
    ProductivityTest.VIDEO_ENCODE to Color(0xFFE040FB),  // purple A200 — video
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

    BackHandler(enabled = state.isRunning || state.isWarmingUp) { /* block back during run */ }

    LaunchedEffect(Unit) { vm.start(preset) }
    LaunchedEffect(vm.completionEvent) {
        vm.completionEvent.collect { json -> onBenchmarkComplete(json) }
    }

    val tint = PROD_TEST_TINT[state.currentTest] ?: Color(0xFF7C4DFF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF020407), Color(0xFF07090F), Color(0xFF050508))
                )
            )
    ) {
        // Animated radial glow centred in the screen
        ProdAnimatedBackground(tint = tint)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            ProdTopHud(state = state, tint = tint, onClose = { vm.stop(); onNavBack() })

            Spacer(Modifier.weight(1f))

            ProdLiveValueDisplay(state = state, tint = tint, previewBitmap = previewBitmap)

            Spacer(Modifier.weight(1f))

            ProdBottomHud(state = state, tint = tint)
        }
    }
}

// ── Background glow ───────────────────────────────────────────────────────────

@Composable
private fun ProdAnimatedBackground(tint: Color) {
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
}

// ── Top HUD ───────────────────────────────────────────────────────────────────

@Composable
private fun ProdTopHud(
    state: ProductivityBenchmarkUiState,
    tint: Color,
    onClose: () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = state.overallProgress,
        animationSpec = tween(300),
        label = "overall_prog"
    )
    val testProgress by animateFloatAsState(
        targetValue = state.currentTestProgress,
        animationSpec = tween(200),
        label = "test_prog"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(PROD_GLASS_DARK)
            .border(0.5.dp, PROD_GLASS_BORDER, RoundedCornerShape(20.dp))
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
                        text = "PRODUCTIVITY BENCHMARK",
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
                            text = "Warming up ${state.currentTestName}…",
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
                progress = { testProgress },
                modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(2.dp)),
                color = tint.copy(alpha = 0.6f),
                trackColor = Color.White.copy(alpha = 0.05f)
            )
        }
    }
}

// ── Live value display ────────────────────────────────────────────────────────

@Composable
private fun ProdLiveValueDisplay(
    state: ProductivityBenchmarkUiState,
    tint: Color,
    previewBitmap: android.graphics.Bitmap? = null
) {
    val valueText = when {
        state.currentValue <= 0.0 -> "—"
        state.currentUnit == "Mchars/s" -> "${"%.2f".format(state.currentValue)} Mchars/s"
        state.currentUnit == "docs/s"   -> "${"%.0f".format(state.currentValue)} docs/s"
        state.currentUnit == "images/s" -> "${"%.0f".format(state.currentValue)} images/s"
        state.currentUnit == "ops/s"    -> "${"%.0f".format(state.currentValue)} ops/s"
        state.currentUnit == "MB/s"     -> "${"%.1f".format(state.currentValue)} MB/s"
        state.currentUnit == "fps"      -> "${"%.1f".format(state.currentValue)} fps"
        else -> "${"%.1f".format(state.currentValue)} ${state.currentUnit}"
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

        // Main metric
        Text(
            text = valueText,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = tint,
            textAlign = TextAlign.Center
        )

        // Test name
        Text(
            text = state.currentTestName.ifEmpty { "" },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )

        // ── Live preview: image / video frame ─────────────────────────────
        AnimatedVisibility(
            visible = previewBitmap != null && !(previewBitmap.isRecycled) &&
                      (state.isRunning || state.isWarmingUp),
            enter = fadeIn(tween(300)), exit = fadeOut(tween(300))
        ) {
            previewBitmap?.let { bmp ->
                if (!bmp.isRecycled) {
                    val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    ) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Current frame being processed",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Bottom gradient so the label is readable over any frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                    )
                                )
                        )
                        Text(
                            text = when (state.currentTest) {
                                ProductivityTest.IMAGE_FILTER -> "📷 Filter Pass"
                                ProductivityTest.IMAGE_RESIZE -> "📐 Resize Output"
                                ProductivityTest.VIDEO_ENCODE -> "🎬 Video Frame"
                                else -> "🖼️ Preview"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                        // Resolution badge top-right
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when (state.currentTest) {
                                    ProductivityTest.IMAGE_FILTER -> "4K"
                                    ProductivityTest.IMAGE_RESIZE -> "4K→QHD"
                                    ProductivityTest.VIDEO_ENCODE -> "1080p"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = tint,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Live operation detail ─────────────────────────────────────────
        // Shows what the benchmark is currently doing, e.g. "Filtering image #247"
        AnimatedVisibility(
            visible = state.currentOperationDetail.isNotEmpty() && (state.isRunning || state.isWarmingUp),
            enter = fadeIn(tween(200)), exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.07f))
                    .border(0.5.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.currentOperationDetail,
                    style = MaterialTheme.typography.labelMedium,
                    color = tint.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Completed tests summary chips
        if (state.completedTests.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            ProdCompletedTestsRow(state)
        }
    }
}

@Composable
private fun ProdCompletedTestsRow(state: ProductivityBenchmarkUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PROD_GLASS_DARK)
            .border(0.5.dp, PROD_GLASS_BORDER, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        state.completedTests.forEach { r ->
            val c = PROD_TEST_TINT[r.test] ?: Color.White
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val shortVal = when (r.unit) {
                    "Mchars/s" -> "${"%.1f".format(r.value)}M"
                    "docs/s"   -> "${"%.0f".format(r.value / 1_000)}k"
                    "MB/s"     -> "${"%.0f".format(r.value)}"
                    "fps"      -> "${"%.0f".format(r.value)}f"
                    else       -> "${"%.0f".format(r.value)}"
                }
                Text(
                    text = shortVal,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = c
                )
                Text(
                    text = when (r.test) {
                        ProductivityTest.CANVAS_OPS   -> "2D"
                        ProductivityTest.IMAGE_FILTER -> "FLT"
                        ProductivityTest.IMAGE_RESIZE -> "RSZ"
                        ProductivityTest.TEXT_OPS     -> "TXT"
                        ProductivityTest.JSON_OPS     -> "JSN"
                        ProductivityTest.COMPRESSION  -> "CMP"
                        ProductivityTest.VIDEO_ENCODE -> "VID"
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
private fun ProdBottomHud(
    state: ProductivityBenchmarkUiState,
    tint: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(PROD_GLASS_DARK)
            .border(0.5.dp, PROD_GLASS_BORDER, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Running score
            ProdHudStat(
                icon = Icons.Default.WorkOutline,
                label = "Score",
                value = if (state.completedTests.isEmpty()) "—"
                        else state.completedTests.map { it.score }.average()
                            .let { "${"%.0f".format(it)}" },
                tint = tint
            )
            // CPU temperature
            ProdHudStat(
                icon = Icons.Default.Thermostat,
                label = "CPU Temp",
                value = "${"%.1f".format(state.cpuTempC)}°C",
                tint = Color(0xFFFF7043)
            )
            // Progress
            ProdHudStat(
                icon = Icons.Default.Speed,
                label = "Progress",
                value = "${state.currentTestIndex + 1} / ${state.totalTests}",
                tint = Color(0xFF00BCD4)
            )
        }
    }
}

@Composable
private fun ProdHudStat(
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
