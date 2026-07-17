package com.tertiaryinfotech.tapcard.ui

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlueDeep
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily
import com.tertiaryinfotech.tapcard.vm.CardViewModel

// Sample cards shown in the hero fan — illustrative only, never persisted.
private val HeroFront = DigitalCard(
    name = "Steven Rice",
    title = "Chief Operations Officer",
    company = "Ninefold",
    email = "steven.rice@ninefold.com",
    phone = "(646) 555-0281",
    theme = "CREATIVE",
)
private val HeroMid = DigitalCard(
    name = "Ava Chen",
    title = "Product Designer",
    company = "Northwind",
    theme = "CORPORATE",
)
private val HeroBack = DigitalCard(
    name = "Marco Diaz",
    title = "Founder",
    company = "Lumen Labs",
    theme = "MODERN",
)

// Aurora blob colours (sampled from the card themes) for the animated backdrop.
private val Coral = Color(0xFFFB7185)
private val Sky = Color(0xFF2563EB)
private val Violet = Color(0xFF7C5CFF)
private val Amber = Color(0xFFF59E0B)

/** Entrance helper: fade + rise as [p] goes 0→1. */
private fun Modifier.appear(p: Float, dy: Dp = 26.dp): Modifier =
    graphicsLayer {
        alpha = p
        translationY = (1f - p) * dy.toPx()
    }

/**
 * Logged-out landing page — a showpiece. An animated aurora backdrop, a staggered
 * entrance, a floating fan of business cards over a breathing glow, and a pulsing
 * CTA. Built to feel premium and inviting the moment it appears.
 */
@Composable
fun WelcomeScreen(vm: CardViewModel) {
    // ---- Continuous motion ----
    val t = rememberInfiniteTransition(label = "welcome")
    val floatA by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(2800, easing = EaseInOut), RepeatMode.Reverse), label = "a")
    val floatB by t.animateFloat(1f, -1f, infiniteRepeatable(tween(3400, easing = EaseInOut), RepeatMode.Reverse), label = "b")
    val floatC by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(4200, easing = EaseInOut), RepeatMode.Reverse), label = "c")
    val glow by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = EaseInOut), RepeatMode.Reverse), label = "glow")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Reverse), label = "drift")

    // ---- Staggered entrance ----
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val eWord by animateFloatAsState(if (shown) 1f else 0f, tween(600, 60, EaseOutCubic), label = "eWord")
    val eHead by animateFloatAsState(if (shown) 1f else 0f, tween(700, 160, EaseOutCubic), label = "eHead")
    val eSub by animateFloatAsState(if (shown) 1f else 0f, tween(700, 280, EaseOutCubic), label = "eSub")
    val eCards by animateFloatAsState(if (shown) 1f else 0f, tween(950, 360, EaseOutCubic), label = "eCards")
    val eCta by animateFloatAsState(if (shown) 1f else 0f, tween(700, 640, EaseOutCubic), label = "eCta")
    val eLogin by animateFloatAsState(if (shown) 1f else 0f, tween(700, 760, EaseOutCubic), label = "eLogin")

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AuroraBackdrop(drift)

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(top = 28.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ---- Wordmark ----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.appear(eWord),
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .graphicsLayer {
                            val s = 1f + glow * 0.06f
                            scaleX = s; scaleY = s
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(BrandBlue, BrandBlueDeep))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Contactless, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    "Tapcard",
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.3).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(30.dp))

            // ---- Headline ----
            Text(
                "Share your\nbusiness card",
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                letterSpacing = (-1.2).sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.appear(eHead),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "One tap or scan — and every connection is saved. No app needed for the other person.",
                fontSize = 15.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.appear(eSub),
            )

            // ---- Floating card fan over a breathing glow ----
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                // Breathing glow behind the stack.
                Box(
                    Modifier
                        .size(320.dp)
                        .graphicsLayer {
                            val s = 0.85f + glow * 0.25f
                            scaleX = s; scaleY = s
                            alpha = 0.5f + glow * 0.35f
                        }
                        .background(
                            Brush.radialGradient(
                                listOf(Coral.copy(alpha = 0.55f), Sky.copy(alpha = 0.28f), Color.Transparent),
                            ),
                        ),
                )

                HeroCard(HeroBack, width = 232.dp, baseX = 38.dp, baseY = 30.dp, baseRot = 13f, baseScale = 0.86f, float = floatC, entrance = eCards, entranceDelay = 0f)
                HeroCard(HeroMid, width = 250.dp, baseX = (-40).dp, baseY = 14.dp, baseRot = (-11f), baseScale = 0.92f, float = floatB, entrance = eCards, entranceDelay = 0.12f)
                HeroCard(HeroFront, width = 268.dp, baseX = 2.dp, baseY = (-14).dp, baseRot = 2f, baseScale = 1f, float = floatA, entrance = eCards, entranceDelay = 0.24f)
            }

            Spacer(Modifier.height(6.dp))

            // ---- CTA with a pulsing glow ----
            Box(Modifier.appear(eCta), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .graphicsLayer { alpha = 0.35f + glow * 0.3f }
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(BrandBlue.copy(alpha = 0.45f), Color.Transparent),
                            ),
                        ),
                )
                PrimaryButton(text = "Get started", onClick = vm::beginSignUp)
            }

            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.appear(eLogin, dy = 14.dp),
            ) {
                Text(
                    "Already have an account? ",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Log in",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue,
                    modifier = Modifier.tappable(vm::beginLogin),
                )
            }
        }
    }
}

/** A single hero card combining its base transform, a floating cycle, and entrance. */
@Composable
private fun HeroCard(
    card: DigitalCard,
    width: Dp,
    baseX: Dp,
    baseY: Dp,
    baseRot: Float,
    baseScale: Float,
    float: Float,
    entrance: Float,
    entranceDelay: Float,
) {
    // Ease the shared entrance progress by a per-card delay so the fan cascades in.
    val e = ((entrance - entranceDelay) / (1f - entranceDelay)).coerceIn(0f, 1f)
    Box(
        Modifier
            .width(width)
            .offset(x = baseX, y = baseY)
            .graphicsLayer {
                alpha = e
                translationY = float * 14.dp.toPx() + (1f - e) * 60.dp.toPx()
                rotationZ = baseRot + float * 2.5f
                val s = baseScale * (0.82f + 0.18f * e)
                scaleX = s; scaleY = s
            },
    ) { CardFace(card) }
}

/** Slowly drifting coloured blobs behind everything, for depth and life. */
@Composable
private fun AuroraBackdrop(drift: Float) {
    Box(Modifier.fillMaxSize()) {
        AuroraBlob(Coral, size = 380.dp, x = (-120).dp + (drift * 40).dp, y = (-80).dp + (drift * 30).dp)
        AuroraBlob(Sky, size = 420.dp, x = (170).dp - (drift * 50).dp, y = (60).dp + (drift * 40).dp)
        AuroraBlob(Violet, size = 360.dp, x = (-90).dp + (drift * 30).dp, y = (420).dp - (drift * 40).dp)
        AuroraBlob(Amber, size = 300.dp, x = (150).dp + (drift * 30).dp, y = (520).dp + (drift * 30).dp)
    }
}

@Composable
private fun AuroraBlob(color: Color, size: Dp, x: Dp, y: Dp) {
    Box(
        Modifier
            .size(size)
            .offset(x = x, y = y)
            .background(
                Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset.Unspecified,
                ),
            ),
    )
}
