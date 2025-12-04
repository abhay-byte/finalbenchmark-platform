package com.ivarna.finalbenchmark2.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException

class RootUtils {
    companion object {
        private const val TAG = "RootUtils"
        
        /**
         * Checks if the device is rooted by looking for common root indicators
         */
        fun isDeviceRooted(): Boolean {
            val result = checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
            Log.d(TAG, "isDeviceRooted() result: $result")
            return result
        }

        /**
         * Checks if root access is actually working by attempting to execute a command with root privileges
         */
        fun canExecuteRootCommand(): Boolean {
            var process: Process? = null
            var exitCode = -1
            var completed = false
            
            return try {
                Log.d(TAG, "Attempting to execute root command: su -c 'id'")
                process = Runtime.getRuntime().exec("su -c 'id'")
                
                // Create a thread to wait for the process
                val waitThread = Thread {
                    try {
                        Thread.sleep(3000) // 3 second timeout
                        if (process != null) {
                            try {
                                // Check if process has exited by trying to get its exit value
                                process.exitValue()
                            } catch (e: Exception) {
                                // Process is still running, terminate it
                                Log.d(TAG, "Root command timed out after 3 seconds")
                                process.destroy()
                            }
                        }
                    } catch (e: InterruptedException) {
                        // Thread interrupted, do nothing
                    }
                }
                
                waitThread.start()
                
                // Wait for the thread to complete with a timeout
                waitThread.join(3000) // 3 second timeout
                
                if (!completed) {
                    // Process didn't complete in time, destroy it
                    Log.d(TAG, "Root command timed out after 3 seconds")
                    process?.destroy()
                    waitThread.interrupt()
                    return false
                }
                
                exitCode = process.waitFor()
                Log.d(TAG, "Root command exit code: $exitCode")
                val result = exitCode == 0
                Log.d(TAG, "canExecuteRootCommand() result: $result")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Error executing root command: ${e.message}", e)
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
         * Checks if root access is working using a more robust approach
         */
        fun canExecuteRootCommandRobust(): Boolean {
            // Try multiple approaches to get root access
            val commands = listOf(
                "su -c 'id'",  // Standard approach
                "su -c 'ls /data'", // Test with a known protected directory
                "su -c 'ls /system'"
            )
            
            for (command in commands) {
                var process: Process? = null
                try {
                    Log.d(TAG, "Attempting robust root command: $command")
                    process = Runtime.getRuntime().exec(command)
                    
                    // Create a thread to wait for the process
                    val waitThread = Thread {
                        try {
                            Thread.sleep(3000) // 3 second timeout
                            if (process != null) {
                                try {
                                    // Check if process has exited by trying to get its exit value
                                    process.exitValue()
                                } catch (e: Exception) {
                                    // Process is still running, terminate it
                                    Log.d(TAG, "Root command timed out after 3 seconds: $command")
                                    process.destroy()
                                }
                            }
                        } catch (e: InterruptedException) {
                            // Thread interrupted, do nothing
                        }
                    }
                    
                    waitThread.start()
                    
                    // Wait for the thread to complete with a timeout
                    waitThread.join(3000) // 3 second timeout
                    
                    val exitCode = process.waitFor()
                    Log.d(TAG, "Root command exit code for '$command': $exitCode")
                    if (exitCode == 0) {
                        Log.d(TAG, "Robust root access confirmed with command: $command")
                        return true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in robust root command execution: ${e.message}", e)
                    continue
                } finally {
                    try {
                        process?.destroy()
                    } catch (e: Exception) {
                        // Ignore errors during process destruction
                    }
                }
            }
            
            Log.d(TAG, "All robust root commands failed")
            return false
        }
        
        /**
         * Requests root access and returns whether it was granted
         */
        fun requestRootAccess(): Boolean {
            return canExecuteRootCommandRobust()
        }

        /**
         * Check if the device is rooted by looking for the 'su' binary
         */
        private fun checkRootMethod1(): Boolean {
            val paths = arrayOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su"
            )
            
            for (path in paths) {
                val file = File(path)
                val exists = file.exists()
                if (exists) {
                    Log.d(TAG, "Found root indicator at: $path")
                }
            }
            return paths.any { File(it).exists() }
        }

        /**
         * Check if the device is rooted by trying to execute 'su' command
         */
        private fun checkRootMethod2(): Boolean {
            var process: Process? = null
            var exitCode = -1
            var completed = false
            
            return try {
                Log.d(TAG, "Testing root command: su -c 'echo root_test'")
                process = Runtime.getRuntime().exec("su -c 'echo root_test'")
                
                // Create a thread to wait for the process
                val waitThread = Thread {
                    try {
                        exitCode = process.waitFor()
                        completed = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error waiting for process: ${e.message}", e)
                    }
                }
                
                waitThread.start()
                
                // Wait for the thread to complete with a timeout
                waitThread.join(3000) // 3 second timeout
                
                if (!completed) {
                    // Process didn't complete in time, destroy it
                    Log.d(TAG, "Root test command timed out after 3000 ms")
                    process.destroy()
                    waitThread.interrupt()
                    return false
                }
                
                Log.d(TAG, "Root test command exit code: $exitCode")
                val result = exitCode == 0
                Log.d(TAG, "checkRootMethod2() result: $result")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Error in checkRootMethod2: ${e.message}", e)
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
         * Check for the existence of common root management apps
         */
        private fun checkRootMethod3(): Boolean {
            val packages = arrayOf(
                "com.noshufou.android.su",
                "com.noshufou.android.su.elite",
                "eu.chainfire.supersu",
                "com.koushikdutta.superuser",
                "com.thirdparty.superuser",
                "com.yellowes.su"
            )
            
            // We can't check for installed packages here without context,
            // but we can still look for the su binary in PATH
            val path = System.getenv("PATH")
            if (!path.isNullOrEmpty()) {
                val paths = path.split(":")
                for (pathDir in paths) {
                    val suBinary = File("$pathDir/su")
                    if (suBinary.exists()) {
                        Log.d(TAG, "Found su binary in PATH: $pathDir/su")
                        return true
                    }
                }
            }
            return false
        }
    }
}