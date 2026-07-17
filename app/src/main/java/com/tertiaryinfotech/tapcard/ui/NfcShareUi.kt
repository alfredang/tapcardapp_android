package com.tertiaryinfotech.tapcard.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.nfc.CardEmulationService
import com.tertiaryinfotech.tapcard.nfc.NfcShare
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue

/** True when the device supports Host Card Emulation (needed for NFC tap-to-share). */
fun hceAvailable(context: Context): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)

/**
 * While shown, publishes [card] over NFC (via [NfcShare] + [CardEmulationService])
 * and asks this app to be the preferred HCE service so a tap reliably delivers it.
 * Everything is torn down when dismissed.
 */
@Composable
fun NfcShareDialog(card: DigitalCard, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val nfcOn = NfcAdapter.getDefaultAdapter(context)?.isEnabled == true

    DisposableEffect(card.id) {
        NfcShare.setForCard(card)
        val activity = context as? Activity
        val adapter = NfcAdapter.getDefaultAdapter(context)
        val emulation = adapter?.let { CardEmulation.getInstance(it) }
        val component = ComponentName(context, CardEmulationService::class.java)
        if (activity != null && emulation != null) {
            runCatching { emulation.setPreferredService(activity, component) }
        }
        onDispose {
            NfcShare.clear()
            if (activity != null && emulation != null) {
                runCatching { emulation.unsetPreferredService(activity) }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Contactless, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(40.dp)) },
        title = { Text("Tap to share") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Hold the back of your phone against the other person's phone. Your card opens on theirs — no app needed.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                if (!nfcOn) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "NFC looks turned off — enable it in Settings, then tap.",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
