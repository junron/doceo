package com.example.attendance.controllers.classlist

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.example.attendance.controllers.ClasslistController
import com.example.attendance.models.Classlist
import com.example.attendance.models.ClasslistGroup
import com.example.attendance.models.Tags
import com.example.attendance.util.android.ocr.TextAnalyzer
import kotlinx.android.synthetic.main.fragment_main_content.*


object Camera {
    private lateinit var cameraProvider: ProcessCameraProvider
    lateinit var classlistGroup: ClasslistGroup
    lateinit var classlist: Classlist

    fun initCamera() {
        val fragment = ClasslistController.context
        val context = fragment.context ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            this.cameraProvider = cameraProvider
            bindPreview(fragment, classlistGroup, classlist, cameraProvider)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindPreview(
        fragment: Fragment,
        classlistGroup: ClasslistGroup,
        classlist: Classlist,
        cameraProvider: ProcessCameraProvider
    ) {
        val context = fragment.context ?: return
        val selector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val preview = Preview.Builder()
            .setTargetName("Preview")
            .build()


        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        with(fragment) {
            preview.setSurfaceProvider(preview_view.previewSurfaceProvider)
            imageAnalyzer.setAnalyzer(
                ContextCompat.getMainExecutor(context),
                TextAnalyzer {
                    classlistGroup.students.filter { student ->
                        val idName =
                            if (student.shortName.length > 4) student.shortName.toLowerCase()
                            else student.name.toLowerCase()

                        idName in it.toLowerCase() || student.name.toLowerCase() in it.toLowerCase() || student.id.substringBefore(
                            "@"
                        ).toLowerCase() in it.toLowerCase()
                    }.forEach { student ->
                        println("Detected $student")
                        val currentState = classlist.studentState[student.id]
                        if (currentState != Tags.present) {
                            classlist.setStudentState(student, Tags.defaultTags.last())
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(
                                    VibrationEffect.createOneShot(
                                        250,
                                        VibrationEffect.DEFAULT_AMPLITUDE
                                    )
                                )
                            } else {
                                //deprecated in API 26
                                vibrator.vibrate(250)
                            }
                        }
                    }
                }
            )
        }
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            fragment.activity as LifecycleOwner,
            selector,
            imageAnalyzer,
            preview
        )
    }

    fun closeCamera() {
        if (::cameraProvider.isInitialized)
            cameraProvider.unbindAll()
    }

}
