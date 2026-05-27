/**
 * ram_benchmark.c — Native RAM bandwidth / latency tests via JNI.
 *
 * Why native?
 *   • JVM bounds-checks + GC prevent SIMD vectorisation → only ~500-800 MB/s
 *     for byte-by-byte loops and ~3-7 GB/s even with LongArray word reads.
 *   • Native C + NEON lets the compiler issue LDP/STP pairs and prefetches,
 *     reaching 15-35 GB/s sequential BW — much closer to true LPDDR5X bandwidth.
 *   • Pointer-chase random access has no JVM call overhead (no safepoints between
 *     stores) → latency results are accurate.
 *
 * Compile flags inherited from CMakeLists.txt: -O3 -ffast-math -march=armv8-a
 * On x86/x86_64 the compiler auto-vectorises the plain C loops.
 */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <pthread.h>
#include <stdint.h>
#include <android/log.h>

#ifdef __ARM_NEON
#  include <arm_neon.h>
#endif

/* Detect L3 cache size on Linux/Android. Returns size in bytes. */
static size_t detect_l3_cache_size(void) {
    const char *paths[] = {
        "/sys/devices/system/cpu/cpu0/cache/index3/size",
        "/sys/devices/system/cpu/cpu0/cache/index4/size",
        "/sys/devices/system/cpu/cpu1/cache/index3/size",
        "/sys/devices/system/cpu/cpu4/cache/index3/size",
    };
    for (int p = 0; p < 4; p++) {
        FILE *f = fopen(paths[p], "r");
        if (f) {
            char buf[64];
            if (fgets(buf, sizeof(buf), f)) {
                fclose(f);
                char *end;
                double val = strtod(buf, &end);
                if (val > 0) {
                    if (*end == 'K' || *end == 'k') val *= 1024;
                    else if (*end == 'M' || *end == 'm') val *= 1024 * 1024;
                    else if (*end == 'G' || *end == 'g') val *= 1024 * 1024 * 1024;
                    return (size_t)val;
                }
            } else {
                fclose(f);
            }
        }
    }
    /* Fallback default: 12 MB (SD 8 Gen 3 baseline is 12MB) */
    return 12UL * 1024UL * 1024UL;
}

#define LOG_TAG  "RamBenchNative"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/* ── Timing ──────────────────────────────────────────────────────────────── */

/*
 * MUST be __attribute__((noinline)).
 * With -O3, Clang can legally hoist clock_gettime() out of while-loops whose
 * bodies (memcpy, NEON loads) are provably free of side-effects on the clock.
 * noinline forces a real call each iteration and defeats that optimisation.
 */
static __attribute__((noinline)) int64_t now_ns(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (int64_t)ts.tv_sec * 1000000000LL + (int64_t)ts.tv_nsec;
}

/* COMPILER_BARRIER: memory clobber prevents -O3 from hoisting clock reads */
#define COMPILER_BARRIER() do { __asm__ volatile("" ::: "memory"); } while (0)

/*
 * COMPILER_BARRIER: prevents the compiler from hoisting now_ns() calls out of
 * timing loops. The "memory" clobber tells the optimizer that any memory may
 * have been read or written, forcing re-evaluation of the loop condition.
 */

/* ── Sequential Read ─────────────────────────────────────────────────────── */
/*
 * Allocates 64 MB on the native heap, faults all pages in, then loops
 * reading 64 bytes per inner iteration (4× 16-byte NEON loads on arm64,
 * or 8× 8-byte uint64_t reads elsewhere).
 * __builtin_prefetch tells the hardware prefetcher to pull the next cache-line
 * before it is needed.
 */
JNIEXPORT jdouble JNICALL
Java_com_ivarna_finalbenchmark2_utils_RamNativeBridge_nativeSeqRead(
        JNIEnv *env, jclass cls, jlong durationMs)
{
    size_t l3_size = detect_l3_cache_size();
    size_t BUF = 64UL * 1024UL * 1024UL;  /* 64 MB */
    if (l3_size * 2 > BUF) {
        BUF = l3_size * 2;
    }
    uint8_t *buf = (uint8_t*)malloc(BUF);
    if (!buf) { LOGE("seqRead alloc failed"); return 0.0; }
    memset(buf, 0xA5, BUF);   /* fault all pages in before clock starts */

    const int64_t end_ns = now_ns() + (int64_t)durationMs * 1000000LL;
    int64_t total_bytes = 0;
    uint64_t sink = 0;

    while (now_ns() < end_ns) {
        COMPILER_BARRIER();
#ifdef __ARM_NEON
        uint64x2_t acc0 = vdupq_n_u64(0), acc1 = vdupq_n_u64(0);
        uint64x2_t acc2 = vdupq_n_u64(0), acc3 = vdupq_n_u64(0);
        const uint8_t *p   = buf;
        const uint8_t *end = buf + BUF;
        while (p < end) {
            __builtin_prefetch(p + 512, 0, 0);
            acc0 = vaddq_u64(acc0, vld1q_u64((const uint64_t*)(p +  0)));
            acc1 = vaddq_u64(acc1, vld1q_u64((const uint64_t*)(p + 16)));
            acc2 = vaddq_u64(acc2, vld1q_u64((const uint64_t*)(p + 32)));
            acc3 = vaddq_u64(acc3, vld1q_u64((const uint64_t*)(p + 48)));
            p += 64;
        }
        sink += vgetq_lane_u64(acc0, 0) + vgetq_lane_u64(acc1, 1) +
                vgetq_lane_u64(acc2, 0) + vgetq_lane_u64(acc3, 1);
#else
        volatile uint64_t s = 0;
        const uint64_t *p   = (const uint64_t*)buf;
        const uint64_t *end = (const uint64_t*)(buf + BUF);
        while (p < end) {
            s += p[0]; s += p[1]; s += p[2]; s += p[3];
            s += p[4]; s += p[5]; s += p[6]; s += p[7];
            p += 8;
        }
        sink += s;
#endif
        total_bytes += (int64_t)BUF;
        COMPILER_BARRIER();
    }

    free(buf);
    if (sink == 0xDEADBEEFDEADBEEFULL) return -1.0; /* anti-DCE */
    return (double)total_bytes / ((double)durationMs / 1000.0) / (1024.0 * 1024.0);
}

/* ── Sequential Write ────────────────────────────────────────────────────── */
/*
 * 64 MB writes, 64 bytes per inner iteration using NEON stores on arm64.
 * We vary the pattern each outer iteration to ensure the CPU cannot cache
 * or eliminate the stores.
 */
JNIEXPORT jdouble JNICALL
Java_com_ivarna_finalbenchmark2_utils_RamNativeBridge_nativeSeqWrite(
        JNIEnv *env, jclass cls, jlong durationMs)
{
    size_t l3_size = detect_l3_cache_size();
    size_t BUF = 64UL * 1024UL * 1024UL;
    if (l3_size * 2 > BUF) {
        BUF = l3_size * 2;
    }
    uint8_t *buf = (uint8_t*)malloc(BUF);
    if (!buf) { LOGE("seqWrite alloc failed"); return 0.0; }
    memset(buf, 0, BUF);   /* fault all pages in */

    const int64_t end_ns = now_ns() + (int64_t)durationMs * 1000000LL;
    int64_t total_bytes = 0;
    uint64_t pattern = 1;

    while (now_ns() < end_ns) {
        COMPILER_BARRIER();
        uint8_t *p   = buf;
        uint8_t *end = buf + BUF;
        uint64_t val_u64 = pattern++;
        while (p < end) {
            __builtin_prefetch(p + 512, 1, 0);
#ifdef __ARM_NEON
            uint64x2_t val0 = vdupq_n_u64(val_u64 + 0);
            uint64x2_t val1 = vdupq_n_u64(val_u64 + 1);
            uint64x2_t val2 = vdupq_n_u64(val_u64 + 2);
            uint64x2_t val3 = vdupq_n_u64(val_u64 + 3);
            vst1q_u64((uint64_t*)(p +  0), val0);
            vst1q_u64((uint64_t*)(p + 16), val1);
            vst1q_u64((uint64_t*)(p + 32), val2);
            vst1q_u64((uint64_t*)(p + 48), val3);
            val_u64 += 4;
#else
            ((uint64_t*)p)[0] = val_u64++; ((uint64_t*)p)[1] = val_u64++;
            ((uint64_t*)p)[2] = val_u64++; ((uint64_t*)p)[3] = val_u64++;
            ((uint64_t*)p)[4] = val_u64++; ((uint64_t*)p)[5] = val_u64++;
            ((uint64_t*)p)[6] = val_u64++; ((uint64_t*)p)[7] = val_u64++;
#endif
            p += 64;
        }
        pattern = val_u64;
        total_bytes += (int64_t)BUF;
        COMPILER_BARRIER();
    }

    free(buf);
    return (double)total_bytes / ((double)durationMs / 1000.0) / (1024.0 * 1024.0);
}

/* ── Random Access (pointer-chase) ──────────────────────────────────────── */
/*
 * Builds a pseudo-random permutation of a 16 MB int array and measures the
 * average time per random-index load.  16 MB exceeds all L1/L2 caches so
 * every access is a cache miss hitting L3 or DRAM.
 * Returns ns / operation.
 */
JNIEXPORT jdouble JNICALL
Java_com_ivarna_finalbenchmark2_utils_RamNativeBridge_nativeRandAccess(
        JNIEnv *env, jclass cls, jlong durationMs)
{
    size_t l3_size = detect_l3_cache_size();
    size_t target_rand_buf_size = 16UL * 1024UL * 1024UL; /* 16 MB */
    if (l3_size * 2 > target_rand_buf_size) {
        target_rand_buf_size = l3_size * 2;
    }
    const size_t COUNT = target_rand_buf_size / sizeof(int32_t);

    int32_t *perm = (int32_t*)malloc(COUNT * sizeof(int32_t));
    if (!perm) { LOGE("randAccess perm alloc failed"); return 999.0; }

    /* Build a random permutation using Knuth shuffle */
    for (size_t i = 0; i < COUNT; i++) perm[i] = (int32_t)i;
    /* Simple LCG for deterministic, fast shuffle (seed = 42) */
    uint64_t rng = 42ULL;
    for (size_t i = COUNT - 1; i > 0; i--) {
        rng = rng * 6364136223846793005ULL + 1442695040888963407ULL;
        size_t j = (rng >> 33) % (i + 1);
        int32_t tmp = perm[i]; perm[i] = perm[j]; perm[j] = tmp;
    }

    int32_t *chain = (int32_t*)malloc(COUNT * sizeof(int32_t));
    if (!chain) { free(perm); LOGE("randAccess chain alloc failed"); return 999.0; }

    /* Rewrite as a closed-cycle Hamiltonian pointer chain: chain[perm[i]] = perm[(i+1) % COUNT] */
    for (size_t i = 0; i < COUNT; i++) {
        chain[perm[i]] = perm[(i + 1) % COUNT];
    }
    free(perm);

    /* Touch all pages */
    volatile int32_t dummy = 0;
    for (size_t i = 0; i < COUNT; i += 256) dummy += chain[i];

    int32_t idx = 0;
    int64_t ops = 0;
    const int64_t t0     = now_ns();
    const int64_t end_ns = t0 + (int64_t)durationMs * 1000000LL;

    while (now_ns() < end_ns) {
        /* Unroll 8× to reduce loop overhead without hiding latency */
        idx = chain[idx]; idx = chain[idx]; idx = chain[idx]; idx = chain[idx];
        idx = chain[idx]; idx = chain[idx]; idx = chain[idx]; idx = chain[idx];
        ops += 8;
    }

    const int64_t elapsed_ns = now_ns() - t0;
    free(chain);
    if (idx < 0) return -1.0; /* anti-DCE */
    return ops == 0 ? 999.0 : (double)elapsed_ns / (double)ops;
}

/* ── Memory Copy ─────────────────────────────────────────────────────────── */
/*
 * Uses Bionic's libc memcpy which is hand-written NEON on arm64.
 * Returns MB/s.
 *
 * WHY __attribute__((noinline)) wrapper?
 * With -O3, Clang inlines memcpy(dst, src, CONSTANT) into a NEON inner loop.
 * Because dst is never read back (just freed), the compiler may prove the
 * writes are dead and eliminate the entire call — even across COMPILER_BARRIER.
 * A noinline wrapper defeats inlining, and the "r"(dst) asm input constraint
 * forces the compiler to treat the written buffer as observable, preventing
 * dead-store elimination.
 *
 * WHY fixed-repetition block timing (no outer while loop)?
 * The outer while(now_ns() < end_ns) loop, combined with an inlined no-op
 * memcpy, would spin for the full durationMs and accumulate a bogus count.
 * Measuring a fixed REPS block removes that failure mode entirely.
 */
__attribute__((noinline)) static void do_memcpy_once(
        void *dst, const void *src, size_t n)
{
    memcpy(dst, src, n);
    /* Force compiler to consider the destination buffer as "read" here,
     * preventing dead-store elimination of the entire copy. */
    __asm__ volatile("" :: "r"(dst) : "memory");
}

JNIEXPORT jdouble JNICALL
Java_com_ivarna_finalbenchmark2_utils_RamNativeBridge_nativeMemCopy(
        JNIEnv *env, jclass cls, jlong durationMs)
{
    size_t l3_size = detect_l3_cache_size();
    size_t BUF = 64UL * 1024UL * 1024UL;
    if (l3_size * 2 > BUF) {
        BUF = l3_size * 2;
    }
    uint8_t *src = (uint8_t*)malloc(BUF);
    uint8_t *dst = (uint8_t*)malloc(BUF);
    if (!src || !dst) { free(src); free(dst); return 0.0; }
    memset(src, 0xDE, BUF);
    memset(dst, 0,    BUF);

    /* Warm-up: fault all pages before the timed section */
    do_memcpy_once(dst, src, BUF);

    COMPILER_BARRIER();
    const int64_t t0 = now_ns();
    const int64_t end_ns = t0 + (int64_t)durationMs * 1000000LL;
    int64_t total_bytes = 0;

    while (now_ns() < end_ns) {
        COMPILER_BARRIER();
        do_memcpy_once(dst, src, BUF);
        total_bytes += (int64_t)BUF;
        COMPILER_BARRIER();
    }
    const int64_t elapsed_ns = now_ns() - t0;
    COMPILER_BARRIER();

    free(src); free(dst);
    if (elapsed_ns <= 0) return 0.0;

    return (double)total_bytes / ((double)elapsed_ns / 1.0e9) / (1024.0 * 1024.0);
}

/* ── Multi-threaded Bandwidth ─────────────────────────────────────────────── */

typedef struct {
    size_t   buf_size;
    int64_t  end_ns;
    int64_t  bytes_done;
    int      thread_id;
} MtArg;

static void* mt_thread(void *arg) {
    MtArg *a = (MtArg*)arg;
    uint8_t *buf = (uint8_t*)malloc(a->buf_size);
    if (!buf) { a->bytes_done = 0; return NULL; }
    memset(buf, (int)(0xA0 + a->thread_id), a->buf_size);  /* fault pages */

    int64_t total = 0;
    uint64_t sink = 0;

#ifdef __ARM_NEON
    uint64x2_t acc0 = vdupq_n_u64(0), acc1 = vdupq_n_u64(0);
    while (now_ns() < a->end_ns) {
        COMPILER_BARRIER();
        const uint8_t *p   = buf;
        const uint8_t *end = buf + a->buf_size;
        while (p < end) {
            __builtin_prefetch(p + 512, 0, 0);
            acc0 = vaddq_u64(acc0, vld1q_u64((const uint64_t*)(p +  0)));
            acc1 = vaddq_u64(acc1, vld1q_u64((const uint64_t*)(p + 16)));
            acc0 = vaddq_u64(acc0, vld1q_u64((const uint64_t*)(p + 32)));
            acc1 = vaddq_u64(acc1, vld1q_u64((const uint64_t*)(p + 48)));
            p += 64;
        }
        total += (int64_t)a->buf_size;
        COMPILER_BARRIER();
    }
    sink = vgetq_lane_u64(acc0, 0) + vgetq_lane_u64(acc1, 1);
#else
    while (now_ns() < a->end_ns) {
        COMPILER_BARRIER();
        volatile uint64_t s = 0;
        const uint64_t *p   = (const uint64_t*)buf;
        const uint64_t *end = (const uint64_t*)(buf + a->buf_size);
        while (p < end) { s += p[0]+p[1]+p[2]+p[3]+p[4]+p[5]+p[6]+p[7]; p += 8; }
        sink += s;
        total += (int64_t)a->buf_size;
        COMPILER_BARRIER();
    }
#endif

    free(buf);
    a->bytes_done = total;
    if (sink == 0xDEADBEEFDEADBEEFULL) a->bytes_done = -1; /* anti-DCE */
    return NULL;
}

JNIEXPORT jdouble JNICALL
Java_com_ivarna_finalbenchmark2_utils_RamNativeBridge_nativeMultiThread(
        JNIEnv *env, jclass cls, jint numThreads, jlong durationMs)
{
    const int   T       = (numThreads < 1 || numThreads > 64) ? 4 : (int)numThreads;
    size_t l3_size = detect_l3_cache_size();
    size_t BUF_T  = 16UL * 1024UL * 1024UL;  /* 16 MB per thread */
    size_t min_aggregate = l3_size * 2;
    if (BUF_T * T < min_aggregate) {
        BUF_T = min_aggregate / T;
        BUF_T = (BUF_T + 63) & ~63UL;
    }

    MtArg         args[64];
    pthread_t     tids[64];
    const int64_t end_ns = now_ns() + (int64_t)durationMs * 1000000LL;

    for (int i = 0; i < T; i++) {
        args[i].buf_size  = BUF_T;
        args[i].end_ns    = end_ns;
        args[i].bytes_done = 0;
        args[i].thread_id = i;
        pthread_create(&tids[i], NULL, mt_thread, &args[i]);
    }
    for (int i = 0; i < T; i++) pthread_join(tids[i], NULL);

    int64_t total_bytes = 0;
    for (int i = 0; i < T; i++) total_bytes += args[i].bytes_done;

    return (double)total_bytes / ((double)durationMs / 1000.0) / (1024.0 * 1024.0);
}
