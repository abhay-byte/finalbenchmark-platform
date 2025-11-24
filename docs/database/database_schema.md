# Database Schema Documentation

This document describes the database schema for the benchmark application based on the ER diagram.

## Table of Contents
- [Users Table](#users-table)
- [Devices Table](#devices-table)
- [Device Specifications Table](#device-specifications-table)
- [SOC/APU Specifications Table](#socapu-specifications-table)
- [CPU Specifications Table](#cpu-specifications-table)
- [GPU Specifications Table](#gpu-specifications-table)
- [RAM Specifications Table](#ram-specifications-table)
- [Storage Specifications Table](#storage-specifications-table)
- [User Devices Table](#user-devices-table)
- [Benchmark Results Table](#benchmark-results-table)
- [Test Environment Table](#test-environment-table)
- [Full Benchmark Details Table](#full-benchmark-details-table)
- [Full Benchmark Telemetry Table](#full-benchmark-telemetry-table)
- [Throttle Test Details Table](#throttle-test-details-table)
- [Efficiency Test Details Table](#efficiency-test-details-table)
- [CPU Test Results Table](#cpu-test-results-table)
- [AI/ML Test Results Table](#aiml-test-results-table)
- [GPU Test Results Table](#gpu-test-results-table)
- [GPU Frame Metrics Table](#gpu-frame-metrics-table)
- [RAM Test Results Table](#ram-test-results-table)
- [Storage Test Results Table](#storage-test-results-table)
- [Productivity Test Results Table](#productivity-test-results-table)
- [Leaderboard Cache Table](#leaderboard-cache-table)
- [Comparison AI Cache Table](#comparison-ai-cache-table)
- [Device Statistics Table](#device-statistics-table)
- [System Logs Table](#system-logs-table)
- [Device Summary View](#device-summary-view)
- [SOC Summary View](#soc-summary-view)
- [Leaderboard Tables](#leaderboard-tables)
- [Comparison Cache Table](#comparison-cache-table)

## Users Table

The `USERS` table stores user account information.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|
| user_id | BIGINT | PRIMARY KEY | Unique identifier for the user |
| email | VARCHAR | UNIQUE | User's email address |
| username | VARCHAR | UNIQUE | User's chosen username |
| password_hash | VARCHAR | NOT NULL | Hashed password |
| first_name | VARCHAR | | User's first name |
| last_name | VARCHAR | | User's last name |
| profile_picture_url | VARCHAR | | URL to user's profile picture |
| account_type | ENUM('guest', 'registered') | NOT NULL | Type of account |
| auth_provider | ENUM('local', 'google', 'facebook', 'apple') | NOT NULL | Authentication provider |
| external_auth_id | VARCHAR | | External authentication ID |
| email_verified | BOOLEAN | DEFAULT FALSE | Whether email is verified |
| email_verified_at | DATETIME | | Timestamp when email was verified |
| verification_token | VARCHAR | | Token for email verification |
| reset_token | VARCHAR | | Token for password reset |
| reset_token_expiry | DATETIME | | Expiration time for reset token |
| created_at | DATETIME | NOT NULL | Timestamp when account was created |
| updated_at | DATETIME | NOT NULL | Timestamp when account was last updated |
| last_login_at | DATETIME | | Timestamp of last login |
| last_login_ip | VARCHAR | | IP address of last login |
| is_active | BOOLEAN | DEFAULT TRUE | Whether account is active |
| is_deleted | BOOLEAN | DEFAULT FALSE | Whether account is deleted |
| country_code | VARCHAR | Country code of user |
| timezone | VARCHAR | | Timezone of user |
| preferences | JSON | | User preferences |

## Devices Table

The `DEVICES` table stores information about various devices that can be benchmarked.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| device_id | BIGINT | PRIMARY KEY | Unique identifier for the device |
| device_name | VARCHAR | UNIQUE | Name of the device |
| manufacturer | VARCHAR | NOT NULL | Device manufacturer |
| model_number | VARCHAR | NOT NULL | Model number of the device |
| marketing_name | VARCHAR | | Marketing name of the device |
| device_code_name | VARCHAR | | Code name of the device |
| device_type | ENUM('phone', 'tablet', 'foldable', 'laptop', 'desktop', 'mini_pc') | NOT NULL | Type of device |
| platform | ENUM('android', 'windows', 'linux', 'macos', 'ios') | NOT NULL | Platform of the device |
| release_date | DATE | | Release date of the device |
| release_price | DECIMAL | | Release price of the device |
| image_url | VARCHAR | | URL to device image |
| created_at | DATETIME | NOT NULL | Timestamp when device was added |
| updated_at | DATETIME | NOT NULL | Timestamp when device was last updated |

## Device Specifications Table

The `DEVICE_SPECIFICATIONS` table contains detailed specifications for devices.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| spec_id | BIGINT | PRIMARY KEY | Unique identifier for the specification |
| device_id | BIGINT | FOREIGN KEY | Reference to the device |
| soc_apu_spec_id | BIGINT | FOREIGN KEY | Reference to SOC/APU specification |
| cpu_spec_id | BIGINT | FOREIGN KEY | Reference to CPU specification |
| gpu_spec_id | BIGINT | FOREIGN KEY | Reference to GPU specification |
| ram_spec_id | BIGINT | FOREIGN KEY | Reference to RAM specification |
| storage_spec_id | BIGINT | FOREIGN KEY | Reference to storage specification |
| is_integrated_system | BOOLEAN | DEFAULT FALSE | Whether it's an integrated system |
| display_size_inch | DECIMAL | | Display size in inches |
| display_width_px | INT | | Display width in pixels |
| display_height_px | INT | | Display height in pixels |
| display_refresh_rate_hz | INT | | Display refresh rate in Hz |
| display_panel_type | VARCHAR | | Type of display panel |
| battery_capacity_mah | INT | | Battery capacity in mAh |
| charging_type | VARCHAR | | Type of charging |
| charging_max_watt | INT | | Maximum charging power in watts |
| os_version | VARCHAR | | OS version |
| os_build_number | VARCHAR | | OS build number |
| os_api_level | INT | OS API level |
| weight_grams | DECIMAL | | Weight in grams |
| thickness_mm | DECIMAL | | Thickness in mm |
| build_material | VARCHAR | | Material used in build |
| ip_rating | VARCHAR | | IP rating |
| has_5g | BOOLEAN | DEFAULT FALSE | Whether device has 5G |
| has_nfc | BOOLEAN | DEFAULT FALSE | Whether device has NFC |
| has_wireless_charging | BOOLEAN | DEFAULT FALSE | Whether device has wireless charging |
| motherboard_model | VARCHAR | | Motherboard model |
| bios_version | VARCHAR | | BIOS version |
| total_usb_ports | INT | | Total number of USB ports |
| has_thunderbolt | BOOLEAN | DEFAULT FALSE | Whether device has Thunderbolt |
| cooling_type | VARCHAR | | Type of cooling system |
| fan_count | INT | | Number of fans |
| created_at | DATETIME | NOT NULL | Timestamp when specification was created |
| updated_at | DATETIME | NOT NULL | Timestamp when specification was last updated |

## SOC/APU Specifications Table

The `SOC_APU_SPECS` table stores information about System-on-Chip and Accelerated Processing Unit specifications.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| soc_apu_spec_id | BIGINT | PRIMARY KEY | Unique identifier for the SOC/APU specification |
| chip_name | VARCHAR | UNIQUE | Name of the chip |
| chip_type | ENUM('soc', 'apu') | NOT NULL | Type of chip |
| manufacturer | VARCHAR | NOT NULL | Chip manufacturer |
| chip_family | VARCHAR | | Chip family |
| codename | VARCHAR | | Codename of the chip |
| cpu_spec_id | BIGINT | FOREIGN KEY | Reference to CPU specification |
| gpu_spec_id | BIGINT | FOREIGN KEY | Reference to GPU specification |
| process_node | VARCHAR | | Process node of the chip |
| total_tdp_watts | INT | | Total TDP in watts |
| min_tdp_watts | INT | | Minimum TDP in watts |
| max_tdp_watts | INT | | Maximum TDP in watts |
| supported_memory_types | VARCHAR | | Supported memory types |
| max_memory_speed_mhz | INT | | Maximum memory speed in MHz |
| npu_model | VARCHAR | | NPU model |
| npu_tops | DECIMAL | | NPU performance in TOPS |
| release_date | DATE | | Release date of the chip |
| avg_full_benchmark_score | DECIMAL | | Average full benchmark score |
| avg_throttle_score | DECIMAL | | Average throttle score |
| avg_efficiency_score | DECIMAL | | Average efficiency score |
| avg_gpu_test_metrics | JSONB | | Average GPU test metrics |
| avg_cpu_category_metrics | JSONB | | Average CPU category metrics |
| avg_ai_ml_category_metrics | JSONB | | Average AI/ML category metrics |
| avg_ram_category_metrics | JSONB | | Average RAM category metrics |
| avg_storage_category_metrics | JSONB | | Average storage category metrics |
| avg_productivity_category_metrics | JSONB | | Average productivity category metrics |
| created_at | DATETIME | NOT NULL | Timestamp when specification was created |
| updated_at | DATETIME | NOT NULL | Timestamp when specification was last updated |

## CPU Specifications Table

The `CPU_SPECS` table contains detailed CPU specifications.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| cpu_spec_id | BIGINT | PRIMARY KEY | Unique identifier for the CPU specification |
| cpu_brand | VARCHAR | NOT NULL | CPU brand |
| cpu_model | VARCHAR | UNIQUE | CPU model |
| cpu_family | VARCHAR | | CPU family |
| cpu_architecture | VARCHAR | CPU architecture |
| cpu_socket_type | VARCHAR | | CPU socket type |
| cpu_cores | INT | NOT NULL | Number of CPU cores |
| cpu_threads | INT | NOT NULL | Number of CPU threads |
| cpu_performance_cores | INT | | Number of performance cores |
| cpu_efficiency_cores | INT | | Number of efficiency cores |
| cpu_base_frequency_ghz | DECIMAL | | Base CPU frequency in GHz |
| cpu_boost_frequency_ghz | DECIMAL | | Boost CPU frequency in GHz |
| cpu_max_turbo_frequency_ghz | DECIMAL | | Maximum turbo frequency in GHz |
| cpu_process_node | VARCHAR | | CPU process node |
| cpu_tdp_watts | INT | | CPU TDP in watts |
| cpu_max_tdp_watts | INT | | Maximum CPU TDP in watts |
| l1_cache_kb | INT | | L1 cache size in KB |
| l2_cache_kb | INT | | L2 cache size in KB |
| l3_cache_kb | INT | | L3 cache size in KB |
| supports_hyperthreading | BOOLEAN | DEFAULT FALSE | Whether CPU supports hyperthreading |
| supports_overclocking | BOOLEAN | DEFAULT FALSE | Whether CPU supports overclocking |
| instruction_sets | VARCHAR | | Supported instruction sets |
| memory_channels | INT | | Number of memory channels |
| supported_memory_types | VARCHAR | | Supported memory types |
| max_memory_speed_mhz | INT | | Maximum memory speed in MHz |
| created_at | DATETIME | NOT NULL | Timestamp when specification was created |
| updated_at | DATETIME | NOT NULL | Timestamp when specification was last updated |

## GPU Specifications Table

The `GPU_SPECS` table contains detailed GPU specifications.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| gpu_spec_id | BIGINT | PRIMARY KEY | Unique identifier for the GPU specification |
| gpu_brand | VARCHAR | NOT NULL | GPU brand |
| gpu_model | VARCHAR | UNIQUE | GPU model |
| gpu_architecture | VARCHAR | GPU architecture |
| gpu_type | ENUM('integrated', 'dedicated', 'mobile') | NOT NULL | Type of GPU |
| gpu_cores | INT | | Number of GPU cores |
| gpu_shader_units | INT | | Number of shader units |
| gpu_compute_units | INT | | Number of compute units |
| gpu_texture_units | INT | | Number of texture units |
| gpu_rops | INT | | Number of ROPs |
| gpu_base_frequency_mhz | DECIMAL | | Base GPU frequency in MHz |
| gpu_boost_frequency_mhz | DECIMAL | | Boost GPU frequency in MHz |
| gpu_memory_size_gb | INT | | GPU memory size in GB |
| gpu_memory_type | VARCHAR | | GPU memory type |
| gpu_memory_bus_width | INT | | GPU memory bus width |
| gpu_memory_bandwidth_gbps | DECIMAL | | GPU memory bandwidth in GB/s |
| gpu_tdp_watts | INT | | GPU TDP in watts |
| gpu_process_node | VARCHAR | | GPU process node |
| supports_ray_tracing | BOOLEAN | DEFAULT FALSE | Whether GPU supports ray tracing |
| supports_dlss | BOOLEAN | DEFAULT FALSE | Whether GPU supports DLSS |
| supports_fsr | BOOLEAN | DEFAULT FALSE | Whether GPU supports FSR |
| directx_version | VARCHAR | DirectX version |
| opengl_version | VARCHAR | | OpenGL version |
| vulkan_version | VARCHAR | | Vulkan version |
| metal_version | VARCHAR | | Metal version |
| max_display_outputs | INT | | Maximum number of display outputs |
| created_at | DATETIME | NOT NULL | Timestamp when specification was created |
| updated_at | DATETIME | NOT NULL | Timestamp when specification was last updated |

## RAM Specifications Table

The `RAM_SPECS` table contains detailed RAM specifications.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| ram_spec_id | BIGINT | PRIMARY KEY | Unique identifier for the RAM specification |
| ram_size_gb | INT | UNIQUE | RAM size in GB |
| ram_type | VARCHAR | UNIQUE | RAM type |
| ram_generation | VARCHAR | | RAM generation |
| ram_frequency_mhz | DECIMAL | | RAM frequency in MHz |
| ram_modules_count | INT | | Number of RAM modules |
| ram_form_factor | VARCHAR | | RAM form factor |
| ram_cas_latency | INT | | RAM CAS latency |
| ram_voltage | DECIMAL | | RAM voltage |
| supports_xmp | BOOLEAN | DEFAULT FALSE | Whether RAM supports XMP |
| supports_expo | BOOLEAN | DEFAULT FALSE | Whether RAM supports EXPO |
| ram_channels | INT | | Number of RAM channels |
| is_ecc | BOOLEAN | DEFAULT FALSE | Whether RAM is ECC |
| ram_manufacturer | VARCHAR | | RAM manufacturer |
| created_at | DATETIME | NOT NULL | Timestamp when specification was created |
| updated_at | DATETIME | NOT NULL | Timestamp when specification was last updated |

## Storage Specifications Table

The `STORAGE_SPECS` table contains detailed storage specifications.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| storage_spec_id | BIGINT | PRIMARY KEY | Unique identifier for the storage specification |
| storage_size_gb | INT | UNIQUE | Storage size in GB |
| storage_type | VARCHAR | UNIQUE | Storage type |
| storage_interface | VARCHAR | | Storage interface |
| storage_form_factor | VARCHAR | Storage form factor |
| storage_pcie_generation | INT | | PCIe generation for storage |
| storage_pcie_lanes | INT | | Number of PCIe lanes |
| supports_nvme | BOOLEAN | DEFAULT FALSE | Whether storage supports NVMe |
| sequential_read_spec_mbps | DECIMAL | | Sequential read speed in MB/s |
| sequential_write_spec_mbps | DECIMAL | | Sequential write speed in MB/s |
| random_read_spec_iops | INT | | Random read IOPS |
| random_write_spec_iops | INT | | Random write IOPS |
| tbw_rating | INT | | TBW (Total Bytes Written) rating |
| mtbf_hours | INT | | MTBF (Mean Time Between Failures) in hours |
| storage_manufacturer | VARCHAR | | Storage manufacturer |
| created_at | DATETIME | NOT NULL | Timestamp when specification was created |
| updated_at | DATETIME | NOT NULL | Timestamp when specification was last updated |

## User Devices Table

The `USER_DEVICES` table links users to their devices.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| user_device_id | BIGINT | PRIMARY KEY | Unique identifier for the user device |
| user_id | BIGINT | FOREIGN KEY | Reference to the user |
| device_id | BIGINT | FOREIGN KEY | Reference to the device |
| custom_device_name | VARCHAR | | Custom name for the device |
| is_primary | BOOLEAN | DEFAULT FALSE | Whether this is the primary device |
| added_at | DATETIME | NOT NULL | Timestamp when device was added |

## Benchmark Results Table

The `BENCHMARK_RESULTS` table stores benchmark test results.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-------------|
| result_id | BIGINT | PRIMARY KEY | Unique identifier for the benchmark result |
| user_id | BIGINT | FOREIGN KEY | Reference to the user who ran the test |
| device_id | BIGINT | FOREIGN KEY | Reference to the device tested |
| benchmark_mode | ENUM('full', 'throttle', 'efficiency') | NOT NULL | Mode of the benchmark |
| overall_score | DECIMAL | | Overall benchmark score |
| performance_grade | ENUM('A+', 'A', 'B+', 'B', 'C', 'D', 'F') | | Performance grade |
| global_rank | INT | | Global rank |
| category_rank | INT | | Category rank |
| test_started_at | DATETIME | | Timestamp when test started |
| test_completed_at | DATETIME | | Timestamp when test completed |
| test_duration_seconds | INT | | Duration of test in seconds |
| app_version | VARCHAR | | Version of the benchmark app |
| is_verified | BOOLEAN | DEFAULT FALSE | Whether result is verified |
| is_public | BOOLEAN | DEFAULT FALSE | Whether result is public |
| is_flagged | BOOLEAN | DEFAULT FALSE | Whether result is flagged |
| share_token | VARCHAR | UNIQUE | Token for sharing results |
| view_count | INT | DEFAULT 0 | Number of views |
| created_at | DATETIME | NOT NULL | Timestamp when result was created |
| updated_at | DATETIME | NOT NULL | Timestamp when result was last updated |

## Test Environment Table

The `TEST_ENVIRONMENT` table stores environmental conditions during benchmark tests.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-------------|
| env_id | BIGINT | PRIMARY KEY | Unique identifier for the test environment |
| result_id | BIGINT | FOREIGN KEY | Reference to the benchmark result |
| ambient_temperature_celsius | DECIMAL | | Ambient temperature in Celsius |
| battery_level_start | INT | | Battery level at start (0-100%) |
| battery_level_end | INT | | Battery level at end (0-100%) |
| is_charging | BOOLEAN | DEFAULT FALSE | Whether device was charging |
| screen_brightness_percent | DECIMAL | | Screen brightness percentage |
| wifi_enabled | BOOLEAN | DEFAULT FALSE | Whether WiFi was enabled |
| bluetooth_enabled | BOOLEAN | DEFAULT FALSE | Whether Bluetooth was enabled |
| mobile_data_enabled | BOOLEAN | DEFAULT FALSE | Whether mobile data was enabled |
| gps_enabled | BOOLEAN | DEFAULT FALSE | Whether GPS was enabled |
| running_apps_count | INT | | Number of running applications |
| available_ram_mb | DECIMAL | | Available RAM in MB |
| available_storage_gb | DECIMAL | | Available storage in GB |
| kernel_version | VARCHAR | | Kernel version |
| build_fingerprint | VARCHAR | Build fingerprint |
| security_patch_level | VARCHAR | Security patch level |
| is_rooted | BOOLEAN | DEFAULT FALSE | Whether device is rooted |
| is_throttling_enabled | BOOLEAN | DEFAULT FALSE | Whether throttling was enabled |
| cpu_temp_start_celsius | DECIMAL | | CPU temperature at start in Celsius |
| gpu_temp_start_celsius | DECIMAL | | GPU temperature at start in Celsius |
| battery_temp_start_celsius | DECIMAL | | Battery temperature at start in Celsius |
| recorded_at | DATETIME | NOT NULL | Timestamp when environment was recorded |

## Full Benchmark Details Table

The `FULL_BENCHMARK_DETAILS` table contains detailed scores for each category in full benchmark mode.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| full_detail_id | BIGINT | PRIMARY KEY | Unique identifier for the full benchmark details |
| result_id | BIGINT | FOREIGN KEY | Reference to the benchmark result |
| cpu_score | DECIMAL | | CPU score |
| ai_ml_score | DECIMAL | | AI/ML score |
| gpu_score | DECIMAL | | GPU score |
| ram_score | DECIMAL | | RAM score |
| storage_score | DECIMAL | | Storage score |
| productivity_score | DECIMAL | | Productivity score |
| cpu_test_results | JSONB | | CPU test results |
| ai_ml_test_results | JSONB | | AI/ML test results |
| gpu_test_results | JSONB | | GPU test results |
| ram_test_results | JSONB | | RAM test results |
| storage_test_results | JSONB | | Storage test results |
| productivity_test_results | JSONB | | Productivity test results |

## Full Benchmark Telemetry Table

The `FULL_BENCHMARK_TELEMETRY` table stores detailed telemetry data for full benchmark tests.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| telemetry_id | BIGINT | PRIMARY KEY | Unique identifier for the telemetry record |
| full_detail_id | BIGINT | FOREIGN KEY | Reference to the full benchmark details |
| cpu_temperature_timeline | JSONB | | CPU temperature timeline |
| gpu_temperature_timeline | JSONB | GPU temperature timeline |
| battery_temperature_timeline | JSONB | | Battery temperature timeline |
| cpu_frequency_timeline | JSONB | | CPU frequency timeline |
| gpu_frequency_timeline | JSONB | | GPU frequency timeline |
| battery_level_timeline | JSONB | Battery level timeline |
| memory_usage_timeline | JSONB | Memory usage timeline |
| power_consumption_timeline | JSONB | | Power consumption timeline |
| thermal_throttle_events | JSONB | | Thermal throttle events |
| performance_state_timeline | JSONB | | Performance state timeline |
| avg_cpu_temp_celsius | DECIMAL | | Average CPU temperature in Celsius |
| max_cpu_temp_celsius | DECIMAL | | Maximum CPU temperature in Celsius |
| avg_gpu_temp_celsius | DECIMAL | | Average GPU temperature in Celsius |
| max_gpu_temp_celsius | DECIMAL | | Maximum GPU temperature in Celsius |
| avg_battery_temp_celsius | DECIMAL | | Average battery temperature in Celsius |
| max_battery_temp_celsius | DECIMAL | | Maximum battery temperature in Celsius |
| avg_cpu_frequency_ghz | DECIMAL | | Average CPU frequency in GHz |
| avg_gpu_frequency_mhz | DECIMAL | | Average GPU frequency in MHz |
| total_throttle_events | INT | | Total number of throttle events |
| total_battery_drain_percent | DECIMAL | | Total battery drain percentage |
| avg_power_consumption_watts | DECIMAL | | Average power consumption in watts |
| peak_power_consumption_watts | DECIMAL | | Peak power consumption in watts |

## Throttle Test Details Table

The `THROTTLE_TEST_DETAILS` table contains detailed results for throttle tests.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| throttle_detail_id | BIGINT | PRIMARY KEY | Unique identifier for the throttle test details |
| result_id | BIGINT | FOREIGN KEY | Reference to the benchmark result |
| test_duration_minutes | INT | | Test duration in minutes |
| initial_performance_score | DECIMAL | | Initial performance score |
| sustained_performance_score | DECIMAL | | Sustained performance score |
| performance_retention_percent | DECIMAL | | Performance retention percentage |
| time_to_throttle_seconds | INT | | Time to throttle in seconds |
| max_cpu_temperature_celsius | DECIMAL | | Maximum CPU temperature in Celsius |
| max_gpu_temperature_celsius | DECIMAL | | Maximum GPU temperature in Celsius |
| max_battery_temperature_celsius | DECIMAL | | Maximum battery temperature in Celsius |
| avg_cpu_temperature_celsius | DECIMAL | | Average CPU temperature in Celsius |
| avg_gpu_temperature_celsius | DECIMAL | | Average GPU temperature in Celsius |
| thermal_efficiency_score | DECIMAL | | Thermal efficiency score |
| throttling_percentage | DECIMAL | | Throttling percentage |
| temperature_curve_data | JSONB | | Temperature curve data |
| performance_curve_data | JSONB | | Performance curve data |
| cpu_frequency_data | JSONB | | CPU frequency data |
| gpu_frequency_data | JSONB | GPU frequency data |

## Efficiency Test Details Table

The `EFFICIENCY_TEST_DETAILS` table contains detailed results for efficiency tests.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| efficiency_detail_id | BIGINT | PRIMARY KEY | Unique identifier for the efficiency test details |
| result_id | BIGINT | FOREIGN KEY | Reference to the benchmark result |
| performance_output_score | DECIMAL | | Performance output score |
| power_consumption_watts | DECIMAL | | Power consumption in watts |
| battery_drain_mah | DECIMAL | | Battery drain in mAh |
| battery_drain_percent | DECIMAL | | Battery drain percentage |
| efficiency_score | DECIMAL | | Efficiency score |
| performance_per_watt | DECIMAL | | Performance per watt |
| avg_cpu_temperature_celsius | DECIMAL | | Average CPU temperature in Celsius |
| avg_gpu_temperature_celsius | DECIMAL | | Average GPU temperature in Celsius |
| avg_battery_temperature_celsius | DECIMAL | | Average battery temperature in Celsius |
| peak_cpu_frequency_ghz | DECIMAL | | Peak CPU frequency in GHz |
| peak_gpu_frequency_mhz | DECIMAL | | Peak GPU frequency in MHz |
| power_consumption_data | JSONB | | Power consumption data |

## CPU Test Results Table

The `CPU_TEST_RESULTS` table contains detailed results for CPU-specific tests.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| cpu_test_id | BIGINT | PRIMARY KEY | Unique identifier for the CPU test result |
| full_detail_id | BIGINT | FOREIGN KEY | Reference to the full benchmark details |
| prime_number_score | DECIMAL | | Prime number test score |
| prime_numbers_found | INT | | Number of prime numbers found |
| prime_test_duration_ms | DECIMAL | Prime test duration in milliseconds |
| fibonacci_score | DECIMAL | | Fibonacci test score |
| fibonacci_computation_time_ms | DECIMAL | | Fibonacci computation time in milliseconds |
| matrix_multiplication_score | DECIMAL | | Matrix multiplication score |
| matrix_ops_per_second | DECIMAL | | Matrix operations per second |
| hash_computing_score | DECIMAL | | Hash computing score |
| hash_computations_per_second | DECIMAL | | Hash computations per second |
| string_sorting_score | DECIMAL | | String sorting score |
| strings_sorted_per_second | INT | | Strings sorted per second |
| ray_tracing_score | DECIMAL | | Ray tracing score |
| rays_traced_per_second | DECIMAL | Rays traced per second |
| compression_score | DECIMAL | | Compression score |
| compression_speed_mbps | DECIMAL | | Compression speed in Mbps |
| decompression_speed_mbps | DECIMAL | | Decompression speed in Mbps |
| monte_carlo_score | DECIMAL | | Monte Carlo test score |
| monte_carlo_iterations | INT | | Monte Carlo iterations |
| json_parsing_score | DECIMAL | | JSON parsing score |
| json_objects_parsed_per_second | INT | | JSON objects parsed per second |
| n_queens_score | DECIMAL | | N-Queens test score |
| n_queens_solutions_found | INT | | N-Queens solutions found |
| n_queens_time_ms | DECIMAL | | N-Queens time in milliseconds |

## AI/ML Test Results Table

The `AI_ML_TEST_RESULTS` table contains detailed results for AI/ML-specific tests.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| ai_ml_test_id | BIGINT | PRIMARY KEY | Unique identifier for the AI/ML test result |
| full_detail_id | BIGINT | FOREIGN KEY | Reference to the full benchmark details |
| llm_inference_score | DECIMAL | | LLM inference score |
| tokens_per_second_prompt | DECIMAL | | Tokens per second for prompt processing |
| tokens_per_second_generation | DECIMAL | | Tokens per second for generation |
| time_to_first_token_ms | DECIMAL | | Time to first token in milliseconds |
| total_tokens_generated | INT | | Total tokens generated |
| image_classification_score | DECIMAL | | Image classification score |
| images_per_second | DECIMAL | | Images processed per second |
| avg_inference_time_ms | DECIMAL | | Average inference time in milliseconds |
| object_detection_score | DECIMAL | | Object detection score |
| detections_per_second | DECIMAL | Detections per second |
| avg_detection_time_ms | DECIMAL | | Average detection time in milliseconds |
| text_embedding_score | DECIMAL | | Text embedding score |
| embeddings_per_second | INT | | Embeddings generated per second |
| avg_embedding_time_ms | DECIMAL | Average embedding time in milliseconds |
| speech_to_text_score | DECIMAL | | Speech to text score |
| real_time_factor | DECIMAL | | Real-time factor |
| transcription_accuracy_percent | DECIMAL | Transcription accuracy percentage |

## GPU Test Results Table

The `GPU_TEST_RESULTS` table contains detailed results for GPU-specific tests.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| gpu_test_id | BIGINT | PRIMARY KEY | Unique identifier for the GPU test result |
| full_detail_id | BIGINT | FOREIGN KEY | Reference to the full benchmark details |
| triangle_rendering_score | DECIMAL | | Triangle rendering score |
| triangle_rendering_fps | DECIMAL | | Triangle rendering FPS |
| triangles_rendered | INT | | Number of triangles rendered |
| compute_shader_score | DECIMAL | | Compute shader score |
| matrix_multiplication_gflops | DECIMAL | Matrix multiplication performance in GFLOPS |
| compute_time_ms | DECIMAL | | Compute time in milliseconds |
| particle_system_score | DECIMAL | | Particle system score |
| max_particles_60fps | INT | | Maximum particles at 60 FPS |
| particle_update_time_ms | DECIMAL | | Particle update time in milliseconds |
| texture_sampling_score | DECIMAL | | Texture sampling score |
| pixel_fillrate_gpixels_per_sec | DECIMAL | | Pixel fill rate in GPixels per second |
| texture_bandwidth_gbps | DECIMAL | | Texture bandwidth in GB/s |
| tessellation_score | DECIMAL | | Tessellation score |
| tessellation_throughput | DECIMAL | | Tessellation throughput |
| geometry_processing_speed | DECIMAL | | Geometry processing speed |
| unity_scene1_score | DECIMAL | | Unity Scene 1 score |
| unity_scene1_avg_fps | DECIMAL | | Unity Scene 1 average FPS |
| unity_scene2_score | DECIMAL | | Unity Scene 2 score |
| unity_scene2_avg_fps | DECIMAL | | Unity Scene 2 average FPS |
| unreal_scene1_score | DECIMAL | | Unreal Scene 1 score |
| unreal_scene1_avg_fps | DECIMAL | | Unreal Scene 1 average FPS |
| unreal_scene2_score | DECIMAL | | Unreal Scene 2 score |
| unreal_scene2_avg_fps | DECIMAL | | Unreal Scene 2 average FPS |
| unreal_scene3_score | DECIMAL | | Unreal Scene 3 score |
| unreal_scene3_avg_fps | DECIMAL | | Unreal Scene 3 average FPS |

## GPU Frame Metrics Table

The `GPU_FRAME_METRICS` table contains detailed frame-by-frame metrics for GPU tests.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| frame_metric_id | BIGINT | PRIMARY KEY | Unique identifier for the frame metric |
| gpu_test_id | BIGINT | FOREIGN KEY | Reference to the GPU test result |
| test_name | VARCHAR | | Name of the test |
| avg_fps | DECIMAL | | Average FPS |
| min_fps | DECIMAL | | Minimum FPS |
| max_fps | DECIMAL | Maximum FPS |
| fps_std_deviation | DECIMAL | FPS standard deviation |
| percentile_1_low_fps | DECIMAL | 1st percentile low FPS |
| percentile_0_1_low_fps | DECIMAL | | 0.1st percentile low FPS |
| percentile_99_fps | DECIMAL | | 99th percentile FPS |
| total_frames | INT | | Total number of frames |
| dropped_frames | INT | | Number of dropped frames |
| frame_time_avg_ms | DECIMAL | | Average frame time in milliseconds |
| frame_time_min_ms | DECIMAL | | Minimum frame time in milliseconds |
| frame_time_max_ms | DECIMAL | | Maximum frame time in milliseconds |
| frame_time_99th_percentile_ms | DECIMAL | | 99th percentile frame time in milliseconds |
| frame_spikes_count | INT | | Number of frame spikes |
| frame_time_distribution | JSONB | Frame time distribution |
| fps_timeline_data | JSONB | FPS timeline data |
| gpu_utilization_timeline | JSONB | | GPU utilization timeline |
| gpu_temperature_timeline | JSONB | GPU temperature timeline |
| gpu_frequency_timeline | JSONB | | GPU frequency timeline |
| avg_gpu_utilization_percent | DECIMAL | | Average GPU utilization percentage |
| max_gpu_utilization_percent | DECIMAL | | Maximum GPU utilization percentage |

## RAM Test Results Table

The `RAM_TEST_RESULTS` table contains detailed results for RAM-specific tests.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| ram_test_id | BIGINT | PRIMARY KEY | Unique identifier for the RAM test result |
| full_detail_id | BIGINT | FOREIGN KEY | Reference to the full benchmark details |
| sequential_read_score | DECIMAL | | Sequential read score |
| sequential_read_speed_mbps | DECIMAL | | Sequential read speed in Mbps |
| sequential_write_score | DECIMAL | | Sequential write score |
| sequential_write_speed_mbps | DECIMAL | | Sequential write speed in Mbps |
| random_access_score | DECIMAL | | Random access score |
| random_read_latency_ns | DECIMAL | | Random read latency in nanoseconds |
| random_write_latency_ns | DECIMAL | | Random write latency in nanoseconds |
| random_ops_per_second | INT | | Random operations per second |
| memory_copy_score | DECIMAL | | Memory copy score |
| memory_copy_bandwidth_mbps | DECIMAL | Memory copy bandwidth in Mbps |
| multithread_score | DECIMAL | | Multithread score |
| multithread_bandwidth_mbps | DECIMAL | | Multithread bandwidth in Mbps |
| optimal_thread_count | INT | | Optimal thread count |
| cache_hierarchy_score | DECIMAL | | Cache hierarchy score |
| l1_cache_size_kb | INT | | L1 cache size in KB |
| l2_cache_size_kb | INT | | L2 cache size in KB |
| l3_cache_size_kb | INT | | L3 cache size in KB |
| l1_cache_speed_gbps | DECIMAL | | L1 cache speed in GB/s |
| l2_cache_speed_gbps | DECIMAL | | L2 cache speed in GB/s |
| l3_cache_speed_gbps | DECIMAL | L3 cache speed in GB/s |
| ram_latency_ns | DECIMAL | RAM latency in nanoseconds |

## Storage Test Results Table

The `STORAGE_TEST_RESULTS` table contains detailed results for storage-specific tests.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-------------|
| storage_test_id | BIGINT | PRIMARY KEY | Unique identifier for the storage test result |
| full_detail_id | BIGINT | FOREIGN KEY | Reference to the full benchmark details |
| sequential_read_score | DECIMAL | | Sequential read score |
| sequential_read_speed_mbps | DECIMAL | | Sequential read speed in Mbps |
| sequential_write_score | DECIMAL | Sequential write score |
| sequential_write_speed_mbps | DECIMAL | | Sequential write speed in Mbps |
| random_4k_read_score | DECIMAL | Random 4K read score |
| random_4k_read_iops | INT | | Random 4K read IOPS |
| random_4k_read_latency_ms | DECIMAL | | Random 4K read latency in milliseconds |
| random_4k_write_score | DECIMAL | Random 4K write score |
| random_4k_write_iops | INT | | Random 4K write IOPS |
| random_4k_write_latency_ms | DECIMAL | | Random 4K write latency in milliseconds |
| small_file_ops_score | DECIMAL | Small file operations score |
| files_created_per_second | INT | | Files created per second |
| files_read_per_second | INT | | Files read per second |
| files_deleted_per_second | INT | | Files deleted per second |
| database_performance_score | DECIMAL | | Database performance score |
| sqlite_inserts_per_second | INT | | SQLite inserts per second |
| sqlite_queries_per_second | INT | | SQLite queries per second |
| sqlite_updates_per_second | INT | | SQLite updates per second |
| sqlite_deletes_per_second | INT | | SQLite deletes per second |
| mixed_workload_score | DECIMAL | | Mixed workload score |
| mixed_workload_throughput_mbps | DECIMAL | | Mixed workload throughput in Mbps |

## Productivity Test Results Table

The `PRODUCTIVITY_TEST_RESULTS` table contains detailed results for productivity-specific tests.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-------------|
| productivity_test_id | BIGINT | PRIMARY KEY | Unique identifier for the productivity test result |
| full_detail_id | BIGINT | FOREIGN KEY | Reference to the full benchmark details |
| ui_rendering_score | DECIMAL | | UI rendering score |
| ui_rendering_avg_fps | DECIMAL | | UI rendering average FPS |
| ui_frame_drops | INT | | UI frame drops |
| jank_percentage | DECIMAL | Jank percentage |
| recyclerview_score | DECIMAL | | RecyclerView score |
| recyclerview_scroll_fps | DECIMAL | RecyclerView scroll FPS |
| recyclerview_memory_mb | DECIMAL | RecyclerView memory usage in MB |
| canvas_drawing_score | DECIMAL | | Canvas drawing score |
| draw_operations_per_second | INT | | Draw operations per second |
| image_filter_score | DECIMAL | | Image filter score |
| images_filtered_per_second | DECIMAL | | Images filtered per second |
| filter_processing_time_ms | DECIMAL | | Filter processing time in milliseconds |
| image_resize_score | DECIMAL | | Image resize score |
| images_resized_per_second | INT | | Images resized per second |
| video_encoding_score | DECIMAL | Video encoding score |
| video_encoding_fps | DECIMAL | Video encoding FPS |
| encoding_time_seconds | DECIMAL | Encoding time in seconds |
| video_transcoding_score | DECIMAL | | Video transcoding score |
| transcoding_realtime_factor | DECIMAL | | Transcoding real-time factor |
| pdf_rendering_score | DECIMAL | | PDF rendering score |
| pdf_pages_per_second | INT | | PDF pages rendered per second |
| text_rendering_score | DECIMAL | | Text rendering score |
| characters_per_second | INT | | Characters rendered per second |
| layout_calculation_time_ms | DECIMAL | | Layout calculation time in milliseconds |
| multitasking_score | DECIMAL | | Multitasking score |
| performance_degradation_percent | DECIMAL | | Performance degradation percentage |

## Leaderboard Cache Table

The `LEADERBOARD_CACHE` table stores cached leaderboard data for quick access.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| cache_id | BIGINT | PRIMARY KEY | Unique identifier for the cache entry |
| benchmark_mode | ENUM('full', 'throttle', 'efficiency') | NOT NULL | Benchmark mode |
| category | VARCHAR | | Category |
| phone_brand | VARCHAR | | Phone brand |
| cpu_brand | VARCHAR | | CPU brand |
| top_10_results | JSONB | | Top 100 results data |
| last_updated | DATETIME | NOT NULL | Timestamp when cache was last updated |
| expires_at | DATETIME | NOT NULL | Timestamp when cache expires |

## Comparison AI Cache Table

The `COMPARISON_AI_CACHE` table stores cached AI-powered device comparisons.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-------------|
| cache_key | VARCHAR | PRIMARY KEY | Unique key for the cache entry |
| device1_id | VARCHAR | | ID of the first device |
| device2_id | VARCHAR | | ID of the second device |
| comparison_type | ENUM('device', 'soc') | NOT NULL | Type of comparison |
| ai_summary | JSONB | | AI-generated comparison summary |
| comparison_data | JSONB | | Raw comparison data |
| access_count | INT | DEFAULT 0 | Number of times accessed |
| created_at | DATETIME | NOT NULL | Timestamp when cache was created |
| last_accessed | DATETIME | | Timestamp of last access |
| expires_at | DATETIME | NOT NULL | Timestamp when cache expires |

## Device Statistics Table

The `DEVICE_STATISTICS` table stores aggregated statistics for each device.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-------------|
| stat_id | BIGINT | PRIMARY KEY | Unique identifier for the statistic |
| device_id | BIGINT | FOREIGN KEY | Reference to the device |
| benchmark_mode | ENUM('full', 'throttle', 'efficiency') | NOT NULL | Benchmark mode |
| total_submissions | INT | | Total number of benchmark submissions |
| avg_overall_score | DECIMAL | | Average overall score |
| min_overall_score | DECIMAL | | Minimum overall score |
| max_overall_score | DECIMAL | | Maximum overall score |
| std_deviation | DECIMAL | | Standard deviation |
| median_score | DECIMAL | Median score |
| rank_position | INT | | Rank position |
| last_calculated | DATETIME | NOT NULL | Timestamp when statistics were last calculated |

## System Logs Table

The `SYSTEM_LOGS` table stores system logs related to benchmark operations.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-------------|
| log_id | BIGINT | PRIMARY KEY | Unique identifier for the log entry |
| user_id | BIGINT | FOREIGN KEY | Reference to the user (if applicable) |
| result_id | BIGINT | FOREIGN KEY | Reference to the benchmark result (if applicable) |
| log_level | ENUM('info', 'warning', 'error', 'critical') | NOT NULL | Log level |
| log_type | VARCHAR | NOT NULL | Type of log |
| message | TEXT | NOT NULL | Log message |
| metadata | JSONB | | Additional metadata |
| ip_address | VARCHAR | | IP address of the client |
| user_agent | VARCHAR | | User agent string |
| created_at | DATETIME | NOT NULL | Timestamp when log was created |

## Device Summary View

The `DEVICE_SUMMARY_VIEW` provides a summary view of device information and benchmarks.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-------------|
| device_id | BIGINT | PRIMARY KEY | Unique identifier for the device |
| device_name | VARCHAR | | Name of the device |
| manufacturer | VARCHAR | | Device manufacturer |
| model_number | VARCHAR | | Model number of the device |
| device_type | ENUM('phone', 'tablet', 'foldable', 'laptop', 'desktop', 'mini_pc') | | Type of device |
| platform | ENUM('android', 'windows', 'linux', 'macos', 'ios') | | Platform of the device |
| release_date | DATE | | Release date of the device |
| soc_apu_spec_id | BIGINT | FOREIGN KEY | Reference to SOC/APU specification |
| soc_name | VARCHAR | | Name of the SOC/APU |
| cpu_model | VARCHAR | | CPU model |
| gpu_model | VARCHAR | | GPU model |
| ram_size_gb | INT | | RAM size in GB |
| storage_size_gb | INT | | Storage size in GB |
| total_benchmarks_submitted | INT | | Total number of benchmarks submitted |
| avg_full_benchmark_score | DECIMAL | | Average full benchmark score |
| avg_throttle_score | DECIMAL | | Average throttle score |
| avg_efficiency_score | DECIMAL | | Average efficiency score |
| full_benchmark_rank | INT | | Full benchmark rank |
| throttle_rank | INT | | Throttle rank |
| efficiency_rank | INT | | Efficiency rank |
| score_percentile | DECIMAL | | Score percentile |
| category_scores | JSONB | | Category-specific scores |
| top_strengths | JSONB | Top strengths of the device |
| top_weaknesses | JSONB | | Top weaknesses of the device |
| last_benchmark_date | DATETIME | | Date of last benchmark |
| last_updated | DATETIME | NOT NULL | Timestamp when view was last updated |

## SOC Summary View

The `SOC_SUMMARY_VIEW` provides a summary view of SOC/APU information and benchmarks.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| soc_apu_spec_id | BIGINT | PRIMARY KEY | Unique identifier for the SOC/APU specification |
| chip_name | VARCHAR | | Name of the chip |
| chip_type | ENUM('soc', 'apu') | | Type of chip |
| manufacturer | VARCHAR | | Chip manufacturer |
| cpu_model | VARCHAR | | CPU model |
| gpu_model | VARCHAR | | GPU model |
| process_node | VARCHAR | | Process node |
| total_tdp_watts | INT | | Total TDP in watts |
| npu_tops | DECIMAL | | NPU performance in TOPS |
| total_devices_using | INT | | Total number of devices using this SOC/APU |
| total_benchmarks_submitted | INT | | Total number of benchmarks submitted |
| avg_full_benchmark_score | DECIMAL | | Average full benchmark score |
| avg_throttle_score | DECIMAL | | Average throttle score |
| avg_efficiency_score | DECIMAL | | Average efficiency score |
| full_benchmark_rank | INT | | Full benchmark rank |
| throttle_rank | INT | | Throttle rank |
| efficiency_rank | INT | | Efficiency rank |
| score_percentile | DECIMAL | | Score percentile |
| category_scores | JSONB | | Category-specific scores |
| gpu_test_summary | JSONB | | GPU test summary |
| thermal_performance_summary | JSONB | | Thermal performance summary |
| power_efficiency_summary | JSONB | | Power efficiency summary |
| competing_socs | JSONB | | Competing SOC information |
| last_benchmark_date | DATETIME | | Date of last benchmark |
| last_updated | DATETIME | NOT NULL | Timestamp when view was last updated |

## Leaderboard Tables

### Leaderboard Full Benchmark
The `LEADERBOARD_FULL_BENCHMARK` table contains full benchmark leaderboard data.

| Column Name | Data Type | Constraints | Description |
|-------------|-------------|
| rank | BIGINT | PRIMARY KEY | Rank position |
| device_id | BIGINT | FOREIGN KEY | Reference to the device |
| soc_apu_spec_id | BIGINT | FOREIGN KEY | Reference to SOC/APU specification |
| device_name | VARCHAR | | Name of the device |
| soc_name | VARCHAR | | Name of the SOC/APU |
| manufacturer | VARCHAR | | Device manufacturer |
| overall_score | DECIMAL | | Overall benchmark score |
| performance_grade | ENUM('A+', 'A', 'B+', 'B', 'C', 'D', 'F') | | Performance grade |
| cpu_score | DECIMAL | | CPU score |
| ai_ml_score | DECIMAL | | AI/ML score |
| gpu_score | DECIMAL | | GPU score |
| ram_score | DECIMAL | | RAM score |
| storage_score | DECIMAL | | Storage score |
| productivity_score | DECIMAL | | Productivity score |
| total_submissions | INT | | Total number of submissions |
| avg_score | DECIMAL | | Average score |
| score_variance | DECIMAL | | Score variance |
| phone_brand | VARCHAR | | Phone brand |
| cpu_brand | VARCHAR | | CPU brand |
| last_updated | DATETIME | NOT NULL | Timestamp when leaderboard was last updated |

### Leaderboard Throttle
The `LEADERBOARD_THROTTLE` table contains throttle test leaderboard data.

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| rank | BIGINT | PRIMARY KEY | Rank position |
| device_id | BIGINT | FOREIGN KEY | Reference to the device |
| soc_apu_spec_id | BIGINT | FOREIGN KEY | Reference to SOC/APU specification |
| device_name | VARCHAR | | Name of the device |
| soc_name | VARCHAR | | Name of the SOC/APU |
| manufacturer | VARCHAR | | Device manufacturer |
| throttle_score | DECIMAL | Throttle test score |
| performance_grade | ENUM('A+', 'A', 'B+', 'B', 'C', 'D', 'F') | | Performance grade |
| performance_retention_percent | DECIMAL | | Performance retention percentage |
| avg_temperature_celsius | DECIMAL | Average temperature in Celsius |
| thermal_efficiency | DECIMAL | | Thermal efficiency |
| total_submissions | INT | | Total number of submissions |
| avg_score | DECIMAL | | Average score |
| phone_brand | VARCHAR | | Phone brand |
| cpu_brand | VARCHAR | | CPU brand |
| last_updated | DATETIME | NOT NULL | Timestamp when leaderboard was last updated |

### Leaderboard Efficiency
The `LEADERBOARD_EFFICIENCY` table contains efficiency test leaderboard data.

| Column Name | Data Type | Constraints | Description |
|-------------|-------------|
| rank | BIGINT | PRIMARY KEY | Rank position |
| device_id | BIGINT | FOREIGN KEY | Reference to the device |
| soc_apu_spec_id | BIGINT | FOREIGN KEY | Reference to SOC/APU specification |
| device_name | VARCHAR | | Name of the device |
| soc_name | VARCHAR | | Name of the SOC/APU |
| manufacturer | VARCHAR | | Device manufacturer |
| efficiency_score | DECIMAL | Efficiency score |
| performance_grade | ENUM('A+', 'A', 'B+', 'B', 'C', 'D', 'F') | | Performance grade |
| performance_per_watt | DECIMAL | | Performance per watt |
| avg_power_consumption_watts | DECIMAL | Average power consumption in watts |
| performance_output | DECIMAL | Performance output |
| total_submissions | INT | | Total number of submissions |
| avg_score | DECIMAL | | Average score |
| phone_brand | VARCHAR | | Phone brand |
| cpu_brand | VARCHAR | | CPU brand |
| last_updated | DATETIME | NOT NULL | Timestamp when leaderboard was last updated |

### Leaderboard Category
The `LEADERBOARD_CATEGORY` table contains category-specific leaderboard data.

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| entry_id | BIGINT | PRIMARY KEY | Unique identifier for the entry |
| category | ENUM('cpu', 'ai_ml', 'gpu', 'ram', 'storage', 'productivity') | NOT NULL | Category |
| rank | BIGINT | | Rank position |
| device_id | BIGINT | FOREIGN KEY | Reference to the device |
| soc_apu_spec_id | BIGINT | FOREIGN KEY | Reference to SOC/APU specification |
| device_name | VARCHAR | | Name of the device |
| soc_name | VARCHAR | | Name of the SOC/APU |
| manufacturer | VARCHAR | | Device manufacturer |
| category_score | DECIMAL | Category-specific score |
| total_submissions | INT | | Total number of submissions |
| avg_score | DECIMAL | | Average score |
| phone_brand | VARCHAR | | Phone brand |
| cpu_brand | VARCHAR | | CPU brand |
| last_updated | DATETIME | NOT NULL | Timestamp when leaderboard was last updated |

## Comparison Cache Table

The `COMPARISON_CACHE` table stores cached device comparison data.

### Schema
| Column Name | Data Type | Constraints | Description |
|-------------|-------------|
| comparison_id | VARCHAR | PRIMARY KEY | Unique identifier for the comparison |
| comparison_type | ENUM('device_vs_device', 'soc_vs_soc', 'device_vs_soc') | NOT NULL | Type of comparison |
| entity1_id | VARCHAR | | ID of the first entity |
| entity2_id | VARCHAR | | ID of the second entity |
| entity1_name | VARCHAR | | Name of the first entity |
| entity2_name | VARCHAR | | Name of the second entity |
| full_comparison_data | JSONB | Full comparison data |
| score_differences | JSONB | | Score differences |
| category_comparisons | JSONB | | Category-specific comparisons |
| visual_chart_data | JSONB | | Data for visual charts |
| winner_summary | JSONB | Summary of the winner |
| recommendations | JSONB | | Recommendations |
| view_count | INT | DEFAULT 0 | Number of times viewed |
| created_at | DATETIME | NOT NULL | Timestamp when comparison was created |
| last_accessed | DATETIME | | Timestamp of last access |
| expires_at | DATETIME | NOT NULL | Timestamp when comparison expires |

## Relationships

The following relationships exist between the tables:

- `USERS` to `BENCHMARK_RESULTS`: One-to-Many (one user can submit many benchmark results)
- `USERS` to `USER_DEVICES`: One-to-Many (one user can own many devices)
- `DEVICES` to `BENCHMARK_RESULTS`: One-to-Many (one device can have many benchmark results)
- `DEVICES` to `USER_DEVICES`: One-to-Many (one device can be owned by many users)
- `DEVICES` to `DEVICE_SPECIFICATIONS`: One-to-One (one device has one specification)
- `DEVICE_SPECIFICATIONS` to `SOC_APU_SPECS`: Many-to-One (many device specs reference one SOC/APU spec)
- `DEVICE_SPECIFICATIONS` to `CPU_SPECS`: Many-to-One (many device specs reference one CPU spec)
- `DEVICE_SPECIFICATIONS` to `GPU_SPECS`: Many-to-One (many device specs reference one GPU spec)
- `DEVICE_SPECIFICATIONS` to `RAM_SPECS`: Many-to-One (many device specs reference one RAM spec)
- `DEVICE_SPECIFICATIONS` to `STORAGE_SPECS`: Many-to-One (many device specs reference one storage spec)
- `BENCHMARK_RESULTS` to `TEST_ENVIRONMENT`: One-to-One (one benchmark result has one test environment)
- `BENCHMARK_RESULTS` to `FULL_BENCHMARK_DETAILS`: One-to-One (one benchmark result has one full details record)
- `BENCHMARK_RESULTS` to `THROTTLE_TEST_DETAILS`: One-to-One (one benchmark result has one throttle details record)
- `BENCHMARK_RESULTS` to `EFFICIENCY_TEST_DETAILS`: One-to-One (one benchmark result has one efficiency details record)
- `FULL_BENCHMARK_DETAILS` to `FULL_BENCHMARK_TELEMETRY`: One-to-One (one full details record has one telemetry record)
- `FULL_BENCHMARK_DETAILS` to `CPU_TEST_RESULTS`: One-to-Many (one full details record has many CPU test results)
- `FULL_BENCHMARK_DETAILS` to `AI_ML_TEST_RESULTS`: One-to-Many (one full details record has many AI/ML test results)
- `FULL_BENCHMARK_DETAILS` to `GPU_TEST_RESULTS`: One-to-Many (one full details record has many GPU test results)
- `FULL_BENCHMARK_DETAILS` to `RAM_TEST_RESULTS`: One-to-Many (one full details record has many RAM test results)
- `FULL_BENCHMARK_DETAILS` to `STORAGE_TEST_RESULTS`: One-to-Many (one full details record has many storage test results)
- `FULL_BENCHMARK_DETAILS` to `PRODUCTIVITY_TEST_RESULTS`: One-to-Many (one full details record has many productivity test results)
- `GPU_TEST_RESULTS` to `GPU_FRAME_METRICS`: One-to-Many (one GPU test result has many frame metrics)
- `SYSTEM_LOGS` to `USERS`: Many-to-One (many logs can reference one user)
- `SYSTEM_LOGS` to `BENCHMARK_RESULTS`: Many-to-One (many logs can reference one benchmark result)
- `DEVICE_STATISTICS` to `DEVICES`: Many-to-One (many statistics can reference one device)
- `DEVICE_SUMMARY_VIEW` to `DEVICES`: One-to-One (one device has one summary view)
- `DEVICE_SUMMARY_VIEW` to `SOC_APU_SPECS`: Many-to-One (many device summaries reference one SOC/APU spec)
- `SOC_SUMMARY_VIEW` to `SOC_APU_SPECS`: One-to-One (one SOC/APU spec has one summary view)
- Leaderboard tables to `DEVICES`: Many-to-One (many leaderboard entries reference one device)
- Leaderboard tables to `SOC_APU_SPECS`: Many-to-One (many leaderboard entries reference one SOC/APU spec)

## JSON Structure Documentation

The following section documents the expected structure of all JSON columns in the database schema.

### 1. avg_gpu_test_metrics (JSONB)

This column in the `SOC_APU_SPECS` table contains structured GPU benchmarking results. It includes 10 GPU subcategories plus an aggregated section (`overall_gpu_metrics`).

**Structure:**
```json
{
  "overall_gpu_metrics": {
    "avg_score": 1234.5,
    "sample_count": 100,
    "avg_fps": 55.2,
    "avg_power_watts": 15.5,
    "avg_temperature_celsius": 75.3,
    "avg_utilization_percent": 85.2,
    "timeline_graph_data": {
      "fps_over_time": [
        {"time": 0, "fps": 60.0},
        {"time": 1, "fps": 58.5}
      ],
      "temperature_over_time": [
        {"time": 0, "temperature": 65.0},
        {"time": 1, "temperature": 67.5}
      ],
      "power_over_time": [
        {"time": 0, "power": 10.2},
        {"time": 1, "power": 12.4}
      ]
    }
  },
  "triangle_rendering": {
    "avg_score": 1100.0,
    "avg_fps": 55.0,
    "sample_count": 10,
    "avg_power_watts": 15.0,
    "avg_temperature_celsius": 70.0,
    "avg_utilization_percent": 80.0,
    "timeline_graph_data": {
      "fps_over_time": [
        {"time": 0, "fps": 60.0},
        {"time": 1, "fps": 55.0}
      ],
      "temperature_over_time": [
        {"time": 0, "temperature": 65.0},
        {"time": 1, "temperature": 70.0}
      ],
      "power_over_time": [
        {"time": 0, "power": 10.0},
        {"time": 1, "power": 15.0}
      ]
    }
  },
 "compute_shader": {
    "avg_score": 1050.0,
    "avg_gflops": 2.5,
    "sample_count": 10,
    "avg_power_watts": 18.0,
    "avg_temperature_celsius": 75.0,
    "avg_utilization_percent": 90.0,
    "timeline_graph_data": {
      "gflops_over_time": [
        {"time": 0, "gflops": 2.0},
        {"time": 1, "gflops": 2.5}
      ],
      "temperature_over_time": [
        {"time": 0, "temperature": 70.0},
        {"time": 1, "temperature": 75.0}
      ],
      "power_over_time": [
        {"time": 0, "power": 15.0},
        {"time": 1, "power": 18.0}
      ]
    }
  },
  "particle_system": {
    "avg_score": 950.0,
    "max_particles_60fps": 50000,
    "sample_count": 10,
    "avg_power_watts": 16.0,
    "avg_temperature_celsius": 72.0,
    "avg_utilization_percent": 85.0,
    "timeline_graph_data": {
      "particle_count_over_time": [
        {"time": 0, "count": 100},
        {"time": 1, "count": 20000}
      ],
      "temperature_over_time": [
        {"time": 0, "temperature": 68.0},
        {"time": 1, "temperature": 72.0}
      ],
      "power_over_time": [
        {"time": 0, "power": 12.0},
        {"time": 1, "power": 16.0}
      ]
    }
 },
  "texture_sampling": {
    "avg_score": 1000.0,
    "avg_pixel_fillrate_gpixels_per_sec": 15.5,
    "sample_count": 10,
    "avg_power_watts": 14.0,
    "avg_temperature_celsius": 68.0,
    "avg_utilization_percent": 78.0
  },
  "tessellation": {
    "avg_score": 900.0,
    "avg_throughput": 1.2,
    "sample_count": 10,
    "avg_power_watts": 17.0,
    "avg_temperature_celsius": 74.0,
    "avg_utilization_percent": 88.0
  },
 "unity_scene1": {
    "avg_score": 850.0,
    "avg_fps": 45.0,
    "sample_count": 10,
    "avg_power_watts": 16.5,
    "avg_temperature_celsius": 73.0,
    "avg_utilization_percent": 82.0
  },
  "unity_scene2": {
    "avg_score": 800.0,
    "avg_fps": 40.0,
    "sample_count": 10,
    "avg_power_watts": 17.5,
    "avg_temperature_celsius": 76.0,
    "avg_utilization_percent": 87.0
  },
  "unreal_scene1": {
    "avg_score": 920.0,
    "avg_fps": 48.0,
    "sample_count": 10,
    "avg_power_watts": 15.8,
    "avg_temperature_celsius": 71.0,
    "avg_utilization_percent": 81.0
  },
  "unreal_scene2": {
    "avg_score": 880.0,
    "avg_fps": 44.0,
    "sample_count": 10,
    "avg_power_watts": 16.2,
    "avg_temperature_celsius": 72.5,
    "avg_utilization_percent": 83.0
  },
  "unreal_scene3": {
    "avg_score": 820.0,
    "avg_fps": 41.0,
    "sample_count": 10,
    "avg_power_watts": 17.2,
    "avg_temperature_celsius": 75.0,
    "avg_utilization_percent": 86.0
  }
}
```

### 2. avg_cpu_category_metrics (JSONB)

This column in the `SOC_APU_SPECS` table contains CPU test categories with their performance metrics.

**Structure:**
```json
{
  "overall_cpu_metrics": {
    "avg_score": 1500.0,
    "sample_count": 100,
    "avg_power_watts": 8.5,
    "avg_temperature_celsius": 65.3,
    "avg_utilization_percent": 95.0,
    "timeline_graph_data": {
      "cpu_frequency_over_time": [
        {"time": 0, "frequency_ghz": 2.5},
        {"time": 1, "frequency_ghz": 2.8}
      ],
      "temperature_over_time": [
        {"time": 0, "temperature": 60.0},
        {"time": 1, "temperature": 65.0}
      ],
      "power_over_time": [
        {"time": 0, "power": 5.0},
        {"time": 1, "power": 8.0}
      ]
    }
  },
  "prime_number": {
    "avg_score": 1200.0,
    "avg_ops_per_second": 1000000,
    "sample_count": 10,
    "avg_power_watts": 7.0,
    "avg_temperature_celsius": 62.0,
    "avg_utilization_percent": 90.0
  },
  "fibonacci": {
    "avg_score": 1100.0,
    "avg_ops_per_second": 800000,
    "sample_count": 10,
    "avg_power_watts": 7.5,
    "avg_temperature_celsius": 63.0,
    "avg_utilization_percent": 92.0
  },
  "matrix_multiplication": {
    "avg_score": 1300.0,
    "avg_gflops": 5.5,
    "sample_count": 10,
    "avg_power_watts": 8.0,
    "avg_temperature_celsius": 64.0,
    "avg_utilization_percent": 94.0
  },
  "hash_computing": {
    "avg_score": 1250.0,
    "avg_ops_per_second": 5000000,
    "sample_count": 10,
    "avg_power_watts": 8.5,
    "avg_temperature_celsius": 65.0,
    "avg_utilization_percent": 95.0
  },
  "string_sorting": {
    "avg_score": 1150.0,
    "avg_ops_per_second": 2000000,
    "sample_count": 10,
    "avg_power_watts": 7.2,
    "avg_temperature_celsius": 62.5,
    "avg_utilization_percent": 91.0
  },
  "ray_tracing": {
    "avg_score": 1050.0,
    "avg_ops_per_second": 150000,
    "sample_count": 10,
    "avg_power_watts": 7.8,
    "avg_temperature_celsius": 64.5,
    "avg_utilization_percent": 93.0
  },
  "compression": {
    "avg_score": 1180.0,
    "avg_speed_mbps": 200.0,
    "sample_count": 10,
    "avg_power_watts": 7.6,
    "avg_temperature_celsius": 63.5,
    "avg_utilization_percent": 92.5
  },
  "monte_carlo": {
    "avg_score": 1080.0,
    "avg_iterations_per_second": 100000,
    "sample_count": 10,
    "avg_power_watts": 7.9,
    "avg_temperature_celsius": 64.8,
    "avg_utilization_percent": 93.5
  },
  "json_parsing": {
    "avg_score": 120.0,
    "avg_ops_per_second": 3000000,
    "sample_count": 10,
    "avg_power_watts": 7.4,
    "avg_temperature_celsius": 63.0,
    "avg_utilization_percent": 91.5
  },
  "n_queens": {
    "avg_score": 100.0,
    "avg_solutions_per_second": 5000,
    "sample_count": 10,
    "avg_power_watts": 7.7,
    "avg_temperature_celsius": 64.2,
    "avg_utilization_percent": 92.8
  }
}
```

### 3. avg_ai_ml_category_metrics (JSONB)

This column in the `SOC_APU_SPECS` table contains AI/ML test categories with their performance metrics.

**Structure:**
```json
{
  "overall_ai_ml_metrics": {
    "avg_score": 2000.0,
    "sample_count": 100,
    "avg_power_watts": 12.0,
    "avg_temperature_celsius": 70.0,
    "avg_utilization_percent": 8.0,
    "timeline_graph_data": {
      "performance_metric_over_time": [
        {"time": 0, "metric": 1000.0},
        {"time": 1, "metric": 1200.0}
      ],
      "temperature_over_time": [
        {"time": 0, "temperature": 65.0},
        {"time": 1, "temperature": 70.0}
      ],
      "power_over_time": [
        {"time": 0, "power": 8.0},
        {"time": 1, "power": 12.0}
      ],
      "npu_utilization_over_time": [
        {"time": 0, "utilization": 70.0},
        {"time": 1, "utilization": 85.0}
      ]
    }
 },
  "llm_inference": {
    "avg_score": 1800.0,
    "tokens_per_second_prompt": 15.5,
    "tokens_per_second_generation": 25.0,
    "time_to_first_token_ms": 120.0,
    "sample_count": 10,
    "avg_power_watts": 13.0,
    "avg_temperature_celsius": 72.0,
    "avg_utilization_percent": 90.0
  },
  "image_classification": {
    "avg_score": 2100.0,
    "images_per_second": 120.0,
    "avg_inference_time_ms": 8.3,
    "sample_count": 10,
    "avg_power_watts": 11.5,
    "avg_temperature_celsius": 68.0,
    "avg_utilization_percent": 85.0
  },
  "object_detection": {
    "avg_score": 1950.0,
    "detections_per_second": 45.0,
    "avg_detection_time_ms": 22.2,
    "sample_count": 10,
    "avg_power_watts": 12.5,
    "avg_temperature_celsius": 71.0,
    "avg_utilization_percent": 88.0
  },
  "text_embedding": {
    "avg_score": 1750.0,
    "embeddings_per_second": 300.0,
    "avg_embedding_time_ms": 3.3,
    "sample_count": 10,
    "avg_power_watts": 11.0,
    "avg_temperature_celsius": 67.0,
    "avg_utilization_percent": 82.0
  },
  "speech_to_text": {
    "avg_score": 1650.0,
    "real_time_factor": 2.5,
    "transcription_accuracy_percent": 95.5,
    "sample_count": 10,
    "avg_power_watts": 12.0,
    "avg_temperature_celsius": 69.0,
    "avg_utilization_percent": 86.0
  }
}
```

### 4. avg_ram_category_metrics (JSONB)

This column in the `SOC_APU_SPECS` table contains RAM test categories with their performance metrics.

**Structure:**
```json
{
  "overall_ram_metrics": {
    "avg_score": 250.0,
    "sample_count": 100,
    "avg_power_watts": 3.5,
    "avg_temperature_celsius": 40.0,
    "avg_utilization_percent": 75.0,
    "timeline_graph_data": {
      "bandwidth_over_time": [
        {"time": 0, "bandwidth_gbps": 15.0},
        {"time": 1, "bandwidth_gbps": 18.0}
      ],
      "latency_over_time": [
        {"time": 0, "latency_ns": 80.0},
        {"time": 1, "latency_ns": 75.0}
      ],
      "temperature_over_time": [
        {"time": 0, "temperature": 35.0},
        {"time": 1, "temperature": 40.0}
      ],
      "power_over_time": [
        {"time": 0, "power": 2.0},
        {"time": 1, "power": 3.5}
      ]
    }
  },
  "sequential_read": {
    "avg_score": 2800.0,
    "avg_speed_mbps": 25000.0,
    "sample_count": 10,
    "avg_power_watts": 3.0,
    "avg_temperature_celsius": 38.0,
    "avg_utilization_percent": 70.0
  },
  "sequential_write": {
    "avg_score": 2600.0,
    "avg_speed_mbps": 22000.0,
    "sample_count": 10,
    "avg_power_watts": 3.2,
    "avg_temperature_celsius": 39.0,
    "avg_utilization_percent": 72.0
  },
 "random_access": {
    "avg_score": 2200.0,
    "avg_latency_ns": 85.0,
    "sample_count": 10,
    "avg_power_watts": 3.5,
    "avg_temperature_celsius": 40.0,
    "avg_utilization_percent": 75.0
  },
  "memory_copy": {
    "avg_score": 2400.0,
    "avg_bandwidth_mbps": 20000.0,
    "sample_count": 10,
    "avg_power_watts": 3.1,
    "avg_temperature_celsius": 38.5,
    "avg_utilization_percent": 71.0
  },
  "multithread": {
    "avg_score": 2700.0,
    "avg_bandwidth_mbps": 23000.0,
    "sample_count": 10,
    "avg_power_watts": 3.4,
    "avg_temperature_celsius": 39.5,
    "avg_utilization_percent": 74.0
  },
  "cache_hierarchy": {
    "avg_score": 2300.0,
    "l1_cache_size_kb": 32,
    "l2_cache_size_kb": 256,
    "l3_cache_size_kb": 8192,
    "l1_cache_speed_gbps": 200.0,
    "l2_cache_speed_gbps": 150.0,
    "l3_cache_speed_gbps": 50.0,
    "sample_count": 10,
    "avg_power_watts": 3.3,
    "avg_temperature_celsius": 39.0,
    "avg_utilization_percent": 73.0
  }
}
```

### 5. avg_storage_category_metrics (JSONB)

This column in the `SOC_APU_SPECS` table contains storage test categories with their performance metrics.

**Structure:**
```json
{
  "overall_storage_metrics": {
    "avg_score": 3000.0,
    "sample_count": 100,
    "avg_power_watts": 2.0,
    "avg_temperature_celsius": 35.0,
    "timeline_graph_data": {
      "bandwidth_over_time": [
        {"time": 0, "bandwidth_mbps": 500.0},
        {"time": 1, "bandwidth_mbps": 800.0}
      ],
      "latency_over_time": [
        {"time": 0, "latency_ms": 0.1},
        {"time": 1, "latency_ms": 0.08}
      ],
      "temperature_over_time": [
        {"time": 0, "temperature": 30.0},
        {"time": 1, "temperature": 35.0}
      ],
      "power_over_time": [
        {"time": 0, "power": 1.0},
        {"time": 1, "power": 2.0}
      ]
    }
  },
  "sequential_read": {
    "avg_score": 3200.0,
    "avg_speed_mbps": 3500.0,
    "sample_count": 10,
    "avg_power_watts": 1.8,
    "avg_temperature_celsius": 34.0
  },
  "sequential_write": {
    "avg_score": 3100.0,
    "avg_speed_mbps": 3200.0,
    "sample_count": 10,
    "avg_power_watts": 1.9,
    "avg_temperature_celsius": 34.5
  },
  "random_4k_read": {
    "avg_score": 2800.0,
    "avg_iops": 80000,
    "sample_count": 10,
    "avg_power_watts": 2.0,
    "avg_temperature_celsius": 35.0
  },
  "random_4k_write": {
    "avg_score": 2600.0,
    "avg_iops": 6000,
    "sample_count": 10,
    "avg_power_watts": 2.1,
    "avg_temperature_celsius": 35.5
  },
  "small_file_ops": {
    "avg_score": 2400.0,
    "files_created_per_second": 1500,
    "files_read_per_second": 1200,
    "files_deleted_per_second": 1800,
    "sample_count": 10,
    "avg_power_watts": 1.9,
    "avg_temperature_celsius": 34.8
  },
  "database_performance": {
    "avg_score": 2700.0,
    "sqlite_inserts_per_second": 25000,
    "sqlite_queries_per_second": 30000,
    "sqlite_updates_per_second": 20000,
    "sqlite_deletes_per_second": 1500,
    "sample_count": 10,
    "avg_power_watts": 2.0,
    "avg_temperature_celsius": 35.2
  },
  "mixed_workload": {
    "avg_score": 2900.0,
    "avg_throughput_mbps": 2800.0,
    "sample_count": 10,
    "avg_power_watts": 1.95,
    "avg_temperature_celsius": 34.9
  }
}
```

### 6. avg_productivity_category_metrics (JSONB)

This column in the `SOC_APU_SPECS` table contains productivity test categories with their performance metrics.

**Structure:**
```json
{
  "overall_productivity_metrics": {
    "avg_score": 1800.0,
    "sample_count": 100,
    "avg_power_watts": 6.5,
    "avg_temperature_celsius": 55.0,
    "timeline_graph_data": {
      "performance_over_time": [
        {"time": 0, "performance": 1000.0},
        {"time": 1, "performance": 1200.0}
      ],
      "fps_over_time": [
        {"time": 0, "fps": 60.0},
        {"time": 1, "fps": 55.0}
      ],
      "temperature_over_time": [
        {"time": 0, "temperature": 50.0},
        {"time": 1, "temperature": 55.0}
      ],
      "power_over_time": [
        {"time": 0, "power": 4.0},
        {"time": 1, "power": 6.5}
      ]
    }
  },
  "ui_rendering": {
    "avg_score": 1900.0,
    "avg_fps": 58.0,
    "sample_count": 10,
    "avg_power_watts": 6.0,
    "avg_temperature_celsius": 54.0
  },
  "recyclerview": {
    "avg_score": 1750.0,
    "scroll_fps": 60.0,
    "memory_mb": 15.5,
    "sample_count": 10,
    "avg_power_watts": 6.2,
    "avg_temperature_celsius": 54.5
  },
  "canvas_drawing": {
    "avg_score": 1850.0,
    "operations_per_second": 50000,
    "sample_count": 10,
    "avg_power_watts": 6.1,
    "avg_temperature_celsius": 54.2
  },
  "image_filter": {
    "avg_score": 1700.0,
    "images_filtered_per_second": 25.0,
    "processing_time_ms": 40.0,
    "sample_count": 10,
    "avg_power_watts": 6.3,
    "avg_temperature_celsius": 54.8
  },
  "image_resize": {
    "avg_score": 1800.0,
    "images_resized_per_second": 40.0,
    "sample_count": 10,
    "avg_power_watts": 6.0,
    "avg_temperature_celsius": 54.0
  },
  "video_encoding": {
    "avg_score": 1600.0,
    "encoding_fps": 30.0,
    "time_seconds": 10.0,
    "sample_count": 10,
    "avg_power_watts": 6.8,
    "avg_temperature_celsius": 55.5
  },
  "video_transcoding": {
    "avg_score": 1550.0,
    "realtime_factor": 1.5,
    "sample_count": 10,
    "avg_power_watts": 6.9,
    "avg_temperature_celsius": 56.0
  },
  "pdf_rendering": {
    "avg_score": 1750.0,
    "pages_per_second": 5.0,
    "sample_count": 10,
    "avg_power_watts": 6.2,
    "avg_temperature_celsius": 54.6
  },
  "text_rendering": {
    "avg_score": 1850.0,
    "characters_per_second": 1000,
    "sample_count": 10,
    "avg_power_watts": 6.0,
    "avg_temperature_celsius": 54.0
  },
  "multitasking": {
    "avg_score": 1650.0,
    "performance_degradation_percent": 15.0,
    "sample_count": 10,
    "avg_power_watts": 7.0,
    "avg_temperature_celsius": 56.5
  }
}
```

### 7. CPU Test Results JSON Structure

The `cpu_test_results` column in the `FULL_BENCHMARK_DETAILS` table contains detailed results for each CPU test:

```json
{
  "prime_number_score": 1200.0,
  "prime_numbers_found": 1000000,
  "prime_test_duration_ms": 1500.0,
  "fibonacci_score": 1100.0,
  "fibonacci_computation_time_ms": 1200.0,
  "matrix_multiplication_score": 1300.0,
  "matrix_ops_per_second": 55000.0,
  "hash_computing_score": 1250.0,
  "hash_computations_per_second": 50000.0,
  "string_sorting_score": 1150.0,
  "strings_sorted_per_second": 20000,
  "ray_tracing_score": 1050.0,
 "rays_traced_per_second": 150000.0,
  "compression_score": 1180.0,
  "compression_speed_mbps": 200.0,
  "decompression_speed_mbps": 20.0,
  "monte_carlo_score": 1080.0,
 "monte_carlo_iterations": 100000,
  "json_parsing_score": 1220.0,
  "json_objects_parsed_per_second": 3000000,
  "n_queens_score": 1000.0,
 "n_queens_solutions_found": 50000,
  "n_queens_time_ms": 2000.0
}
```

### 8. AI/ML Test Results JSON Structure

The `ai_ml_test_results` column in the `FULL_BENCHMARK_DETAILS` table contains detailed results for each AI/ML test:

```json
{
  "llm_inference_score": 1800.0,
  "tokens_per_second_prompt": 15.5,
  "tokens_per_second_generation": 25.0,
  "time_to_first_token_ms": 120.0,
  "total_tokens_generated": 100,
  "image_classification_score": 2100.0,
  "images_per_second": 120.0,
  "avg_inference_time_ms": 8.3,
  "object_detection_score": 1950.0,
  "detections_per_second": 45.0,
  "avg_detection_time_ms": 2.2,
  "text_embedding_score": 1750.0,
  "embeddings_per_second": 300.0,
  "avg_embedding_time_ms": 3.3,
  "speech_to_text_score": 1650.0,
  "real_time_factor": 2.5,
  "transcription_accuracy_percent": 95.5
}
```

### 9. GPU Test Results JSON Structure

The `gpu_test_results` column in the `FULL_BENCHMARK_DETAILS` table contains detailed results for each GPU test:

```json
{
  "triangle_rendering_score": 1100.0,
  "triangle_rendering_fps": 55.0,
  "triangles_rendered": 1000000,
  "compute_shader_score": 1050.0,
  "matrix_multiplication_gflops": 2.5,
  "compute_time_ms": 1200.0,
  "particle_system_score": 950.0,
 "max_particles_60fps": 50000,
  "particle_update_time_ms": 800.0,
  "texture_sampling_score": 100.0,
  "pixel_fillrate_gpixels_per_sec": 15.5,
  "texture_bandwidth_gbps": 25.0,
  "tessellation_score": 900.0,
  "tessellation_throughput": 1.2,
  "geometry_processing_speed": 0.8,
  "unity_scene1_score": 850.0,
  "unity_scene1_avg_fps": 45.0,
  "unity_scene2_score": 800.0,
  "unity_scene2_avg_fps": 40.0,
  "unreal_scene1_score": 920.0,
  "unreal_scene1_avg_fps": 48.0,
  "unreal_scene2_score": 80.0,
  "unreal_scene2_avg_fps": 44.0,
  "unreal_scene3_score": 820.0,
  "unreal_scene3_avg_fps": 41.0
}
```

### 10. RAM Test Results JSON Structure

The `ram_test_results` column in the `FULL_BENCHMARK_DETAILS` table contains detailed results for each RAM test:

```json
{
  "sequential_read_score": 2800.0,
  "sequential_read_speed_mbps": 25000.0,
  "sequential_write_score": 2600.0,
  "sequential_write_speed_mbps": 22000.0,
  "random_access_score": 2200.0,
  "random_read_latency_ns": 80.0,
  "random_write_latency_ns": 85.0,
  "random_ops_per_second": 1500000,
  "memory_copy_score": 2400.0,
  "memory_copy_bandwidth_mbps": 20000.0,
  "multithread_score": 2700.0,
  "multithread_bandwidth_mbps": 23000.0,
  "optimal_thread_count": 8,
  "cache_hierarchy_score": 2300.0,
  "l1_cache_size_kb": 32,
  "l2_cache_size_kb": 256,
  "l3_cache_size_kb": 8192,
  "l1_cache_speed_gbps": 200.0,
  "l2_cache_speed_gbps": 150.0,
  "l3_cache_speed_gbps": 50.0,
  "ram_latency_ns": 75.0
}
```

### 11. Storage Test Results JSON Structure

The `storage_test_results` column in the `FULL_BENCHMARK_DETAILS` table contains detailed results for each storage test:

```json
{
  "sequential_read_score": 3200.0,
  "sequential_read_speed_mbps": 3500.0,
  "sequential_write_score": 3100.0,
  "sequential_write_speed_mbps": 3200.0,
  "random_4k_read_score": 2800.0,
  "random_4k_read_iops": 80000,
  "random_4k_read_latency_ms": 0.08,
  "random_4k_write_score": 2600.0,
  "random_4k_write_iops": 60000,
  "random_4k_write_latency_ms": 0.12,
  "small_file_ops_score": 2400.0,
  "files_created_per_second": 150,
  "files_read_per_second": 1200,
  "files_deleted_per_second": 1800,
  "database_performance_score": 2700.0,
  "sqlite_inserts_per_second": 25000,
  "sqlite_queries_per_second": 30000,
  "sqlite_updates_per_second": 20000,
  "sqlite_deletes_per_second": 15000,
 "mixed_workload_score": 2900.0,
  "mixed_workload_throughput_mbps": 2800.0
}
```

### 12. Productivity Test Results JSON Structure

The `productivity_test_results` column in the `FULL_BENCHMARK_DETAILS` table contains detailed results for each productivity test:

```json
{
  "ui_rendering_score": 1900.0,
  "ui_rendering_avg_fps": 58.0,
  "ui_frame_drops": 2,
  "jank_percentage": 0.5,
  "recyclerview_score": 1750.0,
  "recyclerview_scroll_fps": 60.0,
  "recyclerview_memory_mb": 15.5,
  "canvas_drawing_score": 1850.0,
  "draw_operations_per_second": 5000,
  "image_filter_score": 1700.0,
  "images_filtered_per_second": 25.0,
  "filter_processing_time_ms": 40.0,
  "image_resize_score": 1800.0,
  "images_resized_per_second": 40.0,
  "video_encoding_score": 1600.0,
  "video_encoding_fps": 30.0,
  "encoding_time_seconds": 10.0,
  "video_transcoding_score": 1550.0,
 "transcoding_realtime_factor": 1.5,
  "pdf_rendering_score": 1750.0,
  "pdf_pages_per_second": 5.0,
  "text_rendering_score": 1850.0,
  "characters_per_second": 10000,
 "layout_calculation_time_ms": 2.5,
  "multitasking_score": 1650.0,
  "performance_degradation_percent": 15.0
}
```

### 13. GPU Frame Metrics JSON Structure

The `frame_time_distribution`, `fps_timeline_data`, `gpu_utilization_timeline`, `gpu_temperature_timeline`, `gpu_frequency_timeline` columns in the `GPU_FRAME_METRICS` table contain time-series data:

```json
{
  "frame_time_distribution": {
    "0_16ms": 85,
    "16_33ms": 12,
    "33ms_plus": 3,
    "histogram": [
      {"range": "0-8ms", "count": 50},
      {"range": "8-16ms", "count": 100}
    ]
  },
  "fps_timeline_data": [
    {"time": 0, "fps": 60.0},
    {"time": 1, "fps": 58.0}
  ],
  "gpu_utilization_timeline": [
    {"time": 0, "utilization": 75.0},
    {"time": 1, "utilization": 80.0}
  ],
  "gpu_temperature_timeline": [
    {"time": 0, "temperature": 65.0},
    {"time": 1, "temperature": 68.0}
  ],
  "gpu_frequency_timeline": [
    {"time": 0, "frequency_mhz": 1500.0},
    {"time": 1, "frequency_mhz": 1600.0}
  ]
}
```

### 14. Full Benchmark Telemetry JSON Structure

The `cpu_temperature_timeline`, `gpu_temperature_timeline`, `battery_temperature_timeline`, `cpu_frequency_timeline`, `gpu_frequency_timeline`, `battery_level_timeline`, `memory_usage_timeline`, `power_consumption_timeline`, `thermal_throttle_events`, `performance_state_timeline` columns in the `FULL_BENCHMARK_TELEMETRY` table contain time-series data:

```json
{
  "cpu_temperature_timeline": [
    {"time": 0, "temperature": 45.0},
    {"time": 1, "temperature": 50.0}
  ],
  "gpu_temperature_timeline": [
    {"time": 0, "temperature": 55.0},
    {"time": 1, "temperature": 60.0}
  ],
 "battery_temperature_timeline": [
    {"time": 0, "temperature": 25.0},
    {"time": 1, "temperature": 28.0}
  ],
  "cpu_frequency_timeline": [
    {"time": 0, "frequency_ghz": 2.0},
    {"time": 1, "frequency_ghz": 2.5}
  ],
  "gpu_frequency_timeline": [
    {"time": 0, "frequency_mhz": 1000.0},
    {"time": 1, "frequency_mhz": 1200.0}
  ],
  "battery_level_timeline": [
    {"time": 0, "level_percent": 100.0},
    {"time": 1, "level_percent": 98.0}
  ],
  "memory_usage_timeline": [
    {"time": 0, "usage_mb": 1024.0},
    {"time": 1, "usage_mb": 1536.0}
  ],
  "power_consumption_timeline": [
    {"time": 0, "consumption_watts": 2.0},
    {"time": 1, "consumption_watts": 3.5}
  ],
  "thermal_throttle_events": [
    {"time": 10, "type": "cpu", "severity": "medium"},
    {"time": 15, "type": "gpu", "severity": "high"}
  ],
  "performance_state_timeline": [
    {"time": 0, "state": "normal"},
    {"time": 10, "state": "throttled"}
  ]
}
```

### 15. Throttle Test Details JSON Structure

The `temperature_curve_data`, `performance_curve_data`, `cpu_frequency_data`, `gpu_frequency_data` columns in the `THROTTLE_TEST_DETAILS` table contain curve data:

```json
{
  "temperature_curve_data": [
    {"time": 0, "cpu_temp": 45.0, "gpu_temp": 55.0, "battery_temp": 25.0},
    {"time": 60, "cpu_temp": 70.0, "gpu_temp": 80.0, "battery_temp": 30.0}
  ],
  "performance_curve_data": [
    {"time": 0, "initial_score": 1000.0, "current_score": 1000.0, "percentage": 100.0},
    {"time": 60, "initial_score": 1000.0, "current_score": 800.0, "percentage": 80.0}
  ],
  "cpu_frequency_data": [
    {"time": 0, "frequency_ghz": 2.5, "utilization": 90.0},
    {"time": 60, "frequency_ghz": 2.0, "utilization": 75.0}
  ],
  "gpu_frequency_data": [
    {"time": 0, "frequency_mhz": 1500.0, "utilization": 85.0},
    {"time": 60, "frequency_mhz": 1200.0, "utilization": 60.0}
  ]
}
```

### 16. Efficiency Test Details JSON Structure

The `power_consumption_data` column in the `EFFICIENCY_TEST_DETAILS` table contains power consumption metrics:

```json
{
  "power_curve_data": [
    {"time": 0, "power_watts": 1.5, "efficiency_score": 1000.0},
    {"time": 300, "power_watts": 2.0, "efficiency_score": 950.0}
  ],
  "battery_drain_curve": [
    {"time": 0, "battery_percent": 100.0, "drain_rate_mah": 0.0},
    {"time": 300, "battery_percent": 95.0, "drain_rate_mah": 10.0}
  ],
  "performance_per_watt_timeline": [
    {"time": 0, "ppw": 666.6, "performance": 1000.0, "power": 1.5},
    {"time": 30, "ppw": 475.0, "performance": 950.0, "power": 2.0}
  ]
}
```

### 17. Leaderboard Cache JSON Structure

The `top_100_results` column in the `LEADERBOARD_CACHE` table contains leaderboard data:

```json
{
  "top_100": [
    {
      "rank": 1,
      "device_id": 12345,
      "device_name": "Example Device",
      "soc_name": "Example SOC",
      "manufacturer": "Example Manufacturer",
      "overall_score": 5000.0,
      "performance_grade": "A+",
      "submission_date": "2023-05-15T10:30:00Z"
    }
  ],
  "last_updated": "2023-05-15T10:30:00Z",
  "total_entries": 5000
}
```

### 18. Comparison AI Cache JSON Structure

The `ai_summary`, `comparison_data` columns in the `COMPARISON_AI_CACHE` table contain comparison information:

```json
{
  "ai_summary": {
    "winner": "device1",
    "winner_reason": "Better CPU and GPU performance",
    "key_differences": [
      "Device 1 has 15% better CPU score",
      "Device 2 has 8% better battery life"
    ],
    "recommendation": "Choose Device 1 for performance, Device 2 for battery life"
  },
  "comparison_data": {
    "device1": {
      "name": "Device 1",
      "overall_score": 4500.0,
      "cpu_score": 2200.0,
      "gpu_score": 2000.0,
      "battery_score": 1800.0
    },
    "device2": {
      "name": "Device 2",
      "overall_score": 4300.0,
      "cpu_score": 1900.0,
      "gpu_score": 2100.0,
      "battery_score": 2000.0
    }
  }
}
```

### 19. Device Summary View JSON Structure

The `category_scores`, `top_strengths`, `top_weaknesses` columns in the `DEVICE_SUMMARY_VIEW` table contain device summary information:

```json
{
  "category_scores": {
    "cpu": 2200.0,
    "gpu": 2000.0,
    "ram": 1800.0,
    "storage": 1900.0,
    "ai_ml": 1700.0,
    "productivity": 1600.0
  },
  "top_strengths": [
    "Excellent CPU performance",
    "Good GPU capabilities",
    "Fast storage"
  ],
 "top_weaknesses": [
    "Average RAM performance",
    "Below average AI/ML performance"
  ]
}
```

### 20. SOC Summary View JSON Structure

The `category_scores`, `gpu_test_summary`, `thermal_performance_summary`, `power_efficiency_summary`, `competing_socs` columns in the `SOC_SUMMARY_VIEW` table contain SOC summary information:

```json
{
  "category_scores": {
    "cpu": 2200.0,
    "gpu": 2000.0,
    "ram": 1800.0,
    "storage": 190.0,
    "ai_ml": 1700.0,
    "productivity": 1600.0
  },
  "gpu_test_summary": {
    "triangle_rendering": 2000.0,
    "compute_shader": 1800.0,
    "particle_system": 1900.0
  },
  "thermal_performance_summary": {
    "avg_temperature_celsius": 65.0,
    "max_temperature_celsius": 85.0,
    "thermal_throttling_start": 75.0
  },
  "power_efficiency_summary": {
    "avg_power_watts": 5.5,
    "peak_power_watts": 12.0,
    "performance_per_watt": 400.0
  },
  "competing_socs": [
    {"soc_name": "Competitor SOC A", "score": 4200.0, "difference": -300.0},
    {"soc_name": "Competitor SOC B", "score": 400.0, "difference": -100.0}
  ]
}
```

### 21. Comparison Cache JSON Structure

The `full_comparison_data`, `score_differences`, `category_comparisons`, `visual_chart_data`, `winner_summary`, `recommendations` columns in the `COMPARISON_CACHE` table contain detailed comparison data:

```json
{
  "full_comparison_data": {
    "entity1": {
      "name": "Device A",
      "overall_score": 4500.0,
      "cpu_score": 2200.0,
      "gpu_score": 2000.0
    },
    "entity2": {
      "name": "Device B",
      "overall_score": 4300.0,
      "cpu_score": 2000.0,
      "gpu_score": 2100.0
    }
 },
  "score_differences": {
    "overall": 200.0,
    "cpu": 200.0,
    "gpu": -100.0
  },
  "category_comparisons": [
    {
      "category": "CPU",
      "entity1_score": 220.0,
      "entity2_score": 2000.0,
      "winner": "entity1",
      "advantage_percent": 10.0
    }
  ],
  "visual_chart_data": {
    "radar_chart": [
      {"category": "CPU", "entity1": 2200.0, "entity2": 2000.0},
      {"category": "GPU", "entity1": 2000.0, "entity2": 2100.0}
    ],
    "bar_chart": [
      {"category": "Overall", "entity1": 4500.0, "entity2": 4300.0}
    ]
  },
  "winner_summary": {
    "overall_winner": "entity1",
    "winning_categories": ["CPU", "RAM", "Storage"],
    "losing_categories": ["GPU"]
  },
  "recommendations": [
    "Device A is better for CPU-intensive tasks",
    "Device B is better for GPU-intensive tasks"
  ]
}
```

## Relationship Documentation

The following relationships exist between tables in the database:

### Primary Relationships
- `USERS` to `BENCHMARK_RESULTS`: One-to-Many (one user can submit many benchmark results)
- `USERS` to `USER_DEVICES`: One-to-Many (one user can own many devices)
- `DEVICES` to `BENCHMARK_RESULTS`: One-to-Many (one device can have many benchmark results)
- `DEVICES` to `USER_DEVICES`: One-to-Many (one device can be owned by many users)
- `DEVICES` to `DEVICE_SPECIFICATIONS`: One-to-One (one device has one specification)
- `DEVICE_SPECIFICATIONS` to `SOC_APU_SPECS`: Many-to-One (many device specs reference one SOC/APU spec)
- `DEVICE_SPECIFICATIONS` to `CPU_SPECS`: Many-to-One (many device specs reference one CPU spec)
- `DEVICE_SPECIFICATIONS` to `GPU_SPECS`: Many-to-One (many device specs reference one GPU spec)
- `DEVICE_SPECIFICATIONS` to `RAM_SPECS`: Many-to-One (many device specs reference one RAM spec)
- `DEVICE_SPECIFICATIONS` to `STORAGE_SPECS`: Many-to-One (many device specs reference one storage spec)

### Benchmark Results Relationships
- `BENCHMARK_RESULTS` to `TEST_ENVIRONMENT`: One-to-One (one benchmark result has one test environment)
- `BENCHMARK_RESULTS` to `FULL_BENCHMARK_DETAILS`: One-to-One (one benchmark result has one full details record)
- `BENCHMARK_RESULTS` to `THROTTLE_TEST_DETAILS`: One-to-One (one benchmark result has one throttle details record)
- `BENCHMARK_RESULTS` to `EFFICIENCY_TEST_DETAILS`: One-to-One (one benchmark result has one efficiency details record)

### Full Benchmark Details Relationships
- `FULL_BENCHMARK_DETAILS` to `FULL_BENCHMARK_TELEMETRY`: One-to-One (one full details record has one telemetry record)
- `FULL_BENCHMARK_DETAILS` to `CPU_TEST_RESULTS`: One-to-Many (one full details record has many CPU test results)
- `FULL_BENCHMARK_DETAILS` to `AI_ML_TEST_RESULTS`: One-to-Many (one full details record has many AI/ML test results)
- `FULL_BENCHMARK_DETAILS` to `GPU_TEST_RESULTS`: One-to-Many (one full details record has many GPU test results)
- `FULL_BENCHMARK_DETAILS` to `RAM_TEST_RESULTS`: One-to-Many (one full details record has many RAM test results)
- `FULL_BENCHMARK_DETAILS` to `STORAGE_TEST_RESULTS`: One-to-Many (one full details record has many storage test results)
- `FULL_BENCHMARK_DETAILS` to `PRODUCTIVITY_TEST_RESULTS`: One-to-Many (one full details record has many productivity test results)

### GPU Test Relationships
- `GPU_TEST_RESULTS` to `GPU_FRAME_METRICS`: One-to-Many (one GPU test result has many frame metrics)

### System and Statistics Relationships
- `SYSTEM_LOGS` to `USERS`: Many-to-One (many logs can reference one user)
- `SYSTEM_LOGS` to `BENCHMARK_RESULTS`: Many-to-One (many logs can reference one benchmark result)
- `DEVICE_STATISTICS` to `DEVICES`: Many-to-One (many statistics can reference one device)

### View Relationships
- `DEVICE_SUMMARY_VIEW` to `DEVICES`: One-to-One (one device has one summary view)
- `DEVICE_SUMMARY_VIEW` to `SOC_APU_SPECS`: Many-to-One (many device summaries reference one SOC/APU spec)
- `SOC_SUMMARY_VIEW` to `SOC_APU_SPECS`: One-to-One (one SOC/APU spec has one summary view)

### Leaderboard Relationships
- Leaderboard tables to `DEVICES`: Many-to-One (many leaderboard entries reference one device)
- Leaderboard tables to `SOC_APU_SPECS`: Many-to-One (many leaderboard entries reference one SOC/APU spec)

### Cache Relationships
- `LEADERBOARD_CACHE` to `DEVICES`: No direct relationship (caches aggregated data)
- `COMPARISON_AI_CACHE` to `DEVICES`: No direct relationship (caches comparison data)
- `COMPARISON_CACHE` to `DEVICES`: No direct relationship (caches comparison data)

These relationships ensure data integrity and enable efficient querying across the benchmark application's various features.