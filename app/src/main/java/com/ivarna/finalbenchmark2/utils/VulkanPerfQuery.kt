package com.ivarna.finalbenchmark2.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Phase 4: VK_KHR_performance_query enumeration bridge.
 *
 * Calls native to enumerate available GPU hardware performance counters.
 * Returns a structured [CounterInfo] list or empty list if unsupported.
 *
 * This is informational — does not affect benchmark scores.
 */
object VulkanPerfQuery {

    private const val TAG = "VulkanPerfQuery"

    data class CounterInfo(val name: String, val unit: Int)

    /**
     * @return List of available GPU performance counters, or empty if
     *         VK_KHR_performance_query not supported.
     */
    suspend fun getCounters(): List<CounterInfo> = withContext(Dispatchers.IO) {
        try {
            val json = nativeGetCounters()
            val obj = JSONObject(json)
            if (!obj.optBoolean("available", false)) {
                Log.i(TAG, "Performance query unavailable: ${obj.optString("reason")}")
                return@withContext emptyList()
            }
            val arr = obj.optJSONArray("counters") ?: return@withContext emptyList()
            val result = mutableListOf<CounterInfo>()
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                result += CounterInfo(c.getString("name"), c.getInt("unit"))
            }
            Log.i(TAG, "Enumerated ${result.size} GPU perf counters")
            result
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "JNI link error: ${e.message}"); emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "getCounters exception: ${e.message}"); emptyList()
        }
    }

    private external fun nativeGetCounters(): String

    init {
        try { System.loadLibrary("vulkan_native") }
        catch (e: UnsatisfiedLinkError) { Log.e(TAG, "load vulkan_native: ${e.message}") }
    }
}
