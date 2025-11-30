package com.ivarna.finalbenchmark2.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.ui.components.CpuDataPoint
import com.ivarna.finalbenchmark2.utils.DeviceInfoCollector
import com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class DeviceViewModel : ViewModel() {
    
    private val _cpuHistory = MutableStateFlow<List<CpuDataPoint>>(emptyList())
    val cpuHistory: StateFlow<List<CpuDataPoint>> = _cpuHistory.asStateFlow()
    
    private val _currentCpuUtilization = MutableStateFlow(0f)
    val currentCpuUtilization: StateFlow<Float> = _currentCpuUtilization.asStateFlow()
    
    private val _deviceInfo = MutableStateFlow<com.ivarna.finalbenchmark2.utils.DeviceInfo>(com.ivarna.finalbenchmark2.utils.DeviceInfo(
        deviceModel = "",
        manufacturer = "",
        board = "",
        socName = "",
        cpuArchitecture = "",
        totalCores = 0,
        bigCores = 0,
        smallCores = 0,
        clusterTopology = "",
        cpuFrequencies = mapOf(),
        gpuModel = "",
        gpuVendor = "",
        totalRam = 0,
        availableRam = 0,
        totalStorage = 0,
        freeStorage = 0,
        androidVersion = "",
        apiLevel = 0,
        kernelVersion = "",
        thermalStatus = null,
        batteryTemperature = null,
        batteryCapacity = null
    ))
    val deviceInfo: StateFlow<com.ivarna.finalbenchmark2.utils.DeviceInfo> = _deviceInfo.asStateFlow()
    
    private var cpuUtilizationUtils: CpuUtilizationUtils? = null
    private var isMonitoringStarted = false
    
    fun initialize(context: Context) {
        cpuUtilizationUtils = CpuUtilizationUtils(context)
        if (!isMonitoringStarted) {
            isMonitoringStarted = true
            startCpuMonitoring()
        }
    }
    
    private fun startCpuMonitoring() {
        viewModelScope.launch {
            while (true) {
                try {
                    // Get current CPU utilization
                    val cpuUtil = cpuUtilizationUtils?.getCpuUtilizationPercentage() ?: 0f
                    _currentCpuUtilization.value = cpuUtil
                    
                    // Add to history
                    val now = System.currentTimeMillis()
                    val newHistory = _cpuHistory.value.toMutableList()
                    newHistory.add(CpuDataPoint(now, cpuUtil))
                    
                    // Remove data points older than 30 seconds
                    val cutoffTime = now - 30_000L
                    newHistory.removeAll { it.timestamp < cutoffTime }
                    
                    // Limit total points to prevent memory issues
                    if (newHistory.size > 60) {
                        newHistory.removeAt(0)
                    }
                    
                    _cpuHistory.value = newHistory
                } catch (e: Exception) {
                    // Handle any errors in CPU monitoring gracefully
                }
                
                delay(500) // Update every 500ms
            }
        }
    }
    
    fun updateDeviceInfo(context: Context) {
        _deviceInfo.value = DeviceInfoCollector.getDeviceInfo(context)
    }
}