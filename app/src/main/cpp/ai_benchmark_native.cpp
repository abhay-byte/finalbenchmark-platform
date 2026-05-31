/**
 * ai_benchmark_native.cpp — AI compute benchmarks (library-free, F-Droid safe).
 *
 * Chain: OpenCL (shared ctx) → OpenGL ES 3.1 → NEON CPU
 *
 * THREAD SAFETY FIX: eglMakeCurrent() is called at the start of EVERY
 * gles_matmul() call — EGL contexts are thread-local, so the coroutine
 * worker thread must bind before dispatching compute shaders.
 *
 * TIMING: Adaptive — calibrates 1 iter, scales to fill TARGET_MS.
 *         Minimum floor of MIN_TOTAL_MS to prevent 0ms results.
 */
#include <jni.h>
#include <android/log.h>
#include <chrono>
#include <cstring>
#include <cmath>
#include <vector>
#include <dlfcn.h>
#include <arm_neon.h>
#include <EGL/egl.h>
#include <GLES3/gl31.h>

#include "ocl_shared.h"
#include "ai_vulkan_matmul.h"

#define TAG "AI_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Each test runs until at least TARGET_MS wall-clock ms (≈10s × 9 tests = 90s).
// MIN_TOTAL_MS ensures the timed loop runs long enough to get a clean reading.
// Increased from 3s to 10s for GPU power stabilization and thermal equilibrium.
static const double TARGET_MS    = 10000.0;
static const double MIN_TOTAL_MS = 2000.0;   // hard floor — never report <2s run
static const int    MAX_ITERS    = 1000;      // cap iterations to prevent queue flooding

enum Backend { B_VK=0, B_OCL, B_GLES, B_CPU, B_NONE };
struct BenchResult { double ms, tps; bool ok; Backend b; };
static Backend g_activeBackend = B_NONE;

using hrc = std::chrono::high_resolution_clock;

// ─────────────────────────────────────────────────────────────────────────────
// safe_tps — guards against /0, Inf, NaN before returning to Kotlin/JNI
// ─────────────────────────────────────────────────────────────────────────────
static double safe_tps(int N, double total_ms, int iters) {
    if (total_ms < 1.0 || iters <= 0) return 0.0;
    double avg_ms = total_ms / iters;
    double ops    = 2.0 * (double)N * (double)N * (double)N;
    double t      = ops / (avg_ms / 1000.0);
    if (!std::isfinite(t) || t > 1e15) return 0.0;
    return t;
}

// adaptive_iters — scale to fill TARGET_MS; enforce minimum iters so total ≥ MIN_TOTAL_MS
static int adaptive_iters(double calib_ms) {
    if (calib_ms < 0.001) calib_ms = 0.001;          // clamp sub-µs readings
    int n = (int)(TARGET_MS  / calib_ms) + 1;
    int m = (int)(MIN_TOTAL_MS / calib_ms) + 1;
    if (n < m)          n = m;                               // enforce floor
    if (n < 10)         n = 10;                              // absolute minimum 10 iters
    if (n > MAX_ITERS)  n = MAX_ITERS;                       // P1 FIX: cap to MAX_ITERS (was hardcoded 5000)
    return n;
}

// ─────────────────────────────────────────────────────────────────────────────
// 0. Vulkan Compute — highest priority GPU backend (implemented in ai_vulkan_matmul.cpp)
//    Supports: Adreno (Snapdragon), Mali (MediaTek/Exynos), Xclipse (AMD RDNA), PowerVR
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// 1. OpenCL — reuses shared context from opencl_benchmark.cpp
// ─────────────────────────────────────────────────────────────────────────────
typedef void*    ocl_prog_t;
typedef void*    ocl_kern_t;
typedef void*    ocl_mem_t;
typedef int32_t  cl_int_ai;
typedef void*    cl_ctx_ai;
typedef void*    cl_queue_ai;
typedef void*    cl_dev_ai;
#define CL_MEM_RO  (1<<2)
#define CL_MEM_RW  (1<<0)
#define CL_OK      0

typedef ocl_prog_t (*pfn_CPS) (cl_ctx_ai,uint32_t,const char**,const size_t*,cl_int_ai*);
typedef cl_int_ai  (*pfn_BP)  (ocl_prog_t,uint32_t,const cl_dev_ai*,const char*,void(*)(ocl_prog_t,void*),void*);
typedef ocl_kern_t (*pfn_CK)  (ocl_prog_t,const char*,cl_int_ai*);
typedef cl_int_ai  (*pfn_SKA) (ocl_kern_t,uint32_t,size_t,const void*);
typedef ocl_mem_t  (*pfn_CB)  (cl_ctx_ai,uint64_t,size_t,void*,cl_int_ai*);
typedef cl_int_ai  (*pfn_EWB) (cl_queue_ai,ocl_mem_t,uint32_t,size_t,size_t,const void*,uint32_t,void*,void*);
typedef cl_int_ai  (*pfn_ENDRK)(cl_queue_ai,ocl_kern_t,uint32_t,const size_t*,const size_t*,const size_t*,uint32_t,void*,void*);
typedef cl_int_ai  (*pfn_FIN) (cl_queue_ai);
typedef cl_int_ai  (*pfn_RMO) (ocl_mem_t);
typedef cl_int_ai  (*pfn_RK)  (ocl_kern_t);
typedef cl_int_ai  (*pfn_RP)  (ocl_prog_t);

static const char* OCL_GEMM = R"CL(
__kernel void ai_gemm(__global const float* A,__global const float* B,__global float* C,int N){
    int r=get_global_id(1),c=get_global_id(0);
    if(r>=N||c>=N)return;
    float s=0.f;for(int k=0;k<N;k++)s+=A[r*N+k]*B[k*N+c];
    C[r*N+c]=s;
})CL";

static BenchResult ocl_matmul(int N) {
    if (!ocl_shared_available()) return {0,0,false,B_OCL};
    void*       lib = ocl_shared_lib();
    cl_ctx_ai   ctx = (cl_ctx_ai)  ocl_shared_ctx();
    cl_queue_ai q   = (cl_queue_ai)ocl_shared_queue();
    cl_dev_ai   dev = (cl_dev_ai)  ocl_shared_dev();
    if (!lib||!ctx||!q||!dev) return {0,0,false,B_OCL};

    auto CPS   = (pfn_CPS)  dlsym(lib,"clCreateProgramWithSource");
    auto BP    = (pfn_BP)   dlsym(lib,"clBuildProgram");
    auto CK    = (pfn_CK)   dlsym(lib,"clCreateKernel");
    auto SKA   = (pfn_SKA)  dlsym(lib,"clSetKernelArg");
    auto CB    = (pfn_CB)   dlsym(lib,"clCreateBuffer");
    auto EWB   = (pfn_EWB)  dlsym(lib,"clEnqueueWriteBuffer");
    auto ENDRK = (pfn_ENDRK)dlsym(lib,"clEnqueueNDRangeKernel");
    auto FIN   = (pfn_FIN)  dlsym(lib,"clFinish");
    auto RMO   = (pfn_RMO)  dlsym(lib,"clReleaseMemObject");
    auto RK    = (pfn_RK)   dlsym(lib,"clReleaseKernel");
    auto RP    = (pfn_RP)   dlsym(lib,"clReleaseProgram");
    if (!CPS||!BP||!CK||!SKA||!CB||!EWB||!ENDRK||!FIN) return {0,0,false,B_OCL};

    size_t sz = (size_t)N*N*sizeof(float);
    cl_int_ai err = 0;
    ocl_mem_t bA=CB(ctx,CL_MEM_RO,sz,nullptr,&err);
    ocl_mem_t bB=CB(ctx,CL_MEM_RO,sz,nullptr,&err);
    ocl_mem_t bC=CB(ctx,CL_MEM_RW,sz,nullptr,&err);
    if (!bA||!bB||!bC) {
        if(bA)RMO(bA); if(bB)RMO(bB); if(bC)RMO(bC);
        return {0,0,false,B_OCL};
    }
    std::vector<float> tmp((size_t)N*N, 0.5f);
    EWB(q,bA,1,0,sz,tmp.data(),0,nullptr,nullptr);
    EWB(q,bB,1,0,sz,tmp.data(),0,nullptr,nullptr);

    ocl_prog_t prog = CPS(ctx,1,&OCL_GEMM,nullptr,&err);
    if (err!=CL_OK) { RMO(bA);RMO(bB);RMO(bC); return {0,0,false,B_OCL}; }
    if (BP(prog,1,&dev,"-cl-fast-relaxed-math",nullptr,nullptr)!=CL_OK) {
        RP(prog);RMO(bA);RMO(bB);RMO(bC); return {0,0,false,B_OCL};
    }
    ocl_kern_t kern = CK(prog,"ai_gemm",&err);
    if (err!=CL_OK) { RP(prog);RMO(bA);RMO(bB);RMO(bC); return {0,0,false,B_OCL}; }

    SKA(kern,0,sizeof(ocl_mem_t),&bA);
    SKA(kern,1,sizeof(ocl_mem_t),&bB);
    SKA(kern,2,sizeof(ocl_mem_t),&bC);
    SKA(kern,3,sizeof(int),&N);
    size_t gws[2] = {(size_t)N,(size_t)N};

    // Warmup
    for (int w=0;w<3;w++) { ENDRK(q,kern,2,nullptr,gws,nullptr,0,nullptr,nullptr); FIN(q); }

    // Calibrate
    auto c0 = hrc::now();
    ENDRK(q,kern,2,nullptr,gws,nullptr,0,nullptr,nullptr); FIN(q);
    double calib = std::chrono::duration<double,std::milli>(hrc::now()-c0).count();
    int iters = adaptive_iters(calib);

    // Timed loop — P3 FIX: add clFinish per iteration to prevent queue flooding
    auto t0 = hrc::now();
    for (int i=0;i<iters;i++) {
        ENDRK(q,kern,2,nullptr,gws,nullptr,0,nullptr,nullptr);
        FIN(q);  // Force completion per iteration for accurate timing
    }
    double total_ms = std::chrono::duration<double,std::milli>(hrc::now()-t0).count();
    double tps = safe_tps(N, total_ms, iters);
    LOGI("OCL %dx%d: %.2fms avg (x%d, total=%.0fms) %.1f GFLOPS",
         N,N, total_ms/iters, iters, total_ms, tps/1e9);

    RK(kern); RP(prog); RMO(bA); RMO(bB); RMO(bC);
    if (tps <= 0) return {0,0,false,B_OCL};
    return {total_ms/iters, tps, true, B_OCL};
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. OpenGL ES 3.1 — EGL context re-bound on EVERY call (thread-safe fix)
// ─────────────────────────────────────────────────────────────────────────────
static EGLDisplay g_eDpy   = EGL_NO_DISPLAY;
static EGLContext g_eCtx   = EGL_NO_CONTEXT;
static EGLSurface g_eSurf  = EGL_NO_SURFACE;
static EGLConfig  g_eCfg   = nullptr;

static const char* GLES_CS = R"(#version 310 es
layout(local_size_x=16,local_size_y=16)in;
layout(binding=0)buffer A{float a[];};
layout(binding=1)buffer B{float b[];};
layout(binding=2)buffer C{float c[];};
uniform int N;
void main(){
    uint r=gl_GlobalInvocationID.y,col=gl_GlobalInvocationID.x;
    if(r>=uint(N)||col>=uint(N))return;
    float s=0.;for(int k=0;k<N;k++)s+=a[r*uint(N)+uint(k)]*b[uint(k)*uint(N)+col];
    c[r*uint(N)+col]=s;
})";

static bool gles_init() {
    if (g_eDpy != EGL_NO_DISPLAY) return true;
    g_eDpy = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (g_eDpy == EGL_NO_DISPLAY) return false;
    EGLint mj,mn;
    if (!eglInitialize(g_eDpy,&mj,&mn)) { g_eDpy=EGL_NO_DISPLAY; return false; }
    const EGLint ca[] = {EGL_RENDERABLE_TYPE,EGL_OPENGL_ES3_BIT,
                         EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_NONE};
    EGLint n;
    if (!eglChooseConfig(g_eDpy,ca,&g_eCfg,1,&n)||!n) {
        eglTerminate(g_eDpy); g_eDpy=EGL_NO_DISPLAY; return false;
    }
    const EGLint cta[] = {EGL_CONTEXT_CLIENT_VERSION,3,EGL_NONE};
    g_eCtx = eglCreateContext(g_eDpy,g_eCfg,EGL_NO_CONTEXT,cta);
    if (g_eCtx == EGL_NO_CONTEXT) { eglTerminate(g_eDpy); g_eDpy=EGL_NO_DISPLAY; return false; }
    const EGLint sa[] = {EGL_WIDTH,1,EGL_HEIGHT,1,EGL_NONE};
    g_eSurf = eglCreatePbufferSurface(g_eDpy,g_eCfg,sa);
    // Do NOT call eglMakeCurrent here — will be called per-thread in gles_matmul
    LOGI("GLES init OK (context created, not yet bound to thread)");
    return true;
}

static void gles_destroy() {
    if (g_eDpy == EGL_NO_DISPLAY) return;
    eglMakeCurrent(g_eDpy,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);
    if (g_eSurf) eglDestroySurface(g_eDpy,g_eSurf);
    if (g_eCtx)  eglDestroyContext(g_eDpy,g_eCtx);
    eglTerminate(g_eDpy);
    g_eDpy=EGL_NO_DISPLAY; g_eCtx=EGL_NO_CONTEXT; g_eSurf=EGL_NO_SURFACE;
}

static BenchResult gles_matmul(int N) {
    if (g_eDpy==EGL_NO_DISPLAY || g_eCtx==EGL_NO_CONTEXT) return {0,0,false,B_GLES};

    // ── CRITICAL FIX: bind EGL context to THIS thread before any GL call ──
    if (!eglMakeCurrent(g_eDpy, g_eSurf, g_eSurf, g_eCtx)) {
        LOGE("GLES: eglMakeCurrent failed on worker thread (err=0x%x)", eglGetError());
        return {0,0,false,B_GLES};
    }

    size_t sz = (size_t)N*N*sizeof(float);
    std::vector<float> A((size_t)N*N,0.5f), B_((size_t)N*N,0.3f), C_((size_t)N*N,0.f);
    GLuint ssbos[3]; glGenBuffers(3,ssbos);
    glBindBuffer(GL_SHADER_STORAGE_BUFFER,ssbos[0]);
    glBufferData(GL_SHADER_STORAGE_BUFFER,(GLsizeiptr)sz,A.data(),GL_STATIC_DRAW);
    glBindBuffer(GL_SHADER_STORAGE_BUFFER,ssbos[1]);
    glBufferData(GL_SHADER_STORAGE_BUFFER,(GLsizeiptr)sz,B_.data(),GL_STATIC_DRAW);
    glBindBuffer(GL_SHADER_STORAGE_BUFFER,ssbos[2]);
    glBufferData(GL_SHADER_STORAGE_BUFFER,(GLsizeiptr)sz,C_.data(),GL_DYNAMIC_COPY);

    GLuint cs = glCreateShader(GL_COMPUTE_SHADER);
    glShaderSource(cs,1,&GLES_CS,nullptr); glCompileShader(cs);
    GLint ok=0; glGetShaderiv(cs,GL_COMPILE_STATUS,&ok);
    if (!ok) {
        GLchar buf[512]; glGetShaderInfoLog(cs,512,nullptr,buf);
        LOGE("GLES CS compile: %s",buf);
        glDeleteShader(cs); glDeleteBuffers(3,ssbos);
        eglMakeCurrent(g_eDpy,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);
        return {0,0,false,B_GLES};
    }
    GLuint prog = glCreateProgram();
    glAttachShader(prog,cs); glLinkProgram(prog);
    glUseProgram(prog);
    glUniform1i(glGetUniformLocation(prog,"N"),N);
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER,0,ssbos[0]);
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER,1,ssbos[1]);
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER,2,ssbos[2]);

    // Warmup
    for (int w=0;w<3;w++) {
        glDispatchCompute((N+15)/16,(N+15)/16,1);
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
    }
    glFinish();

    // Calibrate 1 iter
    auto c0 = hrc::now();
    glDispatchCompute((N+15)/16,(N+15)/16,1); glFinish();
    double calib = std::chrono::duration<double,std::milli>(hrc::now()-c0).count();
    int iters = adaptive_iters(calib);

    // Timed loop — FREEZE FIX: glFinish() per iteration to prevent GPU queue flooding.
    // Without this, up to MAX_ITERS=1000 dispatches queue before any GPU drain,
    // starving the render thread and causing >2 min UI freeze on small-N tests.
    // Same pattern as OCL fix (FIN(q) per iter).
    auto t0 = hrc::now();
    for (int i=0;i<iters;i++) {
        glDispatchCompute((N+15)/16,(N+15)/16,1);
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
        glFinish();  // FREEZE FIX: sync per-iter, prevent command queue flooding
    }
    double total_ms = std::chrono::duration<double,std::milli>(hrc::now()-t0).count();
    double tps = safe_tps(N, total_ms, iters);
    LOGI("GLES %dx%d: %.2fms avg (x%d, total=%.0fms) %.1f GFLOPS",
         N,N, total_ms/iters, iters, total_ms, tps/1e9);

    glDeleteProgram(prog); glDeleteShader(cs); glDeleteBuffers(3,ssbos);
    // Unbind context from thread — next call will rebind on whatever thread it's on
    eglMakeCurrent(g_eDpy,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);

    if (tps <= 0) return {0,0,false,B_GLES};
    return {total_ms/iters, tps, true, B_GLES};
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. NEON CPU GEMM — always succeeds
// ─────────────────────────────────────────────────────────────────────────────
static void neon_gemm(int N, const float* __restrict__ A,
                              const float* __restrict__ B,
                              float*       __restrict__ C) {
    for (int i=0; i<N; i+=4) for (int j=0; j<N; j+=4) {
        float32x4_t c0=vdupq_n_f32(0),c1=vdupq_n_f32(0),
                    c2=vdupq_n_f32(0),c3=vdupq_n_f32(0);
        for (int k=0; k<N; k++) {
            float32x4_t bk = vld1q_f32(&B[k*N+j]);
            c0 = vmlaq_n_f32(c0,bk,A[ i   *N+k]);
            c1 = vmlaq_n_f32(c1,bk,A[(i+1)*N+k]);
            c2 = vmlaq_n_f32(c2,bk,A[(i+2)*N+k]);
            c3 = vmlaq_n_f32(c3,bk,A[(i+3)*N+k]);
        }
        vst1q_f32(&C[ i   *N+j],c0); vst1q_f32(&C[(i+1)*N+j],c1);
        vst1q_f32(&C[(i+2)*N+j],c2); vst1q_f32(&C[(i+3)*N+j],c3);
    }
}

static BenchResult cpu_matmul(int N) {
    std::vector<float> A((size_t)N*N,0.5f), B_((size_t)N*N,0.3f), C_((size_t)N*N,0.f);

    // Warmup
    for (int w=0;w<3;w++) { std::fill(C_.begin(),C_.end(),0.f); neon_gemm(N,A.data(),B_.data(),C_.data()); }

    // Calibrate 1 iter
    std::fill(C_.begin(),C_.end(),0.f);
    auto c0 = hrc::now();
    neon_gemm(N,A.data(),B_.data(),C_.data());
    double calib = std::chrono::duration<double,std::milli>(hrc::now()-c0).count();
    int iters = adaptive_iters(calib);

    // Timed loop
    auto t0 = hrc::now();
    for (int i=0;i<iters;i++) { std::fill(C_.begin(),C_.end(),0.f); neon_gemm(N,A.data(),B_.data(),C_.data()); }
    double total_ms = std::chrono::duration<double,std::milli>(hrc::now()-t0).count();
    double tps = safe_tps(N, total_ms, iters);
    LOGI("CPU %dx%d: %.2fms avg (x%d, total=%.0fms) %.1f GFLOPS",
         N,N, total_ms/iters, iters, total_ms, tps/1e9);
    return {total_ms/iters, tps, true, B_CPU};
}

// ─────────────────────────────────────────────────────────────────────────────
// Fallback chain: OpenCL → GLES → CPU
// ─────────────────────────────────────────────────────────────────────────────
static BenchResult run_ai(int N) {
    BenchResult r;
    
    // 1. Try Vulkan first (highest priority — best GPU path on modern Android)
    //    Works on all major GPU families: Adreno, Mali, Xclipse, PowerVR
    if (vulkan_ai_available()) {
        VulkanBenchResult vr = vulkan_ai_matmul(N);
        if (vr.ok) {
            g_activeBackend = B_VK;
            BenchResult br;
            br.ms = vr.ms;
            br.tps = vr.tps;
            br.ok = true;
            br.b = B_VK;
            return br;
        }
        LOGW("Vulkan failed N=%d, trying OpenCL", N);
    }
    
    // 2. Try OpenCL (shared context or self-initialized)
    if (ocl_shared_available()) {
        r = ocl_matmul(N);
        if (r.ok) { g_activeBackend=B_OCL; return r; }
        LOGW("OCL failed N=%d, trying GLES", N);
    }
    
    // 3. Try GLES (eglMakeCurrent called per-call — thread-safe)
    if (g_eDpy != EGL_NO_DISPLAY) {
        r = gles_matmul(N);
        if (r.ok) { g_activeBackend=B_GLES; return r; }
        LOGW("GLES failed N=%d, falling back to CPU", N);
    }
    
    // 4. CPU NEON — always works
    g_activeBackend = B_CPU;
    return cpu_matmul(N);
}

// ─────────────────────────────────────────────────────────────────────────────
// JNI
// ─────────────────────────────────────────────────────────────────────────────
extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_ivarna_finalbenchmark2_aiBenchmark_AiBenchmarkNative_nativeInit(JNIEnv*,jobject) {
    bool hasOCL  = ocl_shared_available();
    // ALWAYS init GLES — OCL can succeed globally but fail per-call (different N, context lost, etc.).
    // If we only init GLES when !hasOCL, the GLES fallback path is unavailable when OCL
    // fails mid-benchmark, leading to CPU-only fallback instead of GPU path.
    bool hasGLES = gles_init();
    LOGI("Init: OCL=%d GLES=%d CPU=always", hasOCL?1:0, hasGLES?1:0);
    return JNI_TRUE;  // CPU is guaranteed — always true
}

JNIEXPORT void JNICALL
Java_com_ivarna_finalbenchmark2_aiBenchmark_AiBenchmarkNative_nativeDestroy(JNIEnv*,jobject) {
    // Unbind before destroy (may be called from a different thread)
    if (g_eDpy != EGL_NO_DISPLAY)
        eglMakeCurrent(g_eDpy,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);
    gles_destroy();
    vulkan_ai_destroy();
    g_activeBackend = B_NONE;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_ivarna_finalbenchmark2_aiBenchmark_AiBenchmarkNative_nativeRunBenchmark(
        JNIEnv* env, jobject, jint id, jint /*iters*/, jint /*warmup*/) {
    // Map ID → N (multiple of 16 for GLES 16×16 workgroup; multiple of 4 for NEON tile)
    // P3 FIX: Use realistic model sizes based on actual AI workloads
    int N = 256;
    switch (id) {
        case 0: N=224; break;   // MobileNet (224x224 input)
        case 1: N=304; break;   // SSD MobileNet (300x300, padded to 304)
        case 2: N=384; break;   // BERT (384 embedding dim)
        case 3: N=1024; break;  // Whisper ASR (1024 encoder dim)
        case 4: N=512;  break;  // LLM MatMul — 512 matches display name "512×512", avoids multi-second per-iter freeze
        case 5: N=640; break;   // YOLOv8 (640x640 input)
        case 6: N=768; break;   // BERT-base (768 hidden dim)
        case 7: N=512; break;   // Generic transformer (512 dim)
        default: {
            jdoubleArray r = env->NewDoubleArray(3);
            jdouble v[3] = {0,0,0};
            env->SetDoubleArrayRegion(r,0,3,v);
            return r;
        }
    }
    // Align to 16 (GLES) — most values already aligned, but ensure it
    N = ((N + 15) / 16) * 16;

    auto r = run_ai(N);

    // Final guard — never pass Inf/NaN to Kotlin
    double ms  = (std::isfinite(r.ms)  && r.ms  > 0) ? r.ms  : 0.0;
    double tps = (std::isfinite(r.tps) && r.tps > 0) ? r.tps : 0.0;
    bool   ok  = r.ok && tps > 0.0;

    jdoubleArray res = env->NewDoubleArray(3);
    jdouble v[3] = {ms, tps, ok ? 1.0 : 0.0};
    env->SetDoubleArrayRegion(res,0,3,v);
    return res;
}

JNIEXPORT jstring JNICALL
Java_com_ivarna_finalbenchmark2_aiBenchmark_AiBenchmarkNative_nativeGetMode(JNIEnv* env, jobject) {
    // Return friendly display names used directly in UI badge and share text.
    const char* m = "CPU";
    switch (g_activeBackend) {
        case B_VK:   m = "Vulkan";   break;
        case B_OCL:  m = "OpenCL";   break;
        case B_GLES: m = "OpenGL ES"; break;
        case B_CPU:  m = "CPU"; break;
        default:     m = "CPU"; break;  // B_NONE: not yet run, show CPU as safe default
    }
    return env->NewStringUTF(m);
}

} // extern "C"
