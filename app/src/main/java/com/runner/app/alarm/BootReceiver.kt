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
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
