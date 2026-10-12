package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

data class AppChipPalette(
    val backgroundLight: Color,
    val textLight: Color,
    val backgroundDark: Color,
    val textDark: Color,
    val iconCircleLight: Color,
    val iconCircleDark: Color,
)

object AppColors {

    val palettes = listOf(
        // 0: Soft Teal / Mint (e.g. AI Gallery)
        AppChipPalette(
            backgroundLight = Color(0xFFC7EFEF),
            textLight = Color(0xFF0D5F60),
            backgroundDark = Color(0xFF1B3D3D),
            textDark = Color(0xFF94E8E8),
            iconCircleLight = Color(0xFF009688),
            iconCircleDark = Color(0xFF26A69A),
        ),
        // 1: Soft Powder Blue (e.g. Chrome / Messages)
        AppChipPalette(
            backgroundLight = Color(0xFFCCE2FF),
            textLight = Color(0xFF1B4E8C),
            backgroundDark = Color(0xFF1C314D),
            textDark = Color(0xFFA1CAFF),
            iconCircleLight = Color(0xFF2979FF),
            iconCircleDark = Color(0xFF448AFF),
        ),
        // 2: Soft Coral / Rose (e.g. Music Player / Gmail)
        AppChipPalette(
            backgroundLight = Color(0xFFFFD4D4),
            textLight = Color(0xFF9C2929),
            backgroundDark = Color(0xFF452222),
            textDark = Color(0xFFFFB4B4),
            iconCircleLight = Color(0xFFE53935),
            iconCircleDark = Color(0xFFEF5350),
        ),
        // 3: Soft Golden / Mustard (e.g. Kumar Edutainment / Drive)
        AppChipPalette(
            backgroundLight = Color(0xFFFBE8BD),
            textLight = Color(0xFF805B07),
            backgroundDark = Color(0xFF3E331B),
            textDark = Color(0xFFFFDF9E),
            iconCircleLight = Color(0xFFD4A017),
            iconCircleDark = Color(0xFFFFB300),
        ),
        // 4: Soft Pastel Green / Mint (e.g. WhatsApp / Phone)
        AppChipPalette(
            backgroundLight = Color(0xFFD0F4DE),
            textLight = Color(0xFF1B643A),
            backgroundDark = Color(0xFF1D3B2A),
            textDark = Color(0xFFA0EBB9),
            iconCircleLight = Color(0xFF2E7D32),
            iconCircleDark = Color(0xFF4CAF50),
        ),
        // 5: Soft Lavender / Periwinkle (e.g. SP Browser)
        AppChipPalette(
            backgroundLight = Color(0xFFE3DCFC),
            textLight = Color(0xFF51388E),
            backgroundDark = Color(0xFF2E2648),
            textDark = Color(0xFFCEBFFB),
            iconCircleLight = Color(0xFF5C6BC0),
            iconCircleDark = Color(0xFF7986CB),
        ),
        // 6: Neutral Cool Gray (e.g. Camera / Clock)
        AppChipPalette(
            backgroundLight = Color(0xFFE5E5E5),
            textLight = Color(0xFF4E5155),
            backgroundDark = Color(0xFF2E3033),
            textDark = Color(0xFFD4D6DA),
            iconCircleLight = Color(0xFF78909C),
            iconCircleDark = Color(0xFF90A4AE),
        ),
        // 7: Warm Sand / Taupe (e.g. Notepad Free / Downloads)
        AppChipPalette(
            backgroundLight = Color(0xFFE8E2D7),
            textLight = Color(0xFF635643),
            backgroundDark = Color(0xFF332F28),
            textDark = Color(0xFFDFD7CA),
            iconCircleLight = Color(0xFF8D6E63),
            iconCircleDark = Color(0xFFA1887F),
        ),
        // 8: Deep Indigo / Violet
        AppChipPalette(
            backgroundLight = Color(0xFFD8E1FF),
            textLight = Color(0xFF2B3E84),
            backgroundDark = Color(0xFF212847),
            textDark = Color(0xFFB5C5FF),
            iconCircleLight = Color(0xFF3F51B5),
            iconCircleDark = Color(0xFF5C6BC0),
        ),
        // 9: Emerald Cyan
        AppChipPalette(
            backgroundLight = Color(0xFFC7F3EA),
            textLight = Color(0xFF156554),
            backgroundDark = Color(0xFF183B33),
            textDark = Color(0xFF97E5D4),
            iconCircleLight = Color(0xFF00897B),
            iconCircleDark = Color(0xFF26A69A),
        )
    )

    fun getPaletteFor(key: String): AppChipPalette {
        val index = (key.hashCode().absoluteValue) % palettes.size
        return palettes[index]
    }
}
