# External GPU Engine Benchmark Algorithms & Methodology

## Summary Table

| Test Name | Engine | Primary Focus | Performance Metric |
|-----------|--------|---------------|-------------------|
| Unity Benchmark Scene 1 | Unity 3D | Rendering performance, lighting | FPS, frame time consistency |
| Unity Benchmark Scene 2 | Unity 3D | Compute operations, particle systems | Compute performance, particle throughput |
| Unreal Benchmark Scene 1 | Unreal Engine | Advanced rendering, shadows | FPS, shadow rendering efficiency |
| Unreal Benchmark Scene 2 | Unreal Engine | Physics simulation, collision | Physics performance, collision accuracy |
| Unreal Benchmark Scene 3 | Unreal Engine | Ray tracing, reflections | Ray tracing performance, reflection quality |

## 1. Introduction

### Purpose of External GPU Engine Benchmarking

External GPU engine benchmarking evaluates the graphics processing capabilities of a device using popular commercial game engines. These tests measure how effectively the device can execute complex rendering operations using industry-standard engines that power many mobile games and applications. The benchmarks use separate APKs to ensure optimal engine performance and access to specialized features.

The external engine benchmarks focus on:
- Real-world engine performance with optimized rendering pipelines
- Shader compilation and execution efficiency
- Memory management and asset streaming
- Engine-specific feature utilization (lighting, physics, post-processing)
- Cross-platform performance consistency

### Architecture Overview

The benchmarking system follows a modular architecture with separate APKs for each engine:

```
Main Benchmarking App (Kotlin)
├── CPU Tests
├── AI/ML Tests  
├── GPU Tests (Native Kotlin)
└── External Engine Tests
    ├── Unity Scenes (2 scenes) → Separate APK
    └── Unreal Scenes (3 scenes) → Separate APK
```

This architecture allows each engine to run in its optimized environment while maintaining centralized result collection and reporting.

### Performance Aspects Evaluated

External GPU engine benchmark tests evaluate multiple dimensions of graphics processing performance:

- **Rendering Pipeline Performance**: Measures vertex, geometry, and fragment shader efficiency within engines
- **Memory Bandwidth Utilization**: Tests texture streaming and asset management capabilities
- **Compute Shader Performance**: Evaluates engine's use of GPU compute for physics, particles, etc.
- **Engine Optimization Efficiency**: Assesses how well engines utilize hardware features
- **Thermal Management**: Tests sustained performance under engine workloads
- **Feature Utilization**: Measures performance with engine-specific features enabled

### Test Selection Rationale

The selected external engine tests were chosen based on their real-world relevance and computational diversity:

- **Real-world Relevance**: Each test represents common game engine workloads found in actual mobile games
- **Engine Diversity**: Tests cover different commercial game engines with distinct architectures
- **Feature Coverage**: Tests vary in their use of lighting, physics, post-processing, and other engine features
- **Measurable Performance Differences**: Tests are designed to show meaningful performance differences across different hardware configurations

## 2. Test List

### External Engine Test 1: Unity Benchmark Scene 1

**Engine Used**: Unity 3D (version 2022.3 LTS or latest)
The test implements a comprehensive rendering scene using Unity's Universal Render Pipeline (URP) or High Definition Render Pipeline (HDRP). The scene includes multiple light sources, complex materials, and animated objects to stress the rendering pipeline. The implementation is packaged as a separate APK for optimal Unity performance.

**Complexity Analysis**: O(n) for n objects with O(m) lights and O(p) materials
The rendering complexity scales with the number of objects, lights, and materials in the scene. The space complexity is O(n) for scene data and GPU resources.

**Dataset/Workload Details**: 
- Scene complexity: Multiple objects with different materials and textures
- Lighting: Multiple dynamic lights with shadows
- Animation: Animated objects and light movement
- Post-processing: Anti-aliasing, bloom, color grading
- Threading model: Unity's multi-threaded rendering pipeline

**Measurements Captured**:
- Frames per second (FPS) at different quality settings
- Frame time consistency and variance
- Memory usage during rendering
- GPU utilization percentage
- Shader compilation time and cache efficiency

**Hardware Behavior Targeted**:
- Rendering pipeline efficiency in Unity
- Memory bandwidth for asset streaming
- Shader execution performance
- Multi-threaded rendering pipeline
- GPU driver optimization for Unity

**Notes on Deterministic Behavior**: Results are deterministic for the same hardware and scene settings, though frame rates will vary based on GPU performance.

### External Engine Test 2: Unity Benchmark Scene 2

**Engine Used**: Unity 3D (version 2022.3 LTS or latest)
The test focuses on compute operations and particle systems within Unity. The scene includes complex particle simulations, compute shader operations, and advanced effects that stress the GPU's compute capabilities. The implementation uses Unity's Job System and Burst Compiler for optimal performance.

**Complexity Analysis**: O(n) for n particles with O(c) compute operations
The complexity scales with particle count and compute operations per frame. The space complexity is O(n) for particle data and compute buffers.

**Dataset/Workload Details**:
- Particle systems: 100K+ particles with physics simulation
- Compute shaders: Complex calculations on GPU
- Effects: Advanced post-processing and effects
- Multi-threading: Unity Job System and Burst Compiler
- Quality settings: Adjustable to match device capabilities

**Measurements Captured**:
- Particles processed per second
- Compute shader execution performance
- Memory bandwidth utilization during compute
- GPU compute unit utilization
- Power consumption during intensive operations

**Hardware Behavior Targeted**:
- GPU compute performance in Unity
- Particle system efficiency
- Compute shader optimization
- Memory bandwidth for compute operations
- Multi-threaded compute execution

**Notes on Deterministic Behavior**: Results are deterministic for the same initial conditions, though visual patterns may vary due to physics interactions.

### External Engine Test 3: Unreal Benchmark Scene 1

**Engine Used**: Unreal Engine (version 5.3 or latest)
The test implements an advanced rendering scene using Unreal's rendering pipeline with Nanite and Lumen features. The scene includes complex geometry, advanced lighting, and high-quality materials to stress modern GPU capabilities. The implementation is packaged as a separate APK for optimal Unreal performance.

**Complexity Analysis**: O(n) for n polygons with O(l) lights and O(f) effects
The rendering complexity scales with polygon count, lights, and post-processing effects. The space complexity is O(n) for scene data and GPU resources.

**Dataset/Workload Details**:
- Geometry: Nanite virtualized geometry system
- Lighting: Lumen global illumination
- Materials: Complex PBR materials with multiple textures
- Post-processing: Advanced effects and anti-aliasing
- Quality settings: Scalable based on device tier

**Measurements Captured**:
- Frames per second (FPS) with advanced features
- Rendering quality metrics
- Memory usage with virtualized geometry
- GPU utilization during advanced rendering
- Feature-specific performance metrics

**Hardware Behavior Targeted**:
- Advanced rendering pipeline efficiency
- Nanite geometry virtualization
- Lumen lighting system
- Memory bandwidth for complex assets
- GPU driver optimization for Unreal

**Notes on Deterministic Behavior**: Results are deterministic for the same hardware and scene settings, though frame rates will vary based on GPU performance.

### External Engine Test 4: Unreal Benchmark Scene 2

**Engine Used**: Unreal Engine (version 5.3 or latest)
The test focuses on physics simulation and collision detection within Unreal Engine. The scene includes complex physics interactions, destructible objects, and collision systems that stress the CPU-GPU pipeline. The implementation uses Unreal's Chaos physics system for advanced simulations.

**Complexity Analysis**: O(n²) for n objects with collision detection
The physics complexity scales quadratically with object count for collision detection. The space complexity is O(n) for physics data and simulation state.

**Dataset/Workload Details**:
- Physics objects: Multiple rigid bodies with constraints
- Collision detection: Complex mesh collisions
- Destructible objects: Fracture and destruction systems
- Simulation: Real-time physics with constraints
- Quality settings: Adjustable physics fidelity

**Measurements Captured**:
- Physics simulation performance (updates/sec)
- Collision detection accuracy
- Frame rate with physics simulation
- Memory usage during physics simulation
- CPU-GPU synchronization overhead

**Hardware Behavior Targeted**:
- Physics simulation performance
- Collision detection efficiency
- Memory bandwidth for physics data
- CPU-GPU pipeline efficiency
- Multi-threaded physics execution

**Notes on Deterministic Behavior**: Results are deterministic for the same initial conditions, though physics interactions may vary based on timing.

### External Engine Test 5: Unreal Benchmark Scene 3

**Engine Used**: Unreal Engine (version 5.3 or latest)
The test focuses on ray tracing and reflection effects using Unreal's ray tracing capabilities. The scene includes real-time ray-traced reflections, shadows, and global illumination to stress modern GPU ray tracing hardware. The implementation uses Unreal's ray tracing features for advanced lighting effects.

**Complexity Analysis**: O(n) for n rays with O(o) objects in scene
The ray tracing complexity scales with ray count and scene complexity. The space complexity is O(n) for ray tracing acceleration structures.

**Dataset/Workload Details**:
- Ray tracing: Real-time reflections and shadows
- Global illumination: Ray-traced lighting
- Acceleration structures: BVH and other acceleration methods
- Quality settings: Adjustable ray count and quality
- Hardware features: Utilizes RT cores when available

**Measurements Captured**:
- Ray tracing performance (rays/sec)
- Reflection quality metrics
- Frame rate with ray tracing enabled
- Memory usage for acceleration structures
- RT core utilization (when available)

**Hardware Behavior Targeted**:
- Ray tracing performance
- RT core efficiency (when available)
- Memory bandwidth for acceleration structures
- Ray tracing pipeline efficiency
- Shader execution for ray tracing

**Notes on Deterministic Behavior**: Results are deterministic for the same hardware and scene settings, though performance will vary based on ray tracing capabilities.

## 3. Scaling & Load Adjustment

### Rules for Adapting Workload to Device Tier

The benchmark automatically adjusts workload parameters based on device performance tier to ensure meaningful results across all device classes:

**Slow Tier Devices** (entry-level smartphones, budget tablets):
- Unity Scenes: Reduced object count, basic lighting, minimal post-processing
- Unreal Scenes: Lower polygon count, simplified materials, disabled advanced features
- Quality settings: Performance-focused presets
- Resolution: 720p for intensive scenes

**Mid Tier Devices** (mid-range smartphones, mainstream tablets):
- Unity Scenes: Moderate object count, standard lighting, some post-processing
- Unreal Scenes: Medium polygon count, standard materials, selective advanced features
- Quality settings: Balanced presets
- Resolution: 1080p for most scenes

**Flagship Tier Devices** (high-end smartphones, premium tablets):
- Unity Scenes: High object count, complex lighting, full post-processing
- Unreal Scenes: High polygon count, complex materials, all advanced features enabled
- Quality settings: Quality-focused presets
- Resolution: 1440p/4K for capable devices

### APK Communication Protocol

**Intent-Based Communication**:
- Main app launches external engine APKs via Intents
- Results passed back through Intent extras
- Deep linking integration for seamless experience
- Bi-directional data passing for configuration

**Result Aggregation**:
- External APKs return structured performance data
- Main app collects and normalizes results
- Results integrated into overall benchmark score
- Error handling for failed engine launches

### Dynamic Warm-up Rules

To ensure accurate measurements and account for system optimizations:

**Engine Warm-up**:
- Execute each engine scene multiple times (2-3 iterations) before timing begins
- Allow engine to compile shaders and load assets
- Monitor performance stabilization before starting timed measurements
- Perform warm-up runs to prime engine caches and pipelines

**Asset Loading**:
- Pre-load all required assets before timing
- Clear engine caches before benchmark to ensure consistency
- Monitor memory pressure during testing

**Iteration Count for Each Benchmark**:
Each external engine benchmark test is executed 3 times for statistical validity, and the results are averaged to produce the final score. The median result is used for scoring to reduce the impact of outliers.

**Rendering Tests**:
- Each test runs for 60 seconds of continuous rendering to account for thermal throttling
- Results are averaged after removing outliers (values >15% deviation from median)
- Minimum run time ensures thermal stabilization and consistent results

**Compute/Physics Tests**:
- Each test runs 3 times with warm-up iterations before timing
- Results are averaged after removing outliers (values >15% deviation from median)
- Minimum run time of 30 seconds per iteration to ensure measurement accuracy

## 4. Performance Metrics

### Rendering Performance

Rendering performance is the primary metric for engine-based GPU benchmark tests, measured from the start of the actual rendering to completion. The system captures:

- **Frames Per Second (FPS)**: Primary metric for interactive applications
- **Frame Time**: Inverse of FPS, important for consistency analysis
- **Frame Time Variance**: Jitter measurements across multiple frames
- **Render Latency**: Time from command submission to completion

### Engine-Specific Metrics

Engine-specific metrics measure the performance of game engine features:

- **Shader Compilation Time**: Time to compile shaders on first use
- **Asset Streaming Performance**: Time to load and stream assets
- **Physics Update Rate**: Updates per second for physics simulation
- **Particle System Throughput**: Particles processed per second

### Resource Utilization

Resource metrics measure the computational efficiency of engine workloads:

- **GPU Memory Usage**: Peak and average memory consumption
- **System Memory Usage**: Engine's system memory footprint
- **Power Consumption**: Energy usage during engine operations (where available)
- **GPU Utilization**: Core usage during rendering operations
- **Thermal Impact**: Temperature changes during sustained engine usage

## 5. Scoring Conversion

### Formula for Converting Raw Performance to Normalized Score

The raw performance metrics are converted to normalized scores using:

```
Normalized Score = (Device Performance / Baseline Performance) × 10
```

For time-based metrics (lower is better):
```
Normalized Score = (Baseline Time / Device Time) × 100
```

For throughput-based metrics (higher is better):
```
Normalized Score = (Device Throughput / Baseline Throughput) × 100
```

### Baseline Reference Device Detail

The baseline device used for normalization is the Google Pixel 6 (2021) with:
- GPU: Mali-G78 MP20 (20-core configuration)
- Memory: 8GB LPDDR5
- Graphics API: Vulkan 1.2 support
- Engine compatibility: Unity 2022.3 LTS, Unreal 5.3
- Baseline performance values established through extensive testing

### External Engine Category Weight in Global Scoring

External engine benchmarks contribute 15% to the overall system benchmark score:
- Unity Performance: 6% of total score
- Unreal Performance: 9% of total score

## 6. Optimization & Anti-Cheat Policy

### Engine Integrity Verification

The system monitors for performance manipulation through:

**APK Integrity**:
- Verify that external engine APKs have not been modified
- Check APK signatures and checksums
- Validate that standard engine builds are used
- Monitor for custom engine modifications

**Engine Settings**:
- Ensure default quality settings are used
- Monitor for modified configuration files
- Validate that standard rendering paths are used
- Check for performance-enhancing modifications

### Performance Monitoring

**Resource Usage Monitoring**:
- Track GPU usage by other processes during benchmark
- Monitor memory pressure from background applications
- Detect interference from other GPU-intensive processes
- Log system load during benchmark execution

**Thermal State Monitoring**:
- Monitor GPU temperature throughout benchmark
- Detect thermal throttling events
- Account for pre-existing thermal conditions
- Adjust scoring based on thermal limitations

### Rules for Rejecting Invalid Runs

An external engine benchmark run is rejected if:

- External APK has been modified or replaced
- Engine settings have been altered from standard configurations
- More than 5% of GPU time is consumed by other processes
- Thermal throttling occurs during performance-critical sections
- GPU memory allocation fails during rendering
- Unexpected system events interrupt the benchmark
- Results deviate significantly (>20%) from previous runs
- Modified engine builds or custom modifications detected

### Integrity Validation

**Communication Integrity**:
- Verify that results are properly communicated from external APKs
- Check for tampering with result data during transmission
- Validate that timing functions return accurate measurements
- Ensure no external performance enhancement tools are active

**Result Validation**:
- Verify results fall within expected ranges
- Check for impossible performance results
- Cross-reference with device hardware capabilities
- Validate consistency across multiple test runs