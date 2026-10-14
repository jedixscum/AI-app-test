package dev.pocketdev.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FB8FF),
    onPrimary = Color(0xFF07204A),
    background = Color(0xFF0F1318),
    onBackground = Color(0xFFE6EAF0),
    surface = Color(0xFF151B22),
    onSurface = Color(0xFFE6EAF0),
    surfaceVariant = Color(0xFF1D252E),
    onSurfaceVariant = Color(0xFFA7B1BF),
    outline = Color(0xFF3A4654),
    error = Color(0xFFFF7A7A),
    onError = Color(0xFF3B0A0A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2B5BD7),
    onPrimary = Color.White,
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF12171D),
    surface = Color.White,
    onSurface = Color(0xFF12171D),
    surfaceVariant = Color(0xFFE9EDF3),
    onSurfaceVariant = Color(0xFF4A5565),
    outline = Color(0xFFB8C0CC),
    error = Color(0xFFB3261E),
)

object StatusColors {
    val success = Color(0xFF5FCB86)
    val warning = Color(0xFFF2C46B)
}

val MonoStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 17.sp)

@Composable
fun PocketDevTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
