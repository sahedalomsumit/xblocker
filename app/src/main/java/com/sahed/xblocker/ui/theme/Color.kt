package com.sahed.xblocker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme

// Brand core
val Accent = Color(0xFF8B5CF6)         // Purple accent
val AccentLight = Color(0xFFA78BFA)    // Light purple
val AccentDim = Color(0x338B5CF6)      // Translucent purple (8%)

val Emerald = Color(0xFF10B981)        // Active/success green
val EmeraldDim = Color(0x2210B981)     // Translucent emerald

val Amber = Color(0xFFF59E0B)          // Warning/timer
val AmberDim = Color(0x22F59E0B)

val Rose = Color(0xFFEF4444)           // Danger/blocked


// Backgrounds
val BgDark: Color
    @Composable
    get() = MaterialTheme.colorScheme.background

val BgDark2: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background == Color(0xFF050505)) Color(0xFF0A0A0A) else Color(0xFFF3F4F6)

val CardBg: Color
    @Composable
    get() = MaterialTheme.colorScheme.surface

val CardBg2: Color
    @Composable
    get() = MaterialTheme.colorScheme.surfaceVariant

val SurfaceOverlay: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background == Color(0xFF050505)) Color(0xFF1A1A2E) else Color(0xFFEDE9FE)

// Borders & dividers
val Border: Color
    @Composable
    get() = MaterialTheme.colorScheme.outline

val BorderAccent: Color
    @Composable
    get() = MaterialTheme.colorScheme.outlineVariant

// Text
val TextMain: Color
    @Composable
    get() = MaterialTheme.colorScheme.onBackground

val TextSub: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background == Color(0xFF050505)) Color(0xFFE2E8F0) else Color(0xFF1F2937)

val TextMuted: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val TextDisabled: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background == Color(0xFF050505)) Color(0xFF475569) else Color(0xFF9CA3AF)

// Status chip backgrounds
val ChipActive: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background == Color(0xFF050505)) Color(0xFF064E3B) else Color(0xFFD1FAE5)

val ChipInactive: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background == Color(0xFF050505)) Color(0xFF1C1C1C) else Color(0xFFE5E7EB)

// Gradient colors
val GradientPurpleStart = Color(0xFF6D28D9)
val GradientPurpleEnd = Color(0xFF8B5CF6)
val GradientCardStart: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background == Color(0xFF050505)) Color(0xFF0D0D0D) else Color(0xFFFFFFFF)
val GradientCardEnd: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background == Color(0xFF050505)) Color(0xFF1A0D2E) else Color(0xFFEDE9FE)
