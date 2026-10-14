package dev.pocketdev.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pocketdev.app.AppViewModel
import dev.pocketdev.app.core.Endpoints
import dev.pocketdev.app.core.InferenceLocation
import dev.pocketdev.app.core.ProjectNames
import dev.pocketdev.app.core.ServiceState
import dev.pocketdev.app.core.StepState
import dev.pocketdev.app.core.TermuxStatus
import dev.pocketdev.app.setup.SetupCatalog

data class StatusInfo(val label: String, val color: Color, val detail: String?, val busy: Boolean = false)

@Composable
fun termuxInfo(t: TermuxStatus): StatusInfo = when (t) {
    TermuxStatus.Unknown -> StatusInfo("Unknown", MaterialTheme.colorScheme.outline, null)
    TermuxStatus.Checking -> StatusInfo("Checking", MaterialTheme.colorScheme.primary, null, busy = true)
    TermuxStatus.NotInstalled -> StatusInfo("Not installed", StatusColors.warning, "Install Termux from F-Droid or GitHub.")
    TermuxStatus.PlayStoreBuild -> StatusInfo("Wrong build", MaterialTheme.colorScheme.error, "The Play Store version of Termux is outdated and does not support this connection. Reinstall from F-Droid.")
    TermuxStatus.PermissionMissing -> StatusInfo("Permission needed", StatusColors.warning, "Allow PocketDev to run commands in Termux.")
    is TermuxStatus.NotResponding -> StatusInfo("Not responding", MaterialTheme.colorScheme.error, t.detail)
    is TermuxStatus.Ready -> StatusInfo("Connected", StatusColors.success, t.version?.let { "Termux $it" })
}

@Composable
fun HomeScreen(vm: AppViewModel, onOpenSetup: () -> Unit, onOpenEditor: () -> Unit) {
    val s by vm.state.collectAsState()
    val settings by vm.settings.collectAsState()
    var showNew by remember { mutableStateOf(false) }
    val tx = termuxInfo(s.termux)
    val ready = s.termux is TermuxStatus.Ready
    val allToolsReady = SetupCatalog.steps.all { s.steps[it.id]?.state == StepState.Done }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("PocketDev AI", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    s.busy?.let { "$it…" } ?: "Your development environment, checked live.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Build with AI") }
                Text(
                    "Available in the next milestone. This version sets up and verifies the environment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Section("Environment") {
                StatusRow("Termux", tx.color, tx.label, tx.detail, tx.busy)
                SetupCatalog.steps.forEach { step ->
                    val p = s.steps[step.id]
                    val st = p?.state ?: StepState.Unknown
                    StatusRow(step.title, stepColor(st), stepLabel(st), busy = st == StepState.Running || st == StepState.Checking)
                }
                if (!ready || !allToolsReady) {
                    Button(onClick = onOpenSetup, modifier = Modifier.fillMaxWidth()) { Text("Continue setup") }
                }
                OutlinedButton(onClick = { vm.refreshAll() }, enabled = s.busy == null, modifier = Modifier.fillMaxWidth()) { Text("Re-check everything") }
            }
        }
        item {
            Section("Editor") {
                StatusRow("code-server", serviceColor(s.codeServer), s.codeServer.name, s.codeServerDetail, s.codeServer == ServiceState.Checking)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (s.codeServer == ServiceState.Running) {
                        Button(onClick = { vm.openInEditor(null); onOpenEditor() }) { Text("Open editor") }
                        OutlinedButton(onClick = { vm.stopEditor() }, enabled = s.busy == null) { Text("Stop") }
                    } else {
                        Button(onClick = { vm.startEditor() }, enabled = ready && allToolsReady && s.busy == null) { Text("Start editor") }
                    }
                }
                if (!allToolsReady) {
                    Text("Finish setup first so code-server is installed and configured.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Section("AI provider") {
                val loc = Endpoints.inferenceLocation(settings.providerBaseUrl)
                val p = s.provider
                StatusRow(
                    settings.providerKind.label,
                    if (p?.reachable == true) StatusColors.success else MaterialTheme.colorScheme.outline,
                    if (loc == InferenceLocation.LOCAL) "Local" else "Remote",
                    p?.detail ?: "Not checked yet",
                )
                Text(
                    "Ollama is optional. The editor and setup work without any AI provider.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Section("Projects") {
                if (s.projects.isEmpty()) {
                    Text("No projects yet. They live in ~/projects inside Termux.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                s.projects.take(8).forEach { name ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(name, style = MonoStyle.copy(fontSize = 14.sp))
                        TextButton(
                            onClick = { vm.openInEditor(name); onOpenEditor() },
                            enabled = s.codeServer == ServiceState.Running,
                        ) { Text("Open") }
                    }
                }
                Button(onClick = { showNew = true }, enabled = ready && s.busy == null, modifier = Modifier.fillMaxWidth()) { Text("New project") }
            }
        }
        item {
            Section("Device") {
                val d = s.device
                Text("Android API ${d.androidSdk}  •  ${d.abis.joinToString()}", style = MaterialTheme.typography.bodyMedium)
                Text("RAM ${d.totalRamMb} MB  •  Free storage ${d.freeStorageMb} MB", style = MaterialTheme.typography.bodyMedium)
                if (d.abis.none { it.startsWith("arm64") || it.startsWith("x86_64") }) {
                    Text("This CPU architecture may not be supported by current Termux packages.", color = StatusColors.warning, style = MaterialTheme.typography.bodySmall)
                }
                if (d.freeStorageMb < 3000) {
                    Text("Low storage: setup needs a few GB of free space.", color = StatusColors.warning, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Section("Diagnostics") {
                Button(onClick = { vm.runDiagnostics() }, enabled = s.busy == null, modifier = Modifier.fillMaxWidth()) { Text("Run diagnostics") }
                s.diagnostics.forEach { r ->
                    StatusRow(r.name, checkColor(r.status), r.status.name, r.detail.take(300))
                }
            }
        }
    }

    if (showNew) {
        var name by remember { mutableStateOf("") }
        val valid = ProjectNames.isValid(name)
        AlertDialog(
            onDismissRequest = { showNew = false },
            title = { Text("New project") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.trim() },
                    label = { Text("Folder name") },
                    singleLine = true,
                    isError = name.isNotEmpty() && !valid,
                    supportingText = { Text("Letters, numbers, dot, dash, underscore") },
                )
            },
            confirmButton = {
                TextButton(enabled = valid, onClick = { vm.createProject(name); showNew = false }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNew = false }) { Text("Cancel") } },
        )
    }
}
