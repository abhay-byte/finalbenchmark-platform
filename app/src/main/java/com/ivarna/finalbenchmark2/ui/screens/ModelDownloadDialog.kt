package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ivarna.finalbenchmark2.aiBenchmark.ModelDownloader
import com.ivarna.finalbenchmark2.aiBenchmark.ModelRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen dialog that checks and downloads AI models before the Full Benchmark starts.
 * Single page, minimal design, follows MaterialTheme. Only one action: Download.
 * After download completes, automatically calls [onProceed].
 */
@Composable
fun ModelDownloadDialog(
    onDismiss: () -> Unit,
    onProceed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Model state
    val models = remember { ModelRepository.models }

    // Local helper: checks if a model file exists and is non-empty
    fun isModelReady(filename: String): Boolean {
        val f = java.io.File(context.filesDir, "models/$filename")
        return f.exists() && f.length() > 0
    }

    var downloadedCount by remember {
        mutableStateOf(models.count { isModelReady(it.filename) })
    }
    val totalCount = models.size
    val allReady = downloadedCount == totalCount

    // Download state
    var isDownloading by remember { mutableStateOf(false) }
    var overallProgress by remember { mutableStateOf(0f) }
    var currentModelName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var downloadComplete by remember { mutableStateOf(false) }

    // Pulse animation for icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "icon_scale"
    )

    // If all already downloaded, proceed immediately
    LaunchedEffect(Unit) {
        if (allReady) {
            delay(300)
            onProceed()
        }
    }

    Dialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(28.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ── Icon ─────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (downloadComplete) Icons.Rounded.Check else Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // ── Title & subtitle ─────────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (downloadComplete) "Models Ready" else "AI Models Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (downloadComplete)
                            "All models downloaded. Starting benchmark…"
                        else
                            "$downloadedCount of $totalCount models ready · ~450 MB total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // ── Model list ───────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    models.forEachIndexed { index, model ->
                        val isReady = isModelReady(model.filename)
                        val isActive = isDownloading && currentModelName == model.title

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Status dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isReady -> MaterialTheme.colorScheme.primary
                                            isActive -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        }
                                    )
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = model.sizeMb,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isReady) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Ready",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else if (isActive) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        // Divider between items (not after last)
                        if (index < models.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }

                // ── Progress bar (visible while downloading) ─────────────────
                if (isDownloading && !downloadComplete) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = currentModelName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(overallProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // ── Error message ─────────────────────────────────────────────
                errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                // ── Download button ───────────────────────────────────────────
                if (!downloadComplete) {
                    Button(
                        onClick = {
                            if (!isDownloading) {
                                isDownloading = true
                                errorMessage = null
                                scope.launch {
                                    try {
                                        val pending = models.filter { !isModelReady(it.filename) }
                                        val total = pending.size.coerceAtLeast(1)

                                        pending.forEachIndexed { index, model ->
                                            currentModelName = model.title
                                            val startProg = index.toFloat() / total
                                            val endProg = (index + 1).toFloat() / total

                                            ModelDownloader.downloadModel(
                                                context,
                                                model.url,
                                                model.filename
                                            ) { fileProgress ->
                                                overallProgress = startProg + fileProgress * (endProg - startProg)
                                            }
                                            downloadedCount++
                                        }

                                        overallProgress = 1f
                                        downloadComplete = true
                                        delay(800)
                                        onProceed()
                                    } catch (e: Exception) {
                                        errorMessage = "Download failed: ${e.message}"
                                        isDownloading = false
                                    }
                                }
                            }
                        },
                        enabled = !isDownloading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Downloading…",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Download Models (~450 MB)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
