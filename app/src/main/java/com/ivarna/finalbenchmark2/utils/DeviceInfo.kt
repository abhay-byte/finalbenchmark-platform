package com.ivarna.finalbenchmark2.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import androidx.annotation.RequiresApi
import java.io.*
import java.util.*

data class DeviceInfo(
    val deviceModel: String,
    val manufacturer: String,
    val board: String,
    val socName: String,
    val cpuArchitecture: String,
    val totalCores: Int,
    val bigCores: Int,
    val smallCores: Int,
    val clusterTopology: String,
    val cpuFrequencies: Map<Int, String>,
    val gpuModel: String,
    val gpuVendor: String,
    val totalRam: Long,
    val availableRam: Long,
    val totalStorage: Long,
    val freeStorage: Long,
    val totalSwap: Long,
    val usedSwap: Long,
    val androidVersion: String,
    val apiLevel: Int,
    val kernelVersion: String,
    val thermalStatus: String?,
    val batteryTemperature: Float?,
    val batteryCapacity: Float?
)

class DeviceInfoCollector {
    companion object {
        fun getDeviceInfo(context: Context): DeviceInfo {
            return DeviceInfo(
                deviceModel = Build.MODEL,
                manufacturer = Build.MANUFACTURER,
                board = Build.BOARD,
                socName = getSocName(),
                cpuArchitecture = Build.SUPPORTED_ABIS[0], // Primary ABI
                totalCores = Runtime.getRuntime().availableProcessors(),
                bigCores = getBigCoresCount(),
                smallCores = getSmallCoresCount(),
                clusterTopology = getClusterTopology(),
                cpuFrequencies = getCpuFrequencies(),
                gpuModel = getGpuModel(),
                gpuVendor = getGpuVendor(),
                totalRam = getTotalRam(context),
                availableRam = getAvailableRam(context),
                totalStorage = getTotalStorage(context),
                freeStorage = getFreeStorage(context),
                totalSwap = getTotalSwap(),
                usedSwap = getUsedSwap(),
                androidVersion = Build.VERSION.RELEASE,
                apiLevel = Build.VERSION.SDK_INT,
                kernelVersion = getKernelVersion(),
                thermalStatus = getThermalStatus(context),
                batteryTemperature = getBatteryTemperature(context),
                batteryCapacity = getBatteryCapacity(context)
            )
        }

        private fun getSocName(): String {
            // Try to get SoC name from system properties
            return try {
                val process = Runtime.getRuntime().exec("getprop ro.board.platform")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val soc = reader.readLine() ?: ""
                if (soc.isNotEmpty()) {
                    soc
                } else {
                    // Fallback to hardware property
                    val hardwareProcess = Runtime.getRuntime().exec("getprop ro.hardware")
                    val hardwareReader = BufferedReader(InputStreamReader(hardwareProcess.inputStream))
                    hardwareReader.readLine() ?: Build.HARDWARE
                }
            } catch (e: Exception) {
                Build.HARDWARE
            }
        }

        private fun getBigCoresCount(): Int {
            // This is a simplified implementation
            // In a real implementation, we would check /sys/devices/system/cpu/cpu*/topology/cluster_id
            // or use more sophisticated core classification
            try {
                val cpuDir = File("/sys/devices/system/cpu/")
                if (cpuDir.exists()) {
                    val cpuFiles = cpuDir.listFiles { file -> 
                        file.name.startsWith("cpu") && file.name.length > 3 && 
                        file.name.substring(3).all { it.isDigit() }
                    }
                    
                    if (cpuFiles != null && cpuFiles.isNotEmpty()) {
                        var bigCores = 0
                        for (cpuFile in cpuFiles) {
                            val cpuNum = cpuFile.name.substring(3).toIntOrNull()
                            if (cpuNum != null && cpuNum >= 0) {
                                val scalingMaxFreqFile = File("/sys/devices/system/cpu/${cpuFile.name}/cpufreq/cpuinfo_max_freq")
                                if (scalingMaxFreqFile.exists()) {
                                    val maxFreq = scalingMaxFreqFile.readText().trim().toLongOrNull() ?: 0
                                    // Consider cores with higher frequencies as big cores (simplified)
                                    if (maxFreq > 1800000) { // 1.8 GHz threshold
                                        bigCores++
                                    }
                                }
                            }
                        }
                        return bigCores
                    }
                }
            } catch (e: Exception) {
                // Fallback to simple calculation if /sys files are not accessible
            }
            
            // Fallback: assume half are big cores (common in ARM big.LITTLE)
            val totalCores = Runtime.getRuntime().availableProcessors()
            return if (totalCores > 2) totalCores / 2 else totalCores
        }

        private fun getSmallCoresCount(): Int {
            val totalCores = Runtime.getRuntime().availableProcessors()
            return totalCores - getBigCoresCount()
        }

        private fun getClusterTopology(): String {
            try {
                val cpuDir = File("/sys/devices/system/cpu/")
                if (cpuDir.exists()) {
                    val cpuFiles = cpuDir.listFiles { file -> 
                        file.name.startsWith("cpu") && file.name.length > 3 && 
                        file.name.substring(3).all { it.isDigit() }
                    }
                    
                    if (cpuFiles != null) {
                        val clusters = mutableMapOf<String, MutableList<Int>>()
                        
                        for (cpuFile in cpuFiles) {
                            val cpuNum = cpuFile.name.substring(3).toIntOrNull()
                            if (cpuNum != null && cpuNum >= 0) {
                                val clusterFile = File("/sys/devices/system/cpu/${cpuFile.name}/topology/cluster_id")
                                if (clusterFile.exists()) {
                                    val clusterId = clusterFile.readText().trim()
                                    if (!clusters.containsKey(clusterId)) {
                                        clusters[clusterId] = mutableListOf()
                                    }
                                    clusters[clusterId]?.add(cpuNum)
                                } else {
                                    // Fallback: try core type
                                    val coreTypeFile = File("/sys/devices/system/cpu/${cpuFile.name}/topology/core_type")
                                    if (coreTypeFile.exists()) {
                                        val coreType = coreTypeFile.readText().trim()
                                        if (!clusters.containsKey(coreType)) {
                                            clusters[coreType] = mutableListOf()
                                        }
                                        clusters[coreType]?.add(cpuNum)
                                    }
                                }
                            }
                        }
                        
                        if (clusters.isNotEmpty()) {
                            val topology = StringBuilder()
                            for ((cluster, cores) in clusters.toSortedMap()) {
                                topology.append("Cluster $cluster: ${cores.size} cores (${cores.joinToString(",")})\n")
                            }
                            return topology.toString().trim()
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to simple topology
            }
            
            // Fallback
            val totalCores = Runtime.getRuntime().availableProcessors()
            return "Cluster 0: $totalCores cores (0-${totalCores - 1})"
        }

        private fun getCpuFrequencies(): Map<Int, String> {
            val frequencies = mutableMapOf<Int, String>()
            val totalCores = Runtime.getRuntime().availableProcessors()
            
            for (i in 0 until totalCores) {
                try {
                    val maxFreqFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                    val minFreqFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_min_freq")
                    
                    var maxFreq = "Unknown"
                    var minFreq = "Unknown"
                    
                    if (maxFreqFile.exists()) {
                        val freqValue = (maxFreqFile.readText().trim().toLongOrNull() ?: 0) / 1000L
                        maxFreq = "${freqValue} MHz"
                    }
                    
                    if (minFreqFile.exists()) {
                        val freqValue = (minFreqFile.readText().trim().toLongOrNull() ?: 0) / 1000L
                        minFreq = "${freqValue} MHz"
                    }
                    
                    frequencies[i] = "$minFreq - $maxFreq"
                } catch (e: Exception) {
                    frequencies[i] = "Unknown"
                }
            }
            
            return frequencies
        }

        private fun getGpuModel(): String {
            // Try to get GPU info from system properties or OpenGL renderer
            return try {
                // This is a simplified approach - in practice, you might need to use OpenGL queries
                val process = Runtime.getRuntime().exec("getprop ro.hardware.graphics")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val gpu = reader.readLine() ?: ""
                if (gpu.isNotEmpty()) {
                    gpu
                } else {
                    // Try another property
                    val process2 = Runtime.getRuntime().exec("getprop ro.opengles.version")
                    val reader2 = BufferedReader(InputStreamReader(process2.inputStream))
                    val glVersion = reader2.readLine() ?: ""
                    when (glVersion) {
                        "19608" -> "OpenGL ES 3.0"
                        "131072" -> "OpenGL ES 2.0"
                        else -> "Unknown GPU"
                    }
                }
            } catch (e: Exception) {
                "Unknown GPU"
            }
        }

        private fun getGpuVendor(): String {
            // Try to get GPU vendor
            return try {
                val process = Runtime.getRuntime().exec("getprop ro.hardware.gpu")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                reader.readLine() ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
        }

        private fun getTotalRam(context: Context): Long {
            try {
                val memInfoFile = File("/proc/meminfo")
                if (memInfoFile.exists()) {
                    val lines = memInfoFile.readLines()
                    for (line in lines) {
                        if (line.startsWith("MemTotal:")) {
                            val parts = line.split("\\s+".toRegex())
                            if (parts.size >= 2) {
                                return (parts[1].toLongOrNull() ?: 0) * 1024 // Convert to bytes
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to ActivityManager
            }
            
            // Fallback using ActivityManager
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            return memInfo.totalMem
        }

        private fun getAvailableRam(context: Context): Long {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            return memInfo.availMem
        }

        private fun getTotalStorage(context: Context): Long {
            val path = context.getExternalFilesDir(null) ?: context.filesDir
            val stat = android.os.StatFs(path.path)
            return stat.blockCountLong * stat.blockSizeLong
        }

        private fun getFreeStorage(context: Context): Long {
            val path = context.getExternalFilesDir(null) ?: context.filesDir
            val stat = android.os.StatFs(path.path)
            return stat.availableBlocksLong * stat.blockSizeLong
        }

        private fun getKernelVersion(): String {
            return try {
                val process = Runtime.getRuntime().exec("uname -r")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                reader.readLine() ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
        }

        private fun getThermalStatus(context: Context): String? {
            // For now, return null since IThermalService requires special permissions and system-level access
            return null
        }

        private fun getBatteryTemperature(context: Context): Float? {
            try {
                val batteryIntent = context.registerReceiver(
                    null,
                    android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
                )
                val temperature = batteryIntent?.getIntExtra("temperature", Int.MIN_VALUE)
                val tempValue = temperature ?: Int.MIN_VALUE
                return if (tempValue != Int.MIN_VALUE) tempValue / 10.0f else null
            } catch (e: Exception) {
                return null
            }
        }

        private fun getBatteryCapacity(context: Context): Float? {
            try {
                val batteryIntent = context.registerReceiver(
                    null,
                    android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
                )
                return batteryIntent?.getIntExtra("level", Int.MIN_VALUE)?.toFloat()
            } catch (e: Exception) {
                return null
            }
        }
        
        private fun getTotalSwap(): Long {
            try {
                val memInfoFile = File("/proc/meminfo")
                if (memInfoFile.exists()) {
                    val lines = memInfoFile.readLines()
                    for (line in lines) {
                        if (line.startsWith("SwapTotal:")) {
                            val parts = line.split("\\s+".toRegex())
                            if (parts.size >= 2) {
                                return (parts[1].toLongOrNull() ?: 0) * 1024 // Convert to bytes
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // If we can't read swap info, return 0
            }
            return 0
        }
        
        private fun getUsedSwap(): Long {
            try {
                val memInfoFile = File("/proc/meminfo")
                if (memInfoFile.exists()) {
                    var totalSwap = 0L
                    var freeSwap = 0L
                    
                    val lines = memInfoFile.readLines()
                    for (line in lines) {
                        if (line.startsWith("SwapTotal:")) {
                            val parts = line.split("\\s+".toRegex())
                            if (parts.size >= 2) {
                                totalSwap = (parts[1].toLongOrNull() ?: 0) * 1024 // Convert to bytes
                            }
                        } else if (line.startsWith("SwapFree:")) {
                            val parts = line.split("\\s+".toRegex())
                            if (parts.size >= 2) {
                                freeSwap = (parts[1].toLongOrNull() ?: 0) * 1024 // Convert to bytes
                            }
                        }
                    }
                    
                    return totalSwap - freeSwap
                }
            } catch (e: Exception) {
                // If we can't read swap info, return 0
            }
            return 0
        }
    }
}