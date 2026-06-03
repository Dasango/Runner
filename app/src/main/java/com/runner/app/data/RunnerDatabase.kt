package com.runner.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScriptEntity::class, AlarmEntity::class, ExecutionLogEntity::class],
    version = 3,
    exportSchema = false
)
abstract class RunnerDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao
    abstract fun alarmDao(): AlarmDao
    abstract fun executionLogDao(): ExecutionLogDao

    companion object {
        @Volatile
        private var instance: RunnerDatabase? = null

        fun get(context: Context): RunnerDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RunnerDatabase::class.java,
                    "runner.db"
                )
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
        }
    }
}
