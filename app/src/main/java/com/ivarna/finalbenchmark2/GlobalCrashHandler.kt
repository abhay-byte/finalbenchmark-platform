package com.ivarna.finalbenchmark2

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GlobalCrashHandler {
    private const val CRASH_FILE = "last_crash.txt"

    fun saveCrash(context: Context, throwable: Throwable) {
        try {
            val file = File(context.filesDir, CRASH_FILE)
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            pw.println("=== FinalBenchmark 2 Crash Report ===")
            pw.println("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            pw.println("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            pw.println("OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            pw.println("Hardware: ${Build.HARDWARE}")
            pw.println()
            pw.println("--- Stack Trace ---")
            throwable.printStackTrace(pw)
            pw.println()
            pw.println("--- Caused By (Root) ---")
            var cause = throwable.cause
            var depth = 0
            while (cause != null && depth < 10) {
                pw.println("Cause #${++depth}: ${cause.javaClass.name}: ${cause.message}")
                cause.stackTrace.take(5).forEach { pw.println("  at $it") }
                cause = cause.cause
                pw.println()
            }
            pw.close()
            file.writeText(sw.toString())
            android.util.Log.e("CrashHandler", "Crash saved to ${file.absolutePath}")
        } catch (_: Exception) {}
    }

    fun hasCrashReport(context: Context): Boolean {
        return File(context.filesDir, CRASH_FILE).exists()
    }

    fun getCrashReport(context: Context): String? {
        val file = File(context.filesDir, CRASH_FILE)
        return if (file.exists()) file.readText() else null
    }

    fun clearCrashReport(context: Context) {
        File(context.filesDir, CRASH_FILE).delete()
    }
}
