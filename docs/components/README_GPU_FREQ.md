# Accurate GPU Frequency Monitoring Implementation with Root Access

## Overview

This project implements an accurate, real-time GPU frequency monitoring system for an Android device information app. The implementation leverages ROOT access to read GPU frequency data from kernel sysfs interfaces, similar to how SmartPack-Kernel Manager achieves accurate readings.

## Analysis of SmartPack-Kernel-Manager Approach

The implementation is based on analysis of the SmartPack-Kernel-Manager project, which provides proven GPU frequency reading approaches for different GPU vendors.

### Key Sysfs Paths Used

#### ADRENO GPU (Qualcomm) - PRIMARY PATHS:
- Current Frequency:
  - `/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq` (Primary path)
  - `/sys/class/kgsl/kgsl-3d0/gpuclk` (Alternative)
  - `/sys/class/kgsl/kgsl-3d0/clock_mhz` (Some kernels)
  - `/sys/devices/platform/kgsl-3d0.0/kgsl/kgsl-3d0/devfreq/cur_freq`

- Maximum Frequency:
  - `/sys/class/kgsl/kgsl-3d0/devfreq/max_freq` (Primary max)
  - `/sys/class/kgsl/kgsl-3d0/max_gpuclk` (Alternative max)
  - `/sys/class/kgsl/kgsl-3d0/gpu_max_clock`

- Minimum Frequency:
 - `/sys/class/kgsl/kgsl-3d0/devfreq/min_freq`
 - `/sys/class/kgsl/kgsl-3d0/gpu_min_clock`

- Available Frequencies:
  - `/sys/class/kgsl/kgsl-3d0/devfreq/available_frequencies`
  - `/sys/class/kgsl/kgsl-3d0/gpu_available_frequencies`

#### MALI GPU (ARM) - PRIMARY PATHS:
- Current Frequency:
  - `/sys/class/misc/mali0/device/devfreq/devfreq*/cur_freq`
  - `/sys/devices/platform/*.mali/devfreq/devfreq*/cur_freq`
  - `/sys/kernel/gpu/gpu_clock` (GED_SKI Driver)

- Maximum Frequency:
 - `/sys/class/misc/mali0/device/devfreq/devfreq*/max_freq`
  - `/sys/devices/platform/*.mali/devfreq/devfreq*/max_freq`
  - `/sys/kernel/gpu/gpu_max_clock`

- Minimum Frequency:
  - `/sys/class/misc/mali0/device/devfreq/devfreq*/min_freq`
  - `/sys/devices/platform/*.mali/devfreq/devfreq*/min_freq`
  - `/sys/kernel/gpu/gpu_min_clock`

#### POWERVR GPU (Imagination) - PRIMARY PATHS:
- Current Frequency:
  - `/sys/devices/platform/pvrsrvkm/sgx_clk_freq`
  - `/sys/kernel/debug/pvr/sgx_clk_freq_read`

#### TEGRA GPU (NVIDIA) - PRIMARY PATHS:
- Current Frequency:
 - `/sys/kernel/debug/clock/gbus/rate`
  - `/sys/devices/platform/host1x/gk20a.0/devfreq/gk20a.0/cur_freq`

## Implementation Components

### 1. RootCommandExecutor
- Executes shell commands with root privileges
- Reads protected sysfs files that require root
- Handles timeout scenarios (command hangs)
- Implements proper error handling for denied permissions
- Caches root access status to avoid repeated checks

### 2. GpuVendorDetector
- Automatically detects GPU vendor (Adreno, Mali, PowerVR, Tegra, etc.)
- Uses OpenGL renderer string: `GLES20.glGetString(GLES20.GL_RENDERER)`
- Reads from sysfs: `/sys/class/kgsl/kgsl-3d0/gpu_model`
- Uses device fingerprint: `Build.HARDWARE`, `Build.DEVICE`, `Build.BOARD`

### 3. GpuPaths
- Comprehensive sysfs path configuration for different GPU vendors
- Handles wildcard paths (e.g., `devfreq*`)
- Provides fallback paths for different kernel versions

### 4. GpuFrequencyReader
- Main class for reading GPU frequency data
- Implements caching for performance optimization
- Supports non-root fallback methods
- Provides detailed error handling

### 5. GpuFrequencyMonitor
- Real-time monitoring with Flow-based updates
- Configurable refresh rate (default: 500ms)
- Background monitoring with proper lifecycle management

### 6. GpuFrequencyCache
- Caches static values that don't change frequently
- Improves performance by avoiding repeated file reads
- Time-based cache expiration (5 seconds)

### 7. GpuFrequencyFallback
- Non-root accessible paths for some devices
- Frame timing estimation as fallback
- Graceful degradation when root unavailable

### 8. UI Components
- `GpuFrequencyCard.kt`: Composable UI component for displaying GPU frequency
- `GpuInfoViewModel.kt`: ViewModel for managing GPU frequency state

## Performance Optimizations

### Caching Strategy
- Static values cached (vendor, max/min freq, available freqs)
- File content caching to avoid repeated reads
- 5-second cache expiry to balance performance and accuracy

### Efficient Root Commands
- Single command execution for multiple file reads when possible
- Background processing using coroutines with IO dispatcher
- Proper cleanup and resource management

### Monitoring
- Configurable refresh rate to balance accuracy and battery life
- Proper lifecycle management in ViewModel
- Efficient StateFlow for UI updates

## Error Handling

### Handle These Scenarios:
1. **Root Permission Denied**: Clear message to user, offer to retry
2. **File Not Found**: Try all paths in priority order, log which paths were attempted
3. **Parse Errors**: Handle non-numeric values, empty files, files with multiple values
4. **Timeout**: Set 2-second timeout for root commands, cancel hanging operations
5. **Different SoC Variants**: Same GPU vendor, different paths
6. **SELinux Restrictions**: Some paths blocked even with root
7. **Kernel Version Differences**: Older kernels may not expose certain sysfs files

### Logging Strategy
- Debug builds: Log to Logcat
- Diagnostic reports with device model, GPU detected, all paths tried, success/failure for each

## Device Compatibility

### Tested Vendors:
- Qualcomm Adreno (Snapdragon SoCs)
- ARM Mali (Samsung Exynos, MediaTek, etc.)
- Imagination PowerVR
- NVIDIA Tegra

### Known Limitations:
- Custom kernels may not expose GPU frequency information
- Stock kernels may have limited sysfs exposure
- SELinux enforcing may block certain paths even with root

## Usage

### In Compose UI:
```kotlin
@Composable
fun GpuInfoScreen() {
    GpuFrequencyCard()
}
```

### Direct Usage:
```kotlin
val gpuFrequencyReader = GpuFrequencyReader()
val result = gpuFrequencyReader.readGpuFrequency()
```

### Real-time Monitoring:
```kotlin
val gpuFrequencyMonitor = GpuFrequencyMonitor(gpuFrequencyReader, scope)
gpuFrequencyMonitor.startMonitoring()
```

## Troubleshooting

### Common Issues:
1. **Root not detected**: Ensure proper root management app is installed
2. **No frequency data**: Check if device kernel exposes GPU frequency information
3. **Permission denied**: Some paths may be restricted by SELinux
4. **Inaccurate readings**: May indicate kernel limitations

### Debugging:
- Enable logging to see which paths are being tried
- Check logcat for specific error messages
- Verify root access using `RootUtils.canExecuteRootCommand()`

## Security Considerations

- All operations are performed with proper root permissions
- No sensitive data is collected beyond GPU frequency
- All sysfs paths are validated before access
- Proper error handling prevents app crashes

## License

This implementation is based on analysis of SmartPack-Kernel Manager and follows similar licensing principles.