package com.tertiaryinfotech.tapcard.util

import android.content.Context
import com.tertiaryinfotech.tapcard.model.DigitalCard
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Lightweight persistence of saved cards as JSON in SharedPreferences.
 * Deliberately simple — no Room for a lightweight app (mirrors the iOS card store).
 */
class CardStore(context: Context) {

    private val prefs = context.getSharedPreferences("Tapcard", Context.MODE_PRIVATE)
    private val key = "savedCards"
    private val maxStored = 100
    private val json = Json { ignoreUnknownKeys = true }

    /** All saved cards, most recent first. */
    fun allCards(): List<DigitalCard> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        val cards = runCatching { json.decodeFromString<List<DigitalCard>>(raw) }.getOrDefault(emptyList())
        return cards.sortedByDescending { it.createdAtEpoch }
    }

    /** Inserts or updates a card (matched by id) and trims to [maxStored]. */
    fun save(card: DigitalCard) {
        val cards = allCards().filterNot { it.id == card.id }.toMutableList()
        cards.add(0, card)
        persist(cards.take(maxStored))
    }

    /** Removes a saved card by id. */
    fun delete(id: String) = persist(allCards().filterNot { it.id == id })

    /** Deletes all saved cards. */
    fun clear() = prefs.edit().remove(key).apply()

    private fun persist(cards: List<DigitalCard>) {
        prefs.edit().putString(key, json.encodeToString(cards)).apply()
    }
}
