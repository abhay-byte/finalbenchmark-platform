package com.ivarna.finalbenchmark2.utils

import android.hardware.Sensor

object SensorUtils {
    /**
     * Maps sensor types to readable names
     */
    fun getSensorName(type: Int): String {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "Ambient Temperature"
            Sensor.TYPE_GAME_ROTATION_VECTOR -> "Game Rotation Vector"
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "Geomagnetic Rotation Vector"
            Sensor.TYPE_GRAVITY -> "Gravity"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope"
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> "Gyroscope Uncalibrated"
            Sensor.TYPE_HEART_RATE -> "Heart Rate"
            Sensor.TYPE_HINGE_ANGLE -> "Hinge Angle"
            Sensor.TYPE_LIGHT -> "Light"
            Sensor.TYPE_LINEAR_ACCELERATION -> "Linear Acceleration"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetic Field"
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "Magnetic Field Uncalibrated"
            Sensor.TYPE_MOTION_DETECT -> "Motion Detect"
            Sensor.TYPE_POSE_6DOF -> "Pose 6DOF"
            Sensor.TYPE_PRESSURE -> "Pressure / Barometer"
            Sensor.TYPE_PROXIMITY -> "Proximity"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "Relative Humidity"
            Sensor.TYPE_ROTATION_VECTOR -> "Rotation Vector"
            Sensor.TYPE_SIGNIFICANT_MOTION -> "Significant Motion"
            Sensor.TYPE_STATIONARY_DETECT -> "Stationary Detect"
            Sensor.TYPE_STEP_COUNTER -> "Step Counter"
            Sensor.TYPE_STEP_DETECTOR -> "Step Detector"
            else -> "Unknown Sensor ($type)"
        }
    }

    /**
     * Maps sensor types to their units
     */
    fun getSensorUnit(type: Int): String {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER -> "m/s²"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "°C"
            Sensor.TYPE_GRAVITY -> "m/s²"
            Sensor.TYPE_GYROSCOPE -> "rad/s"
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> "rad/s"
            Sensor.TYPE_HEART_RATE -> "bpm"
            Sensor.TYPE_LIGHT -> "lux"
            Sensor.TYPE_LINEAR_ACCELERATION -> "m/s²"
            Sensor.TYPE_MAGNETIC_FIELD -> "μT"
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "μT"
            Sensor.TYPE_PRESSURE -> "hPa"
            Sensor.TYPE_PROXIMITY -> "cm"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "%"
            Sensor.TYPE_ROTATION_VECTOR -> ""
            Sensor.TYPE_GAME_ROTATION_VECTOR -> ""
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> ""
            Sensor.TYPE_POSE_6DOF -> ""
            Sensor.TYPE_HINGE_ANGLE -> "°"
            else -> ""
        }
    }

    /**
     * Formats sensor values based on sensor type
     */
    fun formatSensorValues(type: Int, values: List<Float>): String {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER, 
            Sensor.TYPE_GRAVITY, 
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> {
                if (values.size >= 3) {
                    "X: ${"%.2f".format(values[0])}, Y: ${"%.2f".format(values[1])}, Z: ${"%.2f".format(values[2])}"
                } else {
                    values.joinToString(", ") { "%.2f".format(it) }
                }
            }
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> {
                if (values.size >= 4) {
                    "X: ${"%.3f".format(values[0])}, Y: ${"%.3f".format(values[1])}, Z: ${"%.3f".format(values[2])}, W: ${"%.3f".format(values[3])}"
                } else if (values.size >= 3) {
                    "X: ${"%.3f".format(values[0])}, Y: ${"%.3f".format(values[1])}, Z: ${"%.3f".format(values[2])}"
                } else {
                    values.joinToString(", ") { "%.3f".format(it) }
                }
            }
            Sensor.TYPE_LIGHT -> "${"%.2f".format(values[0])} lux"
            Sensor.TYPE_PROXIMITY -> "${"%.2f".format(values[0])} cm"
            Sensor.TYPE_PRESSURE -> "${"%.2f".format(values[0])} hPa"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "${"%.2f".format(values[0])} °C"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "${"%.2f".format(values[0])} %"
            Sensor.TYPE_HEART_RATE -> "${"%.1f".format(values[0])} bpm"
            Sensor.TYPE_HINGE_ANGLE -> "${"%.2f".format(values[0])} °"
            Sensor.TYPE_STEP_COUNTER -> "${values[0].toInt()}"
            Sensor.TYPE_STEP_DETECTOR -> "${values[0].toInt()}"
            else -> values.joinToString(", ") { "%.2f".format(it) }
        }
    }
}