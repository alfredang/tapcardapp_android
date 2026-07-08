package com.tertiaryinfotech.tapcard.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.ui.theme.cardTheme
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a 1920×1080 virtual meeting background for a card — a themed gradient
 * with a white panel holding the card's QR code and name/title, so people on a
 * Zoom / Teams / Meet call can scan it. Shareable out of the app's cache.
 */
object VirtualBackground {

    private const val W = 1920
    private const val H = 1080

    fun generate(card: DigitalCard): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val theme = cardTheme(card.theme)

        // Themed gradient backdrop + a soft top-right glow.
        val bg = Paint().apply {
            shader = LinearGradient(
                0f, 0f, W.toFloat(), H.toFloat(),
                theme.bannerStart.toArgb(), theme.bannerEnd.toArgb(), Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bg)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                W * 0.82f, H * 0.18f, 560f,
                Color.argb(70, 255, 255, 255), Color.argb(0, 255, 255, 255), Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), glow)

        // White info panel (bottom-left).
        val left = 110f
        val top = H - 450f
        val right = 1060f
        val bottom = H - 110f
        val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(RectF(left, top, right, bottom), 44f, 44f, panel)

        // QR, vertically centered in the panel.
        val qrSize = 300
        val qr = VCard.qrBitmap(card, qrSize)
        val qrTop = top + (bottom - top - qrSize) / 2f
        canvas.drawBitmap(qr, left + 30f, qrTop, null)

        // Text block.
        val textX = left + 30f + qrSize + 44f
        canvas.drawText(
            card.displayName, textX, top + 140f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#14141F"); textSize = 66f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            },
        )
        if (card.subtitle.isNotBlank()) {
            canvas.drawText(
                card.subtitle, textX, top + 200f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6B6B80"); textSize = 40f },
            )
        }
        canvas.drawText(
            "Scan to save my contact", textX, top + 272f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.accent.toArgb(); textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            },
        )
        return bmp
    }

    fun share(context: Context, card: DigitalCard) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "tapcard-bg-${card.id}.png")
        FileOutputStream(file).use { generate(card).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${card.displayName} — virtual background")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Save or share background").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}