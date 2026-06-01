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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        startForeground(NOTIFICATION_ID, buildNotification("Ejecutando script…"))

        scope.launch {
            runAlarm(alarmId)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    private suspend fun runAlarm(alarmId: Long) {
        val app = application as RunnerApp
        val db = app.database
        val alarm = db.alarmDao().getById(alarmId) ?: return
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

        if (!NetworkUtils.hasInternet(this)) {
            db.executionLogDao().insert(
                logBase.copy(message = "Cancelado: sin conexión a internet")
            )
            showResultNotification(alarm.name, false, "Sin internet")
            return
        }

        if (script == null) {
            db.executionLogDao().insert(
                logBase.copy(message = "Cancelado: script no encontrado")
            )
            showResultNotification(alarm.name, false, "Script no encontrado")
            return
        }

        val raw = scriptRepo.readScriptContent(script.id)
        if (raw.isNullOrBlank()) {
            db.executionLogDao().insert(
                logBase.copy(message = "Cancelado: contenido del script vacío")
            )
            showResultNotification(alarm.name, false, "Script vacío")
            return
        }

        val jsSource = if (script.isTypeScript) {
            TypeScriptTranspiler.transpile(raw)
        } else {
            raw
        }

        val result = ScriptExecutor().execute(jsSource, script.fileName)

        db.executionLogDao().insert(
            logBase.copy(success = result.success, message = result.message)
        )

        showResultNotification(
            alarm.name,
            result.success,
            if (result.success) "OK" else result.message.take(80)
        )

        if (alarm.repeatDaily && alarm.enabled) {
            val next = alarm.triggerAtMillis + DAY_MILLIS
            db.alarmDao().insert(alarm.copy(triggerAtMillis = next))
            com.runner.app.alarm.AlarmScheduler.schedule(this, alarm.id, next)
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
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

        fun start(context: Context, alarmId: Long) {
            val intent = Intent(context, ScriptExecutionService::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarmId)
            }
            context.startForegroundService(intent)
        }
    }
}
