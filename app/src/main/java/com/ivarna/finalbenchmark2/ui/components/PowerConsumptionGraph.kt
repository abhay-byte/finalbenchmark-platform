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
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

@Composable
fun PowerConsumptionGraph(
    dataPoints: List<PowerDataPoint>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    
    // Calculate dynamic Y-axis range based on data
    val maxPower = remember(dataPoints) {
        if (dataPoints.isEmpty()) 10f
        else {
            val max = dataPoints.maxOfOrNull { it.powerWatts } ?: 10f
            // Round up to nearest 5W for cleaner axis
            (ceil(kotlin.math.abs(max) / 5f) * 5f).coerceAtLeast(5f)
        }
    }
    
    val minPower = remember(dataPoints) {
        if (dataPoints.isEmpty()) -10f
        else {
            val min = dataPoints.minOfOrNull { it.powerWatts } ?: -10f
            // Round down to nearest 5W
            -(ceil(kotlin.math.abs(min) / 5f) * 5f).coerceAtLeast(5f)
        }
    }
    
    // Determine if we need to show negative values (charging)
    val hasNegativeValues = remember(dataPoints) {
        dataPoints.any { it.powerWatts < 0 }
    }
    
    // Calculate the full range
    val fullRange = maxPower - minPower
    val zeroPosition = if (fullRange != 0f) (0f - minPower) / fullRange else 0.5f
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = "Power Consumption (30s)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Current power consumption indicator
            val currentPower = dataPoints.lastOrNull()?.powerWatts ?: 0f
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Current: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%.2f W", currentPower),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Average power (optional)
                if (dataPoints.isNotEmpty()) {
                    val avgPower = dataPoints.map { it.powerWatts }.average().toFloat()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Avg: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%.2f W", avgPower),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
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
                        val padding = 50f
                        val graphWidth = width - padding * 2
                        val graphHeight = height - padding * 2
                        
                        // Draw Y-axis (Power in Watts)
                        drawLine(
                            color = surfaceVariantColor,
                            start = Offset(padding, padding),
                            end = Offset(padding, height - padding),
                            strokeWidth = 2f
                        )
                        
                        // Draw X-axis (time) - but this should be at the zero point, not bottom
                        val zeroY = height - padding - (zeroPosition * graphHeight)
                        drawLine(
                            color = surfaceVariantColor,
                            start = Offset(padding, zeroY),
                            end = Offset(width - padding, zeroY),
                            strokeWidth = 2f
                        )
                        
                        val powerRange = maxPower - minPower
                        val numYTicks = 5
                        
                        // Draw Y-axis grid lines and labels (power values)
                        for (i in 0..numYTicks) {
                            val powerValue = minPower + (powerRange * i / numYTicks)
                            val y = height - padding - ((powerValue - minPower) / powerRange) * graphHeight
                            
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
                                    String.format("%.1fW", powerValue),
                                    padding - 8f,
                                    y + 8f,
                                    paint
                                )
                            }
                        }
                        
                        // Draw X-axis labels (0s, 15s, 30s)
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                color = onSurfaceVariantColor.toArgb()
                                textSize = 24f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            
                            canvas.nativeCanvas.drawText(
                                "0s",
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
                                "30s",
                                width - padding,
                                height - padding + 30f,
                                paint
                            )
                        }
                        
                        // Calculate time range (last 30 seconds)
                        val currentTime = System.currentTimeMillis()
                        val startTime = currentTime - 30_000L
                        
                        // Draw the line graph
                        if (dataPoints.size >= 2) {
                            val path = Path()
                            
                            dataPoints.forEachIndexed { index, point ->
                                val timeProgress = (point.timestamp - startTime).toFloat() / 30_000f
                                val powerProgress = if (powerRange > 0) {
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
                            
                            // Draw the line
                            drawPath(
                                path = path,
                                color = if (currentPower >= 0) primaryColor else Color(0xFF4CAF50), // Different color for charging
                                style = Stroke(width = 3f)
                            )
                            
                            // Optional: Draw data points as circles
                            dataPoints.forEach { point ->
                                val timeProgress = (point.timestamp - startTime).toFloat() / 30_000f
                                val powerProgress = if (powerRange > 0) {
                                    (point.powerWatts - minPower) / powerRange
                                } else {
                                    0.5f
                                }
                                
                                val x = padding + (timeProgress * graphWidth)
                                val y = height - padding - (powerProgress * graphHeight)
                                
                                drawCircle(
                                    color = if (point.powerWatts >= 0) primaryColor else Color(0xFF4CAF50), // Different color for charging
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
                        text = "Collecting power data...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Optional: Power status indicator
            Spacer(modifier = Modifier.height(8.dp))
            
            val powerStatus = when {
                currentPower > 0 -> {
                    // Discharging
                    when {
                        currentPower > 15f -> "High Discharge"
                        currentPower > 8f -> "Moderate Discharge"
                        currentPower > 3f -> "Low Discharge"
                        else -> "Minimal Discharge"
                    }
                }
                currentPower < 0 -> {
                    // Charging
                    when {
                        currentPower < -15f -> "High Charge"
                        currentPower < -8f -> "Moderate Charge"
                        currentPower < -3f -> "Low Charge"
                        else -> "Minimal Charge"
                    }
                }
                else -> "No Change"
            }
            
            val statusColor = when {
                currentPower > 15f -> MaterialTheme.colorScheme.error // High discharge
                currentPower > 8f -> Color(0xFFFB923C) // Moderate discharge (orange)
                currentPower < 0f -> Color(0xFF4CAF50) // Charging (green)
                else -> MaterialTheme.colorScheme.tertiary
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Status: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = powerStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }
        }
    }
}