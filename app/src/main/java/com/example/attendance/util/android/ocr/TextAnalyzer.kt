package com.example.attendance.util.android.ocr


import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


val detector = FirebaseVision.getInstance()
    .onDeviceTextRecognizer

class TextAnalyzer(private val callback: (String) -> Unit) : ImageAnalysis.Analyzer {

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image ?: return
        val imageRotation = degreesToFirebaseRotation(image.imageInfo.rotationDegrees)
        val firebaseImage = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)

        GlobalScope.launch {
            val result = performOcr(firebaseImage.bitmap)
            image.close()
            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }

    private fun degreesToFirebaseRotation(degrees: Int): Int = when (degrees) {
        0 -> FirebaseVisionImageMetadata.ROTATION_0
        90 -> FirebaseVisionImageMetadata.ROTATION_90
        180 -> FirebaseVisionImageMetadata.ROTATION_180
        270 -> FirebaseVisionImageMetadata.ROTATION_270
        else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
    }
}

suspend fun performOcr(
    bitmap: Bitmap
) =
    suspendCoroutine<String> { cont ->
        executeOcr(bitmap, cont)
    }

private fun executeOcr(bitmap: Bitmap, cont: Continuation<String>) {
    detector.processImage(FirebaseVisionImage.fromBitmap(bitmap))
        .addOnSuccessListener { firebaseVisionText ->
            cont.resume(firebaseVisionText.text)
        }
        .addOnFailureListener { e ->
            println("Exception: $e")
            cont.resumeWithException(e)
        }
}
