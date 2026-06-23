package com.zenithblue.fb2Pro.data.providers

import android.content.Context
import com.zenithblue.fb2Pro.domain.model.ItemValue
import com.zenithblue.fb2Pro.utils.CpuNativeBridge
import com.zenithblue.fb2Pro.utils.DeviceInfoCollector
import com.zenithblue.fb2Pro.utils.GpuInfoUtils

class DeviceInfoProvider {
    
    suspend fun getData(context: Context): List<ItemValue> {
        val deviceInfo = DeviceInfoCollector.getDeviceInfo(context)
        val gpuInfoUtils = GpuInfoUtils(context)
        val gpuInfoState = gpuInfoUtils.getGpuInfo()
        
        return buildList {
            // Device section
            add(ItemValue.Text("Device", ""))
            add(ItemValue.Text("Model", "${deviceInfo.manufacturer} ${deviceInfo.deviceModel}"))
            add(ItemValue.Text("Board", deviceInfo.board))
            add(ItemValue.Text("SoC", deviceInfo.socName))
            add(ItemValue.Text("Architecture", deviceInfo.cpuArchitecture))
            
            // CPU section
            add(ItemValue.Text("CPU", ""))
            
            // Get detailed CPU information from native bridge
            val cpuNative = CpuNativeBridge()
            val details = cpuNative.getCpuDetails()
            
            // Add processor details
            add(ItemValue.Text("SoC Name", details.socName))
            add(ItemValue.Text("ABI", details.abi))
            add(ItemValue.Text("ARM Neon", if(details.hasNeon) "Yes" else "No"))
            
            // Add cache configuration
            if (details.caches.isNotEmpty()) {
                add(ItemValue.Text("Cache Configuration", ""))
                details.caches.forEach { cache ->
                    // Format: "L1 Instruction" -> "64KB"
                    val name = "L${cache.level} ${cache.type.replaceFirstChar { it.uppercase() }}"
                    add(ItemValue.Text(name, cache.size))
                }
            }
            
            // Add basic core information
            add(ItemValue.Text("Total Cores", deviceInfo.totalCores.toString()))
            add(ItemValue.Text("Big Cores", deviceInfo.bigCores.toString()))
            add(ItemValue.Text("Small Cores", deviceInfo.smallCores.toString()))
            add(ItemValue.Text("Cluster Topology", deviceInfo.clusterTopology))
            
            // Add CPU frequencies
            deviceInfo.cpuFrequencies.forEach { (core, freq) ->
                add(ItemValue.Text("Core ${core} Frequency", freq))
            }
            
            // GPU section
            add(ItemValue.Text("GPU", ""))
            add(ItemValue.Text("Model", deviceInfo.gpuModel))
            add(ItemValue.Text("Vendor", deviceInfo.gpuVendor))
            
            // Add detailed GPU information if available
            if (gpuInfoState is com.zenithblue.fb2Pro.utils.GpuInfoState.Success) {
                val gpuInfo = gpuInfoState.gpuInfo
                add(ItemValue.Text("OpenGL ES", gpuInfo.basicInfo.openGLVersion))
                
                // Vulkan information
                gpuInfo.vulkanInfo?.let { vulkanInfo ->
                    add(ItemValue.Text("Vulkan Support", if (vulkanInfo.supported) "Yes" else "No"))
                    if (vulkanInfo.supported) {
                        vulkanInfo.apiVersion?.let { add(ItemValue.Text("Vulkan API Version", it)) }
                        vulkanInfo.driverVersion?.let { add(ItemValue.Text("Vulkan Driver Version", it)) }
                        vulkanInfo.physicalDeviceName?.let { add(ItemValue.Text("Physical Device", it)) }
                        vulkanInfo.physicalDeviceType?.let { add(ItemValue.Text("Device Type", it)) }
                        
                        // Add extension counts
                        add(ItemValue.Text("Vulkan Instance Extensions", "${vulkanInfo.instanceExtensions.size}"))
                        add(ItemValue.Text("Vulkan Device Extensions", "${vulkanInfo.deviceExtensions.size}"))
                        
                        // Add some key features
                        vulkanInfo.features?.let { features ->
                            add(ItemValue.Text("Geometry Shader", if (features.geometryShader) "Yes" else "No"))
                            add(ItemValue.Text("Tessellation Shader", if (features.tessellationShader) "Yes" else "No"))
                        }
                        
                        // Add memory heap information
                        vulkanInfo.memoryHeaps?.let { memoryHeaps ->
                            add(ItemValue.Text("Vulkan Memory Heaps", "${memoryHeaps.size}"))
                            // Add total memory from first heap as an example
                            if (memoryHeaps.isNotEmpty()) {
                                val largestHeap = memoryHeaps.maxByOrNull { it.size }
                                largestHeap?.let {
                                    add(ItemValue.Text("Largest Memory Heap", formatBytes(it.size)))
                                }
                            }
                        }
                    }
                }
            }
            
            // Memory section
            add(ItemValue.Text("Memory", ""))
            add(ItemValue.Text("Total RAM", formatBytes(deviceInfo.totalRam)))
            add(ItemValue.Text("Available RAM", formatBytes(deviceInfo.availableRam)))
            
            // Storage section
            add(ItemValue.Text("Storage", ""))
            add(ItemValue.Text("Total Storage", formatBytes(deviceInfo.totalStorage)))
            add(ItemValue.Text("Free Storage", formatBytes(deviceInfo.freeStorage)))
            
            // System section
            add(ItemValue.Text("System", ""))
            add(ItemValue.Text("Android Version", "${deviceInfo.androidVersion} (API ${deviceInfo.apiLevel})"))
            add(ItemValue.Text("Kernel Version", deviceInfo.kernelVersion))
            
            // Battery section (if available)
            if (deviceInfo.batteryTemperature != null) {
                add(ItemValue.Text("Battery", ""))
                add(ItemValue.Text("Temperature", "${deviceInfo.batteryTemperature}°C"))
                if (deviceInfo.batteryCapacity != null) {
                    add(ItemValue.Text("Capacity", "${deviceInfo.batteryCapacity}%"))
                }
            }
        }
    }
    
    private fun formatBytes(bytes: Long): String {
        val unit = 1024
        if (bytes < unit) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = "KMGTPE"[exp - 1] + "B"
        return String.format("%.1f %s", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }
}