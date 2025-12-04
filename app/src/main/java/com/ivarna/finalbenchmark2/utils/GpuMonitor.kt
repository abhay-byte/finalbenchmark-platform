package com.ivarna.finalbenchmark2.utils

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GpuMonitor {

    // Common paths where Android stores GPU frequency. 
    // Adreno (Qualcomm) usually uses the first two.
    // Mali (MediaTek/Exynos) usually uses the 'devfreq' paths.
    private val POSSIBLE_PATHS = listOf(
        "/sys/class/kgsl/kgsl-3d0/gpuclk",             // Adreno (Standard)
        "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",   // Adreno (Alternative)
        "/sys/class/devfreq/gpufreq/cur_freq",         // Mali/Generic
        "/sys/kernel/gpu/gpu_clock",                   // Some older Kernels
        "/sys/devices/platform/galcore/gpu/gpu0/gpufreq/cur_freq" // Pixel/Tensor specific sometimes
    )

    private var cachedPath: String? = null

    /**
     * Finds the valid path for this specific device.
     * We cache it so we don't search every second.
     */
    suspend fun getValidPath(): String? {
        if (cachedPath != null) return cachedPath

        return withContext(Dispatchers.IO) {
            // Check which file exists and is readable
            for (path in POSSIBLE_PATHS) {
                // We use 'ls' to check existence quickly via root
                val result = Shell.cmd("ls $path").exec()
                if (result.isSuccess) {
                    cachedPath = path
                    return@withContext path
                }
            }
            return@withContext null
        }
    }

    /**
     * Reads the current frequency using Root (cat command)
     */
    suspend fun getCurrentFrequency(): String {
        val path = getValidPath() ?: return "GPU Path Not Found"

        return withContext(Dispatchers.IO) {
            // Run 'cat' command as Root
            val result = Shell.cmd("cat $path").exec()
            
            if (result.isSuccess) {
                // The output is usually in Hz (e.g., 6000000)
                val rawFreq = result.out.joinToString("").trim()
                formatFrequency(rawFreq)
            } else {
                "Error reading"
            }
        }
    }

    // Helper to make the number readable (Hz -> MHz)
    private fun formatFrequency(raw: String): String {
        return try {
            val hertz = raw.toLong()
            "${hertz / 1_000_000} MHz"
        } catch (e: Exception) {
            raw // Return raw string if parsing fails
        }
    }
}