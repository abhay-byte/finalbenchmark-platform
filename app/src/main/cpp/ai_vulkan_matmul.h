/**
 * ai_vulkan_matmul.h — Vulkan compute GEMM interface for AI benchmarks
 *
 * Supports all major mobile GPU families:
 *   - Qualcomm Adreno (Snapdragon 7xx/8xx)
 *   - ARM Mali (MediaTek Dimensity, Exynos)
 *   - Samsung Xclipse (AMD RDNA, Exynos 2400+)
 *   - Imagination PowerVR (Tensor G5)
 *
 * Vulkan 1.1+ compute is the universal best path for all of these.
 */
#pragma once

struct VulkanBenchResult {
    double ms;    // avg ms per iteration
    double tps;   // throughput in ops/sec
    bool ok;      // true if benchmark succeeded
};

bool vulkan_ai_init();
void vulkan_ai_destroy();
bool vulkan_ai_available();
VulkanBenchResult vulkan_ai_matmul(int N);
