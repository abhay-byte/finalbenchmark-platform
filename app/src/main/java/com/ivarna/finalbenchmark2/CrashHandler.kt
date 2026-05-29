package com.ivarna.finalbenchmark2

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference
import java.util.Date

object CrashHandler {

    private const val TAG = "CrashHandler"
    private const val EXTRA_STACK_TRACE = "stack_trace"
    private const val MAX_STACK_TRACE_SIZE = 131071
    private const val PREFS_FILE = "crash_handler"
    private const val PREFS_TIMESTAMP = "last_crash_timestamp"
    private const val MIN_MS_BETWEEN_CRASHES = 3000L
    private const val TIME_TO_CONSIDER_FOREGROUND_MS = 500L
    private const val CRASH_FILE = "last_crash.txt"

    private lateinit var app: Application
    private val activityLog = ArrayDeque<String>(50)
    private var lastActivity: WeakReference<Activity> = WeakReference(null)
    private var lastActivityTimestamp: Long = 0L
    private var isInBackground = true

    private var installed = false

    fun install(context: Context, crashActivityClass: Class<out Activity>) {
        if (installed) return
        installed = true
        app = context.applicationContext as Application

        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "App has crashed, executing crash handler", throwable)

            if (hasCrashedInTheLastSeconds(app)) {
                Log.e(TAG, "Crash loop detected, delegating to system handler")
                oldHandler?.uncaughtException(thread, throwable)
                return@setDefaultUncaughtExceptionHandler
            }
            setLastCrashTimestamp(app)

            if (isStackTraceConflictive(throwable, crashActivityClass)) {
                Log.e(TAG, "Conflictive stack trace, delegating to system handler")
                oldHandler?.uncaughtException(thread, throwable)
                return@setDefaultUncaughtExceptionHandler
            }

            if (!isInBackground || lastActivityTimestamp >= Date().time - TIME_TO_CONSIDER_FOREGROUND_MS) {
                val intent = Intent(app, crashActivityClass)
                var stackTraceString: String
                StringWriter().use { sw ->
                    PrintWriter(sw).use { pw -> throwable.printStackTrace(pw) }
                    stackTraceString = sw.toString()
                    if (stackTraceString.length > MAX_STACK_TRACE_SIZE) {
                        val disclaimer = " [stack trace too large]"
                        stackTraceString = stackTraceString.substring(0, MAX_STACK_TRACE_SIZE - disclaimer.length) + disclaimer
                    }
                }
                intent.putExtra(EXTRA_STACK_TRACE, stackTraceString)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                saveToFile(stackTraceString)
                app.startActivity(intent)
            } else {
                Log.e(TAG, "Background crash, delegating to system handler")
                oldHandler?.uncaughtException(thread, throwable)
                return@setDefaultUncaughtExceptionHandler
            }

            // finish last activity AFTER starting crash activity (matches CAOC line 199-204)
            lastActivity.get()?.finish()
            lastActivity.clear()
            killCurrentProcess()
        }

        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            var started = 0
            override fun onActivityCreated(a: Activity, s: Bundle?) {
                if (a !is CrashActivity) {
                    lastActivity = WeakReference(a)
                    lastActivityTimestamp = Date().time
                }
            }
            override fun onActivityStarted(a: Activity) { started++; isInBackground = started == 0 }
            override fun onActivityResumed(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivityStopped(a: Activity) { started--; isInBackground = started == 0 }
            override fun onActivitySaveInstanceState(a: Activity, s: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })

        Log.i(TAG, "Crash handler installed")
    }

    fun getStackTrace(intent: Intent): String? = intent.getStringExtra(EXTRA_STACK_TRACE)

    private fun isStackTraceConflictive(throwable: Throwable, crashActivityClass: Class<out Activity>): Boolean {
        val process = try {
            BufferedReader(FileReader("/proc/self/cmdline")).use { it.readLine().trim() }
        } catch (_: Exception) { null }

        if (process != null && process.endsWith(":crash")) {
            return true // crashed inside crash activity process
        }

        var t: Throwable? = throwable
        while (t != null) {
            for (el in t.stackTrace) {
                if (el.className == "android.app.ActivityThread" &&
                    el.methodName == "handleBindApplication"
                ) return true // init crash
            }
            t = t.cause
        }
        return false
    }

    private fun killCurrentProcess() {
        Process.killProcess(Process.myPid())
        System.exit(10)
    }

    private fun hasCrashedInTheLastSeconds(ctx: Context): Boolean {
        val last = ctx.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).getLong(PREFS_TIMESTAMP, -1L)
        val now = Date().time
        return last in 1 until now && now - last < MIN_MS_BETWEEN_CRASHES
    }

    private fun setLastCrashTimestamp(ctx: Context) {
        ctx.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit().putLong(PREFS_TIMESTAMP, Date().time).commit()
    }

    private fun saveToFile(stackTrace: String) {
        try { File(app.filesDir, CRASH_FILE).writeText(stackTrace) } catch (_: Exception) {}
    }

    fun hasCrashReport(ctx: Context) = File(ctx.filesDir, CRASH_FILE).exists()
    fun getCrashReport(ctx: Context) = File(ctx.filesDir, CRASH_FILE).takeIf { it.exists() }?.readText()
    fun clearCrashReport(ctx: Context) { File(ctx.filesDir, CRASH_FILE).delete() }
}
