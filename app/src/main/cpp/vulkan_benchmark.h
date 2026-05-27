#pragma once
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL
Java_com_ivarna_finalbenchmark2_utils_VulkanBenchmarkBridge_nativeInit(JNIEnv *env, jobject obj);

JNIEXPORT jfloat JNICALL
Java_com_ivarna_finalbenchmark2_utils_VulkanBenchmarkBridge_nativeRunScene(JNIEnv *env, jobject obj, jint sceneId);

JNIEXPORT jstring JNICALL
Java_com_ivarna_finalbenchmark2_utils_VulkanBenchmarkBridge_nativeGetGpuName(JNIEnv *env, jobject obj);

JNIEXPORT void JNICALL
Java_com_ivarna_finalbenchmark2_utils_VulkanBenchmarkBridge_nativeDestroy(JNIEnv *env, jobject obj);

#ifdef __cplusplus
}
#endif
