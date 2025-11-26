package com.ivarna.finalbenchmark2.utils

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import java.io.File

class TemperatureUtils(private val context: Context) {
    
    fun getCpuTemperature(): Float {
        // Try to read from thermal zones (most common location)
        val thermalPaths = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/thermal/thermal_zone2/temp",
            "/sys/class/thermal/thermal_zone3/temp",
            "/sys/class/thermal/thermal_zone4/temp",
            "/sys/class/thermal/thermal_zone5/temp",
            "/sys/class/thermal_zone6/temp",
            "/sys/class/thermal/thermal_zone7/temp",
            "/sys/class/thermal/thermal_zone8/temp",
            "/sys/class/thermal/thermal_zone9/temp"
        )
        
        for (path in thermalPaths) {
            val file = File(path)
            if (file.exists()) {
                try {
                    val temp = file.readText().trim().toFloat()
                    // Convert from millidegree Celsius to Celsius if needed
                    return if (temp > 1000) temp / 1000f else temp
                } catch (e: Exception) {
                    continue
                }
            }
        }
        
        // If thermal zones are not accessible, return -1 to indicate unavailable
        return -1f
    }
    
    fun getBatteryTemperature(): Float {
        val intent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        // Battery temperature is in tenths of a degree Celsius
        return temperature / 10f
    }
}