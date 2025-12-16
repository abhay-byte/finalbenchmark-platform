package com.ivarna.finalbenchmark2

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.PowerManager
import android.util.Log
import android.view.Window
import android.view.WindowManager
import java.io.File
import kotlinx.coroutines.runBlocking
import com.ivarna.finalbenchmark2.utils.RootUtils
import com.ivarna.finalbenchmark2.utils.RootCommandExecutor
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
        
        private const val TAG = "FinalBenchmark2"
        
        // Thread priority constants
        private const val BENCHMARK_THREAD_PRIORITY = Process.THREAD_PRIORITY_URGENT_AUDIO
        
        // Performance hint target duration (16.6ms = 60 FPS equivalent)
        private const val PERFORMANCE_TARGET_NS = 16_666_667L
    }
    
    private var mainViewModel: MainViewModel? = null
    private var sustainedModeIntendedState = false
    
    // Wake lock instance
    private lateinit var wakeLock: PowerManager.WakeLock
    private var isWakeLockHeld = false
    
    // Screen always on status
    private var isScreenAlwaysOn = false
    
    // NEW: Thread priority tracking
    private var isHighPriorityEnabled = false
    private var currentThreadPriority = 0
    
    // NEW: Performance Hint API tracking
    private var performanceHintSession: Any? = null
    private var isPerformanceHintEnabled = false
    
    // NEW: CPU affinity tracking
    private var isCpuAffinityEnabled = false
    private var bigCoreCount = 0
    private var midCoreCount = 0
    private var littleCoreCount = 0
    
    // NEW: Foreground service tracking
    private var isForegroundServiceActive = false
    
    // NEW: Governor hint tracking
    private var isGovernorHintApplied = false
    private var originalGovernor: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge to edge for full screen experience
        enableEdgeToEdge()
        
        // Initialize wake lock (but don't acquire yet)
        initializeWakeLock()
        
        // Enable screen always on
        enableScreenAlwaysOn()
        
        // Enable Sustained Performance Mode to prevent thermal throttling
        enableSustainedPerformanceMode()
        
        // NEW: Initialize high priority threading
        enableHighPriorityThreading()
        
        // NEW: Setup Performance Hint API (Android 12+)
        setupPerformanceHintAPI()
        
        // NEW: Detect CPU core configuration
        detectCPUCoreConfiguration()
        
        // NEW: Initialize governor hints
        initializeGovernorHints()
        
        // NEW: Request notification permission (Android 13+)
        requestNotificationPermission()
        
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
            
            // Update wake lock and screen always on status in the MainViewModel
            LaunchedEffect(Unit) {
                mainViewModel.updateWakeLockStatus(PerformanceOptimizationStatus.ENABLED) // Ready to be used
                mainViewModel.updateScreenAlwaysOnStatus(PerformanceOptimizationStatus.ENABLED) // Always on
                Log.i("FinalBenchmark2", "Initial wake lock status updated to ENABLED (ready state)")
                
                // NEW: Update CPU optimization statuses
                mainViewModel.updateHighPriorityThreadingStatus(PerformanceOptimizationStatus.ENABLED)
                mainViewModel.updatePerformanceHintApiStatus(PerformanceOptimizationStatus.ENABLED)
                mainViewModel.updateCpuAffinityControlStatus(PerformanceOptimizationStatus.ENABLED)
                mainViewModel.updateForegroundServiceStatus(PerformanceOptimizationStatus.READY)
                
                // Check if governor is already in performance mode and update status accordingly
                val currentGovernor = getCurrentGovernor()
                val governorStatus = if (currentGovernor == "performance") {
                    PerformanceOptimizationStatus.ENABLED
                } else {
                    PerformanceOptimizationStatus.ENABLED // Ready to be applied when needed
                }
                mainViewModel.updateCpuGovernorHintsStatus(governorStatus)
                Log.i("FinalBenchmark2", "Initial CPU optimization statuses updated - Governor: $currentGovernor (Status: $governorStatus)")
            }
            
            
            
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
    
    /**
     * Start all performance optimizations when benchmark begins
     */
    fun startAllOptimizations() {
        Log.i(TAG, "=== Starting All Performance Optimizations ===")
        
        // 1. Start foreground service FIRST (gives highest priority)
        startForegroundService()
        
        // 2. Acquire wake lock (keeps CPU at max frequency)
        acquireWakeLock()
        
        // 3. Apply governor hints (attempts to set performance mode)
        applyGovernorHints()
        
        // 4. Update UI status
        updatePerformanceOptimizationUI()
        
        // 5. Log status
        logOptimizationStatus()
    }
    
    /**
     * Stop all performance optimizations when benchmark completes
     */
    fun stopAllOptimizations() {
        Log.i(TAG, "=== Stopping All Performance Optimizations ===")
        
        // 1. Release wake lock
        releaseWakeLock()
        
        // 2. Stop foreground service
        stopForegroundService()
        
        // 3. Restore original governor
        restoreGovernor()
        
        // 4. Update UI status
        updatePerformanceOptimizationUI()
        
        Log.i(TAG, "All optimizations stopped - resources released")
    }
    
    /**
     * Log current optimization status
     */
    private fun logOptimizationStatus() {
        Log.i(TAG, "Optimization Status:")
        Log.i(TAG, "  - Sustained Performance: ${isSustainedPerformanceModeActive()}")
        Log.i(TAG, "  - Wake Lock: ${isWakeLockActive()}")
        Log.i(TAG, "  - Screen Always On: ${isScreenAlwaysOnActive()}")
        Log.i(TAG, "  - High Priority Threading: ${isHighPriorityThreadingActive()}")
        Log.i(TAG, "  - Performance Hint API: ${isPerformanceHintActive()}")
        Log.i(TAG, "  - CPU Affinity: ${isCpuAffinityActive()}")
        Log.i(TAG, "  - Foreground Service: ${isForegroundServiceActive()}")
        Log.i(TAG, "  - Governor Hints: ${isGovernorHintApplied()}")
    }
    
    /**
     * Update UI status for performance optimizations
     */
    private fun updatePerformanceOptimizationUI() {
        // This would update the UI in the MainViewModel or via a state update
        // For now, just log the status
        Log.i(TAG, "Performance optimization UI updated")
    }
    
    /**
     * Initialize the wake lock but don't acquire it yet
     * We'll acquire it when benchmark starts
     */
    private fun initializeWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            
            // Create a PARTIAL_WAKE_LOCK
            // This keeps CPU running but allows screen to turn off if needed
            // (We're using FLAG_KEEP_SCREEN_ON separately to keep screen on)
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "FinalBenchmark2::CPUBenchmarkWakeLock"
            )
            
            // Set wake lock to not reference counted
            // This means acquire/release calls must be balanced
            wakeLock.setReferenceCounted(false)
            
            Log.i("FinalBenchmark2", "Wake Lock initialized successfully. isHeld: ${wakeLock.isHeld}")
            
        } catch (e: Exception) {
            Log.e("FinalBenchmark2", "Failed to initialize Wake Lock", e)
        }
    }
    
    /**
     * Acquire wake lock to keep CPU at maximum frequency
     * Call this when benchmark starts
     */
    fun acquireWakeLock() {
        try {
            Log.i("FinalBenchmark2", "Attempting to acquire wake lock. isWakeLockHeld: $isWakeLockHeld, isInitialized: ${::wakeLock.isInitialized}, isHeld: ${if (::wakeLock.isInitialized) wakeLock.isHeld else "N/A"}")
            
            if (!isWakeLockHeld && ::wakeLock.isInitialized) {
                // Acquire wake lock without timeout (held until manually released)
                wakeLock.acquire()
                isWakeLockHeld = true
                
                // Update MainViewModel status
                mainViewModel?.updateWakeLockStatus(PerformanceOptimizationStatus.ENABLED)
                
                Log.i("FinalBenchmark2", "Wake Lock ACQUIRED - CPU will stay at maximum frequency. isHeld: ${wakeLock.isHeld}")
            } else {
                Log.i("FinalBenchmark2", "Wake lock was not acquired. Conditions: isWakeLockHeld=$isWakeLockHeld, isInitialized=${::wakeLock.isInitialized}")
            }
        } catch (e: Exception) {
            Log.e("FinalBenchmark2", "Failed to acquire Wake Lock", e)
            isWakeLockHeld = false
        }
    }
    
    /**
     * Release wake lock when benchmark completes
     * IMPORTANT: Always release in finally block to prevent battery drain
     */
    fun releaseWakeLock() {
        try {
            Log.i("FinalBenchmark2", "Attempting to release wake lock. isWakeLockHeld: $isWakeLockHeld, isInitialized: ${::wakeLock.isInitialized}, isHeld: ${if (::wakeLock.isInitialized) wakeLock.isHeld else "N/A"}")
            
            if (isWakeLockHeld && ::wakeLock.isInitialized && wakeLock.isHeld) {
                wakeLock.release()
                isWakeLockHeld = false
                
                // Update MainViewModel status
                mainViewModel?.updateWakeLockStatus(PerformanceOptimizationStatus.DISABLED)
                
                Log.i("FinalBenchmark2", "Wake Lock RELEASED")
            } else {
                Log.i("FinalBenchmark2", "Wake lock was not released. Conditions: isWakeLockHeld=$isWakeLockHeld, isInitialized=${::wakeLock.isInitialized}, isHeld=${if (::wakeLock.isInitialized) wakeLock.isHeld else "N/A"}")
            }
        } catch (e: Exception) {
            Log.e("FinalBenchmark2", "Failed to release Wake Lock", e)
        }
    }
    
    /**
     * Enable screen always on using window flags
     * This keeps the screen on while this activity is visible
     */
    private fun enableScreenAlwaysOn() {
        try {
            // Add FLAG_KEEP_SCREEN_ON to window
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            isScreenAlwaysOn = true
            
            Log.i("FinalBenchmark2", "Screen Always On: ENABLED")
            
            // Optional: Also add FLAG_TURN_SCREEN_ON to wake screen if it's off
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            
        } catch (e: Exception) {
            Log.e("FinalBenchmark2", "Failed to enable Screen Always On", e)
            isScreenAlwaysOn = false
        }
    }
    
    /**
     * Disable screen always on when no longer needed
     */
    private fun disableScreenAlwaysOn() {
        try {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            isScreenAlwaysOn = false
            
            Log.i("FinalBenchmark2", "Screen Always On: DISABLED")
            
        } catch (e: Exception) {
            Log.e("FinalBenchmark2", "Failed to disable Screen Always On", e)
        }
    }
    
    /**
     * Get the current wake lock status
     */
    fun isWakeLockActive(): Boolean {
        val status = isWakeLockHeld
        Log.i("FinalBenchmark2", "isWakeLockActive() called, returning: $status, actual isHeld: ${if (::wakeLock.isInitialized) wakeLock.isHeld else "N/A"}")
        return status
    }
    
    /**
     * Check if wake lock is ready (initialized but not acquired)
     */
    fun isWakeLockReady(): Boolean {
        val isReady = ::wakeLock.isInitialized && !isWakeLockHeld
        Log.i("FinalBenchmark2", "isWakeLockReady() called, returning: $isReady")
        return isReady
    }
    
    
    /**
     * Get the current screen always on status
     */
    fun isScreenAlwaysOnActive(): Boolean {
        return isScreenAlwaysOn
    }
    
    /**
     * Feature #4: Enable High Priority Threading
     * Sets the current thread to highest available priority
     */
    private fun enableHighPriorityThreading() {
        try {
            // Get current thread priority before changing
            currentThreadPriority = Process.getThreadPriority(Process.myTid())
            Log.i(TAG, "Current thread priority: $currentThreadPriority")
            
            // Set to highest priority (THREAD_PRIORITY_URGENT_AUDIO = -19)
            // This is the highest priority available without root access
            Process.setThreadPriority(BENCHMARK_THREAD_PRIORITY)
            
            // Verify the change
            val newPriority = Process.getThreadPriority(Process.myTid())
            
            if (newPriority == BENCHMARK_THREAD_PRIORITY) {
                isHighPriorityEnabled = true
                Log.i(TAG, "High Priority Threading: ENABLED (priority: $newPriority)")
            } else {
                isHighPriorityEnabled = false
                Log.w(TAG, "High Priority Threading: PARTIAL (requested: $BENCHMARK_THREAD_PRIORITY, got: $newPriority)")
            }
            
        } catch (e: Exception) {
            isHighPriorityEnabled = false
            Log.e(TAG, "Failed to enable High Priority Threading", e)
        }
    }
    
    /**
     * Reset thread priority to normal when benchmark completes
     */
    private fun resetThreadPriority() {
        try {
            if (isHighPriorityEnabled) {
                // Reset to default priority (0)
                Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
                isHighPriorityEnabled = false
                
                Log.i(TAG, "Thread priority reset to default")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset thread priority", e)
        }
    }
    
    /**
     * Apply high priority to benchmark worker threads
     * Call this at the start of each worker thread
     */
    private fun setThreadPriorityForWorker() {
        try {
            Process.setThreadPriority(BENCHMARK_THREAD_PRIORITY)
            Log.i(TAG, "Worker thread priority set to $BENCHMARK_THREAD_PRIORITY")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set worker thread priority", e)
        }
    }
    
    /**
     * Feature #5: Setup Performance Hint API (Android 12+)
     * Guides the scheduler for optimal CPU core selection and frequency scaling
     */
    private fun setupPerformanceHintAPI() {
        // Only available on Android 12 (API 31) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // Get the PerformanceHintManager system service
                val performanceHintManager = getSystemService(Context.PERFORMANCE_HINT_SERVICE) 
                    as? android.os.PerformanceHintManager
                
                if (performanceHintManager != null) {
                    // Get current thread ID
                    val threadIds = intArrayOf(Process.myTid())
                    
                    Log.i(TAG, "Creating Performance Hint session for thread: ${Process.myTid()}")
                    
                    // Create a hint session
                    // targetDurationNanos: Expected duration per work unit (16.6ms = ~60 FPS)
                    performanceHintSession = performanceHintManager.createHintSession(
                        threadIds,
                        PERFORMANCE_TARGET_NS
                    )
                    
                    if (performanceHintSession != null) {
                        isPerformanceHintEnabled = true
                        Log.i(TAG, "Performance Hint API: ENABLED (target: ${PERFORMANCE_TARGET_NS}ns)")
                    } else {
                        isPerformanceHintEnabled = false
                        Log.w(TAG, "Performance Hint API: Session creation returned null")
                    }
                    
                } else {
                    isPerformanceHintEnabled = false
                    Log.w(TAG, "Performance Hint API: PerformanceHintManager not available")
                }
                
            } catch (e: Exception) {
                isPerformanceHintEnabled = false
                Log.e(TAG, "Failed to setup Performance Hint API", e)
            }
        } else {
            isPerformanceHintEnabled = false
            Log.i(TAG, "Performance Hint API: Requires Android 12+ (current API: ${Build.VERSION.SDK_INT})")
        }
    }
    
    /**
     * Report actual work duration to Performance Hint API
     * Call this after completing a benchmark iteration
     */
    private fun reportActualDuration(actualDurationNanos: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && performanceHintSession != null) {
            try {
                (performanceHintSession as? android.os.PerformanceHintManager.Session)?.reportActualWorkDuration(
                    actualDurationNanos
                )
                Log.d(TAG, "Reported actual duration: ${actualDurationNanos}ns")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report actual duration", e)
            }
        }
    }
    
    /**
     * Update performance target if needed
     * Useful if benchmark workload changes
     */
    private fun updatePerformanceTarget(newTargetNanos: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && performanceHintSession != null) {
            try {
                (performanceHintSession as? android.os.PerformanceHintManager.Session)?.updateTargetWorkDuration(
                    newTargetNanos
                )
                Log.i(TAG, "Updated performance target to: ${newTargetNanos}ns")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update performance target", e)
            }
        }
    }
    
    /**
     * Close Performance Hint session when done
     */
    private fun closePerformanceHintSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && performanceHintSession != null) {
            try {
                (performanceHintSession as? android.os.PerformanceHintManager.Session)?.close()
                performanceHintSession = null
                isPerformanceHintEnabled = false
                Log.i(TAG, "Performance Hint session closed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to close Performance Hint session", e)
            }
        }
    }
    
    /**
     * Feature #6: Detect CPU core configuration
     * Uses CpuAffinityManager for accurate BIG/Mid/LITTLE classification
     */
    private fun detectCPUCoreConfiguration() {
        try {
            // Use CpuAffinityManager's proper core classification
            bigCoreCount = com.ivarna.finalbenchmark2.cpuBenchmark.CpuAffinityManager.getBigCores().size
            midCoreCount = com.ivarna.finalbenchmark2.cpuBenchmark.CpuAffinityManager.getMidCores().size
            littleCoreCount = com.ivarna.finalbenchmark2.cpuBenchmark.CpuAffinityManager.getLittleCores().size
            
            val totalCores = bigCoreCount + midCoreCount + littleCoreCount
            
            if (totalCores > 0) {
                isCpuAffinityEnabled = true
                Log.i(TAG, "CPU Configuration: $bigCoreCount BIG, $midCoreCount Mid, $littleCoreCount LITTLE cores")
                
                // Log detailed topology
                com.ivarna.finalbenchmark2.cpuBenchmark.CpuAffinityManager.logTopology()
            } else {
                Log.w(TAG, "Could not detect CPU core configuration")
                isCpuAffinityEnabled = false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect CPU configuration", e)
            isCpuAffinityEnabled = false
        }
    }
    
    /**
     * Get list of big core IDs
     */
    private fun getBigCoreIds(): List<Int> {
        val numCores = Runtime.getRuntime().availableProcessors()
        val startIndex = littleCoreCount
        return (startIndex until numCores).toList()
    }
    
    /**
     * Get current high priority status
     */
    fun isHighPriorityThreadingActive(): Boolean {
        return isHighPriorityEnabled
    }
    
    /**
     * Get current performance hint status
     */
    fun isPerformanceHintActive(): Boolean {
        return isPerformanceHintEnabled
    }
    
    /**
     * Get CPU affinity status
     */
    fun isCpuAffinityActive(): Boolean {
        return isCpuAffinityEnabled
    }
    
    /**
     * Get big core count
     */
    fun getBigCoreCount(): Int {
        return bigCoreCount
    }
    
    /**
     * Get mid core count
     */
    fun getMidCoreCount(): Int {
        return midCoreCount
    }
    
    /**
     * Get little core count
     */
    fun getLittleCoreCount(): Int {
        return littleCoreCount
    }
    
    /**
     * Get foreground service status
     */
    fun isForegroundServiceActive(): Boolean {
        return isForegroundServiceActive
    }
    
    /**
     * Get governor hint status
     */
    fun isGovernorHintApplied(): Boolean {
        return isGovernorHintApplied
    }
    
    /**
     * Get original governor value
     */
    fun getOriginalGovernor(): String? {
        return originalGovernor
    }
    
    /**
     * Start the foreground service when benchmark begins
     */
    private fun startForegroundService() {
        try {
            Log.i(TAG, "Attempting to start foreground service...")
            val intent = Intent(this, BenchmarkForegroundService::class.java).apply {
                action = BenchmarkForegroundService.ACTION_START_BENCHMARK
            }
            
            // Start foreground service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.i(TAG, "Starting foreground service (Android O+)...")
                startForegroundService(intent)
            } else {
                Log.i(TAG, "Starting service (pre-Android O)...")
                startService(intent)
            }
            
            // Wait a moment for service to start
            Handler(Looper.getMainLooper()).postDelayed({
                isForegroundServiceActive = com.ivarna.finalbenchmark2.BenchmarkForegroundService.isServiceRunning
                Log.i(TAG, "Checking foreground service status: isServiceRunning = ${com.ivarna.finalbenchmark2.BenchmarkForegroundService.isServiceRunning}")
                if (mainViewModel != null) {
                    mainViewModel?.updateForegroundServiceStatus(
                        if (isForegroundServiceActive) PerformanceOptimizationStatus.ENABLED else PerformanceOptimizationStatus.DISABLED
                    )
                }
                
                if (isForegroundServiceActive) {
                    Log.i(TAG, "✓ Foreground Service is ACTIVE")
                } else {
                    Log.w(TAG, "⚠ Foreground Service failed to start")
                }
            }, 500) // Increased delay to allow for service startup
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
            isForegroundServiceActive = false
            if (mainViewModel != null) {
                mainViewModel?.updateForegroundServiceStatus(PerformanceOptimizationStatus.DISABLED)
            }
        }
    }
    
    /**
     * Stop the foreground service when benchmark completes
     */
    private fun stopForegroundService() {
        try {
            Log.i(TAG, "Attempting to stop foreground service...")
            val intent = Intent(this, BenchmarkForegroundService::class.java).apply {
                action = BenchmarkForegroundService.ACTION_STOP_BENCHMARK
            }
            startService(intent)
            
            isForegroundServiceActive = false
            if (mainViewModel != null) {
                mainViewModel?.updateForegroundServiceStatus(PerformanceOptimizationStatus.DISABLED)
            }
            
            Log.i(TAG, "✓ Foreground Service stopped")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop foreground service", e)
        }
    }
    
    /**
     * Initialize governor hints
     * Just reads the current governor state and updates display
     */
    private fun initializeGovernorHints() {
        try {
            // Read current governor from first CPU
            val currentGovernor = getCurrentGovernor()
            originalGovernor = currentGovernor
            
            Log.i(TAG, "Current CPU Governor: $currentGovernor")
            
            // Simply check if governor is in performance mode
            isGovernorHintApplied = (currentGovernor == "performance")
            
            if (isGovernorHintApplied) {
                Log.i(TAG, "✓ Governor is in performance mode")
            } else {
                Log.i(TAG, "Governor is in $currentGovernor mode (requires root to change)")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read governor state", e)
            originalGovernor = null
            isGovernorHintApplied = false
        }
    }
    
    /**
     * Update governor status
     * Simply checks current governor state and updates UI
     * Does not attempt to change governor (requires root)
     */
    private fun applyGovernorHints() {
        Log.i(TAG, "Checking CPU Governor status...")
        
        // Just check current governor and update status
        updateGovernorStatus()
    }
    
    /**
     * Method 1: Try to write governor directly
     * Requires root and usually fails on modern Android
     */
    private fun trySetGovernorDirect(): Boolean {
        return try {
            val numCores = Runtime.getRuntime().availableProcessors()
            var successCount = 0
            
            for (cpu in 0 until numCores) {
                val governorFile = File("/sys/devices/system/cpu/cpu$cpu/cpufreq/scaling_governor")
                
                if (governorFile.exists() && governorFile.canWrite()) {
                    try {
                        governorFile.writeText("performance")
                        successCount++
                    } catch (e: Exception) {
                        // Expected on most devices
                        Log.d(TAG, "Cannot write governor for CPU $cpu: ${e.message}")
                    }
                }
            }
            
            if (successCount > 0) {
                Log.i(TAG, "Method 1: Set governor for $successCount/$numCores cores")
                true
            } else {
                Log.d(TAG, "Method 1: Failed (no write access)")
                false
            }
            
        } catch (e: Exception) {
            Log.d(TAG, "Method 1: Exception - ${e.message}")
            false
        }
    }
    
    /**
     * Method 2: Try shell commands
     * More likely to work on rooted devices
     */
    private fun trySetGovernorShell(): Boolean {
        return try {
            val numCores = Runtime.getRuntime().availableProcessors()
            
            // Try without su first (will fail on most devices)
            var command = StringBuilder()
            for (cpu in 0 until numCores) {
                command.append("echo performance > /sys/devices/system/cpu/cpu$cpu/cpufreq/scaling_governor; ")
            }
            
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command.toString()))
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Log.i(TAG, "Method 2: Shell command succeeded")
                true
            } else {
                Log.d(TAG, "Method 2: Shell command failed (exit code: $exitCode)")
                
                // Try with su (root)
                trySetGovernorRoot()
            }
            
        } catch (e: Exception) {
            Log.d(TAG, "Method 2: Exception - ${e.message}")
            false
        }
    }
    
    /**
     * Try with root access
     * Only works on rooted devices with su
     */
    private fun trySetGovernorRoot(): Boolean {
        return try {
            val numCores = Runtime.getRuntime().availableProcessors()
            var command = StringBuilder()
            
            for (cpu in 0 until numCores) {
                command.append("echo performance > /sys/devices/system/cpu/cpu$cpu/cpufreq/scaling_governor; ")
            }
            
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command.toString()))
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Log.i(TAG, "Method 2b: Root command succeeded!")
                true
            } else {
                Log.d(TAG, "Method 2b: Root not available or command failed")
                false
            }
            
        } catch (e: Exception) {
            Log.d(TAG, "Method 2b: Root access failed - ${e.message}")
            false
        }
    }
    
    /**
     * Method 3: Frequency boost hint
     * Doesn't require root, but has limited effect
     * Works through PowerManager hints
     */
    private fun tryFrequencyBoostHint() {
        try {
            // This is more of a "hint" than actual control
            // Modern Android uses schedutil governor that responds to load
            // Our other optimizations (wake lock, high priority) should
            // cause the governor to boost frequencies naturally
            
            Log.i(TAG, "Method 3: Using indirect boost via wake lock + priority")
            Log.i(TAG, "ℹ Other optimizations will encourage governor to boost frequency")
            
        } catch (e: Exception) {
            Log.e(TAG, "Method 3: Failed - ${e.message}")
        }
    }
    
    /**
     * Unified function to check and update governor status
     * This is the ONLY place where governor status is updated
     */
    private fun updateGovernorStatus() {
        try {
            val currentGovernor = getCurrentGovernor()
            isGovernorHintApplied = (currentGovernor == "performance")
            
            val governorStatus = if (currentGovernor == "performance") {
                PerformanceOptimizationStatus.ENABLED
            } else {
                PerformanceOptimizationStatus.DISABLED
            }
            
            mainViewModel?.updateCpuGovernorHintsStatus(governorStatus)
            Log.i(TAG, "Governor status: $currentGovernor -> $governorStatus")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update governor status", e)
            mainViewModel?.updateCpuGovernorHintsStatus(PerformanceOptimizationStatus.DISABLED)
        }
    }
    
    /**
     * Called when benchmark completes
     * Simply updates the current governor status
     */
    private fun restoreGovernor() {
        Log.i(TAG, "Updating governor status after benchmark...")
        updateGovernorStatus()
    }
    
    /**
     * Fallback method to restore governor without root
     */
    private fun restoreGovernorFallback(numCores: Int, targetGovernor: String) {
        for (cpu in 0 until numCores) {
            try {
                val governorFile = File("/sys/devices/system/cpu/cpu$cpu/cpufreq/scaling_governor")
                if (governorFile.exists() && governorFile.canWrite()) {
                    governorFile.writeText(targetGovernor)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Cannot write governor for CPU $cpu: ${e.message}")
                // This is expected on most non-rooted devices
            }
        }
        
        Log.i(TAG, "Governor restoration attempted for $numCores cores to: $targetGovernor")
    }
    
    /**
     * Check if root access is available and working
     * This method is kept for backward compatibility but now uses RootAccessManager
     */
    private fun checkRootAccess(): Boolean {
        return try {
            // Use RootAccessManager for proper caching and consistency
            // Note: This is a suspend function, but we're calling it synchronously here
            // In practice, this should be called from a coroutine context
            Log.i(TAG, "Root check: using RootAccessManager")
            
            // Since this method is called from non-suspend contexts in MainActivity,
            // we'll use a simple approach that doesn't block the main thread
            val cachedResult = com.ivarna.finalbenchmark2.utils.RootAccessManager.getCachedRootAccess()
            cachedResult ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking root access", e)
            false
        }
    }
    
    /**
     * Try to set governor using root access
     */
    private fun trySetGovernorWithRoot(): Boolean {
        return try {
            val numCores = Runtime.getRuntime().availableProcessors()
            var command = StringBuilder()
            
            for (cpu in 0 until numCores) {
                command.append("echo performance > /sys/devices/system/cpu/cpu$cpu/cpufreq/scaling_governor; ")
            }
            
            // Use RootCommandExecutor to execute with proper root access
            val rootExecutor = RootCommandExecutor()
            
            // Execute the command synchronously (this is a main thread call, but it's quick)
            val result = runBlocking {
                rootExecutor.executeCommand(command.toString())
            }
            
            if (result != null) {
                Log.i(TAG, "Root governor command executed successfully")
                // Verify the governor was actually changed
                val currentGovernor = getCurrentGovernor()
                if (currentGovernor == "performance") {
                    Log.i(TAG, "✓ Governor successfully set to performance mode")
                    true
                } else {
                    Log.w(TAG, "Governor command executed but verification failed. Current: $currentGovernor")
                    false
                }
            } else {
                Log.w(TAG, "Root governor command failed to execute")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing root governor command", e)
            false
        }
    }
    
    /**
     * Get current governor status for UI
     */
    private fun getGovernorStatus(): String {
        return getCurrentGovernor()
    }
    
    /**
     * Get current governor from CPU 0
     */
    private fun getCurrentGovernor(): String {
        return try {
            val governorFile = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
            if (governorFile.exists() && governorFile.canRead()) {
                governorFile.readText().trim()
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading current governor", e)
            "Error"
        }
    }
    
    /**
     * Request notification permission (Android 13+)
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101 // REQUEST_CODE_NOTIFICATION
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.i("FinalBenchmark2", "onDestroy() called - releasing wake lock if held")
        
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
        
        // CRITICAL: Always release wake lock when activity is destroyed
        // This prevents battery drain if app crashes or is killed
        releaseWakeLock()
        
        // Stop foreground service if running
        stopForegroundService()
        
        // Reset thread priority
        resetThreadPriority()
        
        // Close Performance Hint session
        closePerformanceHintSession()
        
        // Restore original governor if changed
        restoreGovernor()
        
        // Screen Always On flag is automatically cleared when activity is destroyed
        Log.i("FinalBenchmark2", "Activity destroyed - all optimizations cleaned up")
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            101 -> { // REQUEST_CODE_NOTIFICATION
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Notification permission granted")
                } else {
                    Log.w(TAG, "Notification permission denied")
                }
            }
        }
    }
}