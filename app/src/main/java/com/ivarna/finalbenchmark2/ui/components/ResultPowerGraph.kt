package com.ivarna.finalbenchmark2.ui.components

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

/**
 * A Power Consumption Graph designed for historical/result display. Unlike the live monitoring
 * graph, this uses relative time from the first data point and shows summary statistics (Avg, Min,
 * Max) instead of "Current".
 */
@Composable
fun ResultPowerGraph(
        dataPoints: List<PowerDataPoint>,
        totalDurationMs: Long = 0L,
        modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // Calculate dynamic Y-axis range based on data with padding
    val (minPower, maxPower) =
            remember(dataPoints) {
                if (dataPoints.isEmpty()) {
                    Pair(-10f, 10f)
                } else {
                    val min = dataPoints.minOfOrNull { it.powerWatts } ?: -5f
                    val max = dataPoints.maxOfOrNull { it.powerWatts } ?: 5f

                    val range = max - min
                    val padding = if (range > 0) range * 0.2f else 2f

                    Pair(min - padding, max + padding)
                }
            }

    // Calculate time range from data
    val (startTime, endTime) =
            remember(dataPoints) {
                if (dataPoints.isEmpty()) {
                    Pair(0L, 1000L)
                } else {
                    val start = dataPoints.minOfOrNull { it.timestamp } ?: 0L
                    val end = dataPoints.maxOfOrNull { it.timestamp } ?: start + 1000L
                    Pair(start, end)
                }
            }

    val timeRangeMs =
            if (totalDurationMs > 0) totalDurationMs else (endTime - startTime).coerceAtLeast(1000L)
    val durationSeconds = (timeRangeMs / 1000.0).toInt()

    val fullRange = maxPower - minPower

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
            // Title
            Text(
                    text = "Power Consumption",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Summary statistics
            if (dataPoints.isNotEmpty()) {
                val avgPower = dataPoints.map { it.powerWatts }.average().toFloat()
                val minPowerVal = dataPoints.minOfOrNull { it.powerWatts } ?: 0f
                val maxPowerVal = dataPoints.maxOfOrNull { it.powerWatts } ?: 0f

                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Average
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                                text = "Avg: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                                text = String.format("%.2f W", avgPower),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (avgPower >= 0) errorColor else secondaryColor
                        )
                    }

                    // Min
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                                text = "Min: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                                text = String.format("%.2f W", minPowerVal),
                                style = MaterialTheme.typography.titleSmall,
                                color = tertiaryColor
                        )
                    }

                    // Max
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                                text = "Max: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                                text = String.format("%.2f W", maxPowerVal),
                                style = MaterialTheme.typography.titleSmall,
                                color = errorColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Graph Canvas
            if (dataPoints.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
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

                        val powerRange = maxPower - minPower
                        val numYTicks = 5

                        // Draw Y-axis grid lines and labels
                        for (i in 0..numYTicks) {
                            val powerValue = minPower + (powerRange * i / numYTicks)
                            val y =
                                    height -
                                            padding -
                                            ((powerValue - minPower) / powerRange * graphHeight)

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
                                        String.format("%.1fW", powerValue),
                                        padding - 8f,
                                        y + 5f,
                                        paint
                                )
                            }
                        }

                        // Draw X-axis labels (time from start)
                        drawIntoCanvas { canvas ->
                            val paint =
                                    android.graphics.Paint().apply {
                                        color = onSurfaceVariantColor.toArgb()
                                        textSize = 20f
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }

                            val midSeconds = durationSeconds / 2

                            canvas.nativeCanvas.drawText(
                                    "0s",
                                    padding,
                                    height - padding + 25f,
                                    paint
                            )
                            canvas.nativeCanvas.drawText(
                                    "${midSeconds}s",
                                    padding + graphWidth / 2,
                                    height - padding + 25f,
                                    paint
                            )
                            canvas.nativeCanvas.drawText(
                                    "${durationSeconds}s",
                                    width - padding,
                                    height - padding + 25f,
                                    paint
                            )
                        }

                        // Draw the line graph
                        if (dataPoints.size >= 2) {
                            val path = Path()

                            dataPoints.forEachIndexed { index, point ->
                                val timeProgress =
                                        (point.timestamp - startTime).toFloat() /
                                                timeRangeMs.toFloat()
                                val powerProgress =
                                        if (powerRange > 0) {
                                            (point.powerWatts - minPower) / powerRange
                                        } else {
                                            0.5f
                                        }

                                val x = padding + (timeProgress * graphWidth)
                                val y = height - padding - (powerProgress * graphHeight)

                                if (index == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }

                            // Calculate average power to determine line color
                            val avgPower = dataPoints.map { it.powerWatts }.average().toFloat()
                            val lineColor = if (avgPower >= 0) errorColor else secondaryColor

                            drawPath(path = path, color = lineColor, style = Stroke(width = 3f))

                            // Draw data points as circles
                            dataPoints.forEach { point ->
                                val timeProgress =
                                        (point.timestamp - startTime).toFloat() /
                                                timeRangeMs.toFloat()
                                val powerProgress =
                                        if (powerRange > 0) {
                                            (point.powerWatts - minPower) / powerRange
                                        } else {
                                            0.5f
                                        }

                                val x = padding + (timeProgress * graphWidth)
                                val y = height - padding - (powerProgress * graphHeight)

                                val pointColor =
                                        if (point.powerWatts >= 0) errorColor else secondaryColor

                                drawCircle(color = pointColor, radius = 4f, center = Offset(x, y))
                            }
                        }

                        // Draw zero line if applicable
                        if (minPower < 0 && maxPower > 0) {
                            val zeroY =
                                    height - padding - ((0f - minPower) / powerRange * graphHeight)
                            drawLine(
                                    color = Color.Gray.copy(alpha = 0.5f),
                                    start = Offset(padding, zeroY),
                                    end = Offset(width - padding, zeroY),
                                    strokeWidth = 1.5f
                            )
                        }
                    }
                }
            } else {
                // Empty state
                Box(
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                            text = "No power data available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
