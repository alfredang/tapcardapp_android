package com.tertiaryinfotech.tapcard.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Brand ───────────────────────────────────────────────────────────────────
// Monochrome: crisp black-on-white. One confident near-black accent across the app.
// (Names kept as "BrandBlue*" so existing imports keep working during the switch.)
val BrandBlue = Color(0xFF111114)       // primary (near-black)
val BrandBlueDeep = Color(0xFF000000)   // gradient end / pressed (black)
val BrandBlueSoft = Color(0xFFD4D4D8)   // soft grey accent
val BrandBlueTint = Color(0xFFF4F4F5)   // faint grey fill behind icons / chips

// Backwards-compatibility aliases — older code imports the violet names.
// They now resolve to the blue brand so nothing breaks during the migration.
val BrandViolet = BrandBlue
val BrandVioletDeep = BrandBlueDeep
val BrandVioletSoft = BrandBlueSoft

// ─── Light scheme ────────────────────────────────────────────────────────────
val LightBackground = Color(0xFFF7F7F8)   // neutral off-white canvas
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F0F1)
val LightOnSurface = Color(0xFF111114)     // near-black text
val LightOnSurfaceVariant = Color(0xFF6B7280) // neutral grey
val LightOutline = Color(0xFFE4E4E7)       // neutral grey border

// ─── Dark scheme ─────────────────────────────────────────────────────────────
// Retained for completeness; the app is pinned to light per product decision.
val DarkBackground = Color(0xFF0B1020)
val DarkSurface = Color(0xFF141A2B)
val DarkSurfaceVariant = Color(0xFF1F273B)
val DarkOnSurface = Color(0xFFEDF1F9)
val DarkOnSurfaceVariant = Color(0xFF9AA6BF)
val DarkOutline = Color(0xFF2A3346)