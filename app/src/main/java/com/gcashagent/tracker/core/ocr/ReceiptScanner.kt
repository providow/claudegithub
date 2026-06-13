package com.gcashagent.tracker.core.ocr

import android.content.Context
import android.net.Uri
import com.gcashagent.tracker.core.util.ReceiptParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * On-device, offline OCR of GCash screenshots using ML Kit Text Recognition
 * (the bundled Latin model ships inside the APK — no network, no Play Services
 * download). Recognized text is handed to [ReceiptParser] for the amount / ref.
 */
class ReceiptScanner(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** OCR the image at [uri]; returns a best-effort amount + reference (nulls on failure). */
    suspend fun scan(uri: Uri): ReceiptParser.ParsedReceipt {
        val text = recognizeText(uri) ?: return ReceiptParser.ParsedReceipt(null, null)
        return ReceiptParser.parse(text)
    }

    private suspend fun recognizeText(uri: Uri): String? =
        suspendCancellableCoroutine { cont ->
            try {
                val image = InputImage.fromFilePath(context, uri)
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it.text) }
                    .addOnFailureListener { cont.resume(null) }
            } catch (e: Exception) {
                cont.resume(null)
            }
        }
}
