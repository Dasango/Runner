package com.runner.app.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runner.app.data.AlarmEntity
import com.runner.app.ui.viewmodel.RunnerViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(viewModel: RunnerViewModel, onCreateAlarm: () -> Unit, onEditAlarm: (Long) -> Unit) {
    val alarms by viewModel.alarms.collectAsState()
    val scripts by viewModel.scripts.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Runner — Alarmas") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateAlarm) {
                Icon(Icons.Default.Add, contentDescription = "Nueva alarma")
            }
        }
    ) { padding ->
        if (alarms.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No hay alarmas", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Crea una alarma y elige un script JS/TS para ejecutar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(alarms, key = { it.id }) { alarm ->
                    val scriptName = scripts.find { it.id == alarm.scriptId }?.name ?: "?"
                    AlarmCard(
                        alarm = alarm,
                        scriptName = scriptName,
                        timeText = DateFormat.format("dd/MM HH:mm", Date(alarm.triggerAtMillis)).toString(),
                        onToggle = { viewModel.toggleAlarm(alarm, it) },
                        onEdit = { onEditAlarm(alarm.id) },
                        onDelete = { viewModel.deleteAlarm(alarm) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: AlarmEntity,
    scriptName: String,
    timeText: String,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(alarm.name, fontWeight = FontWeight.Bold)
                Text("Script: $scriptName", style = MaterialTheme.typography.bodySmall)
                Text("Hora: $timeText", style = MaterialTheme.typography.bodySmall)
                
                val daysOfWeekText = if (alarm.daysOfWeek.isNotEmpty()) {
                    val daysMap = mapOf(
                        1 to "Dom", 2 to "Lun", 3 to "Mar", 4 to "Mié",
                        5 to "Jue", 6 to "Vie", 7 to "Sáb"
                    )
                    alarm.daysOfWeek.split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .mapNotNull { daysMap[it] }
                        .joinToString(", ")
                } else if (alarm.repeatDaily) {
                    "Diario"
                } else {
                    "Una vez"
                }
                Text("Repetir: $daysOfWeekText", style = MaterialTheme.typography.labelSmall)
                
                if (alarm.internetFallback) {
                    val limitText = if (alarm.fallbackRetryLimitMinutes == -1) "Infinito" else "${alarm.fallbackRetryLimitMinutes} min"
                    Text("Fallback internet: $limitText", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
            }
        }
    }
}
