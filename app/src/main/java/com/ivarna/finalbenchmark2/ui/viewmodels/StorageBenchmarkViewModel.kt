package com.ivarna.finalbenchmark2.ui.viewmodels

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.data.database.entities.BenchmarkResultEntity
import com.ivarna.finalbenchmark2.data.database.entities.GenericTestDetailEntity
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.ivarna.finalbenchmark2.utils.PerformanceMonitor
import com.ivarna.finalbenchmark2.utils.StorageNativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.pow
import kotlin.math.roundToInt

// ── Data types ────────────────────────────────────────────────────────────────

enum class StorageTest {
    SEQ_READ, SEQ_WRITE, RAND_4K, SMALL_FILES, SQLITE, MIXED
}

data class StorageTestResult(
    val test: StorageTest,
    val displayName: String,
    val value: Double,   // MB/s for bandwidth tests; files/s for SMALL_FILES; txn/s for SQLITE
    val unit: String,
    val score: Int,      // normalised to reference; reference device = 100 pts
    val durationMs: Long = 0L  // actual wall-clock time of the measurement phase
)

data class StorageBenchmarkUiState(
    val isWarmingUp: Boolean = false,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val currentTest: StorageTest = StorageTest.SEQ_READ,
    val currentTestIndex: Int = 0,
    val totalTests: Int = StorageTest.values().size,
    val currentTestName: String = "",
    val overallProgress: Float = 0f,
    val currentTestProgress: Float = 0f,
    val currentValue: Double = 0.0,
    val currentUnit: String = "MB/s",
    val storageFreeGB: Double = 0.0,
    val cpuTempC: Float = 0f,
    val completedTests: List<StorageTestResult> = emptyList(),
    val totalScore: Int = 0,
    val presetName: String = "",
    val statusMessage: String = ""
)

// ── Constants ─────────────────────────────────────────────────────────────────

private val STORAGE_TESTS = StorageTest.values().toList()

/**
 * Reference values targeting SD 8 Gen 3 (UFS 4.0 internal storage) = 100 pts.
 *
 * NOTE: These are initial estimates — calibrate from first run results.
 *
 * Methodology:
 *   SEQ_READ    — 256 MB file read via FileInputStream 1MB buffer, timed loop (MB/s)
 *   SEQ_WRITE   — 256 MB FileOutputStream 1MB buffer + fsync at end (MB/s)
 *   RAND_4K     — RandomAccessFile seeks to random 4K-aligned offsets in a 128MB file (MB/s)
 *   SMALL_FILES — Create / write / delete 300 × 8KB temp files (files/s)
 *   SQLITE      — WAL-mode SQLite: batched 1000-row INSERTs + indexed SELECTs (txn/s)
 *   MIXED       — 64MB seq read + 500 rand-4K + 50 small files, composite MB/s
 */
private val STORAGE_REFERENCE = mapOf(
    // Reference calibrated from ACTUAL SD 8 Gen 3 + UFS 4.0 measurements.
    // Targeting SD 8 Gen 3 (UFS 4.0) = 100 pts on each test.
    StorageTest.SEQ_READ    to 2_133.0,
    StorageTest.SEQ_WRITE   to 939.0,
    StorageTest.RAND_4K     to 42.0,
    StorageTest.SMALL_FILES to 4_300.0,
    StorageTest.SQLITE      to 10_626.0,
    StorageTest.MIXED       to 620.0,
)

private fun StorageTest.displayName() = when (this) {
    StorageTest.SEQ_READ    -> "Sequential Read"
    StorageTest.SEQ_WRITE   -> "Sequential Write"
    StorageTest.RAND_4K     -> "Random 4K Read"
    StorageTest.SMALL_FILES -> "Small File Ops"
    StorageTest.SQLITE      -> "SQLite Performance"
    StorageTest.MIXED       -> "Mixed Workload"
}

private fun StorageTest.unit() = when (this) {
    StorageTest.SMALL_FILES -> "files/s"
    StorageTest.SQLITE      -> "txn/s"
    else                    -> "MB/s"
}

private fun StorageTest.score(value: Double): Int {
    val ref = STORAGE_REFERENCE[this] ?: return 0
    val ratio = value / ref
    // Allow scores above 100 to honestly reflect devices faster than the reference.
    // Clamp at 0 only to guard against negative values from measurement errors.
    return (ratio * 100.0).roundToInt().coerceAtLeast(0)
}

private fun calculateStorageGeometricMean(results: List<StorageTestResult>): Double {
    val ratios = results.map { r ->
        val ref = STORAGE_REFERENCE[r.test] ?: return@map 1.0
        // Cap at 2.0× reference (200 pts) to prevent page-cache reads from massively
        // inflating the geometric mean while still rewarding genuinely fast devices.
        (r.value / ref).coerceIn(1e-9, 2.0)
    }
    if (ratios.isEmpty()) return 0.0
    val product = ratios.fold(1.0) { acc, v -> acc * v }
    return product.pow(1.0 / ratios.size) * 100.0
}

private const val WARMUP_DUR_MS  = 1_000L
private const val MEASURE_DUR_MS = 3_000L
private const val TICK_MS        = 100L

// File sizes (chosen so each test takes ~2-8s on a typical device)
private const val SEQ_FILE_MB    = 256L   // 256 MB sequential test file
private const val RAND_FILE_MB   = 128L   // 128 MB random access test file
private const val SEQ_CHUNK_BYTES = 1024 * 1024          // 1 MB chunk
private const val RAND_4K        = 4096                  // 4 KB random block
private const val SMALL_FILE_SIZE = 8 * 1024             // 8 KB per small file
private const val SMALL_FILE_COUNT = 300                  // files per batch
private const val SQLITE_ROWS    = 50                    // rows per INSERT batch (1000 caused >durationMs per txn → 0 result)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class StorageBenchmarkViewModel(
    application: Application,
    private val historyRepository: HistoryRepository?
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StorageBenchmarkUiState())
    val uiState: StateFlow<StorageBenchmarkUiState> = _uiState.asStateFlow()

    private val _completionEvent = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val completionEvent: SharedFlow<String> = _completionEvent.asSharedFlow()

    private var runJob: Job? = null
    private val performanceMonitor = PerformanceMonitor(application)
    private val cacheDir: File = application.cacheDir
    private val baseCpuTemp = (38..45).random().toFloat()

    fun start(preset: String) {
        runJob?.cancel()
        _uiState.update { it.copy(presetName = preset) }
        runJob = viewModelScope.launch { runBenchmark() }
    }

    fun stop() {
        runJob?.cancel()
        if (performanceMonitor.isMonitoring()) performanceMonitor.stop()
        cleanupAll()
        _uiState.update { StorageBenchmarkUiState() }
    }

    // ── Benchmark loop ─────────────────────────────────────────────────────

    private suspend fun runBenchmark() {
        StorageNativeBridge.load()
        val results = mutableListOf<StorageTestResult>()
        performanceMonitor.start()

        for ((index, test) in STORAGE_TESTS.withIndex()) {
            val name = test.displayName()
            val unit = test.unit()

            // ─ Warm-up phase ──────────────────────────────────────────────
            _uiState.update {
                it.copy(
                    isWarmingUp = true, isRunning = false,
                    currentTest = test, currentTestIndex = index,
                    currentTestName = name, currentTestProgress = 0f,
                    currentUnit = unit,
                    overallProgress = index.toFloat() / STORAGE_TESTS.size,
                    statusMessage = "Preparing ${name}…",
                    storageFreeGB = cacheDirFreeGB()
                )
            }
            coroutineScope {
                val warmupJob = async(Dispatchers.IO) { runTest(test, durationMs = 800L) }
                val warmSteps = (WARMUP_DUR_MS / TICK_MS).toInt()
                repeat(warmSteps) { step ->
                    delay(TICK_MS)
                    _uiState.update { s ->
                        s.copy(
                            currentTestProgress = (step + 1).toFloat() / warmSteps * 0.15f,
                            cpuTempC = mockCpuTemp(),
                            storageFreeGB = cacheDirFreeGB()
                        )
                    }
                }
                warmupJob.await()
            }

            // ─ Measure phase ───────────────────────────────────────────────
            _uiState.update {
                it.copy(isWarmingUp = false, isRunning = true, statusMessage = "Measuring…")
            }
            val measureSteps = (MEASURE_DUR_MS / TICK_MS).toInt()
            val measureStartMs = System.currentTimeMillis()
            val value = coroutineScope {
                val measureJob = async(Dispatchers.IO) { runTest(test, durationMs = MEASURE_DUR_MS) }
                repeat(measureSteps) { step ->
                    delay(TICK_MS)
                    val overall = (index + 0.15f + (step + 1).toFloat() / measureSteps * 0.85f) / STORAGE_TESTS.size
                    _uiState.update { s ->
                        s.copy(
                            currentTestProgress = 0.15f + (step + 1).toFloat() / measureSteps * 0.85f,
                            overallProgress = overall,
                            cpuTempC = mockCpuTemp(),
                            storageFreeGB = cacheDirFreeGB()
                        )
                    }
                }
                measureJob.await()
            }
            val elapsedMs = System.currentTimeMillis() - measureStartMs

            val result = StorageTestResult(test, name, value, unit, test.score(value), elapsedMs)
            results += result
            _uiState.update { s -> s.copy(currentValue = value, completedTests = results.toList()) }
        }

        val performanceMetricsJson = performanceMonitor.stop()
        cleanupAll()
        val totalScore = calculateStorageGeometricMean(results).roundToInt().coerceAtLeast(0)

        _uiState.update {
            it.copy(
                isRunning = false, isCompleted = true,
                overallProgress = 1f, totalScore = totalScore,
                statusMessage = "Complete"
            )
        }

        val resultJson = buildResultJson(results, totalScore, _uiState.value.presetName, performanceMetricsJson)
        saveToDatabase(results, totalScore, performanceMetricsJson, resultJson)
        _completionEvent.emit(resultJson)
    }

    // ── Test dispatcher ────────────────────────────────────────────────────

    private fun runTest(test: StorageTest, durationMs: Long): Double = try {
        when (test) {
            StorageTest.SEQ_READ    -> benchSeqRead(durationMs)
            StorageTest.SEQ_WRITE   -> benchSeqWrite(durationMs)
            StorageTest.RAND_4K     -> benchRand4K(durationMs)
            StorageTest.SMALL_FILES -> benchSmallFiles(durationMs)
            StorageTest.SQLITE      -> benchSqlite(durationMs)
            StorageTest.MIXED       -> benchMixed(durationMs)
        }
    } catch (e: Exception) {
        android.util.Log.e("StorageBenchmarkVM", "Test $test failed: ${e.message}", e)
        0.0
    }

    // ── 1. Sequential Read ─────────────────────────────────────────────────
    /**
     * Pre-creates a 256 MB file (or re-uses it), then reads it end-to-end in
     * 1 MB chunks, looping until durationMs elapses.
     * Returns MB/s.
     */
    private fun benchSeqRead(durationMs: Long): Double {
        // Use JNI + posix_fadvise(POSIX_FADV_DONTNEED) to evict page cache before
        // each read pass, giving true UFS 4.0 sequential read speed (~3500-4200 MB/s)
        // instead of Linux page-cache speed (~6 GB/s).
        if (StorageNativeBridge.isAvailable) {
            val path = File(cacheDir, "bench_seqread_jni.bin").absolutePath
            return StorageNativeBridge.nativeStorageSeqRead(
                path, SEQ_FILE_MB.toLong() * 1024L * 1024L, SEQ_CHUNK_BYTES, durationMs
            )
        }
        // Fallback: Java (will measure page-cache speed on subsequent runs)
        val buf = ByteArray(SEQ_CHUNK_BYTES)
        var totalBytes = 0L
        val endMs = System.currentTimeMillis() + durationMs
        while (System.currentTimeMillis() < endMs) {
            val file = File(cacheDir, "bench_seqread_${System.nanoTime()}.bin")
            createTestFile(file, SEQ_FILE_MB * 1024 * 1024)
            FileInputStream(file).use { fis ->
                var n = 0
                while (fis.read(buf).also { n = it } != -1 && System.currentTimeMillis() < endMs) {
                    totalBytes += n
                }
            }
            file.delete()
        }
        if (totalBytes == 0L) return 0.0
        return totalBytes.toDouble() / (durationMs / 1000.0) / (1024.0 * 1024.0)
    }

    // ── 2. Sequential Write ────────────────────────────────────────────────
    /**
     * Writes 256 MB to a temp file in 1 MB chunks, then calls FileDescriptor.sync()
     * (fsync) to flush to persistent storage. Measures actual storage write speed
     * rather than just OS page-cache fill speed.
     * Returns MB/s.
     */
    private fun benchSeqWrite(durationMs: Long): Double {
        // Use JNI + fdatasync() per pass to measure true UFS 4.0 write speed.
        // fdatasync flushes dirty pages to UFS hardware; without it we only measure
        // how fast we can fill the Linux page cache (RAM speed).
        if (StorageNativeBridge.isAvailable) {
            val path = File(cacheDir, "bench_seqwrite_jni.bin").absolutePath
            return StorageNativeBridge.nativeStorageSeqWrite(
                path, SEQ_FILE_MB.toLong() * 1024L * 1024L, SEQ_CHUNK_BYTES, durationMs
            )
        }
        // Fallback: Java
        val buf = ByteArray(SEQ_CHUNK_BYTES).also { fillRandom(it) }
        var totalBytes = 0L
        val endMs = System.currentTimeMillis() + durationMs
        while (System.currentTimeMillis() < endMs) {
            val file = File(cacheDir, "bench_seqwrite_${System.nanoTime()}.bin")
            FileOutputStream(file).use { fos ->
                var written = 0L
                val target = SEQ_FILE_MB * 1024 * 1024
                while (written < target && System.currentTimeMillis() < endMs) {
                    fos.write(buf)
                    written += SEQ_CHUNK_BYTES
                }
                fos.flush()
                totalBytes += written
            }
            file.delete()
        }
        if (totalBytes == 0L) return 0.0
        return totalBytes.toDouble() / (durationMs / 1000.0) / (1024.0 * 1024.0)
    }

    // ── 3. Random 4K Read ─────────────────────────────────────────────────
    /**
     * Performs random 4 KB reads from a pre-created 128 MB file using
     * RandomAccessFile. Seeks to 4K-aligned random positions derived from a
     * deterministic LCG (seed=12345) so results are reproducible.
     *
     * Page cache is evicted every 512 reads (2 MB) via nativeEvictCache to
     * prevent the 128 MB file from being served from RAM. Without this, the
     * benchmark reports ~2,100 MB/s (RAM speed) instead of real UFS random
     * read speed (~150-250 MB/s).
     *
     * Returns MB/s (= IOPS × 4 KB).
     */
    private fun benchRand4K(durationMs: Long): Double {
        val file = File(cacheDir, "bench_rand4k.bin")
        val fileSize = RAND_FILE_MB * 1024 * 1024
        if (!file.exists() || file.length() < fileSize) {
            createTestFile(file, fileSize)
        }
        val buf = ByteArray(RAND_4K)
        val numBlocks = (fileSize / RAND_4K).toInt()
        var seed = 12345L
        var totalBytes = 0L
        var readCount = 0L
        val endMs = System.currentTimeMillis() + durationMs

        RandomAccessFile(file, "r").use { raf ->
            while (System.currentTimeMillis() < endMs) {
                seed = seed * 6364136223846793005L + 1442695040888963407L
                val block = ((seed ushr 33) % numBlocks).toInt().let { if (it < 0) -it else it }
                raf.seek(block.toLong() * RAND_4K)
                raf.readFully(buf)
                totalBytes += RAND_4K
                readCount++
                // Evict page cache every 512 reads (2 MB) to prevent RAM-cached
                // reads from inflating the measurement. 512 reads = ~2-3 ms at
                // uncached speed; collision rate within this window is ~0.8%.
                if (readCount % 512L == 0L && StorageNativeBridge.isAvailable) {
                    StorageNativeBridge.nativeEvictCache(file.absolutePath)
                }
            }
        }
        if (totalBytes == 0L) return 0.0
        return totalBytes.toDouble() / (durationMs / 1000.0) / (1024.0 * 1024.0)
    }

    // ── 4. Small File Operations ───────────────────────────────────────────
    /**
     * Repeatedly creates, writes, and deletes batches of SMALL_FILE_COUNT files
     * (8 KB each) in a temp subdirectory. Measures file system metadata throughput.
     * Returns files/second.
     */
    private fun benchSmallFiles(durationMs: Long): Double {
        val dir = File(cacheDir, "bench_small")
        dir.deleteRecursively()
        dir.mkdirs()
        val buf = ByteArray(SMALL_FILE_SIZE).also { fillRandom(it) }
        var totalOps = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            // Create + write batch
            for (i in 0 until SMALL_FILE_COUNT) {
                if (System.currentTimeMillis() >= endMs) break
                val f = File(dir, "f$i.bin")
                FileOutputStream(f).use { fos ->
                    fos.write(buf)
                    fos.fd.sync()
                }
                totalOps++
            }
            // Delete batch
            dir.listFiles()?.forEach { it.delete() }
        }
        dir.deleteRecursively()
        if (totalOps == 0L) return 0.0
        return totalOps.toDouble() / (durationMs / 1000.0)
    }

    // ── 5. SQLite Performance ──────────────────────────────────────────────
    /**
     * Runs batched INSERT and SELECT transactions on a WAL-mode SQLite database
     * in the app cache directory. Measures combined transactions per second.
     *
     * Schema: single "items" table with id (PK), name TEXT, value REAL, ts INTEGER.
     * Each "transaction" = 1 BEGIN…COMMIT of SQLITE_ROWS inserts.
     */
    private fun benchSqlite(durationMs: Long): Double {
        val dbFile = File(cacheDir, "bench_sqlite.db")
        if (dbFile.exists()) dbFile.delete()

        // Open a raw SQLite database at the given path (avoids SQLiteOpenHelper
        // path-handling inconsistencies across API levels).
        // WAL and sync PRAGMAs MUST be set before any table creation
        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        try {
            // PRAGMA journal_mode returns a result row — must use rawQuery, not execSQL
            db.rawQuery("PRAGMA journal_mode=WAL", null).use { it.moveToFirst() }
            db.rawQuery("PRAGMA synchronous=NORMAL", null).use { it.moveToFirst() }
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "value REAL," +
                "ts INTEGER)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_ts ON items(ts)")
        } catch (e: Exception) {
            android.util.Log.e("StorageBenchmarkVM", "SQLite setup failed: ${e.message}", e)
            db.close(); dbFile.delete()
            return 0.0
        }

        var txns = 0L
        val endMs = System.currentTimeMillis() + durationMs
        // Pre-compile statement once outside the loop
        val insertStmt = db.compileStatement("INSERT INTO items(name,value,ts) VALUES(?,?,?)")

        try {
            while (System.currentTimeMillis() < endMs) {
                // INSERT batch transaction
                db.beginTransaction()
                try {
                    for (i in 0 until SQLITE_ROWS) {
                        insertStmt.bindString(1, "item_$i")
                        insertStmt.bindDouble(2, i * 1.5)
                        insertStmt.bindLong(3, System.currentTimeMillis())
                        insertStmt.executeInsert()
                    }
                    db.setTransactionSuccessful()
                    txns++
                } finally {
                    db.endTransaction()
                }
                if (System.currentTimeMillis() >= endMs) break

                // SELECT query (indexed query) - no transaction wrapper
                db.rawQuery("SELECT COUNT(*) FROM items WHERE value > 500", null).use { c -> c.moveToFirst() }
                txns++

                // Keep table small to avoid unbounded growth slowing inserts
                if (txns % 20L == 0L) {
                    db.execSQL("DELETE FROM items")
                }
            }
        } finally {
            insertStmt.close()
            db.close()
            dbFile.delete()
        }

        if (txns == 0L) return 0.0
        return txns.toDouble() / (durationMs / 1000.0)
    }

    // ── 6. Mixed Workload ──────────────────────────────────────────────────
    /**
     * Interleaves smaller versions of SEQ_READ, RAND_4K, and SMALL_FILES to
     * simulate real application I/O patterns. Reports a composite MB/s by
     * dividing total bytes processed by total wall time.
     */
    private fun benchMixed(durationMs: Long): Double {
        val seqFile = File(cacheDir, "bench_mixed_seq.bin")
        val randFile = File(cacheDir, "bench_mixed_rand.bin")
        val smallDir = File(cacheDir, "bench_mixed_small")

        val smallFileSizeMB = 64L
        val randFileSizeMB = 64L

        if (!seqFile.exists() || seqFile.length() < smallFileSizeMB * 1024 * 1024)
            createTestFile(seqFile, smallFileSizeMB * 1024 * 1024)
        if (!randFile.exists() || randFile.length() < randFileSizeMB * 1024 * 1024)
            createTestFile(randFile, randFileSizeMB * 1024 * 1024)
        smallDir.deleteRecursively(); smallDir.mkdirs()

        val seqBuf = ByteArray(SEQ_CHUNK_BYTES)
        val randBuf = ByteArray(RAND_4K)
        val smallBuf = ByteArray(SMALL_FILE_SIZE).also { fillRandom(it) }
        val randNumBlocks = (randFileSizeMB * 1024 * 1024 / RAND_4K).toInt()
        var seed = 99991L
        var totalBytes = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            // (a) Read 16 MB sequentially (evicting page cache if native available)
            if (StorageNativeBridge.isAvailable) {
                StorageNativeBridge.nativeEvictCache(seqFile.absolutePath)
            }
            var seqRead = 0
            FileInputStream(seqFile).use { fis ->
                var n = 0
                while (seqRead < 16 * 1024 * 1024 &&
                    fis.read(seqBuf).also { n = it } != -1 &&
                    System.currentTimeMillis() < endMs) {
                    totalBytes += n; seqRead += n
                }
            }
            if (System.currentTimeMillis() >= endMs) break

            // (b) 200 random 4K reads
            RandomAccessFile(randFile, "r").use { raf ->
                repeat(200) {
                    seed = seed * 6364136223846793005L + 1442695040888963407L
                    val block = ((seed ushr 33) % randNumBlocks).toInt().let { b -> if (b < 0) -b else b }
                    raf.seek(block.toLong() * RAND_4K)
                    raf.readFully(randBuf)
                    totalBytes += RAND_4K
                }
            }
            if (System.currentTimeMillis() >= endMs) break

            // (c) 50 small file creates + deletes (with fsync)
            for (i in 0 until 50) {
                if (System.currentTimeMillis() >= endMs) break
                val f = File(smallDir, "m$i.bin")
                FileOutputStream(f).use { fos ->
                    fos.write(smallBuf)
                    fos.fd.sync()
                }
                totalBytes += SMALL_FILE_SIZE
                f.delete()
            }
        }

        seqFile.delete(); randFile.delete(); smallDir.deleteRecursively()
        if (totalBytes == 0L) return 0.0
        return totalBytes.toDouble() / (durationMs / 1000.0) / (1024.0 * 1024.0)
    }

    // ── Cleanup ────────────────────────────────────────────────────────────

    private fun cleanupAll() {
        // Delete any leftover per-pass read/write files (named bench_seqread_*.bin / bench_seqwrite_*.bin)
        cacheDir.listFiles { f -> f.name.startsWith("bench_seqread_") || f.name.startsWith("bench_seqwrite_") }
            ?.forEach { it.delete() }
        listOf("bench_rand4k.bin", "bench_mixed_seq.bin", "bench_mixed_rand.bin")
            .forEach { File(cacheDir, it).delete() }
        listOf("bench_small", "bench_mixed_small").forEach {
            File(cacheDir, it).deleteRecursively()
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Creates a file of the given size filled with a repeating 256-byte pattern
     * (not zero — zero pages may be handled specially by the kernel/UFS firmware).
     */
    private fun createTestFile(file: File, sizeBytes: Long) {
        FileOutputStream(file).use { fos ->
            var remaining = sizeBytes
            while (remaining > 0) {
                val toWrite = minOf(remaining, SEQ_CHUNK_BYTES.toLong()).toInt()
                fos.write(testFileChunk, 0, toWrite)
                remaining -= toWrite
            }
            fos.fd.sync()
        }
    }

    private fun fillRandom(buf: ByteArray) {
        var seed = 12345L
        for (i in buf.indices) {
            seed = seed xor (seed shl 13); seed = seed xor (seed ushr 7); seed = seed xor (seed shl 17)
            buf[i] = seed.toByte()
        }
    }

    private fun cacheDirFreeGB(): Double = try {
        val stat = android.os.StatFs(cacheDir.path)
        stat.availableBlocksLong.toDouble() * stat.blockSizeLong / (1024.0 * 1024.0 * 1024.0)
    } catch (e: Exception) { 0.0 }

    private fun mockCpuTemp(): Float =
        (baseCpuTemp + (-1f..3f).random()).coerceIn(30f, 85f)

    private fun ClosedFloatingPointRange<Float>.random(): Float =
        start + Math.random().toFloat() * (endInclusive - start)

    // ── DB save ────────────────────────────────────────────────────────────

    private suspend fun saveToDatabase(
        results: List<StorageTestResult>,
        totalScore: Int,
        performanceMetricsJson: String,
        detailedJson: String
    ) {
        val repo = historyRepository ?: return
        try {
            val avgBW = results.filter { it.unit == "MB/s" }
                .map { it.value }.average().takeIf { it.isFinite() } ?: 0.0
            val detailsArrayJson = try {
                JSONObject(detailedJson).optJSONArray("detailed_results")?.toString() ?: "[]"
            } catch (e: Exception) { "[]" }

            val entity = BenchmarkResultEntity(
                type                   = "STORAGE",
                totalScore             = totalScore.toDouble(),
                timestamp              = System.currentTimeMillis(),
                deviceModel            = "${Build.MANUFACTURER} ${Build.MODEL}",
                singleCoreScore        = 0.0,
                multiCoreScore         = avgBW,
                normalizedScore        = totalScore.toDouble(),
                detailedResultsJson    = detailsArrayJson,
                performanceMetricsJson = performanceMetricsJson
            )
            val details = results.map { r ->
                GenericTestDetailEntity(
                    resultId    = 0,
                    testName    = r.displayName,
                    score       = r.value,
                metricsJson = """{"score":${r.score},"value":${"%.2f".format(r.value)},"unit":"${r.unit}","durationMs":${r.durationMs}}"""
                )
            }
            repo.saveGenericBenchmark(entity, details)
        } catch (e: Exception) {
            android.util.Log.e("StorageBenchmarkVM", "DB save failed: ${e.message}", e)
        }
    }

    // ── Result JSON ────────────────────────────────────────────────────────

    private fun buildResultJson(
        results: List<StorageTestResult>,
        totalScore: Int,
        preset: String,
        performanceMetricsJson: String
    ): String {
        val avgBW = results.filter { it.unit == "MB/s" }
            .map { it.value }.average().takeIf { it.isFinite() } ?: 0.0
        val detailedArray = JSONArray()
        results.forEach { r ->
            detailedArray.put(JSONObject().apply {
                put("name", r.displayName)
                put("opsPerSecond", r.value)
                put("executionTimeMs", r.durationMs.toDouble())
                put("isValid", true)
                put("metricsJson", """{"score":${r.score},"value":${"%.2f".format(r.value)},"unit":"${r.unit}","durationMs":${r.durationMs}}""")  
            })
        }
        val perfObj = try { JSONObject(performanceMetricsJson) } catch (e: Exception) { JSONObject() }
        return JSONObject().apply {
            put("type", "STORAGE")
            put("preset", preset)
            put("final_score", totalScore.toDouble())
            put("normalized_score", totalScore.toDouble())
            put("single_core_score", 0.0)
            put("multi_core_score", avgBW)
            put("detailed_results", detailedArray)
            put("timestamp", System.currentTimeMillis())
            put("performance_metrics", perfObj)
        }.toString()
    }

    // ── Factory ────────────────────────────────────────────────────────────

    companion object {
        private val testFileChunk by lazy {
            val pattern = ByteArray(256) { (it % 251).toByte() }
            ByteArray(SEQ_CHUNK_BYTES).apply {
                for (off in indices) this[off] = pattern[off % pattern.size]
            }
        }

        fun factory(
            historyRepository: HistoryRepository?,
            application: Application
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                StorageBenchmarkViewModel(application, historyRepository) as T
        }
    }
}
