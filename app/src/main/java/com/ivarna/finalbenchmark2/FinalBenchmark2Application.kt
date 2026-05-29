package com.ivarna.finalbenchmark2

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

class FinalBenchmark2Application : Application() {

    override fun onCreate() {
        super.onCreate()

        // Track the last foreground activity so crash handler can finish() it
        var lastActivity: WeakReference<Activity> = WeakReference(null)
        var startedActivities = 0

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, b: Bundle?) {
                if (a !is CrashActivity) lastActivity = WeakReference(a)
            }
            override fun onActivityStarted(a: Activity) { startedActivities++ }
            override fun onActivityResumed(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivityStopped(a: Activity) { startedActivities-- }
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            GlobalCrashHandler.handle(
                applicationContext,
                throwable,
                lastActivity.get(),
                startedActivities > 0,
                defaultHandler
            )
        }
    }
}
