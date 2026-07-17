package com.tertiaryinfotech.tapcard.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlueDeep
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily
import com.tertiaryinfotech.tapcard.vm.CardViewModel
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: CardViewModel) {
    Box(Modifier.fillMaxSize()) {
        AppBackground()
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.linearGradient(listOf(BrandBlue, BrandBlueDeep))),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.CreditCard,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                            Spacer(Modifier.size(10.dp))
                            Text(
                                "Tapcard",
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            },
        ) { padding ->
            if (vm.cards.isEmpty()) {
                EmptyHome(padding, onScan = vm::scanForMyCard, onManual = vm::startManualEntry)
            } else {
                CardHero(vm, padding)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardHero(vm: CardViewModel, padding: PaddingValues) {
    val cards = vm.cards
    val pagerState = rememberPagerState(pageCount = { cards.size })

    // Entrance animation.
    var appear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear = true }
    val enter by animateFloatAsState(if (appear) 1f else 0f, tween(500), label = "enter")

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .graphicsLayer {
                alpha = enter
                translationY = (1f - enter) * 40.dp.toPx()
            }
            .padding(vertical = 8.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 16.dp,
            contentPadding = PaddingValues(horizontal = 32.dp),
        ) { page ->
            val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val t = 1f - offset.coerceIn(0f, 1f)
            CardFace(
                cards[page],
                modifier = Modifier
                    .graphicsLayer {
                        val s = lerp(0.88f, 1f, t)
                        scaleX = s
                        scaleY = s
                        alpha = lerp(0.55f, 1f, t)
                    }
                    .tappable { vm.openCard(cards[page]) },
            )
        }

        if (cards.size > 1) {
            Spacer(Modifier.height(14.dp))
            PageDots(count = cards.size, selected = pagerState.currentPage)
        }

        Spacer(Modifier.height(24.dp))

        val current = cards[pagerState.currentPage.coerceIn(0, cards.lastIndex)]
        Column(Modifier.padding(horizontal = Spacing.page)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Share", Icons.Filled.Share, ShareBlue, Modifier.weight(1f)) { vm.openShare(current) }
                QuickAction("View", Icons.Filled.Visibility, ViewViolet, Modifier.weight(1f)) { vm.openCard(current) }
                QuickAction("Edit", Icons.Filled.Edit, EditAmber, Modifier.weight(1f)) {
                    vm.draft = current
                    vm.editDraft()
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PageDots(count: Int, selected: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(count) { i ->
            val active = i == selected
            val color by animateColorAsState(
                if (active) BrandBlue else MaterialTheme.colorScheme.outline,
                label = "dot",
            )
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(width = if (active) 22.dp else 7.dp, height = 7.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

private val ShareBlue = Color(0xFF2563EB)
private val ViewViolet = Color(0xFF7C3AED)
private val EditAmber = Color(0xFFF59E0B)

@Composable
private fun QuickAction(
    text: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(Radius.md))
            .tappable(onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.height(9.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun EmptyHome(
    padding: PaddingValues,
    onScan: () -> Unit,
    onManual: () -> Unit,
) {
    var appear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear = true }
    val enter by animateFloatAsState(if (appear) 1f else 0f, tween(500), label = "enterEmpty")

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = Spacing.page)
            .graphicsLayer {
                alpha = enter
                translationY = (1f - enter) * 40.dp.toPx()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(64.dp))
        Box(
            Modifier
                .size(104.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(BrandBlue, BrandBlueDeep))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.CreditCard,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Create your first card",
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Scan a paper business card or add one by hand, then share it instantly with a tap or a QR code.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        PrimaryButton("Scan contact information", icon = Icons.Filled.PhotoCamera, onClick = onScan)
        Spacer(Modifier.height(12.dp))
        OutlinedActionButton("Add manually", Icons.Filled.Add, onClick = onManual)
    }
}