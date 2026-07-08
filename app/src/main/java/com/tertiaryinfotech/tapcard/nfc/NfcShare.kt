package com.tertiaryinfotech.tapcard.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.net.ApiConfig
import com.tertiaryinfotech.tapcard.util.VCard

/**
 * Holds the NDEF payload the [CardEmulationService] serves over NFC while the
 * user is actively "tapping to share". Set it right before a tap, clear it after.
 *
 * A synced card (has a public [DigitalCard.slug]) is shared as its live web link;
 * a local-only card is shared as a vCard so the tap still saves the contact offline.
 */
object NfcShare {

    /** NDEF file bytes = 2-byte length prefix (NLEN) + NDEF message, or null when idle. */
    @Volatile
    var ndefFile: ByteArray? = null
        private set

    val isSharing: Boolean get() = ndefFile != null

    fun setForCard(card: DigitalCard) {
        val message = if (card.slug.isNotBlank()) {
            NdefMessage(NdefRecord.createUri("${ApiConfig.PUBLIC_WEB_URL}/c/${card.slug}"))
        } else {
            NdefMessage(NdefRecord.createMime("text/vcard", VCard.build(card).toByteArray(Charsets.UTF_8)))
        }
        val bytes = message.toByteArray()
        val nlen = bytes.size
        ndefFile = byteArrayOf((nlen shr 8).toByte(), (nlen and 0xFF).toByte()) + bytes
    }

    fun clear() {
        ndefFile = null
    }
}
