package com.tertiaryinfotech.tapcard.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Stores card images inline as base64 "data URLs" — the quick, no-external-
 * storage approach: pick/capture -> downscale -> JPEG -> data URL. The string
 * syncs to the backend like any other field and renders on the web via <img>.
 */
object CardImages {

    /**
     * Reads the image at [uri], downscales it to fit [maxDim] px, compresses to
     * JPEG, and returns "data:image/jpeg;base64,..." (or null on failure).
     * Kept small on purpose so the encoded string stays well under the backend cap.
     */
    suspend fun uriToDataUrl(
        context: Context,
        uri: Uri,
        maxDim: Int = 720,
        quality: Int = 80,
    ): String? = withContext(Dispatchers.IO) {
        val bitmap = decodeScaled(context, uri, maxDim) ?: return@withContext null
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        bitmap.recycle()
        "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Turns a stored image string into something Coil can load: a decoded
     * ByteBuffer for inline data URLs, or the raw URL otherwise. Null when blank.
     */
    fun model(value: String): Any? {
        if (value.isBlank()) return null
        if (value.startsWith("data:")) {
            val base64 = value.substringAfter(",", "")
            return runCatching { ByteBuffer.wrap(Base64.decode(base64, Base64.DEFAULT)) }.getOrNull()
        }
        return value
    }

    private fun decodeScaled(context: Context, uri: Uri, maxDim: Int): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.isMutableRequired = false
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                val longest = maxOf(w, h)
                if (longest > maxDim && longest > 0) {
                    val scale = maxDim.toFloat() / longest
                    decoder.setTargetSize(
                        (w * scale).toInt().coerceAtLeast(1),
                        (h * scale).toInt().coerceAtLeast(1),
                    )
                }
            }
        } else {
            // API 26/27 fallback: sample down with BitmapFactory.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            while (longest / sample > maxDim) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        }
    }.getOrNull()
}
