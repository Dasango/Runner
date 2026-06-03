package com.runner.app.script

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.runner.app.MainActivity
import com.runner.app.RunnerApp
import com.runner.app.data.ExecutionLogEntity
import com.runner.app.util.NetworkUtils
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScriptExecutionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L
        if (alarmId == -1L) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        createChannel()
        // Notificación inicial obligatoria
        startForeground(NOTIFICATION_ID, buildNotification("Preparando ejecución…"))

        scope.launch {
            try {
                runAlarm(alarmId)
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun runAlarm(alarmId: Long) {
        val app = application as RunnerApp
        val db = app.database
        var alarm = db.alarmDao().getById(alarmId) ?: return
        val script = db.scriptDao().getById(alarm.scriptId)
        val scriptRepo = com.runner.app.data.ScriptRepository(this, db.scriptDao())

        val logBase = ExecutionLogEntity(
            alarmId = alarm.id,
            alarmName = alarm.name,
            scriptName = script?.name ?: "?",
            executedAt = System.currentTimeMillis(),
            success = false,
            message = ""
        )

        while (currentCoroutineContext().isActive) {
            if (!NetworkUtils.hasInternet(this)) {
                if (alarm.internetFallback) {
                    val originalScheduledTime = if (alarm.pendingFallback) alarm.fallbackScheduledTime else System.currentTimeMillis()
                    val retryCount = if (alarm.pendingFallback) alarm.fallbackRetryCount + 1 else 0
                    
                    val intervalMinutes = getFibonacciIntervalMinutes(retryCount)
                    val now = System.currentTimeMillis()
                    
                    // Verificar límite de fallback
                    if (alarm.fallbackRetryLimitMinutes != -1 && 
                        (now + intervalMinutes * 60000L - originalScheduledTime) > alarm.fallbackRetryLimitMinutes * 60000L) {
                        
                        db.executionLogDao().insert(logBase.copy(
                            message = "Fallback cancelado: el límite de ${alarm.fallbackRetryLimitMinutes} min se alcanzaría antes del próximo reintento"
                        ))
                        showResultNotification(alarm.name, false, "Límite de espera de internet alcanzado")
                        db.alarmDao().insert(alarm.copy(pendingFallback = false, fallbackRetryCount = 0))
                        break
                    }

                    db.executionLogDao().insert(
                        logBase.copy(message = "Sin internet. Reintento #$retryCount en $intervalMinutes min (Fibonacci)")
                    )
                    
                    alarm = alarm.copy(
                        pendingFallback = true,
                        fallbackScheduledTime = originalScheduledTime,
                        fallbackRetryCount = retryCount
                    )
                    db.alarmDao().insert(alarm)

                    // Mostrar cuenta regresiva en la notificación
                    runCountdown(alarm.name, intervalMinutes)
                    
                    // Al terminar delay, el bucle repite y vuelve a chequear internet
                    continue
                } else {
                    db.executionLogDao().insert(logBase.copy(message = "Cancelado: sin conexión a internet"))
                    showResultNotification(alarm.name, false, "Sin internet")
                    handleNextSchedule(alarm, db)
                    break
                }
            }

            // Hay internet, procedemos
            if (script == null) {
                db.executionLogDao().insert(logBase.copy(message = "Cancelado: script no encontrado"))
                showResultNotification(alarm.name, false, "Script no encontrado")
                break
            }

            val raw = scriptRepo.readScriptContent(script.id)
            if (raw.isNullOrBlank()) {
                db.executionLogDao().insert(logBase.copy(message = "Cancelado: contenido del script vacío"))
                showResultNotification(alarm.name, false, "Script vacío")
                break
            }

            val jsSource = if (script.isTypeScript) TypeScriptTranspiler.transpile(raw) else raw
            val result = ScriptExecutor().execute(jsSource, script.fileName)

            db.executionLogDao().insert(logBase.copy(success = result.success, message = result.message))
            showResultNotification(alarm.name, result.success, if (result.success) "OK" else result.message.take(80))

            val updatedAlarm = alarm.copy(pendingFallback = false, fallbackRetryCount = 0)
            handleNextSchedule(updatedAlarm, db)
            break
        }
    }

    private suspend fun runCountdown(alarmName: String, minutes: Int) {
        val totalSeconds = minutes * 60
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        for (second in totalSeconds downTo 0) {
            if (!currentCoroutineContext().isActive) break
            
            val m = second / 60
            val s = second % 60
            val timeStr = String.format(Locale.getDefault(), "%d:%02d", m, s)
            
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("Reintentando: $alarmName")
                .setContentText("Esperando internet... Próximo intento en $timeStr")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()
            
            nm.notify(NOTIFICATION_ID, notification)
            delay(1000)
        }
    }

    private fun getFibonacciIntervalMinutes(retryCount: Int): Int {
        if (retryCount <= 0) return 5
        if (retryCount == 1) return 5
        var a = 1
        var b = 1
        for (i in 2..retryCount) {
            val next = a + b
            a = b
            b = next
        }
        return b * 5
    }

    private suspend fun handleNextSchedule(alarm: com.runner.app.data.AlarmEntity, db: com.runner.app.data.RunnerDatabase) {
        if (alarm.repeatDaily || alarm.daysOfWeek.isNotEmpty()) {
            val cal = Calendar.getInstance().apply { timeInMillis = alarm.triggerAtMillis }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val days = if (alarm.daysOfWeek.isEmpty() && alarm.repeatDaily) "1,2,3,4,5,6,7" else alarm.daysOfWeek
            val next = com.runner.app.alarm.AlarmScheduler.calculateNextTriggerTime(hour, minute, days)
            db.alarmDao().insert(alarm.copy(triggerAtMillis = next))
            com.runner.app.alarm.AlarmScheduler.schedule(this, alarm.id, next)
        } else {
            db.alarmDao().setEnabled(alarm.id, false)
        }
    }

    private fun showResultNotification(alarmName: String, success: Boolean, detail: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (success) "Runner: $alarmName" else "Runner falló: $alarmName")
            .setContentText(detail)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Runner")
            .setContentText(text)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ejecución de scripts",
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        private const val CHANNEL_ID = "runner_execution"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context, alarmId: Long) {
            val intent = Intent(context, ScriptExecutionService::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarmId)
            }
            context.startForegroundService(intent)
        }

        fun checkAndRunPendingFallbacks(context: Context) {
            val app = context.applicationContext as RunnerApp
            val db = app.database
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                val now = System.currentTimeMillis()
                db.alarmDao().getEnabled().forEach { alarm ->
                    if (alarm.pendingFallback) {
                        val expired = if (alarm.fallbackRetryLimitMinutes != -1) {
                            (now - alarm.fallbackScheduledTime) > (alarm.fallbackRetryLimitMinutes * 60 * 1000L)
                        } else {
                            false
                        }

                        if (expired) {
                            val logBase = ExecutionLogEntity(
                                alarmId = alarm.id,
                                alarmName = alarm.name,
                                scriptName = db.scriptDao().getById(alarm.scriptId)?.name ?: "?",
                                executedAt = now,
                                success = false,
                                message = "Expirado: límite de fallback superado"
                            )
                            db.executionLogDao().insert(logBase)
                            db.alarmDao().insert(alarm.copy(pendingFallback = false))
                        } else {
                            start(context, alarm.id)
                        }
                    }
                }
            }
        }
    }
}
