//! CPU Benchmark Library
//!
//! This library implements various CPU benchmark algorithms as specified in the
//! documentation. It includes both single-core and multi-core performance tests
//! to evaluate different aspects of CPU performance.
//!
//! # Features
//!
//! - Single-core and multi-core benchmark algorithms
//! - Support for different device tiers (Slow, Mid, Flagship)
//! - FFI bindings for use from other languages (Kotlin, Python)
//! - Comprehensive test suite
//!
//! # Algorithms
//!
//! The library implements the following benchmark algorithms:
//!
//! - Prime number generation (Sieve of Eratosthenes)
//! - Fibonacci sequence (recursive and memoized)
//! - Matrix multiplication
//! - Hash computing (SHA-256, MD5)
//! - String sorting
//! - Ray tracing
//! - Compression/decompression
//! - Monte Carlo simulation
//! - JSON parsing
//! - N-Queens problem
//!
//! # Usage
//!
//! For direct Rust usage, see the main function in `main.rs`. For FFI usage from
//! other languages, see the `ffi` module and example files.

pub mod algorithms;
pub mod types;
pub mod utils;
pub mod ffi;
pub mod jni_interface;
pub mod android_affinity;

pub use algorithms::*;
pub use types::*;
pub use utils::*;
pub use ffi::*;