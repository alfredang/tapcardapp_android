package com.tertiaryinfotech.tapcard.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.tertiaryinfotech.tapcard.model.DigitalCard
import java.io.File
import java.io.FileOutputStream

/**
 * Builds the vCard (VCF) representation of a card, encodes it as a QR bitmap,
 * and shares it out of the app's private cache via FileProvider. The vCard is
 * the universal "save contact" payload — scanning the QR drops the contact
 * straight into iOS / Android / Google / Outlook address books.
 */
object VCard {

    /** RFC 6350 vCard 3.0 text for a card. */
    fun build(card: DigitalCard): String = buildString {
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")
        if (card.name.isNotBlank()) {
            appendLine("N:${card.name};;;")
            appendLine("FN:${card.name}")
        }
        if (card.company.isNotBlank()) appendLine("ORG:${card.company}")
        if (card.title.isNotBlank()) appendLine("TITLE:${card.title}")
        if (card.phone.isNotBlank()) appendLine("TEL;TYPE=CELL:${card.phone}")
        if (card.email.isNotBlank()) appendLine("EMAIL;TYPE=INTERNET:${card.email}")
        if (card.website.isNotBlank()) appendLine("URL:${normalizeUrl(card.website)}")
        if (card.address.isNotBlank()) appendLine("ADR;TYPE=WORK:;;${card.address};;;;")
        append("END:VCARD")
    }

    /** Generates a square QR [Bitmap] encoding the card's vCard. */
    fun qrBitmap(card: DigitalCard, size: Int = 720): Bitmap {
        val matrix = QRCodeWriter().encode(build(card), BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    /** Writes the QR PNG to cache/share and returns a shareable content:// Uri. */
    fun shareQr(context: Context, card: DigitalCard) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "tapcard-qr-${card.id}.png")
        FileOutputStream(file).use { qrBitmap(card).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${card.displayName} — digital business card")
            putExtra(Intent.EXTRA_TEXT, "Scan to save my contact. Powered by Tapcard.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share card").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    /** Opens the system "add contact" sheet pre-filled from the card. */
    fun addToContacts(context: Context, card: DigitalCard) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = android.provider.ContactsContract.Contacts.CONTENT_TYPE
            putExtra(android.provider.ContactsContract.Intents.Insert.NAME, card.name)
            putExtra(android.provider.ContactsContract.Intents.Insert.COMPANY, card.company)
            putExtra(android.provider.ContactsContract.Intents.Insert.JOB_TITLE, card.title)
            putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, card.phone)
            putExtra(android.provider.ContactsContract.Intents.Insert.EMAIL, card.email)
            putExtra(android.provider.ContactsContract.Intents.Insert.POSTAL, card.address)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun normalizeUrl(url: String): String =
        if (url.startsWith("http", ignoreCase = true)) url else "https://$url"
}
