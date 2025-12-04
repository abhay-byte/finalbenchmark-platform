package com.ivarna.finalbenchmark2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.utils.GpuFrequencyReader
import com.ivarna.finalbenchmark2.utils.GpuFrequencyFallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GpuInfoViewModel : ViewModel() {
    private val gpuFrequencyReader = GpuFrequencyReader()
    private val gpuFrequencyFallback = GpuFrequencyFallback()
    
    private val _gpuFrequencyState = MutableStateFlow(
        GpuFrequencyReader.GpuFrequencyState.NotSupported as GpuFrequencyReader.GpuFrequencyState
    )
    val gpuFrequencyState: StateFlow<GpuFrequencyReader.GpuFrequencyState> = _gpuFrequencyState
    
    init {
        refreshGpuFrequency()
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
}