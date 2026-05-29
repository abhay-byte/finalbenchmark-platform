package com.ivarna.finalbenchmark2

import android.app.Application

class FinalBenchmark2Application : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            GlobalCrashHandler.handle(applicationContext, throwable, defaultHandler)
        }
    }
}
