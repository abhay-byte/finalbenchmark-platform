package com.ivarna.finalbenchmark2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ivarna.finalbenchmark2.navigation.MainNavigation
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.ui.theme.LocalThemeMode
import com.ivarna.finalbenchmark2.ui.theme.provideThemeMode
import com.ivarna.finalbenchmark2.utils.ThemePreferences
import com.ivarna.finalbenchmark2.ui.theme.ThemeMode
import dev.chrisbanes.haze.HazeState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val themePreferences = ThemePreferences(this)
        val themeMode = themePreferences.getThemeMode()
        
        // Apply the theme mode using AppCompatDelegate BEFORE setting content
        // This ensures the system theme is applied on first launch
        applyThemeMode(themeMode)
        
        setContent {
            var currentThemeMode by remember { mutableStateOf(themeMode) }
            
            // Provide theme mode to the composition
            provideThemeMode(currentThemeMode) {
                FinalBenchmark2Theme(themeMode = currentThemeMode) {
                    val hazeState = remember { HazeState() }
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        MainNavigation(
                            modifier = Modifier.padding(innerPadding),
                            hazeState = hazeState
                        )
                    }
                }
            }
        }
    }
    
    private fun applyThemeMode(themeMode: ThemeMode) {
        when (themeMode) {
            ThemeMode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            ThemeMode.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            ThemeMode.SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            // For custom themes, we'll default to dark mode since they're mostly dark themes
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
    
    fun updateTheme(themeMode: ThemeMode) {
        applyThemeMode(themeMode)
        // Recreate the activity to apply the theme immediately
        recreate()
    }
}