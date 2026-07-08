package com.tertiaryinfotech.tapcard.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
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

    /**
     * Generates a square QR [Bitmap] encoding the card's vCard, with the Tapcard
     * brand mark in the centre. Uses high error correction so the mark doesn't
     * break scannability.
     */
    fun qrBitmap(card: DigitalCard, size: Int = 720, branded: Boolean = true): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1,
        )
        val matrix = QRCodeWriter().encode(build(card), BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        if (branded) drawBrandMark(bmp, size)
        return bmp
    }

    /** Draws a small rounded Tapcard "card" badge in the centre of the QR. */
    private fun drawBrandMark(bmp: Bitmap, size: Int) {
        val canvas = Canvas(bmp)
        val c = size / 2f
        val badge = size * 0.22f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // White rounded backing so QR modules don't clash with the mark.
        paint.color = Color.WHITE
        val bg = badge
        canvas.drawRoundRect(RectF(c - bg / 2, c - bg / 2, c + bg / 2, c + bg / 2), bg * 0.28f, bg * 0.28f, paint)

        // Blue rounded "card".
        paint.color = Color.parseColor("#2563EB")
        val card = badge * 0.72f
        val cardRect = RectF(c - card / 2, c - card / 2, c + card / 2, c + card / 2)
        canvas.drawRoundRect(cardRect, card * 0.24f, card * 0.24f, paint)

        // White stripe on the card (mimics the app's credit-card glyph).
        paint.color = Color.WHITE
        val stripeH = card * 0.14f
        val stripeTop = c - card * 0.12f
        canvas.drawRoundRect(
            RectF(c - card * 0.30f, stripeTop, c + card * 0.30f, stripeTop + stripeH),
            stripeH / 2, stripeH / 2, paint,
        )
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
