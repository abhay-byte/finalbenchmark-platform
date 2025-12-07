//! Utility functions for CPU benchmark operations

use std::time::{Duration, Instant};
use crate::types::{BenchmarkConfig, DeviceTier, WorkloadParams};

/// Get workload parameters based on device tier
pub fn get_workload_params(tier: &DeviceTier) -> WorkloadParams {
    match tier {
        DeviceTier::Slow => WorkloadParams {
            prime_range: 1_000_000,
            fibonacci_n_range: (30, 38),
            matrix_size: 500,
            hash_data_size_mb: 25,
            string_count: 250_000,
            ray_tracing_resolution: (256, 256),
            ray_tracing_depth: 2,
            compression_data_size_mb: 25,
            monte_carlo_samples: 25_000_000,
            json_data_size_mb: 2,
            nqueens_size: 12,
        },
        DeviceTier::Mid => WorkloadParams {
            prime_range: 8_000_000,         // INCREASED from 6M
            fibonacci_n_range: (32, 38),    // Same
            matrix_size: 700,               // INCREASED from 600
            hash_data_size_mb: 50,          // INCREASED from 40
            string_count: 700_000,          // INCREASED from 500K
            ray_tracing_resolution: (350, 350), // INCREASED from (300, 300)
            ray_tracing_depth: 3,           // Same
            compression_data_size_mb: 30,   // INCREASED from 25
            monte_carlo_samples: 60_000_000, // INCREASED from 40M
            json_data_size_mb: 5,           // INCREASED from 4
            nqueens_size: 13,               // Same
        },
        DeviceTier::Flagship => WorkloadParams {
            prime_range: 20_000_000,        // INCREASED: More work for 8 cores
            fibonacci_n_range: (35, 42),    // INCREASED from (38, 45)
            matrix_size: 1200,              // INCREASED from 1000 (more parallel work)
            hash_data_size_mb: 150,         // INCREASED from 100
            string_count: 2_000_000,        // INCREASED from 1.25M (better scaling test)
            ray_tracing_resolution: (600, 600), // INCREASED from (500, 500)
            ray_tracing_depth: 5,           // Same
            compression_data_size_mb: 80,   // INCREASED from 60
            monte_carlo_samples: 150_000_000, // INCREASED from 120M (embarrassingly parallel)
            json_data_size_mb: 15,          // INCREASED from 10
            nqueens_size: 16,               // INCREASED from 15 (exponentially harder)
        },
    }
}

/// Run a benchmark function and return execution time
pub fn run_benchmark<F>(mut f: F) -> Duration 
where 
    F: FnMut(),
{
    let start = Instant::now();
    f();
    start.elapsed()
}

/// Run a benchmark function multiple times and return average execution time
pub fn run_benchmark_multiple<F>(f: F, iterations: usize) -> Duration 
where 
    F: FnMut() -> (),
{
    let mut total_duration = Duration::new(0, 0);
    let mut f = f;
    
    for _ in 0..iterations {
        let start = Instant::now();
        f();
        total_duration += start.elapsed();
    }
    
    Duration::from_nanos(total_duration.as_nanos() as u64 / iterations as u64)
}

/// Calculate operations per second based on execution time and operation count
pub fn calculate_ops_per_second(operation_count: u64, execution_time: Duration) -> f64 {
    if execution_time.is_zero() {
        return f64::INFINITY;
    }
    
    let execution_time_secs = execution_time.as_secs_f64();
    operation_count as f64 / execution_time_secs
}

/// Verify that a number is prime
pub fn is_prime(n: u64) -> bool {
    if n <= 1 {
        return false;
    }
    if n <= 3 {
        return true;
    }
    if n % 2 == 0 || n % 3 == 0 {
        return false;
    }
    
    let mut i = 5;
    while i * i <= n {
        if n % i == 0 || n % (i + 2) == 0 {
            return false;
        }
        i += 6;
    }
    
    true
}

/// Generate random string of specified length
pub fn generate_random_string(length: usize) -> String {
    use rand::Rng;
    const CHARSET: &[u8] = b"ABCDEFGHIJKLMNOPQRSTUVWXYZ\
                            abcdefghijklmnopqrstuvwxyz\
                            0123456789";
    
    let mut rng = rand::thread_rng();
    (0..length)
        .map(|_| {
            let idx = rng.gen_range(0..CHARSET.len());
            CHARSET[idx] as char
        })
        .collect()
}

/// Validate benchmark config and adjust if needed
pub fn validate_config(config: &mut BenchmarkConfig) {
    if config.iterations == 0 {
        config.iterations = 3;
    }
    
    if config.warmup_count == 0 {
        config.warmup_count = 3;
    }
}