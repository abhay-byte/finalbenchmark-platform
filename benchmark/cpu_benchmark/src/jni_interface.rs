//! JNI Interface for Android
//!
//! This module provides JNI-compatible functions that call Rust benchmark functions directly,
//! bypassing the C FFI layer to avoid null pointer issues. Each function parses JSON parameters
//! directly into WorkloadParams and calls the corresponding Rust algorithm function.

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use serde_json;

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

// Macro to implement JNI benchmark functions with direct Rust calls and preset-based workloads
macro_rules! impl_jni_benchmark {
    ($func_name:ident, $rust_func:path, $log_name:expr) => {
        #[no_mangle]
        pub extern "C" fn $func_name(
            mut env: JNIEnv,
            _class: JClass,
            params_json: JString,
        ) -> jstring {
            // Get the params JSON string from Java (for logging purposes only)
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
            
            // Parse JSON parameters and use them directly (no platform-specific overrides)
            let params: crate::types::WorkloadParams =
            match serde_json::from_str(&params_str) {
                Ok(p) => p,
                Err(e) => {
                    #[cfg(target_os = "android")]
                    error!("Failed to parse JSON for {}: {:?}", $log_name, e);
                    // Instead of returning null, use default Mid-tier workload parameters
                    #[cfg(target_os = "android")]
                    error!("Using default Mid-tier workload parameters for {}", $log_name);
                    crate::utils::get_workload_params(&crate::types::DeviceTier::Mid)
                }
            };
            
            let result = $rust_func(&params);
            
            #[cfg(target_os = "android")]
            error!("Benchmark completed for {}: {} - {:.2}ms, ops/sec: {:.2}", $log_name, result.name, result.execution_time.as_secs_f64() * 1000.0, result.ops_per_second);
            
            // Log detailed timing information for debugging the 10x issue
            let execution_time_secs = result.execution_time.as_secs_f64();
            let execution_time_millis = result.execution_time.as_millis() as f64;
            
            #[cfg(target_os = "android")]
            error!("Benchmark completed for {}: {} - Duration: {:.6}s ({:.6}ms), Operations: {}, Ops/sec: {:.2}",
                   $log_name, result.name, execution_time_secs, execution_time_millis,
                   (result.ops_per_second * execution_time_secs) as u64, result.ops_per_second);
            
            // Verify time measurement consistency
            #[cfg(target_os = "android")]
            if (execution_time_secs * 1000.0 - execution_time_millis).abs() > 0.1 {
                error!("WARNING: Time measurement inconsistency detected for {}", result.name);
            }
            
            let result_json = serde_json::json!({
                "name": result.name,
                "execution_time_ms": execution_time_secs * 1000.0,  // Use consistent time in ms
                "ops_per_second": result.ops_per_second,  // Raw ops/second from algorithm
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
    
    // Parse JSON into a temporary structure with string device tier, then convert
    #[derive(serde::Deserialize)]
    struct ConfigWithStringTier {
        iterations: usize,
        warmup: bool,
        warmup_count: usize,
        device_tier: String,
    }
    
    let config_with_string: ConfigWithStringTier = match serde_json::from_str(&config_str) {
        Ok(c) => c,
        Err(e) => {
            #[cfg(target_os = "android")]
            error!("Failed to parse JSON into ConfigWithStringTier: {:?}", e);
            return std::ptr::null_mut();
        }
    };
    
    // Convert string device tier to enum with fallback handling
    let device_tier = match config_with_string.device_tier.to_lowercase().as_str() {
        "slow" => crate::types::DeviceTier::Slow,
        "mid" | "medium" => crate::types::DeviceTier::Mid,
        "flagship" | "high" | "fast" => crate::types::DeviceTier::Flagship,
        _ => {
            #[cfg(target_os = "android")]
            error!("Unknown device tier '{}', defaulting to Mid", config_with_string.device_tier);
            crate::types::DeviceTier::Mid
        }
    };
    
    let config = crate::types::BenchmarkConfig {
        iterations: config_with_string.iterations,
        warmup: config_with_string.warmup,
        warmup_count: config_with_string.warmup_count,
        device_tier,
    };
    
    #[cfg(target_os = "android")]
    error!("Successfully parsed BenchmarkConfig, calling benchmark suite...");
    
    // Get workload parameters based on the requested device tier (no platform-specific overrides)
    let params = crate::utils::get_workload_params(&config.device_tier);
    
    // Run warmup iterations if enabled
    if config.warmup {
        run_warmup(&params);
    }
    
    // Log debug information about the workload parameters being used
    #[cfg(target_os = "android")]
    error!("Workload parameters for tier {:?}: {}", config.device_tier, serde_json::json!({
        "tier": format!("{:?}", config.device_tier),
        "prime_range": params.prime_range,
        "matrix_size": params.matrix_size,
        "hash_data_size_mb": params.hash_data_size_mb,
        "string_count": params.string_count,
        "monte_carlo_samples": params.monte_carlo_samples,
        "json_data_size_mb": params.json_data_size_mb,
        "fibonacci_n_range": [params.fibonacci_n_range.0, params.fibonacci_n_range.1],
        "ray_tracing_resolution": [params.ray_tracing_resolution.0, params.ray_tracing_resolution.1],
        "ray_tracing_depth": params.ray_tracing_depth,
        "compression_data_size_mb": params.compression_data_size_mb,
        "nqueens_size": params.nqueens_size
    }));
    
    // Run the actual benchmarks
    let single_core_results = run_single_core_benchmarks(&params);
    let multi_core_results = run_multi_core_benchmarks(&params);
    
    // Log detailed information about each result for debugging
    #[cfg(target_os = "android")]
    {
        for result in &single_core_results {
            let execution_time_secs = result.execution_time.as_secs_f64();
            error!("Single-core result - {}: {:.2} ops/sec, Duration: {:.6}s",
                   result.name, result.ops_per_second, execution_time_secs);
        }
        for result in &multi_core_results {
            let execution_time_secs = result.execution_time.as_secs_f64();
            error!("Multi-core result - {}: {:.2} ops/sec, Duration: {:.6}s",
                   result.name, result.ops_per_second, execution_time_secs);
        }
    }
    
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