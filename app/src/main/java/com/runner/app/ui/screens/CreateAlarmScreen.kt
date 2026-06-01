package com.runner.app.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAlarmScreen(viewModel: RunnerViewModel, onDone: () -> Unit) {
    val scripts by viewModel.scripts.collectAsState()
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var selectedScriptId by remember { mutableStateOf<Long?>(null) }
    var hour by remember { mutableIntStateOf(8) }
    var minute by remember { mutableIntStateOf(0) }
    var repeatDaily by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva alarma") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                    }, hour, minute, DateFormat.is24HourFormat(context)).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Hora: ${"%02d".format(hour)}:${"%02d".format(minute)}")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = repeatDaily, onCheckedChange = { repeatDaily = it })
                Text("Repetir todos los días")
            }

            Text(
                "La alarma solo ejecuta el script si hay internet. Si el teléfono está apagado, no se activa.",
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
                    viewModel.createAlarm(name, scriptId, hour, minute, repeatDaily) { ok, msg ->
                        message = msg
                        if (ok) onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = scripts.isNotEmpty()
            ) {
                Text("Programar alarma")
            }

            message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private object DateFormat {
    fun is24HourFormat(context: android.content.Context): Boolean {
        return android.text.format.DateFormat.is24HourFormat(context)
    }
}
