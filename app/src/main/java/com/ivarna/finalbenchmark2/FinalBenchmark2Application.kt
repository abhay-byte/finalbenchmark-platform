package com.ivarna.finalbenchmark2

import android.app.Application

class FinalBenchmark2Application : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this, CrashActivity::class.java)
    }
}
