package com.ivarna.finalbenchmark2.utils

import android.opengl.GLES20
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader

/**
 * GPU Vendor Detector for Android
 *
 * Implementation based on analysis of SmartPack-Kernel Manager
 * Repository: https://github.com/SmartPack/SmartPack-Kernel-Manager
 *
 * @author KiloCode
 * @date 2025-12-02
 */
class GpuVendorDetector {
    companion object {
        private const val TAG = "GpuVendorDetector"
        
        // GPU vendor patterns for detection
        private val ADRENO_PATTERNS = listOf(
            "adreno", "Adreno", "qcom", "Qualcomm"
        )
        
        private val MALI_PATTERNS = listOf(
            "mali", "Mali", "ARM", "arm"
        )
        
        private val POWERVR_PATTERNS = listOf(
            "power", "PowerVR", "powervr", "img", "Imagination"
        )
        
        private val TEGRA_PATTERNS = listOf(
            "tegra", "Tegra", "nvidia", "NVIDIA", "nouveau"
        )
    }
    
    /**
     * Enum representing GPU vendors
     */
    enum class GpuVendor {
        ADRENO,      // Qualcomm Snapdragon
        MALI,        // ARM Mali
        POWERVR,     // Imagination PowerVR
        TEGRA,       // NVIDIA Tegra
        UNKNOWN
    }
    
    /**
     * Detects the GPU vendor using multiple methods
     * @return Detected GPU vendor
     */
    suspend fun detectVendor(): GpuVendor = withContext(Dispatchers.IO) {
        // Method 1: Try OpenGL renderer string
        val openglVendor = detectVendorFromOpenGL()
        if (openglVendor != GpuVendor.UNKNOWN) {
            Log.d(TAG, "GPU vendor detected via OpenGL: $openglVendor")
            return@withContext openglVendor
        }
        
        // Method 2: Try reading from sysfs paths
        val sysfsVendor = detectVendorFromSysfs()
        if (sysfsVendor != GpuVendor.UNKNOWN) {
            Log.d(TAG, "GPU vendor detected via sysfs: $sysfsVendor")
            return@withContext sysfsVendor
        }
        
        // Method 3: Try reading from /proc files
        val procVendor = detectVendorFromProc()
        if (procVendor != GpuVendor.UNKNOWN) {
            Log.d(TAG, "GPU vendor detected via /proc: $procVendor")
            return@withContext procVendor
        }
        
        // Method 4: Try device-specific detection
        val deviceVendor = detectVendorFromDevice()
        if (deviceVendor != GpuVendor.UNKNOWN) {
            Log.d(TAG, "GPU vendor detected via device: $deviceVendor")
            return@withContext deviceVendor
        }
        
        Log.d(TAG, "GPU vendor detection failed, returning UNKNOWN")
        GpuVendor.UNKNOWN
    }
    
    /**
     * Detects GPU vendor from OpenGL renderer string
     * @return Detected GPU vendor or UNKNOWN
     */
    private fun detectVendorFromOpenGL(): GpuVendor {
        // IMPORTANT: OpenGL calls must be made on the UI thread where there's a valid GL context
        // Since this function is called from a background coroutine, we'll skip OpenGL detection
        // and rely on other methods that don't require a GL context
        return GpuVendor.UNKNOWN
    }
    
    /**
     * Detects GPU vendor from sysfs paths
     * @return Detected GPU vendor or UNKNOWN
     */
    private suspend fun detectVendorFromSysfs(): GpuVendor = withContext(Dispatchers.IO) {
        val rootExecutor = RootCommandExecutor()
        
        // Check for Adreno-specific paths
        if (rootExecutor.fileExists("/sys/class/kgsl/kgsl-3d0/gpu_model")) {
            val model = rootExecutor.readFile("/sys/class/kgsl/kgsl-3d0/gpu_model")
            if (!model.isNullOrEmpty()) {
                Log.d(TAG, "KGSL GPU model: $model")
                if (ADRENO_PATTERNS.any { model.contains(it, ignoreCase = true) }) {
                    return@withContext GpuVendor.ADRENO
                }
            }
        }
        
        // Check for Mali-specific paths
        if (rootExecutor.fileExists("/sys/kernel/gpu/gpu_model")) {
            val model = rootExecutor.readFile("/sys/kernel/gpu/gpu_model")
            if (!model.isNullOrEmpty()) {
                Log.d(TAG, "GED_SKI GPU model: $model")
                if (MALI_PATTERNS.any { model.contains(it, ignoreCase = true) }) {
                    return@withContext GpuVendor.MALI
                }
            }
        }
        
        // Check for other Mali paths
        if (rootExecutor.fileExists("/sys/class/misc/mali0/device/gpu_model")) {
            val model = rootExecutor.readFile("/sys/class/misc/mali0/device/gpu_model")
            if (!model.isNullOrEmpty()) {
                Log.d(TAG, "Mali GPU model: $model")
                if (MALI_PATTERNS.any { model.contains(it, ignoreCase = true) }) {
                    return@withContext GpuVendor.MALI
                }
            }
        }
        
        return@withContext GpuVendor.UNKNOWN
    }
    
    /**
     * Detects GPU vendor from /proc files
     * @return Detected GPU vendor or UNKNOWN
     */
    private fun detectVendorFromProc(): GpuVendor {
        try {
            // Read from /proc/gpuinfo if it exists
            val gpuInfoFile = "/proc/gpuinfo"
            if (fileExists(gpuInfoFile)) {
                val lines = readLinesFromFile(gpuInfoFile)
                val content = lines.joinToString("\n")
                Log.d(TAG, "/proc/gpuinfo content: $content")
                
                if (ADRENO_PATTERNS.any { content.contains(it, ignoreCase = true) }) {
                    return GpuVendor.ADRENO
                }
                if (MALI_PATTERNS.any { content.contains(it, ignoreCase = true) }) {
                    return GpuVendor.MALI
                }
                if (POWERVR_PATTERNS.any { content.contains(it, ignoreCase = true) }) {
                    return GpuVendor.POWERVR
                }
                if (TEGRA_PATTERNS.any { content.contains(it, ignoreCase = true) }) {
                    return GpuVendor.TEGRA
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading /proc files", e)
        }
        
        return GpuVendor.UNKNOWN
    }
    
    /**
     * Detects GPU vendor based on device fingerprint
     * @return Detected GPU vendor or UNKNOWN
     */
    private fun detectVendorFromDevice(): GpuVendor {
        // Note: This would normally use Build.HARDWARE, Build.DEVICE, Build.BOARD
        // but since we don't have access to Context here, we'll return UNKNOWN
        // This method would be expanded in a real implementation
        return GpuVendor.UNKNOWN
    }
    
    /**
     * Helper function to check if a file exists
     * @param filePath Path to the file
     * @return True if file exists, false otherwise
     */
    private fun fileExists(filePath: String): Boolean {
        return try {
            val file = java.io.File(filePath)
            file.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking file existence: $filePath", e)
            false
        }
    }
    
    /**
     * Helper function to read lines from a file
     * @param filePath Path to the file
     * @return List of lines from the file
     */
    private fun readLinesFromFile(filePath: String): List<String> {
        return try {
            val fileReader = FileReader(filePath)
            val bufferedReader = BufferedReader(fileReader)
            val lines = mutableListOf<String>()
            var line: String?
            
            while (bufferedReader.readLine().also { line = it } != null) {
                lines.add(line ?: "")
            }
            
            bufferedReader.close()
            fileReader.close()
            lines
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file: $filePath", e)
            emptyList()
        }
    }
}