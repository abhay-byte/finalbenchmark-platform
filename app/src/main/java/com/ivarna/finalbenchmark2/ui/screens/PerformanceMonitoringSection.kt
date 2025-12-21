package com.ivarna.finalbenchmark2.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.finalbenchmark2.ui.components.BatteryTemperatureGraph
import com.ivarna.finalbenchmark2.ui.components.CpuDataPoint
import com.ivarna.finalbenchmark2.ui.components.CpuTemperatureGraph
import com.ivarna.finalbenchmark2.ui.components.PowerDataPoint
import com.ivarna.finalbenchmark2.ui.components.ResultCpuGraph
import com.ivarna.finalbenchmark2.ui.components.ResultPowerGraph
import com.ivarna.finalbenchmark2.ui.components.TemperatureDataPoint
import org.json.JSONObject

@Composable
fun PerformanceMonitoringSection(performanceMetricsJson: String) {
    // Parse the performance metrics JSON
    val parsedData =
            remember(performanceMetricsJson) {
                try {
                    if (performanceMetricsJson.isBlank() || performanceMetricsJson == "{}") {
                        return@remember ParsedPerformanceData(
                                emptyList(),
                                emptyList(),
                                emptyList(),
                                emptyList()
                        )
                    }

                    val json = JSONObject(performanceMetricsJson)

                    // Parse power consumption
                    val powerObj = json.optJSONObject("powerConsumption")
                    val powerDataPoints =
                            if (powerObj != null) {
                                val watts = powerObj.optJSONArray("watts")
                                val timestamps = powerObj.optJSONArray("timestamp")
                                if (watts != null &&
                                                timestamps != null &&
                                                watts.length() == timestamps.length()
                                ) {
                                    (0 until watts.length()).map { i ->
                                        PowerDataPoint(
                                                timestamp = timestamps.getLong(i),
                                                powerWatts = watts.getDouble(i).toFloat()
                                        )
                                    }
                                } else {
                                    emptyList()
                                }
                            } else {
                                emptyList()
                            }

                    // Parse CPU usage
                    val cpuObj = json.optJSONObject("cpuUsage")
                    val cpuUsageDataPoints =
                            if (cpuObj != null) {
                                val usage = cpuObj.optJSONArray("usage")
                                val timestamps = cpuObj.optJSONArray("timestamp")
                                if (usage != null &&
                                                timestamps != null &&
                                                usage.length() == timestamps.length()
                                ) {
                                    (0 until usage.length()).map { i ->
                                        CpuDataPoint(
                                                timestamp = timestamps.getLong(i),
                                                utilization = usage.getDouble(i).toFloat()
                                        )
                                    }
                                } else {
                                    emptyList()
                                }
                            } else {
                                emptyList()
                            }

                    // Parse CPU temperature
                    val cpuTempObj = json.optJSONObject("cpuTemperature")
                    val cpuTempDataPoints =
                            if (cpuTempObj != null) {
                                val temps = cpuTempObj.optJSONArray("temperature")
                                val timestamps = cpuTempObj.optJSONArray("timestamp")
                                if (temps != null &&
                                                timestamps != null &&
                                                temps.length() == timestamps.length()
                                ) {
                                    (0 until temps.length()).map { i ->
                                        TemperatureDataPoint(
                                                timestamp = timestamps.getLong(i),
                                                temperature = temps.getDouble(i).toFloat()
                                        )
                                    }
                                } else {
                                    emptyList()
                                }
                            } else {
                                emptyList()
                            }

                    // Parse battery temperature
                    val batteryTempObj = json.optJSONObject("batteryTemperature")
                    val batteryTempDataPoints =
                            if (batteryTempObj != null) {
                                val temps = batteryTempObj.optJSONArray("temperature")
                                val timestamps = batteryTempObj.optJSONArray("timestamp")
                                if (temps != null &&
                                                timestamps != null &&
                                                temps.length() == timestamps.length()
                                ) {
                                    (0 until temps.length()).map { i ->
                                        TemperatureDataPoint(
                                                timestamp = timestamps.getLong(i),
                                                temperature = temps.getDouble(i).toFloat()
                                        )
                                    }
                                } else {
                                    emptyList()
                                }
                            } else {
                                emptyList()
                            }

                    ParsedPerformanceData(
                            powerDataPoints,
                            cpuUsageDataPoints,
                            cpuTempDataPoints,
                            batteryTempDataPoints
                    )
                } catch (e: Exception) {
                    Log.e("PerformanceMonitoringSection", "Error parsing performance metrics", e)
                    ParsedPerformanceData(emptyList(), emptyList(), emptyList(), emptyList())
                }
            }

    val powerData = parsedData.powerData
    val cpuUsageData = parsedData.cpuUsageData
    val cpuTempData = parsedData.cpuTempData
    val batteryTempData = parsedData.batteryTempData

    // Only show section if we have data
    if (powerData.isNotEmpty() ||
                    cpuUsageData.isNotEmpty() ||
                    cpuTempData.isNotEmpty() ||
                    batteryTempData.isNotEmpty()
    ) {
        // Calculate total runtime from timestamps
        val totalRuntimeSeconds =
                remember(powerData, cpuUsageData, cpuTempData, batteryTempData) {
                    val allTimestamps = mutableListOf<Long>()
                    if (powerData.isNotEmpty()) {
                        allTimestamps.add(powerData.first().timestamp)
                        allTimestamps.add(powerData.last().timestamp)
                    }
                    if (cpuUsageData.isNotEmpty()) {
                        allTimestamps.add(cpuUsageData.first().timestamp)
                        allTimestamps.add(cpuUsageData.last().timestamp)
                    }
                    if (cpuTempData.isNotEmpty()) {
                        allTimestamps.add(cpuTempData.first().timestamp)
                        allTimestamps.add(cpuTempData.last().timestamp)
                    }
                    if (batteryTempData.isNotEmpty()) {
                        allTimestamps.add(batteryTempData.first().timestamp)
                        allTimestamps.add(batteryTempData.last().timestamp)
                    }

                    if (allTimestamps.isNotEmpty()) {
                        val duration =
                                (allTimestamps.maxOrNull()!! - allTimestamps.minOrNull()!!) / 1000.0
                        duration
                    } else {
                        0.0
                    }
                }

        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Box(
                    modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                    brush = Brush.verticalGradient(
                                            colors = listOf(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                                            )
                                    )
                            )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    // Title and Runtime
                    androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                    androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                                text = "Performance Monitoring",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                                text = String.format("Runtime: %.1fs", totalRuntimeSeconds),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

                // Power Consumption Graph
                if (powerData.isNotEmpty()) {
                    val totalDurationMs = (totalRuntimeSeconds * 1000).toLong()
                    ResultPowerGraph(
                            dataPoints = powerData,
                            totalDurationMs = totalDurationMs,
                            modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // CPU Usage Graph
                if (cpuUsageData.isNotEmpty()) {
                    val totalDurationMs = (totalRuntimeSeconds * 1000).toLong()
                    ResultCpuGraph(
                            dataPoints = cpuUsageData,
                            totalDurationMs = totalDurationMs,
                            modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // CPU Temperature Graph
                if (cpuTempData.isNotEmpty()) {
                    CpuTemperatureGraph(
                            dataPoints = cpuTempData,
                            modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Battery Temperature Graph
                if (batteryTempData.isNotEmpty()) {
                    BatteryTemperatureGraph(dataPoints = batteryTempData)
                }
                }
            }
        }
    }
}

// Helper data class for parsed performance data
private data class ParsedPerformanceData(
        val powerData: List<PowerDataPoint>,
        val cpuUsageData: List<CpuDataPoint>,
        val cpuTempData: List<TemperatureDataPoint>,
        val batteryTempData: List<TemperatureDataPoint>
)
