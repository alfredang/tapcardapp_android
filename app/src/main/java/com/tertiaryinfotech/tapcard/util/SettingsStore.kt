package com.tertiaryinfotech.tapcard.util

import android.content.Context
import com.tertiaryinfotech.tapcard.model.UserSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Local copy of the synced [UserSettings], so the QR and WhatsApp links use the
 * right country code offline and before the first server fetch completes.
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("TapcardSettings", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): UserSettings =
        prefs.getString(KEY, null)
            ?.let { runCatching { json.decodeFromString<UserSettings>(it) }.getOrNull() }
            ?: UserSettings()

    fun save(settings: UserSettings) {
        prefs.edit().putString(KEY, json.encodeToString(settings)).apply()
    }

    private companion object {
        const val KEY = "settings"
    }
}
