package com.ivarna.finalbenchmark2.utils

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import android.view.WindowInsets
import androidx.core.content.ContextCompat
import kotlin.math.sqrt

data class DisplayInfo(
    val resolution: String,
    val density: String,
    val physicalSize: String,
    val aspectRatio: String,
    val exactDpiX: String,
    val exactDpiY: String,
    val realMetrics: String,
    val refreshRate: String,
    val maxRefreshRate: String,
    val hdrSupport: String,
    val hdrTypes: List<String>,
    val wideColorGamut: Boolean,
    val orientation: String,
    val rotation: Int,
    val brightnessLevel: String?,
    val screenTimeout: String?,
    val safeAreaInsets: String,
    val displayCutout: String
)

class DisplayUtils(private val context: Context) {
    
    fun getDisplayInfo(): DisplayInfo {
        val windowManager = ContextCompat.getSystemService(context, WindowManager::class.java) as WindowManager
        
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            windowManager.defaultDisplay
        }
        
        val displayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.getRealMetrics(displayMetrics)
        } else {
            @Suppress("DEPRECATION")
            display?.getRealMetrics(displayMetrics)
        }
        
        val displaySize = android.graphics.Point().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display?.getRealSize(this)
            } else {
                @Suppress("DEPRECATION")
                display?.getSize(this)
            }
        }
        
        // Calculate physical size in inches
        val xInches = displayMetrics.widthPixels / displayMetrics.xdpi.toDouble()
        val yInches = displayMetrics.heightPixels / displayMetrics.ydpi.toDouble()
        val physicalSizeInches = sqrt(xInches * xInches + yInches * yInches)
        
        // Calculate aspect ratio
        val aspectRatio = calculateAspectRatio(displaySize.x, displaySize.y)
        
        // Get refresh rate information
        val refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            "${display?.refreshRate?.toString()?.toFloat()?.toInt()} Hz"
        } else {
            "${display?.refreshRate?.toInt()} Hz"
        }
        
        val maxRefreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // Get all supported display modes and find the maximum refresh rate
                val maxMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    display?.supportedModes?.maxByOrNull { it.refreshRate }
                } else {
                    // For older versions, use refresh rate as max since supportedModes isn't available
                    null
                }
                if (maxMode != null) {
                    val maxRate = maxMode.refreshRate.toString().toFloat().toInt().toString()
                    "${maxRate} Hz"
                } else {
                    // For older versions or if maxMode is null, just return the current refresh rate
                    display?.refreshRate?.toString()?.toFloat()?.toInt()?.toString()?.let { "${it} Hz" } ?: "Unknown"
                }
            } catch (e: Exception) {
                "Unknown"
            }
        } else {
            "Unknown"
        }
        
        // Get HDR capabilities
        val (hdrSupport, hdrTypes) = getHdrCapabilities(display)
        
        // Get wide color gamut support
        val wideColorGamut = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.resources.configuration.isScreenWideColorGamut
        } else {
            false
        }
        
        // Get orientation and rotation
        val orientation = when (context.resources.configuration.orientation) {
            android.content.res.Configuration.ORIENTATION_PORTRAIT -> "Portrait"
            android.content.res.Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
            else -> "Unknown"
        }
        
        val rotation = when (display?.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> -1
        }
        
        // Get display cutout information (notch, etc.)
        val displayCutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // On API 30+, we can access display cutout differently
                    "Available (API ${Build.VERSION.SDK_INT})"
                } else {
                    // For API 28-29, display cutout is available
                    "Available (API ${Build.VERSION.SDK_INT})"
                }
            } catch (e: Exception) {
                "Not available"
            }
        } else {
            "Not available (API < 28)"
        }
        
        // Get safe area insets
        val safeAreaInsets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // On API 30+, we can access insets differently
                    "Available (API ${Build.VERSION.SDK_INT})"
                } else {
                    "Available (API ${Build.VERSION.SDK_INT})"
                }
            } catch (e: Exception) {
                "Not available"
            }
        } else {
            "Not available (API < 28)"
        }
        
        return DisplayInfo(
            resolution = "${displaySize.x} x ${displaySize.y}",
            density = "${displayMetrics.density}x (${displayMetrics.densityDpi} DPI)",
            physicalSize = "${String.format("%.1f", physicalSizeInches)}\"",
            aspectRatio = aspectRatio,
            exactDpiX = "${displayMetrics.xdpi} DPI",
            exactDpiY = "${displayMetrics.ydpi} DPI",
            realMetrics = "w${displaySize.x}dp x h${displaySize.y}dp",
            refreshRate = refreshRate,
            maxRefreshRate = maxRefreshRate,
            hdrSupport = hdrSupport,
            hdrTypes = hdrTypes,
            wideColorGamut = wideColorGamut,
            orientation = orientation,
            rotation = rotation,
            brightnessLevel = null, // Would require additional permission to access
            screenTimeout = null, // Would require additional permission to access
            safeAreaInsets = safeAreaInsets,
            displayCutout = displayCutout
        )
    }
    
    private fun getHdrCapabilities(display: Display?): Pair<String, List<String>> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && display != null) {
            try {
                val hdrCaps = display.hdrCapabilities
                if (hdrCaps != null) {
                    val supportedHdrTypes = hdrCaps.supportedHdrTypes
                    val hdrTypeList = mutableListOf<String>()
                    
                    for (type in supportedHdrTypes) {
                        when (type) {
                            Display.HdrCapabilities.HDR_TYPE_HDR10 -> hdrTypeList.add("HDR10")
                            Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> hdrTypeList.add("HDR10+")
                            Display.HdrCapabilities.HDR_TYPE_HLG -> hdrTypeList.add("HLG")
                            Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> hdrTypeList.add("Dolby Vision")
                            else -> hdrTypeList.add("Unknown HDR ($type)")
                        }
                    }
                    
                    return Pair("Yes (${hdrTypeList.size} type${if (hdrTypeList.size > 1) "s" else ""})", hdrTypeList)
                }
            } catch (e: Exception) {
                // HDR capabilities might not be available on some devices
                return Pair("Error reading HDR capabilities", emptyList())
            }
        }
        
        return Pair("No", emptyList())
    }
    
    private fun calculateAspectRatio(width: Int, height: Int): String {
        val gcd = gcd(width, height)
        val aspectWidth = width / gcd
        val aspectHeight = height / gcd
        
        // Common aspect ratios for better readability
        val calculatedRatio = aspectWidth.toDouble() / aspectHeight.toDouble()
        return when {
            // Check for common aspect ratios
            calculatedRatio == 1.0 -> "1:1" // Square
            (kotlin.math.abs(calculatedRatio - 4.0/3.0) < 0.01) -> "4:3"
            (kotlin.math.abs(calculatedRatio - 3.0/2.0) < 0.01) -> "3:2"
            (kotlin.math.abs(calculatedRatio - 16.0/10.0) < 0.01) -> "16:10"
            (kotlin.math.abs(calculatedRatio - 16.0/9.0) < 0.01) -> "16:9"
            (kotlin.math.abs(calculatedRatio - 1.85) < 0.01) -> "1.85:1"
            (kotlin.math.abs(calculatedRatio - 21.0/9.0) < 0.01) -> "21:9"
            (kotlin.math.abs(calculatedRatio - 2.37) < 0.01) -> "2.37:1" // Common for ultrawide
            else -> "$aspectWidth:$aspectHeight"
        }
    }
    
    private fun gcd(a: Int, b: Int): Int {
        return if (b == 0) a else gcd(b, a % b)
    }
}