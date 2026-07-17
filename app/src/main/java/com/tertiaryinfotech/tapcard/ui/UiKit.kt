package com.tertiaryinfotech.tapcard.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A tappable surface with a subtle spring-based press-down scale plus ripple —
 * gives buttons and cards a tactile, premium feel instead of a flat tap.
 */
fun Modifier.tappable(onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 650f),
        label = "tapScale",
    )
    this
        .scale(scale)
        .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick)
}

/**
 * Two soft radial "glow" blobs layered over a base fill to give flat gradient
 * cards a modern mesh-gradient depth. Drop inside a clipped Box behind content.
 */
@Composable
fun CardGlow(tint: Color) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .offset(x = 210.dp, y = (-60).dp)
                .size(260.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                    ),
                ),
        )
        Box(
            Modifier
                .offset(x = (-90).dp, y = 120.dp)
                .size(240.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(tint.copy(alpha = 0.55f), Color.Transparent),
                    ),
                ),
        )
    }
}
