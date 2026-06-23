package com.zenithblue.fb2Pro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zenithblue.fb2Pro.utils.RootUtils
import com.zenithblue.fb2Pro.utils.RootAccessManager
import kotlin.math.max

@Composable
fun GpuUtilizationGraph(
    dataPoints: List<GpuDataPoint>,
    modifier: Modifier = Modifier,
    requiresRoot: Boolean = false
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = "GPU Utilization (30s)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Use RootAccessManager to check root access status efficiently
            var hasRootAccess by remember { mutableStateOf<Boolean?>(null) }
            var isCheckingRoot by remember { mutableStateOf(false) }
            
            LaunchedEffect(requiresRoot) {
                if (requiresRoot) {
                    // First, check if we have cached result
                    val cachedResult = RootAccessManager.getCachedRootAccess()
                    if (cachedResult != null) {
                        hasRootAccess = cachedResult
                    } else {
                        // No cached result, start checking
                        isCheckingRoot = true
                        hasRootAccess = RootAccessManager.hasRootAccess()
                        isCheckingRoot = false
                    }
                } else {
                    hasRootAccess = true
                }
            }
            
            // Show loading state while checking root access
            if (requiresRoot && hasRootAccess == null && isCheckingRoot) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Checking root access...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (requiresRoot && hasRootAccess == false) {
                // Show root requirement message
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Root required: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Requires root access to work",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Root access required",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Current utilization indicator
                val currentGpu = dataPoints.lastOrNull()?.utilization ?: 0f
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Current: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${currentGpu.toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Graph Canvas
                if (dataPoints.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val width = size.width
                            val height = size.height
                            val padding = 40f
                            val graphWidth = width - padding * 2
                            val graphHeight = height - padding * 2
                            
                            // Draw Y-axis (0-100%)
                            drawLine(
                                color = surfaceVariantColor,
                                start = Offset(padding, padding),
                                end = Offset(padding, height - padding),
                                strokeWidth = 2f
                            )
                            
                            // Draw X-axis (time)
                            drawLine(
                                color = surfaceVariantColor,
                                start = Offset(padding, height - padding),
                                end = Offset(width - padding, height - padding),
                                strokeWidth = 2f
                            )
                            
                            // Draw Y-axis grid lines and labels (0%, 25%, 50%, 75%, 100%)
                            for (i in 0..4) {
                                val y = height - padding - (graphHeight * i / 4)
                                val percentage = i * 25
                                
                                // Grid line
                                drawLine(
                                    color = surfaceVariantColor.copy(alpha = 0.3f),
                                    start = Offset(padding, y),
                                    end = Offset(width - padding, y),
                                    strokeWidth = 1f
                                )
                                
                                // Label
                                drawIntoCanvas { canvas ->
                                    val paint = android.graphics.Paint().apply {
                                        color = onSurfaceVariantColor.toArgb()
                                        textSize = 24f
                                        textAlign = android.graphics.Paint.Align.RIGHT
                                    }
                                    canvas.nativeCanvas.drawText(
                                        "$percentage%",
                                        padding - 8f,
                                        y + 8f,
                                        paint
                                    )
                                }
                            }
                            
                            // Draw X-axis labels based on actual time range
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint().apply {
                                    color = onSurfaceVariantColor.toArgb()
                                    textSize = 24f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                                
                                // X-axis labels always show "30s ago", "15s ago", "0s ago" (from left to right)
                                // These represent fixed positions in the 30-second window
                                
                                canvas.nativeCanvas.drawText(
                                    "30s",
                                    padding,
                                    height - padding + 30f,
                                    paint
                                )
                                canvas.nativeCanvas.drawText(
                                    "15s",
                                    padding + graphWidth / 2,
                                    height - padding + 30f,
                                    paint
                                )
                                canvas.nativeCanvas.drawText(
                                    "0s",
                                    width - padding,
                                    height - padding + 30f,
                                    paint
                                )
                            }
                            
                            // Calculate time range based on actual data points to prevent compression
                            val maxTimestamp = if (dataPoints.isNotEmpty()) {
                                dataPoints.maxOf { it.timestamp }
                            } else {
                                System.currentTimeMillis()
                            }
                            val startTime = maxTimestamp - 30_000L
                            
                            // Draw the line graph
                            if (dataPoints.size >= 2) {
                                val path = Path()
                                
                                dataPoints.forEachIndexed { index, point ->
                                    val timeProgress = (point.timestamp - startTime).toFloat() / 30_000f
                                    val x = padding + (timeProgress * graphWidth)
                                    val y = height - padding - ((point.utilization / 100f) * graphHeight)
                                    
                                    if (index == 0) {
                                        path.moveTo(x, y)
                                    } else {
                                        path.lineTo(x, y)
                                    }
                                }
                                
                                // Draw the line
                                drawPath(
                                    path = path,
                                    color = primaryColor,
                                    style = Stroke(width = 3f)
                                )
                                
                                // Optional: Draw data points as circles
                                dataPoints.forEach { point ->
                                    val timeProgress = (point.timestamp - startTime).toFloat() / 30_000f
                                    val x = padding + (timeProgress * graphWidth)
                                    val y = height - padding - ((point.utilization / 100f) * graphHeight)
                                    
                                    drawCircle(
                                        color = primaryColor,
                                        radius = 4f,
                                        center = Offset(x, y)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Collecting data...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}