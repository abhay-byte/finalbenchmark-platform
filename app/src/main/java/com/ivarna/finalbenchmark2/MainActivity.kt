package com.ivarna.finalbenchmark2

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import com.ivarna.finalbenchmark2.ui.viewmodels.PerformanceOptimizationStatus
import com.ivarna.finalbenchmark2.navigation.MainNavigation
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.ui.theme.LocalThemeMode
import com.ivarna.finalbenchmark2.ui.theme.provideThemeMode
import com.ivarna.finalbenchmark2.ui.viewmodels.MainViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.RootStatus
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
    
    private var mainViewModel: MainViewModel? = null
    private var sustainedModeIntendedState = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge to edge for full screen experience
        enableEdgeToEdge()
        
        // Enable Sustained Performance Mode to prevent thermal throttling
        enableSustainedPerformanceMode()
        
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
            
            // Initialize MainViewModel to check root access once at app startup
            val mainViewModel: MainViewModel = viewModel()
            // Store reference to mainViewModel for use in Activity methods
            this@MainActivity.mainViewModel = mainViewModel
            val rootStatus by mainViewModel.rootState.collectAsStateWithLifecycle()
            val isRootAvailable = rootStatus == RootStatus.ROOT_WORKING || rootStatus == RootStatus.ROOT_AVAILABLE
            
            // Update performance optimization status based on sustained performance mode
            val performanceOptimizations by mainViewModel.performanceOptimizations.collectAsStateWithLifecycle()
            
            
            
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
                            hazeState = hazeState,
                            rootStatus = rootStatus
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Enable Sustained Performance Mode to prevent thermal throttling
     * This should be called as early as possible in the Activity lifecycle
     */
    private fun enableSustainedPerformanceMode() {
        // Check if device supports API 24 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                // Enable sustained performance mode
                window.setSustainedPerformanceMode(true)
                sustainedModeIntendedState = true
                
                // Log success (optional but recommended for debugging)
                Log.i("FinalBenchmark2", "Sustained Performance Mode: ENABLED")
            } catch (e: Exception) {
                // Handle any errors gracefully
                sustainedModeIntendedState = false
                Log.e("FinalBenchmark2", "Error enabling Sustained Performance Mode", e)
                // Check if it's because the device doesn't support it
                if (e is java.lang.UnsupportedOperationException) {
                    Log.w("FinalBenchmark2", "Sustained Performance Mode: NOT SUPPORTED on this device")
                }
            }
        } else {
            // Running on Android < 7.0, feature not available
            sustainedModeIntendedState = false
            Log.w("FinalBenchmark2", "Sustained Performance Mode requires API 24+. Current API: ${Build.VERSION.SDK_INT}")
        }
    }
    
    /**
     * Check if sustained performance mode is currently active by checking our internal state
     * This method returns our intended state of the sustained performance mode
     */
    fun isSustainedPerformanceModeActive(): Boolean {
        return sustainedModeIntendedState
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Sustained mode is automatically disabled when activity is destroyed
        // But you can manually disable it if needed:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                this@MainActivity.window.setSustainedPerformanceMode(false)
                sustainedModeIntendedState = false
            } catch (e: Exception) {
                Log.e("FinalBenchmark2", "Error disabling Sustained Performance Mode", e)
                sustainedModeIntendedState = false
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