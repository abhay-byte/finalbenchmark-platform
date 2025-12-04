package com.ivarna.finalbenchmark2.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeoutException

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
        private const val TIMEOUT_MS = 2000L // 2 seconds timeout
    }
    
    /**
     * Executes a shell command with root privileges
     * @param command The command to execute
     * @return Output of the command or null if failed
     */
    suspend fun executeCommand(command: String): String? = withContext(Dispatchers.IO) {
        var process: Process? = null
        try {
            Log.d(TAG, "Executing root command: su -c '$command'")
            process = Runtime.getRuntime().exec("su -c '$command'")
            
            // Set up timeout mechanism (compatible with API 24)
            val timeoutThread = Thread {
                try {
                    Thread.sleep(TIMEOUT_MS)
                    if (process != null) {
                        try {
                            // Check if process has exited by trying to get its exit value
                            process.exitValue()
                        } catch (e: Exception) {
                            // Process is still running, terminate it
                            Log.d(TAG, "Command timed out after $TIMEOUT_MS ms, destroying process")
                            process.destroy()
                        }
                    }
                } catch (e: InterruptedException) {
                    // Thread interrupted, do nothing
                }
            }
            
            timeoutThread.start()
            
            val reader = BufferedReader(process.inputStream.reader())
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            timeoutThread.interrupt() // Stop the timeout thread since command completed
            
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                val result = output.toString().trim()
                Log.d(TAG, "Command executed successfully: $command")
                result
            } else {
                Log.e(TAG, "Command failed with exit code $exitCode: $command")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception executing command: $command", e)
            null
        } finally {
            try {
                process?.destroy()
            } catch (e: Exception) {
                // Ignore errors during process destruction
            }
        }
    }
    
    /**
     * Reads content from a file using root privileges
     * @param path The path to the file
     * @return Content of the file or null if failed
     */
    suspend fun readFile(path: String): String? = withContext(Dispatchers.IO) {
        var process: Process? = null
        try {
            Log.d(TAG, "Reading file with root: $path")
            process = Runtime.getRuntime().exec("su -c 'cat \"$path\"'")
            
            // Set up timeout mechanism (compatible with API 24)
            val timeoutThread = Thread {
                try {
                    Thread.sleep(TIMEOUT_MS)
                    if (process != null) {
                        try {
                            // Check if process has exited by trying to get its exit value
                            process.exitValue()
                        } catch (e: Exception) {
                            // Process is still running, terminate it
                            Log.d(TAG, "File read timed out after $TIMEOUT_MS ms, destroying process")
                            process.destroy()
                        }
                    }
                } catch (e: InterruptedException) {
                    // Thread interrupted, do nothing
                }
            }
            
            timeoutThread.start()
            
            val reader = BufferedReader(process.inputStream.reader())
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            timeoutThread.interrupt() // Stop the timeout thread since command completed
            
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                val result = output.toString().trim()
                Log.d(TAG, "File read successfully: $path")
                result
            } else {
                Log.e(TAG, "File read failed with exit code $exitCode: $path")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception reading file: $path", e)
            null
        } finally {
            try {
                process?.destroy()
            } catch (e: Exception) {
                // Ignore errors during process destruction
            }
        }
    }
    
    /**
     * Checks if a file exists using root privileges
     * @param path The path to check
     * @return True if file exists, false otherwise
     */
    suspend fun fileExists(path: String): Boolean = withContext(Dispatchers.IO) {
        var process: Process? = null
        try {
            Log.d(TAG, "Checking file existence with root: $path")
            process = Runtime.getRuntime().exec("su -c '[ -e \"$path\" ] && echo \"exists\" || echo \"not_exists\"'")
            
            // Set up timeout mechanism (compatible with API 24)
            val timeoutThread = Thread {
                try {
                    Thread.sleep(TIMEOUT_MS)
                    if (process != null) {
                        try {
                            // Check if process has exited by trying to get its exit value
                            process.exitValue()
                        } catch (e: Exception) {
                            // Process is still running, terminate it
                            Log.d(TAG, "File exists check timed out after $TIMEOUT_MS ms, destroying process")
                            process.destroy()
                        }
                    }
                } catch (e: InterruptedException) {
                    // Thread interrupted, do nothing
                }
            }
            
            timeoutThread.start()
            
            val reader = BufferedReader(process.inputStream.reader())
            val output = reader.readLine()
            
            timeoutThread.interrupt() // Stop the timeout thread since command completed
            
            val exitCode = process.waitFor()
            if (exitCode == 0 && output?.trim() == "exists") {
                Log.d(TAG, "File exists check for $path: true")
                true
            } else {
                Log.d(TAG, "File exists check for $path: false (exitCode: $exitCode, output: $output)")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception checking file existence: $path", e)
            false
        } finally {
            try {
                process?.destroy()
            } catch (e: Exception) {
                // Ignore errors during process destruction
            }
        }
    }
    
    /**
     * Checks if the device has root access
     * @return True if root access is available, false otherwise
     */
    fun hasRootAccess(): Boolean {
        return RootUtils.requestRootAccess()
    }
    
    /**
     * Lists files in a directory using root privileges
     * @param path The directory path to list
     * @return List of files/directories in the path or null if failed
     */
    suspend fun listDirectory(path: String): List<String>? = withContext(Dispatchers.IO) {
        var process: Process? = null
        try {
            Log.d(TAG, "Listing directory with root: $path")
            process = Runtime.getRuntime().exec("su -c 'ls -1 \"$path\" 2>/dev/null || echo \"error\"'")
            
            // Set up timeout mechanism (compatible with API 24)
            val timeoutThread = Thread {
                try {
                    Thread.sleep(TIMEOUT_MS)
                    if (process != null) {
                        try {
                            // Check if process has exited by trying to get its exit value
                            process.exitValue()
                        } catch (e: Exception) {
                            // Process is still running, terminate it
                            Log.d(TAG, "Directory listing timed out after $TIMEOUT_MS ms, destroying process")
                            process.destroy()
                        }
                    }
                } catch (e: InterruptedException) {
                    // Thread interrupted, do nothing
                }
            }
            
            timeoutThread.start()
            
            val reader = BufferedReader(process.inputStream.reader())
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            timeoutThread.interrupt() // Stop the timeout thread since command completed
            
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                val result = output.toString().trim()
                if (result != "error") {
                    val files = result.split("\n").filter { it.isNotEmpty() }
                    Log.d(TAG, "Directory listing successful for $path, found ${files.size} items")
                    files
                } else {
                    Log.e(TAG, "Directory listing failed: $path")
                    null
                }
            } else {
                Log.e(TAG, "Directory listing failed with exit code $exitCode: $path")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception listing directory: $path", e)
            null
        } finally {
            try {
                process?.destroy()
            } catch (e: Exception) {
                // Ignore errors during process destruction
            }
        }
    }
    
    /**
     * Finds files matching a pattern using root privileges
     * @param pattern The pattern to search for
     * @return List of matching file paths or null if failed
     */
    suspend fun findFiles(pattern: String): List<String>? = withContext(Dispatchers.IO) {
        var process: Process? = null
        try {
            Log.d(TAG, "Finding files with root: $pattern")
            process = Runtime.getRuntime().exec("su -c 'find $pattern 2>/dev/null || echo \"error\"'")
            
            // Set up timeout mechanism (compatible with API 24)
            val timeoutThread = Thread {
                try {
                    Thread.sleep(TIMEOUT_MS)
                    if (process != null) {
                        try {
                            // Check if process has exited by trying to get its exit value
                            process.exitValue()
                        } catch (e: Exception) {
                            // Process is still running, terminate it
                            Log.d(TAG, "File search timed out after $TIMEOUT_MS ms, destroying process")
                            process.destroy()
                        }
                    }
                } catch (e: InterruptedException) {
                    // Thread interrupted, do nothing
                }
            }
            
            timeoutThread.start()
            
            val reader = BufferedReader(process.inputStream.reader())
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            timeoutThread.interrupt() // Stop the timeout thread since command completed
            
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                val result = output.toString().trim()
                if (result != "error") {
                    val files = result.split("\n").filter { it.isNotEmpty() }
                    Log.d(TAG, "File search successful for $pattern, found ${files.size} items")
                    files
                } else {
                    Log.e(TAG, "File search failed: $pattern")
                    null
                }
            } else {
                Log.e(TAG, "File search failed with exit code $exitCode: $pattern")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception finding files: $pattern", e)
            null
        } finally {
            try {
                process?.destroy()
            } catch (e: Exception) {
                // Ignore errors during process destruction
            }
        }
    }
}