cmake_minimum_required(VERSION 3.22.1)

# F-Droid reproducible builds: disable build-id and remove embedded build paths
add_link_options("LINKER:--build-id=none")
add_compile_options("-ffile-prefix-map=${CMAKE_CURRENT_SOURCE_DIR}=.")

project("vulkan-native")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# ============================================
# MAXIMUM OPTIMIZATION FLAGS
# ============================================

# Release build optimization flags
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -DNDEBUG")
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -O3 -DNDEBUG")

# Additional aggressive optimizations
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffast-math")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -funroll-loops")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fomit-frame-pointer")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffunction-sections")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fdata-sections")

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffast-math")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -funroll-loops")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fomit-frame-pointer")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffunction-sections")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fdata-sections")

# Architecture-specific optimizations
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8-a+crypto")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mtune=cortex-a76")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=armv8-a+crypto")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -mtune=cortex-a76")
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv7-a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mfpu=neon")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mfloat-abi=softfp")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=armv7-a")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -mfpu=neon")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -mfloat-abi=softfp")
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=x86-64")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mtune=generic")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=x86-64")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -mtune=generic")
elseif(ANDROID_ABI STREQUAL "x86")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=i686")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mtune=generic")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=i686")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -mtune=generic")
endif()

# ============================================
# LINK-TIME OPTIMIZATION (LTO)
# ============================================

# Note: LTO is disabled because NDK 25 uses gold linker which doesn't support
# LTO properly on arm64. The optimization flags above still provide significant
# performance improvements.
# set(CMAKE_INTERPROCEDURAL_OPTIMIZATION TRUE)

# ============================================
# LINKER OPTIMIZATIONS
# ============================================

# Remove unused sections
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--gc-sections")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--as-needed")

# 16 KB page size alignment for newer Android devices
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384")

# ============================================
# BUILD YOUR LIBRARY
# ============================================

# Add library
add_library(
    vulkan_native
    SHARED
    vulkan_info.cpp
    vulkan_benchmark.cpp        # Phase 2: Vulkan compute/render benchmark
    vulkan_perf_query.cpp       # Phase 4: VK_KHR_performance_query counter enumeration
    opencl_benchmark.cpp        # Phase 3: OpenCL via dlopen (no link-time dep)
    cpu_info.cpp                # CPU info native implementation
    cpu_affinity.cpp            # CPU affinity control for single-core benchmarks
    ram_benchmark.c             # RAM bandwidth/latency benchmarks (NEON + pthreads)
    storage_benchmark.c         # Storage UFS benchmarks (posix_fadvise + fdatasync)
    ai_benchmark_native.cpp     # AI compute benchmarks (OpenCL→Vulkan→GLES→CPU fallback)
    ai_vulkan_matmul.cpp        # Vulkan compute GEMM for AI benchmark (universal GPU support)
)

# Find required libraries
find_library(
    log-lib
    log
)

find_library(
    android-lib
    android
)

# Find and link Vulkan library
find_library(
    vulkan-lib
    vulkan
)

# EGL + GLESv3 for AI benchmark OpenGL ES compute backend
find_library(
    egl-lib
    EGL
)
find_library(
    glesv3-lib
    GLESv3
)

# Find dl library for dlopen (OpenCL runtime loading — Phase 3)
find_library(
    dl-lib
    dl
)

# Check if Vulkan library exists — define macro for runtime guard
if(vulkan-lib)
    message(STATUS "Vulkan library found: ${vulkan-lib}")
    target_compile_definitions(vulkan_native PRIVATE HAS_VULKAN=1)
else()
    message(WARNING "Vulkan library not found. Vulkan benchmark will be disabled at runtime.")
    target_compile_definitions(vulkan_native PRIVATE HAS_VULKAN=0)
endif()

# Link libraries to the native library
target_link_libraries(
    vulkan_native
    ${log-lib}
    ${android-lib}
    ${dl-lib}
    ${egl-lib}
    ${glesv3-lib}
)

# Only link Vulkan if found — linking missing lib causes runtime SIGSEGV
if(vulkan-lib)
    target_link_libraries(vulkan_native ${vulkan-lib})
endif()

# Set include directories
target_include_directories(
    vulkan_native
    PRIVATE
    ${ANDROID_NDK}/sources/android/native_app_glue
)

# Apply per-target optimizations
target_compile_options(
    vulkan_native
    PRIVATE
    $<$<CONFIG:RELEASE>:-O3 -ffast-math -funroll-loops>
    $<$<CONFIG:DEBUG>:-O2>  # Even debug gets some optimization
)
