/**
 * ocl_shared.h — shared OpenCL context for all benchmark modules.
 *
 * opencl_benchmark.cpp OWNS the context (defines OCL_SHARED_IMPL before including).
 * ai_benchmark_native.cpp and any other module READS via the extern declarations.
 *
 * This prevents the double-dlopen / double-context crash: only one libOpenCL.so
 * handle exists per process lifetime.
 */
#pragma once
#include <stdint.h>
#include <stdbool.h>

// ── Minimal CL type aliases (no actual OpenCL header needed) ─────────────────
typedef void* ocl_ctx_t;        // cl_context
typedef void* ocl_queue_t;      // cl_command_queue
typedef void* ocl_dev_t;        // cl_device_id
typedef void* ocl_lib_t;        // dlopen handle

#ifdef OCL_SHARED_IMPL
// opencl_benchmark.cpp defines these as its own statics and exposes accessors.
// Other TUs get the extern accessors below.
#else
// For consumer TUs: declare the accessor functions.
#ifdef __cplusplus
extern "C" {
#endif

// Returns true if OpenCL was successfully initialised by opencl_benchmark.cpp
bool ocl_shared_available(void);
ocl_lib_t  ocl_shared_lib(void);
ocl_ctx_t  ocl_shared_ctx(void);
ocl_queue_t ocl_shared_queue(void);
ocl_dev_t  ocl_shared_dev(void);

#ifdef __cplusplus
}
#endif
#endif // OCL_SHARED_IMPL
