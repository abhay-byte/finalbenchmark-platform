package com.ivarna.finalbenchmark2.utils

import android.content.Context
import android.util.Log
import java.io.File
import kotlin.math.exp
import kotlin.math.pow

class CpuUtilizationUtils(context: Context) {
    private val cpuDir = File("/sys/devices/system/cpu/")
    private val totalCores = Runtime.getRuntime().availableProcessors()
    
    // Exponent calibrated so:
    // - 2000/3200 (62.5% ratio) → ~30% utilization
    // - 2800/3200 (87.5% ratio) → ~80% utilization
    private val SCALING_EXPONENT = 2.5f
    
    // Cache for CPU frequencies with 200ms refresh rate
    private val frequencyCache = mutableMapOf<Int, Pair<Long, Long>>() // coreId to (currentFreq, maxFreq)
    private var lastCacheUpdate = 0L
    private val CACHE_DURATION_MS = 200L // 200ms cache duration
    
    // Variables for core utilizations
    private val coreUtilizations = mutableMapOf<Int, Float>()

    /**
     * Calculate CPU utilization percentage using exponential scaling.
     * This better reflects real-world CPU workload intensity based on power consumption physics:
     * P ∝ V² × f where voltage scales non-linearly with frequency.
     * At lower frequencies, CPU does less intensive work despite clock cycles.
     *
     * @param useExponentialScaling Whether to use exponential scaling (true) or linear scaling (false)
     */
    fun getCpuUtilizationPercentage(useExponentialScaling: Boolean = true): Float {
        return try {
            if (!cpuDir.exists()) {
                return 0f
            }

            var totalWeightedUtilization = 0f
            var totalWeight = 0L
            var validCores = 0

            for (i in 0 until totalCores) {
                val currentFreq = getCurrentCpuFreq(i)
                val maxFreq = getMaxCpuFreq(i)
                
                if (currentFreq > 0 && maxFreq > 0) {
                    // Calculate utilization based on scaling method
                    val utilization = if (useExponentialScaling) {
                        calculateExponentialUtilization(currentFreq, maxFreq)
                    } else {
                        calculateLinearUtilization(currentFreq, maxFreq)
                    }
                    
                    // Weight by max frequency (performance cores contribute more)
                    val weight = maxFreq
                    totalWeightedUtilization += utilization * weight
                    totalWeight += weight
                    validCores++
                }
            }

            if (validCores > 0 && totalWeight > 0) {
                val weightedUtilization = totalWeightedUtilization / totalWeight
                return weightedUtilization.coerceIn(0f, 100f) // Clamp between 0-100%
            }
            
            0f
        } catch (e: Exception) {
            Log.e("CpuUtilizationUtils", "Error getting CPU utilization", e)
            0f
        }
    }

    /**
     * CORRECTED: Calculates CPU utilization using exponential scaling.
     * This compresses lower frequencies and expands higher frequencies.
     *
     * Physics basis: CPU power ∝ V² × f, where voltage scales non-linearly with frequency.
     * At lower frequencies, CPU does proportionally LESS work than the linear ratio suggests.
     *
     * Formula: utilization = (ratio^EXPONENT) × 100
     * Where EXPONENT = 2.5 to match power consumption characteristics
     */
    fun calculateExponentialUtilization(currentSpeed: Long, maxSpeed: Long): Float {
        if (maxSpeed == 0L) return 0f
        if (currentSpeed <= 0L) return 0f
        
        val ratio = (currentSpeed.toFloat() / maxSpeed.toFloat()).coerceIn(0f, 1f)
        
        // CORRECTED: Exponential scaling compresses lower values
        // OLD (WRONG): ratio^(1/2.5) inflates values
        // NEW (CORRECT): ratio^2.5 compresses lower values, keeps high values high
        val utilization = ratio.pow(SCALING_EXPONENT) * 100f
        
        return utilization.coerceIn(0f, 100f)
    }

    /**
     * Alternative: Power curve scaling (closer to actual physics)
     * Models P ∝ V² × f relationship
     */
    fun calculatePowerCurveUtilization(currentSpeed: Long, maxSpeed: Long): Float {
        if (maxSpeed == 0L) return 0f
        if (currentSpeed <= 0L) return 0f
        
        val ratio = (currentSpeed.toFloat() / maxSpeed.toFloat()).coerceIn(0f, 1f)
        
        // Approximate voltage scaling: V ≈ 0.6 + 0.4×ratio (realistic for modern CPUs)
        val voltageRatio = 0.6f + 0.4f * ratio
        
        // Power-based utilization: P ∝ V² × f
        val powerRatio = voltageRatio * voltageRatio * ratio
        
        // Normalize so max speed = 100%
        val maxPower = 1.0f * 1.0f * 1.0f  // At ratio=1, voltage=1
        val utilization = (powerRatio / maxPower).toFloat() * 100f
        
        return utilization.coerceIn(0f, 100f)
    }

    /**
     * Alternative: Sigmoid-like scaling for smooth compression
     */
    fun calculateSigmoidUtilization(currentSpeed: Long, maxSpeed: Long): Float {
        if (maxSpeed == 0L) return 0f
        if (currentSpeed <= 0L) return 0f
        
        val ratio = (currentSpeed.toFloat() / maxSpeed.toFloat()).coerceIn(0f, 1f)
        
        // Modified sigmoid: utilization = 100 / (1 + e^(-k(x-0.5)))
        // This creates S-curve that compresses low values, expands high values
        val k = 8f  // Steepness factor
        val shifted = ratio - 0.5f
        val sigmoid = 1f / (1f + exp(-k * shifted))
        
        // Normalize to 0-100 range
        val utilization = sigmoid * 100f
        
        return utilization.coerceIn(0f, 100f)
    }

    /**
     * Calculate linear utilization (original method for comparison)
     */
    fun calculateLinearUtilization(currentSpeed: Long, maxSpeed: Long): Float {
        if (maxSpeed == 0L) return 0f
        if (currentSpeed <= 0L) return 0f
        
        return (currentSpeed.toFloat() / maxSpeed.toFloat() * 100).coerceIn(0f, 100f)
    }

    /**
     * Get the current frequency of a specific CPU core
     */
    private fun getCurrentCpuFreq(coreIndex: Int): Long {
        try {
            val scalingCurFreqFile = File("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_cur_freq")
            val cpuFreqFile = File("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/cpuinfo_cur_freq")
            
            // Try scaling_cur_freq first (if available)
            // sysfs always returns frequencies in kHz, so no conversion needed
            if (scalingCurFreqFile.exists()) {
                val freq = scalingCurFreqFile.readText().trim().toLongOrNull()
                if (freq != null && freq > 0) {
                    return freq // Return frequency as kHz (sysfs standard unit)
                }
            }
            
            // Fallback to cpuinfo_cur_freq
            // sysfs always returns frequencies in kHz, so no conversion needed
            if (cpuFreqFile.exists()) {
                val freq = cpuFreqFile.readText().trim().toLongOrNull()
                if (freq != null && freq > 0) {
                    return freq // Return frequency as kHz (sysfs standard unit)
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
            val cpuinfoMaxFreqFile = File("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/cpuinfo_max_freq")
            val scalingMaxFreqFile = File("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_max_freq")
            
            // Try cpuinfo_max_freq first (theoretical max)
            if (cpuinfoMaxFreqFile.exists()) {
                val freq = cpuinfoMaxFreqFile.readText().trim().toLongOrNull()
                if (freq != null && freq > 0) {
                    return freq // Return frequency as kHz (sysfs standard unit)
                }
            }
            
            // Fallback to scaling_max_freq (current scaling max)
            if (scalingMaxFreqFile.exists()) {
                val freq = scalingMaxFreqFile.readText().trim().toLongOrNull()
                if (freq != null && freq > 0) {
                    return freq // Return frequency as kHz (sysfs standard unit)
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
        // Use cache if it's still valid
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCacheUpdate < CACHE_DURATION_MS && frequencyCache.isNotEmpty()) {
            return frequencyCache.toMap()
        }
        
        val frequencies = mutableMapOf<Int, Pair<Long, Long>>()
        
        for (i in 0 until totalCores) {
            val currentFreq = getCurrentCpuFreq(i)
            val maxFreq = getMaxCpuFreq(i)
            frequencies[i] = Pair(currentFreq, maxFreq)
        }
        
        // Update cache
        frequencyCache.clear()
        frequencyCache.putAll(frequencies)
        lastCacheUpdate = currentTime
        
        return frequencies
    }

    /**
     * Calculate individual core utilization percentages using exponential scaling
     */
    fun getCoreUtilizationPercentages(useExponentialScaling: Boolean = true): Map<Int, Float> {
        try {
            if (!cpuDir.exists()) {
                return emptyMap()
            }

            val coreUtilizations = mutableMapOf<Int, Float>()
            val allFrequencies = getAllCoreFrequencies()

            for ((coreIndex, freqPair) in allFrequencies) {
                val (currentFreq, maxFreq) = freqPair
                if (currentFreq > 0 && maxFreq > 0) {
                    val utilization = if (useExponentialScaling) {
                        calculateExponentialUtilization(currentFreq, maxFreq)
                    } else {
                        calculateLinearUtilization(currentFreq, maxFreq)
                    }
                    coreUtilizations[coreIndex] = utilization
                } else {
                    coreUtilizations[coreIndex] = 0f
                }
            }

            return coreUtilizations
        } catch (e: Exception) {
            Log.e("CpuUtilizationUtils", "Error getting core utilizations", e)
            return emptyMap()
        }
    }

    /**
     * Calculate per-cluster utilization (for big.LITTLE visualization)
     * Groups cores by max frequency to represent different cluster types
     */
    fun getClusterUtilization(useExponentialScaling: Boolean = true): Map<String, Float> {
        val frequencies = getAllCoreFrequencies()
        if (frequencies.isEmpty()) return emptyMap()

        // Group cores by max frequency (assumes cores with same max freq are in same cluster)
        val clusters = frequencies.entries.groupBy { it.value.second } // Group by max frequency

        return clusters.mapKeys { (maxFreq, _) ->
            when {
                maxFreq >= 3000000L -> "Prime"      // > 3.0 GHz
                maxFreq >= 2500000L -> "Performance" // 2.5-3.0 GHz
                maxFreq >= 2000000L -> "Balanced"    // 2.0-2.5 GHz
                else -> "Efficiency"                 // < 2.0 GHz
            }
        }.mapValues { (_, clusterCores) ->
            val onlineCores = clusterCores.filter { entry -> entry.value.first > 0 && entry.value.second > 0 }
            if (onlineCores.isEmpty()) return@mapValues 0f

            val avgUtilization = onlineCores.map { entry ->
                val (currentFreq, maxFreq) = entry.value
                if (useExponentialScaling) {
                    calculateExponentialUtilization(currentFreq, maxFreq)
                } else {
                    calculateLinearUtilization(currentFreq, maxFreq)
                }
            }.average().toFloat()

            avgUtilization
        }
    }

    /**
     * Demonstrate the difference between scaling methods
     */
    fun demonstrateScalingDifference(): String {
        val maxSpeed = 3200000L  // 3.2 GHz
        val testSpeeds = listOf(
            800000L,   // 0.8 GHz
            1600000L,  // 1.6 GHz
            2000000L, // 2.0 GHz
            2400000L,  // 2.4 GHz
            2800000L,  // 2.8 GHz
            3200000L   // 3.2 GHz
        )

        val result = StringBuilder()
        result.append("Speed(MHz) | Linear% | Exponential% | PowerCurve% | Sigmoid%\n")
        result.append("--------------------------------------------------------------\n")

        testSpeeds.forEach { speed ->
            val speedMhz = (speed / 1000).toInt()
            val linear = (speed.toFloat() / maxSpeed * 100)
            val exponential = calculateExponentialUtilization(speed, maxSpeed)
            val powerCurve = calculatePowerCurveUtilization(speed, maxSpeed)
            val sigmoid = calculateSigmoidUtilization(speed, maxSpeed)

            result.append("${speedMhz.toString().padEnd(10)} | " +
                    "${linear.toInt().toString().padEnd(7)} | " +
                    "${exponential.toInt().toString().padEnd(12)} | " +
                    "${powerCurve.toInt().toString().padEnd(11)} | " +
                    "${sigmoid.toInt()}\n")
        }

        return result.toString()
    }
    
    /**
     * Get the current CPU scaling governor
     */
    fun getCurrentCpuGovernor(): String? {
        try {
            // Usually all cores use the same governor, so we check core 0
            val governorFile = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
            if (governorFile.exists()) {
                return governorFile.readText().trim()
            }
        } catch (e: Exception) {
            Log.e("CpuUtilizationUtils", "Error getting CPU governor", e)
        }
        return null
    }
    
    /**
     * Debug function to log step-by-step calculation to help identify issues
     */
    fun debugCpuUtilization(): String {
        val debugResult = StringBuilder()
        debugResult.append("=== CPU Utilization Debug ===\n")
        debugResult.append("Total cores: $totalCores\n")
        
        var totalWeightedUtilization = 0f
        var totalWeight = 0L
        var validCores = 0
        
        for (i in 0 until totalCores) {
            val currentFreq = getCurrentCpuFreq(i)
            val maxFreq = getMaxCpuFreq(i)
            
            debugResult.append("Core $i: current=${currentFreq}kHz, max=${maxFreq}kHz\n")
            
            if (currentFreq > 0 && maxFreq > 0) {
                val ratio = (currentFreq.toFloat() / maxFreq.toFloat()).coerceIn(0f, 1f)
                val utilization = calculateExponentialUtilization(currentFreq, maxFreq)
                
                debugResult.append("  -> ratio=${String.format("%.4f", ratio)}, utilization=${String.format("%.2f", utilization)}%\n")
                
                val weight = maxFreq
                totalWeightedUtilization += utilization * weight
                totalWeight += weight
                validCores++
            } else {
                debugResult.append("  -> invalid frequencies (current: ${currentFreq}, max: ${maxFreq})\n")
            }
        }
        
        debugResult.append("Valid cores: $validCores\n")
        debugResult.append("Total weight: $totalWeight\n")
        
        if (validCores > 0 && totalWeight > 0) {
            val weightedUtilization = totalWeightedUtilization / totalWeight
            val finalUtilization = weightedUtilization.coerceIn(0f, 100f)
            debugResult.append("Final utilization: ${String.format("%.2f", finalUtilization)}%\n")
        } else {
            debugResult.append("Final utilization: 0% (no valid cores or zero weight)\n")
        }
        
        return debugResult.toString()
    }
}