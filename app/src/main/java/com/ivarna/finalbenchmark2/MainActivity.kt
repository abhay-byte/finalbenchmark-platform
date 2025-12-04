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
import com.topjohnwu.superuser.Shell
import dev.chrisbanes.haze.HazeState

class MainActivity : ComponentActivity() {
    
    // 1. Configure Shell at the top level
    companion object {
        init {
            // Set settings before the main shell is created
            Shell.enableVerboseLogging = true
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(10)
            )
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge to edge for full screen experience
        enableEdgeToEdge()
        
        // Use WindowInsetsController to hide system bars for full-screen immersive mode
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        
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
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {} // Empty top bar to remove any system app bar
                    ) { innerPadding ->
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