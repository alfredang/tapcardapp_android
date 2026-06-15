package com.tertiaryinfotech.tapcard.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
}
