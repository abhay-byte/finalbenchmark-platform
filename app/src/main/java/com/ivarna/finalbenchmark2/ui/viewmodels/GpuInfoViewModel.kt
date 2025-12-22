package com.ivarna.finalbenchmark2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.ui.components.GpuDataPoint
import com.ivarna.finalbenchmark2.utils.GpuFrequencyReader
import com.ivarna.finalbenchmark2.utils.GpuFrequencyFallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.min

class GpuInfoViewModel : ViewModel() {
    private val gpuFrequencyReader = GpuFrequencyReader()
    private val gpuFrequencyMonitor = com.ivarna.finalbenchmark2.utils.GpuFrequencyMonitor(gpuFrequencyReader, viewModelScope)
    private val gpuFrequencyFallback = GpuFrequencyFallback()
    
    private val _gpuFrequencyState = MutableStateFlow(
        GpuFrequencyReader.GpuFrequencyState.NotSupported as GpuFrequencyReader.GpuFrequencyState
    )
    val gpuFrequencyState: StateFlow<GpuFrequencyReader.GpuFrequencyState> = _gpuFrequencyState
    
    // GPU utilization data points for the graph
    private val _gpuHistory = MutableStateFlow<List<GpuDataPoint>>(emptyList())
    val gpuHistory: StateFlow<List<GpuDataPoint>> = _gpuHistory
    
    init {
        refreshGpuFrequency()
        startGpuMonitoring()
    }
    
    fun refreshGpuFrequency() {
        viewModelScope.launch {
            // Try primary root-based reading first
            val primaryResult = gpuFrequencyReader.readGpuFrequency()
            
            val result = when (primaryResult) {
                is GpuFrequencyReader.GpuFrequencyState.RequiresRoot -> {
                    // If root is required but not available, try fallback
                    gpuFrequencyFallback.readGpuFrequencyWithoutRoot() ?: primaryResult
                }
                is GpuFrequencyReader.GpuFrequencyState.Error -> {
                    // If primary reading failed, try fallback
                    gpuFrequencyFallback.readGpuFrequencyWithoutRoot() ?: primaryResult
                }
                else -> {
                    // Use primary result if it was successful
                    primaryResult
                }
            }
            
            _gpuFrequencyState.value = result
        }
    }
    
    private fun startGpuMonitoring() {
        viewModelScope.launch {
            while (true) {
                try {
                    val result = gpuFrequencyReader.readGpuFrequency()
                    if (result is GpuFrequencyReader.GpuFrequencyState.Available) {
                        val gpuData = result.data
                        val currentFreq = gpuData.currentFrequencyMhz
                        val maxFreq = gpuData.maxFrequencyMhz
                        
                        // Calculate GPU utilization based on current frequency vs max frequency
                        // Use exponential calculation as specified in the requirements
                        val utilization = if (maxFreq != null && maxFreq > 0) {
                            // Exponential calculation: (current_freq/max_freq) * 100
                            // Higher frequency means higher utilization
                            min(100f, (currentFreq.toFloat() / maxFreq.toFloat()) * 100f)
                        } else {
                            // If max frequency is not available, try to get it from available frequencies
                            val availableFreqs = gpuData.availableFrequencies
                            if (availableFreqs != null && availableFreqs.isNotEmpty()) {
                                val maxAvailableFreq = availableFreqs.maxOrNull()
                                if (maxAvailableFreq != null && maxAvailableFreq > 0) {
                                    min(100f, (currentFreq.toFloat() / maxAvailableFreq.toFloat()) * 100f)
                                } else {
                                    // If still no max frequency, we can't calculate utilization
                                    // Use a simple heuristic: if current frequency > 0, set some utilization
                                    if (currentFreq > 0) {
                                        // For devices where max freq is not available, just use current freq as a percentage
                                        // of a typical max GPU frequency (e.g., 1000 MHz for basic GPUs)
                                        val typicalMaxFreq = 1000L // Adjust based on device capabilities
                                        min(100f, (currentFreq.toFloat() / typicalMaxFreq.toFloat()) * 100f)
                                    } else {
                                        0f
                                    }
                                }
                            } else {
                                // If no available frequencies either, use the same fallback
                                if (currentFreq > 0) {
                                    val typicalMaxFreq = 1000L
                                    min(100f, (currentFreq.toFloat() / typicalMaxFreq.toFloat()) * 100f)
                                } else {
                                    0f
                                }
                            }
                        }
                        
                        val newPoint = GpuDataPoint(
                            timestamp = System.currentTimeMillis(),
                            utilization = utilization
                        )
                        
                        // Update history with new data point, keeping only last 30 seconds
                        val updatedHistory = (_gpuHistory.value + newPoint)
                            .filter { it.timestamp > System.currentTimeMillis() - 30_000L }
                        
                        _gpuHistory.value = updatedHistory
                    }
                    
                    // CRITICAL FIX: Always update the state so the Card UI reflects real-time values
                    _gpuFrequencyState.value = result
                } catch (e: Exception) {
                    // Log error but continue monitoring
                    e.printStackTrace()
                }
                
                delay(1000) // Update every 1000ms (1 second) for better performance
            }
        }
    }
}