package com.ivarna.finalbenchmark2

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Foreground Service for CPU Benchmark
 * Maintains high priority during benchmark execution
 */
class BenchmarkForegroundService : Service() {

    companion object {
        private const val TAG = "FinalBenchmark2"
        private const val NOTIFICATION_ID = 9001
        private const val CHANNEL_ID = "benchmark_service_channel"
        private const val CHANNEL_NAME = "CPU Benchmark Service"
        
        // Service state
        var isServiceRunning = false
            private set
        
        // Actions
        const val ACTION_START_BENCHMARK = "com.ivarna.finalbenchmark2.START_BENCHMARK"
        const val ACTION_STOP_BENCHMARK = "com.ivarna.finalbenchmark2.STOP_BENCHMARK"
        
        fun start(context: Context) {
            val intent = Intent(context, BenchmarkForegroundService::class.java)
            intent.action = ACTION_START_BENCHMARK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, BenchmarkForegroundService::class.java)
            intent.action = ACTION_STOP_BENCHMARK
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "BenchmarkForegroundService: onCreate()")
        
        // Create notification channel (required for Android 8.0+)
        createNotificationChannel()
        
        // Boost service process priority
        boostProcessPriority()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "BenchmarkForegroundService: onStartCommand() action=${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_BENCHMARK -> {
                startForegroundService()
            }
            ACTION_STOP_BENCHMARK -> {
                stopForegroundService()
            }
            else -> {
                // Default: start foreground service
                startForegroundService()
            }
        }
        
        // START_STICKY: Service will be restarted if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        Log.i(TAG, "BenchmarkForegroundService: onDestroy()")
    }

    /**
     * Create notification channel for Android 8.0+
     * Required before showing any notifications
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when CPU benchmark is running"
                setShowBadge(false)
                // Don't make sound or vibration
                setSound(null, null)
                enableVibration(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            
            Log.i(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }

    /**
     * Start the service in foreground mode with notification
     */
    private fun startForegroundService() {
        if (isServiceRunning) {
            Log.i(TAG, "Service already running, updating notification")
            updateNotification("Benchmark in progress...")
            return
        }
        
        val notification = createNotification(
            "CPU Benchmark Active",
            "Running performance tests..."
        )
        
        // Start foreground service with appropriate service type for Android 14+
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ requires specifying foreground service type
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10-13
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
                )
            } else {
                // Android 9 and below
                startForeground(NOTIFICATION_ID, notification)
            }
            isServiceRunning = true
            Log.i(TAG, "✓ Foreground Service STARTED - Maximum priority active")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
            // Fallback: try without service type
            try {
                startForeground(NOTIFICATION_ID, notification)
                isServiceRunning = true
                Log.i(TAG, "✓ Foreground Service STARTED (fallback)")
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback also failed", e2)
            }
        }
    }

    /**
     * Stop the foreground service
     */
    private fun stopForegroundService() {
        if (!isServiceRunning) {
            Log.i(TAG, "Service not running, nothing to stop")
            return
        }
        
        // Stop foreground mode and remove notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        
        isServiceRunning = false
        stopSelf()
        
        Log.i(TAG, "✓ Foreground Service STOPPED")
    }

    /**
     * Create notification for foreground service
     */
    private fun createNotification(title: String, message: String): Notification {
        // Intent to open app when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use your app icon
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Can't be dismissed by user
            .setPriority(NotificationCompat.PRIORITY_LOW) // Don't disturb user
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * Update notification text while service is running
     */
    private fun updateNotification(message: String) {
        val notification = createNotification("CPU Benchmark Active", message)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Boost process priority to maximum
     * Called when service starts
     */
    private fun boostProcessPriority() {
        try {
            // Set process priority to foreground
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            Log.i(TAG, "Service process priority boosted to URGENT_AUDIO")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to boost service priority", e)
        }
    }
}