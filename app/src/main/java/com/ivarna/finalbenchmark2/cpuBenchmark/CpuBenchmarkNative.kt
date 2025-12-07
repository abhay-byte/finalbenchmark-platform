package com.ivarna.finalbenchmark2.cpuBenchmark

import android.util.Log

/**
 * Native interface for CPU benchmark FFI
 *
 * This class provides the JNI interface to call the Rust CPU benchmark library
 * using the C-compatible FFI functions.
 */
object CpuBenchmarkNative {
    // Load the native library that contains the Rust FFI functions
    // The library name should match the one specified in your Android build
    init {
        try {
            System.loadLibrary("cpu_benchmark")
            Log.d("CpuBenchmarkNative", "Successfully loaded cpu_benchmark native library")
            // Initialize the Rust logger
            initLogger()
        } catch (e: UnsatisfiedLinkError) {
            Log.e("CpuBenchmarkNative", "Failed to load cpu_benchmark native library", e)
            e.printStackTrace()
            // Library not found - this is expected during development if the native library isn't built
            // The app will fall back to simulation mode
        }
    }

    // Declare the native functions that correspond to the Rust FFI functions
    @JvmStatic
    external fun runCpuBenchmarkSuite(configJson: String): String?

    @JvmStatic
    external fun runSingleCorePrimeGeneration(paramsJson: String): String?

    @JvmStatic
    external fun runMultiCorePrimeGeneration(paramsJson: String): String?

    @JvmStatic
    external fun runSingleCoreFibonacciRecursive(paramsJson: String): String?

    @JvmStatic
    external fun runMultiCoreFibonacciMemoized(paramsJson: String): String?

    @JvmStatic
    external fun runSingleCoreMatrixMultiplication(paramsJson: String): String?

    @JvmStatic
    external fun runMultiCoreMatrixMultiplication(paramsJson: String): String?

    @JvmStatic
    external fun runSingleCoreHashComputing(paramsJson: String): String?

    @JvmStatic
    external fun runMultiCoreHashComputing(paramsJson: String): String?

    @JvmStatic
    external fun runSingleCoreStringSorting(paramsJson: String): String?

    @JvmStatic
    external fun runMultiCoreStringSorting(paramsJson: String): String?

    @JvmStatic
    external fun runSingleCoreRayTracing(paramsJson: String): String?

    @JvmStatic
    external fun runMultiCoreRayTracing(paramsJson: String): String?

    @JvmStatic
    external fun runSingleCoreCompression(paramsJson: String): String?

    @JvmStatic
    external fun runMultiCoreCompression(paramsJson: String): String?

    @JvmStatic
    external fun runSingleCoreMonteCarloPi(paramsJson: String): String?

    @JvmStatic
    external fun runMultiCoreMonteCarloPi(paramsJson: String): String?

    @JvmStatic
    external fun runSingleCoreJsonParsing(paramsJson: String): String?

    @JvmStatic
    external fun runMultiCoreJsonParsing(paramsJson: String): String?

    @JvmStatic
    external fun runSingleCoreNqueens(paramsJson: String): String?

    @JvmStatic
    external fun runMultiCoreNqueens(paramsJson: String): String?

    @JvmStatic
    external fun initLogger()

    @JvmStatic
    external fun setBigCoreIds(coreIds: IntArray)
    
    @JvmStatic
    external fun freeCString(str: String?)
    
    /**
     * Helper function to safely call native functions with debugging
     */
    fun callNativeFunction(functionName: String, paramsJson: String, nativeCall: () -> String?): String? {
        Log.d("CpuBenchmarkNative", "Calling native function: $functionName with params: $paramsJson")
        return try {
            val result = nativeCall()
            Log.d("CpuBenchmarkNative", "Native function $functionName returned: $result")
            result
        } catch (e: Exception) {
            Log.e("CpuBenchmarkNative", "Error calling native function $functionName", e)
            null
        }
    }
}