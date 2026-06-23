#include <jni.h>
#include <string>
#include <vector>
#include <sstream>
#include <fstream>
#include <sys/system_properties.h>
#include <sys/auxv.h>
#include <android/log.h>
#ifdef __arm__
#include <asm/hwcap.h>
#elif defined(__aarch64__)
#include <asm/hwcap.h>
#endif

#define LOG_TAG "CpuNative"

// Helper to read a single line from a file
std::string readFile(const std::string& path) {
    std::ifstream file(path);
    std::string content;
    if (file.good()) {
        std::getline(file, content);
    }
    return content;
}

// Helper to get System Property (SoC Name)
std::string getSystemProperty(const char* key) {
    char value[PROP_VALUE_MAX];
    __system_property_get(key, value);
    return std::string(value);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_zenithblue_fb2Pro_utils_CpuNativeBridge_getCpuDetailsNative(JNIEnv* env, jclass clazz) {
    std::stringstream json;
    json << "{";

    // 1. SoC Name
    std::string soc = getSystemProperty("ro.soc.model");
    if (soc.empty()) soc = getSystemProperty("ro.board.platform");
    json << "\"socName\": \"" << (soc.empty() ? "Unknown SoC" : soc) << "\", ";

    // 2. ABI (Passed from Java usually, but we can check pointer size)
    #if defined(__aarch64__)
        json << "\"abi\": \"arm64-v8a\", ";
    #elif defined(__arm__)
        json << "\"abi\": \"armeabi-v7a\", ";
    #elif defined(__x86_64__)
        json << "\"abi\": \"x86_64\", ";
    #else
        json << "\"abi\": \"x86\", ";
    #endif

    // 3. Neon / ASIMD Support
    bool hasNeon = false;
    #if defined(__aarch64__) || defined(__arm__)
        #if defined(__aarch64__)
            unsigned long hwcaps = getauxval(AT_HWCAP);
            if (hwcaps & HWCAP_ASIMD) hasNeon = true;
        #elif defined(__arm__)
            unsigned long hwcaps = getauxval(AT_HWCAP);
            if (hwcaps & HWCAP_NEON) hasNeon = true;
        #endif
    #endif
    json << "\"neon\": " << (hasNeon ? "true" : "false") << ", ";

    // 4. Cache Information (Parsing /sys/devices/system/cpu/cpu0/cache/)
    json << "\"caches\": [";
    
    // Iterate indices usually 0 to 4
    bool firstCache = true;
    for (int i = 0; i < 4; ++i) {
        std::string basePath = "/sys/devices/system/cpu/cpu0/cache/index" + std::to_string(i) + "/";
        std::string size = readFile(basePath + "size");
        
        if (!size.empty()) {
            std::string level = readFile(basePath + "level");
            std::string type = readFile(basePath + "type"); // Data, Instruction, Unified
            
            if (!firstCache) json << ", ";
            json << "{";
            json << "\"level\": \"" << level << "\", ";
            json << "\"type\": \"" << type << "\", ";
            json << "\"size\": \"" << size << "\"";
            json << "}";
            firstCache = false;
        }
    }
    json << "]";

    json << "}";
    return env->NewStringUTF(json.str().c_str());
}