package com.zenithblue.fb2Pro.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.MediaCodecList
import android.media.MediaDrm
import android.media.AudioDeviceInfo
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import android.util.Size
import androidx.core.content.ContextCompat
import java.io.File
import java.util.UUID

data class BatterySpec(
    val level: Int,
    val status: String,
    val technology: String,
    val temperature: Float,
    val voltage: Float,
    val health: String,
    val designCapacity: String
)

data class NetworkSpec(
    val networkType: String,
    val signalStrength: String,
    val wifiSpeed: String,
    val wifiFrequency: String,
    val wifiStandard: String,
    val bluetoothFeatures: List<String>,
    val nfcSupported: Boolean,
    val irBlasterSupported: Boolean
)

data class CameraSpec(
    val id: String,
    val direction: String,
    val resolution: String,
    val aperture: String,
    val focalLength: String,
    val capabilities: List<String>
)

data class MemoryStorageSpec(
    val ramTotal: String,
    val ramAvailable: String,
    val storageTotal: String,
    val storageAvailable: String
)

data class AudioMediaSpec(
    val speakers: List<String>,
    val widevineLevel: String,
    val supportedCodecs: List<String>
)

data class PeripheralsSpec(
    val biometricSupport: List<String>,
    val simSlots: Int,
    val vibrationSupport: Boolean,
    val usbOtg: Boolean,
    val displayHdr: List<String>,
    val systemArchitecture: List<String>
)

class HardwareUtils(private val context: Context) {

    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    private val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

    fun getBatterySpecs(): BatterySpec {
        val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = (level.toFloat() / scale.toFloat() * 100).toInt()
        
        val status = when (intent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)) {
            android.os.BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            android.os.BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            android.os.BatteryManager.BATTERY_STATUS_FULL -> "Full"
            else -> "Unknown"
        }
        
        val technology = intent?.getStringExtra(android.os.BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
        val temperature = (intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0f
        val voltage = (intent?.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000.0f
        
        val health = when (intent?.getIntExtra(android.os.BatteryManager.EXTRA_HEALTH, -1)) {
            android.os.BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            android.os.BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            android.os.BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            android.os.BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            android.os.BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
            android.os.BatteryManager.BATTERY_HEALTH_UNKNOWN -> "Unknown"
            else -> "Unknown"
        }

        // Try to get design capacity using reflection
        val designCapacity = try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val powerProfileInstance = powerProfileClass.getConstructor(Context::class.java).newInstance(context)
            val batteryCapacityMethod = powerProfileClass.getMethod("getBatteryCapacity")
            val capacity = batteryCapacityMethod.invoke(powerProfileInstance) as Double
            "${capacity.toInt()} mAh"
        } catch (e: Exception) {
            "Unknown"
        }

        return BatterySpec(
            level = batteryPct,
            status = status,
            technology = technology,
            temperature = temperature,
            voltage = voltage,
            health = health,
            designCapacity = designCapacity
        )
    }

    private fun getNetworkTypeSafely(): String {
        return try {
            // Check for permission explicitly
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                return "Permission Missing" // Or just return "Unknown"
            }

            // Safe call
            val networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                 telephonyManager.dataNetworkType
            } else {
                 telephonyManager.networkType
            }
            
            return mapNetworkTypeToString(networkType) // Helper function to convert int to "4G", "5G" etc.

        } catch (e: SecurityException) {
            Log.e("HardwareUtils", "Permission denied for network type: ${e.message}")
            return "Unknown (Permission Denied)"
        } catch (e: Exception) {
            Log.e("HardwareUtils", "Error getting network type: ${e.message}")
            return "Unknown"
        }
    }
    
    private fun mapNetworkTypeToString(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
            TelephonyManager.NETWORK_TYPE_LTE,
            TelephonyManager.NETWORK_TYPE_IWLAN -> "4G"
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            else -> "Unknown"
        }
    }
    
    fun getNetworkSpecs(): NetworkSpec {
        // Safe network type retrieval with proper exception handling
        val networkType = getNetworkTypeSafely()
        
        // Signal strength
        val signalStrength = try {
            // For older versions, use deprecated method
            @Suppress("DEPRECATION")
            val signal = telephonyManager.signalStrength
            if (signal != null) {
                val level = signal.level
                if (level != 0) "${level} dBm" else "Unknown"
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }

        // WiFi info
        val wifiSpeed = if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
            "${wifiManager.connectionInfo?.linkSpeed ?: 0} Mbps"
        } else {
            "Unknown"
        }

        val wifiFrequency = if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
            val frequency = wifiManager.connectionInfo?.frequency
            if (frequency != null) {
                when {
                    frequency >= 2400 && frequency < 2500 -> "2.4 GHz"
                    frequency >= 5000 && frequency < 6000 -> "5 GHz"
                    else -> "${frequency} MHz"
                }
            } else {
                "Unknown"
            }
        } else {
            "Unknown"
        }

        val wifiStandard = "802.11n/ac" // Default to 802.11n/ac

        // Bluetooth features
        val bluetoothFeatures = mutableListOf<String>()
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bluetoothFeatures.add("BLE")
        }
        // Note: FEATURE_BLUETOOTH_LE_AUDIO may not be available in all Android versions
        // We'll skip this for now to avoid compilation error

        // NFC and IR
        val nfcSupported = nfcAdapter != null
        val irBlasterSupported = context.packageManager.hasSystemFeature("consumerir.transmitter")

        return NetworkSpec(
            networkType = networkType,
            signalStrength = signalStrength,
            wifiSpeed = wifiSpeed,
            wifiFrequency = wifiFrequency,
            wifiStandard = wifiStandard,
            bluetoothFeatures = bluetoothFeatures,
            nfcSupported = nfcSupported,
            irBlasterSupported = irBlasterSupported
        )
    }

    fun getCameraSpecs(): List<CameraSpec> {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraSpecs = mutableListOf<CameraSpec>()

        try {
            val cameraIds = cameraManager.cameraIdList
            for (id in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val direction = when (facing) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                    CameraCharacteristics.LENS_FACING_BACK -> "Back"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                    else -> "Unknown"
                }
                
                val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                val resolution = if (sensorSize != null) {
                    val mp = (sensorSize.width * sensorSize.height) / 1_000_000.0
                    "${String.format("%.1f", mp)} MP"
                } else {
                    "Unknown"
                }
                
                val apertures = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                val aperture = if (apertures != null && apertures.isNotEmpty()) {
                    "f/${apertures.joinToString(", ")}"
                } else {
                    "Unknown"
                }
                
                val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val focalLength = if (focalLengths != null && focalLengths.isNotEmpty()) {
                    "${focalLengths.joinToString(", ")} mm"
                } else {
                    "Unknown"
                }
                
                val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.map { cap ->
                    when (cap) {
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "Backward Compatible"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "Burst Capture"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO -> "High Speed Video"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "Depth Output"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA -> "Multi-Camera"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING -> "Manual Post Processing"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "Manual Sensor"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MOTION_TRACKING -> "Motion Tracking"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "YUV Reprocessing"
                        else -> "Other"
                    }
                } ?: emptyList()

                cameraSpecs.add(
                    CameraSpec(
                        id = id,
                        direction = direction,
                        resolution = resolution,
                        aperture = aperture,
                        focalLength = focalLength,
                        capabilities = capabilities
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("HardwareUtils", "Error getting camera specs", e)
        }

        return cameraSpecs
    }

    fun getMemoryStorageSpecs(): MemoryStorageSpec {
        // RAM
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalMem = memoryInfo.totalMem
        val availableMem = memoryInfo.availMem

        // Storage
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        // Use StorageManager directly for storage stats
        
        val storageVolumes = storageManager.storageVolumes
        var totalStorage = 0L
        var availableStorage = 0L

        for (volume in storageVolumes) {
            if (!volume.isRemovable) {
                try {
                    val path = volume.directory?.path ?: Environment.getExternalStorageDirectory().path
                    val file = File(path)
                    totalStorage += file.totalSpace
                    availableStorage += file.usableSpace
                } catch (e: Exception) {
                    Log.e("HardwareUtils", "Error getting storage info", e)
                }
            }
        }

        return MemoryStorageSpec(
            ramTotal = formatBytes(totalMem),
            ramAvailable = formatBytes(availableMem),
            storageTotal = formatBytes(totalStorage),
            storageAvailable = formatBytes(availableStorage)
        )
    }

    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return "${String.format("%.1f", size)} ${units[unitIndex]}"
    }

    fun getAudioMediaSpecs(): AudioMediaSpec {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        
        // Speakers
        val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
        val speakers = devices.filter { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            .map { "Speaker (${it.productName})" }

        // Widevine level
        val widevineLevel = getWidevineLevel()

        // Supported codecs
        val codecs = mutableListOf<String>()
        try {
            val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            val codecInfos = mediaCodecList.codecInfos
            for (info in codecInfos) {
                if (info.isEncoder) continue
                codecs.addAll(info.supportedTypes)
            }
        } catch (e: Exception) {
            Log.e("HardwareUtils", "Error getting media codecs", e)
        }

        return AudioMediaSpec(
            speakers = speakers,
            widevineLevel = widevineLevel,
            supportedCodecs = codecs.distinct()
        )
    }

    private fun getWidevineLevel(): String {
        return try {
            val widevineUuid = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed")
            val mediaDrm = MediaDrm(widevineUuid)
            // Use the constant value directly instead of PROPERTY_SECURITY_LEVEL
            val securityLevel = mediaDrm.getPropertyString("securityLevel")
            mediaDrm.release()
            securityLevel
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getPeripheralsSpecs(): PeripheralsSpec {
        // Biometric support
        val biometricSupport = mutableListOf<String>()
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            biometricSupport.add("Fingerprint")
        }
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)) {
            biometricSupport.add("Face Recognition")
        }

        // SIM slots
        val simSlots = try {
            subscriptionManager.activeSubscriptionInfoCountMax
        } catch (e: Exception) {
            1 // Default to 1 if we can't determine
        }

        // Vibration
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        val vibrationSupport = vibrator.hasAmplitudeControl()

        // USB OTG
        val usbOtg = context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)

        // Display HDR
        val displayHdr = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
            val display = windowManager.defaultDisplay
            val hdrCapabilities = display.hdrCapabilities
            if (hdrCapabilities != null) {
                for (type in hdrCapabilities.supportedHdrTypes) {
                    when (type) {
                        android.view.Display.HdrCapabilities.HDR_TYPE_HDR10 -> displayHdr.add("HDR10")
                        android.view.Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> displayHdr.add("HDR10+")
                        android.view.Display.HdrCapabilities.HDR_TYPE_HLG -> displayHdr.add("HLG")
                        android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> displayHdr.add("Dolby Vision")
                    }
                }
            }
        }

        // System architecture
        val architectures = Build.SUPPORTED_ABIS.toList()

        return PeripheralsSpec(
            biometricSupport = biometricSupport,
            simSlots = simSlots,
            vibrationSupport = vibrationSupport,
            usbOtg = usbOtg,
            displayHdr = displayHdr,
            systemArchitecture = architectures
        )
    }

    fun getGnssSpecs(): List<String> {
        val providers = locationManager.allProviders
        val gnssFeatures = mutableListOf<String>()
        
        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            gnssFeatures.add("GPS")
        }
        if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
            gnssFeatures.add("Network")
        }
        if (providers.contains(LocationManager.PASSIVE_PROVIDER)) {
            gnssFeatures.add("Passive")
        }
        
        return gnssFeatures
    }
}