//! FFI (Foreign Function Interface) bindings for CPU benchmark
//!
//! This module provides C-compatible functions that can be called from other languages
//! like Kotlin (via JNI) and Python (via cffi or similar). These functions follow
//! C calling conventions and use C-compatible data types to ensure compatibility
//! across different platforms and languages.
//!
//! # Usage
//!
//! To use these functions from other languages:
//!
//! 1. Compile the Rust library as a static or dynamic library
//! 2. Link with your application
//! 3. Call the exported functions directly
//! 4. Remember to free any returned strings or structures using the provided free functions
//!
//! # Memory Management
//!
//! All functions that return heap-allocated data (strings, structures) require the caller
//! to free the memory using the corresponding free function when done. This prevents
//! memory leaks in the calling application.
//!
//! # Thread Safety
//!
//! The FFI functions are generally safe to call from multiple threads, but individual
//! benchmark runs are not designed to be thread-safe with each other. If running multiple
//! benchmarks concurrently, ensure proper synchronization in the calling application.

use std::ffi::{CStr, CString};
use std::os::raw::c_char;
use serde_json;
use crate::types::{BenchmarkConfig, WorkloadParams};
use crate::utils;
use crate::algorithms;
use crate::android_affinity;

/// C-compatible result structure for benchmark results
#[repr(C)]
pub struct CBenchmarkResult {
    /// Name of the benchmark test
    pub name: *mut c_char,
    /// Execution time in milliseconds
    pub execution_time_ms: f64,
    /// Operations per second achieved
    pub ops_per_second: f64,
    /// Whether results were valid
    pub is_valid: bool,
    /// JSON string containing additional metrics
    pub metrics_json: *mut c_char,
}

/// C-compatible configuration structure
#[repr(C)]
pub struct CBenchmarkConfig {
    /// Number of iterations to run each test
    pub iterations: usize,
    /// Whether to run warmup iterations
    pub warmup: bool,
    /// Number of warmup iterations
    pub warmup_count: usize,
    /// Device tier as string: "slow", "mid", or "flagship"
    pub device_tier: *mut c_char,
}

/// C-compatible device tier enum values
#[repr(C)]
pub enum CDeviceTier {
    Slow = 0,
    Mid = 1,
    Flagship = 2,
}

/// Frees a CBenchmarkResult allocated by the benchmark functions
///
/// # Safety
/// This function should only be called on CBenchmarkResult instances allocated by
/// the benchmark functions in this library. Double-freeing or freeing invalid
/// pointers will result in undefined behavior.
#[no_mangle]
pub unsafe extern "C" fn free_benchmark_result(result: *mut CBenchmarkResult) {
    if !result.is_null() {
        let result = Box::from_raw(result);
        
        // Free the name string
        if !result.name.is_null() {
            let _ = CString::from_raw(result.name);
        }
        
        // Free the metrics JSON string
        if !result.metrics_json.is_null() {
            let _ = CString::from_raw(result.metrics_json);
        }
    }
}

/// Frees a CBenchmarkConfig allocated by the library
///
/// # Safety
/// This function should only be called on CBenchmarkConfig instances allocated by
/// the library functions. Double-freeing or freeing invalid pointers will result
/// in undefined behavior.
#[no_mangle]
pub unsafe extern "C" fn free_benchmark_config(config: *mut CBenchmarkConfig) {
    if !config.is_null() {
        let config = Box::from_raw(config);
        
        // Free the device tier string
        if !config.device_tier.is_null() {
            let _ = CString::from_raw(config.device_tier);
        }
    }
}

/// Creates a default benchmark configuration
/// 
/// # Safety
/// The returned pointer must be freed using free_benchmark_config when no longer needed.
/// Failure to do so will result in memory leaks.
#[no_mangle]
pub extern "C" fn create_default_config() -> *mut CBenchmarkConfig {
    let config = CBenchmarkConfig {
        iterations: 3,
        warmup: true,
        warmup_count: 3,
        device_tier: CString::new("mid").unwrap().into_raw(),
    };
    
    Box::into_raw(Box::new(config))
}

/// Runs the complete CPU benchmark suite and returns results as a JSON string
/// 
/// # Parameters
/// * `config_json`: A JSON string representing the benchmark configuration
/// 
/// # Returns
/// A JSON string containing all benchmark results, or null if an error occurs
/// 
/// # Safety
/// The returned string must be freed using free_c_string when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_cpu_benchmark_suite(config_json: *const c_char) -> *mut c_char {
    if config_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let config_str = match CStr::from_ptr(config_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let config: BenchmarkConfig = match serde_json::from_str(config_str) {
        Ok(c) => c,
        Err(_) => return std::ptr::null_mut(),
    };
    
    // Get workload parameters based on device tier
    let params = utils::get_workload_params(&config.device_tier);
    
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
    
    let result_json = match serde_json::to_string(&suite_result) {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    match CString::new(result_json) {
        Ok(c_string) => c_string.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Runs a single-core prime generation benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_single_core_prime_generation(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::single_core_prime_generation(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a multi-core prime generation benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_multi_core_prime_generation(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::multi_core_prime_generation(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a single-core Fibonacci recursive benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_single_core_fibonacci_recursive(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::single_core_fibonacci_recursive(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a multi-core Fibonacci memoized benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_multi_core_fibonacci_memoized(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::multi_core_fibonacci_memoized(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a single-core matrix multiplication benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_single_core_matrix_multiplication(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::single_core_matrix_multiplication(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a multi-core matrix multiplication benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_multi_core_matrix_multiplication(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::multi_core_matrix_multiplication(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a single-core hash computing benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_single_core_hash_computing(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::single_core_hash_computing(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a multi-core hash computing benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_multi_core_hash_computing(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::multi_core_hash_computing(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a single-core string sorting benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_single_core_string_sorting(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::single_core_string_sorting(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a multi-core string sorting benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_multi_core_string_sorting(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::multi_core_string_sorting(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a single-core ray tracing benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_single_core_ray_tracing(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::single_core_ray_tracing(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a multi-core ray tracing benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_multi_core_ray_tracing(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::multi_core_ray_tracing(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a single-core compression benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_single_core_compression(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::single_core_compression(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a multi-core compression benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_multi_core_compression(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::multi_core_compression(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a single-core Monte Carlo π benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_single_core_monte_carlo_pi(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::single_core_monte_carlo_pi(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a multi-core Monte Carlo π benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_multi_core_monte_carlo_pi(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::multi_core_monte_carlo_pi(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a single-core JSON parsing benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_single_core_json_parsing(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::single_core_json_parsing(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a multi-core JSON parsing benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_multi_core_json_parsing(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::multi_core_json_parsing(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a single-core N-Queens benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_single_core_nqueens(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::single_core_nqueens(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Runs a multi-core N-Queens benchmark
/// 
/// # Parameters
/// * `params_json`: A JSON string representing the workload parameters
/// 
/// # Returns
/// A CBenchmarkResult containing the results of the benchmark
/// 
/// # Safety
/// The returned CBenchmarkResult must be freed using free_benchmark_result when no longer needed.
/// The input string must be a valid null-terminated C string.
#[no_mangle]
pub unsafe extern "C" fn run_multi_core_nqueens(params_json: *const c_char) -> *mut CBenchmarkResult {
    if params_json.is_null() {
        return std::ptr::null_mut();
    }
    
    let params_str = match CStr::from_ptr(params_json).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let params: WorkloadParams = match serde_json::from_str(params_str) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let result = algorithms::multi_core_nqueens(&params);
    
    // Convert to C-compatible structure
    let c_result = CBenchmarkResult {
        name: match CString::new(result.name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        execution_time_ms: result.execution_time.as_secs_f64() * 1000.0,
        ops_per_second: result.ops_per_second,
        is_valid: result.is_valid,
        metrics_json: match CString::new(result.metrics.to_string()) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    };
    
    Box::into_raw(Box::new(c_result))
}

/// Sets the big core IDs for CPU affinity control from JNI
///
/// # Parameters
/// * `env`: JNI environment pointer
/// * `class`: JNI class reference
/// * `core_ids`: An array of core IDs that are considered "big" cores
///
/// # Safety
/// The input array must be valid and the length must match the actual array size.
#[no_mangle]
pub unsafe extern "C" fn Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_setBigCoreIds(
    env: jni::JNIEnv,
    _class: jni::objects::JClass,
    core_ids: jni::objects::JIntArray,
) {
    eprintln!("RustBenchmark: JNI setBigCoreIds called");
    
    let array_len = match env.get_array_length(&core_ids) {
        Ok(len) => len as usize,
        Err(e) => {
            eprintln!("RustBenchmark: Failed to get array length: {:?}", e);
            return;
        }
    };
    
    let mut buffer: Vec<jni::sys::jint> = vec![0; array_len];
    if let Err(e) = env.get_int_array_region(core_ids, 0, &mut buffer) {
        eprintln!("RustBenchmark: Failed to get int array region: {:?}", e);
        return;
    }
    
    let core_ids_vec: Vec<usize> = buffer.iter().map(|&x| x as usize).collect();
    eprintln!("RustBenchmark: Received big core IDs from Java: {:?}", core_ids_vec);
    
    // Store the big core IDs for use in benchmarks
    crate::android_affinity::set_big_cores(core_ids_vec);
    eprintln!("RustBenchmark: Big core IDs stored successfully");
}

/// Initializes the Rust logger for JNI usage
#[no_mangle]
pub unsafe extern "C" fn Java_com_ivarna_finalbenchmark2_cpuBenchmark_CpuBenchmarkNative_initLogger(
    env: jni::JNIEnv,
    _class: jni::objects::JClass,
) {
    eprintln!("RustBenchmark: Logger initialized from JNI");
}

/// Helper function to run warmup iterations
fn run_warmup(params: &WorkloadParams) {
    // Run a quick version of each benchmark for warmup
    let _ = algorithms::single_core_prime_generation(params);
    let _ = algorithms::single_core_fibonacci_recursive(params);
    let _ = algorithms::single_core_matrix_multiplication(params);
}

/// Helper function to run all single-core benchmarks
fn run_single_core_benchmarks(params: &WorkloadParams) -> Vec<crate::types::BenchmarkResult> {
    vec![
        algorithms::single_core_prime_generation(params),
        algorithms::single_core_fibonacci_recursive(params),
        algorithms::single_core_matrix_multiplication(params),
        algorithms::single_core_hash_computing(params),
        algorithms::single_core_string_sorting(params),
        algorithms::single_core_ray_tracing(params),
        algorithms::single_core_compression(params),
        algorithms::single_core_monte_carlo_pi(params),
        algorithms::single_core_json_parsing(params),
        algorithms::single_core_nqueens(params),
    ]
}

/// Helper function to run all multi-core benchmarks
fn run_multi_core_benchmarks(params: &WorkloadParams) -> Vec<crate::types::BenchmarkResult> {
    vec![
        algorithms::multi_core_prime_generation(params),
        algorithms::multi_core_fibonacci_memoized(params),
        algorithms::multi_core_matrix_multiplication(params),
        algorithms::multi_core_hash_computing(params),
        algorithms::multi_core_string_sorting(params),
        algorithms::multi_core_ray_tracing(params),
        algorithms::multi_core_compression(params),
        algorithms::multi_core_monte_carlo_pi(params),
        algorithms::multi_core_json_parsing(params),
        algorithms::multi_core_nqueens(params),
    ]
}

/// Frees a C string allocated by the library
/// 
/// # Safety
/// This function should only be called on strings allocated by the library functions.
/// Double-freeing or freeing invalid pointers will result in undefined behavior.
#[no_mangle]
pub unsafe extern "C" fn free_c_string(str: *mut c_char) {
    if !str.is_null() {
        let _ = CString::from_raw(str);
    }
}