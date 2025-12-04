package com.ivarna.finalbenchmark2.utils

import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * GPU Frequency Reader for Android
 *
 * Implementation based on analysis of SmartPack-Kernel Manager
 * Repository: https://github.com/SmartPack/SmartPack-Kernel-Manager
 *
 * Supported GPU Vendors:
 * - Qualcomm Adreno (Snapdragon SoCs)
 * - ARM Mali (Samsung Exynos, MediaTek, etc.)
 * - Imagination PowerVR
 * - NVIDIA Tegra
 *
 * Root Requirement: YES (Non-root fallback available)
 *
 * Sysfs Paths Used:
 * - Adreno: /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq
 * - Mali: /sys/kernel/gpu/gpu_clock
 * - [Document all paths used]
 *
 * @author KiloCode
 * @date 2025-12-02
 */
class RootCommandExecutor {
    companion object {
        private const val TAG = "RootCommandExecutor"
        
        // Initialize Shell at the class level
        init {
            Shell.enableVerboseLogging = false
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(10)
            )
        }
    }
    
    /**
     * Executes a shell command with root privileges
     * @param command The command to execute
     * @return Output of the command or null if failed
     */
    suspend fun executeCommand(command: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Executing root command: $command")
            val result = Shell.cmd(command).exec()
            
            if (result.isSuccess) {
                val output = result.out.joinToString("\n").trim()
                Log.d(TAG, "Command executed successfully: $command")
                output
            } else {
                Log.e(TAG, "Command failed: $command, stderr: ${result.err.joinToString("\n")}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception executing command: $command", e)
            null
        }
    }
    
    /**
     * Reads content from a file using root privileges
     * @param path The path to the file
     * @return Content of the file or null if failed
     */
    suspend fun readFile(path: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Reading file with root: $path")
            val result = Shell.cmd("cat $path").exec()
            
            if (result.isSuccess) {
                val output = result.out.joinToString("\n").trim()
                Log.d(TAG, "File read successfully: $path")
                output
            } else {
                Log.e(TAG, "File read failed: $path, stderr: ${result.err.joinToString("\n")}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception reading file: $path", e)
            null
        }
    }
    
    /**
     * Checks if a file exists using root privileges
     * @param path The path to check
     * @return True if file exists, false otherwise
     */
    suspend fun fileExists(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking file existence with root: $path")
            val result = Shell.cmd("ls $path").exec()
            
            val exists = result.isSuccess
            Log.d(TAG, "File exists check for $path: $exists")
            exists
        } catch (e: Exception) {
            Log.e(TAG, "Exception checking file existence: $path", e)
            false
        }
    }
    
    /**
     * Checks if the device has root access
     * @return True if root access is available, false otherwise
     */
    fun hasRootAccess(): Boolean {
        return try {
            Shell.getShell().isRoot
        } catch (e: Exception) {
            Log.e(TAG, "Error checking root access", e)
            false
        }
    }
    
    /**
     * Lists files in a directory using root privileges
     * @param path The directory path to list
     * @return List of files/directories in the path or null if failed
     */
    suspend fun listDirectory(path: String): List<String>? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Listing directory with root: $path")
            val result = Shell.cmd("ls -1 $path").exec()
            
            if (result.isSuccess) {
                val output = result.out.filter { it.isNotEmpty() }
                Log.d(TAG, "Directory listing successful for $path, found ${output.size} items")
                output
            } else {
                Log.e(TAG, "Directory listing failed: $path, stderr: ${result.err.joinToString("\n")}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception listing directory: $path", e)
            null
        }
    }
    
    /**
     * Finds files matching a pattern using root privileges
     * @param pattern The pattern to search for
     * @return List of matching file paths or null if failed
     */
    suspend fun findFiles(pattern: String): List<String>? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Finding files with root: $pattern")
            val result = Shell.cmd("find $pattern 2>/dev/null").exec()
            
            if (result.isSuccess) {
                val output = result.out.filter { it.isNotEmpty() }
                Log.d(TAG, "File search successful for $pattern, found ${output.size} items")
                output
            } else {
                Log.e(TAG, "File search failed: $pattern, stderr: ${result.err.joinToString("\n")}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception finding files: $pattern", e)
            null
        }
    }
}