package com.tertiaryinfotech.tapcard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily
import com.tertiaryinfotech.tapcard.ui.theme.cardTheme

/**
 * The premium "face" of a digital card — a bold, layered, credit-card-style
 * surface with a glossy highlight, depth, an NFC glyph and a wordmark. Shared by
 * the My Card hero and the card detail screen.
 */
@Composable
fun CardFace(card: DigitalCard, modifier: Modifier = Modifier) {
    val theme = cardTheme(card.theme)
    val onCard = theme.bannerText
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1.60f)
            .shadow(
                elevation = 28.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = theme.bannerStart.copy(alpha = 0.6f),
                ambientColor = theme.bannerStart.copy(alpha = 0.4f),
            )
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(theme.bannerStart, theme.bannerEnd),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 900f),
                ),
            ),
    ) {
        // Photo cover (if provided) with a legibility scrim.
        if (card.coverBanner.isNotBlank()) {
            AsyncImage(
                model = card.coverBanner,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.55f))),
                ),
            )
        }

        // Depth: soft decorative light blobs.
        CardGlow(tint = lerp(theme.bannerStart, Color.White, 0.45f))

        // Glossy diagonal highlight, top-left.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(120f, 60f),
                        radius = 620f,
                    ),
                ),
        )

        // Inner hairline highlight for a glassy edge.
        Box(
            Modifier
                .fillMaxSize()
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(28.dp)),
        )

        // Top row: contactless glyph + wordmark, and optional company logo.
        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Contactless,
                contentDescription = null,
                tint = onCard.copy(alpha = 0.9f),
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                "TAPCARD",
                color = onCard.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.weight(1f))
            if (card.companyLogo.isNotBlank()) {
                AsyncImage(
                    model = card.companyLogo,
                    contentDescription = "Company logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(9.dp)),
                )
            }
        }

        // Identity block, bottom-left.
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (card.profilePhoto.isNotBlank()) {
                    AsyncImage(
                        model = card.profilePhoto,
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    Text(
                        card.displayName.take(1).uppercase(),
                        color = onCard,
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                card.displayName,
                color = onCard,
                fontFamily = DisplayFontFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
            val sub = listOf(card.title, card.company).filter { it.isNotBlank() }.joinToString("  ·  ")
            if (sub.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    sub,
                    color = onCard.copy(alpha = 0.92f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (card.tagline.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(card.tagline, color = onCard.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
}