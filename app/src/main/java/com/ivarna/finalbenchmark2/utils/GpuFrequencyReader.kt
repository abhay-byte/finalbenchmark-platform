package com.ivarna.finalbenchmark2.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * GPU Frequency Reader for Android
 *
 * Implementation based on analysis of SmartPack-Kernel Manager
 * Repository: https://github.com/SmartPack/SmartPack-Kernel-Manager
 *
 * @author KiloCode
 * @date 2025-12-02
 */
class GpuFrequencyReader {
    companion object {
        private const val TAG = "GpuFrequencyReader"
    }
    
    private val rootExecutor = RootCommandExecutor()
    private val vendorDetector = GpuVendorDetector()
    private val cache = GpuFrequencyCache()
    
    /**
     * Data class to hold GPU frequency information
     */
    data class GpuFrequencyData(
        val currentFrequencyMhz: Long,
        val maxFrequencyMhz: Long?,
        val minFrequencyMhz: Long?,
        val availableFrequencies: List<Long>?,
        val governor: String?,
        val utilizationPercent: Int?,
        val temperatureCelsius: Int?,
        val vendor: GpuVendorDetector.GpuVendor,
        val sourcePath: String,  // Which sysfs path was used
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Sealed class for representing different GPU frequency states
     */
    sealed class GpuFrequencyState {
        data class Available(val data: GpuFrequencyData) : GpuFrequencyState()
        object RequiresRoot : GpuFrequencyState()
        object NotSupported : GpuFrequencyState()
        data class Error(val message: String) : GpuFrequencyState()
    }
    
    /**
     * Reads GPU frequency data
     * @return GpuFrequencyData or null if reading fails
     */
    suspend fun readGpuFrequency(): GpuFrequencyState = withContext(Dispatchers.IO) {
        try {
            // Step 1: Check if root is available
            if (!rootExecutor.hasRootAccess()) {
                Log.w(TAG, "Root access not available, trying fallback methods")
                
                // Try non-root fallback methods
                val fallbackReader = GpuFrequencyFallback()
                val fallbackResult = fallbackReader.readGpuFrequencyWithoutRoot()
                if (fallbackResult != null) {
                    Log.d(TAG, "Successfully read GPU frequency using fallback method")
                    return@withContext fallbackResult
                }
                
                Log.w(TAG, "All fallback methods failed")
                return@withContext GpuFrequencyState.RequiresRoot
            }
            
            // Step 2: Try the common paths first before vendor detection
            val commonFreq = tryCommonPaths()
            if (commonFreq != null) {
                return@withContext commonFreq
            }
            
            // Step 3: Detect GPU vendor if not cached
            var vendor = cache.getCachedVendor()
            if (vendor == null) {
                vendor = vendorDetector.detectVendor()
                if (vendor != GpuVendorDetector.GpuVendor.UNKNOWN) {
                    cache.cacheVendor(vendor)
                }
                Log.d(TAG, "Detected GPU vendor: $vendor")
            } else {
                Log.d(TAG, "Using cached GPU vendor: $vendor")
            }
            
            // Step 4: Get appropriate paths for current frequency
            val currentFreqPaths = GpuPaths.getPathsForVendor(vendor, "current_frequency")
            
            // Step 5: Try each path until one succeeds
            for (path in currentFreqPaths) {
                try {
                    Log.d(TAG, "Trying path: $path for vendor: $vendor")
                    // Handle wildcard paths
                    val resolvedPaths = if (path.contains('*')) {
                        val wildcardResolved = GpuPaths.resolveWildcardPath(path, rootExecutor)
                        Log.d(TAG, "Wildcard path $path resolved to: $wildcardResolved")
                        wildcardResolved
                    } else {
                        listOf(path)
                    }
                    
                    for (resolvedPath in resolvedPaths) {
                        Log.d(TAG, "Checking existence of path: $resolvedPath")
                        if (rootExecutor.fileExists(resolvedPath)) {
                            Log.d(TAG, "File exists: $resolvedPath")
                            // Check if we have cached content for this path
                            var content = cache.getCachedFileContent(resolvedPath)
                            if (content == null) {
                                Log.d(TAG, "Reading file content from: $resolvedPath")
                                content = rootExecutor.readFile(resolvedPath)
                                if (content != null) {
                                    Log.d(TAG, "Successfully read content from $resolvedPath: $content")
                                    cache.cacheFileContent(resolvedPath, content)
                                } else {
                                    Log.d(TAG, "Failed to read content from $resolvedPath")
                                }
                            } else {
                                Log.d(TAG, "Using cached content for $resolvedPath")
                            }
                            
                            if (content != null) {
                                val frequency = parseFrequency(content, vendor)
                                if (frequency > 0) {
                                    Log.d(TAG, "Successfully parsed frequency: $frequency MHz from $resolvedPath")
                                    // Successfully read current frequency, now get additional data
                                    val maxFreq = readMaxFrequency(vendor)
                                    val minFreq = readMinFrequency(vendor)
                                    val availableFreqs = readAvailableFrequencies(vendor)
                                    val governor = readGovernor(vendor)
                                    
                                    val data = GpuFrequencyData(
                                        currentFrequencyMhz = frequency,
                                        maxFrequencyMhz = maxFreq,
                                        minFrequencyMhz = minFreq,
                                        availableFrequencies = availableFreqs,
                                        governor = governor,
                                        utilizationPercent = null, // TODO: Implement utilization reading
                                        temperatureCelsius = null, // TODO: Implement temperature reading
                                        vendor = vendor,
                                        sourcePath = resolvedPath
                                    )
                                    
                                    Log.d(TAG, "Successfully read GPU frequency: ${data.currentFrequencyMhz} MHz from $resolvedPath")
                                    return@withContext GpuFrequencyState.Available(data)
                                } else {
                                    Log.d(TAG, "Parsed frequency was 0 or invalid from $resolvedPath: $content")
                                }
                            }
                        } else {
                            Log.d(TAG, "File does not exist: $resolvedPath")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading from path: $path", e)
                    continue
                }
            }
            
            // Step 6: All paths failed, return error
            Log.e(TAG, "Failed to read GPU frequency from any path for vendor: $vendor")
            GpuFrequencyState.Error("Failed to read GPU frequency from any available path")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error reading GPU frequency", e)
            GpuFrequencyState.Error("Unexpected error: ${e.message}")
        }
    }
    
    /**
     * Try common paths first for faster detection
     */
    private suspend fun tryCommonPaths(): GpuFrequencyState? = withContext(Dispatchers.IO) {
        val commonPaths = listOf(
            "/sys/class/kgsl/kgsl-3d0/gpuclk",             // Adreno (Standard)
            "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",   // Adreno (Alternative)
            "/sys/class/devfreq/gpufreq/cur_freq",         // Mali/Generic
            "/sys/kernel/gpu/gpu_clock",                   // Some older Kernels
            "/sys/devices/platform/galcore/gpu/gpu0/gpufreq/cur_freq" // Pixel/Tensor specific sometimes
        )
        
        for (path in commonPaths) {
            try {
                Log.d(TAG, "Trying common path: $path")
                if (rootExecutor.fileExists(path)) {
                    Log.d(TAG, "File exists: $path")
                    val content = rootExecutor.readFile(path)
                    if (content != null) {
                        val frequency = parseFrequency(content, GpuVendorDetector.GpuVendor.UNKNOWN)
                        if (frequency > 0) {
                            Log.d(TAG, "Successfully parsed frequency: $frequency MHz from common path: $path")
                            
                            val data = GpuFrequencyData(
                                currentFrequencyMhz = frequency,
                                maxFrequencyMhz = null,
                                minFrequencyMhz = null,
                                availableFrequencies = null,
                                governor = null,
                                utilizationPercent = null,
                                temperatureCelsius = null,
                                vendor = GpuVendorDetector.GpuVendor.UNKNOWN,
                                sourcePath = path
                            )
                            
                            return@withContext GpuFrequencyState.Available(data)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading from common path: $path", e)
                continue
            }
        }
        
        null
    }
    
    /**
     * Reads maximum frequency for the current GPU vendor
     */
    private suspend fun readMaxFrequency(vendor: GpuVendorDetector.GpuVendor): Long? {
        // Check if we have cached max frequency
        val cachedMaxFreq = cache.getCachedMaxFreq()
        if (cachedMaxFreq != null) {
            Log.d(TAG, "Using cached max frequency: ${cachedMaxFreq} MHz")
            return cachedMaxFreq
        }
        
        val maxFreqPaths = GpuPaths.getPathsForVendor(vendor, "max_frequency")
        
        for (path in maxFreqPaths) {
            try {
                val resolvedPaths = if (path.contains('*')) {
                    GpuPaths.resolveWildcardPath(path, rootExecutor)
                } else {
                    listOf(path)
                }
                
                for (resolvedPath in resolvedPaths) {
                    if (rootExecutor.fileExists(resolvedPath)) {
                        // Check if we have cached content for this path
                        var content = cache.getCachedFileContent(resolvedPath)
                        if (content == null) {
                            content = rootExecutor.readFile(resolvedPath)
                            if (content != null) {
                                cache.cacheFileContent(resolvedPath, content)
                            }
                        }
                        
                        if (content != null) {
                            val frequency = parseFrequency(content, vendor)
                            if (frequency > 0) {
                                Log.d(TAG, "Max frequency: ${frequency} MHz from $resolvedPath")
                                cache.cacheMaxFreq(frequency)
                                return frequency
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading max frequency from path: $path", e)
                continue
            }
        }
        
        return null
    }
    
    /**
     * Reads minimum frequency for the current GPU vendor
     */
    private suspend fun readMinFrequency(vendor: GpuVendorDetector.GpuVendor): Long? {
        val minFreqPaths = GpuPaths.getPathsForVendor(vendor, "min_frequency")
        
        for (path in minFreqPaths) {
            try {
                val resolvedPaths = if (path.contains('*')) {
                    GpuPaths.resolveWildcardPath(path, rootExecutor)
                } else {
                    listOf(path)
                }
                
                for (resolvedPath in resolvedPaths) {
                    if (rootExecutor.fileExists(resolvedPath)) {
                        val content = rootExecutor.readFile(resolvedPath)
                        if (content != null) {
                            val frequency = parseFrequency(content, vendor)
                            if (frequency > 0) {
                                Log.d(TAG, "Min frequency: ${frequency} MHz from $resolvedPath")
                                return frequency
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading min frequency from path: $path", e)
                continue
            }
        }
        
        return null
    }
    
    /**
     * Reads available frequencies for the current GPU vendor
     */
    private suspend fun readAvailableFrequencies(vendor: GpuVendorDetector.GpuVendor): List<Long>? {
        // Check if we have cached available frequencies
        val cachedAvailableFreqs = cache.getCachedAvailableFreqs()
        if (cachedAvailableFreqs != null) {
            Log.d(TAG, "Using cached available frequencies: ${cachedAvailableFreqs.size} entries")
            return cachedAvailableFreqs
        }
        
        val availableFreqPaths = GpuPaths.getPathsForVendor(vendor, "available_frequencies")
        
        for (path in availableFreqPaths) {
            try {
                val resolvedPaths = if (path.contains('*')) {
                    GpuPaths.resolveWildcardPath(path, rootExecutor)
                } else {
                    listOf(path)
                }
                
                for (resolvedPath in resolvedPaths) {
                    if (rootExecutor.fileExists(resolvedPath)) {
                        // Check if we have cached content for this path
                        var content = cache.getCachedFileContent(resolvedPath)
                        if (content == null) {
                            content = rootExecutor.readFile(resolvedPath)
                            if (content != null) {
                                cache.cacheFileContent(resolvedPath, content)
                            }
                        }
                        
                        if (content != null) {
                            val frequencies = parseAvailableFrequencies(content, vendor)
                            if (frequencies.isNotEmpty()) {
                                Log.d(TAG, "Available frequencies: ${frequencies.size} entries from $resolvedPath")
                                cache.cacheAvailableFreqs(frequencies)
                                return frequencies
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading available frequencies from path: $path", e)
                continue
            }
        }
        
        return null
    }
    
    /**
     * Reads governor for the current GPU vendor
     */
    private suspend fun readGovernor(vendor: GpuVendorDetector.GpuVendor): String? {
        val governorPaths = GpuPaths.getPathsForVendor(vendor, "gpu_info").filter { it.contains("governor") }
        
        for (path in governorPaths) {
            try {
                if (rootExecutor.fileExists(path)) {
                    // Check if we have cached content for this path
                    var content = cache.getCachedFileContent(path)
                    if (content == null) {
                        content = rootExecutor.readFile(path)
                        if (content != null) {
                            cache.cacheFileContent(path, content)
                        }
                    }
                    
                    if (content != null && content.isNotEmpty()) {
                        Log.d(TAG, "Governor: $content from $path")
                        return content.trim()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading governor from path: $path", e)
                continue
            }
        }
        
        return null
    }
    
    /**
     * Parses frequency from raw string content
     * @param rawValue The raw string value from sysfs
     * @param vendor The GPU vendor for context-specific parsing
     * @return Frequency in MHz
     */
    fun parseFrequency(rawValue: String, vendor: GpuVendorDetector.GpuVendor): Long {
        val cleanValue = rawValue.trim()
        
        return when {
            // Most sysfs files return Hz (very large numbers)
            cleanValue.toLongOrNull() != null -> {
                val hz = cleanValue.toLong()
                when {
                    hz > 10_000_000 -> hz / 1_000_000  // Hz → MHz
                    hz > 10_000 -> hz / 1_000           // KHz → MHz
                    else -> hz                          // Already MHz
                }
            }
            
            // Some files have units in the string
            cleanValue.contains("MHz", ignoreCase = true) -> {
                cleanValue.filter { it.isDigit() }.toLongOrNull() ?: 0
            }
            
            cleanValue.contains("KHz", ignoreCase = true) -> {
                val khz = cleanValue.filter { it.isDigit() }.toLongOrNull() ?: 0
                khz / 1000
            }
            
            else -> 0
        }
    }
    
    /**
     * Parses available frequencies from raw string content
     * @param rawValue The raw string value from sysfs
     * @param vendor The GPU vendor for context-specific parsing
     * @return List of frequencies in MHz
     */
    fun parseAvailableFrequencies(rawValue: String, vendor: GpuVendorDetector.GpuVendor): List<Long> {
        val cleanValue = rawValue.trim()
        
        // Split by spaces or commas to get individual frequency values
        val values = cleanValue.split("\\s+".toRegex()).flatMap { it.split(",") }
        
        val frequencies = mutableListOf<Long>()
        for (value in values) {
            val cleanValue = value.trim()
            if (cleanValue.isNotEmpty()) {
                val frequency = parseFrequency(cleanValue, vendor)
                if (frequency > 0) {
                    frequencies.add(frequency)
                }
            }
        }
        
        return frequencies
    }
}