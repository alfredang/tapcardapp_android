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
        bannerStart = Color(0xFF6A47F5), bannerEnd = Color(0xFFF86E59),
        bannerText = Color.White, accent = Color(0xFF6A47F5), corner = 22.dp,
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
    CardTheme(
        key = "OCEAN", label = "Ocean",
        bannerStart = Color(0xFF0891B2), bannerEnd = Color(0xFF2563EB),
        bannerText = Color.White, accent = Color(0xFF0E7490), corner = 20.dp,
    ),
    CardTheme(
        key = "FOREST", label = "Forest",
        bannerStart = Color(0xFF166534), bannerEnd = Color(0xFF22C55E),
        bannerText = Color.White, accent = Color(0xFF16A34A), corner = 18.dp,
    ),
    CardTheme(
        key = "SUNSET", label = "Sunset",
        bannerStart = Color(0xFFF97316), bannerEnd = Color(0xFFEC4899),
        bannerText = Color.White, accent = Color(0xFFEA580C), corner = 24.dp,
    ),
    CardTheme(
        key = "ROSE", label = "Rose",
        bannerStart = Color(0xFFE11D48), bannerEnd = Color(0xFFFB7185),
        bannerText = Color.White, accent = Color(0xFFE11D48), corner = 22.dp,
    ),
    CardTheme(
        key = "INDIGO", label = "Indigo",
        bannerStart = Color(0xFF4338CA), bannerEnd = Color(0xFF818CF8),
        bannerText = Color.White, accent = Color(0xFF4F46E5), corner = 18.dp,
    ),
    CardTheme(
        key = "TEAL", label = "Teal",
        bannerStart = Color(0xFF0D9488), bannerEnd = Color(0xFF2DD4BF),
        bannerText = Color.White, accent = Color(0xFF0D9488), corner = 20.dp,
    ),
    CardTheme(
        key = "AMBER", label = "Amber",
        bannerStart = Color(0xFFD97706), bannerEnd = Color(0xFFFBBF24),
        bannerText = Color.White, accent = Color(0xFFD97706), corner = 18.dp,
    ),
    CardTheme(
        key = "CRIMSON", label = "Crimson",
        bannerStart = Color(0xFFB91C1C), bannerEnd = Color(0xFFEF4444),
        bannerText = Color.White, accent = Color(0xFFDC2626), corner = 16.dp,
    ),
    CardTheme(
        key = "LAVENDER", label = "Lavender",
        bannerStart = Color(0xFFA78BFA), bannerEnd = Color(0xFFF0ABFC),
        bannerText = Color.White, accent = Color(0xFF8B5CF6), corner = 24.dp,
    ),
    CardTheme(
        key = "MIDNIGHT", label = "Midnight",
        bannerStart = Color(0xFF1E293B), bannerEnd = Color(0xFF0EA5E9),
        bannerText = Color.White, accent = Color(0xFF38BDF8), corner = 20.dp,
    ),
    CardTheme(
        key = "SKY", label = "Sky",
        bannerStart = Color(0xFF0284C7), bannerEnd = Color(0xFF38BDF8),
        bannerText = Color.White, accent = Color(0xFF0284C7), corner = 22.dp,
    ),
    CardTheme(
        key = "MINT", label = "Mint",
        bannerStart = Color(0xFF10B981), bannerEnd = Color(0xFF6EE7B7),
        bannerText = Color.White, accent = Color(0xFF10B981), corner = 24.dp,
    ),
    CardTheme(
        key = "PEACH", label = "Peach",
        bannerStart = Color(0xFFFB923C), bannerEnd = Color(0xFFFDA4AF),
        bannerText = Color.White, accent = Color(0xFFF97316), corner = 26.dp,
    ),
    CardTheme(
        key = "GRAPHITE", label = "Graphite",
        bannerStart = Color(0xFF27272A), bannerEnd = Color(0xFF52525B),
        bannerText = Color.White, accent = Color(0xFFD4D4D8), corner = 16.dp,
    ),
)

/** Resolve a theme by key, defaulting to Modern. */
fun cardTheme(key: String?): CardTheme =
    CardThemes.firstOrNull { it.key == key } ?: CardThemes.first { it.key == "MODERN" }
