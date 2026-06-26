package com.sahed.xblocker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val XBlockerDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0x338B5CF6),
    onPrimaryContainer = Color(0xFFA78BFA),
    secondary = Color(0xFF10B981),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0x2210B981),
    onSecondaryContainer = Color(0xFF10B981),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0x22F59E0B),
    onTertiaryContainer = Color(0xFFF59E0B),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    background = Color(0xFF050505),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0D0D0D),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0x14FFFFFF),
    outlineVariant = Color(0x338B5CF6),
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF050505),
    surfaceTint = Color(0xFF8B5CF6)
)

private val XBlockerLightColorScheme = lightColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF6D28D9),
    secondary = Color(0xFF10B981),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF92400E),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    background = Color(0xFFF9FAFB),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0x14111827),
    outlineVariant = Color(0xFFDDD6FE),
    inverseSurface = Color(0xFF111827),
    inverseOnSurface = Color(0xFFF9FAFB),
    surfaceTint = Color(0xFF8B5CF6)
)

@Composable
fun XBlockerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) XBlockerDarkColorScheme else XBlockerLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = XBlockerTypography,
        content = content
    )
}
