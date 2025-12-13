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

/**
 * A CPU Utilization Graph designed for historical/result display. Unlike the live monitoring graph,
 * this uses relative time from the first data point and shows summary statistics (Avg, Min, Max)
 * instead of "Current".
 */
@Composable
fun ResultCpuGraph(
        dataPoints: List<CpuDataPoint>,
        totalDurationMs: Long = 0L,
        modifier: Modifier = Modifier
) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
        val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
        val secondaryColor = MaterialTheme.colorScheme.secondary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary
        val errorColor = MaterialTheme.colorScheme.error

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
                if (totalDurationMs > 0) totalDurationMs
                else (endTime - startTime).coerceAtLeast(1000L)
        val durationSeconds = (timeRangeMs / 1000.0).toInt()

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
                                text = "CPU Utilization",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Summary statistics
                        if (dataPoints.isNotEmpty()) {
                                val avgCpu = dataPoints.map { it.utilization }.average().toFloat()
                                val minCpu = dataPoints.minOfOrNull { it.utilization } ?: 0f
                                val maxCpu = dataPoints.maxOfOrNull { it.utilization } ?: 0f

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
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                                Text(
                                                        text = "${avgCpu.toInt()}%",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = primaryColor
                                                )
                                        }

                                        // Min
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                        text = "Min: ",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                                Text(
                                                        text = "${minCpu.toInt()}%",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = tertiaryColor
                                                )
                                        }

                                        // Max
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                        text = "Max: ",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                                Text(
                                                        text = "${maxCpu.toInt()}%",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color =
                                                                if (maxCpu > 90) errorColor
                                                                else secondaryColor
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
                                                        end =
                                                                Offset(
                                                                        width - padding,
                                                                        height - padding
                                                                ),
                                                        strokeWidth = 2f
                                                )

                                                // Draw Y-axis grid lines and labels (0%, 25%, 50%,
                                                // 75%, 100%)
                                                for (i in 0..4) {
                                                        val y =
                                                                height -
                                                                        padding -
                                                                        (graphHeight * i / 4)
                                                        val percentage = i * 25

                                                        // Grid line
                                                        drawLine(
                                                                color =
                                                                        surfaceVariantColor.copy(
                                                                                alpha = 0.3f
                                                                        ),
                                                                start = Offset(padding, y),
                                                                end = Offset(width - padding, y),
                                                                strokeWidth = 1f
                                                        )

                                                        // Label
                                                        drawIntoCanvas { canvas ->
                                                                val paint =
                                                                        android.graphics.Paint()
                                                                                .apply {
                                                                                        color =
                                                                                                onSurfaceVariantColor
                                                                                                        .toArgb()
                                                                                        textSize =
                                                                                                20f
                                                                                        textAlign =
                                                                                                android.graphics
                                                                                                        .Paint
                                                                                                        .Align
                                                                                                        .RIGHT
                                                                                }
                                                                canvas.nativeCanvas.drawText(
                                                                        "$percentage%",
                                                                        padding - 8f,
                                                                        y + 6f,
                                                                        paint
                                                                )
                                                        }
                                                }

                                                // Draw X-axis labels (time from start)
                                                drawIntoCanvas { canvas ->
                                                        val paint =
                                                                android.graphics.Paint().apply {
                                                                        color =
                                                                                onSurfaceVariantColor
                                                                                        .toArgb()
                                                                        textSize = 20f
                                                                        textAlign =
                                                                                android.graphics
                                                                                        .Paint.Align
                                                                                        .CENTER
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
                                                                        (point.timestamp -
                                                                                        startTime)
                                                                                .toFloat() /
                                                                                timeRangeMs
                                                                                        .toFloat()
                                                                val x =
                                                                        padding +
                                                                                (timeProgress *
                                                                                        graphWidth)
                                                                val y =
                                                                        height -
                                                                                padding -
                                                                                ((point.utilization /
                                                                                        100f) *
                                                                                        graphHeight)

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

                                                        // Draw data points as circles
                                                        dataPoints.forEach { point ->
                                                                val timeProgress =
                                                                        (point.timestamp -
                                                                                        startTime)
                                                                                .toFloat() /
                                                                                timeRangeMs
                                                                                        .toFloat()
                                                                val x =
                                                                        padding +
                                                                                (timeProgress *
                                                                                        graphWidth)
                                                                val y =
                                                                        height -
                                                                                padding -
                                                                                ((point.utilization /
                                                                                        100f) *
                                                                                        graphHeight)

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
                                        modifier = Modifier.fillMaxWidth().height(140.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Text(
                                                text = "No CPU data available",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                        )
                                }
                        }
                }
        }
}
