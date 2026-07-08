package com.tertiaryinfotech.tapcard.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Brand ───────────────────────────────────────────────────────────────────
// Professional blue brand, Blinq-style. One confident accent across the app.
val BrandBlue = Color(0xFF2563EB)       // primary
val BrandBlueDeep = Color(0xFF1D4ED8)   // gradient end / pressed
val BrandBlueSoft = Color(0xFF93B4FF)   // soft accent
val BrandBlueTint = Color(0xFFEAF1FF)   // faint fill behind icons / chips

// Backwards-compatibility aliases — older code imports the violet names.
// They now resolve to the blue brand so nothing breaks during the migration.
val BrandViolet = BrandBlue
val BrandVioletDeep = BrandBlueDeep
val BrandVioletSoft = BrandBlueSoft

// ─── Light scheme ────────────────────────────────────────────────────────────
val LightBackground = Color(0xFFF5F7FB)   // cool off-white canvas
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEF1F7)
val LightOnSurface = Color(0xFF0F1729)
val LightOnSurfaceVariant = Color(0xFF64708A)
val LightOutline = Color(0xFFE1E6F0)

// ─── Dark scheme ─────────────────────────────────────────────────────────────
// Retained for completeness; the app is pinned to light per product decision.
val DarkBackground = Color(0xFF0B1020)
val DarkSurface = Color(0xFF141A2B)
val DarkSurfaceVariant = Color(0xFF1F273B)
val DarkOnSurface = Color(0xFFEDF1F9)
val DarkOnSurfaceVariant = Color(0xFF9AA6BF)
val DarkOutline = Color(0xFF2A3346)