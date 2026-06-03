package com.runner.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.runner.app.RunnerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val pendingResult = goAsync()
        scope.launch {
            try {
                val db = (context.applicationContext as RunnerApp).database
                val now = System.currentTimeMillis()
                db.alarmDao().getEnabled().forEach { alarm ->
                    if (alarm.triggerAtMillis > now) {
                        AlarmScheduler.schedule(context, alarm.id, alarm.triggerAtMillis)
                    } else {
                        // Time passed while device was offline/off.
                        if (alarm.daysOfWeek.isNotEmpty() || alarm.repeatDaily) {
                            val cal = java.util.Calendar.getInstance().apply { timeInMillis = alarm.triggerAtMillis }
                            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                            val minute = cal.get(java.util.Calendar.MINUTE)
                            val days = if (alarm.daysOfWeek.isEmpty() && alarm.repeatDaily) "1,2,3,4,5,6,7" else alarm.daysOfWeek
                            val next = AlarmScheduler.calculateNextTriggerTime(hour, minute, days)
                            db.alarmDao().insert(alarm.copy(triggerAtMillis = next))
                            AlarmScheduler.schedule(context, alarm.id, next)
                        } else {
                            db.alarmDao().setEnabled(alarm.id, false)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
