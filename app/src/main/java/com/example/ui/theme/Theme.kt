package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** The selectable colour palettes offered under Settings > Themes. */
enum class LauncherTheme(val key: String, val label: String) {
    DEFAULT("default", "Default"),
    PINK("pink", "Pink"),
    GREEN("green", "Green"),
    SAFFRON("saffron", "Saffron"),
    SEATTLE("seattle", "Seattle"),
    OCEAN("ocean", "Ocean"),
    LAVENDER("lavender", "Lavender");

    companion object {
        fun fromKey(key: String?): LauncherTheme =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

private val DefaultLight = lightColorScheme(
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

private val DefaultDark = darkColorScheme(
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

private val PinkLight = lightColorScheme(
    primary = Color(0xFFC2185B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE4EC),
    onPrimaryContainer = Color(0xFF3E001D),
    background = Color(0xFFFFF5F8),
    onBackground = Color(0xFF2A1A20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2A1A20),
    surfaceVariant = Color(0xFFFCE4EC),
    onSurfaceVariant = Color(0xFF5A3B45),
    outline = Color(0xFFEAC7D2),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val PinkDark = darkColorScheme(
    primary = Color(0xFFF48FB1),
    onPrimary = Color(0xFF4A0026),
    primaryContainer = Color(0xFF5A1B33),
    onPrimaryContainer = Color(0xFFFFD9E2),
    background = Color(0xFF1A1014),
    onBackground = Color(0xFFF0DDE4),
    surface = Color(0xFF241419),
    onSurface = Color(0xFFF0DDE4),
    surfaceVariant = Color(0xFF2E1A20),
    onSurfaceVariant = Color(0xFFD8BEC6),
    outline = Color(0xFF44323A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val GreenLight = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F1E4),
    onPrimaryContainer = Color(0xFF0A2E10),
    background = Color(0xFFF3F9F3),
    onBackground = Color(0xFF1A241B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A241B),
    surfaceVariant = Color(0xFFE3F1E4),
    onSurfaceVariant = Color(0xFF3F4A40),
    outline = Color(0xFFC8DCC9),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val GreenDark = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF0A3810),
    primaryContainer = Color(0xFF1B4D22),
    onPrimaryContainer = Color(0xFFA5D6A7),
    background = Color(0xFF0F1511),
    onBackground = Color(0xFFDCE8DD),
    surface = Color(0xFF18211A),
    onSurface = Color(0xFFDCE8DD),
    surfaceVariant = Color(0xFF1F2A20),
    onSurfaceVariant = Color(0xFFC0CCC1),
    outline = Color(0xFF37443A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val SaffronLight = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFDE6CC),
    onPrimaryContainer = Color(0xFF3D1A00),
    background = Color(0xFFFFF8F0),
    onBackground = Color(0xFF2A1E12),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2A1E12),
    surfaceVariant = Color(0xFFFDECD8),
    onSurfaceVariant = Color(0xFF5A4433),
    outline = Color(0xFFE7D3BC),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val SaffronDark = darkColorScheme(
    primary = Color(0xFFFFB74D),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF6B3E00),
    onPrimaryContainer = Color(0xFFFFDDB3),
    background = Color(0xFF1A1207),
    onBackground = Color(0xFFF0E2D0),
    surface = Color(0xFF241A0E),
    onSurface = Color(0xFFF0E2D0),
    surfaceVariant = Color(0xFF2E2314),
    onSurfaceVariant = Color(0xFFD6C4AE),
    outline = Color(0xFF463828),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val SeattleLight = lightColorScheme(
    primary = Color(0xFF37474F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E9ED),
    onPrimaryContainer = Color(0xFF12191D),
    background = Color(0xFFF2F5F7),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFE3E9ED),
    onSurfaceVariant = Color(0xFF40484C),
    outline = Color(0xFFCBD3D8),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val SeattleDark = darkColorScheme(
    primary = Color(0xFFB0BEC5),
    onPrimary = Color(0xFF1B2A32),
    primaryContainer = Color(0xFF2B3A42),
    onPrimaryContainer = Color(0xFFCCD9E0),
    background = Color(0xFF0F1214),
    onBackground = Color(0xFFDEE3E6),
    surface = Color(0xFF171C1F),
    onSurface = Color(0xFFDEE3E6),
    surfaceVariant = Color(0xFF1F262A),
    onSurfaceVariant = Color(0xFFBEC7CC),
    outline = Color(0xFF3A444A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val OceanLight = lightColorScheme(
    primary = Color(0xFF0277BD),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1F0F8),
    onPrimaryContainer = Color(0xFF00263A),
    background = Color(0xFFF2F9FD),
    onBackground = Color(0xFF12222B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF12222B),
    surfaceVariant = Color(0xFFE1EEF5),
    onSurfaceVariant = Color(0xFF3A4A52),
    outline = Color(0xFFC6DBE6),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val OceanDark = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF00344E),
    primaryContainer = Color(0xFF0C4A66),
    onPrimaryContainer = Color(0xFFB3E5FC),
    background = Color(0xFF0A1216),
    onBackground = Color(0xFFD6E6EE),
    surface = Color(0xFF111C21),
    onSurface = Color(0xFFD6E6EE),
    surfaceVariant = Color(0xFF18262D),
    onSurfaceVariant = Color(0xFFB8CAD3),
    outline = Color(0xFF33454D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LavenderLight = lightColorScheme(
    primary = Color(0xFF6A4C93),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFECE4F6),
    onPrimaryContainer = Color(0xFF22103A),
    background = Color(0xFFF8F5FC),
    onBackground = Color(0xFF1E1826),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E1826),
    surfaceVariant = Color(0xFFEDE6F6),
    onSurfaceVariant = Color(0xFF4A4256),
    outline = Color(0xFFD6CBE8),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val LavenderDark = darkColorScheme(
    primary = Color(0xFFC3A9E8),
    onPrimary = Color(0xFF33195A),
    primaryContainer = Color(0xFF4A2E73),
    onPrimaryContainer = Color(0xFFEADDFF),
    background = Color(0xFF130E1A),
    onBackground = Color(0xFFE8DEF2),
    surface = Color(0xFF1B1424),
    onSurface = Color(0xFFE8DEF2),
    surfaceVariant = Color(0xFF241A2F),
    onSurfaceVariant = Color(0xFFCFC3DC),
    outline = Color(0xFF443A50),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

fun colorSchemeFor(theme: LauncherTheme, dark: Boolean): ColorScheme = when (theme) {
    LauncherTheme.DEFAULT -> if (dark) DefaultDark else DefaultLight
    LauncherTheme.PINK -> if (dark) PinkDark else PinkLight
    LauncherTheme.GREEN -> if (dark) GreenDark else GreenLight
    LauncherTheme.SAFFRON -> if (dark) SaffronDark else SaffronLight
    LauncherTheme.SEATTLE -> if (dark) SeattleDark else SeattleLight
    LauncherTheme.OCEAN -> if (dark) OceanDark else OceanLight
    LauncherTheme.LAVENDER -> if (dark) LavenderDark else LavenderLight
}

/** Preview swatch colour for the Themes picker. */
fun previewColorFor(theme: LauncherTheme, dark: Boolean): Color = colorSchemeFor(theme, dark).primary

@Composable
fun FocusLauncherTheme(
    darkTheme: Boolean,
    theme: LauncherTheme = LauncherTheme.DEFAULT,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorSchemeFor(theme, darkTheme),
        typography = Typography,
        content = content
    )
}
