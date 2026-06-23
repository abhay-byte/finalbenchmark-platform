package com.zenithblue.fb2Pro.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Performance monitoring system that tracks power consumption, CPU usage, CPU temperature, and
 * battery temperature during benchmark execution.
 *
 * Collects metrics every second and returns data in JSON format.
 */
class PerformanceMonitor(private val context: Context) {

    private val TAG = "PerformanceMonitor"

    // Utility instances
    private val cpuUtilizationUtils = CpuUtilizationUtils(context)
    private val powerUtils = PowerUtils(context)
    private val temperatureUtils = TemperatureUtils(context)

    // Data storage
    private val powerWatts = mutableListOf<Float>()
    private val powerTimestamps = mutableListOf<Long>()

    private val cpuUsage = mutableListOf<Float>()
    private val cpuUsageTimestamps = mutableListOf<Long>()

    private val cpuTemperature = mutableListOf<Float>()
    private val cpuTempTimestamps = mutableListOf<Long>()

    private val batteryTemperature = mutableListOf<Float>()
    private val batteryTempTimestamps = mutableListOf<Long>()

    // Monitoring state
    private var monitoringJob: Job? = null
    private var isMonitoring = false

    /**
     * Start monitoring performance metrics. Collects data every 1 second in a background coroutine.
     */
    fun start() {
        if (isMonitoring) {
            Log.w(TAG, "Monitoring already started")
            return
        }

        // Clear previous data
        clearData()

        isMonitoring = true

        // Start monitoring in a background coroutine
        monitoringJob =
                CoroutineScope(Dispatchers.Default).launch {
                    Log.d(TAG, "Performance monitoring started")

                    while (isActive && isMonitoring) {
                        try {
                            val timestamp = System.currentTimeMillis()

                            // Collect power consumption
                            val power = powerUtils.estimatePowerConsumption()
                            powerWatts.add(power)
                            powerTimestamps.add(timestamp)

                            // Collect CPU usage
                            val cpu = cpuUtilizationUtils.getCpuUtilizationPercentage()
                            cpuUsage.add(cpu)
                            cpuUsageTimestamps.add(timestamp)

                            // Collect CPU temperature
                            val cpuTemp = temperatureUtils.getCpuTemperature()
                            cpuTemperature.add(cpuTemp)
                            cpuTempTimestamps.add(timestamp)

                            // Collect battery temperature
                            val batteryTemp = temperatureUtils.getBatteryTemperature()
                            batteryTemperature.add(batteryTemp)
                            batteryTempTimestamps.add(timestamp)

                            Log.d(
                                    TAG,
                                    "Collected metrics - Power: ${power}W, CPU: ${cpu}%, CPU Temp: ${cpuTemp}°C, Battery Temp: ${batteryTemp}°C"
                            )

                            // Wait 1 second before next collection
                            delay(1000)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error collecting metrics", e)
                        }
                    }

                    Log.d(TAG, "Performance monitoring stopped")
                }
    }

    /**
     * Stop monitoring and return collected data as JSON.
     *
     * @return JSON string with format: { "powerConsumption": {
     * ```
     *     "watts": [1.2, 1.5, 1.8],
     *     "timestamp": [1633072800, 1633072801, 1633072802]
     * ```
     * }, "cpuUsage": {
     * ```
     *     "usage": [50, 60, 70],
     *     "timestamp": [1633072800, 1633072801, 1633072802]
     * ```
     * }, "cpuTemperature": {
     * ```
     *     "temperature": [50, 60, 70],
     *     "timestamp": [1633072800, 1633072801, 1633072802]
     * ```
     * }, "batteryTemperature": {
     * ```
     *     "temperature": [50, 60, 70],
     *     "timestamp": [1633072800, 1633072801, 1633072802]
     * ```
     * } }
     */
    fun stop(): String {
        if (!isMonitoring) {
            Log.w(TAG, "Monitoring not started")
            return "{}"
        }

        isMonitoring = false

        // Cancel the monitoring job
        monitoringJob?.cancel()
        monitoringJob = null

        // Build JSON object
        return try {
            val json = JSONObject()

            // Power consumption
            val powerObj = JSONObject()
            powerObj.put("watts", JSONArray(powerWatts))
            powerObj.put("timestamp", JSONArray(powerTimestamps))
            json.put("powerConsumption", powerObj)

            // CPU usage
            val cpuObj = JSONObject()
            cpuObj.put("usage", JSONArray(cpuUsage))
            cpuObj.put("timestamp", JSONArray(cpuUsageTimestamps))
            json.put("cpuUsage", cpuObj)

            // CPU temperature
            val cpuTempObj = JSONObject()
            cpuTempObj.put("temperature", JSONArray(cpuTemperature))
            cpuTempObj.put("timestamp", JSONArray(cpuTempTimestamps))
            json.put("cpuTemperature", cpuTempObj)

            // Battery temperature
            val batteryTempObj = JSONObject()
            batteryTempObj.put("temperature", JSONArray(batteryTemperature))
            batteryTempObj.put("timestamp", JSONArray(batteryTempTimestamps))
            json.put("batteryTemperature", batteryTempObj)

            val result = json.toString()
            Log.d(TAG, "Generated JSON with ${powerWatts.size} data points")
            Log.d(TAG, "JSON preview: ${result.take(200)}...")

            result
        } catch (e: Exception) {
            Log.e(TAG, "Error generating JSON", e)
            "{}"
        }
    }

    /** Clear all collected data */
    private fun clearData() {
        powerWatts.clear()
        powerTimestamps.clear()
        cpuUsage.clear()
        cpuUsageTimestamps.clear()
        cpuTemperature.clear()
        cpuTempTimestamps.clear()
        batteryTemperature.clear()
        batteryTempTimestamps.clear()
    }

    /** Check if monitoring is currently active */
    fun isMonitoring(): Boolean = isMonitoring

    /** Get the number of data points collected so far */
    fun getDataPointCount(): Int = powerWatts.size
}
