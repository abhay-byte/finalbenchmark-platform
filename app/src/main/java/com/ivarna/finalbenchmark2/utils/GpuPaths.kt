package com.ivarna.finalbenchmark2.utils

import com.ivarna.finalbenchmark2.utils.GpuVendorDetector.GpuVendor

/**
 * GPU Path Configuration for Android
 *
 * Implementation based on analysis of SmartPack-Kernel Manager
 * Repository: https://github.com/SmartPack/SmartPack-Kernel-Manager
 */
class GpuPaths {
    companion object {
        // ADRENO GPU (Qualcomm) - PRIMARY PATHS:
        val ADRENO_PATHS = mapOf(
            "current_frequency" to listOf(
                // Current Frequency - Most common paths for Adreno
                "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",       // Primary path
                "/sys/class/kgsl/kgsl-3d0/gpuclk",                 // Alternative
                "/sys/class/kgsl/kgsl-3d0/gpuclk_gpu_hz",          // Hz format
                "/sys/class/kgsl/kgsl-3d0/gpuclk_3d_hz",           // Hz format
                "/sys/class/kgsl/kgsl-3d0/gpubusy",                // Some devices report current freq here
                "/sys/class/kgsl/kgsl-3d0/clock_mhz",              // Some kernels
                "/sys/devices/platform/kgsl-3d0.0/kgsl/kgsl-3d0/devfreq/cur_freq", // Alternative path
                
                // Alternative paths for different kernel versions
                "/sys/class/kgsl/kgsl-3d0/gpu_clock_freq",         // Some OnePlus/Adreno variants
                "/sys/class/kgsl/kgsl-3d0/gpu_current_freq",       // Some Xiaomi/Adreno variants
                "/sys/class/kgsl/kgsl-3d0/gpu_freq",               // Some variants
                "/sys/class/kgsl/kgsl-3d0/gpu_state",              // Some variants
                "/sys/class/kgsl/kgsl-3d0/max_gpuclk",             // May show current in some cases
                "/sys/class/kgsl/kgsl-3d0/min_gpuclk",             // May show current in some cases
            ),
            
            "max_frequency" to listOf(
                // Maximum Frequency - Most common paths for Adreno
                "/sys/class/kgsl/kgsl-3d0/devfreq/max_freq",       // Primary max
                "/sys/class/kgsl/kgsl-3d0/max_gpuclk",             // Alternative max
                "/sys/class/kgsl/kgsl-3d0/gpu_max_clock",          // Hz format
                "/sys/class/kgsl/kgsl-3d0/max_clock_mhz",          // MHz format
                "/sys/class/kgsl/kgsl-3d0/max_gpu_freq",           // Some variants
                "/sys/class/kgsl/kgsl-3d0/max_freq_mhz",           // Some variants
                "/sys/class/kgsl/kgsl-3d0/gpu_max_freq",           // Some variants
            ),
            
            "min_frequency" to listOf(
                // Minimum Frequency - Most common paths for Adreno
                "/sys/class/kgsl/kgsl-3d0/devfreq/min_freq",       // Standard min
                "/sys/class/kgsl/kgsl-3d0/gpu_min_clock",          // Hz format
                "/sys/class/kgsl/kgsl-3d0/min_clock_mhz",          // MHz format
                "/sys/class/kgsl/kgsl-3d0/min_gpu_freq",           // Some variants
                "/sys/class/kgsl/kgsl-3d0/min_freq_mhz",           // Some variants
                "/sys/class/kgsl/kgsl-3d0/gpu_min_freq",           // Some variants
            ),
            
            "available_frequencies" to listOf(
                // Available Frequencies (Table) - Most common paths for Adreno
                "/sys/class/kgsl/kgsl-3d0/devfreq/available_frequencies", // Standard path
                "/sys/class/kgsl/kgsl-3d0/gpu_available_frequencies",     // Alternative
                "/sys/class/kgsl/kgsl-3d0/gpu_available_clocks",          // Hz format
                "/sys/class/kgsl/kgsl-3d0/gpu_freq_table",               // Some variants
                "/sys/class/kgsl/kgsl-3d0/gpu_freq_list",                // Some variants
                "/sys/class/kgsl/kgsl-3d0/possible_freq",                // Some variants
            ),
            
            "gpu_info" to listOf(
                // GPU Power Level and Utilization
                "/sys/class/kgsl/kgsl-3d0/gpubusy",                      // Busy time (use for utilization calc)
                "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",          // Direct percentage
                "/sys/class/kgsl/kgsl-3d0/gpu_state_info",               // State information
                
                // GPU Governor
                "/sys/class/kgsl/kgsl-3d0/devfreq/governor",             // Current governor
                "/sys/class/kgsl/kgsl-3d0/devfreq/available_governors",  // Available governors
                
                // GPU Idle State
                "/sys/class/kgsl/kgsl-3d0/idle_timer",                   // Idle timeout
                "/sys/class/kgsl/kgsl-3d0/force_no_nap",                 // Power management
                
                // Additional Info
                "/sys/class/kgsl/kgsl-3d0/gpu_model",                    // GPU model name
                "/sys/class/kgsl/kgsl-3d0/gpuclk_last_set",              // Last set frequency
                "/sys/class/kgsl/kgsl-3d0/device_name",                  // Device name
                "/sys/class/kgsl/kgsl-3d0/clk",                          // Clock information
                "/sys/class/kgsl/kgsl-3d0/clk_info",                     // Clock information
                
                // Power level info
                "/sys/class/kgsl/kgsl-3d0/default_pwrlevel",             // Default power level
                "/sys/class/kgsl/kgsl-3d0/max_pwrlevel",                 // Max power level
                "/sys/class/kgsl/kgsl-3d0/min_pwrlevel",                 // Min power level
                "/sys/class/kgsl/kgsl-3d0/num_pwrlevels",                // Number of power levels
                
                // Thermal and power management
                "/sys/class/kgsl/kgsl-3d0/thermal_mitigation",           // Thermal mitigation status
                "/sys/class/kgsl/kgsl-3d0/power_policy",                 // Power policy
                "/sys/class/kgsl/kgsl-3d0/force_bus_on",                 // Bus control
                "/sys/class/kgsl/kgsl-3d0/force_clk_on",                 // Clock control
                "/sys/class/kgsl/kgsl-3d0/force_rail_on",                // Rail control
                "/sys/class/kgsl/kgsl-3d0/force_wake",                   // Wake control
            )
        )
        
        // MALI GPU (ARM) - PRIMARY PATHS:
        val MALI_PATHS = mapOf(
            "current_frequency" to listOf(
                // Standard Mali Devfreq Paths
                "/sys/class/misc/mali0/device/devfreq/devfreq*/cur_freq",
                "/sys/devices/platform/*.mali/devfreq/devfreq*/cur_freq",
                "/sys/devices/platform/mali/devfreq/devfreq*/cur_freq",
                "/sys/bus/platform/drivers/mali/*/devfreq/devfreq*/cur_freq",
                
                // Mali Clock Paths (Direct)
                "/sys/devices/platform/*.mali/clock",
                "/sys/devices/1140000.mali/clock",
                "/sys/devices/130000.mali/clock",
                "/sys/devices/platform/soc/*.mali/clock",
                
                // GED_SKI Driver Paths (Custom Mali Driver)
                "/sys/kernel/gpu/gpu_clock",                        // Current freq
                "/sys/kernel/gpu/gpu_min_clock",                    // Min freq
                "/sys/kernel/gpu/gpu_max_clock",                    // Max freq
                "/sys/kernel/gpu/gpu_freq_table",                   // Available freqs
                "/sys/kernel/gpu/gpu_model",                        // GPU model name
                "/sys/kernel/gpu/gpu_load",                         // GPU load %
                "/sys/kernel/gpu/gpu_tmu",                          // Temperature
                "/sys/kernel/gpu/gpu_governor",                     // Governor name
                "/sys/kernel/gpu/gpu_available_governor",
                
                // Mali Power Policy
                "/sys/devices/platform/*.mali/power_policy",
                "/sys/devices/1304000.mali/power_policy",
            ),
            
            "max_frequency" to listOf(
                // Maximum Frequency
                "/sys/class/misc/mali0/device/devfreq/devfreq*/max_freq",
                "/sys/devices/platform/*.mali/devfreq/devfreq*/max_freq",
                "/sys/kernel/gpu/gpu_max_clock",
            ),
            
            "min_frequency" to listOf(
                // Minimum Frequency
                "/sys/class/misc/mali0/device/devfreq/devfreq*/min_freq",
                "/sys/devices/platform/*.mali/devfreq/devfreq*/min_freq",
                "/sys/kernel/gpu/gpu_min_clock",
            ),
            
            "available_frequencies" to listOf(
                // Available Frequencies
                "/sys/class/misc/mali0/device/devfreq/devfreq*/available_frequencies",
                "/sys/devices/platform/*.mali/devfreq/devfreq*/available_frequencies",
                "/sys/kernel/gpu/gpu_freq_table",
            ),
            
            "gpu_info" to listOf(
                // Governor
                "/sys/class/misc/mali0/device/devfreq/devfreq*/governor",
                "/sys/devices/platform/*.mali/devfreq/devfreq*/governor",
                "/sys/kernel/gpu/gpu_governor",
                
                // GPU Utilization
                "/sys/class/misc/mali0/device/utilization",
                "/sys/devices/platform/*.mali/utilization",
                "/sys/kernel/gpu/gpu_load",
                
                // Mali DDK Paths (Older)
                "/sys/devices/platform/mali.0/clock",
                "/sys/devices/ffa30000.gpu/clock",
                "/sys/module/mali/parameters/mali_gpu_clock",
            )
        )
        
        // POWERVR GPU (Imagination) - PRIMARY PATHS:
        val POWERVR_PATHS = mapOf(
            "current_frequency" to listOf(
                "/sys/devices/platform/pvrsrvkm/sgx_clk_freq",
                "/sys/kernel/debug/pvr/sgx_clk_freq_read",
                "/sys/class/misc/pvrsrvkm/device/sgx_clk_freq",
                "/sys/devices/platform/omap/pvrsrvkm/sgx_clk_core",
                "/d/clk/gpu_*_clk/clk_rate",
                "/sys/kernel/gpu/gpu_clock",
            ),
            
            "max_frequency" to listOf(
                "/sys/devices/platform/pvrsrvkm/sgx_max_freq",
            ),
            
            "min_frequency" to listOf(
                "/sys/devices/platform/pvrsrvkm/sgx_min_freq",
            ),
            
            "available_frequencies" to listOf(
                "/sys/devices/platform/pvrsrvkm/sgx_available_freqs",
            ),
            
            "gpu_info" to listOf(
                "/sys/devices/platform/pvrsrvkm/sgx_load",
            )
        )
        
        // TEGRA GPU (NVIDIA) - PRIMARY PATHS:
        val TEGRA_PATHS = mapOf(
            "current_frequency" to listOf(
                "/sys/kernel/debug/clock/gbus/rate",
                "/sys/kernel/debug/clock/gpu_dvfs/rate", 
                "/sys/devices/platform/host1x/gk20a.0/devfreq/gk20a.0/cur_freq",
                "/sys/devices/57000000.gpu/devfreq/57000000.gpu/cur_freq",
                "/d/clk/gpu/clk_rate",
            ),
            
            "max_frequency" to listOf(
                "/sys/kernel/debug/clock/gbus/max_rate",
                "/sys/devices/platform/host1x/gk20a.0/devfreq/gk20a.0/max_freq",
            ),
            
            "min_frequency" to listOf(
                "/sys/kernel/debug/clock/gbus/min_rate",
                "/sys/devices/platform/host1x/gk20a.0/devfreq/gk20a.0/min_freq",
            ),
            
            "available_frequencies" to listOf(
                "/sys/devices/platform/host1x/gk20a.0/devfreq/gk20a.0/available_frequencies",
            ),
            
            "gpu_info" to listOf(
                "/sys/devices/platform/host1x/gk20a.0/devfreq/gk20a.0/governor",
            )
        )
        
        // FALLBACK GENERIC PATHS:
        val GENERIC_PATHS = mapOf(
            "current_frequency" to listOf(
                "/sys/class/devfreq/*/cur_freq",  // Generic devfreq
                "/sys/kernel/gpu/gpu_clock",
                "/sys/class/kgsl/kgsl-3d0/gpuclk",
                "/d/clk/gpu*/clk_rate",
            ),
            
            "max_frequency" to listOf(
                "/sys/class/devfreq/*/max_freq",
                "/sys/class/kgsl/kgsl-3d0/max_gpuclk",
            ),
            
            "min_frequency" to listOf(
                "/sys/class/devfreq/*/min_freq",
                "/sys/class/kgsl/kgsl-3d0/min_gpuclk",
            ),
            
            "available_frequencies" to listOf(
                "/sys/class/devfreq/*/available_frequencies",
                "/sys/class/kgsl/kgsl-3d0/gpu_available_frequencies",
            ),
            
            "gpu_info" to listOf(
                "/sys/class/devfreq/*/governor",
                "/sys/class/kgsl/kgsl-3d0/gpubusy",
            )
        )
        
        /**
         * Gets the appropriate paths for a given GPU vendor and parameter type
         * @param vendor The GPU vendor
         * @param paramType The parameter type (current_frequency, max_frequency, etc.)
         * @return List of paths to try for the given vendor and parameter
         */
        fun getPathsForVendor(vendor: GpuVendorDetector.GpuVendor, paramType: String): List<String> {
            return when (vendor) {
                GpuVendorDetector.GpuVendor.ADRENO -> ADRENO_PATHS[paramType] ?: emptyList()
                GpuVendorDetector.GpuVendor.MALI -> MALI_PATHS[paramType] ?: emptyList()
                GpuVendorDetector.GpuVendor.POWERVR -> POWERVR_PATHS[paramType] ?: emptyList()
                GpuVendorDetector.GpuVendor.TEGRA -> TEGRA_PATHS[paramType] ?: emptyList()
                GpuVendorDetector.GpuVendor.UNKNOWN -> GENERIC_PATHS[paramType] ?: emptyList()
            }
        }
        
        /**
         * Resolves wildcard paths by finding actual paths that match the pattern
         * @param path The path with wildcards
         * @param rootExecutor The root command executor to use for path resolution
         * @return List of resolved paths
         */
        suspend fun resolveWildcardPath(path: String, rootExecutor: RootCommandExecutor): List<String> {
            if (!path.contains('*')) {
                return listOf(path)
            }
            
            // Find the directory part containing the wildcard and the file part
            val lastSlashIndex = path.lastIndexOf('/')
            if (lastSlashIndex == -1) return emptyList()
            
            var dirPart = path.substring(0, lastSlashIndex)
            val filePart = path.substring(lastSlashIndex + 1)
            
            // If the file part has a wildcard, we need to resolve it in the parent directory
            if (filePart.contains('*')) {
                val parentDir = dirPart.substring(0, dirPart.lastIndexOf('/'))
                val dirPattern = dirPart.substring(dirPart.lastIndexOf('/') + 1)
                
                // List the parent directory to find directories matching the pattern
                val parentContents = rootExecutor.listDirectory(parentDir)
                if (parentContents != null) {
                    val matchingEntries = parentContents.filter { entry ->
                        matchesPattern(entry, dirPattern)
                    }
                    
                    // For each matching directory, check if the target file exists
                    val results = mutableListOf<String>()
                    for (entry in matchingEntries) {
                        val testPath = "$parentDir/$entry/$filePart"
                        if (rootExecutor.fileExists(testPath)) {
                            results.add(testPath)
                        }
                    }
                    return results
                }
            }
            // If the directory part has a wildcard, resolve it
            else if (dirPart.contains('*')) {
                val parentDir = dirPart.substring(0, dirPart.lastIndexOf('/'))
                val dirPattern = dirPart.substring(dirPart.lastIndexOf('/') + 1)
                
                // List the parent directory to find entries matching the pattern
                val parentContents = rootExecutor.listDirectory(parentDir)
                if (parentContents != null) {
                    val matchingEntries = parentContents.filter { entry ->
                        matchesPattern(entry, dirPattern)
                    }
                    
                    // For each matching entry, form the complete path
                    return matchingEntries.map { entry ->
                        "$parentDir/$entry/$filePart"
                    }
                }
            }
            
            return emptyList()
        }
        
        /**
         * Checks if a string matches a pattern with wildcards
         * @param str The string to check
         * @param pattern The pattern that may contain wildcards
         * @return True if the string matches the pattern
         */
        private fun matchesPattern(str: String, pattern: String): Boolean {
            if (!pattern.contains('*')) {
                return str == pattern
            }
            
            // Convert wildcard pattern to regex
            val regexPattern = pattern.replace("*", ".*")
            return str.matches(Regex("^$regexPattern$"))
        }
    }
}