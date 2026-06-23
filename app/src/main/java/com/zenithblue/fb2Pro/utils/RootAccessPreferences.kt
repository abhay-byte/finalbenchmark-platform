package com.zenithblue.fb2Pro.utils

import android.content.Context

class RootAccessPreferences(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("root_access_preferences", Context.MODE_PRIVATE)
    private val USE_ROOT_KEY = "use_root_access"
    
    fun setUseRootAccess(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(USE_ROOT_KEY, enabled).apply()
    }
    
    fun getUseRootAccess(): Boolean {
        return sharedPreferences.getBoolean(USE_ROOT_KEY, false) // Default to false
    }
}