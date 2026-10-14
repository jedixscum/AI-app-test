package dev.pocketdev.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.pocketdev.app.core.CheckStatus
import dev.pocketdev.app.core.ServiceState
import dev.pocketdev.app.core.StepState

@Composable
fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
fun Dot(color: Color) {
    Box(Modifier.size(10.dp).background(color, CircleShape))
}

@Composable
fun StatusRow(label: String, color: Color, status: String, detail: String? = null, busy: Boolean = false) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.padding(top = 5.dp).size(12.dp), contentAlignment = Alignment.Center) {
            if (busy) CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp) else Dot(color)
        }
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(status, style = MaterialTheme.typography.bodyMedium, color = color)
            }
            if (!detail.isNullOrBlank()) {
                Spacer(Modifier.size(2.dp))
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun stepColor(s: StepState): Color = when (s) {
    StepState.Done -> StatusColors.success
    StepState.Failed -> MaterialTheme.colorScheme.error
    StepState.Running, StepState.Checking -> MaterialTheme.colorScheme.primary
    StepState.Missing -> StatusColors.warning
    StepState.Unknown -> MaterialTheme.colorScheme.outline
}

fun stepLabel(s: StepState): String = when (s) {
    StepState.Done -> "Ready"
    StepState.Failed -> "Failed"
    StepState.Running -> "Installing"
    StepState.Checking -> "Checking"
    StepState.Missing -> "Not installed"
    StepState.Unknown -> "Unknown"
}

@Composable
fun serviceColor(s: ServiceState): Color = when (s) {
    ServiceState.Running -> StatusColors.success
    ServiceState.Error -> MaterialTheme.colorScheme.error
    ServiceState.Checking -> MaterialTheme.colorScheme.primary
    ServiceState.Stopped -> StatusColors.warning
    ServiceState.Unknown -> MaterialTheme.colorScheme.outline
}

@Composable
fun checkColor(s: CheckStatus): Color = when (s) {
    CheckStatus.Pass -> StatusColors.success
    CheckStatus.Fail -> MaterialTheme.colorScheme.error
    CheckStatus.Skipped -> MaterialTheme.colorScheme.outline
}
