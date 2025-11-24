# Throttle Test (Stress Test) Benchmark Mode

## Overview

The Throttle Test is a comprehensive stress testing mode that simultaneously runs intensive CPU and GPU workloads to evaluate thermal management and performance consistency under sustained load. This test pushes the device's hardware to its limits to identify thermal throttling behavior, measure sustained performance capabilities, and assess the effectiveness of the device's thermal management system.

Duration options: 10 minutes, 30 minutes, or 1 hour
Primary output: Throttling percentage, temperature curves, sustained performance scores

## Test Configuration

### Simultaneous Workloads
The throttle test runs the following benchmarks concurrently:
- **CPU Component**: Multi-core stress test from [CPU Benchmark](../benchmark/cpu.md)
- **GPU Component**: Native GPU test from [GPU Native Benchmark](../benchmark/gpu_native.md)

### Duration Options
- **Short Test**: 10 minutes - Quick thermal assessment
- **Standard Test**: 30 minutes - Comprehensive thermal profiling  
- **Extended Test**: 60 minutes - Maximum stress evaluation

## Performance Monitoring

### Real-time Metrics
During the test, the system continuously monitors:

**Temperature Tracking**:
- CPU temperature (per core when available)
- GPU temperature
- Battery temperature
- Skin temperature (device surface)
- Thermal sensor readings from all available sensors

**Performance Metrics**:
- CPU clock speeds (per core)
- GPU clock speeds
- CPU utilization per core
- GPU utilization percentage
- Memory bandwidth usage
- Frame rates (for GPU component)

**System State**:
- Active cooling system status (fans, pumps)
- Thermal throttling events
- Power consumption
- Battery drain rate
- Memory pressure

## Measurements Captured

### Primary Outputs

**Throttling Percentage**:
```
Throttling % = (Peak Performance - Sustained Performance) / Peak Performance × 100
```
Where peak performance is the highest sustained performance in the first 30 seconds, and sustained performance is the average performance over the remainder of the test.

**Temperature Curves**:
- Temperature vs. time graphs for all monitored components
- Time to reach thermal limits
- Maximum temperatures achieved
- Temperature stabilization patterns

**Sustained Performance Score**:
- Average performance over the test duration
- Performance consistency metrics (standard deviation)
- Minimum performance thresholds maintained
- Recovery time after throttling events

### Secondary Metrics

**Performance Degradation Analysis**:
- Performance drop-off rate over time
- Inflection points where throttling begins
- Periods of stable performance vs. throttling
- Correlation between temperature and performance drops

**Thermal Efficiency Rating**:
- Temperature rise rate during load
- Effectiveness of thermal dissipation
- Relationship between power consumption and heat generation
- Thermal resistance of device housing

## Test Execution Sequence

### Pre-test Preparation
1. **System State Verification**:
   - Ensure device battery level >50%
   - Close background applications
   - Verify thermal sensors are functional
   - Record ambient temperature

2. **Initial Measurements**:
   - Record baseline temperatures
   - Measure idle CPU/GPU frequencies
   - Establish baseline performance metrics

### Test Execution
1. **Warm-up Phase (30 seconds)**:
   - Begin CPU multi-core test
   - Begin GPU native test
   - Allow workloads to reach steady state
   - Monitor for immediate thermal response

2. **Monitoring Phase**:
   - Continuously record all metrics at 1Hz intervals
   - Detect and log thermal throttling events
   - Monitor performance consistency
   - Track temperature progression

3. **Sustained Load Maintenance**:
   - Maintain maximum workload throughout duration
   - Adjust workload intensity if performance drops below threshold
   - Continue monitoring during throttling periods

### Post-test Analysis
1. **Data Aggregation**:
   - Compile temperature curves
   - Calculate throttling percentages
   - Generate performance consistency metrics
   - Identify thermal behavior patterns

2. **Recovery Monitoring**:
   - Track temperature decay after test completion
   - Monitor return to normal operating frequencies
   - Record cooldown time to safe temperatures

## Scoring Methodology

### Throttling Score (0-100 scale)
```
Throttling Score = Max(0, 100 - Throttling Percentage)
```
A score of 100 indicates no performance degradation, while a score of 0 indicates complete performance loss.

### Thermal Management Grade
- **A+**: <10% throttling, excellent thermal management
- **A**: 10-20% throttling, good thermal management
- **B+**: 20-30% throttling, adequate thermal management
- **B**: 30-40% throttling, acceptable thermal management
- **C**: 40-50% throttling, poor thermal management
- **D**: 50-75% throttling, inadequate thermal management
- **F**: >75% throttling, severe thermal issues

### Sustained Performance Index
```
SPI = (Average Sustained Performance / Peak Performance) × 100
```
This metric evaluates how well the device maintains performance over time.

## Anti-Cheat Measures

### System Integrity Verification
- Monitor for CPU/GPU governor modifications during test
- Verify thermal mitigation settings remain standard
- Detect external cooling solutions or modifications
- Validate that performance profiles are stock settings

### Result Validation
- Compare temperature readings across multiple sensors
- Verify performance measurements against hardware limits
- Check for impossible performance consistency
- Validate throttling behavior against known thermal models

## Technical Specifications

### CPU Workload Details
From the multi-core CPU benchmark:
- All available CPU cores utilized at maximum capacity
- Mixed integer and floating-point operations
- Memory bandwidth stressing routines
- Cache thrashing patterns to maximize thermal output

### GPU Workload Details  
From the native GPU benchmark:
- Maximum vertex and fragment shader complexity
- High-intensity compute shader operations
- Maximum texture sampling and fill rate stress
- Continuous rendering to prevent power-saving modes

### Environmental Requirements
- Ambient temperature: 23°C ± 2°C (73°F ± 4°F)
- Stable power source recommended for extended tests
- No external cooling or heating sources
- Flat, hard surface for consistent thermal behavior

## Data Collected

During the throttle test, the following data is collected and stored in the database:

### Benchmark Results
- Overall benchmark score and performance grade
- Test duration and completion timestamps
- App version and verification status
- Global and category rankings

### Test Environment Data
- Ambient temperature during testing
- Battery levels at start and end
- Charging status and screen brightness
- WiFi, Bluetooth, and mobile data status
- Number of running applications
- Available RAM and storage
- Kernel version and build fingerprint
- Device temperature at start (CPU, GPU, battery)

### Throttle Test Details
- Test duration in minutes
- Initial and sustained performance scores
- Performance retention percentage
- Time to throttle in seconds
- Maximum and average temperatures (CPU, GPU, battery)
- Thermal efficiency score and throttling percentage
- Temperature and performance curve data
- CPU and GPU frequency data

### Full Benchmark Details (for comparison)
- CPU, AI/ML, GPU, RAM, Storage, and Productivity scores
- Detailed test results for each category in JSON format
- Performance metrics from all test subcategories

### Telemetry Data
- CPU, GPU, and battery temperature timelines
- CPU and GPU frequency timelines
- Battery level and memory usage timelines
- Power consumption timeline
- Thermal throttle events and performance state timeline
- Average and maximum temperature/frequency values
- Total throttle events and battery drain percentage
- Average and peak power consumption values