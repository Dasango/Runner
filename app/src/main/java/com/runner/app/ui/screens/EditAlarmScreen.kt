package com.runner.app.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.runner.app.ui.viewmodel.RunnerViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmScreen(viewModel: RunnerViewModel, alarmId: Long, onDone: () -> Unit) {
    val scripts by viewModel.scripts.collectAsState()
    val context = LocalContext.current
    
    var name by remember { mutableStateOf("") }
    var selectedScriptId by remember { mutableStateOf<Long?>(null) }
    var hour by remember { mutableIntStateOf(8) }
    var minute by remember { mutableIntStateOf(0) }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var internetFallback by remember { mutableStateOf(false) }
    var fallbackLimitInfinite by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(alarmId) {
        val alarm = viewModel.getAlarmById(alarmId)
        if (alarm != null) {
            name = alarm.name
            selectedScriptId = alarm.scriptId
            val cal = Calendar.getInstance().apply { timeInMillis = alarm.triggerAtMillis }
            hour = cal.get(Calendar.HOUR_OF_DAY)
            minute = cal.get(Calendar.MINUTE)
            selectedDays = if (alarm.daysOfWeek.isNotEmpty()) {
                alarm.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            } else {
                emptySet()
            }
            internetFallback = alarm.internetFallback
            fallbackLimitInfinite = alarm.fallbackRetryLimitMinutes == -1
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar alarma") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Script", style = MaterialTheme.typography.titleSmall)
                if (scripts.isEmpty()) {
                    Text(
                        "Sube un script primero en la pestaña Scripts.",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    scripts.forEach { script ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedScriptId == script.id,
                                onCheckedChange = { if (it) selectedScriptId = script.id }
                            )
                            Text(script.name)
                        }
                    }
                }

                Button(
                    onClick = {
                        TimePickerDialog(context, { _, h, m ->
                            hour = h
                            minute = m
                        }, hour, minute, true).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Hora: ${"%02d".format(hour)}:${"%02d".format(minute)}")
                }

                Text("Repetir los días (dejar vacío para una sola vez):", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val daysOfWeekNames = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
                    val daysOfWeekValues = listOf(1, 2, 3, 4, 5, 6, 7)
                    daysOfWeekValues.forEachIndexed { index, dayVal ->
                        val isSelected = selectedDays.contains(dayVal)
                        if (isSelected) {
                            Button(
                                onClick = { selectedDays = selectedDays - dayVal },
                                contentPadding = PaddingValues(4.dp),
                                modifier = Modifier.size(width = 44.dp, height = 36.dp)
                            ) {
                                Text(daysOfWeekNames[index], style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { selectedDays = selectedDays + dayVal },
                                contentPadding = PaddingValues(4.dp),
                                modifier = Modifier.size(width = 44.dp, height = 36.dp)
                            ) {
                                Text(daysOfWeekNames[index], style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = internetFallback, onCheckedChange = { internetFallback = it })
                    Text("Reintentar si falla el internet (Fallback)")
                }

                if (internetFallback) {
                    Column(modifier = Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Plazo de reintentos:", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = fallbackLimitInfinite,
                                onClick = { fallbackLimitInfinite = true }
                            )
                            Text("Indefinido (hasta que haya internet)", modifier = Modifier.padding(start = 4.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !fallbackLimitInfinite,
                                onClick = { fallbackLimitInfinite = false }
                            )
                            Text("Límite de 30 minutos", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }

                Text(
                    "Si el teléfono está apagado al momento de la alarma, esta se reprogramará para su siguiente ocurrencia al encenderlo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val scriptId = selectedScriptId
                        if (name.isBlank() || scriptId == null) {
                            message = "Completa nombre y script"
                            return@Button
                        }
                        val daysStr = selectedDays.sorted().joinToString(",")
                        val fallbackLimit = if (fallbackLimitInfinite) -1 else 30
                        viewModel.updateAlarm(alarmId, name, scriptId, hour, minute, daysStr, internetFallback, fallbackLimit) { ok, msg ->
                            message = msg
                            if (ok) onDone()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = scripts.isNotEmpty()
                ) {
                    Text("Guardar cambios")
                }

                message?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
