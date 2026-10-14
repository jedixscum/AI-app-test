package dev.pocketdev.app.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.pocketdev.app.core.TermuxStatus

private const val PHANTOM_CMD_A12_13 =
    "adb shell \"/system/bin/device_config set_sync_disabled_for_tests persistent; " +
        "/system/bin/device_config put activity_manager max_phantom_processes 2147483647\""

private fun launch(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
fun KeepAliveScreen(vm: AppViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val ready = s.termux is TermuxStatus.Ready
    val sdk = Build.VERSION.SDK_INT

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = onBack) { Text("Back to Setup") }
            Text("Keep services running", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Android 12 and newer may kill background processes started from Termux, which would stop code-server mid-session. " +
                    "This page tests whether that happens on your phone and lists the fixes. The fixes are suggestions; they are not applied automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Section("1. Test your phone") {
                Text(
                    "Start a harmless background process in Termux, use your phone normally, then check whether it is still alive. " +
                        "Try again after 30 minutes with the screen off for a meaningful result.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.startKeepAliveTest() }, enabled = ready && s.busy == null) { Text("Start test") }
                    OutlinedButton(onClick = { vm.checkKeepAliveTest() }, enabled = ready && s.busy == null) { Text("Check result") }
                }
                if (!ready) Text("Connect Termux first (Setup tab).", style = MaterialTheme.typography.bodySmall, color = StatusColors.warning)
            }
        }
        item {
            Section("2. Battery settings (all Android versions)") {
                Text(
                    "Set both Termux and PocketDev AI to unrestricted or not optimized. Wording varies by manufacturer, and some brands add extra background limits of their own.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(
                    onClick = { launch(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Open battery optimization list") }
                Text(
                    "Termux can also hold a wake lock from its notification. Expand the Termux notification and tap Acquire wakelock.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Section("3. Child process limit (Android $sdk on this phone)") {
                if (sdk >= 34) {
                    Text(
                        "Android 14 and newer: turn on Developer options, then enable \"Disable child process restrictions\".",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(
                        onClick = { launch(context, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Open Developer options") }
                    Text(
                        "If Developer options is hidden, tap Build number seven times in About phone. The exact setting name may differ by manufacturer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (sdk >= 31) {
                    Text(
                        "Android 12 and 13 have no toggle for this. The usual fix is one ADB command, run from a computer or from another phone over wireless debugging:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(PHANTOM_CMD_A12_13, style = MonoStyle)
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(PHANTOM_CMD_A12_13)) }) { Text("Copy command") }
                    Text(
                        "This is a community-documented workaround, not an official Android feature. It may stop working after updates and can need repeating after a reboot.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "Android 11 and older do not have this limit. The battery settings above are usually enough.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        item {
            Section("Optional: Shizuku") {
                Text(
                    "Shizuku could apply the step-3 setting without a computer, but it needs wireless debugging pairing and is not required. " +
                        "PocketDev AI does not use it yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
