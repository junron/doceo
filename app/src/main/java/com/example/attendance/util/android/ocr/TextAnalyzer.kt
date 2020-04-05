package com.example.attendance.util.android.ocr


import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.pow


val detector = FirebaseVision.getInstance()
    .onDeviceTextRecognizer

class TextAnalyzer(private val callback: (String) -> Unit, private val imageView: ImageView) :
    ImageAnalysis.Analyzer {

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image ?: return
        val imageRotation = degreesToFirebaseRotation(image.imageInfo.rotationDegrees)
        val firebaseImage = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)

        GlobalScope.launch {
            val result = performOcr(firebaseImage.bitmap, imageView)
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
    bitmap: Bitmap,
    outputImageView: ImageView? = null
) =
    suspendCoroutine<String> { cont ->
        executeOcr(bitmap, cont)
        GlobalScope.launch(Dispatchers.Main) {
            outputImageView?.setImageBitmap(bitmap)
        }
//        val mat = Mat()
//        Utils.bitmapToMat(bitmap, mat)
//        val grayImg = Mat()
////        Grayscale
//        Imgproc.cvtColor(mat, grayImg, Imgproc.COLOR_BGR2GRAY)
//        val thresh = Mat()
////        Otsu Thresholding
//        Imgproc.threshold(grayImg, thresh, -1.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)
////        Contouring
//        val contours = mutableListOf<MatOfPoint>()
//        Imgproc.findContours(
//            thresh,
//            contours,
//            Mat(),
//            Imgproc.RETR_TREE,
//            Imgproc.CHAIN_APPROX_SIMPLE
//        )
//        val imageCenter = Point(mat.width() / 2.0, mat.height() / 2.0)
//        val contour = contours.filter {
//            Imgproc.contourArea(it) > 1000
//        }.minBy {
//            computeDistanceToCenter(it, imageCenter.x, imageCenter.y)
//        } ?: return@suspendCoroutine cont.resume("")
//        val boundingRect = Imgproc.boundingRect(contour)
//        val cropped = grayImg.submat(boundingRect)
//        val resized = Mat()
//        Imgproc.resize(
//            cropped,
//            resized,
//            Size(cropped.cols() * 2.0, cropped.rows() * 2.0),
//            0.0, 0.0,
//            Imgproc.INTER_CUBIC
//        )
//        if (outputImageView != null) {
//            val niceImg = Mat()
//            mat.copyTo(niceImg)
//            Imgproc.drawContours(niceImg, listOf(contour), -1, Scalar(0.0, 255.0, 0.0), 10)
//            val displayBitmap =
//                Bitmap.createBitmap(mat.width(), mat.height(), bitmap.config)
//            Utils.matToBitmap(niceImg, displayBitmap)
//            GlobalScope.launch(Dispatchers.Main) {
//                outputImageView.setImageBitmap(displayBitmap)
//            }
//        }
//        val analyzeBitmap = Bitmap.createBitmap(resized.width(), resized.height(), bitmap.config)
//        Utils.matToBitmap(resized, analyzeBitmap)
//        if (analyzeBitmap.height < 32 || analyzeBitmap.width < 32) {
//            cont.resume("")
//        } else executeOcr(analyzeBitmap, cont)
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

private fun computeDistanceToCenter(mat: Mat, imgX: Double, imgY: Double): Double {
    val p = Imgproc.moments(mat)
    val x = p._m10 / p._m00
    val y = p._m01 / p._m00
    return (x - imgX).pow(2) + (y - imgY).pow(2)
}
