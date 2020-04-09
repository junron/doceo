package com.example.attendance.controllers

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Size
import android.view.Gravity
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager2.widget.ViewPager2
import com.example.attendance.R
import com.example.attendance.adapters.ClasslistPagerAdapter
import com.example.attendance.models.Attendance
import com.example.attendance.models.ClasslistInstance
import com.example.attendance.models.Students
import com.example.attendance.models.Tags
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.Permissions
import com.example.attendance.util.android.Preferences
import com.example.attendance.util.android.nearby.AndroidNearby
import com.example.attendance.util.android.ocr.TextAnalyzer
import com.example.attendance.util.auth.User
import com.example.attendance.util.auth.UserLoader
import com.example.attendance.util.isToday
import com.example.attendance.util.isYesterday
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import kotlinx.android.synthetic.main.fragment_main_content.*
import kotlinx.serialization.UnstableDefault
import java.text.SimpleDateFormat
import java.util.*

@UnstableDefault
object ClasslistController : FragmentController() {
    private lateinit var cameraProvider: ProcessCameraProvider
    lateinit var attendance: Attendance
    private lateinit var callback: () -> Unit
    private val renderListeners = mutableListOf<(Boolean) -> Unit>()
    private var classlist: ClasslistInstance? = null
    private var navigateClasslistId: String? = null

    override fun init(context: Fragment) {
        super.init(context)
        with(context) {
            toolbarClasslistToolbar.apply {
                setNavigationIcon(R.drawable.ic_baseline_close_24)
                navigationIcon?.setTint(Color.WHITE)
                setNavigationOnClickListener {
                    closeCamera()
                    Navigation.navigate(R.id.attendanceFragment)
                }
            }
            if (!UserLoader.getUser().isMentorRep) classlistNavigation.menu.getItem(2).isEnabled =
                false
            classlistNavigation.setOnNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.classlist_tap -> {
                        closeCamera()
                        preview_view.visibility = View.GONE
                    }
                    R.id.classlist_ocr -> {
                        Permissions.requestPermissions(
                            context.activity!!,
                            Manifest.permission.CAMERA
                        )
                        preview_view.visibility = View.VISIBLE
                    }
                    R.id.classlist_advertise -> {
                        Dexter.withActivity(context.activity)
                            .withPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                            .withListener(object : BasePermissionListener() {
                                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                                    AndroidNearby.startDiscovery()
                                    println("Discovering")
                                    Snackbar.make(
                                        classlistViewPager,
                                        "Discovering students...",
                                        Snackbar.LENGTH_INDEFINITE
                                    )
                                        .setAnchorView(classlistNavigation)
                                        .setAction("Stop") {
                                            AndroidNearby.stopDiscovery()
                                        }
                                        .show()
                                }
                            })
                            .check()
                    }
                }
                true
            }
            classlistMore.setOnClickListener {
                drawer_layout_end.openDrawer(Gravity.RIGHT)
            }
            classlistAdd.setOnClickListener {
                navigateClasslistId = attendance.newClasslist()

            }
            classlistViewPager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    val prevClasslist = this@ClasslistController.classlist
                    classlist = this@ClasslistController.attendance.classlists[position]
                    val classlist = this@ClasslistController.classlist ?: return
                    var displayDate = formatDate(classlist.created.toDate())
                    if (displayDate == prevClasslist?.let { formatDate(prevClasslist.created.toDate()) })
                        displayDate = formatDate(classlist.created.toDate(), true)
                    toolbarClasslistToolbar?.subtitle =
                        displayDate + if (position == attendance.classlists.lastIndex) " (Latest)"
                        else ""
                }
            })
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

        val preview = Preview.Builder()
            .setTargetName("Preview")
            .build()


        val students = Students.filterStudents(attendance.constraints.split(" "))
        val vibrator = context.context!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        with(context) {
            preview.setSurfaceProvider(preview_view.previewSurfaceProvider)
            imageAnalyzer.setAnalyzer(
                ContextCompat.getMainExecutor(context),
                TextAnalyzer {
                    students.filter { student ->
                        val idName =
                            if (student.shortName.length > 4) student.shortName.toLowerCase()
                            else student.name.toLowerCase()

                        idName in it.toLowerCase() || student.name.toLowerCase() in it.toLowerCase() || student.id.substringBefore(
                            "@"
                        ).toLowerCase() in it.toLowerCase()
                    }.forEach { student ->
                        val classlist = attendance.classlists[classlistViewPager.currentItem]
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
        cameraProvider.bindToLifecycle(
            context.activity as LifecycleOwner,
            selector,
            imageAnalyzer,
            preview
        )
    }

    private fun closeCamera() {
        if (::cameraProvider.isInitialized)
            cameraProvider.unbindAll()
    }


    fun setClasslist(attendance: Attendance) {
        val run = {
            this.attendance = attendance
            with(context) {
                initializeFields(attendance)
                classlistViewPager.adapter = ClasslistPagerAdapter(
                    attendance,
                    this,
                    Preferences.getShowFullName(attendance.id)
                )
                classlistViewPager.setCurrentItem(attendance.classlists.lastIndex, false)
            }
            attendance.addListener {
                val classlistIndex =
                    it.indexOfFirst { classlistInstance -> classlistInstance.id == navigateClasslistId }
                println(classlistIndex)
                println(navigateClasslistId)
                println(it)
                if (classlistIndex != -1) {
                    context.classlistViewPager.setCurrentItem(classlistIndex, false)
                    navigateClasslistId = null
                }
            }
        }
        if (contextInitialized() && context.view != null) run()
        else callback = run
    }

    private fun initializeFields(attendance: Attendance) {
        this.attendance = attendance
        with(context) {
            toolbarClasslistToolbar.title = attendance.name
            attendanceName.text = attendance.name
            showFullNames.isChecked = Preferences.getShowFullName(attendance.id)
            showFullNames.setOnCheckedChangeListener { _, isChecked ->
                drawer_layout_end.closeDrawer(Gravity.RIGHT)
                Preferences.setFullName(attendance.id, isChecked)
                renderListeners.forEach { it(isChecked) }
            }
        }
    }

    fun attendanceUpdated(attendances: List<Attendance>) {
        if (::attendance.isInitialized)
            attendances.forEach { attendance ->
                if (attendance.id != this.attendance.id) return
                if (attendance.isInitialized())
                    initializeFields(attendance)
            }
    }

    fun addRenderListener(callback: (Boolean) -> Unit) {
        this.renderListeners += callback
    }

    fun onNearbyCompleted(user: User): Boolean {
        with(context) {
            val vibrator = context!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val student = Students.getStudentById(user.email) ?: return false
            val students = Students.filterStudents(attendance.constraints.split(" "))
            val classlist = attendance.classlists[classlistViewPager.currentItem]
            if (student !in students) return false
            val state = classlist.studentState[student.id] ?: return false
            if (state != Tags.present) {
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
        return true
    }


    private fun formatDate(date: Date, seconds: Boolean = false): String {
        val sdf = SimpleDateFormat("dd MMM")
        val sdf2 = SimpleDateFormat("hh:mm${if (seconds) ":ss" else ""} a")
        val day: String = when {
            date.isToday() -> "Today"
            date.isYesterday() -> "Yesterday"
            else -> sdf.format(date)
        }
        return "$day at ${sdf2.format(date)}"
    }
}
