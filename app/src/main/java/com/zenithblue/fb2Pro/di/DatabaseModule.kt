package com.zenithblue.fb2Pro.di

import android.content.Context
import com.zenithblue.fb2Pro.data.database.AppDatabase
import com.zenithblue.fb2Pro.data.repository.HistoryRepository
import com.zenithblue.fb2Pro.ui.viewmodels.HistoryViewModel

object DatabaseInitializer {

    fun createHistoryViewModel(context: Context): HistoryViewModel {
        val database = AppDatabase.getDatabase(context)
        val dao = database.benchmarkDao()
        val repository = HistoryRepository(dao)
        return HistoryViewModel(repository)
    }

    class HistoryViewModelFactory(private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return createHistoryViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}