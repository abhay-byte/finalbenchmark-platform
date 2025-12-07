//! Android-specific CPU affinity control
//! Sets thread affinity to specific CPU cores

use std::fs::File;
use std::io::Write;
use std::sync::Mutex;

// Static variable to store big core IDs
static BIG_CORE_IDS: Mutex<Option<Vec<usize>>> = Mutex::new(None);

/// Set CPU affinity for current thread to specific cores
///
/// # Arguments
/// * `core_ids` - Vector of CPU core IDs to pin thread to
///
/// # Example
/// ```
/// // Pin thread to cores 4, 5, 6, 7 (big cores on SD845)
/// set_thread_affinity(vec![4, 5, 6, 7]);
/// ```
pub fn set_thread_affinity(core_ids: Vec<usize>) -> Result<(), String> {
    eprintln!("RustBenchmark: Set thread affinity to cores {:?}", core_ids);
    
    #[cfg(target_os = "android")]
    {
        use libc::{cpu_set_t, sched_setaffinity, CPU_SET, CPU_ZERO};
        use std::mem;
        
        unsafe {
            let mut cpu_set: cpu_set_t = mem::zeroed();
            CPU_ZERO(&mut cpu_set);
            
            // Set bits for each core ID
            for core_id in &core_ids {
                CPU_SET(*core_id, &mut cpu_set);
            }
            
            // Apply affinity to current thread (pid 0 = current thread)
            let result = sched_setaffinity(0, mem::size_of::<cpu_set_t>(), &cpu_set);
            
            if result == 0 {
                eprintln!("RustBenchmark: Successfully set thread affinity to cores {:?}", core_ids);
                Ok(())
            } else {
                let error_msg = format!("Failed to set CPU affinity: errno {}", result);
                eprintln!("RustBenchmark: {}", error_msg);
                Err(error_msg)
            }
        }
    }
    
    #[cfg(not(target_os = "android"))]
    {
        eprintln!("RustBenchmark: CPU affinity not supported on non-Android platforms");
        Ok(()) // No-op on non-Android platforms
    }
}

/// Set thread priority to maximum (requires root on some devices)
pub fn set_thread_priority_max() -> Result<(), String> {
    #[cfg(target_os = "android")]
    {
        use libc::{sched_param, sched_setscheduler, SCHED_FIFO};
        
        unsafe {
            let param = sched_param {
                sched_priority: 99, // Maximum priority
            };
            
            let result = sched_setscheduler(0, SCHED_FIFO, &param);
            
            if result == 0 {
                Ok(())
            } else {
                // Fallback to SCHED_OTHER with nice value
                use libc::{setpriority, PRIO_PROCESS};
                setpriority(PRIO_PROCESS, 0, -20); // Highest nice value
                Ok(())
            }
        }
    }
    
    #[cfg(not(target_os = "android"))]
    {
        Ok(())
    }
}

/// Get big core IDs by reading from sysfs
/// Returns cores with max frequency > 2.0 GHz
pub fn detect_big_cores() -> Vec<usize> {
    eprintln!("RustBenchmark: Starting big core detection...");
    let mut big_cores = Vec::new();
    
    for i in 0..16 {
        let freq_path = format!("/sys/devices/system/cpu/cpu{}/cpufreq/cpuinfo_max_freq", i);
        
        match std::fs::read_to_string(&freq_path) {
            Ok(contents) => {
                match contents.trim().parse::<u64>() {
                    Ok(freq_khz) => {
                        // Cores with max freq > 2.0 GHz are big cores
                        if freq_khz > 2_000_000 {
                            eprintln!("RustBenchmark: CPU{} detected as BIG core ({} MHz)", i, freq_khz / 1000);
                            big_cores.push(i);
                        } else {
                            eprintln!("RustBenchmark: CPU{} detected as LITTLE core ({} MHz)", i, freq_khz / 1000);
                        }
                    }
                    Err(e) => {
                        eprintln!("RustBenchmark: Failed to parse frequency for CPU{}: {}", i, e);
                    }
                }
            }
            Err(e) => {
                eprintln!("RustBenchmark: Failed to read frequency for CPU{}: {}", i, e);
            }
        }
    }
    
    eprintln!("RustBenchmark: Big cores detected: {:?}", big_cores);
    big_cores
}

/// Get LITTLE core IDs
pub fn detect_little_cores() -> Vec<usize> {
    let mut little_cores = Vec::new();
    
    for i in 0..16 {
        let freq_path = format!("/sys/devices/system/cpu/cpu{}/cpufreq/cpuinfo_max_freq", i);
        
        if let Ok(contents) = std::fs::read_to_string(&freq_path) {
            if let Ok(freq_khz) = contents.trim().parse::<u64>() {
                // Cores with max freq <= 2.0 GHz are LITTLE cores
                if freq_khz <= 2_000_000 {
                    little_cores.push(i);
                }
            }
        }
    }
    
    little_cores
}

/// Set the big core IDs for later use in benchmarks
pub fn set_big_cores(core_ids: Vec<usize>) {
    if let Ok(mut guard) = BIG_CORE_IDS.lock() {
        *guard = Some(core_ids);
    }
}

/// Get the stored big core IDs
pub fn get_big_cores() -> Vec<usize> {
    if let Ok(guard) = BIG_CORE_IDS.lock() {
        if let Some(ref core_ids) = *guard {
            core_ids.clone()
        } else {
            // Fallback to detection if not set
            detect_big_cores()
        }
    } else {
        // Fallback to detection if mutex is poisoned
        detect_big_cores()
    }
}