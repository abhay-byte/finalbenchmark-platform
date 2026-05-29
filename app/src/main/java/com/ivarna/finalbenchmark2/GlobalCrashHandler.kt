package com.ivarna.finalbenchmark2

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class GlobalCrashHandler(
    private val context: Context,
    private val crashActivityClass: Class<*>,
    private val defaultHandler: Thread.UncaughtExceptionHandler
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            Log.e(TAG, "App crashed, launching crash screen", throwable)

            val stacktrace = StringWriter().let { sw ->
                PrintWriter(sw).use { pw ->
                    pw.println("=== FinalBenchmark 2 Crash ===")
                    pw.println("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                    pw.println("OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    pw.println()
                    throwable.printStackTrace(pw)
                }
                saveToFile(context, sw.toString())
                sw.toString()
            }

            val intent = Intent(context, crashActivityClass).apply {
                putExtra(EXTRA_STACK_TRACE, stacktrace)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
            Process.killProcess(Process.myPid())
        } catch (e: Exception) {
            Log.e(TAG, "Crash handler failed to launch activity, delegating to system", e)
            defaultHandler.uncaughtException(thread, throwable)
        }
    }

    companion object {
        private const val TAG = "GlobalCrashHandler"
        private const val EXTRA_STACK_TRACE = "stack_trace"
        private const val CRASH_FILE = "last_crash.txt"

        fun install(context: Context, crashActivityClass: Class<*>) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
                ?: return
            if (defaultHandler is GlobalCrashHandler) return // already installed
            Thread.setDefaultUncaughtExceptionHandler(
                GlobalCrashHandler(context.applicationContext, crashActivityClass, defaultHandler)
            )
        }

        fun getStackTrace(intent: Intent): String? =
            intent.getStringExtra(EXTRA_STACK_TRACE)

        private fun saveToFile(context: Context, stacktrace: String) {
            try { File(context.filesDir, CRASH_FILE).writeText(stacktrace) } catch (_: Exception) {}
        }

        fun hasCrashReport(context: Context) = File(context.filesDir, CRASH_FILE).exists()
        fun getCrashReport(context: Context) =
            File(context.filesDir, CRASH_FILE).takeIf { it.exists() }?.readText()
        fun clearCrashReport(context: Context) { File(context.filesDir, CRASH_FILE).delete() }
    }
}
