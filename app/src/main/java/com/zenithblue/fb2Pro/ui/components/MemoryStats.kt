package com.zenithblue.fb2Pro.ui.components

data class MemoryStats(
    val usedBytes: Long,      // Used RAM in bytes
    val totalBytes: Long,     // Total RAM in bytes
    val usagePercent: Int     // Usage percentage (0-100)
) {
    val availableBytes: Long = totalBytes - usedBytes
    
    override fun toString(): String {
        return "Current: ${formatBytes(usedBytes)} / ${formatBytes(totalBytes)} (${usagePercent}%)"
    }
    
    companion object {
        fun formatBytes(bytes: Long): String {
            val unit = 1024
            if (bytes < unit) return "$bytes B"
            val exp = (kotlin.math.ln(bytes.toDouble()) / kotlin.math.ln(unit.toDouble())).toInt()
            val pre = "KMGTPE"[exp - 1] + "B"
            
            // Calculate power manually to avoid import issues
            var result = 1.0
            for (i in 0 until exp) {
                result *= unit.toDouble()
            }
            
            return String.format("%.1f %s", bytes / result, pre)
        }
    }
}