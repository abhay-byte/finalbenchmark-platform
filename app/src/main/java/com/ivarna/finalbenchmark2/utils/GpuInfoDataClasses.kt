package com.ivarna.finalbenchmark2.utils

/**
 * Data classes for GPU information structures
 */

// Sealed class for GPU info loading states
sealed class GpuInfoState {
    object Loading : GpuInfoState()
    data class Success(val gpuInfo: GpuInfo) : GpuInfoState()
    data class Error(val message: String) : GpuInfoState()
}

// Main GPU info container
data class GpuInfo(
    val basicInfo: GpuBasicInfo,
    val frequencyInfo: GpuFrequencyInfo?,
    val openGLInfo: OpenGLInfo?,
    val vulkanInfo: VulkanInfo?
)

// Basic GPU information
data class GpuBasicInfo(
    val name: String,
    val vendor: String,
    val driverVersion: String,
    val openGLVersion: String,
    val vulkanVersion: String? // null if not supported
)

// GPU frequency information
data class GpuFrequencyInfo(
    val currentFrequency: Long?, // in MHz
    val maxFrequency: Long?     // in MHz
)

// OpenGL information
data class OpenGLInfo(
    val version: String,
    val glslVersion: String,
    val extensions: List<String>,
    val capabilities: OpenGLCapabilities
)

// OpenGL capabilities
data class OpenGLCapabilities(
    val maxTextureSize: Int,
    val maxViewportWidth: Int,
    val maxViewportHeight: Int,
    val maxFragmentUniformVectors: Int,
    val maxVertexAttributes: Int,
    val maxRenderbufferSize: Int,
    val supportedTextureCompressionFormats: List<String>
)

// Vulkan information
data class VulkanInfo(
    val supported: Boolean,
    val apiVersion: String?,
    val driverVersion: String?,
    val physicalDeviceName: String?,
    val physicalDeviceType: String?,
    val instanceExtensions: List<String>,
    val deviceExtensions: List<String>,
    val features: VulkanFeatures?,
    val memoryHeaps: List<VulkanMemoryHeap>?
)

// Vulkan features
data class VulkanFeatures(
    val robustBufferAccess: Boolean,
    val fullDrawIndexUint32: Boolean,
    val imageCubeArray: Boolean,
    val independentBlend: Boolean,
    val geometryShader: Boolean,
    val tessellationShader: Boolean,
    val sampleRateShading: Boolean,
    val dualSrcBlend: Boolean,
    val logicOp: Boolean,
    val multiDrawIndirect: Boolean,
    val drawIndirectFirstInstance: Boolean,
    val depthClamp: Boolean,
    val depthBiasClamp: Boolean,
    val fillModeNonSolid: Boolean,
    val depthBounds: Boolean,
    val wideLines: Boolean,
    val largePoints: Boolean,
    val alphaToOne: Boolean,
    val multiViewport: Boolean,
    val samplerAnisotropy: Boolean,
    val textureCompressionETC2: Boolean,
    val textureCompressionASTC_LDR: Boolean,
    val textureCompressionBC: Boolean,
    val occlusionQueryPrecise: Boolean,
    val pipelineStatisticsQuery: Boolean,
    val vertexPipelineStoresAndAtomics: Boolean,
    val fragmentStoresAndAtomics: Boolean,
    val shaderTessellationAndGeometryPointSize: Boolean,
    val shaderImageGatherExtended: Boolean,
    val shaderStorageImageExtendedFormats: Boolean,
    val shaderStorageImageMultisample: Boolean,
    val shaderStorageImageReadWithoutFormat: Boolean,
    val shaderStorageImageWriteWithoutFormat: Boolean,
    val shaderUniformBufferArrayDynamicIndexing: Boolean,
    val shaderSampledImageArrayDynamicIndexing: Boolean,
    val shaderStorageBufferArrayDynamicIndexing: Boolean,
    val shaderStorageImageArrayDynamicIndexing: Boolean,
    val shaderClipDistance: Boolean,
    val shaderCullDistance: Boolean,
    val shaderFloat64: Boolean,
    val shaderInt64: Boolean,
    val shaderInt16: Boolean,
    val shaderResourceResidency: Boolean,
    val shaderResourceMinLod: Boolean,
    val sparseBinding: Boolean,
    val sparseResidencyBuffer: Boolean,
    val sparseResidencyImage2D: Boolean,
    val sparseResidencyImage3D: Boolean,
    val sparseResidency2Samples: Boolean,
    val sparseResidency4Samples: Boolean,
    val sparseResidency8Samples: Boolean,
    val sparseResidency16Samples: Boolean,
    val sparseResidencyAliased: Boolean,
    val variableMultisampleRate: Boolean,
    val inheritedQueries: Boolean
)

// Vulkan memory heap
data class VulkanMemoryHeap(
    val index: Int,
    val size: Long, // in bytes
    val flags: String // "DEVICE_LOCAL", "HOST_VISIBLE", etc.
)

// Extension function to convert VulkanFeatures to sorted list
fun VulkanFeatures.toSortedList(): List<Pair<String, Boolean>> {
    val rawMap = mapOf(
        "Alpha To One" to alphaToOne,
        "Depth Bias Clamp" to depthBiasClamp,
        "Depth Bounds" to depthBounds,
        "Depth Clamp" to depthClamp,
        "Draw Indirect First Instance" to drawIndirectFirstInstance,
        "Dual Src Blend" to dualSrcBlend,
        "Fill Mode Non Solid" to fillModeNonSolid,
        "Fragment Stores And Atomics" to fragmentStoresAndAtomics,
        "Full Draw Index Uint32" to fullDrawIndexUint32,
        "Geometry Shader" to geometryShader,
        "Image Cube Array" to imageCubeArray,
        "Independent Blend" to independentBlend,
        "Inherited Queries" to inheritedQueries,
        "Large Points" to largePoints,
        "Logic Op" to logicOp,
        "Multi Draw Indirect" to multiDrawIndirect,
        "Multi Viewport" to multiViewport,
        "Occlusion Query Precise" to occlusionQueryPrecise,
        "Pipeline Statistics Query" to pipelineStatisticsQuery,
        "Robust Buffer Access" to robustBufferAccess,
        "Sample Rate Shading" to sampleRateShading,
        "Sampler Anisotropy" to samplerAnisotropy,
        "Shader Clip Distance" to shaderClipDistance,
        "Shader Cull Distance" to shaderCullDistance,
        "Shader Float64" to shaderFloat64,
        "Shader Image Gather Extended" to shaderImageGatherExtended,
        "Shader Int16" to shaderInt16,
        "Shader Int64" to shaderInt64,
        "Shader Resource Min Lod" to shaderResourceMinLod,
        "Shader Resource Residency" to shaderResourceResidency,
        "Shader Sampled Image Array Dynamic Indexing" to shaderSampledImageArrayDynamicIndexing,
        "Shader Storage Buffer Array Dynamic Indexing" to shaderStorageBufferArrayDynamicIndexing,
        "Shader Storage Image Array Dynamic Indexing" to shaderStorageImageArrayDynamicIndexing,
        "Shader Storage Image Extended Formats" to shaderStorageImageExtendedFormats,
        "Shader Storage Image Multisample" to shaderStorageImageMultisample,
        "Shader Storage Image Read Without Format" to shaderStorageImageReadWithoutFormat,
        "Shader Storage Image Write Without Format" to shaderStorageImageWriteWithoutFormat,
        "Shader Tessellation And Geometry Point Size" to shaderTessellationAndGeometryPointSize,
        "Shader Uniform Buffer Array Dynamic Indexing" to shaderUniformBufferArrayDynamicIndexing,
        "Sparse Binding" to sparseBinding,
        "Sparse Residency 2 Samples" to sparseResidency2Samples,
        "Sparse Residency 4 Samples" to sparseResidency4Samples,
        "Sparse Residency 8 Samples" to sparseResidency8Samples,
        "Sparse Residency 16 Samples" to sparseResidency16Samples,
        "Sparse Residency Aliased" to sparseResidencyAliased,
        "Sparse Residency Buffer" to sparseResidencyBuffer,
        "Sparse Residency Image2D" to sparseResidencyImage2D,
        "Sparse Residency Image3D" to sparseResidencyImage3D,
        "Tessellation Shader" to tessellationShader,
        "Texture Compression ASTC_LDR" to textureCompressionASTC_LDR,
        "Texture Compression BC" to textureCompressionBC,
        "Texture Compression ETC2" to textureCompressionETC2,
        "Variable Multisample Rate" to variableMultisampleRate,
        "Vertex Pipeline Stores And Atomics" to vertexPipelineStoresAndAtomics,
        "Wide Lines" to wideLines
    )
    return rawMap.toList().sortedBy { it.first }
}