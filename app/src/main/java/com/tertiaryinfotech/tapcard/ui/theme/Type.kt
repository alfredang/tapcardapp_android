@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.tertiaryinfotech.tapcard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.R

// Variable-font helpers — one TTF per family, weight pinned via FontVariation.
private fun jakarta(weight: Int) = Font(
    R.font.plus_jakarta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Primary UI typeface — clean, modern, great at bold weights. */
val JakartaFamily = FontFamily(
    jakarta(400), jakarta(500), jakarta(600), jakarta(700), jakarta(800),
)

/**
 * Display typeface for big headlines. Unified to Plus Jakarta Sans (heavy
 * weights) so the whole app shares one typeface, headlines just carry more weight.
 */
val DisplayFontFamily = FontFamily(
    jakarta(600), jakarta(700), jakarta(800),
)

/**
 * Bold, modern type scale. Big headlines use Space Grotesk; everything else is
 * Plus Jakarta Sans. Tight tracking on large sizes for that premium feel.
 */
val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
        displayMedium = displayMedium.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        displaySmall = displaySmall.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineLarge = headlineLarge.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineSmall = headlineSmall.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleLarge = titleLarge.copy(fontFamily = JakartaFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        titleMedium = titleMedium.copy(fontFamily = JakartaFamily, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontFamily = JakartaFamily, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = JakartaFamily),
        bodyMedium = bodyMedium.copy(fontFamily = JakartaFamily),
        bodySmall = bodySmall.copy(fontFamily = JakartaFamily),
        labelLarge = labelLarge.copy(fontFamily = JakartaFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
        labelMedium = labelMedium.copy(fontFamily = JakartaFamily, fontWeight = FontWeight.SemiBold),
        labelSmall = labelSmall.copy(fontFamily = JakartaFamily, fontWeight = FontWeight.SemiBold),
    )
}

/** Default text style so every bare Text() inherits the brand typeface. */
val DefaultBrandTextStyle = TextStyle(fontFamily = JakartaFamily)
