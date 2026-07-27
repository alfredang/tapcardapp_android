package com.tertiaryinfotech.tapcard.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Brand ───────────────────────────────────────────────────────────────────
// Matches the tapcard.tertiaryinfotech.com palette: a violet primary paired with
// a coral and a teal secondary, on a bright near-white canvas.
// (Names kept as "BrandBlue*"/"BrandViolet*" so existing imports keep working.)
val BrandViolet = Color(0xFF7C5CFF)      // primary  (web --primary)
val BrandVioletDeep = Color(0xFF5B3FE0)  // gradient end / pressed
val BrandVioletSoft = Color(0xFFE7E0FF)  // soft tint behind icons / chips
val BrandVioletTint = Color(0xFFF3F0FF)  // faintest fill

// Secondary + tertiary accents, used for gradients, stats and highlights.
val BrandCoral = Color(0xFFFA6B55)       // web --accent
val BrandCoralSoft = Color(0xFFFFE2DC)
val BrandTeal = Color(0xFF16B5A3)        // web --highlight
val BrandTealSoft = Color(0xFFD3F5F0)

// Backwards-compatibility aliases — older code imports the blue names.
val BrandBlue = BrandViolet
val BrandBlueDeep = BrandVioletDeep
val BrandBlueSoft = BrandVioletSoft
val BrandBlueTint = BrandVioletTint

// ─── Light scheme ────────────────────────────────────────────────────────────
val LightBackground = Color(0xFFFCFAFF)       // faintly violet off-white canvas
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF5F1FE)
val LightOnSurface = Color(0xFF1B1030)        // deep violet-black text
val LightOnSurfaceVariant = Color(0xFF645C7A) // muted violet-grey
val LightOutline = Color(0xFFE6E0F2)          // soft violet border

// ─── Dark scheme ─────────────────────────────────────────────────────────────
// Retained for completeness; the app is pinned to light per product decision.
val DarkBackground = Color(0xFF0D0916)
val DarkSurface = Color(0xFF16101F)
val DarkSurfaceVariant = Color(0xFF221A2E)
val DarkOnSurface = Color(0xFFF3F0FA)
val DarkOnSurfaceVariant = Color(0xFFAEA4C2)
val DarkOutline = Color(0xFF322943)
