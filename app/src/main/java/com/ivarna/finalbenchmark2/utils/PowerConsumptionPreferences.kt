package com.ivarna.finalbenchmark2.utils

import android.content.Context
import androidx.compose.runtime.mutableStateOf

class PowerConsumptionPreferences(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("power_consumption_preferences", Context.MODE_PRIVATE)
    
    companion object {
        private const val POWER_CONSUMPTION_MULTIPLIER_KEY = "power_consumption_multiplier"
        private const val DEFAULT_MULTIPLIER = 1.0f
    }
    
    fun setMultiplier(multiplier: Float) {
        sharedPreferences.edit().putFloat(POWER_CONSUMPTION_MULTIPLIER_KEY, multiplier).apply()
    }
    
    fun getMultiplier(): Float {
        return sharedPreferences.getFloat(POWER_CONSUMPTION_MULTIPLIER_KEY, DEFAULT_MULTIPLIER)
    }
    
    // Create a mutable state for the multiplier that can be observed
    fun getMultiplierState() = mutableStateOf(getMultiplier())
}