package dev.pocketdev.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.pocketdev.app.AppViewModel
import dev.pocketdev.app.core.StepState
import dev.pocketdev.app.core.TermuxStatus
import dev.pocketdev.app.setup.SetupCatalog
import dev.pocketdev.app.termux.TermuxConstants

private fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
fun SetupScreen(vm: AppViewModel, onOpenKeepAlive: () -> Unit) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refreshTermux() }
    val ready = s.termux is TermuxStatus.Ready
    val tx = termuxInfo(s.termux)

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Setup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            Section("1. Connect to Termux") {
                StatusRow("Termux", tx.color, tx.label, tx.detail, tx.busy)
                when (val t = s.termux) {
                    TermuxStatus.NotInstalled, TermuxStatus.PlayStoreBuild -> {
                        Text("Install Termux from F-Droid (or its GitHub releases). The Play Store build is outdated.", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { openUrl(context, TermuxConstants.F_DROID_URL) }, modifier = Modifier.fillMaxWidth()) { Text("Open Termux on F-Droid") }
                    }
                    TermuxStatus.PermissionMissing -> {
                        Text("Android will ask whether PocketDev may run commands in Termux.", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { permissionLauncher.launch(TermuxConstants.PERMISSION) }, modifier = Modifier.fillMaxWidth()) { Text("Grant permission") }
                    }
                    is TermuxStatus.NotResponding -> {
                        Text("Termux must allow external apps. Open Termux, paste this command, press Enter, then fully close and reopen Termux:", style = MaterialTheme.typography.bodyMedium)
                        Text(TermuxConstants.ENABLE_EXTERNAL_APPS_CMD, style = MonoStyle)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(TermuxConstants.ENABLE_EXTERNAL_APPS_CMD)) }) { Text("Copy command") }
                            OutlinedButton(onClick = {
                                context.packageManager.getLaunchIntentForPackage(TermuxConstants.PACKAGE)?.let { context.startActivity(it) }
                            }) { Text("Open Termux") }
                        }
                    }
                    else -> if (t is TermuxStatus.Ready) {
                        Text("Termux ran a command and returned the result. The connection works.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedButton(onClick = { vm.refreshTermux() }, enabled = s.busy == null, modifier = Modifier.fillMaxWidth()) { Text("Test connection") }
            }
        }
        item {
            Button(
                onClick = { vm.runAllMissing() },
                enabled = ready && s.busy == null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Install everything that is missing") }
        }
        items(SetupCatalog.steps, key = { it.id }) { step ->
            val p = s.steps[step.id]
            val st = p?.state ?: StepState.Unknown
            Section(step.title) {
                StatusRow(step.sizeHint, stepColor(st), stepLabel(st), busy = st == StepState.Running || st == StepState.Checking)
                Text(step.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!p?.output.isNullOrBlank()) {
                    Text(p!!.output, style = MonoStyle, modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
                }
                if (st != StepState.Done) {
                    Button(
                        onClick = { vm.runStep(step) },
                        enabled = ready && s.busy == null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (st == StepState.Failed) "Retry" else "Install") }
                }
            }
        }
        item {
            Section("Keep services running") {
                Text(
                    "Android can stop background processes such as code-server. Check your phone and follow the fixes if it does.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onOpenKeepAlive, modifier = Modifier.fillMaxWidth()) { Text("Open keep-alive guide") }
            }
        }
        item {
            Text(
                "Installs run inside Termux and continue if you leave this screen, but this app only shows progress while it is open. " +
                    "Success is reported only after a separate check confirms the tool works.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
