package com.ivarna.finalbenchmark2.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.utils.RootUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Enum to represent different root states
enum class RootStatus {
    NO_ROOT,           // Device is not rooted
    ROOT_AVAILABLE,    // Device is rooted but commands don't work
    ROOT_WORKING       // Device is rooted and commands work
}

// Enum to represent performance optimization states
enum class PerformanceOptimizationStatus {
    NOT_SUPPORTED,     // Device doesn't support the optimization
    DISABLED,          // Optimization is available but disabled
    ENABLED,           // Optimization is active
    READY              // Optimization is initialized but not yet acquired (for wake lock)
}

data class PerformanceOptimizations(
    val sustainedPerformanceMode: PerformanceOptimizationStatus = PerformanceOptimizationStatus.DISABLED,
    val wakeLockStatus: PerformanceOptimizationStatus = PerformanceOptimizationStatus.ENABLED, // Ready state
    val screenAlwaysOnStatus: PerformanceOptimizationStatus = PerformanceOptimizationStatus.ENABLED,
    val highPriorityThreading: PerformanceOptimizationStatus = PerformanceOptimizationStatus.DISABLED,
    val performanceHintApi: PerformanceOptimizationStatus = PerformanceOptimizationStatus.DISABLED,
    val cpuAffinityControl: PerformanceOptimizationStatus = PerformanceOptimizationStatus.DISABLED
)

class MainViewModel : ViewModel() {
    
    private val _rootState = MutableStateFlow(RootStatus.NO_ROOT)
    val rootState: StateFlow<RootStatus> = _rootState.asStateFlow()
    
    private val _performanceOptimizations = MutableStateFlow(PerformanceOptimizations())
    val performanceOptimizations: StateFlow<PerformanceOptimizations> = _performanceOptimizations.asStateFlow()

    init {
        checkRootAccess()
    }

    private fun checkRootAccess() {
        viewModelScope.launch(Dispatchers.IO) {
            // This runs only ONCE when the app starts
            Log.d("MainViewModel", "Starting root access check...")
            val isRoot = RootUtils.isDeviceRooted()
            Log.d("MainViewModel", "Device rooted check result: $isRoot")
            var canExecuteRoot = false
            if (isRoot) {
                Log.d("MainViewModel", "Checking if root commands work...")
                canExecuteRoot = RootUtils.canExecuteRootCommand()
                Log.d("MainViewModel", "Root command execution check result: $canExecuteRoot")
            } else {
                Log.d("MainViewModel", "Skipping root command check since device is not rooted")
            }
            
            val result = when {
                isRoot && canExecuteRoot -> RootStatus.ROOT_WORKING
                isRoot && !canExecuteRoot -> RootStatus.ROOT_AVAILABLE
                else -> RootStatus.NO_ROOT
            }
            
            Log.d("MainViewModel", "Final root access result: $result")
            _rootState.value = result
        }
    }
    
    fun updateSustainedPerformanceModeStatus(status: PerformanceOptimizationStatus) {
        _performanceOptimizations.value = _performanceOptimizations.value.copy(
            sustainedPerformanceMode = status
        )
    }
    
    fun updateWakeLockStatus(status: PerformanceOptimizationStatus) {
        _performanceOptimizations.value = _performanceOptimizations.value.copy(
            wakeLockStatus = status
        )
    }
    
    fun updateScreenAlwaysOnStatus(status: PerformanceOptimizationStatus) {
        _performanceOptimizations.value = _performanceOptimizations.value.copy(
            screenAlwaysOnStatus = status
        )
    }
    
    fun updateHighPriorityThreadingStatus(status: PerformanceOptimizationStatus) {
        _performanceOptimizations.value = _performanceOptimizations.value.copy(
            highPriorityThreading = status
        )
    }
    
    fun updatePerformanceHintApiStatus(status: PerformanceOptimizationStatus) {
        _performanceOptimizations.value = _performanceOptimizations.value.copy(
            performanceHintApi = status
        )
    }
    
    fun updateCpuAffinityControlStatus(status: PerformanceOptimizationStatus) {
        _performanceOptimizations.value = _performanceOptimizations.value.copy(
            cpuAffinityControl = status
        )
    }
    
    fun acquireWakeLock() {
        // This will be handled by the MainActivity
    }
    
    fun releaseWakeLock() {
        // This will be handled by the MainActivity
    }
}