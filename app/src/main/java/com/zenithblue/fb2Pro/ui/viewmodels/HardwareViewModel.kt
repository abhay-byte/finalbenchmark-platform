package com.zenithblue.fb2Pro.ui.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zenithblue.fb2Pro.utils.BatterySpec
import com.zenithblue.fb2Pro.utils.CameraSpec
import com.zenithblue.fb2Pro.utils.HardwareUtils
import com.zenithblue.fb2Pro.utils.MemoryStorageSpec
import com.zenithblue.fb2Pro.utils.NetworkSpec
import com.zenithblue.fb2Pro.utils.AudioMediaSpec
import com.zenithblue.fb2Pro.utils.PeripheralsSpec
import kotlinx.coroutines.launch

class HardwareViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _hardwareUtils = HardwareUtils(application.applicationContext)
    
    private val _batterySpecs = MutableLiveData<BatterySpec>()
    val batterySpecs: LiveData<BatterySpec> = _batterySpecs
    
    private val _networkSpecs = MutableLiveData<NetworkSpec>()
    val networkSpecs: LiveData<NetworkSpec> = _networkSpecs
    
    private val _cameraSpecs = MutableLiveData<List<CameraSpec>>()
    val cameraSpecs: LiveData<List<CameraSpec>> = _cameraSpecs
    
    private val _memoryStorageSpecs = MutableLiveData<MemoryStorageSpec>()
    val memoryStorageSpecs: LiveData<MemoryStorageSpec> = _memoryStorageSpecs
    
    private val _audioMediaSpecs = MutableLiveData<AudioMediaSpec>()
    val audioMediaSpecs: LiveData<AudioMediaSpec> = _audioMediaSpecs
    
    private val _peripheralsSpecs = MutableLiveData<PeripheralsSpec>()
    val peripheralsSpecs: LiveData<PeripheralsSpec> = _peripheralsSpecs
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadHardwareSpecs() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Load all hardware specs in parallel using async
                val batterySpecs = _hardwareUtils.getBatterySpecs()
                
                // Handle network specs separately to prevent entire load from failing
                val networkSpecs = try {
                    _hardwareUtils.getNetworkSpecs()
                } catch (e: Exception) {
                    Log.e("HardwareViewModel", "Error getting network specs: ${e.message}")
                    // Return a default NetworkSpec with error information
                    NetworkSpec(
                        networkType = "Unknown (Error: ${e.message})",
                        signalStrength = "Unknown",
                        wifiSpeed = "Unknown",
                        wifiFrequency = "Unknown",
                        wifiStandard = "Unknown",
                        bluetoothFeatures = emptyList(),
                        nfcSupported = false,
                        irBlasterSupported = false
                    )
                }
                
                val cameraSpecs = _hardwareUtils.getCameraSpecs()
                val memoryStorageSpecs = _hardwareUtils.getMemoryStorageSpecs()
                val audioMediaSpecs = _hardwareUtils.getAudioMediaSpecs()
                val peripheralsSpecs = _hardwareUtils.getPeripheralsSpecs()
                
                _batterySpecs.value = batterySpecs
                _networkSpecs.value = networkSpecs
                _cameraSpecs.value = cameraSpecs
                _memoryStorageSpecs.value = memoryStorageSpecs
                _audioMediaSpecs.value = audioMediaSpecs
                _peripheralsSpecs.value = peripheralsSpecs
                
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshHardwareSpecs() {
        loadHardwareSpecs()
    }
}