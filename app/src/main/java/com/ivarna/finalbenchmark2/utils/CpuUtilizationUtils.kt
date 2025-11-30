package com.ivarna.finalbenchmark2.utils

import android.content.Context
import android.util.Log
import java.io.File

class CpuUtilizationUtils(context: Context) {
    private val cpuDir = File("/sys/devices/system/cpu/")
    private val totalCores = Runtime.getRuntime().availableProcessors()

    /**
     * Calculate CPU utilization percentage based on current vs max clock speeds
     * Uses weighted calculation where cores with higher max frequency contribute more to utilization
     */
    fun getCpuUtilizationPercentage(): Float {
        try {
            if (!cpuDir.exists()) {
                return 0f
            }

            var totalWeightedCurrentFreq = 0.0
            var totalWeightedMaxFreq = 0.0
            var validCores = 0

            for (i in 0 until totalCores) {
                val currentFreq = getCurrentCpuFreq(i)
                val maxFreq = getMaxCpuFreq(i)
                
                if (currentFreq > 0 && maxFreq > 0) {
                    // Weight calculation: higher max frequency cores contribute more
                    totalWeightedCurrentFreq += currentFreq * maxFreq
                    totalWeightedMaxFreq += maxFreq * maxFreq
                    validCores++
                }
            }

            if (validCores > 0 && totalWeightedMaxFreq > 0) {
                val utilization = (totalWeightedCurrentFreq / totalWeightedMaxFreq * 100).toFloat()
                return utilization.coerceIn(0f, 100f) // Clamp between 0-100%
            }
        } catch (e: Exception) {
            Log.e("CpuUtilizationUtils", "Error getting CPU utilization", e)
        }
        
        return 0f
    }

    /**
     * Get the current frequency of a specific CPU core
     */
    private fun getCurrentCpuFreq(coreIndex: Int): Long {
        try {
            val scalingCurFreqFile = File("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_cur_freq")
            val cpuFreqFile = File("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/cpuinfo_cur_freq")
            
            // Try scaling_cur_freq first (if available)
            if (scalingCurFreqFile.exists()) {
                val freq = scalingCurFreqFile.readText().trim().toLongOrNull()
                if (freq != null && freq > 0) {
                    return freq / 1000L // Convert to MHz
                }
            }
            
            // Fallback to cpuinfo_cur_freq
            if (cpuFreqFile.exists()) {
                val freq = cpuFreqFile.readText().trim().toLongOrNull()
                if (freq != null && freq > 0) {
                    return freq / 1000L // Convert to MHz
                }
            }
            
            // Some systems use stats/trans_table
            val timeInStateFile = File("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/stats/time_in_state")
            if (timeInStateFile.exists()) {
                val lines = timeInStateFile.readLines()
                for (line in lines) {
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size == 2) {
                        val freq = parts[0].toLongOrNull()
                        val time = parts[1].toLongOrNull()
                        if (freq != null && time != null && time > 0) {
                            return freq / 1000L // Convert to MHz
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CpuUtilizationUtils", "Error getting current frequency for core $coreIndex", e)
        }
        
        return 0L
    }

    /**
     * Get the maximum frequency of a specific CPU core
     */
    private fun getMaxCpuFreq(coreIndex: Int): Long {
        try {
            val scalingMaxFreqFile = File("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/cpuinfo_max_freq")
            
            if (scalingMaxFreqFile.exists()) {
                val freq = scalingMaxFreqFile.readText().trim().toLongOrNull()
                if (freq != null && freq > 0) {
                    return freq / 1000L // Convert to MHz
                }
            }
        } catch (e: Exception) {
            Log.e("CpuUtilizationUtils", "Error getting max frequency for core $coreIndex", e)
        }
        
        return 0L
    }

    /**
     * Get all core frequencies as a map of core index to (current, max) frequencies
     */
    fun getAllCoreFrequencies(): Map<Int, Pair<Long, Long>> {
        val frequencies = mutableMapOf<Int, Pair<Long, Long>>()
        
        for (i in 0 until totalCores) {
            val currentFreq = getCurrentCpuFreq(i)
            val maxFreq = getMaxCpuFreq(i)
            frequencies[i] = Pair(currentFreq, maxFreq)
        }
        
        return frequencies
    }
}