package com.runner.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.runner.app.alarm.AlarmScheduler
import com.runner.app.data.AlarmEntity
import com.runner.app.data.ExecutionLogEntity
import com.runner.app.data.RunnerDatabase
import com.runner.app.data.ScriptEntity
import com.runner.app.data.ScriptRepository
import com.runner.app.script.ScriptExecutor
import com.runner.app.script.TypeScriptTranspiler
import com.runner.app.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class RunnerViewModel(
    application: Application,
    private val database: RunnerDatabase
) : AndroidViewModel(application) {

    private val scriptRepo = ScriptRepository(application, database.scriptDao())
    private val scriptExecutor = ScriptExecutor()

    val scripts = scriptRepo.observeScripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alarms = database.alarmDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs = database.executionLogDao().observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getLogById(id: Long): ExecutionLogEntity? {
        return database.executionLogDao().getById(id)
    }

    fun uploadScript(name: String, fileName: String, content: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                scriptRepo.saveScript(name, fileName, content)
                onResult(true, "Script guardado")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Error al guardar")
            }
        }
    }

    fun updateScript(id: Long, name: String, content: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                scriptRepo.updateScript(id, name, content)
                onResult(true, "Script actualizado")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Error al actualizar")
            }
        }
    }

    fun getScriptContent(id: Long, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(scriptRepo.readScriptContent(id))
        }
    }

    fun deleteScript(id: Long) {
        viewModelScope.launch {
            scriptRepo.deleteScript(id)
        }
    }

    fun createAlarm(
        name: String,
        scriptId: Long,
        hour: Int,
        minute: Int,
        daysOfWeek: String,
        internetFallback: Boolean,
        fallbackRetryLimitMinutes: Int,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val triggerAt = AlarmScheduler.calculateNextTriggerTime(hour, minute, daysOfWeek)
            val alarm = AlarmEntity(
                name = name,
                scriptId = scriptId,
                triggerAtMillis = triggerAt,
                repeatDaily = false,
                daysOfWeek = daysOfWeek,
                internetFallback = internetFallback,
                fallbackRetryLimitMinutes = fallbackRetryLimitMinutes
            )
            val id = database.alarmDao().insert(alarm)
            AlarmScheduler.schedule(getApplication(), id, triggerAt)
            onResult(true, "Alarma programada")
        }
    }

    suspend fun getAlarmById(id: Long): AlarmEntity? {
        return database.alarmDao().getById(id)
    }

    fun updateAlarm(
        id: Long,
        name: String,
        scriptId: Long,
        hour: Int,
        minute: Int,
        daysOfWeek: String,
        internetFallback: Boolean,
        fallbackRetryLimitMinutes: Int,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val triggerAt = AlarmScheduler.calculateNextTriggerTime(hour, minute, daysOfWeek)
            val existing = database.alarmDao().getById(id)
            if (existing == null) {
                onResult(false, "Alarma no encontrada")
                return@launch
            }
            val alarm = existing.copy(
                name = name,
                scriptId = scriptId,
                triggerAtMillis = triggerAt,
                enabled = true,
                daysOfWeek = daysOfWeek,
                internetFallback = internetFallback,
                fallbackRetryLimitMinutes = fallbackRetryLimitMinutes,
                pendingFallback = false,
                fallbackScheduledTime = 0L,
                fallbackRetryCount = 0
            )
            database.alarmDao().insert(alarm)
            AlarmScheduler.cancel(getApplication(), id)
            AlarmScheduler.schedule(getApplication(), id, triggerAt)
            onResult(true, "Alarma actualizada")
        }
    }

    fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val now = System.currentTimeMillis()
                val nextTrigger = if (alarm.triggerAtMillis <= now) {
                    val cal = Calendar.getInstance().apply { timeInMillis = alarm.triggerAtMillis }
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val minute = cal.get(Calendar.MINUTE)
                    val days = if (alarm.daysOfWeek.isEmpty() && alarm.repeatDaily) "1,2,3,4,5,6,7" else alarm.daysOfWeek
                    AlarmScheduler.calculateNextTriggerTime(hour, minute, days)
                } else {
                    alarm.triggerAtMillis
                }
                val updated = alarm.copy(enabled = true, triggerAtMillis = nextTrigger, pendingFallback = false)
                database.alarmDao().insert(updated)
                AlarmScheduler.schedule(getApplication(), alarm.id, nextTrigger)
            } else {
                val updated = alarm.copy(enabled = false, pendingFallback = false)
                database.alarmDao().insert(updated)
                AlarmScheduler.cancel(getApplication(), alarm.id)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancel(getApplication(), alarm.id)
            database.alarmDao().deleteById(alarm.id)
        }
    }

    fun runScriptNow(script: ScriptEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (!NetworkUtils.hasInternet(getApplication())) {
                onResult(false, "Sin conexión a internet")
                return@launch
            }
            val raw = scriptRepo.readScriptContent(script.id)
            if (raw.isNullOrBlank()) {
                onResult(false, "Script vacío")
                return@launch
            }
            val js = if (script.isTypeScript) TypeScriptTranspiler.transpile(raw) else raw
            
            val result = withContext(Dispatchers.IO) {
                scriptExecutor.execute(js, script.fileName)
            }

            database.executionLogDao().insert(
                ExecutionLogEntity(
                    alarmId = -1,
                    alarmName = "Manual",
                    scriptName = script.name,
                    executedAt = System.currentTimeMillis(),
                    success = result.success,
                    message = result.message
                )
            )
            onResult(result.success, result.message)
        }
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}

class RunnerViewModelFactory(
    private val database: RunnerDatabase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        throw UnsupportedOperationException("Use create(modelClass, extras)")
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
        val app = checkNotNull(
            extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
        ) { "Application missing" }
        if (modelClass.isAssignableFrom(RunnerViewModel::class.java)) {
            return RunnerViewModel(app, database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
