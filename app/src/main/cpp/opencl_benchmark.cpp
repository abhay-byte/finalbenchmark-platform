/**
 * opencl_benchmark.cpp — Phase 3: OpenCL compute benchmark via dlopen.
 *
 * No link-time dependency on libOpenCL.so — resolved at runtime via dlopen.
 * Falls back gracefully if OpenCL is not present on device.
 *
 * Scene 0: Memory copy bandwidth — 64 MB device-to-device copy × 20 iterations.
 * Scene 1: Julia fractal compute — 1920×1080 kernel, 128 iter, reports GFLOPS.
 */
#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>
#include <chrono>
#include <cstring>
#include <cstdint>
#include <string>

#define TAG "OpenCLBench"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// ─── OpenCL type aliases (avoid including CL headers) ──────────────────────
typedef void*     cl_platform_id;
typedef void*     cl_device_id;
typedef void*     cl_context;
typedef void*     cl_command_queue;
typedef void*     cl_program;
typedef void*     cl_kernel;
typedef void*     cl_mem;
typedef void*     cl_event;
typedef int32_t   cl_int;
typedef uint32_t  cl_uint;
typedef uint64_t  cl_ulong;
typedef size_t    cl_device_type;
#define CL_DEVICE_TYPE_GPU   (1 << 2)
#define CL_MEM_READ_WRITE    (1 << 0)
#define CL_SUCCESS           0
#define CL_PROFILING_COMMAND_START  0x1282
#define CL_PROFILING_COMMAND_END    0x1283
#define CL_QUEUE_PROFILING_ENABLE   (1 << 1)

// ─── Function pointer types (prefixed pfn_ to avoid name clash) ────────────
typedef cl_int (*pfn_GetPlatformIDs)(cl_uint, cl_platform_id*, cl_uint*);
typedef cl_int (*pfn_GetDeviceIDs)(cl_platform_id, cl_device_type, cl_uint, cl_device_id*, cl_uint*);
typedef cl_context (*pfn_CreateContext)(const cl_int*, cl_uint, const cl_device_id*, void(*)(const char*,const void*,size_t,void*), void*, cl_int*);
typedef cl_command_queue (*pfn_CreateCommandQueue)(cl_context, cl_device_id, cl_ulong, cl_int*);
typedef cl_mem (*pfn_CreateBuffer)(cl_context, cl_ulong, size_t, void*, cl_int*);
typedef cl_int (*pfn_EnqueueWriteBuffer)(cl_command_queue, cl_mem, cl_uint, size_t, size_t, const void*, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_EnqueueCopyBuffer)(cl_command_queue, cl_mem, cl_mem, size_t, size_t, size_t, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_GetEventProfilingInfo)(cl_event, cl_uint, size_t, void*, size_t*);
typedef cl_int (*pfn_ReleaseEvent)(cl_event);
typedef cl_int (*pfn_Finish)(cl_command_queue);
typedef cl_program (*pfn_CreateProgramWithSource)(cl_context, cl_uint, const char**, const size_t*, cl_int*);
typedef cl_int (*pfn_BuildProgram)(cl_program, cl_uint, const cl_device_id*, const char*, void(*)(cl_program, void*), void*);
typedef cl_kernel (*pfn_CreateKernel)(cl_program, const char*, cl_int*);
typedef cl_int (*pfn_SetKernelArg)(cl_kernel, cl_uint, size_t, const void*);
typedef cl_int (*pfn_EnqueueNDRangeKernel)(cl_command_queue, cl_kernel, cl_uint, const size_t*, const size_t*, const size_t*, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_ReleaseMemObject)(cl_mem);
typedef cl_int (*pfn_ReleaseKernel)(cl_kernel);
typedef cl_int (*pfn_ReleaseProgram)(cl_program);
typedef cl_int (*pfn_ReleaseCommandQueue)(cl_command_queue);
typedef cl_int (*pfn_ReleaseContext)(cl_context);

#define CL_SYM(name) ((pfn_##name)dlsym(g_clLib, "cl"#name))

// ─── Julia CL kernel ────────────────────────────────────────────────────────
static const char* JULIA_KERNEL_SRC = R"CL(
__kernel void julia(__global float* out, int W, int H, float cx, float cy, int maxIter) {
    int px = get_global_id(0), py = get_global_id(1);
    if (px >= W || py >= H) return;
    float zr = (float)px / W * 3.5f - 1.75f;
    float zi = (float)py / H * 2.0f - 1.0f;
    int i = 0;
    for (; i < maxIter; i++) {
        float r2 = zr*zr - zi*zi + cx;
        float i2 = 2.0f*zr*zi + cy;
        zr = r2; zi = i2;
        if (zr*zr + zi*zi > 4.0f) break;
    }
    out[py * W + px] = (float)i / maxIter;
}
)CL";

// ─── State ──────────────────────────────────────────────────────────────────
static void*            g_clLib = nullptr;
static cl_context       g_ctx   = nullptr;
static cl_command_queue g_queue = nullptr;
static cl_device_id     g_dev   = nullptr;
static bool             g_clInit = false;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_ivarna_finalbenchmark2_utils_OpenCLBenchmarkBridge_nativeInit(JNIEnv*, jobject) {
    if (g_clInit) return JNI_TRUE;
    g_clLib = dlopen("libOpenCL.so", RTLD_LAZY);
    if (!g_clLib) { LOGI("libOpenCL.so not found: %s", dlerror()); return JNI_FALSE; }

    auto GetPlatformIDs    = CL_SYM(GetPlatformIDs);
    auto GetDeviceIDs      = CL_SYM(GetDeviceIDs);
    auto CreateContext     = CL_SYM(CreateContext);
    auto CreateCommandQueue = CL_SYM(CreateCommandQueue);
    if (!GetPlatformIDs || !GetDeviceIDs || !CreateContext || !CreateCommandQueue) {
        LOGE("Required CL symbols missing"); return JNI_FALSE;
    }

    cl_platform_id platform = nullptr;
    cl_uint nPlatforms = 0;
    if (GetPlatformIDs(1, &platform, &nPlatforms) != CL_SUCCESS || nPlatforms == 0) {
        LOGE("No OpenCL platforms"); return JNI_FALSE;
    }
    cl_uint nDevs = 0;
    if (GetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, 1, &g_dev, &nDevs) != CL_SUCCESS || nDevs == 0) {
        LOGE("No OpenCL GPU devices"); return JNI_FALSE;
    }
    cl_int err = 0;
    g_ctx = CreateContext(nullptr, 1, &g_dev, nullptr, nullptr, &err);
    if (!g_ctx || err != CL_SUCCESS) { LOGE("CreateContext failed: %d", err); return JNI_FALSE; }
    g_queue = CreateCommandQueue(g_ctx, g_dev, (cl_ulong)CL_QUEUE_PROFILING_ENABLE, &err);
    if (!g_queue || err != CL_SUCCESS) { LOGE("CreateCommandQueue failed: %d", err); return JNI_FALSE; }
    g_clInit = true;
    LOGI("OpenCL initialized OK");
    return JNI_TRUE;
}

JNIEXPORT jfloat JNICALL
Java_com_ivarna_finalbenchmark2_utils_OpenCLBenchmarkBridge_nativeRunScene(JNIEnv*, jobject, jint sceneId) {
    if (!g_clInit || !g_clLib) return -1.0f;

    auto CreateBuffer          = CL_SYM(CreateBuffer);
    auto EnqueueCopyBuffer     = CL_SYM(EnqueueCopyBuffer);
    auto Finish                = CL_SYM(Finish);
    auto ReleaseMemObject      = CL_SYM(ReleaseMemObject);
    auto ReleaseEvent          = CL_SYM(ReleaseEvent);
    auto CreateProgramWithSource = CL_SYM(CreateProgramWithSource);
    auto BuildProgram          = CL_SYM(BuildProgram);
    auto CreateKernel          = CL_SYM(CreateKernel);
    auto SetKernelArg          = CL_SYM(SetKernelArg);
    auto EnqueueNDRangeKernel  = CL_SYM(EnqueueNDRangeKernel);
    auto ReleaseKernel         = CL_SYM(ReleaseKernel);
    auto ReleaseProgram        = CL_SYM(ReleaseProgram);

    if (!CreateBuffer || !EnqueueCopyBuffer || !Finish || !ReleaseMemObject) return -1.0f;

    if (sceneId == 0) {
        // ─── Scene 0: Memory bandwidth ────────────────────────────────────
        const size_t BUF_BYTES = 64 * 1024 * 1024; // 64 MB
        cl_int err = 0;
        cl_mem src = CreateBuffer(g_ctx, CL_MEM_READ_WRITE, BUF_BYTES, nullptr, &err);
        cl_mem dst = CreateBuffer(g_ctx, CL_MEM_READ_WRITE, BUF_BYTES, nullptr, &err);
        if (!src || !dst) { LOGE("CreateBuffer failed"); return -1.0f; }
        // Warm-up
        cl_event ev = nullptr;
        EnqueueCopyBuffer(g_queue, src, dst, 0, 0, BUF_BYTES, 0, nullptr, &ev);
        Finish(g_queue);
        if (ev && ReleaseEvent) ReleaseEvent(ev);
        // Timed
        const int ITER = 20;
        auto t0 = std::chrono::high_resolution_clock::now();
        for (int i = 0; i < ITER; i++) {
            EnqueueCopyBuffer(g_queue, src, dst, 0, 0, BUF_BYTES, 0, nullptr, nullptr);
        }
        Finish(g_queue);
        auto t1 = std::chrono::high_resolution_clock::now();
        ReleaseMemObject(src); ReleaseMemObject(dst);
        double sec = std::chrono::duration<double>(t1 - t0).count();
        double gbs = (double)BUF_BYTES * ITER / sec / 1e9;
        LOGI("OpenCL MemBW: %.2f GB/s", gbs);
        return (float)gbs;
    } else {
        // ─── Scene 1: Julia fractal compute ──────────────────────────────
        if (!CreateProgramWithSource || !BuildProgram || !CreateKernel || !SetKernelArg ||
            !EnqueueNDRangeKernel || !ReleaseKernel || !ReleaseProgram) return -1.0f;
        const int W = 1920, H = 1080, MAX_ITER = 128;
        cl_int err = 0;
        cl_mem outBuf = CreateBuffer(g_ctx, CL_MEM_READ_WRITE, (size_t)W * H * sizeof(float), nullptr, &err);
        if (!outBuf) { LOGE("Julia CreateBuffer failed"); return -1.0f; }
        cl_program prog = CreateProgramWithSource(g_ctx, 1, &JULIA_KERNEL_SRC, nullptr, &err);
        if (err != CL_SUCCESS) { ReleaseMemObject(outBuf); return -1.0f; }
        BuildProgram(prog, 1, &g_dev, "", nullptr, nullptr);
        cl_kernel kern = CreateKernel(prog, "julia", &err);
        if (err != CL_SUCCESS) { ReleaseProgram(prog); ReleaseMemObject(outBuf); return -1.0f; }
        float cx = -0.7f, cy = 0.27015f;
        SetKernelArg(kern, 0, sizeof(cl_mem), &outBuf);
        SetKernelArg(kern, 1, sizeof(int), &W);
        SetKernelArg(kern, 2, sizeof(int), &H);
        SetKernelArg(kern, 3, sizeof(float), &cx);
        SetKernelArg(kern, 4, sizeof(float), &cy);
        SetKernelArg(kern, 5, sizeof(int), &MAX_ITER);
        size_t gws[2] = {(size_t)W, (size_t)H};
        size_t lws[2] = {8, 8};
        const int ITER = 5;
        auto t0 = std::chrono::high_resolution_clock::now();
        for (int i = 0; i < ITER; i++)
            EnqueueNDRangeKernel(g_queue, kern, 2, nullptr, gws, lws, 0, nullptr, nullptr);
        Finish(g_queue);
        auto t1 = std::chrono::high_resolution_clock::now();
        ReleaseKernel(kern); ReleaseProgram(prog); ReleaseMemObject(outBuf);
        double ms = std::chrono::duration<double, std::milli>(t1 - t0).count() / ITER;
        float fps = (float)(1000.0 / ms);
        LOGI("OpenCL Julia: %.2f ms → %.1f fps", ms, fps);
        return fps;
    }
}

JNIEXPORT void JNICALL
Java_com_ivarna_finalbenchmark2_utils_OpenCLBenchmarkBridge_nativeDestroy(JNIEnv*, jobject) {
    if (!g_clLib) return;
    if (g_queue) { auto f = (pfn_ReleaseCommandQueue)dlsym(g_clLib, "clReleaseCommandQueue"); if (f) f(g_queue); g_queue = nullptr; }
    if (g_ctx)   { auto f = (pfn_ReleaseContext)dlsym(g_clLib, "clReleaseContext");           if (f) f(g_ctx);   g_ctx   = nullptr; }
    dlclose(g_clLib); g_clLib = nullptr; g_clInit = false;
    LOGI("OpenCL destroyed");
}

} // extern "C"
