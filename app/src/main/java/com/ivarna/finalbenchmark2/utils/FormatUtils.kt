package com.ivarna.finalbenchmark2.utils

import kotlin.math.ln

/**
 * Format bytes to human-readable string
 */
fun formatBytes(bytes: Long): String {
    val unit = 1024
    if (bytes < unit) return "$bytes B"
    val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
    val pre = "KMGTPE"[exp - 1] + "B"
    
    // Calculate power manually to avoid import issues
    var result = 1.0
    for (i in 0 until exp) {
        result *= unit.toDouble()
    }
    
    return String.format("%.1f %s", bytes / result, pre)
}

/**
 * Sanitize a Double before passing to JSONObject.put().
 * org.json throws on Infinity and NaN — return 0.0 for both.
 */
fun sanitizeDouble(value: Double): Double = when {
    value.isNaN()      -> 0.0
    value.isInfinite() -> 0.0
    else               -> value
}