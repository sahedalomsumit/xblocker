package com.sahed.xblocker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val XBlockerColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = TextMain,
    primaryContainer = AccentDim,
    onPrimaryContainer = AccentLight,
    secondary = Emerald,
    onSecondary = TextMain,
    secondaryContainer = EmeraldDim,
    onSecondaryContainer = Emerald,
    tertiary = Amber,
    onTertiary = TextMain,
    tertiaryContainer = AmberDim,
    onTertiaryContainer = Amber,
    error = Rose,
    onError = TextMain,
    background = BgDark,
    onBackground = TextMain,
    surface = CardBg,
    onSurface = TextMain,
    surfaceVariant = CardBg2,
    onSurfaceVariant = TextMuted,
    outline = Border,
    outlineVariant = BorderAccent,
    inverseSurface = TextMain,
    inverseOnSurface = BgDark,
    surfaceTint = Accent
)

@Composable
fun XBlockerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = XBlockerColorScheme,
        typography = XBlockerTypography,
        content = content
    )
}
