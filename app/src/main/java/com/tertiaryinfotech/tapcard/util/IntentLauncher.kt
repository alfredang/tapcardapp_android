package com.tertiaryinfotech.tapcard.util

import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Starts [intent] but never crashes the app if the device has no app that can
 * handle it — e.g. no dialer, email client, WhatsApp, browser or contacts app.
 * In that case it shows a short message instead of throwing
 * ActivityNotFoundException (or SecurityException) on the UI.
 */
fun Context.launchSafely(
    intent: Intent,
    failureMessage: String = "No app available to handle this",
) {
    runCatching { startActivity(intent) }
        .onFailure { Toast.makeText(this, failureMessage, Toast.LENGTH_SHORT).show() }
}
