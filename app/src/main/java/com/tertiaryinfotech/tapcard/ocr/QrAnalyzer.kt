package com.tertiaryinfotech.tapcard.ocr

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.tertiaryinfotech.tapcard.model.DigitalCard

/**
 * Live camera analyzer that reads QR / barcodes off the preview stream and fires
 * [onCard] the first time it sees one carrying usable contact info or a profile
 * link — so the user just points the camera, no shutter tap needed.
 */
class QrAnalyzer(private val onCard: (DigitalCard) -> Unit) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @Volatile
    private var handled = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (handled) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (!handled) {
                    CardScanner.cardFromBarcodes(barcodes)?.let { card ->
                        handled = true
                        onCard(card)
                    }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
