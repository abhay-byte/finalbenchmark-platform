package com.ivarna.finalbenchmark2.cpuBenchmark

import android.os.Process
import android.util.Log
import java.io.File

/**
 * Manages CPU affinity and thread priority for benchmarks Uses Android APIs to ensure threads run
 * on big cores at max performance
 */
object CpuAffinityManager {
    private const val TAG = "CpuAffinityManager"
    
    // Track if native library is available
    private var nativeLibraryAvailable = false
    
    // Load the native library
    init {
        try {
            System.loadLibrary("vulkan_native")
            nativeLibraryAvailable = true
            Log.i(TAG, "Native CPU affinity library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            nativeLibraryAvailable = false
            Log.w(TAG, "Native CPU affinity library not available, will use fallback method", e)
        }
    }
    
    // Native method declarations
    private external fun nativeSetCpuAffinity(coreId: Int): Boolean
    private external fun nativeResetCpuAffinity(): Boolean
    private external fun nativeGetCpuAffinity(): IntArray?

    enum class CoreType {
        LITTLE,
        MID,
        BIG
    }
    
    data class CpuCore(
            val id: Int,
            val maxFreqKhz: Long,
            val isOnline: Boolean,
            val isBigCore: Boolean,  // Kept for backward compatibility
            val coreType: CoreType
    )

    private var cachedCores: List<CpuCore>? = null

    /** Detect all CPU cores and classify as LITTLE, Mid, or BIG based on frequency */
    fun detectCpuTopology(): List<CpuCore> {
        cachedCores?.let {
            return it
        }

        val cores = mutableListOf<CpuCore>()
        val numCores = Runtime.getRuntime().availableProcessors()
        
        // First pass: collect all frequencies to determine thresholds
        val frequencies = mutableListOf<Long>()
        for (i in 0 until numCores) {
            try {
                val maxFreqPath = "/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq"
                val maxFreq = File(maxFreqPath).readText().trim().toLong()
                frequencies.add(maxFreq)
            } catch (e: Exception) {
                Log.w(TAG, "Could not read CPU$i frequency", e)
            }
        }
        
        // Determine thresholds based on unique frequencies
        val uniqueFreqs = frequencies.distinct().sorted()
        val (littleThreshold, bigThreshold) = when {
            uniqueFreqs.size >= 3 -> {
                // 3+ frequency levels: use dynamic thresholds
                val midPoint = uniqueFreqs[uniqueFreqs.size / 2]
                val highPoint = uniqueFreqs[uniqueFreqs.size * 3 / 4]
                Pair(midPoint, highPoint)
            }
            uniqueFreqs.size == 2 -> {
                // 2 frequency levels: simple big.LITTLE
                Pair(uniqueFreqs[0], uniqueFreqs[1])
            }
            else -> {
                // All cores same frequency: use fixed thresholds
                Pair(2_500_000L, 3_000_000L)  // 2.5 GHz and 3.0 GHz
            }
        }
        
        Log.d(TAG, "CPU frequency thresholds: LITTLE < ${littleThreshold/1000}MHz, MID < ${bigThreshold/1000}MHz, BIG >= ${bigThreshold/1000}MHz")

        // Second pass: classify cores
        for (i in 0 until numCores) {
            try {
                val maxFreqPath = "/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq"
                val maxFreq = File(maxFreqPath).readText().trim().toLong()

                // Classify core type based on frequency
                val coreType = when {
                    maxFreq >= bigThreshold -> CoreType.BIG
                    maxFreq >= littleThreshold -> CoreType.MID
                    else -> CoreType.LITTLE
                }
                
                // Backward compatibility: isBigCore = true for BIG and MID cores
                val isBigCore = coreType != CoreType.LITTLE

                cores.add(
                        CpuCore(
                                id = i,
                                maxFreqKhz = maxFreq,
                                isOnline = true,
                                isBigCore = isBigCore,
                                coreType = coreType
                        )
                )

                Log.d(TAG, "CPU$i: $coreType, Max: ${maxFreq/1000}MHz")
            } catch (e: Exception) {
                Log.w(TAG, "Could not read CPU$i info", e)
            }
        }

        cachedCores = cores
        return cores
    }

    fun getBigCores(): List<Int> {
        return detectCpuTopology().filter { it.coreType == CoreType.BIG }.map { it.id }
    }
    
    fun getMidCores(): List<Int> {
        return detectCpuTopology().filter { it.coreType == CoreType.MID }.map { it.id }
    }

    fun getLittleCores(): List<Int> {
        return detectCpuTopology().filter { it.coreType == CoreType.LITTLE }.map { it.id }
    }

    /**
     * Pin current thread to the last (largest) CPU core In big.LITTLE architectures, the last core
     * is typically the highest-performance core This ensures single-core benchmarks run on the
     * fastest available core
     */
    fun setLastCoreAffinity() {
        try {
            val cores = detectCpuTopology()
            if (cores.isEmpty()) {
                Log.w(TAG, "No CPU cores detected, cannot set affinity")
                return
            }

            // Get the last core (highest core ID) - typically the largest/fastest core
            val lastCore = cores.maxByOrNull { it.id }
            if (lastCore == null) {
                Log.w(TAG, "Could not determine last core")
                return
            }

            // Try to use native CPU affinity if available
            if (nativeLibraryAvailable) {
                try {
                    val success = nativeSetCpuAffinity(lastCore.id)
                    
                    if (success) {
                        Log.d(
                            TAG,
                            "✓ Successfully pinned thread to CPU${lastCore.id} " +
                            "(${lastCore.maxFreqKhz/1000}MHz, ${lastCore.coreType} core)"
                        )
                        
                        // Verify the affinity was set
                        val affinity = nativeGetCpuAffinity()
                        if (affinity != null) {
                            Log.d(TAG, "Current CPU affinity: ${affinity.contentToString()}")
                        }
                        return
                    } else {
                        Log.w(
                            TAG,
                            "Failed to set CPU affinity to core ${lastCore.id}, falling back to thread priority hint"
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Exception calling native CPU affinity, falling back to thread priority hint", e)
                }
            }
            
            // Fallback: Use thread priority hint
            Log.d(
                TAG,
                "Using thread priority hint for CPU${lastCore.id} (${lastCore.maxFreqKhz/1000}MHz, ${lastCore.coreType} core)"
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error setting last core affinity", e)
        }
    }

    /**
     * Set maximum thread priority for benchmark execution This is Android's way of requesting high
     * performance
     */
    fun setMaxPerformance() {
        try {
            // Set thread priority to urgent display (highest non-rt priority)
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
            Log.d(TAG, "✓ Set thread priority to URGENT_DISPLAY")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set thread priority", e)
        }
    }

    /** Reset thread priority to default */
    fun resetPerformance() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
            Log.d(TAG, "Reset thread priority to DEFAULT")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset thread priority", e)
        }
    }
    
    /**
     * Reset CPU affinity to allow thread to run on all cores
     * Should be called after single-core benchmarks complete
     */
    fun resetCpuAffinity() {
        if (!nativeLibraryAvailable) {
            return
        }
        
        try {
            val success = nativeResetCpuAffinity()
            if (success) {
                Log.d(TAG, "✓ Reset CPU affinity to all cores")
            } else {
                Log.w(TAG, "Failed to reset CPU affinity")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception resetting CPU affinity", e)
        }
    }

    /** Log CPU topology for debugging */
    fun logTopology() {
        val cores = detectCpuTopology()
        val bigCores = cores.filter { it.coreType == CoreType.BIG }
        val midCores = cores.filter { it.coreType == CoreType.MID }
        val littleCores = cores.filter { it.coreType == CoreType.LITTLE }

        Log.i(TAG, "=== CPU TOPOLOGY ===")
        Log.i(TAG, "Total cores: ${cores.size}")
        Log.i(TAG, "BIG cores: ${bigCores.size} (IDs: ${bigCores.map { it.id }}, Max: ${bigCores.maxOfOrNull { it.maxFreqKhz/1000 } ?: 0}MHz)")
        Log.i(TAG, "Mid cores: ${midCores.size} (IDs: ${midCores.map { it.id }}, Max: ${midCores.maxOfOrNull { it.maxFreqKhz/1000 } ?: 0}MHz)")
        Log.i(TAG, "LITTLE cores: ${littleCores.size} (IDs: ${littleCores.map { it.id }}, Max: ${littleCores.maxOfOrNull { it.maxFreqKhz/1000 } ?: 0}MHz)")
        Log.i(TAG, "===================")
    }
}
