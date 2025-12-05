package com.ivarna.finalbenchmark2.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ivarna.finalbenchmark2.data.database.dao.BenchmarkDao
import com.ivarna.finalbenchmark2.data.database.entities.BenchmarkResultEntity
import com.ivarna.finalbenchmark2.data.database.entities.CpuTestDetailEntity

@Database(
    entities = [BenchmarkResultEntity::class, CpuTestDetailEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun benchmarkDao(): BenchmarkDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "benchmark_database"
                )
                .fallbackToDestructiveMigration() // Recreates database on schema changes
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}