package com.ivarna.finalbenchmark2.utils

import android.content.Context
import androidx.compose.runtime.mutableStateOf

class PowerConsumptionPreferences(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("power_consumption_preferences", Context.MODE_PRIVATE)
    
    companion object {
        private const val POWER_CONSUMPTION_MULTIPLIER_KEY = "power_consumption_multiplier"
        private const val DEFAULT_MULTIPLIER = 1.0f
    }
    
    fun setMultiplier(multiplier: Float) {
        sharedPreferences.edit().putFloat(POWER_CONSUMPTION_MULTIPLIER_KEY, multiplier).apply()
    }
    
    fun getMultiplier(): Float {
        return sharedPreferences.getFloat(POWER_CONSUMPTION_MULTIPLIER_KEY, DEFAULT_MULTIPLIER)
    }
    
    // Create a mutable state for the multiplier that can be observed
    fun getMultiplierState() = mutableStateOf(getMultiplier())

    suspend fun autoSelectMultiplier(onProgress: (String) -> Unit): Float {
        val powerUtils = PowerUtils(context)

        // 1. Temporarily set multiplier to 1.0 to read "raw" values
        val originalMultiplier = getMultiplier()
        setMultiplier(1.0f)

        try {
            onProgress("Measuring baseline power...")
            var totalPower = 0f
            var validSamples = 0
            val samplesToTake = 10 // 2 seconds / 200ms = 10 samples

            // 2. Monitor for 2 seconds
            for (i in 1..samplesToTake) {
                val powerInfo = powerUtils.getPowerConsumptionInfo()
                
                // Use absolute value as some kernels report negative for discharge
                val p = kotlin.math.abs(powerInfo.power)
                
                // Filter out likely invalid zero readings if possible, but if all are zero we handle it later
                if (p > 0.0001f) {
                    totalPower += p
                    validSamples++
                }
                
                onProgress("Sampling power... ${i * 10}%")
                kotlinx.coroutines.delay(200) // 200ms delay
            }

            val avgRawPower = if (validSamples > 0) totalPower / validSamples else 0f

            onProgress("Analyzing power data...")

            // 3. Check if power is valid (> 0.7 and < 20w)
            // If raw power is 0, we can't calibrate.
            if (avgRawPower < 0.0001f) {
                // Restore original if we failed to read anything
                setMultiplier(originalMultiplier)
                return originalMultiplier
            }

            // Candidates to check. We prefer standard units (1.0, 0.001) but allow user's 0.1/10/etc
            val candidates = listOf(
                100f, 10f, 1.0f, 0.1f, 0.01f, 0.001f, 0.000001f
            )
            
            var bestMultiplier = 1.0f
            
            // Logic: Find multiplier that puts power in [0.7, 3.0] range (Idle range)
            var foundValid = false
            var minDiffFromTarget = Float.MAX_VALUE
            val targetPower = 1.5f // Ideal idle target ~1.5W

            // First pass: Strictly check for 0.7 - 3.0 range
            for (m in candidates) {
                val testPower = avgRawPower * m
                if (testPower >= 0.7f && testPower <= 3.0f) {
                    val diff = kotlin.math.abs(testPower - targetPower)
                    if (diff < minDiffFromTarget) {
                        minDiffFromTarget = diff
                        bestMultiplier = m
                        foundValid = true
                    }
                }
            }

            // Second pass: If strict range failed, look for "acceptable" idle range (0.5 - 5.0)
            if (!foundValid) {
                 for (m in candidates) {
                    val testPower = avgRawPower * m
                    if (testPower >= 0.5f && testPower <= 5.0f) {
                        val diff = kotlin.math.abs(testPower - targetPower)
                        if (diff < minDiffFromTarget) {
                            minDiffFromTarget = diff
                            bestMultiplier = m
                            foundValid = true
                        }
                    }
                }
            }
            
            // Fallback: Just try to get it into a broad sanity range (0.1 - 20)
            if (!foundValid) {
                for (m in candidates) {
                    val testPower = avgRawPower * m
                    if (testPower >= 0.1f && testPower <= 20.0f) {
                         val diff = kotlin.math.abs(testPower - targetPower)
                        if (diff < minDiffFromTarget) {
                            minDiffFromTarget = diff
                            bestMultiplier = m
                            foundValid = true
                        }
                    }
                }
            }

            // Absolute Fallback
            if (!foundValid) {
                if (avgRawPower > 100000f) bestMultiplier = 0.000001f // Microwatts
                else if (avgRawPower > 1000f) bestMultiplier = 0.001f // Milliwatts
                else if (avgRawPower > 20f) bestMultiplier = 0.1f 
                else if (avgRawPower < 0.01f && avgRawPower > 0) bestMultiplier = 100f
                else bestMultiplier = 1.0f
            }

            setMultiplier(bestMultiplier)
            return bestMultiplier

        } catch (e: Exception) {
            e.printStackTrace()
            // Restore on error
            setMultiplier(originalMultiplier)
            return originalMultiplier
        }
    }
}