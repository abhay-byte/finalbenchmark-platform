package com.ivarna.finalbenchmark2.ui.viewmodels

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.utils.SensorUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SensorUiState(
    val name: String,
    val type: Int,
    val isSupported: Boolean,
    val values: List<Float> = emptyList(),
    val vendor: String = "",
    val power: Float = 0f,
    val resolution: Float = 0f,
    val maxRange: Float = 0f,
    val unit: String = ""
)

class SensorViewModel : ViewModel() {
    private var sensorManager: SensorManager? = null
    private val _sensorStates = MutableStateFlow<List<SensorUiState>>(emptyList())
    val sensorStates: StateFlow<List<SensorUiState>> = _sensorStates.asStateFlow()
    
    private val _supportedSensorCount = MutableStateFlow(0)
    val supportedSensorCount: StateFlow<Int> = _supportedSensorCount.asStateFlow()
    
    private val _totalSensorCount = MutableStateFlow(0)
    val totalSensorCount: StateFlow<Int> = _totalSensorCount.asStateFlow()
    
    private val targetSensors = listOf(
        Sensor.TYPE_ACCELEROMETER to "Accelerometer",
        Sensor.TYPE_AMBIENT_TEMPERATURE to "Ambient Temperature",
        Sensor.TYPE_GAME_ROTATION_VECTOR to "Game Rotation Vector",
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR to "Geomagnetic Rotation Vector",
        Sensor.TYPE_GRAVITY to "Gravity",
        Sensor.TYPE_GYROSCOPE to "Gyroscope",
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED to "Gyroscope Uncalibrated",
        Sensor.TYPE_HEART_RATE to "Heart Rate",
        Sensor.TYPE_HINGE_ANGLE to "Hinge Angle",
        Sensor.TYPE_LIGHT to "Light",
        Sensor.TYPE_LINEAR_ACCELERATION to "Linear Acceleration",
        Sensor.TYPE_MAGNETIC_FIELD to "Magnetic Field",
        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED to "Magnetic Field Uncalibrated",
        Sensor.TYPE_MOTION_DETECT to "Motion Detect",
        Sensor.TYPE_POSE_6DOF to "Pose 6DOF",
        Sensor.TYPE_PRESSURE to "Pressure / Barometer",
        Sensor.TYPE_PROXIMITY to "Proximity",
        Sensor.TYPE_RELATIVE_HUMIDITY to "Relative Humidity",
        Sensor.TYPE_ROTATION_VECTOR to "Rotation Vector",
        Sensor.TYPE_SIGNIFICANT_MOTION to "Significant Motion",
        Sensor.TYPE_STATIONARY_DETECT to "Stationary Detect",
        Sensor.TYPE_STEP_COUNTER to "Step Counter",
        Sensor.TYPE_STEP_DETECTOR to "Step Detector"
    )
    
    fun initialize(context: Context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager = sm
        
        val allSensors = sm.getSensorList(Sensor.TYPE_ALL)
        val sensorStates = targetSensors.map { (type, name) ->
            val sensor = allSensors.find { it.type == type }
            if (sensor != null) {
                SensorUiState(
                    name = name,
                    type = type,
                    isSupported = true,
                    vendor = sensor.vendor,
                    power = sensor.power,
                    resolution = sensor.resolution,
                    maxRange = sensor.maximumRange,
                    unit = SensorUtils.getSensorUnit(type)
                )
            } else {
                SensorUiState(
                    name = name,
                    type = type,
                    isSupported = false
                )
            }
        }.sortedBy { it.name }
        
        _totalSensorCount.value = sensorStates.size
        _supportedSensorCount.value = sensorStates.count { it.isSupported }
        _sensorStates.value = sensorStates
        
        // Register listeners for supported sensors
        registerSensorListeners(sm, allSensors)
    }
    
    private fun registerSensorListeners(sensorManager: SensorManager, allSensors: List<android.hardware.Sensor>) {
        val supportedSensorTypes = _sensorStates.value.filter { it.isSupported }.map { it.type }
        
        for (type in supportedSensorTypes) {
            val sensor = allSensors.find { it.type == type }
            if (sensor != null) {
                // Register with a delay to avoid overwhelming the system
                sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }
    
    fun unregisterAllListeners() {
        sensorManager?.unregisterListener(sensorEventListener)
    }
    
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return
            
            val updatedStates = _sensorStates.value.map { sensorState ->
                if (sensorState.type == event.sensor.type) {
                    sensorState.copy(values = event.values.toList())
                } else {
                    sensorState
                }
            }
            
            _sensorStates.value = updatedStates
        }
        
        override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        unregisterAllListeners()
    }
}