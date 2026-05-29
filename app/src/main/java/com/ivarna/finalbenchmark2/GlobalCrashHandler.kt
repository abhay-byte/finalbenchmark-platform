package com.ivarna.finalbenchmark2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GlobalCrashHandler {

    private const val TAG = "GlobalCrashHandler"
    private const val CRASH_FILE = "last_crash.txt"
    private const val PREFS_FILE = "crash_handler_prefs"
    private const val PREF_LAST_CRASH_TS = "last_crash_timestamp"
    // If another crash happens within 3 seconds, skip the crash screen (avoid infinite loop)
    private const val MIN_MS_BETWEEN_CRASHES = 3_000L
    // Binder transaction limit is 1 MB; stay well under it
    private const val MAX_STACKTRACE_BYTES = 131_071

    fun handle(
        context: Context,
        throwable: Throwable,
        lastActivity: Activity?,
        isInForeground: Boolean,
        defaultHandler: Thread.UncaughtExceptionHandler?
    ) {
        try {
            // --- Loop protection: don't restart if we already crashed very recently ---
            if (hasCrashedRecently(context)) {
                Log.e(TAG, "Crash loop detected — delegating to system handler")
                lastActivity?.finish()
                defaultHandler?.uncaughtException(Thread.currentThread(), throwable)
                return
            }
            setLastCrashTimestamp(context)

            // --- Check for init crash (handleBindApplication on stack) ---
            if (isInitCrash(throwable)) {
                Log.e(TAG, "Init crash detected — delegating to system handler")
                defaultHandler?.uncaughtException(Thread.currentThread(), throwable)
                return
            }

            val stacktrace = formatCrash(throwable)

            // Save to file for reference / logcat debugging
            try { File(context.filesDir, CRASH_FILE).writeText(stacktrace) } catch (_: Exception) {}

            // Only show crash screen if the app is (or was just) in foreground
            if (isInForeground) {
                val intent = Intent(context, CrashActivity::class.java).apply {
                    putExtra("stack_trace", stacktrace.take(MAX_STACKTRACE_BYTES))
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )
                }

                // Finish the last visible activity first to avoid the "ghost" behind the crash screen
                lastActivity?.finish()

                context.startActivity(intent)
            } else {
                Log.e(TAG, "Crash in background — not showing crash screen:\n$stacktrace")
                defaultHandler?.uncaughtException(Thread.currentThread(), throwable)
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Crash handler itself failed", e)
        } finally {
            // Always kill main process — crash screen (same process, new task) was already started
            Process.killProcess(Process.myPid())
            System.exit(10)
        }
    }

    private fun isInitCrash(throwable: Throwable): Boolean {
        var t: Throwable? = throwable
        while (t != null) {
            for (el in t.stackTrace) {
                if (el.className == "android.app.ActivityThread" &&
                    el.methodName == "handleBindApplication") return true
            }
            t = t.cause
        }
        return false
    }

    private fun formatCrash(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        pw.println("=== FinalBenchmark 2 Crash Report ===")
        pw.println("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        pw.println("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        pw.println("OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        pw.println("Hardware: ${Build.HARDWARE}")
        pw.println()
        throwable.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }

    private fun hasCrashedRecently(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val last = prefs.getLong(PREF_LAST_CRASH_TS, -1L)
        val now = System.currentTimeMillis()
        return last in 1 until now && now - last < MIN_MS_BETWEEN_CRASHES
    }

    private fun setLastCrashTimestamp(context: Context) {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit().putLong(PREF_LAST_CRASH_TS, System.currentTimeMillis()).commit()
    }

    // For MainNavigation fallback dialog
    fun hasCrashReport(context: Context) = File(context.filesDir, CRASH_FILE).exists()
    fun getCrashReport(context: Context) =
        File(context.filesDir, CRASH_FILE).takeIf { it.exists() }?.readText()
    fun clearCrashReport(context: Context) { File(context.filesDir, CRASH_FILE).delete() }
}
