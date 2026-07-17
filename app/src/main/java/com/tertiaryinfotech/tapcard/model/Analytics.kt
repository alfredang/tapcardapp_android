package com.tertiaryinfotech.tapcard.model

import kotlinx.serialization.Serializable

/** Headline counts across all of the user's cards. */
@Serializable
data class AnalyticsTotals(
    val views: Int = 0,
    val taps: Int = 0,
    val leads: Int = 0,
)

/** Per-card performance row. */
@Serializable
data class CardStats(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val views: Int = 0,
    val taps: Int = 0,
)

/**
 * Card-performance summary for the analytics screen. Mirrors the shape returned
 * by GET /api/mobile/analytics. `byType` maps raw event names (VIEW, QR_SCAN,
 * EMAIL_CLICK, …) to their counts.
 */
@Serializable
data class AnalyticsSummary(
    val totals: AnalyticsTotals = AnalyticsTotals(),
    val byType: Map<String, Int> = emptyMap(),
    val last30dViews: Int = 0,
    val cards: List<CardStats> = emptyList(),
)
