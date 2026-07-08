package com.tertiaryinfotech.tapcard.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The 6 card visual themes, ported from the web app's `lib/themes.ts` so a card
 * looks the same across web and Android. Each drives the card-face banner and
 * accent.
 */
data class CardTheme(
    val key: String,
    val label: String,
    val bannerStart: Color,
    val bannerEnd: Color,
    val bannerText: Color,
    val accent: Color,
    val corner: Dp,
)

val CardThemes: List<CardTheme> = listOf(
    CardTheme(
        key = "CORPORATE", label = "Corporate",
        bannerStart = Color(0xFF1E3A8A), bannerEnd = Color(0xFF2563EB),
        bannerText = Color.White, accent = Color(0xFF2563EB), corner = 16.dp,
    ),
    CardTheme(
        key = "MODERN", label = "Modern",
        bannerStart = Color(0xFF7C5CFF), bannerEnd = Color(0xFFC44BFF),
        bannerText = Color.White, accent = Color(0xFF9B87FF), corner = 22.dp,
    ),
    CardTheme(
        key = "MINIMALIST", label = "Minimalist",
        bannerStart = Color(0xFFF4F4F5), bannerEnd = Color(0xFFE7E7EA),
        bannerText = Color(0xFF111111), accent = Color(0xFF111111), corner = 14.dp,
    ),
    CardTheme(
        key = "DARK", label = "Dark",
        bannerStart = Color(0xFF18181B), bannerEnd = Color(0xFF2A2A35),
        bannerText = Color.White, accent = Color(0xFF22D3EE), corner = 18.dp,
    ),
    CardTheme(
        key = "CREATIVE", label = "Creative",
        bannerStart = Color(0xFFFB7185), bannerEnd = Color(0xFFF59E0B),
        bannerText = Color.White, accent = Color(0xFFEA580C), corner = 26.dp,
    ),
    CardTheme(
        key = "LUXURY", label = "Luxury",
        bannerStart = Color(0xFF1A1407), bannerEnd = Color(0xFF3A2F1A),
        bannerText = Color(0xFFF7EFE0), accent = Color(0xFFD4AF37), corner = 16.dp,
    ),
)

/** Resolve a theme by key, defaulting to Modern. */
fun cardTheme(key: String?): CardTheme =
    CardThemes.firstOrNull { it.key == key } ?: CardThemes.first { it.key == "MODERN" }
