package app.quieta.notiflab.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3482FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E7FF),
    onPrimaryContainer = Color(0xFF0B2A4A),
    surface = Color(0xFFF7F7F8),
    onSurface = Color(0xFF1B1B1F),
    onSurfaceVariant = Color(0xFF5B5B60),
    background = Color(0xFFF2F3F5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FBBFF),
    onPrimary = Color(0xFF00306A),
    primaryContainer = Color(0xFF1A3A5C),
    onPrimaryContainer = Color(0xFFD6E7FF),
    surface = Color(0xFF1B1B1F),
    onSurface = Color(0xFFE6E1E6),
    onSurfaceVariant = Color(0xFFC7C5CA),
    background = Color(0xFF121214),
)

@Composable
fun NotiflabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
