package dev.pages.mywinecellar.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colori della cantina, in versione chiara e scura. */
@Immutable
data class CantinaPalette(
    val burgundy: Color,   // superfici del marchio: barra in alto, miniature
    val accent: Color,     // titoli e controlli colorati
    val wineBright: Color, // cuori e link
    val gold: Color,
    val goldSoft: Color,
    val parchment: Color,  // sfondo
    val cream: Color,      // schede e finestre
    val ink: Color,        // testo
    val inkSoft: Color,    // testo secondario
)

val LightPalette = CantinaPalette(
    burgundy = Color(0xFF4A0F1A), accent = Color(0xFF4A0F1A), wineBright = Color(0xFF8E1F36),
    gold = Color(0xFFC9A65C), goldSoft = Color(0xFFEBD9AE),
    parchment = Color(0xFFF4EADA), cream = Color(0xFFFBF6EC),
    ink = Color(0xFF2B1D16), inkSoft = Color(0xFF6B5A50),
)

val DarkPalette = CantinaPalette(
    burgundy = Color(0xFF2E0A12), accent = Color(0xFFE08AA0), wineBright = Color(0xFFE5738C),
    gold = Color(0xFFC9A65C), goldSoft = Color(0xFF4A3B22),
    parchment = Color(0xFF1A1114), cream = Color(0xFF261A1E),
    ink = Color(0xFFF1E6D6), inkSoft = Color(0xFFBFAF9F),
)

val LocalCantina = staticCompositionLocalOf { LightPalette }

/** Testo chiaro sulle superfici del marchio (uguale in chiaro e in scuro). */
val OnBrand = Color(0xFFF4EADA)

val Burgundy: Color @Composable @ReadOnlyComposable get() = LocalCantina.current.burgundy
val Accent: Color @Composable @ReadOnlyComposable get() = LocalCantina.current.accent
val WineBright: Color @Composable @ReadOnlyComposable get() = LocalCantina.current.wineBright
val Gold: Color @Composable @ReadOnlyComposable get() = LocalCantina.current.gold
val GoldSoft: Color @Composable @ReadOnlyComposable get() = LocalCantina.current.goldSoft
val Parchment: Color @Composable @ReadOnlyComposable get() = LocalCantina.current.parchment
val Cream: Color @Composable @ReadOnlyComposable get() = LocalCantina.current.cream
val Ink: Color @Composable @ReadOnlyComposable get() = LocalCantina.current.ink
val InkSoft: Color @Composable @ReadOnlyComposable get() = LocalCantina.current.inkSoft

@Composable
fun CantinaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val p = if (darkTheme) DarkPalette else LightPalette
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF8E1F36), onPrimary = OnBrand,
            secondary = p.gold, onSecondary = Color(0xFF2B1D16),
            background = p.parchment, onBackground = p.ink,
            surface = p.cream, onSurface = p.ink,
            surfaceVariant = p.cream, onSurfaceVariant = p.inkSoft,
            outline = p.gold,
            primaryContainer = p.goldSoft, onPrimaryContainer = p.ink,
            secondaryContainer = p.goldSoft, onSecondaryContainer = p.ink,
            surfaceContainerLowest = p.cream, surfaceContainerLow = p.cream, surfaceContainer = p.cream,
            surfaceContainerHigh = p.cream, surfaceContainerHighest = Color(0xFF33242A),
            surfaceTint = p.wineBright,
            error = Color(0xFFFFB4AB),
        )
    } else {
        lightColorScheme(
            primary = p.burgundy, onPrimary = OnBrand,
            secondary = p.gold, onSecondary = p.ink,
            background = p.parchment, onBackground = p.ink,
            surface = p.cream, onSurface = p.ink,
            surfaceVariant = p.cream, onSurfaceVariant = p.inkSoft,
            outline = p.gold,
            primaryContainer = p.goldSoft, onPrimaryContainer = p.ink,
            secondaryContainer = p.goldSoft, onSecondaryContainer = p.ink,
            surfaceContainerLowest = p.cream, surfaceContainerLow = p.cream, surfaceContainer = p.cream,
            surfaceContainerHigh = p.cream, surfaceContainerHighest = Color(0xFFF1E6D2),
            surfaceTint = p.burgundy,
        )
    }
    CompositionLocalProvider(LocalCantina provides p) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
