package com.runner.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.runner.app.script.ScriptExecutionService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L
        if (alarmId == -1L) return
        ScriptExecutionService.start(context, alarmId)
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
    }
}
