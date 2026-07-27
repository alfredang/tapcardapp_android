package com.tertiaryinfotech.tapcard.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = BrandViolet,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = BrandVioletSoft,
    onPrimaryContainer = BrandVioletDeep,
    secondary = BrandCoral,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = BrandCoralSoft,
    onSecondaryContainer = Color(0xFF7A2417),
    tertiary = BrandTeal,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = BrandTealSoft,
    onTertiaryContainer = Color(0xFF075147),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline,
)

private val DarkColors = darkColorScheme(
    primary = BrandBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = BrandBlueDeep,
    onPrimaryContainer = BrandBlueSoft,
    secondary = BrandBlueSoft,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
)

// Consistently rounded corners for Material components (text fields, dialogs, menus).
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun TapcardTheme(
    // Product decision: Tapcard is a light-themed app. We intentionally ignore
    // the system dark setting so the brand looks consistent for everyone.
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            window.statusBarColor = colors.background.toArgb()
        }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes) {
        // Make every bare Text() inherit the brand typeface, not the system font.
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = JakartaFamily),
            content = content,
        )
    }
}
