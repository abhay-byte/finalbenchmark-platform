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

# Architecture-specific optimizations
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8-a+crypto")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mtune=cortex-a76")
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv7-a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mfpu=neon")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mfloat-abi=softfp")
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=x86-64")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mtune=generic")
elseif(ANDROID_ABI STREQUAL "x86")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=i686")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mtune=generic")
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
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

# ============================================
# BUILD YOUR LIBRARY
# ============================================

# Add library
add_library(
    vulkan_native
    SHARED
    vulkan_info.cpp
    cpu_info.cpp  # Added CPU info native implementation
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

# Check if Vulkan library exists
if(NOT vulkan-lib)
    message(WARNING "Vulkan library not found. This is expected on some Android devices.")
endif()

# Link libraries to the native library
target_link_libraries(
    vulkan_native
    ${log-lib}
    ${android-lib}
    ${vulkan-lib}
)

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
