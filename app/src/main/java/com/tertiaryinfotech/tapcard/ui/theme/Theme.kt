package com.tertiaryinfotech.tapcard.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Tapcard brand blue (light) and a lighter tint for dark mode.
val BrandBlue = Color(0xFF2563EB)
val BrandBlueDark = Color(0xFF6E9BFF)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    secondary = BrandBlue,
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = Color(0xFF06122B),
    secondary = BrandBlueDark,
)

private val AppTypography = Typography()

@Composable
fun TapcardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
