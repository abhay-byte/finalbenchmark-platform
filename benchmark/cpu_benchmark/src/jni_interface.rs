//! JNI Interface for Android
//!
//! This module provides JNI-compatible functions that call Rust benchmark functions directly,
//! bypassing the C FFI layer to avoid null pointer issues. Each function parses JSON parameters
//! directly into WorkloadParams and calls the corresponding Rust algorithm function.

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use serde_json;
use std::ffi::CString;
use std::os::raw::c_char;

#[cfg(target_os = "android")]
use log::error;

#[cfg(target_os = "android")]
use android_logger::Config;

// Initialize logger function that can be called once from the app
#[no_mangle]
pub extern "C" fn Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_initLogger(
    _env: JNIEnv,
    _class: JClass,
) {
    #[cfg(target_os = "android")]
    let _ = android_logger::init_once(
        Config::default()
            .with_max_level(log::LevelFilter::Debug)
            .with_tag("RustBenchmark")
    );
}

// Macro to implement JNI benchmark functions with direct Rust calls
macro_rules! impl_jni_benchmark {
    ($func_name:ident, $rust_func:path, $log_name:expr) => {
        #[no_mangle]
        pub extern "C" fn $func_name(
            mut env: JNIEnv,
            _class: JClass,
            params_json: JString,
        ) -> jstring {
            let params_str: String = match env.get_string(&params_json) {
                Ok(s) => s.into(),
                Err(e) => {
                    #[cfg(target_os = "android")]
                    error!("Failed to get Java string for {}: {:?}", $log_name, e);
                    return std::ptr::null_mut();
                }
            };
            
            #[cfg(target_os = "android")]
            error!("Received params JSON for {}: {}", $log_name, params_str);
            
            let params: crate::types::WorkloadParams = match serde_json::from_str(&params_str) {
                Ok(p) => p,
                Err(e) => {
                    #[cfg(target_os = "android")]
                    error!("Failed to parse JSON for {}: {:?}", $log_name, e);
                    return std::ptr::null_mut();
                }
            };
            
            #[cfg(target_os = "android")]
            error!("Successfully parsed WorkloadParams for {}, calling benchmark...", $log_name);
            
            let result = $rust_func(&params);
            
            #[cfg(target_os = "android")]
            error!("Benchmark completed for {}: {} - {:.2}ms", $log_name, result.name, result.execution_time.as_secs_f64() * 1000.0);
            
            let result_json = serde_json::json!({
                "name": result.name,
                "execution_time_ms": result.execution_time.as_secs_f64() * 1000.0,
                "ops_per_second": result.ops_per_second,
                "is_valid": result.is_valid,
                "metrics_json": result.metrics.to_string()
            });
            
            let result_str = result_json.to_string();
            #[cfg(target_os = "android")]
            error!("Returning result to Java for {}: {}", $log_name, result_str);
            
            match env.new_string(&result_str) {
                Ok(s) => s.into_raw(),
                Err(e) => {
                    #[cfg(target_os = "android")]
                    error!("Failed to create Java string for {}: {:?}", $log_name, e);
                    std::ptr::null_mut()
                }
            }
        }
    };
}

// Single-core benchmarks
impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runSingleCorePrimeGeneration,
    crate::algorithms::single_core_prime_generation,
    "Prime Generation"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runSingleCoreFibonacciRecursive,
    crate::algorithms::single_core_fibonacci_recursive,
    "Fibonacci Recursive"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runSingleCoreMatrixMultiplication,
    crate::algorithms::single_core_matrix_multiplication,
    "Matrix Multiplication"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runSingleCoreHashComputing,
    crate::algorithms::single_core_hash_computing,
    "Hash Computing"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runSingleCoreStringSorting,
    crate::algorithms::single_core_string_sorting,
    "String Sorting"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runSingleCoreRayTracing,
    crate::algorithms::single_core_ray_tracing,
    "Ray Tracing"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runSingleCoreCompression,
    crate::algorithms::single_core_compression,
    "Compression"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runSingleCoreMonteCarloPi,
    crate::algorithms::single_core_monte_carlo_pi,
    "Monte Carlo Pi"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runSingleCoreJsonParsing,
    crate::algorithms::single_core_json_parsing,
    "JSON Parsing"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runSingleCoreNqueens,
    crate::algorithms::single_core_nqueens,
    "N-Queens"
);

// Multi-core benchmarks
impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runMultiCorePrimeGeneration,
    crate::algorithms::multi_core_prime_generation,
    "Multi-Core Prime Generation"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runMultiCoreFibonacciMemoized,
    crate::algorithms::multi_core_fibonacci_memoized,
    "Multi-Core Fibonacci Memoized"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runMultiCoreMatrixMultiplication,
    crate::algorithms::multi_core_matrix_multiplication,
    "Multi-Core Matrix Multiplication"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runMultiCoreHashComputing,
    crate::algorithms::multi_core_hash_computing,
    "Multi-Core Hash Computing"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runMultiCoreStringSorting,
    crate::algorithms::multi_core_string_sorting,
    "Multi-Core String Sorting"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runMultiCoreRayTracing,
    crate::algorithms::multi_core_ray_tracing,
    "Multi-Core Ray Tracing"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runMultiCoreCompression,
    crate::algorithms::multi_core_compression,
    "Multi-Core Compression"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runMultiCoreMonteCarloPi,
    crate::algorithms::multi_core_monte_carlo_pi,
    "Multi-Core Monte Carlo Pi"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runMultiCoreJsonParsing,
    crate::algorithms::multi_core_json_parsing,
    "Multi-Core JSON Parsing"
);

impl_jni_benchmark!(
    Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runMultiCoreNqueens,
    crate::algorithms::multi_core_nqueens,
    "Multi-Core N-Queens"
);

/// JNI wrapper for runCpuBenchmarkSuite - DIRECT CALL
#[no_mangle]
pub extern "C" fn Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_runCpuBenchmarkSuite(
    mut env: JNIEnv,
    _class: JClass,
    config_json: JString,
) -> jstring {
    // Get the config JSON string from Java
    let config_str: String = match env.get_string(&config_json) {
        Ok(s) => s.into(),
        Err(e) => {
            #[cfg(target_os = "android")]
            error!("Failed to get Java string: {:?}", e);
            return std::ptr::null_mut();
        }
    };
    
    #[cfg(target_os = "android")]
    error!("Received config JSON: {}", config_str);
    
    // Parse JSON directly into BenchmarkConfig
    let config: crate::types::BenchmarkConfig = match serde_json::from_str(&config_str) {
        Ok(c) => c,
        Err(e) => {
            #[cfg(target_os = "android")]
            error!("Failed to parse JSON into BenchmarkConfig: {:?}", e);
            return std::ptr::null_mut();
        }
    };
    
    #[cfg(target_os = "android")]
    error!("Successfully parsed BenchmarkConfig, calling benchmark suite...");
    
    // Get workload parameters based on device tier
    let params = crate::utils::get_workload_params(&config.device_tier);
    
    // Run warmup iterations if enabled
    if config.warmup {
        run_warmup(&params);
    }
    
    // Run the actual benchmarks
    let single_core_results = run_single_core_benchmarks(&params);
    let multi_core_results = run_multi_core_benchmarks(&params);
    
    // Combine results into a single structure
    let suite_result = serde_json::json!({
        "single_core_results": single_core_results,
        "multi_core_results": multi_core_results,
    });
    
    let result_json = suite_result.to_string();
    #[cfg(target_os = "android")]
    error!("Benchmark suite completed, returning result to Java: {}", result_json);
    
    let java_result = match env.new_string(&result_json) {
        Ok(s) => s,
        Err(e) => {
            #[cfg(target_os = "android")]
            error!("Failed to create Java string: {:?}", e);
            return std::ptr::null_mut();
        }
    };
    
    java_result.into_raw()
}

/// Helper function to run warmup iterations
fn run_warmup(params: &crate::types::WorkloadParams) {
    // Run a quick version of each benchmark for warmup
    let _ = crate::algorithms::single_core_prime_generation(params);
    let _ = crate::algorithms::single_core_fibonacci_recursive(params);
    let _ = crate::algorithms::single_core_matrix_multiplication(params);
}

/// Helper function to run all single-core benchmarks
fn run_single_core_benchmarks(params: &crate::types::WorkloadParams) -> Vec<crate::types::BenchmarkResult> {
    vec![
        crate::algorithms::single_core_prime_generation(params),
        crate::algorithms::single_core_fibonacci_recursive(params),
        crate::algorithms::single_core_matrix_multiplication(params),
        crate::algorithms::single_core_hash_computing(params),
        crate::algorithms::single_core_string_sorting(params),
        crate::algorithms::single_core_ray_tracing(params),
        crate::algorithms::single_core_compression(params),
        crate::algorithms::single_core_monte_carlo_pi(params),
        crate::algorithms::single_core_json_parsing(params),
        crate::algorithms::single_core_nqueens(params),
    ]
}

/// Helper function to run all multi-core benchmarks
fn run_multi_core_benchmarks(params: &crate::types::WorkloadParams) -> Vec<crate::types::BenchmarkResult> {
    vec![
        crate::algorithms::multi_core_prime_generation(params),
        crate::algorithms::multi_core_fibonacci_memoized(params),
        crate::algorithms::multi_core_matrix_multiplication(params),
        crate::algorithms::multi_core_hash_computing(params),
        crate::algorithms::multi_core_string_sorting(params),
        crate::algorithms::multi_core_ray_tracing(params),
        crate::algorithms::multi_core_compression(params),
        crate::algorithms::multi_core_monte_carlo_pi(params),
        crate::algorithms::multi_core_json_parsing(params),
        crate::algorithms::multi_core_nqueens(params),
    ]
}

/// JNI wrapper for freeCString
#[no_mangle]
pub extern "C" fn Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_freeCString(
    _env: JNIEnv,
    _class: JClass,
    _str_to_free: JString,
) {
    // This function is not needed as we handle memory management within the individual wrapper functions
    // The memory is managed by the individual wrapper functions
}