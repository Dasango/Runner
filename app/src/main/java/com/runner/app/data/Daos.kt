package com.runner.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY uploadedAt DESC")
    fun observeAll(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts ORDER BY uploadedAt DESC")
    suspend fun getAll(): List<ScriptEntity>

    @Query("SELECT * FROM scripts WHERE id = :id")
    suspend fun getById(id: Long): ScriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(script: ScriptEntity): Long

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY triggerAtMillis ASC")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getEnabled(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmEntity): Long

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ExecutionLogDao {
    @Query("SELECT * FROM execution_logs ORDER BY executedAt DESC LIMIT 100")
    fun observeRecent(): Flow<List<ExecutionLogEntity>>

    @Query("SELECT * FROM execution_logs WHERE id = :id")
    suspend fun getById(id: Long): ExecutionLogEntity?

    @Insert
    suspend fun insert(log: ExecutionLogEntity)
}
