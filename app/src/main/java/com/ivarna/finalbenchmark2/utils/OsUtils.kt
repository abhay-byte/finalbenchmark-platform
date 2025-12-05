package com.ivarna.finalbenchmark2.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class OsInfo(
    val androidVersion: String,
    val androidCodeName: String,
    val apiLevel: String,
    val securityPatch: String,
    val kernelVersion: String,
    val kernelArch: String,
    val buildId: String,
    val baseband: String,
    val bootloader: String,
    val javaVmVersion: String,
    val systemUptime: String,
    val isRooted: Boolean,
    val googlePlayServicesVersion: String
)

object OsUtils {
    
    fun getOsInfo(context: Context): OsInfo {
        val packageManager = context.packageManager
        val playServicesVer = try {
            val packageInfo = packageManager.getPackageInfo("com.google.android.gms", 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }

        return OsInfo(
            androidVersion = Build.VERSION.RELEASE,
            androidCodeName = getAndroidCodeName(Build.VERSION.SDK_INT),
            apiLevel = Build.VERSION.SDK_INT.toString(),
            securityPatch = if (Build.VERSION.SDK_INT >= 23) Build.VERSION.SECURITY_PATCH else "N/A",
            kernelVersion = getKernelVersion(),
            kernelArch = System.getProperty("os.arch") ?: "Unknown",
            buildId = Build.DISPLAY,
            baseband = getBasebandVersion(),
            bootloader = Build.BOOTLOADER,
            javaVmVersion = System.getProperty("java.vm.version") ?: "Unknown",
            systemUptime = formatUptime(SystemClock.elapsedRealtime()),
            isRooted = checkRootMethod(),
            googlePlayServicesVersion = playServicesVer
        )
    }

    private fun getAndroidCodeName(apiLevel: Int): String {
        return when (apiLevel) {
            24 -> "Nougat"
            25 -> "Nougat"
            26 -> "Oreo"
            27 -> "Oreo"
            28 -> "Pie"
            29 -> "Android 10"
            30 -> "Android 11"
            31 -> "Android 12"
            32 -> "Android 12L"
            33 -> "Android 13 (Tiramisu)"
            34 -> "Android 14 (UpsideDownCake)"
            35 -> "Android 15"
            else -> "Unknown"
        }
    }

    private fun getBasebandVersion(): String {
        return try {
            val process = Runtime.getRuntime().exec("getprop gsm.version.baseband")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine() ?: "Unknown"
            reader.close()
            result
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getKernelVersion(): String {
        return try {
            val process = Runtime.getRuntime().exec("uname -r")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine() ?: "Unknown"
            reader.close()
            result
        } catch (e: Exception) {
            // Fallback to reading from /proc/version
            try {
                val procVersion = File("/proc/version").readText().trim()
                // Extract kernel version from the first part of the string
                val parts = procVersion.split(" ")
                if (parts.isNotEmpty()) {
                    parts[2] // Usually the kernel version is the third element
                } else {
                    "Unknown"
                }
            } catch (e: Exception) {
                "Unknown"
            }
        }
    }

    private fun checkRootMethod(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/tmp/su"
        )
        return paths.any { File(it).exists() } || checkSuInPath()
    }

    private fun checkSuInPath(): Boolean {
        val path = System.getenv("PATH")
        if (!path.isNullOrEmpty()) {
            val paths = path.split(":")
            for (p in paths) {
                val su = File(p, "su")
                if (su.exists()) {
                    return true
                }
            }
        }
        return false
    }

    private fun formatUptime(millis: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}