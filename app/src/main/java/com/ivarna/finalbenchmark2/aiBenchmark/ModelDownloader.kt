package com.ivarna.finalbenchmark2.aiBenchmark

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object ModelDownloader {
    private const val TAG = "ModelDownloader"

    suspend fun downloadModel(
        context: Context,
        modelUrl: String,
        fileName: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        // Create models directory if it doesn't exist
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
            Log.d(TAG, "Created models directory: ${modelsDir.absolutePath}")
        }

        val file = File(modelsDir, fileName)

        // Strict existence check: Must exist AND have content > 0 bytes
        if (file.exists()) {
            if (file.length() > 0) {
                Log.d(TAG, "Model $fileName already exists and is valid (${file.length()} bytes).")
                onProgress(1.0f)
                return@withContext file
            } else {
                Log.w(TAG, "Model $fileName exists but is empty (0 bytes). Deleting and re-downloading.")
                file.delete()
            }
        }

        Log.d(TAG, "Downloading $fileName from $modelUrl")
        
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder().url(modelUrl).build()

        try {
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed: HTTP ${response.code}")
                if (response.code == 401 || response.code == 403) {
                     Log.e(TAG, "CRITICAL: Authentication Required. This model (Gemma) is likely GATED.")
                     Log.e(TAG, "Please download manually from: $modelUrl")
                     Log.e(TAG, "And place it in: ${file.absolutePath}")
                     Log.e(TAG, "Or use the 'Device File Explorer' in Android Studio to upload to /data/data/com.ivarna.finalbenchmark2/files/models/")
                }
                response.close()
                return@withContext null
            }

        val body = response.body
        if (body == null) {
            Log.e(TAG, "Download failed: Response body is null")
            return@withContext null
        }

        val contentLength = body.contentLength()

        // Fix #20: Stream body directly to file — never buffer entire response in memory.
        // Uses 64 KB chunks for better throughput on large models (Gemma ~300 MB).
        val inputStream = body.byteStream()
        val outputStream = FileOutputStream(file)

        val data = ByteArray(65536) // 64 KB chunks
        var total: Long = 0
        var count: Int

        while (inputStream.read(data).also { count = it } != -1) {
            total += count.toLong()
            if (contentLength > 0L) {
                onProgress(total.toFloat() / contentLength)
            }
            outputStream.write(data, 0, count)
        }

        outputStream.flush()
        outputStream.close()
        inputStream.close()
        body.close()
        response.close()
            
            Log.d(TAG, "Download complete: ${file.absolutePath} (${file.length()} bytes)")
            onProgress(1.0f)
            return@withContext file

        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            if (file.exists()) file.delete() // Cleanup partial/failed file
            return@withContext null
        }
    }
    fun areAllModelsDownloaded(context: Context): Boolean {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) return false

        return ModelRepository.models.all { model ->
            File(modelsDir, model.filename).exists()
        }
    }
}

object ModelRepository {
    // Links need to be direct downloads. 
    // Using widely available TFHub/storage links for MobileNetV3 and others as placeholders.
    // Note: User can replace these with their specific Qualcomm optimized versions.
    
    // Base URL for V2 Models
    // https://github.com/abhay-byte/ai-tf-models/releases/tag/models-v2
    const val BASE_URL_V2 = "https://github.com/abhay-byte/ai-tf-models/releases/download/models-v2"

    // MobileNet V3 Small (Image Classification)
    const val MOBILENET_V3_URL = "$BASE_URL_V2/mobilenet_v3_small.tflite"
    const val MOBILENET_FILENAME = "mobilenet_v3_small.tflite" 

    // MobileNet V1 (Image Classification - Legacy/Baseline)
    const val MOBILENET_V1_URL = "$BASE_URL_V2/mobilenet_v1.tflite"
    const val MOBILENET_V1_FILENAME = "mobilenet_v1.tflite"
    
    // Gemma 3 270M (LLM)
    const val GEMMA_URL = "$BASE_URL_V2/gemma3-270m-it-q8.litertlm"
    const val GEMMA_FILENAME = "gemma3-270m-it-q8.litertlm"

    // EfficientDet Lite0 (Object Detection)
    const val EFFICIENTDET_URL = "$BASE_URL_V2/efficientdet_lite0.tflite"
    const val EFFICIENTDET_FILENAME = "efficientdet_lite0.tflite"

    // YOLOv8n (Object Detection - Modern)
    // Using float16 variant for balance
    const val YOLO_V8_URL = "$BASE_URL_V2/yolov8n_float16.tflite"
    const val YOLO_V8_FILENAME = "yolov8n_float16.tflite"

    // MiniLM-L6 (Text Embedding)
    const val MINILM_URL = "$BASE_URL_V2/all-MiniLM-L6-v2-quant.tflite"
    const val MINILM_FILENAME = "all-MiniLM-L6-v2-quant.tflite"

    // Universal Sentence Encoder QA (Question Answering)


    // MobileBERT (Text Classification / Feature Extraction)
    const val MOBILEBERT_URL = "$BASE_URL_V2/mobilebert.tflite"
    const val MOBILEBERT_FILENAME = "mobilebert.tflite"

    // Whisper Tiny (Speech to Text)
    const val WHISPER_URL = "$BASE_URL_V2/whisper-tiny-en.tflite"
    const val WHISPER_FILENAME = "whisper-tiny-en.tflite"

    // DTLN (Audio Noise Suppression)
    const val DTLN_URL = "$BASE_URL_V2/dtln_quantized.tflite"
    const val DTLN_FILENAME = "dtln_quantized.tflite"

    data class ModelInfo(
        val url: String, 
        val filename: String, 
        val title: String,
        val sizeMb: String = "~50MB"
    )

    val models = listOf(
        ModelInfo(MOBILENET_V3_URL, MOBILENET_FILENAME, "MobileNet V3", "~10MB"),
        ModelInfo(MOBILENET_V1_URL, MOBILENET_V1_FILENAME, "MobileNet V1", "~16MB"),
        ModelInfo(EFFICIENTDET_URL, EFFICIENTDET_FILENAME, "EfficientDet", "~4MB"),
        ModelInfo(YOLO_V8_URL, YOLO_V8_FILENAME, "YOLOv8n", "~6MB"),
        ModelInfo(MINILM_URL, MINILM_FILENAME, "MiniLM", "~22MB"),

        ModelInfo(MOBILEBERT_URL, MOBILEBERT_FILENAME, "MobileBERT", "~96MB"),
        ModelInfo(WHISPER_URL, WHISPER_FILENAME, "Whisper Tiny", "~40MB"),
        ModelInfo(DTLN_URL, DTLN_FILENAME, "DTLN Noise Suppression", "~1MB"),
        ModelInfo(GEMMA_URL, GEMMA_FILENAME, "Gemma 3 LLM", "~300MB")
    )
}
