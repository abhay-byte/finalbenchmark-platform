package com.zenithblue.fb2Pro.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zenithblue.fb2Pro.data.database.dao.BenchmarkDao
import com.zenithblue.fb2Pro.data.database.entities.BenchmarkResultEntity
import com.zenithblue.fb2Pro.data.database.entities.CpuTestDetailEntity

import com.zenithblue.fb2Pro.data.database.entities.GenericTestDetailEntity

@Database(
        entities = [BenchmarkResultEntity::class, CpuTestDetailEntity::class, GenericTestDetailEntity::class],
        version = 4,
        exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun benchmarkDao(): BenchmarkDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE
                    ?: synchronized(this) {
                        val instance =
                                Room.databaseBuilder(
                                                context.applicationContext,
                                                AppDatabase::class.java,
                                                "benchmark_database"
                                        )
                                        .fallbackToDestructiveMigration() // Recreates database on
                                        // schema changes
                                        .build()
                        INSTANCE = instance
                        instance
                    }
        }
    }
}
