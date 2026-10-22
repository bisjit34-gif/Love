package com.riptika.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.riptika.app.voice.Listener

class MainActivity : ComponentActivity() {
    private val vm: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val listener = Listener(this)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    var input by remember { mutableStateOf("") }
                    var lang by remember { mutableStateOf("bn-BD") }
                    var showSettings by remember { mutableStateOf(false) }
                    val listState = rememberLazyListState()
                    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
                        if (ok) listener.start(lang, { vm.send(it) }, { vm.messages.add(Msg("assistant", it)) })
                    }
                    LaunchedEffect(vm.messages.size) {
                        if (vm.messages.isNotEmpty()) listState.animateScrollToItem(vm.messages.size - 1)
                    }
                    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("RIPTIKA", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            TextButton(onClick = { lang = if (lang == "bn-BD") "hi-IN" else "bn-BD" }) {
                                Text(if (lang == "bn-BD") "বাংলা" else "हिन्दी")
                            }
                            TextButton(onClick = { showSettings = true }) { Text("Settings") }
                        }
                        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState) {
                            items(vm.messages) { m ->
                                val mine = m.role == "user"
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = if (mine) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ) { Text(m.text, Modifier.padding(10.dp).widthIn(max = 300.dp)) }
                                }
                            }
                        }
                        if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Message...") })
                            TextButton(onClick = { micPerm.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Mic") }
                            Button(onClick = { vm.send(input); input = "" }) { Text("Send") }
                        }
                    }
                    val ct = vm.confirmText
                    if (ct != null) {
                        AlertDialog(
                            onDismissRequest = { vm.answer(false) },
                            title = { Text("Allow action?") },
                            text = { Text(ct) },
                            confirmButton = { TextButton(onClick = { vm.answer(true) }) { Text("Allow") } },
                            dismissButton = { TextButton(onClick = { vm.answer(false) }) { Text("Deny") } }
                        )
                    }
                    if (showSettings) {
                        var url by remember { mutableStateOf(vm.cfg.baseUrl) }
                        var tok by remember { mutableStateOf(vm.cfg.token) }
                        AlertDialog(
                            onDismissRequest = { showSettings = false },
                            title = { Text("Settings") },
                            text = {
                                Column {
                                    OutlinedTextField(url, { url = it }, label = { Text("Backend URL") }, singleLine = true)
                                    OutlinedTextField(tok, { tok = it }, label = { Text("App token") }, singleLine = true)
                                    TextButton(onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                                        Text("Enable screen reading (Accessibility)")
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { vm.cfg.baseUrl = url; vm.cfg.token = tok; showSettings = false }) { Text("Save") }
                            }
                        )
                    }
                }
            }
        }
    }
}
