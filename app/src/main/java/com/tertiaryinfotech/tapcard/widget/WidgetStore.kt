package com.tertiaryinfotech.tapcard.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.tertiaryinfotech.tapcard.model.DigitalCard
import kotlinx.serialization.json.Json

/**
 * Persists the "primary" card (the one shown on the home-screen widget) so the
 * [CardWidgetProvider] — which runs outside the app process — can render it.
 * The ViewModel calls [savePrimary] whenever the card list changes.
 */
object WidgetStore {

    private const val PREFS = "TapcardWidget"
    private const val KEY = "primaryCard"
    private val json = Json { ignoreUnknownKeys = true }

    fun savePrimary(context: Context, card: DigitalCard?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (card == null) {
            prefs.edit().remove(KEY).apply()
        } else {
            prefs.edit().putString(KEY, json.encodeToString(DigitalCard.serializer(), card)).apply()
        }
        refresh(context)
    }

    fun loadPrimary(context: Context): DigitalCard? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return null
        return runCatching { json.decodeFromString(DigitalCard.serializer(), raw) }.getOrNull()
    }

    /** Repaints any placed widgets (safe no-op when none are on the home screen). */
    fun refresh(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, CardWidgetProvider::class.java))
        if (ids.isNotEmpty()) CardWidgetProvider().onUpdate(context, manager, ids)
    }
}
