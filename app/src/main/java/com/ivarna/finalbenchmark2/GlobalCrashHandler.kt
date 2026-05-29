package com.ivarna.finalbenchmark2

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
    private const val CRASH_FILE = "last_crash.txt"

    fun handle(context: android.content.Context, throwable: Throwable, defaultHandler: Thread.UncaughtExceptionHandler?) {
        try {
            val stacktrace = formatCrash(throwable)
            File(context.filesDir, CRASH_FILE).writeText(stacktrace)
            val intent = Intent(context, CrashActivity::class.java).apply {
                putExtra("stack_trace", stacktrace)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
            Process.killProcess(Process.myPid())
            System.exit(10)
        } catch (e: Exception) {
            Log.e("CrashHandler", "Crash handler itself crashed", e)
            defaultHandler?.uncaughtException(Thread.currentThread(), throwable)
        }
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
        pw.println("--- Full Stack Trace ---")
        throwable.printStackTrace(pw)
        pw.println()
        pw.println("--- Root Cause Chain ---")
        var cause = throwable.cause
        var depth = 0
        while (cause != null && depth < 10) {
            pw.println("Cause #${++depth}: ${cause.javaClass.name}: ${cause.message}")
            cause.stackTrace.take(8).forEach { pw.println("  at $it") }
            cause = cause.cause
            pw.println()
        }
        pw.close()
        return sw.toString()
    }

    fun hasCrashReport(context: android.content.Context): Boolean =
        File(context.filesDir, CRASH_FILE).exists()

    fun getCrashReport(context: android.content.Context): String? =
        File(context.filesDir, CRASH_FILE).takeIf { it.exists() }?.readText()

    fun clearCrashReport(context: android.content.Context) {
        File(context.filesDir, CRASH_FILE).delete()
    }
}
