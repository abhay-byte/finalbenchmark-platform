# Efficiency Test (Performance per Watt)

## Overview

The Efficiency Test evaluates the power efficiency of a device by measuring performance relative to power consumption. This test runs the same intensive workload as the throttle test but for a fixed duration of 1 minute, focusing specifically on how efficiently the device utilizes power while maintaining performance. The test provides insights into battery life implications and thermal efficiency under sustained load.

Duration: 1 minute
Primary output: Efficiency score, battery drain rate, power consumption metrics

## Test Configuration

### Workload
The efficiency test runs the following benchmarks simultaneously:
- **CPU Component**: Multi-core stress test from [CPU Benchmark](../benchmark/cpu.md)
- **GPU Component**: Native GPU test from [GPU Native Benchmark](../benchmark/gpu_native.md)

This is identical to the workload used in the [Throttle Test](./throttle.md), ensuring consistent performance measurement for efficiency calculations.

### Duration
- **Fixed Duration**: 1 minute (60 seconds)
- **Continuous Monitoring**: Power consumption and performance metrics recorded throughout

## Measurements Captured

### Primary Outputs

**Efficiency Score**:
```
Efficiency Score = (Performance Output / Power Consumption) × 1000
```
Where:
- Performance Output = Average performance score during the test
- Power Consumption = Average power consumption in watts during the test
- Scale: 0-15,000 points

**Battery Drain Rate**:
- Initial battery level vs. final battery level
- Drain rate in mAh per minute
- Percentage drain per minute

**Performance per Watt**:
- Direct calculation of performance units per watt consumed
- Average sustained performance during the test period

### Secondary Metrics

**Power Consumption Analysis**:
- Average power consumption in watts
- Peak power consumption during test
- Power consumption timeline data
- Correlation between performance and power draw

**Temperature Impact**:
- Average CPU temperature during test
- Average GPU temperature during test
- Average battery temperature during test
- Temperature vs. efficiency correlation

**Frequency Analysis**:
- Peak CPU frequency achieved
- Peak GPU frequency achieved
- Frequency stability during test
- Throttling impact on efficiency

## Scoring Methodology

### Efficiency Score Range
- **A+**: >12,000 points (Exceptional efficiency)
- **A**: 10,000-12,000 points (Excellent efficiency)
- **B+**: 8,000-10,000 points (Good efficiency)
- **B**: 6,000-8,000 points (Above average efficiency)
- **C**: 4,000-6,000 points (Average efficiency)
- **D**: 2,000-4,000 points (Below average efficiency)
- **F**: <2,000 points (Poor efficiency)

### Performance Grading
The efficiency score is converted to a letter grade based on the above ranges, providing an easy-to-understand assessment of the device's power efficiency.

## Test Execution Sequence

### Pre-test Preparation
1. **System State Verification**:
   - Ensure device battery level >50%
   - Close background applications
   - Verify power monitoring sensors are functional
   - Record ambient temperature

2. **Initial Measurements**:
   - Record baseline battery level
   - Record baseline temperatures (CPU, GPU, battery)
   - Measure idle power consumption
   - Establish baseline performance metrics

### Test Execution
1. **Warm-up Phase (5 seconds)**:
   - Begin CPU multi-core test
   - Begin GPU native test
   - Allow workloads to reach steady state
   - Monitor for immediate power draw changes

2. **Monitoring Phase (55 seconds)**:
   - Continuously record performance metrics at 1Hz intervals
   - Monitor power consumption in real-time
   - Track battery drain rate
   - Record temperature progression
   - Log frequency changes

3. **Finalization Phase (10 seconds)**:
   - Continue monitoring as workloads conclude
   - Record final battery level
   - Capture final temperature readings
   - Compile efficiency calculations

### Post-test Analysis
1. **Data Aggregation**:
   - Calculate average performance output
   - Calculate average power consumption
   - Compute efficiency score
   - Generate battery drain analysis
   - Create efficiency timeline data

2. **Scoring and Grading**:
   - Apply efficiency scoring formula
   - Assign performance grade
   - Generate efficiency report

## Technical Specifications

### Environmental Requirements
- Ambient temperature: 23°C ± 2°C (73°F ± 4°F)
- Stable power source recommended
- No external cooling or heating sources
- Flat, hard surface for consistent thermal behavior

### Monitoring Requirements
- Power consumption monitoring (if available through system APIs)
- Battery level monitoring
- Temperature sensor access for CPU, GPU, and battery
- CPU/GPU frequency monitoring
- Performance counters for sustained workload measurement

## Anti-Cheat Measures

### System Integrity Verification
- Monitor for power management modifications during test
- Verify thermal mitigation settings remain standard
- Detect external power sources or modifications
- Validate that performance profiles are stock settings

### Result Validation
- Compare power consumption readings across available sensors
- Verify performance measurements against hardware limits
- Check for impossible efficiency consistency
- Validate efficiency behavior against known thermal models

## Data Collected

The efficiency test collects comprehensive data on power efficiency and performance:

**Performance Metrics** (from EFFICIENCY_TEST_DETAILS table):
- Performance output score
- Overall efficiency score
- Performance per watt measurement

**Power Consumption Data**:
- Average power consumption in watts
- Power consumption timeline data (in JSONB format)

**Battery Metrics**:
- Battery drain in mAh
- Battery drain percentage

**Thermal Data**:
- Average CPU temperature during test
- Average GPU temperature during test
- Average battery temperature during test

**Frequency Information**:
- Peak CPU frequency achieved (GHz)
- Peak GPU frequency achieved (MHz)

This data enables detailed analysis of how efficiently the device utilizes power while maintaining performance under sustained load.