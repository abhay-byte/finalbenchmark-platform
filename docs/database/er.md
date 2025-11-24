erDiagram
    USERS ||--o{ BENCHMARK_RESULTS : submits
    USERS ||--o{ USER_DEVICES : owns
    USERS {
        bigint user_id PK
        string email UK
        string username UK
        string password_hash
        string first_name
        string last_name
        string profile_picture_url
        enum account_type "guest,registered"
        enum auth_provider "local,google,facebook,apple"
        string external_auth_id
        boolean email_verified
        datetime email_verified_at
        string verification_token
        string reset_token
        datetime reset_token_expiry
        datetime created_at
        datetime updated_at
        datetime last_login_at
        string last_login_ip
        boolean is_active
        boolean is_deleted
        string country_code
        string timezone
        json preferences
    }

    DEVICES ||--o{ BENCHMARK_RESULTS : tested_on
    DEVICES ||--o{ USER_DEVICES : links
    DEVICES ||--|| DEVICE_SPECIFICATIONS : has
    DEVICES {
        bigint device_id PK
        string device_name UK
        string manufacturer
        string model_number
        string marketing_name
        string device_code_name
        enum device_type "phone,tablet,foldable,laptop,desktop,mini_pc"
        enum platform "android,windows,linux,macos,ios"
        date release_date
        decimal release_price
        string image_url
        datetime created_at
        datetime updated_at
    }

    DEVICE_SPECIFICATIONS ||--o| SOC_APU_SPECS : references
    DEVICE_SPECIFICATIONS ||--o| CPU_SPECS : references
    DEVICE_SPECIFICATIONS ||--o| GPU_SPECS : references
    DEVICE_SPECIFICATIONS ||--|| RAM_SPECS : references
    DEVICE_SPECIFICATIONS ||--|| STORAGE_SPECS : references
    DEVICE_SPECIFICATIONS {
        bigint spec_id PK
        bigint device_id FK
        bigint soc_apu_spec_id FK
        bigint cpu_spec_id FK
        bigint gpu_spec_id FK
        bigint ram_spec_id FK
        bigint storage_spec_id FK
        boolean is_integrated_system
        decimal display_size_inch
        int display_width_px
        int display_height_px
        int display_refresh_rate_hz
        string display_panel_type
        int battery_capacity_mah
        string charging_type
        int charging_max_watt
        string os_version
        string os_build_number
        int os_api_level
        decimal weight_grams
        decimal thickness_mm
        string build_material
        string ip_rating
        boolean has_5g
        boolean has_nfc
        boolean has_wireless_charging
        string motherboard_model
        string bios_version
        int total_usb_ports
        boolean has_thunderbolt
        string cooling_type
        int fan_count
        datetime created_at
        datetime updated_at
    }

    SOC_APU_SPECS ||--|| CPU_SPECS : contains
    SOC_APU_SPECS ||--|| GPU_SPECS : contains
    SOC_APU_SPECS {
        bigint soc_apu_spec_id PK
        string chip_name UK
        enum chip_type "soc,apu"
        string manufacturer
        string chip_family
        string codename
        bigint cpu_spec_id FK
        bigint gpu_spec_id FK
        string process_node
        int total_tdp_watts
        int min_tdp_watts
        int max_tdp_watts
        string supported_memory_types
        int max_memory_speed_mhz
        string npu_model
        decimal npu_tops
        date release_date
        decimal avg_full_benchmark_score
        decimal avg_throttle_score
        decimal avg_efficiency_score
        json avg_gpu_test_metrics
        json avg_cpu_category_metrics
        json avg_ai_ml_category_metrics
        json avg_ram_category_metrics
        json avg_storage_category_metrics
        json avg_productivity_category_metrics
        datetime created_at
        datetime updated_at
    }

    CPU_SPECS {
        bigint cpu_spec_id PK
        string cpu_brand
        string cpu_model UK
        string cpu_family
        string cpu_architecture
        string cpu_socket_type
        int cpu_cores
        int cpu_threads
        int cpu_performance_cores
        int cpu_efficiency_cores
        decimal cpu_base_frequency_ghz
        decimal cpu_boost_frequency_ghz
        decimal cpu_max_turbo_frequency_ghz
        string cpu_process_node
        int cpu_tdp_watts
        int cpu_max_tdp_watts
        int l1_cache_kb
        int l2_cache_kb
        int l3_cache_kb
        boolean supports_hyperthreading
        boolean supports_overclocking
        string instruction_sets
        int memory_channels
        string supported_memory_types
        int max_memory_speed_mhz
        datetime created_at
        datetime updated_at
    }

    GPU_SPECS {
        bigint gpu_spec_id PK
        string gpu_brand
        string gpu_model UK
        string gpu_architecture
        enum gpu_type "integrated,dedicated,mobile"
        int gpu_cores
        int gpu_shader_units
        int gpu_compute_units
        int gpu_texture_units
        int gpu_rops
        decimal gpu_base_frequency_mhz
        decimal gpu_boost_frequency_mhz
        int gpu_memory_size_gb
        string gpu_memory_type
        int gpu_memory_bus_width
        decimal gpu_memory_bandwidth_gbps
        int gpu_tdp_watts
        string gpu_process_node
        boolean supports_ray_tracing
        boolean supports_dlss
        boolean supports_fsr
        string directx_version
        string opengl_version
        string vulkan_version
        string metal_version
        int max_display_outputs
        datetime created_at
        datetime updated_at
    }

    RAM_SPECS {
        bigint ram_spec_id PK
        int ram_size_gb UK
        string ram_type UK
        string ram_generation
        decimal ram_frequency_mhz
        int ram_modules_count
        string ram_form_factor
        int ram_cas_latency
        decimal ram_voltage
        boolean supports_xmp
        boolean supports_expo
        int ram_channels
        boolean is_ecc
        string ram_manufacturer
        datetime created_at
        datetime updated_at
    }

    STORAGE_SPECS {
        bigint storage_spec_id PK
        int storage_size_gb UK
        string storage_type UK
        string storage_interface
        string storage_form_factor
        int storage_pcie_generation
        int storage_pcie_lanes
        boolean supports_nvme
        decimal sequential_read_spec_mbps
        decimal sequential_write_spec_mbps
        int random_read_spec_iops
        int random_write_spec_iops
        int tbw_rating
        int mtbf_hours
        string storage_manufacturer
        datetime created_at
        datetime updated_at
    }

    USER_DEVICES {
        bigint user_device_id PK
        bigint user_id FK
        bigint device_id FK
        string custom_device_name
        boolean is_primary
        datetime added_at
    }

    BENCHMARK_RESULTS ||--|| FULL_BENCHMARK_DETAILS : contains
    BENCHMARK_RESULTS ||--|| THROTTLE_TEST_DETAILS : contains
    BENCHMARK_RESULTS ||--|| EFFICIENCY_TEST_DETAILS : contains
    BENCHMARK_RESULTS ||--|| TEST_ENVIRONMENT : has
    BENCHMARK_RESULTS {
        bigint result_id PK
        bigint user_id FK
        bigint device_id FK
        enum benchmark_mode "full,throttle,efficiency"
        decimal overall_score
        enum performance_grade "A+,A,B+,B,C,D,F"
        int global_rank
        int category_rank
        datetime test_started_at
        datetime test_completed_at
        int test_duration_seconds
        string app_version
        boolean is_verified
        boolean is_public
        boolean is_flagged
        string share_token UK
        int view_count
        datetime created_at
        datetime updated_at
    }

    TEST_ENVIRONMENT {
        bigint env_id PK
        bigint result_id FK
        decimal ambient_temperature_celsius
        int battery_level_start
        int battery_level_end
        boolean is_charging
        decimal screen_brightness_percent
        boolean wifi_enabled
        boolean bluetooth_enabled
        boolean mobile_data_enabled
        boolean gps_enabled
        int running_apps_count
        decimal available_ram_mb
        decimal available_storage_gb
        string kernel_version
        string build_fingerprint
        string security_patch_level
        boolean is_rooted
        boolean is_throttling_enabled
        decimal cpu_temp_start_celsius
        decimal gpu_temp_start_celsius
        decimal battery_temp_start_celsius
        datetime recorded_at
    }

    FULL_BENCHMARK_DETAILS ||--|| FULL_BENCHMARK_TELEMETRY : tracks
    FULL_BENCHMARK_DETAILS {
        bigint full_detail_id PK
        bigint result_id FK
        decimal cpu_score
        decimal ai_ml_score
        decimal gpu_score
        decimal ram_score
        decimal storage_score
        decimal productivity_score
        json cpu_test_results
        json ai_ml_test_results
        json gpu_test_results
        json ram_test_results
        json storage_test_results
        json productivity_test_results
    }

    FULL_BENCHMARK_TELEMETRY {
        bigint telemetry_id PK
        bigint full_detail_id FK
        json cpu_temperature_timeline
        json gpu_temperature_timeline
        json battery_temperature_timeline
        json cpu_frequency_timeline
        json gpu_frequency_timeline
        json battery_level_timeline
        json memory_usage_timeline
        json power_consumption_timeline
        json thermal_throttle_events
        json performance_state_timeline
        decimal avg_cpu_temp_celsius
        decimal max_cpu_temp_celsius
        decimal avg_gpu_temp_celsius
        decimal max_gpu_temp_celsius
        decimal avg_battery_temp_celsius
        decimal max_battery_temp_celsius
        decimal avg_cpu_frequency_ghz
        decimal avg_gpu_frequency_mhz
        int total_throttle_events
        decimal total_battery_drain_percent
        decimal avg_power_consumption_watts
        decimal peak_power_consumption_watts
    }

    THROTTLE_TEST_DETAILS {
        bigint throttle_detail_id PK
        bigint result_id FK
        int test_duration_minutes
        decimal initial_performance_score
        decimal sustained_performance_score
        decimal performance_retention_percent
        int time_to_throttle_seconds
        decimal max_cpu_temperature_celsius
        decimal max_gpu_temperature_celsius
        decimal max_battery_temperature_celsius
        decimal avg_cpu_temperature_celsius
        decimal avg_gpu_temperature_celsius
        decimal thermal_efficiency_score
        decimal throttling_percentage
        json temperature_curve_data
        json performance_curve_data
        json cpu_frequency_data
        json gpu_frequency_data
    }

    EFFICIENCY_TEST_DETAILS {
        bigint efficiency_detail_id PK
        bigint result_id FK
        decimal performance_output_score
        decimal power_consumption_watts
        decimal battery_drain_mah
        decimal battery_drain_percent
        decimal efficiency_score
        decimal performance_per_watt
        decimal avg_cpu_temperature_celsius
        decimal avg_gpu_temperature_celsius
        decimal avg_battery_temperature_celsius
        decimal peak_cpu_frequency_ghz
        decimal peak_gpu_frequency_mhz
        json power_consumption_data
    }

    FULL_BENCHMARK_DETAILS ||--|| CPU_TEST_RESULTS : contains
    CPU_TEST_RESULTS {
        bigint cpu_test_id PK
        bigint full_detail_id FK
        decimal prime_number_score
        int prime_numbers_found
        decimal prime_test_duration_ms
        decimal fibonacci_score
        decimal fibonacci_computation_time_ms
        decimal matrix_multiplication_score
        decimal matrix_ops_per_second
        decimal hash_computing_score
        decimal hash_computations_per_second
        decimal string_sorting_score
        int strings_sorted_per_second
        decimal ray_tracing_score
        decimal rays_traced_per_second
        decimal compression_score
        decimal compression_speed_mbps
        decimal decompression_speed_mbps
        decimal monte_carlo_score
        int monte_carlo_iterations
        decimal json_parsing_score
        int json_objects_parsed_per_second
        decimal n_queens_score
        int n_queens_solutions_found
        decimal n_queens_time_ms
    }

    FULL_BENCHMARK_DETAILS ||--|| AI_ML_TEST_RESULTS : contains
    AI_ML_TEST_RESULTS {
        bigint ai_ml_test_id PK
        bigint full_detail_id FK
        decimal llm_inference_score
        decimal tokens_per_second_prompt
        decimal tokens_per_second_generation
        decimal time_to_first_token_ms
        int total_tokens_generated
        decimal image_classification_score
        decimal images_per_second
        decimal avg_inference_time_ms
        decimal object_detection_score
        decimal detections_per_second
        decimal avg_detection_time_ms
        decimal text_embedding_score
        int embeddings_per_second
        decimal avg_embedding_time_ms
        decimal speech_to_text_score
        decimal real_time_factor
        decimal transcription_accuracy_percent
    }

    FULL_BENCHMARK_DETAILS ||--|| GPU_TEST_RESULTS : contains
    GPU_TEST_RESULTS ||--|| GPU_FRAME_METRICS : tracks
    GPU_TEST_RESULTS {
        bigint gpu_test_id PK
        bigint full_detail_id FK
        decimal triangle_rendering_score
        decimal triangle_rendering_fps
        int triangles_rendered
        decimal compute_shader_score
        decimal matrix_multiplication_gflops
        decimal compute_time_ms
        decimal particle_system_score
        int max_particles_60fps
        decimal particle_update_time_ms
        decimal texture_sampling_score
        decimal pixel_fillrate_gpixels_per_sec
        decimal texture_bandwidth_gbps
        decimal tessellation_score
        decimal tessellation_throughput
        decimal geometry_processing_speed
        decimal unity_scene1_score
        decimal unity_scene1_avg_fps
        decimal unity_scene2_score
        decimal unity_scene2_avg_fps
        decimal unreal_scene1_score
        decimal unreal_scene1_avg_fps
        decimal unreal_scene2_score
        decimal unreal_scene2_avg_fps
        decimal unreal_scene3_score
        decimal unreal_scene3_avg_fps
    }

    GPU_FRAME_METRICS {
        bigint frame_metric_id PK
        bigint gpu_test_id FK
        string test_name
        decimal avg_fps
        decimal min_fps
        decimal max_fps
        decimal fps_std_deviation
        decimal percentile_1_low_fps
        decimal percentile_0_1_low_fps
        decimal percentile_99_fps
        int total_frames
        int dropped_frames
        decimal frame_time_avg_ms
        decimal frame_time_min_ms
        decimal frame_time_max_ms
        decimal frame_time_99th_percentile_ms
        int frame_spikes_count
        json frame_time_distribution
        json fps_timeline_data
        json gpu_utilization_timeline
        json gpu_temperature_timeline
        json gpu_frequency_timeline
        decimal avg_gpu_utilization_percent
        decimal max_gpu_utilization_percent
    }

    FULL_BENCHMARK_DETAILS ||--|| RAM_TEST_RESULTS : contains
    RAM_TEST_RESULTS {
        bigint ram_test_id PK
        bigint full_detail_id FK
        decimal sequential_read_score
        decimal sequential_read_speed_mbps
        decimal sequential_write_score
        decimal sequential_write_speed_mbps
        decimal random_access_score
        decimal random_read_latency_ns
        decimal random_write_latency_ns
        int random_ops_per_second
        decimal memory_copy_score
        decimal memory_copy_bandwidth_mbps
        decimal multithread_score
        decimal multithread_bandwidth_mbps
        int optimal_thread_count
        decimal cache_hierarchy_score
        int l1_cache_size_kb
        int l2_cache_size_kb
        int l3_cache_size_kb
        decimal l1_cache_speed_gbps
        decimal l2_cache_speed_gbps
        decimal l3_cache_speed_gbps
        decimal ram_latency_ns
    }

    FULL_BENCHMARK_DETAILS ||--|| STORAGE_TEST_RESULTS : contains
    STORAGE_TEST_RESULTS {
        bigint storage_test_id PK
        bigint full_detail_id FK
        decimal sequential_read_score
        decimal sequential_read_speed_mbps
        decimal sequential_write_score
        decimal sequential_write_speed_mbps
        decimal random_4k_read_score
        int random_4k_read_iops
        decimal random_4k_read_latency_ms
        decimal random_4k_write_score
        int random_4k_write_iops
        decimal random_4k_write_latency_ms
        decimal small_file_ops_score
        int files_created_per_second
        int files_read_per_second
        int files_deleted_per_second
        decimal database_performance_score
        int sqlite_inserts_per_second
        int sqlite_queries_per_second
        int sqlite_updates_per_second
        int sqlite_deletes_per_second
        decimal mixed_workload_score
        decimal mixed_workload_throughput_mbps
    }

    FULL_BENCHMARK_DETAILS ||--|| PRODUCTIVITY_TEST_RESULTS : contains
    PRODUCTIVITY_TEST_RESULTS {
        bigint productivity_test_id PK
        bigint full_detail_id FK
        decimal ui_rendering_score
        decimal ui_rendering_avg_fps
        int ui_frame_drops
        decimal jank_percentage
        decimal recyclerview_score
        decimal recyclerview_scroll_fps
        decimal recyclerview_memory_mb
        decimal canvas_drawing_score
        int draw_operations_per_second
        decimal image_filter_score
        decimal images_filtered_per_second
        decimal filter_processing_time_ms
        decimal image_resize_score
        int images_resized_per_second
        decimal video_encoding_score
        decimal video_encoding_fps
        decimal encoding_time_seconds
        decimal video_transcoding_score
        decimal transcoding_realtime_factor
        decimal pdf_rendering_score
        int pdf_pages_per_second
        decimal text_rendering_score
        int characters_per_second
        decimal layout_calculation_time_ms
        decimal multitasking_score
        decimal performance_degradation_percent
    }

    LEADERBOARD_CACHE {
        bigint cache_id PK
        enum benchmark_mode "full,throttle,efficiency"
        string category
        string phone_brand
        string cpu_brand
        json top_100_results
        datetime last_updated
        datetime expires_at
    }

    COMPARISON_AI_CACHE {
        string cache_key PK
        string device1_id
        string device2_id
        enum comparison_type "device,soc"
        json ai_summary
        json comparison_data
        int access_count
        datetime created_at
        datetime last_accessed
        datetime expires_at
    }

    DEVICE_STATISTICS {
        bigint stat_id PK
        bigint device_id FK
        enum benchmark_mode "full,throttle,efficiency"
        int total_submissions
        decimal avg_overall_score
        decimal min_overall_score
        decimal max_overall_score
        decimal std_deviation
        decimal median_score
        int rank_position
        datetime last_calculated
    }

    SYSTEM_LOGS {
        bigint log_id PK
        bigint user_id FK
        bigint result_id FK
        enum log_level "info,warning,error,critical"
        string log_type
        text message
        json metadata
        string ip_address
        string user_agent
        datetime created_at
    }

    DEVICE_SUMMARY_VIEW {
        bigint device_id PK
        string device_name
        string manufacturer
        string model_number
        enum device_type "phone,tablet,foldable,laptop,desktop,mini_pc"
        enum platform "android,windows,linux,macos,ios"
        date release_date
        bigint soc_apu_spec_id FK
        string soc_name
        string cpu_model
        string gpu_model
        int ram_size_gb
        int storage_size_gb
        int total_benchmarks_submitted
        decimal avg_full_benchmark_score
        decimal avg_throttle_score
        decimal avg_efficiency_score
        int full_benchmark_rank
        int throttle_rank
        int efficiency_rank
        decimal score_percentile
        json category_scores
        json top_strengths
        json top_weaknesses
        datetime last_benchmark_date
        datetime last_updated
    }

    SOC_SUMMARY_VIEW {
        bigint soc_apu_spec_id PK
        string chip_name
        enum chip_type "soc,apu"
        string manufacturer
        string cpu_model
        string gpu_model
        string process_node
        int total_tdp_watts
        decimal npu_tops
        int total_devices_using
        int total_benchmarks_submitted
        decimal avg_full_benchmark_score
        decimal avg_throttle_score
        decimal avg_efficiency_score
        int full_benchmark_rank
        int throttle_rank
        int efficiency_rank
        decimal score_percentile
        json category_scores
        json gpu_test_summary
        json thermal_performance_summary
        json power_efficiency_summary
        json competing_socs
        datetime last_benchmark_date
        datetime last_updated
    }

    LEADERBOARD_FULL_BENCHMARK {
        bigint rank PK
        bigint device_id FK
        bigint soc_apu_spec_id FK
        string device_name
        string soc_name
        string manufacturer
        decimal overall_score
        enum performance_grade "A+,A,B+,B,C,D,F"
        decimal cpu_score
        decimal ai_ml_score
        decimal gpu_score
        decimal ram_score
        decimal storage_score
        decimal productivity_score
        int total_submissions
        decimal avg_score
        decimal score_variance
        string phone_brand
        string cpu_brand
        datetime last_updated
    }

    LEADERBOARD_THROTTLE {
        bigint rank PK
        bigint device_id FK
        bigint soc_apu_spec_id FK
        string device_name
        string soc_name
        string manufacturer
        decimal throttle_score
        enum performance_grade "A+,A,B+,B,C,D,F"
        decimal performance_retention_percent
        decimal avg_temperature_celsius
        decimal thermal_efficiency
        int total_submissions
        decimal avg_score
        string phone_brand
        string cpu_brand
        datetime last_updated
    }

    LEADERBOARD_EFFICIENCY {
        bigint rank PK
        bigint device_id FK
        bigint soc_apu_spec_id FK
        string device_name
        string soc_name
        string manufacturer
        decimal efficiency_score
        enum performance_grade "A+,A,B+,B,C,D,F"
        decimal performance_per_watt
        decimal avg_power_consumption_watts
        decimal performance_output
        int total_submissions
        decimal avg_score
        string phone_brand
        string cpu_brand
        datetime last_updated
    }

    LEADERBOARD_CATEGORY {
        bigint entry_id PK
        enum category "cpu,ai_ml,gpu,ram,storage,productivity"
        bigint rank
        bigint device_id FK
        bigint soc_apu_spec_id FK
        string device_name
        string soc_name
        string manufacturer
        decimal category_score
        int total_submissions
        decimal avg_score
        string phone_brand
        string cpu_brand
        datetime last_updated
    }

    COMPARISON_CACHE {
        string comparison_id PK
        enum comparison_type "device_vs_device,soc_vs_soc,device_vs_soc"
        string entity1_id
        string entity2_id
        string entity1_name
        string entity2_name
        json full_comparison_data
        json score_differences
        json category_comparisons
        json visual_chart_data
        json winner_summary
        json recommendations
        int view_count
        datetime created_at
        datetime last_accessed
        datetime expires_at
    }