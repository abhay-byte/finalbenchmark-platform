package com.zenithblue.fb2Pro.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Singleton class to manage root access state across the application
 * This prevents repeated root checks and improves performance by caching results
 * 
 * The heavy root check (Runtime.getRuntime().exec() calls) happens only once per app session
 * Subsequent checks during Activity recreation (theme changes) return cached results instantly
 */
object RootAccessManager {
    private var cachedRootState: Boolean? = null
    private val mutex = Mutex()

    /**
     * Returns cached value if available, otherwise performs the heavy check.
     * This is safe to call during Activity recreation and theme changes.
     * 
     * @return true if root access is granted, false otherwise
     */
    suspend fun isRootGranted(): Boolean {
        // Double-checked locking pattern for optimal performance
        val cachedValue = cachedRootState
        if (cachedValue != null) {
            return cachedValue
        }
        
        return mutex.withLock {
            // Check again inside the lock to avoid race conditions
            if (cachedRootState == null) {
                // This is the HEAVY blocking call - run it off the main thread
                val result = withContext(Dispatchers.IO) {
                    RootUtils.canExecuteRootCommandRobust()
                }
                cachedRootState = result
            }
            cachedRootState!!
        }
    }

    /**
     * Forces a re-check by clearing the cache and performing a new root check.
     * Use this only for the "Retry" button functionality in RootCheckScreen.
     * 
     * @return true if root access is granted, false otherwise
     */
    suspend fun forceRefresh(): Boolean {
        return mutex.withLock {
            // Clear cache first
            cachedRootState = null
            
            // Perform new check
            val result = withContext(Dispatchers.IO) {
                RootUtils.canExecuteRootCommandRobust()
            }
            cachedRootState = result
            result
        }
    }
    
    /**
     * Gets the cached root access state without performing a new check.
     * Returns null if no cached result exists.
     * 
     * @return cached Boolean value or null if not checked yet
     */
    fun getCachedRootAccess(): Boolean? {
        return cachedRootState
    }
    
    /**
     * Legacy method for backward compatibility with existing code.
     * Use isRootGranted() for new code.
     */
    suspend fun hasRootAccess(): Boolean {
        return isRootGranted()
    }
    
    /**
     * Resets the cached root access state (for testing purposes)
     */
    fun reset() {
        cachedRootState = null
    }
}