package com.ivarna.finalbenchmark2.utils

import android.content.Context

class BenchmarkPreferences(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("benchmark_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val SHOW_INDIVIDUAL_OPTIONS_KEY = "show_individual_options"
    }

    fun setShowIndividualOptions(show: Boolean) {
        sharedPreferences.edit().putBoolean(SHOW_INDIVIDUAL_OPTIONS_KEY, show).apply()
    }

    fun getShowIndividualOptions(): Boolean {
        return sharedPreferences.getBoolean(SHOW_INDIVIDUAL_OPTIONS_KEY, false)
    }
}
