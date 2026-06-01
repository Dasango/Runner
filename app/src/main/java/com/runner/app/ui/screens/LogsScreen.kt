package com.runner.app.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runner.app.ui.viewmodel.RunnerViewModel
import java.util.Date

private const val PREVIEW_MAX_LINES = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    viewModel: RunnerViewModel,
    onOpenLog: (Long) -> Unit
) {
    val logs by viewModel.logs.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Historial de ejecución") }) }
    ) { padding ->
        if (logs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Aún no hay ejecuciones registradas.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(logs, key = { it.id }) { log ->
                    val isLongMessage = log.message.lines().size > PREVIEW_MAX_LINES ||
                        log.message.length > 200

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenLog(log.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (log.success) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${log.alarmName} → ${log.scriptName}",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    DateFormat.format(
                                        "dd/MM/yyyy HH:mm:ss",
                                        Date(log.executedAt)
                                    ).toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    if (log.success) "Éxito" else "Falló / omitido",
                                    color = if (log.success) {
                                        Color(0xFF2E7D32)
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = log.message.ifBlank { "(sin mensaje)" },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = PREVIEW_MAX_LINES,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isLongMessage) {
                                    Text(
                                        "Toca para ver el mensaje completo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Ver detalle",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
