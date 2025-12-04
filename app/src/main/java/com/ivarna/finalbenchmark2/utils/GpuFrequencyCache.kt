package com.ivarna.finalbenchmark2.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * GPU Frequency Cache for Android
 *
 * Implementation based on analysis of SmartPack-Kernel Manager
 * Repository: https://github.com/SmartPack/SmartPack-Kernel-Manager
 *
 * @author KiloCode
 * @date 2025-12-02
 */
class GpuFrequencyCache {
    companion object {
        private const val TAG = "GpuFrequencyCache"
        private const val CACHE_EXPIRY_MS = 5000L // 5 seconds
    }
    
    // Cache static values that don't change frequently
    private val cachedVendor = ConcurrentHashMap<String, Pair<GpuVendorDetector.GpuVendor, Long>>()
    private val cachedSuccessPath = ConcurrentHashMap<String, Pair<String, Long>>()
    private val cachedMaxFreq = ConcurrentHashMap<String, Pair<Long, Long>>()
    private val cachedMinFreq = ConcurrentHashMap<String, Pair<Long, Long>>()
    private val cachedAvailableFreqs = ConcurrentHashMap<String, Pair<List<Long>, Long>>()
    
    // Cache for file content to avoid repeated reads
    private val fileContentCache = ConcurrentHashMap<String, Pair<String, Long>>()
    
    /**
     * Gets cached GPU vendor or null if not cached or expired
     */
    fun getCachedVendor(): GpuVendorDetector.GpuVendor? {
        val cached = cachedVendor["vendor"]
        return if (cached != null && System.currentTimeMillis() - cached.second < CACHE_EXPIRY_MS) {
            cached.first
        } else {
            null
        }
    }
    
    /**
     * Caches the GPU vendor
     */
    fun cacheVendor(vendor: GpuVendorDetector.GpuVendor) {
        cachedVendor["vendor"] = Pair(vendor, System.currentTimeMillis())
    }
    
    /**
     * Gets cached success path or null if not cached or expired
     */
    fun getCachedSuccessPath(): String? {
        val cached = cachedSuccessPath["path"]
        return if (cached != null && System.currentTimeMillis() - cached.second < CACHE_EXPIRY_MS) {
            cached.first
        } else {
            null
        }
    }
    
    /**
     * Caches the success path
     */
    fun cacheSuccessPath(path: String) {
        cachedSuccessPath["path"] = Pair(path, System.currentTimeMillis())
    }
    
    /**
     * Gets cached max frequency or null if not cached or expired
     */
    fun getCachedMaxFreq(): Long? {
        val cached = cachedMaxFreq["max"]
        return if (cached != null && System.currentTimeMillis() - cached.second < CACHE_EXPIRY_MS) {
            cached.first
        } else {
            null
        }
    }
    
    /**
     * Caches the max frequency
     */
    fun cacheMaxFreq(freq: Long) {
        cachedMaxFreq["max"] = Pair(freq, System.currentTimeMillis())
    }
    
    /**
     * Gets cached min frequency or null if not cached or expired
     */
    fun getCachedMinFreq(): Long? {
        val cached = cachedMinFreq["min"]
        return if (cached != null && System.currentTimeMillis() - cached.second < CACHE_EXPIRY_MS) {
            cached.first
        } else {
            null
        }
    }
    
    /**
     * Caches the min frequency
     */
    fun cacheMinFreq(freq: Long) {
        cachedMinFreq["min"] = Pair(freq, System.currentTimeMillis())
    }
    
    /**
     * Gets cached available frequencies or null if not cached or expired
     */
    fun getCachedAvailableFreqs(): List<Long>? {
        val cached = cachedAvailableFreqs["available"]
        return if (cached != null && System.currentTimeMillis() - cached.second < CACHE_EXPIRY_MS) {
            cached.first
        } else {
            null
        }
    }
    
    /**
     * Caches the available frequencies
     */
    fun cacheAvailableFreqs(freqs: List<Long>) {
        cachedAvailableFreqs["available"] = Pair(freqs, System.currentTimeMillis())
    }
    
    /**
     * Gets cached file content or null if not cached or expired
     */
    fun getCachedFileContent(path: String): String? {
        val cached = fileContentCache[path]
        return if (cached != null && System.currentTimeMillis() - cached.second < CACHE_EXPIRY_MS) {
            cached.first
        } else {
            null
        }
    }
    
    /**
     * Caches file content
     */
    fun cacheFileContent(path: String, content: String) {
        fileContentCache[path] = Pair(content, System.currentTimeMillis())
    }
    
    /**
     * Clears all cached values
     */
    fun clearCache() {
        cachedVendor.clear()
        cachedSuccessPath.clear()
        cachedMaxFreq.clear()
        cachedMinFreq.clear()
        cachedAvailableFreqs.clear()
        fileContentCache.clear()
    }
}