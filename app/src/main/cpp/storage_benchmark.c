/**
 * storage_benchmark.c — Native UFS/storage I/O benchmarks via JNI.
 *
 * Why native?
 *   • Java FileInputStream/FileOutputStream add per-call syscall overhead and
 *     cannot call posix_fadvise(), so reads always hit the Linux page cache
 *     and report ~6 GB/s (RAM speed) instead of real UFS speed (~4 GB/s).
 *   • fdatasync() per pass gives true sustained write speed (UFS hardware flush).
 *
 * All functions are JNI-callable from StorageNativeBridge.kt.
 */

#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <time.h>
#include <sys/stat.h>
#include <android/log.h>

#define LOG_TAG  "StorageBenchNative"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/* ── Timing ─────────────────────────────────────────────────────────────── */

static int64_t now_ms(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (int64_t)ts.tv_sec * 1000LL + (int64_t)ts.tv_nsec / 1000000LL;
}

/* ── Sequential Read ─────────────────────────────────────────────────────
 *
 * Protocol:
 *  1. Pre-create the test file ONCE (outside the timed loop) with non-zero data.
 *  2. Before each read pass call posix_fadvise(POSIX_FADV_DONTNEED) to evict
 *     all file pages from the Linux page cache.
 *  3. Time repeated sequential reads until durationMs elapses.
 *
 * This gives true UFS 4.0 sequential read speed (~3500–4200 MB/s), not the
 * ~6 GB/s page-cache speed that Java benchmarks report.
 *
 * Parameters:
 *   jpath       — absolute path for the temp test file in app cacheDir
 *   fileSizeBytes — size of the test file (64 MB recommended)
 *   chunkSize   — read buffer size in bytes (1 MB recommended)
 *   durationMs  — measurement window in ms (3000 = 3 s)
 *
 * Returns MB/s.
 */
JNIEXPORT jdouble JNICALL
Java_com_ivarna_finalbenchmark2_utils_StorageNativeBridge_nativeStorageSeqRead(
        JNIEnv *env, jclass cls,
        jstring jpath, jlong fileSizeBytes, jint chunkSize, jlong durationMs)
{
    const char *path = (*env)->GetStringUTFChars(env, jpath, NULL);
    if (!path) return 0.0;

    uint8_t *buf = (uint8_t *)malloc((size_t)chunkSize);
    if (!buf) {
        LOGE("seqRead: malloc failed");
        (*env)->ReleaseStringUTFChars(env, jpath, path);
        return 0.0;
    }
    /* Fill buffer with non-zero repeating pattern */
    for (int i = 0; i < chunkSize; i++) buf[i] = (uint8_t)(i ^ 0xA5);

    /* ── Pre-create test file (outside timing) ── */
    struct stat st;
    int needs_creation = 1;
    if (stat(path, &st) == 0) {
        if ((jlong)st.st_size == fileSizeBytes) {
            needs_creation = 0;
        }
    }

    if (needs_creation) {
        int wfd = open(path, O_WRONLY | O_CREAT | O_TRUNC, 0600);
        if (wfd < 0) {
            LOGE("seqRead: open for write failed: %s", path);
            free(buf);
            (*env)->ReleaseStringUTFChars(env, jpath, path);
            return 0.0;
        }
        int64_t rem = fileSizeBytes;
        while (rem > 0) {
            ssize_t toW = (rem < (int64_t)chunkSize) ? (ssize_t)rem : (ssize_t)chunkSize;
            ssize_t written = write(wfd, buf, (size_t)toW);
            if (written <= 0) break;
            rem -= written;
        }
        fdatasync(wfd);   /* ensure all bytes are physically on UFS before reading */
        close(wfd);
    }

    /* ── Timed read loop ── */
    int64_t totalBytes = 0;
    const int64_t endMs = now_ms() + (int64_t)durationMs;

    while (now_ms() < endMs) {
        int rfd = open(path, O_RDONLY);
        if (rfd < 0) break;

        /* Evict the file's pages from page cache so reads hit real UFS storage */
        posix_fadvise(rfd, 0, 0, POSIX_FADV_DONTNEED);

        ssize_t n;
        while ((n = read(rfd, buf, (size_t)chunkSize)) > 0 && now_ms() < endMs) {
            totalBytes += n;
        }
        close(rfd);
    }

    free(buf);
    (*env)->ReleaseStringUTFChars(env, jpath, path);

    if (totalBytes == 0) return 0.0;
    return (double)totalBytes / ((double)durationMs / 1000.0) / (1024.0 * 1024.0);
}

/* ── Sequential Write ────────────────────────────────────────────────────
 *
 * Protocol:
 *  Loop { open new file → write fileSizeBytes in chunkSize pieces →
 *         fdatasync (flush dirty pages to UFS) → close → unlink }
 *
 * fdatasync() is inside the timed loop because it represents the real cost of
 * committing data to persistent storage.  Without it, we'd only measure how
 * fast we can fill the kernel page cache (RAM speed).
 *
 * Expected: ~1800–2500 MB/s on UFS 4.0 devices (SD 8 Gen 3 reference = 2000).
 *
 * Returns MB/s.
 */
JNIEXPORT jdouble JNICALL
Java_com_ivarna_finalbenchmark2_utils_StorageNativeBridge_nativeStorageSeqWrite(
        JNIEnv *env, jclass cls,
        jstring jpath, jlong fileSizeBytes, jint chunkSize, jlong durationMs)
{
    const char *path = (*env)->GetStringUTFChars(env, jpath, NULL);
    if (!path) return 0.0;

    uint8_t *buf = (uint8_t *)malloc((size_t)chunkSize);
    if (!buf) {
        LOGE("seqWrite: malloc failed");
        (*env)->ReleaseStringUTFChars(env, jpath, path);
        return 0.0;
    }
    /* Non-zero pseudo-random data (zero pages may be deduplicated by UFS FTL) */
    for (int i = 0; i < chunkSize; i++) buf[i] = (uint8_t)(i * 7 ^ 0x55);

    int64_t totalBytes = 0;
    const int64_t endMs = now_ms() + (int64_t)durationMs;

    while (now_ms() < endMs) {
        int fd = open(path, O_WRONLY | O_CREAT | O_TRUNC, 0600);
        if (fd < 0) break;

        int64_t written = 0;
        while (written < fileSizeBytes && now_ms() < endMs) {
            ssize_t toW = (fileSizeBytes - written < (int64_t)chunkSize)
                          ? (ssize_t)(fileSizeBytes - written)
                          : (ssize_t)chunkSize;
            ssize_t n = write(fd, buf, (size_t)toW);
            if (n <= 0) break;
            written += n;
        }

        /* fdatasync: flush dirty pages to UFS — this is the real latency cost */
        fdatasync(fd);
        close(fd);
        unlink(path);
        totalBytes += written;
    }

    free(buf);
    (*env)->ReleaseStringUTFChars(env, jpath, path);

    if (totalBytes == 0) return 0.0;
    return (double)totalBytes / ((double)durationMs / 1000.0) / (1024.0 * 1024.0);
}

JNIEXPORT jboolean JNICALL
Java_com_ivarna_finalbenchmark2_utils_StorageNativeBridge_nativeEvictCache(
        JNIEnv *env, jclass cls, jstring jpath)
{
    const char *path = (*env)->GetStringUTFChars(env, jpath, NULL);
    if (!path) return JNI_FALSE;
    int fd = open(path, O_RDONLY);
    if (fd >= 0) {
        posix_fadvise(fd, 0, 0, POSIX_FADV_DONTNEED);
        close(fd);
        (*env)->ReleaseStringUTFChars(env, jpath, path);
        return JNI_TRUE;
    }
    (*env)->ReleaseStringUTFChars(env, jpath, path);
    return JNI_FALSE;
}

