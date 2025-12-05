#include <jni.h>
#include <string>
#include <vulkan/vulkan.h>
#include <sstream>
#include <vector>
#include <android/log.h>
#include <iomanip>
#include <android_native_app_glue.h>

// Macro for logging
#define LOG_TAG "VulkanNative"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Helper: API Version to String
std::string apiVersionToString(uint32_t version) {
    uint32_t major = VK_VERSION_MAJOR(version);
    uint32_t minor = VK_VERSION_MINOR(version);
    uint32_t patch = VK_VERSION_PATCH(version);
    std::stringstream ss;
    ss << major << "." << minor << "." << patch;
    return ss.str();
}

// Helper: Driver Version to String
std::string driverVersionToString(uint32_t version, uint32_t vendorId) {
    uint32_t major = (version >> 22) & 0x3ff;
    uint32_t minor = (version >> 12) & 0x3ff;
    uint32_t patch = version & 0xfff;
    std::stringstream ss;
    ss << major << "." << minor << "." << patch;
    return ss.str();
}

// Helper: Memory Flags to String
std::string memoryHeapFlagsToString(VkMemoryHeapFlags flags) {
    std::vector<std::string> flagStrings;
    if (flags & VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) flagStrings.push_back("DEVICE_LOCAL");
    if (flags & VK_MEMORY_HEAP_MULTI_INSTANCE_BIT) flagStrings.push_back("MULTI_INSTANCE");
    
    std::stringstream ss;
    for (size_t i = 0; i < flagStrings.size(); ++i) {
        if (i > 0) ss << ", ";
        ss << flagStrings[i];
    }
    return ss.str();
}

// Helper: Device Type to String
std::string deviceTypeToString(VkPhysicalDeviceType type) {
    switch (type) {
        case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU: return "Integrated GPU";
        case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU: return "Discrete GPU";
        case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU: return "Virtual GPU";
        case VK_PHYSICAL_DEVICE_TYPE_CPU: return "CPU";
        default: return "Other";
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ivarna_finalbenchmark2_utils_VulkanNativeBridge_getVulkanInfoNative(JNIEnv* env, jclass clazz) {
    // 1. Initialize Vulkan Instance
    VkApplicationInfo appInfo = {};
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName = "FinalBenchmark2";
    appInfo.apiVersion = VK_API_VERSION_1_0;

    VkInstanceCreateInfo createInfo = {};
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    createInfo.pApplicationInfo = &appInfo;
    
    VkInstance instance;
    if (vkCreateInstance(&createInfo, nullptr, &instance) != VK_SUCCESS) {
        return env->NewStringUTF("{\"supported\": false, \"error\": \"Failed to create Vulkan instance\"}");
    }

    // 2. Enumerate Devices
    uint32_t deviceCount = 0;
    vkEnumeratePhysicalDevices(instance, &deviceCount, nullptr);
    if (deviceCount == 0) {
        vkDestroyInstance(instance, nullptr);
        return env->NewStringUTF("{\"supported\": true, \"error\": \"No Vulkan devices found\"}");
    }

    std::vector<VkPhysicalDevice> devices(deviceCount);
    vkEnumeratePhysicalDevices(instance, &deviceCount, devices.data());
    VkPhysicalDevice device = devices[0]; // Use first device

    // 3. Get Properties & Features
    VkPhysicalDeviceProperties deviceProps;
    vkGetPhysicalDeviceProperties(device, &deviceProps);
    
    VkPhysicalDeviceMemoryProperties memoryProps;
    vkGetPhysicalDeviceMemoryProperties(device, &memoryProps);
    
    VkPhysicalDeviceFeatures deviceFeatures;
    vkGetPhysicalDeviceFeatures(device, &deviceFeatures); // Valid for Vulkan 1.0 features only
    
    // Enumerate instance extensions
    uint32_t instanceExtCount = 0;
    vkEnumerateInstanceExtensionProperties(nullptr, &instanceExtCount, nullptr);
    std::vector<VkExtensionProperties> instanceExtensions(instanceExtCount);
    if (instanceExtCount > 0) {
        vkEnumerateInstanceExtensionProperties(nullptr, &instanceExtCount, instanceExtensions.data());
    }
    
    // Enumerate device extensions
    uint32_t deviceExtCount = 0;
    vkEnumerateDeviceExtensionProperties(device, nullptr, &deviceExtCount, nullptr);
    std::vector<VkExtensionProperties> deviceExtensions(deviceExtCount);
    if (deviceExtCount > 0) {
        vkEnumerateDeviceExtensionProperties(device, nullptr, &deviceExtCount, deviceExtensions.data());
    }
    
    // 4. Build JSON
    std::stringstream json;
    json << "{";
    json << "\"supported\": true, ";
    json << "\"apiVersion\": \"" << apiVersionToString(deviceProps.apiVersion) << "\", ";
    json << "\"driverVersion\": \"" << driverVersionToString(deviceProps.driverVersion, deviceProps.vendorID) << "\", ";
    json << "\"physicalDeviceName\": \"" << deviceProps.deviceName << "\", ";
    json << "\"physicalDeviceType\": \"" << deviceTypeToString(deviceProps.deviceType) << "\", ";
    json << "\"vendorId\": " << deviceProps.vendorID << ", ";
    json << "\"deviceId\": " << deviceProps.deviceID << ", ";
    
    // Memory Heaps
    json << "\"memoryHeaps\": [";
    for (uint32_t i = 0; i < memoryProps.memoryHeapCount; ++i) {
        if (i > 0) json << ", ";
        json << "{";
        json << "\"index\": " << i << ", ";
        json << "\"size\": " << memoryProps.memoryHeaps[i].size << ", ";
        json << "\"flags\": \"" << memoryHeapFlagsToString(memoryProps.memoryHeaps[i].flags) << "\"";
        json << "}";
    }
    json << "], ";
    
    // Memory types
    json << "\"memoryTypes\": [";
    for (uint32_t i = 0; i < memoryProps.memoryTypeCount; ++i) {
        if (i > 0) json << ", ";
        json << "{";
        json << "\"index\": " << i << ", ";
        json << "\"heapIndex\": " << memoryProps.memoryTypes[i].heapIndex << ", ";
        json << "\"propertyFlags\": " << memoryProps.memoryTypes[i].propertyFlags;
        json << "}";
    }
    json << "], ";
    
    // Extensions
    json << "\"instanceExtensions\": [";
    for (uint32_t i = 0; i < instanceExtCount; ++i) {
        if (i > 0) json << ", ";
        json << "\"" << instanceExtensions[i].extensionName << "\"";
    }
    json << "], ";
    
    json << "\"deviceExtensions\": [";
    for (uint32_t i = 0; i < deviceExtCount; ++i) {
        if (i > 0) json << ", ";
        json << "\"" << deviceExtensions[i].extensionName << "\"";
    }
    json << "], ";
    
    // Features (ALL Vulkan 1.0 Features)
    json << "\"features\": {";
    json << "\"robustBufferAccess\": " << (deviceFeatures.robustBufferAccess ? "true" : "false") << ", ";
    json << "\"fullDrawIndexUint32\": " << (deviceFeatures.fullDrawIndexUint32 ? "true" : "false") << ", ";
    json << "\"imageCubeArray\": " << (deviceFeatures.imageCubeArray ? "true" : "false") << ", ";
    json << "\"independentBlend\": " << (deviceFeatures.independentBlend ? "true" : "false") << ", ";
    json << "\"geometryShader\": " << (deviceFeatures.geometryShader ? "true" : "false") << ", ";
    json << "\"tessellationShader\": " << (deviceFeatures.tessellationShader ? "true" : "false") << ", ";
    json << "\"sampleRateShading\": " << (deviceFeatures.sampleRateShading ? "true" : "false") << ", ";
    json << "\"dualSrcBlend\": " << (deviceFeatures.dualSrcBlend ? "true" : "false") << ", ";
    json << "\"logicOp\": " << (deviceFeatures.logicOp ? "true" : "false") << ", ";
    json << "\"multiDrawIndirect\": " << (deviceFeatures.multiDrawIndirect ? "true" : "false") << ", ";
    json << "\"drawIndirectFirstInstance\": " << (deviceFeatures.drawIndirectFirstInstance ? "true" : "false") << ", ";
    json << "\"depthClamp\": " << (deviceFeatures.depthClamp ? "true" : "false") << ", ";
    json << "\"depthBiasClamp\": " << (deviceFeatures.depthBiasClamp ? "true" : "false") << ", ";
    json << "\"fillModeNonSolid\": " << (deviceFeatures.fillModeNonSolid ? "true" : "false") << ", ";
    json << "\"depthBounds\": " << (deviceFeatures.depthBounds ? "true" : "false") << ", ";
    json << "\"wideLines\": " << (deviceFeatures.wideLines ? "true" : "false") << ", ";
    json << "\"largePoints\": " << (deviceFeatures.largePoints ? "true" : "false") << ", ";
    json << "\"alphaToOne\": " << (deviceFeatures.alphaToOne ? "true" : "false") << ", ";
    json << "\"multiViewport\": " << (deviceFeatures.multiViewport ? "true" : "false") << ", ";
    json << "\"samplerAnisotropy\": " << (deviceFeatures.samplerAnisotropy ? "true" : "false") << ", ";
    json << "\"textureCompressionETC2\": " << (deviceFeatures.textureCompressionETC2 ? "true" : "false") << ", ";
    json << "\"textureCompressionASTC_LDR\": " << (deviceFeatures.textureCompressionASTC_LDR ? "true" : "false") << ", ";
    json << "\"textureCompressionBC\": " << (deviceFeatures.textureCompressionBC ? "true" : "false") << ", ";
    json << "\"occlusionQueryPrecise\": " << (deviceFeatures.occlusionQueryPrecise ? "true" : "false") << ", ";
    json << "\"pipelineStatisticsQuery\": " << (deviceFeatures.pipelineStatisticsQuery ? "true" : "false") << ", ";
    json << "\"vertexPipelineStoresAndAtomics\": " << (deviceFeatures.vertexPipelineStoresAndAtomics ? "true" : "false") << ", ";
    json << "\"fragmentStoresAndAtomics\": " << (deviceFeatures.fragmentStoresAndAtomics ? "true" : "false") << ", ";
    json << "\"shaderTessellationAndGeometryPointSize\": " << (deviceFeatures.shaderTessellationAndGeometryPointSize ? "true" : "false") << ", ";
    json << "\"shaderImageGatherExtended\": " << (deviceFeatures.shaderImageGatherExtended ? "true" : "false") << ", ";
    json << "\"shaderStorageImageExtendedFormats\": " << (deviceFeatures.shaderStorageImageExtendedFormats ? "true" : "false") << ", ";
    json << "\"shaderStorageImageMultisample\": " << (deviceFeatures.shaderStorageImageMultisample ? "true" : "false") << ", ";
    json << "\"shaderStorageImageReadWithoutFormat\": " << (deviceFeatures.shaderStorageImageReadWithoutFormat ? "true" : "false") << ", ";
    json << "\"shaderStorageImageWriteWithoutFormat\": " << (deviceFeatures.shaderStorageImageWriteWithoutFormat ? "true" : "false") << ", ";
    json << "\"shaderUniformBufferArrayDynamicIndexing\": " << (deviceFeatures.shaderUniformBufferArrayDynamicIndexing ? "true" : "false") << ", ";
    json << "\"shaderSampledImageArrayDynamicIndexing\": " << (deviceFeatures.shaderSampledImageArrayDynamicIndexing ? "true" : "false") << ", ";
    json << "\"shaderStorageBufferArrayDynamicIndexing\": " << (deviceFeatures.shaderStorageBufferArrayDynamicIndexing ? "true" : "false") << ", ";
    json << "\"shaderStorageImageArrayDynamicIndexing\": " << (deviceFeatures.shaderStorageImageArrayDynamicIndexing ? "true" : "false") << ", ";
    json << "\"shaderClipDistance\": " << (deviceFeatures.shaderClipDistance ? "true" : "false") << ", ";
    json << "\"shaderCullDistance\": " << (deviceFeatures.shaderCullDistance ? "true" : "false") << ", ";
    json << "\"shaderFloat64\": " << (deviceFeatures.shaderFloat64 ? "true" : "false") << ", ";
    json << "\"shaderInt64\": " << (deviceFeatures.shaderInt64 ? "true" : "false") << ", ";
    json << "\"shaderInt16\": " << (deviceFeatures.shaderInt16 ? "true" : "false") << ", ";
    json << "\"shaderResourceResidency\": " << (deviceFeatures.shaderResourceResidency ? "true" : "false") << ", ";
    json << "\"shaderResourceMinLod\": " << (deviceFeatures.shaderResourceMinLod ? "true" : "false") << ", ";
    json << "\"sparseBinding\": " << (deviceFeatures.sparseBinding ? "true" : "false") << ", ";
    json << "\"sparseResidencyBuffer\": " << (deviceFeatures.sparseResidencyBuffer ? "true" : "false") << ", ";
    json << "\"sparseResidencyImage2D\": " << (deviceFeatures.sparseResidencyImage2D ? "true" : "false") << ", ";
    json << "\"sparseResidencyImage3D\": " << (deviceFeatures.sparseResidencyImage3D ? "true" : "false") << ", ";
    json << "\"sparseResidency2Samples\": " << (deviceFeatures.sparseResidency2Samples ? "true" : "false") << ", ";
    json << "\"sparseResidency4Samples\": " << (deviceFeatures.sparseResidency4Samples ? "true" : "false") << ", ";
    json << "\"sparseResidency8Samples\": " << (deviceFeatures.sparseResidency8Samples ? "true" : "false") << ", ";
    json << "\"sparseResidency16Samples\": " << (deviceFeatures.sparseResidency16Samples ? "true" : "false") << ", ";
    json << "\"sparseResidencyAliased\": " << (deviceFeatures.sparseResidencyAliased ? "true" : "false") << ", ";
    json << "\"variableMultisampleRate\": " << (deviceFeatures.variableMultisampleRate ? "true" : "false") << ", ";
    json << "\"inheritedQueries\": " << (deviceFeatures.inheritedQueries ? "true" : "false");
    json << "}";
    
    json << "}"; // End root object

    vkDestroyInstance(instance, nullptr);
    return env->NewStringUTF(json.str().c_str());
}