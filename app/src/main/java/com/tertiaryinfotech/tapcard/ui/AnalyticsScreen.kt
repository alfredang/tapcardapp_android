package com.tertiaryinfotech.tapcard.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.model.CardStats
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlueDeep
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily
import com.tertiaryinfotech.tapcard.vm.CardViewModel

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
        topBar = {
            TopAppBar(
                title = { Text("Insights", fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
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

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val totals = data?.totals
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Views", totals?.views ?: 0, Modifier.weight(1f))
                    StatCard("Taps", totals?.taps ?: 0, Modifier.weight(1f))
                    StatCard("Leads", totals?.leads ?: 0, Modifier.weight(1f))
                }
            }

            item {
                Text(
                    "${data?.last30dViews ?: 0} views in the last 30 days",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }

            val breakdown = data?.byType.orEmpty()
                .entries
                .filter { it.value > 0 }
                .sortedByDescending { it.value }
            if (breakdown.isNotEmpty()) {
                item { SectionLabel("BREAKDOWN") }
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        breakdown.forEach { (type, count) ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    EventLabels[type] ?: type,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    count.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            val cards = data?.cards.orEmpty().filter { it.views > 0 || it.taps > 0 }
            if (cards.isNotEmpty()) {
                item { SectionLabel("BY CARD") }
                items(cards, key = { it.id }) { card -> CardStatRow(card) }
            }

            if (totals != null && totals.views == 0 && totals.taps == 0 && totals.leads == 0) {
                item { EmptyAnalytics() }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier
            .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = BrandBlue)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(BrandBlue, BrandBlueDeep)))
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value.toString(), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CardStatRow(card: CardStats) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                card.name.ifBlank { "Untitled card" },
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (card.slug.isNotBlank()) {
                Text("/${card.slug}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("${card.views} views", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text("${card.taps} taps", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyAnalytics() {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.BarChart, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("No activity yet", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            "Publish a card and share its link — views, taps and leads will show up here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
    }
}
