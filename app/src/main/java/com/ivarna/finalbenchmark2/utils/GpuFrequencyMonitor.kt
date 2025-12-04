package com.ivarna.finalbenchmark2.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * GPU Frequency Monitor for Android
 *
 * Implementation based on analysis of SmartPack-Kernel Manager
 * Repository: https://github.com/SmartPack/SmartPack-Kernel-Manager
 *
 * @author KiloCode
 * @date 2025-12-02
 */
class GpuFrequencyMonitor(
    private val gpuFrequencyReader: GpuFrequencyReader,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "GpuFrequencyMonitor"
    }
    
    private val _frequencyFlow = MutableStateFlow<GpuFrequencyReader.GpuFrequencyState>(
        GpuFrequencyReader.GpuFrequencyState.NotSupported
    )
    val frequencyFlow: StateFlow<GpuFrequencyReader.GpuFrequencyState> = _frequencyFlow.asStateFlow()
    
    private var monitorJob: Job? = null
    
    // Configurable refresh rate (default: 500ms for smooth updates)
    var refreshRateMs: Long = 500L
    
    /**
     * Starts the GPU frequency monitoring
     */
    fun startMonitoring() {
        stopMonitoring()
        monitorJob = scope.launch {
            while (true) {
                if (monitorJob?.isActive == false) break
                try {
                    val data = gpuFrequencyReader.readGpuFrequency()
                    _frequencyFlow.value = data
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading GPU frequency", e)
                    _frequencyFlow.value = GpuFrequencyReader.GpuFrequencyState.Error(e.message ?: "Unknown error")
                }
                delay(refreshRateMs)
            }
        }
    }
    
    /**
     * Stops the GPU frequency monitoring
     */
    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }
    
    /**
     * Returns whether the monitoring is currently running
     */
    fun isMonitoring(): Boolean {
        return monitorJob?.isActive == true
    }
}