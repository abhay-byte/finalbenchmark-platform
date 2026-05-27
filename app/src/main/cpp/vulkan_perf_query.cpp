/**
 * vulkan_perf_query.cpp — Phase 4: VK_KHR_performance_query hardware counters.
 *
 * Enumerates available GPU performance counters via VK_KHR_performance_query
 * extension. On Adreno 750 / Snapdragon 8 Gen 3, this exposes:
 *   - GPU ALU utilization %
 *   - L1/L2 texture cache hit rate
 *   - Memory bandwidth bytes/cycle
 *
 * Returns JSON string: {"available": true, "counters": [...]}
 * Returns {} if extension not supported.
 *
 * Requires: Phase 2 Vulkan instance already initialized (shares g_instance).
 * NOTE: VK_KHR_performance_query may require special device permissions on
 *       production Android builds; it is listed as informational when
 *       permissions are denied.
 */
#include <jni.h>
#include <vulkan/vulkan.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <sstream>

#define TAG "VulkanPerfQuery"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_ivarna_finalbenchmark2_utils_VulkanPerfQuery_nativeGetCounters(JNIEnv* env, jobject) {
    // Create a temporary instance to enumerate extensions
    VkApplicationInfo appInfo{};
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName = "PerfQuery";
    appInfo.apiVersion = VK_API_VERSION_1_2;

    VkInstanceCreateInfo ici{};
    ici.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    ici.pApplicationInfo = &appInfo;

    VkInstance inst = VK_NULL_HANDLE;
    if (vkCreateInstance(&ici, nullptr, &inst) != VK_SUCCESS) {
        return env->NewStringUTF("{}");
    }

    uint32_t devCount = 0;
    vkEnumeratePhysicalDevices(inst, &devCount, nullptr);
    if (devCount == 0) { vkDestroyInstance(inst, nullptr); return env->NewStringUTF("{}"); }
    std::vector<VkPhysicalDevice> devs(devCount);
    vkEnumeratePhysicalDevices(inst, &devCount, devs.data());
    VkPhysicalDevice physDev = devs[0];

    // Check extension support
    uint32_t extCount = 0;
    vkEnumerateDeviceExtensionProperties(physDev, nullptr, &extCount, nullptr);
    std::vector<VkExtensionProperties> exts(extCount);
    vkEnumerateDeviceExtensionProperties(physDev, nullptr, &extCount, exts.data());

    bool hasPerfQuery = false;
    for (auto& e : exts) {
        if (std::string(e.extensionName) == VK_KHR_PERFORMANCE_QUERY_EXTENSION_NAME) {
            hasPerfQuery = true; break;
        }
    }

    if (!hasPerfQuery) {
        LOGI("VK_KHR_performance_query not supported on this device");
        vkDestroyInstance(inst, nullptr);
        return env->NewStringUTF("{\"available\":false,\"reason\":\"VK_KHR_performance_query not supported\"}");
    }

    // Load extension function
    auto vkEnumeratePhysicalDeviceQueueFamilyPerformanceQueryCountersKHR =
        (PFN_vkEnumeratePhysicalDeviceQueueFamilyPerformanceQueryCountersKHR)
        vkGetInstanceProcAddr(inst, "vkEnumeratePhysicalDeviceQueueFamilyPerformanceQueryCountersKHR");

    if (!vkEnumeratePhysicalDeviceQueueFamilyPerformanceQueryCountersKHR) {
        vkDestroyInstance(inst, nullptr);
        return env->NewStringUTF("{\"available\":true,\"counters\":[]}");
    }

    // Find compute queue family
    uint32_t qfCount = 0;
    vkGetPhysicalDeviceQueueFamilyProperties(physDev, &qfCount, nullptr);
    uint32_t computeQF = 0;

    uint32_t counterCount = 0;
    vkEnumeratePhysicalDeviceQueueFamilyPerformanceQueryCountersKHR(
        physDev, computeQF, &counterCount, nullptr, nullptr);

    std::vector<VkPerformanceCounterKHR> counters(counterCount);
    std::vector<VkPerformanceCounterDescriptionKHR> descs(counterCount);
    for (auto& c : counters) c.sType = VK_STRUCTURE_TYPE_PERFORMANCE_COUNTER_KHR;
    for (auto& d : descs)   d.sType = VK_STRUCTURE_TYPE_PERFORMANCE_COUNTER_DESCRIPTION_KHR;

    vkEnumeratePhysicalDeviceQueueFamilyPerformanceQueryCountersKHR(
        physDev, computeQF, &counterCount, counters.data(), descs.data());

    // Build JSON
    std::ostringstream json;
    json << "{\"available\":true,\"counter_count\":" << counterCount << ",\"counters\":[";
    const int MAX_REPORT = (int)std::min((uint32_t)20, counterCount);
    for (int i = 0; i < MAX_REPORT; i++) {
        if (i > 0) json << ",";
        // Escape name for JSON
        std::string name = descs[i].name;
        for (char& c : name) if (c == '"' || c == '\\') c = '_';
        json << "{\"name\":\"" << name << "\",\"unit\":" << (int)counters[i].unit << "}";
    }
    json << "]}";

    LOGI("VK_KHR_performance_query: %u counters available", counterCount);
    vkDestroyInstance(inst, nullptr);
    return env->NewStringUTF(json.str().c_str());
}

} // extern "C"
