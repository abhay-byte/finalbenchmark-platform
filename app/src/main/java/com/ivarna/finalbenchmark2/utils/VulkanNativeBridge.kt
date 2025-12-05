package com.ivarna.finalbenchmark2.utils

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Native bridge for Vulkan information extraction using JNI
 */
class VulkanNativeBridge {
    companion object {
        private const val TAG = "VulkanNativeBridge"
        
        // Load the native library
        private var isLibraryLoaded = false // Add this flag

        init {
            try {
                System.loadLibrary("vulkan_native")
                isLibraryLoaded = true // Mark as success
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load vulkan_native native library", e)
                isLibraryLoaded = false
            }
        }
        
        /**
         * Get Vulkan information from native code
         */
        @JvmStatic
        external fun getVulkanInfoNative(): String
        
        /**
         * Fallback Vulkan information when native library is not available
         */
        private fun getVulkanInfoFallback(): VulkanInfo {
            // Return a basic VulkanInfo with supported = false to indicate lack of detailed info
            return VulkanInfo(
                supported = false, // Assume not supported if native library fails
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
    
    /**
     * Get detailed Vulkan information by calling the native function and parsing the result
     */
    fun getVulkanInfo(): VulkanInfo {
        // 1. Check if library failed to load
        if (!isLibraryLoaded) {
            Log.e(TAG, "Aborting: Native library not loaded")
            return getVulkanInfoFallback() // This causes "Supported: No"
        }

        return try {
            val jsonString = getVulkanInfoNative()
            Log.d(TAG, "Vulkan info JSON: $jsonString")
            
            val jsonObject = JSONObject(jsonString)
            
            // Extract basic properties
            val supported = jsonObject.optBoolean("supported", false)
            
            if (!supported) {
                val error = jsonObject.optString("error", "Unknown error")
                Log.w(TAG, "Vulkan not supported: $error")
                return VulkanInfo(
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
            
            // Extract properties when Vulkan is supported
            val apiVersion: String? = jsonObject.optString("apiVersion", null)
            val driverVersion: String? = jsonObject.optString("driverVersion", null)
            val physicalDeviceName: String? = jsonObject.optString("physicalDeviceName", null)
            val physicalDeviceType: String? = jsonObject.optString("physicalDeviceType", null)
            
            // Extract extensions
            val instanceExtensions = mutableListOf<String>()
            val instanceExtensionsArray = jsonObject.optJSONArray("instanceExtensions")
            if (instanceExtensionsArray != null) {
                for (i in 0 until instanceExtensionsArray.length()) {
                    try {
                        val extension = instanceExtensionsArray.getString(i)
                        if (extension != null) {
                            instanceExtensions.add(extension)
                        }
                    } catch (e: Exception) {
                        // Skip invalid extension entry
                        Log.w(TAG, "Invalid extension at index $i", e)
                    }
                }
            }
            
            // Extract device extensions
            val deviceExtensions = mutableListOf<String>()
            val deviceExtensionsArray = jsonObject.optJSONArray("deviceExtensions")
            if (deviceExtensionsArray != null) {
                for (i in 0 until deviceExtensionsArray.length()) {
                    try {
                        val extension = deviceExtensionsArray.getString(i)
                        if (extension != null) {
                            deviceExtensions.add(extension)
                        }
                    } catch (e: Exception) {
                        // Skip invalid extension entry
                        Log.w(TAG, "Invalid device extension at index $i", e)
                    }
                }
            }
            
            // Extract memory heaps
            val memoryHeaps = mutableListOf<VulkanMemoryHeap>()
            val memoryHeapsArray = jsonObject.optJSONArray("memoryHeaps")
            if (memoryHeapsArray != null) {
                for (i in 0 until memoryHeapsArray.length()) {
                    try {
                        val heapObj = memoryHeapsArray.getJSONObject(i)
                        if (heapObj != null) {
                            memoryHeaps.add(
                                VulkanMemoryHeap(
                                    index = i,
                                    size = heapObj.optLong("size", 0L),
                                    flags = heapObj.optString("flags", "UNKNOWN")
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // Skip invalid heap entry
                        Log.w(TAG, "Invalid memory heap at index $i", e)
                    }
                }
            }
            
            // Extract features
            val featuresObj = jsonObject.optJSONObject("features")
            val features = if (featuresObj != null) {
                VulkanFeatures(
                    robustBufferAccess = featuresObj.optBoolean("robustBufferAccess", false),
                    fullDrawIndexUint32 = featuresObj.optBoolean("fullDrawIndexUint32", false),
                    imageCubeArray = featuresObj.optBoolean("imageCubeArray", false),
                    independentBlend = featuresObj.optBoolean("independentBlend", false),
                    geometryShader = featuresObj.optBoolean("geometryShader", false),
                    tessellationShader = featuresObj.optBoolean("tessellationShader", false),
                    sampleRateShading = featuresObj.optBoolean("sampleRateShading", false),
                    dualSrcBlend = featuresObj.optBoolean("dualSrcBlend", false),
                    logicOp = featuresObj.optBoolean("logicOp", false),
                    multiDrawIndirect = featuresObj.optBoolean("multiDrawIndirect", false),
                    drawIndirectFirstInstance = featuresObj.optBoolean("drawIndirectFirstInstance", false),
                    depthClamp = featuresObj.optBoolean("depthClamp", false),
                    depthBiasClamp = featuresObj.optBoolean("depthBiasClamp", false),
                    fillModeNonSolid = featuresObj.optBoolean("fillModeNonSolid", false),
                    depthBounds = featuresObj.optBoolean("depthBounds", false),
                    wideLines = featuresObj.optBoolean("wideLines", false),
                    largePoints = featuresObj.optBoolean("largePoints", false),
                    alphaToOne = featuresObj.optBoolean("alphaToOne", false),
                    multiViewport = featuresObj.optBoolean("multiViewport", false),
                    samplerAnisotropy = featuresObj.optBoolean("samplerAnisotropy", false),
                    textureCompressionETC2 = featuresObj.optBoolean("textureCompressionETC2", false),
                    textureCompressionASTC_LDR = featuresObj.optBoolean("textureCompressionASTC_LDR", false),
                    textureCompressionBC = featuresObj.optBoolean("textureCompressionBC", false),
                    occlusionQueryPrecise = featuresObj.optBoolean("occlusionQueryPrecise", false),
                    pipelineStatisticsQuery = featuresObj.optBoolean("pipelineStatisticsQuery", false),
                    vertexPipelineStoresAndAtomics = featuresObj.optBoolean("vertexPipelineStoresAndAtomics", false),
                    fragmentStoresAndAtomics = featuresObj.optBoolean("fragmentStoresAndAtomics", false),
                    shaderTessellationAndGeometryPointSize = featuresObj.optBoolean("shaderTessellationAndGeometryPointSize", false),
                    shaderImageGatherExtended = featuresObj.optBoolean("shaderImageGatherExtended", false),
                    shaderStorageImageExtendedFormats = featuresObj.optBoolean("shaderStorageImageExtendedFormats", false),
                    shaderStorageImageMultisample = featuresObj.optBoolean("shaderStorageImageMultisample", false),
                    shaderStorageImageReadWithoutFormat = featuresObj.optBoolean("shaderStorageImageReadWithoutFormat", false),
                    shaderStorageImageWriteWithoutFormat = featuresObj.optBoolean("shaderStorageImageWriteWithoutFormat", false),
                    shaderUniformBufferArrayDynamicIndexing = featuresObj.optBoolean("shaderUniformBufferArrayDynamicIndexing", false),
                    shaderSampledImageArrayDynamicIndexing = featuresObj.optBoolean("shaderSampledImageArrayDynamicIndexing", false),
                    shaderStorageBufferArrayDynamicIndexing = featuresObj.optBoolean("shaderStorageBufferArrayDynamicIndexing", false),
                    shaderStorageImageArrayDynamicIndexing = featuresObj.optBoolean("shaderStorageImageArrayDynamicIndexing", false),
                    shaderClipDistance = featuresObj.optBoolean("shaderClipDistance", false),
                    shaderCullDistance = featuresObj.optBoolean("shaderCullDistance", false),
                    shaderFloat64 = featuresObj.optBoolean("shaderFloat64", false),
                    shaderInt64 = featuresObj.optBoolean("shaderInt64", false),
                    shaderInt16 = featuresObj.optBoolean("shaderInt16", false),
                    shaderResourceResidency = featuresObj.optBoolean("shaderResourceResidency", false),
                    shaderResourceMinLod = featuresObj.optBoolean("shaderResourceMinLod", false),
                    sparseBinding = featuresObj.optBoolean("sparseBinding", false),
                    sparseResidencyBuffer = featuresObj.optBoolean("sparseResidencyBuffer", false),
                    sparseResidencyImage2D = featuresObj.optBoolean("sparseResidencyImage2D", false),
                    sparseResidencyImage3D = featuresObj.optBoolean("sparseResidencyImage3D", false),
                    sparseResidency2Samples = featuresObj.optBoolean("sparseResidency2Samples", false),
                    sparseResidency4Samples = featuresObj.optBoolean("sparseResidency4Samples", false),
                    sparseResidency8Samples = featuresObj.optBoolean("sparseResidency8Samples", false),
                    sparseResidency16Samples = featuresObj.optBoolean("sparseResidency16Samples", false),
                    sparseResidencyAliased = featuresObj.optBoolean("sparseResidencyAliased", false),
                    variableMultisampleRate = featuresObj.optBoolean("variableMultisampleRate", false),
                    inheritedQueries = featuresObj.optBoolean("inheritedQueries", false)
                )
            } else {
                null
            }
            
            VulkanInfo(
                supported = true,
                apiVersion = apiVersion,
                driverVersion = driverVersion,
                physicalDeviceName = physicalDeviceName,
                physicalDeviceType = physicalDeviceType,
                instanceExtensions = instanceExtensions,
                deviceExtensions = deviceExtensions,
                features = features,
                memoryHeaps = memoryHeaps
            )
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library not available, using fallback", e)
            // Return fallback info when native library is not available
            getVulkanInfoFallback()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Vulkan info", e)
            // Return fallback info when there's an error parsing the JSON
            getVulkanInfoFallback()
        }
    }
}