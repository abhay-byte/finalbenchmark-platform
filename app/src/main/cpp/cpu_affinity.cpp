#include <jni.h>
#include <sched.h>
#include <unistd.h>
#include <android/log.h>
#include <errno.h>
#include <string.h>

#define LOG_TAG "NativeCpuAffinity"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

/**
 * Sets CPU affinity for the current thread to a specific core
 * 
 * @param env JNI environment
 * @param obj Java object (unused)
 * @param coreId The CPU core ID to pin to (0-based)
 * @return true if successful, false otherwise
 */
JNIEXPORT jboolean JNICALL
Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuAffinityManager_nativeSetCpuAffinity(
        JNIEnv* env,
        jobject obj,
        jint coreId) {
    
    cpu_set_t cpuset;
    CPU_ZERO(&cpuset);              // Clear the CPU set
    CPU_SET(coreId, &cpuset);       // Add the specified core to the set
    
    pid_t tid = gettid();           // Get current thread ID
    
    // Set CPU affinity for the current thread
    int result = sched_setaffinity(tid, sizeof(cpu_set_t), &cpuset);
    
    if (result == 0) {
        LOGI("Successfully pinned thread %d to CPU core %d", tid, coreId);
        return JNI_TRUE;
    } else {
        LOGE("Failed to set CPU affinity for thread %d to core %d: %s (errno=%d)", 
             tid, coreId, strerror(errno), errno);
        return JNI_FALSE;
    }
}

/**
 * Resets CPU affinity to allow the thread to run on all cores
 * 
 * @param env JNI environment
 * @param obj Java object (unused)
 * @return true if successful, false otherwise
 */
JNIEXPORT jboolean JNICALL
Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuAffinityManager_nativeResetCpuAffinity(
        JNIEnv* env,
        jobject obj) {
    
    cpu_set_t cpuset;
    CPU_ZERO(&cpuset);
    
    // Add all available CPU cores to the set
    int numCores = sysconf(_SC_NPROCESSORS_CONF);
    for (int i = 0; i < numCores; i++) {
        CPU_SET(i, &cpuset);
    }
    
    pid_t tid = gettid();
    int result = sched_setaffinity(tid, sizeof(cpu_set_t), &cpuset);
    
    if (result == 0) {
        LOGI("Successfully reset CPU affinity for thread %d (all %d cores)", tid, numCores);
        return JNI_TRUE;
    } else {
        LOGE("Failed to reset CPU affinity for thread %d: %s (errno=%d)", 
             tid, strerror(errno), errno);
        return JNI_FALSE;
    }
}

/**
 * Gets the current CPU affinity mask for the thread
 * 
 * @param env JNI environment
 * @param obj Java object (unused)
 * @return Array of CPU core IDs that the thread is allowed to run on
 */
JNIEXPORT jintArray JNICALL
Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuAffinityManager_nativeGetCpuAffinity(
        JNIEnv* env,
        jobject obj) {
    
    cpu_set_t cpuset;
    CPU_ZERO(&cpuset);
    
    pid_t tid = gettid();
    int result = sched_getaffinity(tid, sizeof(cpu_set_t), &cpuset);
    
    if (result != 0) {
        LOGE("Failed to get CPU affinity: %s (errno=%d)", strerror(errno), errno);
        return nullptr;
    }
    
    // Count how many cores are in the affinity mask
    int numCores = sysconf(_SC_NPROCESSORS_CONF);
    int count = 0;
    for (int i = 0; i < numCores; i++) {
        if (CPU_ISSET(i, &cpuset)) {
            count++;
        }
    }
    
    // Create Java int array
    jintArray coreArray = env->NewIntArray(count);
    if (coreArray == nullptr) {
        LOGE("Failed to allocate Java int array");
        return nullptr;
    }
    
    // Fill the array with core IDs
    jint* cores = new jint[count];
    int index = 0;
    for (int i = 0; i < numCores; i++) {
        if (CPU_ISSET(i, &cpuset)) {
            cores[index++] = i;
        }
    }
    
    env->SetIntArrayRegion(coreArray, 0, count, cores);
    delete[] cores;
    
    LOGD("Current CPU affinity for thread %d: %d cores", tid, count);
    
    return coreArray;
}

} // extern "C"
