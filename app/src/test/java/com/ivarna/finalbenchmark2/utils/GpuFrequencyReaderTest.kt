package com.ivarna.finalbenchmark2.utils

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class GpuFrequencyReaderTest {

    @Test
    fun testGpuFrequencyReaderCreation() {
        val reader = GpuFrequencyReader()
        assertNotNull(reader)
    }

    @Test
    fun testGpuVendorDetectorCreation() {
        val detector = GpuVendorDetector()
        assertNotNull(detector)
    }

    @Test
    fun testGpuPathsCreation() {
        val paths = GpuPaths()
        assertNotNull(paths)
    }

    @Test
    fun testGpuFrequencyMonitorCreation() {
        val reader = GpuFrequencyReader()
        val monitor = GpuFrequencyMonitor(reader, kotlinx.coroutines.MainScope())
        assertNotNull(monitor)
    }

    @Test
    fun testRootCommandExecutorCreation() {
        val executor = RootCommandExecutor()
        assertNotNull(executor)
    }
}