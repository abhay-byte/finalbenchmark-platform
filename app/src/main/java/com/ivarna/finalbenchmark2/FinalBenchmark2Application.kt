package com.ivarna.finalbenchmark2

import android.app.Activity
import android.app.Application
import java.lang.ref.WeakReference

class FinalBenchmark2Application : Application() {

    override fun onCreate() {
        super.onCreate()
        GlobalCrashHandler.install(this, CrashActivity::class.java)
    }
}
