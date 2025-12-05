package com.ivarna.finalbenchmark2.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils
import com.ivarna.finalbenchmark2.utils.PowerUtils
import com.ivarna.finalbenchmark2.utils.TemperatureUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        
        monitoringJob = viewModelScope.launch {
            while (true) {
                try {
                    val cpuLoad = cpuUtilizationUtils.getCpuUtilizationPercentage()
                    val powerInfo = powerUtils.getPowerConsumptionInfo()
                    val cpuTemp = temperatureUtils.getCpuTemperature()
                    
                    _systemStats.value = SystemStats(
                        cpuLoad = cpuLoad,
                        power = powerInfo.power,
                        temp = cpuTemp
                    )
                    
                    kotlinx.coroutines.delay(100) // Update every 100ms for more responsive monitoring
                } catch (e: Exception) {
                    e.printStackTrace()
                    kotlinx.coroutines.delay(2000) // On error, wait longer before retrying
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