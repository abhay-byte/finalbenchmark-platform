package com.zenithblue.fb2Pro.utils

import org.json.JSONObject

data class CpuCache(val level: String, val type: String, val size: String)
data class CpuDetails(val socName: String, val abi: String, val hasNeon: Boolean, val caches: List<CpuCache>)

class CpuNativeBridge {
    companion object {
        init {
            try {
                System.loadLibrary("vulkan_native") // Must match CMake name
            } catch (e: Exception) { e.printStackTrace() }
        }

        @JvmStatic
        external fun getCpuDetailsNative(): String
    }

    fun getCpuDetails(): CpuDetails {
        return try {
            val json = JSONObject(getCpuDetailsNative())
            val socName = json.optString("socName", "Unknown")
            val abi = json.optString("abi", "Unknown")
            val hasNeon = json.optBoolean("neon", false)
            
            val caches = mutableListOf<CpuCache>()
            val cacheArray = json.optJSONArray("caches")
            if (cacheArray != null) {
                for (i in 0 until cacheArray.length()) {
                    val obj = cacheArray.getJSONObject(i)
                    caches.add(CpuCache(
                        level = obj.optString("level"),
                        type = obj.optString("type"),
                        size = obj.optString("size")
                    ))
                }
            }
            CpuDetails(socName, abi, hasNeon, caches)
        } catch (e: Exception) {
            CpuDetails("Unknown", "Unknown", false, emptyList())
        }
    }
}