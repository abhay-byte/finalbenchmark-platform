package com.ivarna.finalbenchmark2.ui.viewmodels

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.ui.components.CpuDataPoint
import com.ivarna.finalbenchmark2.ui.components.MemoryDataPoint
import com.ivarna.finalbenchmark2.ui.components.PowerDataPoint
import com.ivarna.finalbenchmark2.utils.DeviceInfoCollector
import com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils
import com.ivarna.finalbenchmark2.utils.PowerUtils
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
    
    // NEW: Power monitoring
    private val _powerHistory = MutableStateFlow<List<PowerDataPoint>>(emptyList())
    val powerHistory: StateFlow<List<PowerDataPoint>> = _powerHistory.asStateFlow()
    
    private val _currentPowerConsumption = MutableStateFlow(0f)
    val currentPowerConsumption: StateFlow<Float> = _currentPowerConsumption.asStateFlow()
    
    // NEW: Memory monitoring
    private val _memoryHistory = MutableStateFlow<List<MemoryDataPoint>>(emptyList())
    val memoryHistory: StateFlow<List<MemoryDataPoint>> = _memoryHistory.asStateFlow()
    
    private val _currentMemoryUtilization = MutableStateFlow(0f)
    val currentMemoryUtilization: StateFlow<Float> = _currentMemoryUtilization.asStateFlow()
    
    // NEW: System process monitoring
    private val _systemInfoSummary = MutableStateFlow<com.ivarna.finalbenchmark2.ui.components.SystemInfoSummary>(com.ivarna.finalbenchmark2.ui.components.SystemInfoSummary())
    val systemInfoSummary: StateFlow<com.ivarna.finalbenchmark2.ui.components.SystemInfoSummary> = _systemInfoSummary.asStateFlow()
    
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
        totalSwap = 0,
        usedSwap = 0,
        androidVersion = "",
        apiLevel = 0,
        kernelVersion = "",
        thermalStatus = null,
        batteryTemperature = null,
        batteryCapacity = null
    ))
    val deviceInfo: StateFlow<com.ivarna.finalbenchmark2.utils.DeviceInfo> = _deviceInfo.asStateFlow()
    
    private var cpuUtilizationUtils: CpuUtilizationUtils? = null
    private var powerUtils: PowerUtils? = null
    private var isMonitoringStarted = false
    
    fun initialize(context: Context) {
        cpuUtilizationUtils = CpuUtilizationUtils(context)
        powerUtils = PowerUtils(context)
        if (!isMonitoringStarted) {
            isMonitoringStarted = true
            startCpuMonitoring()
            startPowerMonitoring() // NEW: Start power monitoring
            startMemoryMonitoring(context) // NEW: Start memory monitoring
        }
    }
    
    fun fetchSystemInfo(context: Context) {
        viewModelScope.launch {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val pm = context.packageManager
            
            // 1. Get Running Processes
            val runningProcesses = am.runningAppProcesses ?: emptyList()
            val pids = runningProcesses.map { it.pid }.toIntArray()
            
            // Get memory info for each process
            val memoryInfos = if (pids.isNotEmpty()) {
                am.getProcessMemoryInfo(pids)
            } else {
                emptyArray()
            }
            
            val processList = runningProcesses.mapIndexed { index, process ->
                val memInfo = if (index < memoryInfos.size) memoryInfos[index] else null
                val ramMb = if (memInfo != null) (memInfo.totalPss / 1024) else 0 // Total PSS is in KB, convert to MB
                
                com.ivarna.finalbenchmark2.ui.components.ProcessItem(
                    name = process.processName,
                    pid = process.pid,
                    ramUsage = ramMb,
                    state = convertImportance(process.importance),
                    packageName = process.processName
                )
            }

            // 2. Get Totals (Requires QUERY_ALL_PACKAGES)
            var totalPackages = 0
            var totalServices = 0
            try {
                val allPackages = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledPackages(android.content.pm.PackageManager.PackageInfoFlags.of(android.content.pm.PackageManager.GET_SERVICES.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstalledPackages(android.content.pm.PackageManager.GET_SERVICES)
                }
                
                totalPackages = allPackages.size
                totalServices = allPackages.sumOf { it.services?.size ?: 0 }
            } catch (e: Exception) {
                // Handle permission issues gracefully
                e.printStackTrace()
            }
            
            val summary = com.ivarna.finalbenchmark2.ui.components.SystemInfoSummary(
                runningProcesses = runningProcesses.size,
                totalPackages = totalPackages,
                totalServices = totalServices,
                processes = processList.sortedByDescending { it.ramUsage }
            )
            
            _systemInfoSummary.value = summary
        }
    }
    
    private fun convertImportance(importance: Int): String {
        return when (importance) {
            android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND,
            android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE,
            android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE -> "Foreground"
            android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE,
            android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING -> "Service"
            android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND,
            android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "Background"
            else -> "Unknown"
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
    
    // NEW: Start power monitoring with improved real-time updates
    private fun startPowerMonitoring() {
        viewModelScope.launch {
            while (true) {
                try {
                    // Get current power consumption
                    val powerInfo = powerUtils?.getPowerConsumptionInfo()
                    val powerWatts = powerInfo?.power ?: 0f
                    _currentPowerConsumption.value = powerWatts // Preserve sign for charging/discharging
                    
                    // Add to history
                    val now = System.currentTimeMillis()
                    val newHistory = _powerHistory.value.toMutableList()
                    newHistory.add(PowerDataPoint(now, powerWatts))
                    
                    // Remove data points older than 30 seconds
                    val cutoffTime = now - 30_000L
                    newHistory.removeAll { it.timestamp < cutoffTime }
                    
                    // Limit total points to prevent memory issues
                    if (newHistory.size > 60) {
                        newHistory.removeAt(0)
                    }
                    
                    _powerHistory.value = newHistory.toList() // Ensure immutable list is set
                } catch (e: Exception) {
                    // Handle any errors in power monitoring gracefully
                    e.printStackTrace()
                }
                
                delay(1000) // Update every 1000ms (1 second) as requested
            }
        }
    }
    
    // NEW: Start memory monitoring
    private fun startMemoryMonitoring(context: Context) {
        viewModelScope.launch {
            while (true) {
                try {
                    // Get current memory utilization
                    val memoryUtil = getMemoryUsage(context)
                    _currentMemoryUtilization.value = memoryUtil.toFloat()
                    
                    // Add to history
                    val now = System.currentTimeMillis()
                    val newHistory = _memoryHistory.value.toMutableList()
                    newHistory.add(MemoryDataPoint(now, memoryUtil.toFloat()))
                    
                    // Remove data points older than 30 seconds
                    val cutoffTime = now - 30_000L
                    newHistory.removeAll { it.timestamp < cutoffTime }
                    
                    // Limit total points to prevent memory issues
                    if (newHistory.size > 60) {
                        newHistory.removeAt(0)
                    }
                    
                    _memoryHistory.value = newHistory
                } catch (e: Exception) {
                    // Handle any errors in memory monitoring gracefully
                    e.printStackTrace()
                }
                
                delay(1000) // Update every 1000ms (1 second)
            }
        }
    }
    
    private fun getMemoryUsage(context: Context): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val total = memoryInfo.totalMem
        val available = memoryInfo.availMem
        val used = total - available
        val percent = ((used.toDouble() / total.toDouble()) * 100).toInt()

        return percent.coerceIn(0, 100)
    }
    
    fun updateDeviceInfo(context: Context) {
        _deviceInfo.value = DeviceInfoCollector.getDeviceInfo(context)
    }
}