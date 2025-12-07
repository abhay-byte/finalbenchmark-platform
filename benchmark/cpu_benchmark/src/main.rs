//! CPU Benchmark CLI Application
//!
//! This is the main entry point for the CPU benchmark suite.
//! It provides a command-line interface to run various CPU benchmarks.

use cpu_benchmark::{types::{BenchmarkConfig, DeviceTier, WorkloadParams}, utils};
use std::time::Instant;

// Normalization factor to scale the final score to the target range (~2000)
// After rebalancing individual scores to be in similar ranges (~70 points per test),
// we no longer need a heavy normalization factor
const NORMALIZATION_FACTOR: f64 = 1.0; // Set to 1.0 for naturally balanced scoring system

fn main() {
    println!("========================================");
    println!(" CPU BENCHMARK RESULTS");
    println!("========================================");
    
    // Parse command line arguments to determine device tier
    let args: Vec<String> = std::env::args().collect();
    let device_tier = if args.len() > 1 {
        match args[1].to_lowercase().as_str() {
            "slow" => DeviceTier::Slow,
            "mid" => DeviceTier::Mid,
            "flagship" => DeviceTier::Flagship,
            _ => {
                eprintln!("Invalid device tier: {}. Using 'mid' as default.", args[1]);
                println!("Usage: {} [slow|mid|flagship]", args[0]);
                DeviceTier::Mid
            }
        }
    } else {
        // Default to mid tier if no argument provided
        DeviceTier::Mid
    };
    
    println!("Running benchmarks for {:?} tier device", device_tier);
    
    // Create benchmark configuration
    let mut config = BenchmarkConfig {
        iterations: 3,
        warmup: true,
        warmup_count: 3,
        device_tier,
    };
    
    utils::validate_config(&mut config);
    
    // Get workload parameters based on device tier
    let params = utils::get_workload_params(&config.device_tier);
    
    // Run warmup iterations if enabled
    if config.warmup {
        println!("\nRunning warmup iterations...");
        run_warmup(&params);
    }
    
    // Run the actual benchmarks
    println!("\nRunning benchmarks...");
    let start_time = Instant::now();
    
    // Single-core benchmarks
    let single_core_results = run_single_core_benchmarks(&params);
    println!("Completed {} single-core benchmarks", single_core_results.len());
    
    // Multi-core benchmarks
    let multi_core_results = run_multi_core_benchmarks(&params);
    println!("Completed {} multi-core benchmarks", multi_core_results.len());
    
    let total_time = start_time.elapsed();
    println!("\nTotal benchmark time: {:?}", total_time);
    
    // Display results
    display_results(&single_core_results, &multi_core_results);
}

fn run_warmup(params: &WorkloadParams) {
    // Run a quick version of each benchmark for warmup
    let _ = cpu_benchmark::algorithms::single_core_prime_generation(params);
    let _ = cpu_benchmark::algorithms::single_core_fibonacci_recursive(params);
    let _ = cpu_benchmark::algorithms::single_core_matrix_multiplication(params);
}

/// Calculate individual scores for each benchmark result
///
/// Scoring Philosophy:
/// To ensure all benchmarks contribute meaningfully to the final score,
/// each test has its own scaling factor to normalize results to a similar range.
/// The goal is to have each test produce scores of approximately 70 points,
/// leading to a final combined score naturally under 2000 for mid-range devices.
///
/// Scaling factors are determined based on typical performance ranges for each test:
/// - Tests that naturally produce high ops/sec get smaller scaling factors
/// - Tests that naturally produce low ops/sec get larger scaling factors
/// - This ensures balanced contribution to the final score
fn calculate_individual_scores(results: &[cpu_benchmark::types::BenchmarkResult]) -> Vec<cpu_benchmark::types::BenchmarkScore> {
    results
        .iter()
        .map(|result| {
            // UPDATED: Different scaling factors for single-core vs multi-core
            // Multi-core factors are 4-5x smaller because ops/sec is 4-8x higher
            let score = match result.name.as_str() {
                // ===== SINGLE-CORE BENCHMARKS =====
                "Single-Core Prime Generation" => {
                    result.ops_per_second * 0.00000001
                },
                "Single-Core Fibonacci Recursive" => {
                    result.ops_per_second * 0.00012
                },
                "Single-Core Matrix Multiplication" => {
                    result.ops_per_second * 0.000000025
                },
                "Single-Core Hash Computing" => {
                    result.ops_per_second * 0.00000001
                },
                "Single-Core String Sorting" => {
                    result.ops_per_second * 0.00000015
                },
                "Single-Core Ray Tracing" => {
                    result.ops_per_second * 0.0000006
                },
                "Single-Core Compression" => {
                    result.ops_per_second * 0.00000007
                },
                "Single-Core Monte Carlo π" => {
                    result.ops_per_second * 0.0000007
                },
                "Single-Core JSON Parsing" => {
                    result.ops_per_second * 0.0000004
                },
                "Single-Core N-Queens" => {
                    result.ops_per_second * 0.0007
                },
                
                // ===== MULTI-CORE BENCHMARKS =====
                // Factors are ~5x SMALLER because multi-core ops/sec is ~5x HIGHER
                "Multi-Core Prime Generation" => {
                    result.ops_per_second * 0.00000020  // 5x smaller (was 0.00000001)
                },
                "Multi-Core Fibonacci Memoized" => {
                    result.ops_per_second * 0.0024  // 5x smaller (was 0.00012)
                },
                "Multi-Core Matrix Multiplication" => {
                    result.ops_per_second * 0.00000010  // 4x smaller (was 0.000000025)
                },
                "Multi-Core Hash Computing" => {
                    result.ops_per_second * 0.00000020  // 5x smaller (was 0.00000001)
                },
                "Multi-Core String Sorting" => {
                    result.ops_per_second * 0.00000030  // 5x smaller (was 0.00000015)
                },
                "Multi-Core Ray Tracing" => {
                    result.ops_per_second * 0.0000030  // 5x smaller (was 0.0000006)
                },
                "Multi-Core Compression" => {
                    result.ops_per_second * 0.000000035  // 5x smaller (was 0.00000007)
                },
                "Multi-Core Monte Carlo π" => {
                    result.ops_per_second * 0.0000035  // 5x smaller (was 0.0000007)
                },
                "Multi-Core JSON Parsing" => {
                    result.ops_per_second * 0.0000020  // 5x smaller (was 0.0000004)
                },
                "Multi-Core N-Queens" => {
                    result.ops_per_second * 0.000035  // 5x smaller (was 0.00007)
                },
                
                // Default case
                _ => {
                    // Detect if multi-core and use appropriate default
                    if result.name.contains("Multi-Core") {
                        result.ops_per_second * 0.00005  // Multi-core default
                    } else {
                        result.ops_per_second * 0.0001   // Single-core default
                    }
                }
            };
            
            cpu_benchmark::types::BenchmarkScore {
                name: result.name.clone(),
                ops_per_second: result.ops_per_second,
                score,
            }
        })
        .collect()
}

fn run_single_core_benchmarks(params: &WorkloadParams) -> Vec<cpu_benchmark::types::BenchmarkResult> {
    use std::time::Instant;
    
    let mut results = Vec::new();
    
    // Single-core prime generation
    println!("Starting Single-Core Prime Generation benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::single_core_prime_generation(params);
    let elapsed = start_time.elapsed();
    println!("Completed Single-Core Prime Generation in {:?}", elapsed);
    results.push(result);
    
    // Single-core fibonacci recursive
    println!("Starting Single-Core Fibonacci Recursive benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::single_core_fibonacci_recursive(params);
    let elapsed = start_time.elapsed();
    println!("Completed Single-Core Fibonacci Recursive in {:?}", elapsed);
    results.push(result);
    
    // Single-core matrix multiplication
    println!("Starting Single-Core Matrix Multiplication benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::single_core_matrix_multiplication(params);
    let elapsed = start_time.elapsed();
    println!("Completed Single-Core Matrix Multiplication in {:?}", elapsed);
    results.push(result);
    
    // Single-core hash computing
    println!("Starting Single-Core Hash Computing benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::single_core_hash_computing(params);
    let elapsed = start_time.elapsed();
    println!("Completed Single-Core Hash Computing in {:?}", elapsed);
    results.push(result);
    
    // Single-core string sorting
    println!("Starting Single-Core String Sorting benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::single_core_string_sorting(params);
    let elapsed = start_time.elapsed();
    println!("Completed Single-Core String Sorting in {:?}", elapsed);
    results.push(result);
    
    // Single-core ray tracing
    println!("Starting Single-Core Ray Tracing benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::single_core_ray_tracing(params);
    let elapsed = start_time.elapsed();
    println!("Completed Single-Core Ray Tracing in {:?}", elapsed);
    results.push(result);
    
    // Single-core compression
    println!("Starting Single-Core Compression benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::single_core_compression(params);
    let elapsed = start_time.elapsed();
    println!("Completed Single-Core Compression in {:?}", elapsed);
    results.push(result);
    
    // Single-core monte carlo pi
    println!("Starting Single-Core Monte Carlo π benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::single_core_monte_carlo_pi(params);
    let elapsed = start_time.elapsed();
    println!("Completed Single-Core Monte Carlo π in {:?}", elapsed);
    results.push(result);
    
    // Single-core json parsing
    println!("Starting Single-Core JSON Parsing benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::single_core_json_parsing(params);
    let elapsed = start_time.elapsed();
    println!("Completed Single-Core JSON Parsing in {:?}", elapsed);
    results.push(result);
    
    // Single-core nqueens
    println!("Starting Single-Core N-Queens benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::single_core_nqueens(params);
    let elapsed = start_time.elapsed();
    println!("Completed Single-Core N-Queens in {:?}", elapsed);
    results.push(result);
    
    results
}

fn run_multi_core_benchmarks(params: &WorkloadParams) -> Vec<cpu_benchmark::types::BenchmarkResult> {
    use std::time::Instant;
    
    let mut results = Vec::new();
    
    // Multi-core prime generation
    println!("Starting Multi-Core Prime Generation benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::multi_core_prime_generation(params);
    let elapsed = start_time.elapsed();
    println!("Completed Multi-Core Prime Generation in {:?}", elapsed);
    results.push(result);
    
    // Multi-core fibonacci memoized
    println!("Starting Multi-Core Fibonacci Memoized benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::multi_core_fibonacci_memoized(params);
    let elapsed = start_time.elapsed();
    println!("Completed Multi-Core Fibonacci Memoized in {:?}", elapsed);
    results.push(result);
    
    // Multi-core matrix multiplication
    println!("Starting Multi-Core Matrix Multiplication benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::multi_core_matrix_multiplication(params);
    let elapsed = start_time.elapsed();
    println!("Completed Multi-Core Matrix Multiplication in {:?}", elapsed);
    results.push(result);
    
    // Multi-core hash computing
    println!("Starting Multi-Core Hash Computing benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::multi_core_hash_computing(params);
    let elapsed = start_time.elapsed();
    println!("Completed Multi-Core Hash Computing in {:?}", elapsed);
    results.push(result);
    
    // Multi-core string sorting
    println!("Starting Multi-Core String Sorting benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::multi_core_string_sorting(params);
    let elapsed = start_time.elapsed();
    println!("Completed Multi-Core String Sorting in {:?}", elapsed);
    results.push(result);
    
    // Multi-core ray tracing
    println!("Starting Multi-Core Ray Tracing benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::multi_core_ray_tracing(params);
    let elapsed = start_time.elapsed();
    println!("Completed Multi-Core Ray Tracing in {:?}", elapsed);
    results.push(result);
    
    // Multi-core compression
    println!("Starting Multi-Core Compression benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::multi_core_compression(params);
    let elapsed = start_time.elapsed();
    println!("Completed Multi-Core Compression in {:?}", elapsed);
    results.push(result);
    
    // Multi-core monte carlo pi
    println!("Starting Multi-Core Monte Carlo π benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::multi_core_monte_carlo_pi(params);
    let elapsed = start_time.elapsed();
    println!("Completed Multi-Core Monte Carlo π in {:?}", elapsed);
    results.push(result);
    
    // Multi-core json parsing
    println!("Starting Multi-Core JSON Parsing benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::multi_core_json_parsing(params);
    let elapsed = start_time.elapsed();
    println!("Completed Multi-Core JSON Parsing in {:?}", elapsed);
    results.push(result);
    
    // Multi-core nqueens
    println!("Starting Multi-Core N-Queens benchmark...");
    let start_time = Instant::now();
    let result = cpu_benchmark::algorithms::multi_core_nqueens(params);
    let elapsed = start_time.elapsed();
    println!("Completed Multi-Core N-Queens in {:?}", elapsed);
    results.push(result);
    
    results
}

fn display_results(
    single_core_results: &[cpu_benchmark::types::BenchmarkResult],
    multi_core_results: &[cpu_benchmark::types::BenchmarkResult]
) {
    // Calculate and display individual benchmark scores
    let single_core_scores = calculate_individual_scores(single_core_results);
    let multi_core_scores = calculate_individual_scores(multi_core_results);
    
    println!("\n-- Individual Test Scores --");
    for score in &single_core_scores {
        println!("{} (Single): {:.2}", score.name.replace("Single-Core ", ""), score.score);
    }
    for score in &multi_core_scores {
        println!("{} (Multi): {:.2}", score.name.replace("Multi-Core ", ""), score.score);
    }
    
    // Calculate and display summary category scores
    let single_core_score: f64 = single_core_scores.iter().map(|score| score.score).sum();
    let multi_core_score: f64 = multi_core_scores.iter().map(|score| score.score).sum();
    
    println!("\n-- Category Summary Scores --");
    println!("Single-Core Score: {:.0}", single_core_score);
    println!("Multi-Core Score: {:.0}", multi_core_score);
    
    // Calculate and display core performance ratio
    let core_ratio = if single_core_score > 0.0 {
        multi_core_score / single_core_score
    } else {
        0.0
    };
    println!("Core Performance Ratio (Multi/Single): {:.2}x", core_ratio);
    
    // Calculate and display final CPU score
    let cpu_score = calculate_cpu_score(single_core_results, multi_core_results);
    
    // Print the weighted combined score before normalization
    println!("\n-- Weighted Scoring --");
    let single_core_weight = 0.35;
    let multi_core_weight = 0.65;
    let single_core_scores = calculate_individual_scores(single_core_results);
    let multi_core_scores = calculate_individual_scores(multi_core_results);
    
    // Sum the balanced individual scores for each category
    let rebalanced_single_core_score: f64 = single_core_scores
        .iter()
        .filter(|score| score.score > 0.0)
        .map(|score| score.score)
        .sum();
    
    let rebalanced_multi_core_score: f64 = multi_core_scores
        .iter()
        .filter(|score| score.score > 0.0)
        .map(|score| score.score)
        .sum();
    
    let weighted_score = (rebalanced_single_core_score * single_core_weight) + (rebalanced_multi_core_score * multi_core_weight);
    println!("Combined Weighted Score: {:.2}", weighted_score);
    
    display_cpu_score(cpu_score);
}

/// Calculate final CPU score based on all benchmark results
///
/// This function works with balanced individual scores from calculate_individual_scores.
/// Individual scores are now designed to produce approximately 70 points per test,
/// leading to a natural final score under 2000 for mid-range devices without heavy normalization.
fn calculate_cpu_score(
    single_core_results: &[cpu_benchmark::types::BenchmarkResult],
    multi_core_results: &[cpu_benchmark::types::BenchmarkResult]
) -> f64 {
    // Calculate individual scores first (these are now balanced)
    let single_core_scores = calculate_individual_scores(single_core_results);
    let multi_core_scores = calculate_individual_scores(multi_core_results);
    
    // Calculate weighted category scores based on balanced individual scores
    let single_core_weight = 0.35; // 35% weight to single-core performance
    let multi_core_weight = 0.65;   // 65% weight to multi-core performance
    
    // Sum the balanced individual scores for each category
    let single_core_score: f64 = single_core_scores
        .iter()
        .filter(|score| score.score > 0.0) // Only include valid scores
        .map(|score| score.score)
        .sum();
    
    let multi_core_score: f64 = multi_core_scores
        .iter()
        .filter(|score| score.score > 0.0) // Only include valid scores
        .map(|score| score.score)
        .sum();
    
    // Calculate final weighted score
    let weighted_score = (single_core_score * single_core_weight) + (multi_core_score * multi_core_weight);
    
    // Apply normalization factor to bring score to target range (~2000)
    // With NORMALIZATION_FACTOR now at 1.0, the score naturally falls in the desired range
    weighted_score * NORMALIZATION_FACTOR
}


/// Display the final CPU score with rating
fn display_cpu_score(normalized_score: f64) {
    // Calculate the raw score by reversing the normalization
    let raw_score = normalized_score / NORMALIZATION_FACTOR;
    
    println!("\nFinal Normalized Score: {:.2}", normalized_score);
    println!("Normalization Factor Used: {:.6}", NORMALIZATION_FACTOR);
    println!("Raw Score (before normalization): {:.2}", raw_score);
    
    // Determine rating based on normalized score
    let rating = if normalized_score >= 1800.0 {
        "★★★ (Exceptional Performance)"
    } else if normalized_score >= 1500.0 {
        "★★★★☆ (High Performance)"
    } else if normalized_score >= 1000.0 {
        "★★★☆☆ (Good Performance)"
    } else if normalized_score >= 600.0 {
        "★★☆☆☆ (Moderate Performance)"
    } else if normalized_score >= 300.0 {
        "★☆☆☆ (Basic Performance)"
    } else {
        "☆☆☆ (Low Performance)"
    };
    
    println!("Rating: {}", rating);
    
    // Add comment about the scoring system
    println!("\nNote: CPU Score is a weighted combination of all benchmarks,");
    println!("with single-core performance having 35% weight and multi-core 65% weight.");
    println!("Higher scores indicate better CPU performance.");
}
