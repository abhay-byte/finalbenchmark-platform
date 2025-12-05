package com.ivarna.finalbenchmark2.di

import android.content.Context
import com.ivarna.finalbenchmark2.data.database.AppDatabase
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.ivarna.finalbenchmark2.ui.viewmodels.HistoryViewModel

object DatabaseInitializer {

    fun createHistoryViewModel(context: Context): HistoryViewModel {
        val database = AppDatabase.getDatabase(context)
        val dao = database.benchmarkDao()
        val repository = HistoryRepository(dao)
        return HistoryViewModel(repository)
    }
}