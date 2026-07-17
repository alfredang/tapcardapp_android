package com.tertiaryinfotech.tapcard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.util.CardImages
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily
import com.tertiaryinfotech.tapcard.ui.theme.cardTheme

// Body text colours — the lower half is always white, so text stays dark
// regardless of the banner theme.
private val CardInk = Color(0xFF111114)
private val CardInkMuted = Color(0xFF6B7280)

private val BannerHeight = 116.dp
private val AvatarSize = 88.dp

/**
 * The "face" of a digital card — a Blinq-style profile card: a colour banner
 * (theme-driven) carrying the company logo + name, a circular avatar overlapping
 * the banner edge, then a clean white body with the person's name, title and
 * company. Shared by the My Card hero, the editor preview and the detail screen.
 */
@Composable
fun CardFace(card: DigitalCard, modifier: Modifier = Modifier) {
    val theme = cardTheme(card.theme)
    val onBanner = theme.bannerText

    Box(
        modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.35f),
                ambientColor = Color.Black.copy(alpha = 0.25f),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE7E7EA), RoundedCornerShape(24.dp)),
    ) {
        Column {
            // ---- Banner ----
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(BannerHeight)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(theme.bannerStart, theme.bannerEnd),
                            start = Offset(0f, 0f),
                            end = Offset(700f, 300f),
                        ),
                    ),
            ) {
                // Optional cover photo behind the banner, with a legibility scrim.
                if (card.coverBanner.isNotBlank()) {
                    AsyncImage(
                        model = CardImages.model(card.coverBanner),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.20f), Color.Black.copy(alpha = 0.45f)),
                            ),
                        ),
                    )
                }

                // Logo + company name, top-left.
                Row(
                    Modifier.padding(start = 18.dp, top = 18.dp, end = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (card.companyLogo.isNotBlank()) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = CardImages.model(card.companyLogo),
                                contentDescription = "Company logo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(26.dp).clip(CircleShape),
                            )
                        }
                        Spacer(Modifier.size(10.dp))
                    } else {
                        Icon(
                            Icons.Filled.Contactless,
                            contentDescription = null,
                            tint = onBanner.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                    }
                    Text(
                        card.company.ifBlank { "TAPCARD" },
                        color = onBanner,
                        fontFamily = DisplayFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = if (card.company.isBlank()) 2.sp else (-0.2).sp,
                    )
                }
            }

            // ---- Body ----
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 20.dp, end = 20.dp, top = AvatarSize / 2 + 12.dp, bottom = 22.dp),
            ) {
                Text(
                    card.displayName,
                    color = CardInk,
                    fontFamily = DisplayFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                )
                if (card.title.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(card.title, color = CardInkMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                if (card.company.isNotBlank()) {
                    Spacer(Modifier.height(1.dp))
                    Text(card.company, color = CardInkMuted, fontSize = 14.sp)
                }
                if (card.tagline.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(card.tagline, color = CardInkMuted.copy(alpha = 0.85f), fontSize = 13.sp)
                }
            }
        }

        // ---- Avatar, overlapping the banner's bottom edge ----
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = 20.dp, y = BannerHeight - AvatarSize / 2)
                .size(AvatarSize)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(4.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (card.profilePhoto.isNotBlank()) {
                AsyncImage(
                    model = CardImages.model(card.profilePhoto),
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(theme.bannerStart, theme.bannerEnd)),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        card.displayName.take(1).uppercase(),
                        color = onBanner,
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                    )
                }
            }
        }
    }
}
