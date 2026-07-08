package com.tertiaryinfotech.tapcard.util

import android.content.Context

/** Remembers whether the first-run onboarding has been shown. */
class OnboardingStore(context: Context) {

    private val prefs = context.getSharedPreferences("TapcardOnboarding", Context.MODE_PRIVATE)

    var seen: Boolean
        get() = prefs.getBoolean("seen", false)
        set(value) {
            prefs.edit().putBoolean("seen", value).apply()
        }
}