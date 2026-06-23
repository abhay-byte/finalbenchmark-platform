package com.zenithblue.fb2Pro.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenithblue.fb2Pro.utils.RootAccessManager
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
    val cpuAffinityControl: PerformanceOptimizationStatus = PerformanceOptimizationStatus.DISABLED,
    val foregroundService: PerformanceOptimizationStatus = PerformanceOptimizationStatus.DISABLED,
    val cpuGovernorHints: PerformanceOptimizationStatus = PerformanceOptimizationStatus.DISABLED
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
            // Use RootAccessManager for proper caching - this prevents repeated heavy checks
            val hasRootAccess = RootAccessManager.isRootGranted()
            
            // For simplicity, we'll treat all root access as ROOT_WORKING 
            // since RootAccessManager already verifies that commands can execute
            val result = if (hasRootAccess) RootStatus.ROOT_WORKING else RootStatus.NO_ROOT
            
            Log.d("MainViewModel", "Root access result via RootAccessManager: $result")
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
    
    fun updateForegroundServiceStatus(status: PerformanceOptimizationStatus) {
        _performanceOptimizations.value = _performanceOptimizations.value.copy(
            foregroundService = status
        )
    }
    
    fun updateCpuGovernorHintsStatus(status: PerformanceOptimizationStatus) {
        _performanceOptimizations.value = _performanceOptimizations.value.copy(
            cpuGovernorHints = status
        )
    }
    
    fun acquireWakeLock() {
        // This will be handled by the MainActivity
    }
    
    fun releaseWakeLock() {
        // This will be handled by the MainActivity
    }
}
