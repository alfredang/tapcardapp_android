 package com.tertiaryinfotech.tapcard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlueDeep
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily
import com.tertiaryinfotech.tapcard.vm.CardViewModel
import kotlinx.coroutines.launch

private data class Slide(val icon: ImageVector, val title: String, val body: String)

private val Slides = listOf(
    Slide(
        Icons.Filled.CreditCard,
        "Your card, always on you",
        "Share your details in a tap or a scan — the other person doesn't even need the app.",
    ),
    Slide(
        Icons.Filled.PhotoCamera,
        "Scan any card",
        "Snap a paper business card or a QR and Tapcard captures the details for you.",
    ),
    Slide(
        Icons.Filled.Groups,
        "Never lose a connection",
        "Save the people you meet, capture leads, and see how your card performs.",
    ),
)

@Composable
fun OnboardingScreen(vm: CardViewModel) {
    val pagerState = rememberPagerState(pageCount = { Slides.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == Slides.lastIndex

    Box(Modifier.fillMaxSize()) {
        AppBackground()
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.page)
                .padding(top = 12.dp, bottom = 28.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = vm::finishOnboarding) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                SlideView(Slides[page])
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(Slides.size) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (active) 22.dp else 7.dp, height = 7.dp)
                            .clip(CircleShape)
                            .background(if (active) BrandBlue else MaterialTheme.colorScheme.outline),
                    )
                }
            }

            PrimaryButton(
                text = if (isLast) "Get started" else "Next",
                icon = if (isLast) Icons.Filled.Share else null,
            ) {
                if (isLast) vm.finishOnboarding()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }
        }
    }
}

@Composable
private fun SlideView(slide: Slide) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(132.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(Brush.linearGradient(listOf(BrandBlue, BrandBlueDeep))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(slide.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(60.dp))
        }
        Spacer(Modifier.height(36.dp))
        Text(
            slide.title,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            slide.body,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}