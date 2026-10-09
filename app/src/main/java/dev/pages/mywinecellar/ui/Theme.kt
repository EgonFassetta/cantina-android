package dev.pages.mywinecellar.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Burgundy = Color(0xFF4A0F1A)
val WineBright = Color(0xFF8E1F36)
val Gold = Color(0xFFC9A65C)
val Parchment = Color(0xFFF4EADA)
val Cream = Color(0xFFFBF6EC)
val GoldSoft = Color(0xFFEBD9AE)
val Ink = Color(0xFF2B1D16)
val InkSoft = Color(0xFF6B5A50)

@Composable
fun CantinaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Burgundy,
            onPrimary = Parchment,
            secondary = Gold,
            onSecondary = Ink,
            background = Parchment,
            onBackground = Ink,
            surface = Cream,
            onSurface = Ink,
            surfaceVariant = Cream,
            onSurfaceVariant = InkSoft,
            outline = Gold,
            primaryContainer = GoldSoft,
            onPrimaryContainer = Ink,
            secondaryContainer = GoldSoft,
            onSecondaryContainer = Ink,
            surfaceContainerLowest = Cream,
            surfaceContainerLow = Cream,
            surfaceContainer = Cream,
            surfaceContainerHigh = Cream,
            surfaceContainerHighest = Color(0xFFF1E6D2),
            surfaceTint = Burgundy,
        ),
        content = content,
    )
}
