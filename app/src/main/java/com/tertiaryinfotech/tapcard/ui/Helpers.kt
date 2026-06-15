package com.tertiaryinfotech.tapcard.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

/** Formats an epoch-millis timestamp like "14 Jun 2026". */
fun formatDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))
