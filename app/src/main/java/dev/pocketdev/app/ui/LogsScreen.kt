package dev.pocketdev.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.pocketdev.app.AppViewModel
import dev.pocketdev.app.data.LogLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(vm: AppViewModel) {
    val entries by vm.logEntries.collectAsState()
    val context = LocalContext.current
    val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, vm.exportLogs())
                context.startActivity(Intent.createChooser(send, "Export logs (secrets redacted)"))
            }) { Text("Export") }
            OutlinedButton(onClick = { vm.clearLogs() }) { Text("Clear") }
        }
        if (entries.isEmpty()) {
            Text("No log entries yet.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(entries.reversed()) { e ->
                val color = when (e.level) {
                    LogLevel.Error -> MaterialTheme.colorScheme.error
                    LogLevel.Warn -> StatusColors.warning
                    LogLevel.Info -> MaterialTheme.colorScheme.onSurface
                }
                Text("${fmt.format(Date(e.time))} [${e.source}] ${e.message}", style = MonoStyle, color = color)
            }
        }
    }
}
