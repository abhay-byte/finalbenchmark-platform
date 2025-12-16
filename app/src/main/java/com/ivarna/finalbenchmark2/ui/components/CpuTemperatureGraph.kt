package com.ivarna.finalbenchmark2.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Composable
fun CpuTemperatureGraph(dataPoints: List<TemperatureDataPoint>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val errorColor = MaterialTheme.colorScheme.error
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // Calculate dynamic Y-axis range based on data
    val (minTemp, maxTemp) =
            remember(dataPoints) {
                if (dataPoints.isEmpty()) {
                    Pair(20f, 80f) // Default range if no data
                } else {
                    val min = dataPoints.minOfOrNull { it.temperature } ?: 20f
                    val max = dataPoints.maxOfOrNull { it.temperature } ?: 80f

                    // Add 10% padding
                    val range = max - min
                    val padding = if (range > 0) range * 0.1f else 5f

                    Pair((min - padding).coerceAtLeast(0f), max + padding)
                }
            }

    Card(
            modifier = modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                    ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Calculate duration for title
            val durationSeconds =
                    remember(dataPoints) {
                        if (dataPoints.size >= 2) {
                            val duration =
                                    (dataPoints.last().timestamp - dataPoints.first().timestamp) /
                                            1000
                            duration
                        } else {
                            0L
                        }
                    }

            // Title
            Text(
                    text =
                            if (durationSeconds > 0) "CPU Temperature (${durationSeconds}s)"
                            else "CPU Temperature",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Current and average temperature indicators
            val maxTemp = dataPoints.maxOfOrNull { it.temperature } ?: 0f
            val avgTemp =
                    if (dataPoints.isNotEmpty()) {
                        dataPoints.map { it.temperature }.average().toFloat()
                    } else {
                        0f
                    }

            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            text = "Max: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                            text = String.format("%.1f°C", maxTemp),
                            style = MaterialTheme.typography.titleSmall,
                            color =
                                    when {
                                        maxTemp > 70f -> errorColor
                                        maxTemp > 60f -> tertiaryColor
                                        else -> primaryColor
                                    }
                    )
                }

                if (dataPoints.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                                text = "Avg: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                                text = String.format("%.1f°C", avgTemp),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Graph Canvas
            if (dataPoints.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val padding = 50f
                        val graphWidth = width - padding * 2
                        val graphHeight = height - padding * 2

                        // Draw Y-axis
                        drawLine(
                                color = surfaceVariantColor,
                                start = Offset(padding, padding),
                                end = Offset(padding, height - padding),
                                strokeWidth = 2f
                        )

                        // Draw X-axis
                        drawLine(
                                color = surfaceVariantColor,
                                start = Offset(padding, height - padding),
                                end = Offset(width - padding, height - padding),
                                strokeWidth = 2f
                        )

                        // Draw Y-axis grid lines and labels
                        val tempRange = maxTemp - minTemp
                        val numYTicks = 5

                        for (i in 0..numYTicks) {
                            val tempValue = minTemp + (tempRange * i / numYTicks)
                            val y = height - padding - (graphHeight * i / numYTicks)

                            // Grid line
                            drawLine(
                                    color = surfaceVariantColor.copy(alpha = 0.3f),
                                    start = Offset(padding, y),
                                    end = Offset(width - padding, y),
                                    strokeWidth = 1f
                            )

                            // Label
                            drawIntoCanvas { canvas ->
                                val paint =
                                        android.graphics.Paint().apply {
                                            color = onSurfaceVariantColor.toArgb()
                                            textSize = 20f
                                            textAlign = android.graphics.Paint.Align.RIGHT
                                        }
                                canvas.nativeCanvas.drawText(
                                        String.format("%.0f°C", tempValue),
                                        padding - 8f,
                                        y + 6f,
                                        paint
                                )
                            }
                        }

                        // Draw X-axis labels (time)
                        drawIntoCanvas { canvas ->
                            val paint =
                                    android.graphics.Paint().apply {
                                        color = onSurfaceVariantColor.toArgb()
                                        textSize = 20f
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }

                            // Calculate time range
                            val startTime = dataPoints.firstOrNull()?.timestamp ?: 0L
                            val endTime = dataPoints.lastOrNull()?.timestamp ?: 0L
                            val duration = (endTime - startTime) / 1000 // in seconds

                            canvas.nativeCanvas.drawText(
                                    "0s",
                                    padding,
                                    height - padding + 25f,
                                    paint
                            )
                            canvas.nativeCanvas.drawText(
                                    "${duration / 2}s",
                                    padding + graphWidth / 2,
                                    height - padding + 25f,
                                    paint
                            )
                            canvas.nativeCanvas.drawText(
                                    "${duration}s",
                                    width - padding,
                                    height - padding + 25f,
                                    paint
                            )
                        }

                        // Draw the line graph
                        if (dataPoints.size >= 2) {
                            val path = Path()
                            val startTime = dataPoints.first().timestamp
                            val endTime = dataPoints.last().timestamp
                            val timeRange = (endTime - startTime).toFloat()

                            dataPoints.forEachIndexed { index, point ->
                                val timeProgress =
                                        if (timeRange > 0) {
                                            (point.timestamp - startTime).toFloat() / timeRange
                                        } else {
                                            0f
                                        }
                                val tempProgress =
                                        if (tempRange > 0) {
                                            (point.temperature - minTemp) / tempRange
                                        } else {
                                            0.5f
                                        }

                                val x = padding + (timeProgress * graphWidth)
                                val y = height - padding - (tempProgress * graphHeight)

                                if (index == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }

                            // Draw the line
                            drawPath(path = path, color = primaryColor, style = Stroke(width = 3f))

                            // Draw data points as circles
                            dataPoints.forEach { point ->
                                val timeProgress =
                                        if (timeRange > 0) {
                                            (point.timestamp - startTime).toFloat() / timeRange
                                        } else {
                                            0f
                                        }
                                val tempProgress =
                                        if (tempRange > 0) {
                                            (point.temperature - minTemp) / tempRange
                                        } else {
                                            0.5f
                                        }

                                val x = padding + (timeProgress * graphWidth)
                                val y = height - padding - (tempProgress * graphHeight)

                                drawCircle(color = primaryColor, radius = 4f, center = Offset(x, y))
                            }
                        }
                    }
                }
            } else {
                // Empty state
                Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                            text = "No temperature data",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
