package com.zenithblue.fb2Pro.ui.viewmodels

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenithblue.fb2Pro.ui.components.MemoryDataPoint
import com.zenithblue.fb2Pro.ui.components.MemoryStats
import com.zenithblue.fb2Pro.ui.components.SystemInfoSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MemoryUtilizationViewModel : ViewModel() {
    
    private val _memoryHistory = MutableStateFlow<List<MemoryDataPoint>>(emptyList())
    val memoryHistory: StateFlow<List<MemoryDataPoint>> = _memoryHistory.asStateFlow()
    
    private val _currentMemoryStats = MutableStateFlow<MemoryStats?>(null)
    val currentMemoryStats: StateFlow<MemoryStats?> = _currentMemoryStats.asStateFlow()
    
    private val _systemInfoSummary = MutableStateFlow(SystemInfoSummary())
    val systemInfoSummary: StateFlow<SystemInfoSummary> = _systemInfoSummary.asStateFlow()
    
    private var isMonitoringStarted = false
    
    fun initialize(context: Context) {
        if (!isMonitoringStarted) {
            isMonitoringStarted = true
            startMemoryMonitoring(context)
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
                
                com.zenithblue.fb2Pro.ui.components.ProcessItem(
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
            
            val summary = SystemInfoSummary(
                runningProcesses = runningProcesses.size,
                totalPackages = totalPackages,
                totalServices = totalServices,
                processes = processList.sortedByDescending { it.ramUsage }
            )
            
            _systemInfoSummary.value = summary
        }
    }
    
    private fun startMemoryMonitoring(context: Context) {
        viewModelScope.launch {
            while (true) {
                try {
                    // Get current memory stats
                    val memoryStats = getMemoryUsage(context)
                    _currentMemoryStats.value = memoryStats
                    
                    // Add to history
                    val now = System.currentTimeMillis()
                    val newHistory = _memoryHistory.value.toMutableList()
                    newHistory.add(MemoryDataPoint(now, memoryStats.usagePercent.toFloat()))
                    
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
    
    private fun getMemoryUsage(context: Context): MemoryStats {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val total = memoryInfo.totalMem
        val available = memoryInfo.availMem
        val used = total - available
        val percent = ((used.toDouble() / total.toDouble()) * 100).toInt()

        return MemoryStats(used, total, percent.coerceIn(0, 100))
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
}