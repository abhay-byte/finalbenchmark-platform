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
import kotlin.math.abs
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
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error
    val errorContainerColor = MaterialTheme.colorScheme.errorContainer
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    
    // Calculate dynamic Y-axis range based on data with padding
    val (minPower, maxPower) = remember(dataPoints) {
        if (dataPoints.isEmpty()) {
            Pair(-10f, 10f) // Default range if no data
        } else {
            val min = dataPoints.minOfOrNull { it.powerWatts } ?: -5f
            val max = dataPoints.maxOfOrNull { it.powerWatts } ?: 5f
            
            // Calculate range and add 10-20% padding
            val range = max - min
            val padding = if (range > 0) range * 0.2f else 2f // 20% padding or 2W if range is 0
            
            Pair(min - padding, max + padding)
        }
    }
    
    // Determine if we need to show negative values (charging)
    val hasNegativeValues = remember(dataPoints) {
        dataPoints.any { it.powerWatts < 0 }
    }
    
    // Calculate the full range
    val fullRange = maxPower - minPower
    // Zero position is now at the top (negative values above X-axis, positive below)
    // This creates an inverted Y-axis where negative values appear above the X-axis
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
                        color = if (currentPower >= 0) secondaryColor else errorColor  // Charging: secondary, Discharging: error
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
                            color = if (avgPower >= 0) secondaryColor else errorColor // Charging: secondary, Discharging: error
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
                        
                        // For true Y-axis inversion, we need to flip the coordinate system
                        // Negative values should appear above the X-axis (top half), positive below (bottom half)
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
                            // For inverted Y-axis, we want negative values at the top and positive at the bottom
                            // So we calculate the Y position with the inverted mapping
                            val powerProgress = (powerValue - minPower) / powerRange
                            val y = padding + (powerProgress * graphHeight)
                            
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
                                    textSize = 16f
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
                        
                        // Draw X-axis labels (0s, 15s, 30s)
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                color = onSurfaceVariantColor.toArgb()
                                textSize = 16f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            
                            // Calculate time range (last 30 seconds)
                            val currentTime = System.currentTimeMillis()
                            val startTime = currentTime - 30_000L
                            
                            // Get the latest 30 seconds of data for positioning
                            val relevantDataPoints = dataPoints.filter { it.timestamp >= startTime }
                            
                            if (relevantDataPoints.isNotEmpty()) {
                                val earliestPoint = relevantDataPoints.minByOrNull { it.timestamp } ?: relevantDataPoints.first()
                                val latestPoint = relevantDataPoints.maxByOrNull { it.timestamp } ?: relevantDataPoints.last()
                                
                                canvas.nativeCanvas.drawText(
                                    "0s",
                                    padding,
                                    height - padding + 20f,
                                    paint
                                )
                                canvas.nativeCanvas.drawText(
                                    "15s",
                                    padding + graphWidth / 2,
                                    height - padding + 20f,
                                    paint
                                )
                                canvas.nativeCanvas.drawText(
                                    "30s",
                                    width - padding,
                                    height - padding + 20f,
                                    paint
                                )
                            } else {
                                // Default labels if no data
                                canvas.nativeCanvas.drawText(
                                    "0s",
                                    padding,
                                    height - padding + 20f,
                                    paint
                                )
                                canvas.nativeCanvas.drawText(
                                    "15s",
                                    padding + graphWidth / 2,
                                    height - padding + 20f,
                                    paint
                                )
                                canvas.nativeCanvas.drawText(
                                    "30s",
                                    width - padding,
                                    height - padding + 20f,
                                    paint
                                )
                            }
                        }
                        
                        // Calculate time range (last 30 seconds)
                        val currentTime = System.currentTimeMillis()
                        val startTime = currentTime - 30_000L
                        
                        // Draw the line graph with inverted Y-axis
                        if (dataPoints.size >= 2) {
                            val path = Path()
                            
                            dataPoints.forEachIndexed { index, point ->
                                val timeProgress = (point.timestamp - startTime).toFloat() / 30_000f
                                val powerProgress = if (powerRange > 0) {
                                    (point.powerWatts - minPower) / powerRange
                                } else {
                                    0.5f
                                }
                                
                                // Calculate position with inverted Y-axis mapping (negative values at top)
                                val x = padding + (timeProgress * graphWidth)
                                val y = padding + (powerProgress * graphHeight)
                                
                                if (index == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }
                            
                            // Draw the line - use theme-aware colors for charging vs discharging
                            // Use Material Design colors that adapt to theme and are visible in both light/dark modes
                            val lineColor = if (currentPower >= 0) {
                                // Charging: use secondary color (visible in both themes)
                                secondaryColor
                            } else {
                                // Discharging: use error color (red-like, visible in both themes)
                                errorColor
                            }
                            
                            drawPath(
                                path = path,
                                color = lineColor,
                                style = Stroke(width = 3f)
                            )
                            
                            // Draw data points as circles
                            dataPoints.forEach { point ->
                                val timeProgress = (point.timestamp - startTime).toFloat() / 30_000f
                                val powerProgress = if (powerRange > 0) {
                                    (point.powerWatts - minPower) / powerRange
                                } else {
                                    0.5f
                                }
                                
                                val x = padding + (timeProgress * graphWidth)
                                val y = padding + (powerProgress * graphHeight)
                                
                                val pointColor = if (point.powerWatts >= 0) {
                                    // Charging: use secondary color
                                    secondaryColor
                                } else {
                                    // Discharging: use error color
                                    errorColor
                                }
                                
                                drawCircle(
                                    color = pointColor,
                                    radius = 4f,
                                    center = Offset(x, y)
                                )
                            }
                        }
                        
                        // Draw zero line if it's within the range
                        if (minPower < 0 && maxPower > 0) {
                            val powerProgress = (0f - minPower) / powerRange
                            val zeroYPos = padding + (powerProgress * graphHeight)
                            drawLine(
                                color = Color.Gray,
                                start = Offset(padding, zeroYPos),
                                end = Offset(width - padding, zeroYPos),
                                strokeWidth = 1f
                            )
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
            
            // Power status indicator with corrected logic
            Spacer(modifier = Modifier.height(8.dp))
            
            val powerStatus = when {
                currentPower > 0 -> {
                    // Discharging (power leaving device, negative in our display)
                    when {
                        currentPower > 15f -> "High Discharge"
                        currentPower > 8f -> "Moderate Discharge"
                        currentPower > 3f -> "Low Discharge"
                        else -> "Minimal Discharge"
                    }
                }
                currentPower < 0 -> {
                    // Charging (power going into device, positive in our display)
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
                currentPower > 15f -> errorColor // High discharge (error color)
                currentPower > 8f -> errorContainerColor // Moderate discharge
                currentPower < 0f -> secondaryColor // Charging (secondary color)
                else -> tertiaryColor
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