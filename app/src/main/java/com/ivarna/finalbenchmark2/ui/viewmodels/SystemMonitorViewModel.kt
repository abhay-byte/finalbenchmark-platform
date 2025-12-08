package com.ivarna.finalbenchmark2.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils
import com.ivarna.finalbenchmark2.utils.PowerUtils
import com.ivarna.finalbenchmark2.utils.TemperatureUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Import the SystemStats data class from SystemModels
import com.ivarna.finalbenchmark2.ui.models.SystemStats

class SystemMonitorViewModel(application: Application) : AndroidViewModel(application) {
    private val _systemStats = MutableStateFlow(SystemStats())
    val systemStats: StateFlow<SystemStats> = _systemStats.asStateFlow()
    
    private val cpuUtilizationUtils = CpuUtilizationUtils(application)
    private val powerUtils = PowerUtils(application)
    private val temperatureUtils = TemperatureUtils(application)
    
    private var monitoringJob: kotlinx.coroutines.Job? = null
    
    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        
        monitoringJob = viewModelScope.launch(Dispatchers.IO) { // Run on background thread
            while (isActive) { // Use isActive for proper lifecycle management
                try {
                    // 1. Fetch Data
                    val cpuLoad = cpuUtilizationUtils.getCpuUtilizationPercentage()
                    val powerInfo = powerUtils.getPowerConsumptionInfo()
                    val cpuTemp = temperatureUtils.getCpuTemperature()
                    
                    // 2. Update State
                    _systemStats.value = SystemStats(
                        cpuLoad = cpuLoad,
                        power = powerInfo.power,
                        temp = cpuTemp
                    )
                    
                    // 3. THROTTLE (The Fix) - Update only once per second
                    delay(1000L)
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(2000L) // On error, wait longer before retrying
                }
            }
        }
    }
    
    fun stopMonitoring() {
        monitoringJob?.cancel()
    }
    
    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}