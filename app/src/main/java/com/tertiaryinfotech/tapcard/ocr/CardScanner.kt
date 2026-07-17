package com.tertiaryinfotech.tapcard.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tertiaryinfotech.tapcard.model.DigitalCard
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Thin wrapper around ML Kit's on-device Latin text recognizer. Everything runs
 * locally on the phone — no network, no API key — matching the iOS app's
 * "OCR stays on device" privacy posture.
 */
object CardScanner {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val barcodeScanner = BarcodeScanning.getClient()

    /** Recognises all text in the image at [uri] and returns it as raw lines. */
    suspend fun recognize(context: Context, uri: Uri): String =
        suspendCancellableCoroutine { cont ->
            val image = try {
                InputImage.fromFilePath(context, uri)
            } catch (e: Exception) {
                cont.resumeWithException(e)
                return@suspendCancellableCoroutine
            }
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    /**
     * Looks for a QR / barcode in the image and, if one carries contact details
     * (a vCard/MECARD QR) or a profile link (e.g. LinkedIn), maps it to a card.
     * Returns null when there's no usable code — callers then fall back to OCR.
     */
    suspend fun scanCardFromQr(context: Context, uri: Uri): DigitalCard? {
        val barcodes = suspendCancellableCoroutine<List<Barcode>> { cont ->
            val image = try {
                InputImage.fromFilePath(context, uri)
            } catch (e: Exception) {
                cont.resumeWithException(e)
                return@suspendCancellableCoroutine
            }
            barcodeScanner.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

        return cardFromBarcodes(barcodes)
    }

    /**
     * Maps already-detected barcodes to a card — shared by still-image capture and
     * the live camera analyzer. Prefers structured contact info (vCard/MECARD QR),
     * else accepts a profile / web link (LinkedIn, a Tapcard card URL, …).
     * Returns null when there's no usable code.
     */
    fun cardFromBarcodes(barcodes: List<Barcode>): DigitalCard? {
        barcodes.firstNotNullOfOrNull { it.contactInfo?.takeIf(::hasContact)?.let(::fromContactInfo) }
            ?.let { return it }

        val link = barcodes.firstNotNullOfOrNull { it.url?.url ?: it.rawValue }
            ?.takeIf { it.startsWith("http", ignoreCase = true) }
        return link?.let(::fromLink)
    }

    private fun hasContact(info: Barcode.ContactInfo): Boolean =
        !info.name?.formattedName.isNullOrBlank() ||
            info.phones.isNotEmpty() || info.emails.isNotEmpty()

    private fun fromContactInfo(info: Barcode.ContactInfo): DigitalCard {
        val name = info.name?.formattedName?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(info.name?.first, info.name?.last).joinToString(" ").trim()
        val urls = info.urls.orEmpty()
        return DigitalCard(
            name = name,
            title = info.title.orEmpty(),
            company = info.organization.orEmpty(),
            phone = info.phones.firstOrNull()?.number.orEmpty(),
            email = info.emails.firstOrNull()?.address.orEmpty(),
            linkedin = urls.firstOrNull { it.contains("linkedin", ignoreCase = true) }.orEmpty(),
            website = urls.firstOrNull { !it.contains("linkedin", ignoreCase = true) }.orEmpty(),
        )
    }

    /** Routes a scanned profile/web link into the right field by its domain. */
    private fun fromLink(url: String): DigitalCard {
        fun has(vararg needles: String) = needles.any { url.contains(it, ignoreCase = true) }
        return when {
            has("linkedin.") -> DigitalCard(linkedin = url)
            has("instagram.") -> DigitalCard(instagram = url)
            has("facebook.", "fb.com") -> DigitalCard(facebook = url)
            has("tiktok.") -> DigitalCard(tiktok = url)
            has("t.me", "telegram.") -> DigitalCard(telegram = url)
            has("youtube.", "youtu.be") -> DigitalCard(youtube = url)
            has("twitter.", "x.com") -> DigitalCard(twitter = url)
            else -> DigitalCard(website = url)
        }
    }
}
