package com.example.attendance.controllers

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Size
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.example.attendance.R
import com.example.attendance.adapters.ClasslistPagerAdapter
import com.example.attendance.models.Attendance
import com.example.attendance.models.ClasslistEvent
import com.example.attendance.models.Students
import com.example.attendance.models.Tags
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.Permissions
import com.example.attendance.util.android.ocr.TextAnalyzer
import com.example.attendance.util.toDate
import kotlinx.android.synthetic.main.fragment_main_content.*
import kotlinx.serialization.UnstableDefault

@UnstableDefault
object ClasslistController : FragmentController() {
    private lateinit var cameraProvider: ProcessCameraProvider
    lateinit var attendance: Attendance
    private lateinit var callback: () -> Unit

    override fun init(context: Fragment) {
        super.init(context)
        with(context) {
            toolbarClasslistToolbar.apply {
                setNavigationIcon(R.drawable.ic_baseline_close_24)
                navigationIcon?.setTint(Color.WHITE)
                setNavigationOnClickListener {
                    Navigation.navigate(R.id.attendanceFragment)
                }
            }
            classlistNavigation.setOnNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.classlist_tap -> {
                        closeCamera()
                        outputImageView.visibility = View.GONE
                    }
                    R.id.classlist_ocr -> {
                        Permissions.requestPermissions(
                            context.activity!!,
                            Manifest.permission.CAMERA
                        )
                        outputImageView.visibility = View.VISIBLE
                    }
                }
                true
            }
        }
        if (::callback.isInitialized)
            callback()
    }

    fun initCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context.context!!)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            this.cameraProvider = cameraProvider
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(context.context))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val selector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val students = Students.filterStudents(attendance.constraints.split(" "))
        val vibrator = context.context!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        with(context) {
            imageAnalyzer.setAnalyzer(
                ContextCompat.getMainExecutor(context),
                TextAnalyzer({
                    students.filter { student ->
                        val idName =
                            if (student.shortName.length > 4) student.shortName.toLowerCase()
                            else student.name.toLowerCase()

                        idName in it.toLowerCase() || student.name.toLowerCase() in it.toLowerCase() || student.id.substringBefore(
                            "@"
                        ).toLowerCase() in it.toLowerCase()
                    }.forEach { student ->
                        val classlist = attendance.classlists.last()
                        val currentState = classlist.getParsedEvents()
                            .filter { it is ClasslistEvent.StateChanged && it.targetId == student.id }
                            .map { it as ClasslistEvent.StateChanged }
                            .maxBy { it.timestamp.toDate() }?.state
                        if (currentState != Tags.present) {
                            classlist.addEvent(
                                ClasslistEvent.StateChanged(
                                    student.id,
                                    Tags.present
                                )
                            )
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
                }, outputImageView)
            )
        }
        cameraProvider.bindToLifecycle(
            context.activity as LifecycleOwner,
            selector,
            imageAnalyzer
        )
    }

    fun closeCamera() {
        if (::cameraProvider.isInitialized)
            cameraProvider.unbindAll()
    }


    fun setClasslist(attendance: Attendance) {
        val run = {
            this.attendance = attendance
            with(context) {
                toolbarClasslistToolbar.title = attendance.name
                classlistViewPager.adapter = ClasslistPagerAdapter(attendance, this)
            }
        }
        if (contextInitialized() && context.view != null) run()
        else callback = run
    }
}
