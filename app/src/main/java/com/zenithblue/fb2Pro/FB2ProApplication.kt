package com.zenithblue.fb2Pro

import android.app.Application

class FB2ProApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this, CrashActivity::class.java)
    }
}
