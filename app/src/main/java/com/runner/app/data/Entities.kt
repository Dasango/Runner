package com.runner.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val fileName: String,
    val isTypeScript: Boolean,
    val uploadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val scriptId: Long,
    val triggerAtMillis: Long,
    val enabled: Boolean = true,
    val repeatDaily: Boolean = false,
    val daysOfWeek: String = "", // Comma-separated days of week (1=Sunday, 2=Monday, ..., 7=Saturday). Empty means one-time.
    val internetFallback: Boolean = false,
    val fallbackRetryLimitMinutes: Int = -1, // -1 means infinite, otherwise e.g. 30 minutes
    val pendingFallback: Boolean = false,
    val fallbackScheduledTime: Long = 0L,
    val fallbackRetryCount: Int = 0
)

@Entity(tableName = "execution_logs")
data class ExecutionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alarmId: Long,
    val alarmName: String,
    val scriptName: String,
    val executedAt: Long,
    val success: Boolean,
    val message: String
)
