package com.ivarna.finalbenchmark2.cpuBenchmark

import android.util.Log
import java.io.File

/**
 * Detects CPU topology on ARM big.LITTLE architectures
 * Identifies performance (big) and efficiency (LITTLE) cores
 */
class CpuTopologyDetector {
    
    data class CpuCore(
        val id: Int,
        val maxFreqKhz: Long,
        val minFreqKhz: Long,
        val currentFreqKhz: Long,
        val isOnline: Boolean,
        val isBigCore: Boolean
    )
    
    companion object {
        private const val TAG = "CpuTopologyDetector"
        private const val CPU_BASE_PATH = "/sys/devices/system/cpu"
        
        // Frequency threshold to distinguish big vs LITTLE cores (in KHz)
        // Cores with max freq > 2.0 GHz are typically "big" cores
        private const val BIG_CORE_THRESHOLD_KHZ = 2000000L
    }
    
    /**
     * Detect all CPU cores and classify them as big or LITTLE
     */
    fun detectCpuTopology(): List<CpuCore> {
        val cores = mutableListOf<CpuCore>()
        
        // Detect number of cores
        val numCores = Runtime.getRuntime().availableProcessors()
        Log.d(TAG, "Detected $numCores CPU cores")
        
        for (i in 0 until numCores) {
            try {
                val cpuPath = File("$CPU_BASE_PATH/cpu$i")
                if (!cpuPath.exists()) continue
                
                // Read online status
                val isOnline = readIntFromFile("$CPU_BASE_PATH/cpu$i/online") == 1
                
                // Read frequency information
                val maxFreq = readLongFromFile("$CPU_BASE_PATH/cpu$i/cpufreq/cpuinfo_max_freq")
                val minFreq = readLongFromFile("$CPU_BASE_PATH/cpu$i/cpufreq/cpuinfo_min_freq")
                val currentFreq = readLongFromFile("$CPU_BASE_PATH/cpu$i/cpufreq/scaling_cur_freq")
                
                // Classify as big or LITTLE core based on max frequency
                val isBigCore = maxFreq > BIG_CORE_THRESHOLD_KHZ
                
                val core = CpuCore(
                    id = i,
                    maxFreqKhz = maxFreq,
                    minFreqKhz = minFreq,
                    currentFreqKhz = currentFreq,
                    isOnline = isOnline,
                    isBigCore = isBigCore
                )
                
                cores.add(core)
                
                Log.d(TAG, "CPU$i: ${if (isBigCore) "BIG" else "LITTLE"}, " +
                        "Max: ${maxFreq/1000}MHz, Current: ${currentFreq/1000}MHz, " +
                        "Online: $isOnline")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reading CPU$i info", e)
            }
        }
        
        return cores
    }
    
    /**
     * Get list of big (performance) core IDs
     */
    fun getBigCoreIds(): List<Int> {
        return detectCpuTopology()
            .filter { it.isBigCore && it.isOnline }
            .map { it.id }
    }
    
    /**
     * Get list of LITTLE (efficiency) core IDs
     */
    fun getLittleCoreIds(): List<Int> {
        return detectCpuTopology()
            .filter { !it.isBigCore && it.isOnline }
            .map { it.id }
    }
    
    /**
     * Read integer value from sysfs file
     */
    private fun readIntFromFile(path: String): Int {
        return try {
            File(path).readText().trim().toInt()
        } catch (e: Exception) {
            // If file doesn't exist or can't be read, assume online
            1
        }
    }
    
    /**
     * Read long value from sysfs file
     */
    private fun readLongFromFile(path: String): Long {
        return try {
            File(path).readText().trim().toLong()
        } catch (e: Exception) {
            Log.w(TAG, "Could not read from $path: ${e.message}")
            0L
        }
    }
    
    /**
     * Log comprehensive CPU topology information
     */
    fun logTopologyInfo() {
        val cores = detectCpuTopology()
        val bigCores = cores.filter { it.isBigCore }
        val littleCores = cores.filter { !it.isBigCore }
        
        Log.i(TAG, "=== CPU TOPOLOGY ===")
        Log.i(TAG, "Total cores: ${cores.size}")
        Log.i(TAG, "Big cores: ${bigCores.size} (IDs: ${bigCores.map { it.id }})")
        Log.i(TAG, "LITTLE cores: ${littleCores.size} (IDs: ${littleCores.map { it.id }})")
        
        // Log individual core details
        cores.forEach { core ->
            Log.i(TAG, "CPU${core.id}: ${if (core.isBigCore) "BIG" else "LITTLE"} | " +
                    "Max: ${core.maxFreqKhz/1000}MHz | " +
                    "Current: ${core.currentFreqKhz/1000}MHz | " +
                    "Online: ${core.isOnline}")
        }
        Log.i(TAG, "===================")
    }
}