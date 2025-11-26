package com.ivarna.finalbenchmark2.utils

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.ivarna.finalbenchmark2.ui.theme.ThemeMode

class ThemePreferences(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE)
    
    companion object {
        private const val THEME_MODE_KEY = "theme_mode"
    }
    
    fun setThemeMode(themeMode: ThemeMode) {
        sharedPreferences.edit().putString(THEME_MODE_KEY, themeMode.name).apply()
    }
    
    fun getThemeMode(): ThemeMode {
        val themeModeName = sharedPreferences.getString(THEME_MODE_KEY, ThemeMode.SYSTEM.name)
        return ThemeMode.valueOf(themeModeName ?: ThemeMode.SYSTEM.name)
    }
    
    // Create a mutable state for the theme mode that can be observed
    fun getThemeModeState() = mutableStateOf(getThemeMode())
}