package com.tertiaryinfotech.tapcard.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.model.CardStats
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily
import com.tertiaryinfotech.tapcard.vm.CardViewModel

private val StatBlue = Color(0xFF2563EB)
private val StatViolet = Color(0xFF7C3AED)
private val StatGreen = Color(0xFF059669)

/** Cycled across the breakdown bars so each metric reads distinctly. */
private val BarPalette = listOf(
    Color(0xFF2563EB), Color(0xFF7C3AED), Color(0xFF059669), Color(0xFFF59E0B),
    Color(0xFF0891B2), Color(0xFFEC4899), Color(0xFFEA580C), Color(0xFF6366F1),
)

/** Human labels for the raw analytics event types. */
private val EventLabels = mapOf(
    "VIEW" to "Card views",
    "QR_SCAN" to "QR scans",
    "WHATSAPP_CLICK" to "WhatsApp taps",
    "EMAIL_CLICK" to "Email taps",
    "WEBSITE_CLICK" to "Website taps",
    "PHONE_CLICK" to "Call taps",
    "SOCIAL_CLICK" to "Social taps",
    "VCF_DOWNLOAD" to "Contact saves",
    "LEAD_SUBMIT" to "Leads captured",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(vm: CardViewModel) {
    val data = vm.analytics

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (!vm.isAuthenticated) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                SignedOutState(
                    icon = Icons.Filled.BarChart,
                    title = "See how your card performs",
                    message = "Sign in to track views, taps and leads on your published cards.",
                    onSignIn = vm::openAuth,
                )
            }
            return@Scaffold
        }

        if (vm.isAnalyticsLoading && data == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue)
            }
            return@Scaffold
        }

        val totals = data?.totals
        val hasActivity = totals != null && (totals.views > 0 || totals.taps > 0 || totals.leads > 0)
        // Exclude raw VIEW — it's already the headline "Views" stat, so the breakdown
        // stays complementary (how people *engaged*) rather than repeating the number.
        val breakdown = data?.byType.orEmpty().entries
            .filter { it.value > 0 && it.key != "VIEW" }
            .sortedByDescending { it.value }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .tappable(vm::openSettings),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    ScreenTitle("Insights", "How your cards are performing")
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Views", totals?.views ?: 0, Icons.Filled.Visibility, StatBlue, Modifier.weight(1f))
                    StatCard("Taps", totals?.taps ?: 0, Icons.Filled.TouchApp, StatViolet, Modifier.weight(1f))
                    StatCard("Leads", totals?.leads ?: 0, Icons.Filled.PersonAddAlt1, StatGreen, Modifier.weight(1f))
                }
            }

            if (hasActivity) {
                if (breakdown.isNotEmpty()) {
                    item { SectionLabel("How people engaged") }
                    item { BreakdownBars(breakdown.map { (EventLabels[it.key] ?: it.key) to it.value }) }
                }

                // Per-card only makes sense with more than one active card — otherwise
                // it just repeats the totals above.
                val cards = data?.cards.orEmpty().filter { it.views > 0 || it.taps > 0 }
                if (cards.size > 1) {
                    item { SectionLabel("By card") }
                    items(cards, key = { it.id }) { card -> CardStatRow(card) }
                }
            } else {
                item { EmptyAnalytics { vm.cards.firstOrNull()?.let(vm::openShare) } }
            }
        }
    }
}

// ─── Stat card ───────────────────────────────────────────────────────────────────
@Composable
private fun StatCard(label: String, value: Int, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            value.toString(),
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = DisplayFontFamily,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            label.uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        )
    }
}

// ─── Breakdown bars ─────────────────────────────────────────────────────────────
@Composable
private fun BreakdownBars(items: List<Pair<String, Int>>) {
    val max = (items.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items.forEachIndexed { index, (label, count) ->
            val color = BarPalette[index % BarPalette.size]
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(count.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
                }
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.14f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(count / max.toFloat())
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color),
                    )
                }
            }
        }
    }
}

// ─── Per-card performance ────────────────────────────────────────────────────────
@Composable
private fun CardStatRow(card: CardStats) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text(
            card.name.ifBlank { "Untitled card" },
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (card.slug.isNotBlank()) {
            Text("/${card.slug}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat("Views", card.views, Modifier.weight(1f))
            MiniStat("Taps", card.taps, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(value.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── Empty state ─────────────────────────────────────────────────────────────────
@Composable
private fun EmptyAnalytics(onShare: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 40.dp, start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("No activity yet", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            "Share your card to start collecting views, taps and leads — they'll show up here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        PrimaryButton("Share your card", icon = Icons.Filled.Share, onClick = onShare)
    }
}
