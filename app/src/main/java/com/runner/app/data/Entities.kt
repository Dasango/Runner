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
    val repeatDaily: Boolean = false
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
