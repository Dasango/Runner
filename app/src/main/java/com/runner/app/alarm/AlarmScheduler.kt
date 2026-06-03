package com.runner.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import java.util.Calendar

object AlarmScheduler {
    private const val ACTION_ALARM = "com.runner.app.ALARM_TRIGGER"

    fun calculateNextTriggerTime(hour: Int, minute: Int, daysOfWeek: String): Long {
        val nowCal = Calendar.getInstance()
        val targetCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (daysOfWeek.isBlank()) {
            // One-time alarm
            if (targetCal.timeInMillis <= nowCal.timeInMillis) {
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return targetCal.timeInMillis
        }

        val selectedDays = daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        if (selectedDays.isEmpty()) {
            if (targetCal.timeInMillis <= nowCal.timeInMillis) {
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return targetCal.timeInMillis
        }

        for (i in 0..7) {
            val checkCal = Calendar.getInstance().apply {
                timeInMillis = targetCal.timeInMillis
                add(Calendar.DAY_OF_YEAR, i)
            }
            val dayOfWeek = checkCal.get(Calendar.DAY_OF_WEEK)
            if (selectedDays.contains(dayOfWeek)) {
                if (i == 0 && checkCal.timeInMillis <= nowCal.timeInMillis) {
                    continue
                }
                return checkCal.timeInMillis
            }
        }
        return targetCal.timeInMillis
    }

    fun schedule(context: Context, alarmId: Long, triggerAtMillis: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            Toast.makeText(
                context,
                "Permite alarmas exactas en ajustes del sistema",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            return
        }

        val pi = pendingIntent(context, alarmId)
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pi
        )
    }

    fun cancel(context: Context, alarmId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context, alarmId))
    }

    private fun pendingIntent(context: Context, alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
