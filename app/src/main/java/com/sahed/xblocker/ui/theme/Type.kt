package com.sahed.xblocker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.sahed.xblocker.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val fontName = GoogleFont("Inter")

val InterFont = FontFamily(
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Bold),
)

val XBlockerTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        color = TextMain
    ),
    displayMedium = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        color = TextMain
    ),
    headlineLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        color = TextMain
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        color = TextMain
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        color = TextMain
    ),
    titleLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        color = TextMain
    ),
    titleMedium = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = TextMain
    ),
    titleSmall = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextSub
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = TextSub
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextMuted
    ),
    bodySmall = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextMuted
    ),
    labelLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextMain
    ),
    labelMedium = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextMuted
    ),
    labelSmall = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        color = TextDisabled
    ),
)
