package com.zenithblue.fb2Pro.utils

import android.content.Context
import android.content.pm.PackageManager
import android.opengl.GLES20
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * Utility class for fetching comprehensive GPU information
 */
class GpuInfoUtils(private val context: Context) {
    
    companion object {
        private const val TAG = "GpuInfoUtils"
        
        // OpenGL context attributes
        private val EGL_CONTEXT_CLIENT_VERSION = intArrayOf(2, 0)
        private val EGL_CONFIG_ATTRIBUTES = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 16,
            EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_NONE
        )
    }
    
    /**
     * Fetches comprehensive GPU information
     */
    suspend fun getGpuInfo(): GpuInfoState {
        return try {
            val basicInfo = getGpuBasicInfo()
            val frequencyInfo = getGpuFrequency()
            val openGLInfo = getOpenGLInfo()
            val vulkanInfo = getVulkanInfo()
            
            val gpuInfo = GpuInfo(
                basicInfo = basicInfo,
                frequencyInfo = frequencyInfo,
                openGLInfo = openGLInfo,
                vulkanInfo = vulkanInfo
            )
            
            GpuInfoState.Success(gpuInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching GPU info", e)
            GpuInfoState.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Gets basic GPU information using OpenGL ES
     */
    private fun getGpuBasicInfo(): GpuBasicInfo {
        var name = "Unknown"
        var vendor = "Unknown"
        var driverVersion = "Unknown"
        var openGLVersion = "Unknown"
        var vulkanVersion: String? = null
        
        // Create temporary OpenGL context to query basic info
        val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            val version = IntArray(2)
            if (EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                // Choose EGL config
                val configs = arrayOfNulls<EGLConfig>(1)
                val numConfigs = IntArray(1)
                if (EGL14.eglChooseConfig(
                        eglDisplay,
                        EGL_CONFIG_ATTRIBUTES,
                        0,
                        configs,
                        0,
                        configs.size,
                        numConfigs,
                        0
                    ) && numConfigs[0] > 0
                ) {
                    val eglConfig = configs[0]
                    val eglContext = EGL14.eglCreateContext(
                        eglDisplay,
                        eglConfig,
                        EGL14.EGL_NO_CONTEXT,
                        intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                        0
                    )
                    
                    if (eglContext != EGL14.EGL_NO_CONTEXT) {
                        // Create a dummy surface
                        val surface = EGL14.eglCreatePbufferSurface(
                            eglDisplay,
                            eglConfig!!,
                            intArrayOf(
                                EGL14.EGL_WIDTH, 1,
                                EGL14.EGL_HEIGHT, 1,
                                EGL14.EGL_NONE
                            ),
                            0
                        )
                        
                        if (surface != EGL14.EGL_NO_SURFACE) {
                            // Make context current
                            EGL14.eglMakeCurrent(eglDisplay, surface, surface, eglContext)
                            
                            // Query OpenGL information
                            name = GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown"
                            vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: "Unknown"
                            driverVersion = GLES20.glGetString(GLES20.GL_VERSION) ?: "Unknown"
                            openGLVersion = GLES20.glGetString(GLES20.GL_VERSION) ?: "Unknown"
                            
                            // Clean up
                            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                            EGL14.eglDestroySurface(eglDisplay, surface)
                        }
                        
                        EGL14.eglDestroyContext(eglDisplay, eglContext)
                    }
                }
                EGL14.eglTerminate(eglDisplay)
            }
        }
        
        // Check Vulkan support
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)) {
            // For now, just indicate Vulkan is supported - detailed info requires NDK
            vulkanVersion = "Supported (NDK required for details)"
        } else {
            vulkanVersion = "Not Supported"
        }
        
        return GpuBasicInfo(
            name = name,
            vendor = vendor,
            driverVersion = driverVersion,
            openGLVersion = openGLVersion,
            vulkanVersion = vulkanVersion
        )
    }
    
    /**
     * Gets GPU frequency information from sysfs
     */
    private fun getGpuFrequency(): GpuFrequencyInfo? {
        try {
            val currentFreq = readGpuFrequencyFromPath(getCurrentFrequencyPaths())
            val maxFreq = readGpuFrequencyFromPath(getMaxFrequencyPaths())
            
            return GpuFrequencyInfo(
                currentFrequency = currentFreq,
                maxFrequency = maxFreq
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not read GPU frequency: ${e.message}")
            return null
        }
    }
    
    /**
     * Reads GPU frequency from available paths
     */
    private fun readGpuFrequencyFromPath(paths: List<String>): Long? {
        for (path in paths) {
            try {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val freqStr = file.readText().trim()
                    val freqHz = freqStr.toLongOrNull()
                    if (freqHz != null) {
                        // Convert Hz to MHz
                        return freqHz / 1_000_000
                    }
                }
            } catch (e: IOException) {
                Log.d(TAG, "Could not read frequency from $path: ${e.message}")
                continue
            }
        }
        return null
    }
    
    /**
     * Gets paths for current GPU frequency
     */
    private fun getCurrentFrequencyPaths(): List<String> {
        return listOf(
            // Adreno (Qualcomm)
            "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",
            "/sys/class/kgsl/kgsl-3d0/gpuclk",
            
            // Mali (ARM) - using glob pattern matching
            "/sys/devices/platform/mali/mali/devfreq/mali/cur_freq",
            "/sys/devices/platform/mali/mali/cur_freq",
            "/sys/class/misc/mali0/device/devfreq/cur_freq",
            
            // PowerVR (Imagination)
            "/sys/devices/platform/pvrsrvkm/sgx_dvfs_curr_freq",
            "/sys/class/misc/pvrsrvkm/device/sgx_dvfs_curr_freq",
            
            // NVIDIA Tegra
            "/sys/kernel/debug/clock/gbus/rate",
            "/sys/devices/platform/host1x/57000000.gpu/devfreq/57000000.gpu/cur_freq"
        )
    }
    
    /**
     * Gets paths for maximum GPU frequency
     */
    private fun getMaxFrequencyPaths(): List<String> {
        return listOf(
            // Adreno (Qualcomm)
            "/sys/class/kgsl/kgsl-3d0/devfreq/max_freq",
            
            // Mali (ARM) - using glob pattern matching
            "/sys/devices/platform/mali/mali/devfreq/max_freq",
            "/sys/devices/platform/mali/mali/max_freq",
            "/sys/class/misc/mali0/device/devfreq/max_freq",
            
            // PowerVR (Imagination)
            "/sys/devices/platform/pvrsrvkm/sgx_dvfs_max_freq",
            "/sys/class/misc/pvrsrvkm/device/sgx_dvfs_max_freq",
            
            // NVIDIA Tegra
            "/sys/kernel/debug/clock/gbus/max_rate",
            "/sys/devices/platform/host1x/57000000.gpu/devfreq/57000000.gpu/max_freq"
        )
    }
    
    /**
     * Gets OpenGL information including extensions and capabilities
     */
    private fun getOpenGLInfo(): OpenGLInfo? {
        var version = "Unknown"
        var glslVersion = "Unknown"
        var extensions = emptyList<String>()
        var capabilities = OpenGLCapabilities(
            maxTextureSize = 0,
            maxViewportWidth = 0,
            maxViewportHeight = 0,
            maxFragmentUniformVectors = 0,
            maxVertexAttributes = 0,
            maxRenderbufferSize = 0,
            supportedTextureCompressionFormats = emptyList()
        )
        
        // Create temporary OpenGL context to query info
        val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            val versionArray = IntArray(2)
            if (EGL14.eglInitialize(eglDisplay, versionArray, 0, versionArray, 1)) {
                // Choose EGL config
                val configs = arrayOfNulls<EGLConfig>(1)
                val numConfigs = IntArray(1)
                if (EGL14.eglChooseConfig(
                        eglDisplay,
                        EGL_CONFIG_ATTRIBUTES,
                        0,
                        configs,
                        0,
                        configs.size,
                        numConfigs,
                        0
                    ) && numConfigs[0] > 0
                ) {
                    val eglConfig = configs[0]
                    val eglContext = EGL14.eglCreateContext(
                        eglDisplay,
                        eglConfig,
                        EGL14.EGL_NO_CONTEXT,
                        intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                        0
                    )
                    
                    if (eglContext != EGL14.EGL_NO_CONTEXT) {
                        // Create a dummy surface
                        val surface = EGL14.eglCreatePbufferSurface(
                            eglDisplay,
                            eglConfig!!,
                            intArrayOf(
                                EGL14.EGL_WIDTH, 1,
                                EGL14.EGL_HEIGHT, 1,
                                EGL14.EGL_NONE
                            ),
                            0
                        )
                        
                        if (surface != EGL14.EGL_NO_SURFACE) {
                            // Make context current
                            EGL14.eglMakeCurrent(eglDisplay, surface, surface, eglContext)
                            
                            // Query OpenGL information
                            version = GLES20.glGetString(GLES20.GL_VERSION) ?: "Unknown"
                            glslVersion = GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION) ?: "Unknown"
                            
                            // Get extensions
                            val extensionsStr = GLES20.glGetString(GLES20.GL_EXTENSIONS) ?: ""
                            extensions = extensionsStr.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                            
                            // Query capabilities
                            val intValues = IntArray(1)
                            
                            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, intValues, 0)
                            val maxTextureSize = intValues[0]
                            
                            GLES20.glGetIntegerv(GLES20.GL_MAX_VIEWPORT_DIMS, intValues, 0)
                            val maxViewportWidth = intValues[0]
                            val maxViewportHeight = if (intValues.size > 1) intValues[1] else intValues[0]
                            
                            GLES20.glGetIntegerv(GLES20.GL_MAX_FRAGMENT_UNIFORM_VECTORS, intValues, 0)
                            val maxFragmentUniformVectors = intValues[0]
                            
                            GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, intValues, 0)
                            val maxVertexAttributes = intValues[0]
                            
                            // For renderbuffer size, we'll use max texture size as a proxy
                            val maxRenderbufferSize = maxTextureSize
                            
                            // Determine supported texture compression formats
                            val supportedFormats = mutableListOf<String>()
                            if (extensions.any { it.contains("ETC", ignoreCase = true) }) {
                                supportedFormats.add("ETC2")
                            }
                            if (extensions.any { it.contains("ASTC", ignoreCase = true) }) {
                                supportedFormats.add("ASTC")
                            }
                            if (extensions.any { it.contains("DXT", ignoreCase = true) || it.contains("S3TC", ignoreCase = true) }) {
                                supportedFormats.addAll(listOf("DXT1", "DXT5"))
                            }
                            if (extensions.any { it.contains("PVRTC", ignoreCase = true) }) {
                                supportedFormats.add("PVRTC")
                            }
                            
                            capabilities = OpenGLCapabilities(
                                maxTextureSize = maxTextureSize,
                                maxViewportWidth = maxViewportWidth,
                                maxViewportHeight = maxViewportHeight,
                                maxFragmentUniformVectors = maxFragmentUniformVectors,
                                maxVertexAttributes = maxVertexAttributes,
                                maxRenderbufferSize = maxRenderbufferSize,
                                supportedTextureCompressionFormats = supportedFormats
                            )
                            
                            // Clean up
                            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                            EGL14.eglDestroySurface(eglDisplay, surface)
                        }
                        
                        EGL14.eglDestroyContext(eglDisplay, eglContext)
                    }
                }
                EGL14.eglTerminate(eglDisplay)
            }
        }
        
        return OpenGLInfo(
            version = version,
            glslVersion = glslVersion,
            extensions = extensions,
            capabilities = capabilities
        )
    }
    
    /**
     * Gets Vulkan information using the native bridge
     */
    private fun getVulkanInfo(): VulkanInfo? {
        return try {
            val bridge = VulkanNativeBridge()
            bridge.getVulkanInfo()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Vulkan info from native bridge", e)
            // Fallback to basic implementation
            val isSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
            
            if (isSupported) {
                VulkanInfo(
                    supported = true,
                    apiVersion = "1.0.0", // Default fallback
                    driverVersion = "System Default",
                    physicalDeviceName = "Hardware Device",
                    physicalDeviceType = "Integrated GPU", // Common for mobile
                    instanceExtensions = emptyList(),
                    deviceExtensions = emptyList(),
                    features = null, // Detailed features require NDK
                    memoryHeaps = null // Memory info requires NDK
                )
            } else {
                VulkanInfo(
                    supported = false,
                    apiVersion = null,
                    driverVersion = null,
                    physicalDeviceName = null,
                    physicalDeviceType = null,
                    instanceExtensions = emptyList(),
                    deviceExtensions = emptyList(),
                    features = null,
                    memoryHeaps = null
                )
            }
        }
    }
}