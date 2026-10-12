package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF2ECE7),
    onPrimaryContainer = Color(0xFF1C1B1F),
    background = Color(0xFFFAF7F5),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF3EBE6),
    onSurfaceVariant = Color(0xFF49454E),
    outline = Color(0xFFE2DDD8),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE4E1E6),
    onPrimary = Color(0xFF1B1B1F),
    primaryContainer = Color(0xFF2C2C30),
    onPrimaryContainer = Color(0xFFE4E1E6),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEAEAEA),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFEAEAEA),
    surfaceVariant = Color(0xFF252528),
    onSurfaceVariant = Color(0xFFC7C5D0),
    outline = Color(0xFF38383C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun FocusLauncherTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
