package com.tertiaryinfotech.tapcard.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/**
 * Emulates an NFC Forum Type 4 Tag holding the current card's NDEF message, so
 * tapping this phone to another NFC phone (or reader) delivers the card link /
 * vCard — the "tap to share" moment. The payload comes from [NfcShare]; when
 * nothing is being shared, reads return a not-found status.
 *
 * Implements the minimal Type 4 command set: SELECT (application / CC / NDEF
 * files) and READ BINARY, per the NFC Forum spec.
 */
class CardEmulationService : HostApduService() {

    // Which file the reader has selected: none / Capability Container / NDEF.
    private var selected = FILE_NONE

    override fun processCommandApdu(apdu: ByteArray?, extras: Bundle?): ByteArray {
        if (apdu == null) return SW_ERROR

        // SELECT the NDEF tag application by AID.
        if (startsWith(apdu, SELECT_NDEF_APP)) {
            selected = FILE_NONE
            return SW_OK
        }

        // SELECT a file by id: 00 A4 00 0C 02 <fid_hi> <fid_lo>.
        if (apdu.size >= 7 && apdu[0] == 0x00.toByte() && apdu[1] == 0xA4.toByte() &&
            apdu[2] == 0x00.toByte() && apdu[3] == 0x0C.toByte() && apdu[4] == 0x02.toByte()
        ) {
            val fid = ((apdu[5].toInt() and 0xFF) shl 8) or (apdu[6].toInt() and 0xFF)
            selected = when (fid) {
                0xE103 -> FILE_CC
                0xE104 -> FILE_NDEF
                else -> FILE_NONE
            }
            return if (selected != FILE_NONE) SW_OK else SW_FILE_NOT_FOUND
        }

        // READ BINARY: 00 B0 <offset_hi> <offset_lo> <le>.
        if (apdu.size >= 5 && apdu[0] == 0x00.toByte() && apdu[1] == 0xB0.toByte()) {
            val offset = ((apdu[2].toInt() and 0xFF) shl 8) or (apdu[3].toInt() and 0xFF)
            val le = apdu[4].toInt() and 0xFF
            val file = when (selected) {
                FILE_CC -> CC_FILE
                FILE_NDEF -> NfcShare.ndefFile ?: return SW_FILE_NOT_FOUND
                else -> return SW_FILE_NOT_FOUND
            }
            if (offset > file.size) return SW_ERROR
            val end = minOf(offset + le, file.size)
            return file.copyOfRange(offset, end) + SW_OK
        }

        return SW_ERROR
    }

    override fun onDeactivated(reason: Int) {
        selected = FILE_NONE
    }

    private fun startsWith(a: ByteArray, prefix: ByteArray): Boolean {
        if (a.size < prefix.size) return false
        for (i in prefix.indices) if (a[i] != prefix[i]) return false
        return true
    }

    companion object {
        private const val FILE_NONE = 0
        private const val FILE_CC = 1
        private const val FILE_NDEF = 2

        // SELECT AID for the NDEF application (D2760000850101).
        private val SELECT_NDEF_APP = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
            0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01,
        )

        private val SW_OK = byteArrayOf(0x90.toByte(), 0x00)
        private val SW_ERROR = byteArrayOf(0x6A, 0x82.toByte())
        private val SW_FILE_NOT_FOUND = byteArrayOf(0x6A, 0x82.toByte())

        // Capability Container: version 2.0, NDEF file E104, max 255 bytes, read-only.
        private val CC_FILE = byteArrayOf(
            0x00, 0x0F, 0x20, 0x00, 0x3B, 0x00, 0x34,
            0x04, 0x06, 0xE1.toByte(), 0x04, 0x00, 0xFF.toByte(), 0x00, 0xFF.toByte(),
        )
    }
}