package com.runner.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.runner.app.ui.viewmodel.RunnerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScriptScreen(viewModel: RunnerViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val text = stream.bufferedReader().readText()
            content = text
            val fn = uri.lastPathSegment?.substringAfterLast('/') ?: "script.js"
            fileName = fn
            if (name.isBlank()) {
                name = fn.substringBeforeLast('.')
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subir script") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { picker.launch(arrayOf("application/javascript", "text/javascript", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Elegir archivo .js o .ts")
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Código") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                minLines = 10
            )

            Text(
                "También puedes pegar el código directamente.",
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                onClick = {
                    if (name.isBlank() || content.isBlank()) {
                        message = "Nombre y código requeridos"
                        return@Button
                    }
                    val fn = fileName.ifBlank {
                        if (content.contains(": ") || content.contains("interface ")) "script.ts" else "script.js"
                    }
                    viewModel.uploadScript(name, fn, content) { ok, msg ->
                        message = msg
                        if (ok) onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar script")
            }

            message?.let { Text(it) }
        }
    }
}
