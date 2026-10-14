package dev.pocketdev.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.pocketdev.app.AppViewModel
import dev.pocketdev.app.core.Endpoints
import dev.pocketdev.app.core.InferenceLocation
import dev.pocketdev.app.core.ProviderKind

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val saved by vm.settings.collectAsState()
    val s by vm.state.collectAsState()
    var kind by remember(saved.providerKind) { mutableStateOf(saved.providerKind) }
    var url by remember(saved.providerBaseUrl) { mutableStateOf(saved.providerBaseUrl) }
    val loc = Endpoints.inferenceLocation(url)

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold) }
        item {
            Section("AI provider") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProviderKind.entries.forEach { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = { kind = k; if (url.isBlank() || url == ProviderKind.entries.firstOrNull { it != k }?.defaultUrl) url = k.defaultUrl },
                            label = { Text(k.label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (loc == InferenceLocation.LOCAL) "Local: requests stay on this device."
                    else "Remote: prompts and code sent here leave this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (loc == InferenceLocation.LOCAL) StatusColors.success else StatusColors.warning,
                )
                Text(
                    "Plain HTTP is only allowed for 127.0.0.1 and localhost. Remote endpoints must use HTTPS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.saveProvider(kind, url) }, enabled = s.busy == null) { Text("Save and test") }
                    OutlinedButton(onClick = { vm.checkProvider() }, enabled = s.busy == null) { Text("Test only") }
                }
                s.provider?.let { Text(it.detail, style = MaterialTheme.typography.bodySmall) }
            }
        }
        item {
            Section("About this version") {
                Text("Milestone 1: Termux connection, setup, code-server editor, diagnostics and logs.", style = MaterialTheme.typography.bodyMedium)
                Text("Not included yet: model downloads, the AI coding agent, GitHub sign-in, builds and APK export.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
