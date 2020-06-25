package com.example.attendance.controllers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Size
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager2.widget.ViewPager2
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.adapters.ClasslistPagerAdapter
import com.example.attendance.models.*
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.Permissions
import com.example.attendance.util.android.Preferences
import com.example.attendance.util.android.Sharing
import com.example.attendance.util.android.nearby.AndroidNearby
import com.example.attendance.util.android.ocr.TextAnalyzer
import com.example.attendance.util.auth.User
import com.example.attendance.util.auth.UserLoader
import com.example.attendance.util.isToday
import com.example.attendance.util.isYesterday
import com.example.attendance.util.suffix
import com.example.attendance.util.toStringValue
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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
        MainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
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
                        classlistViewPager.visibility = View.VISIBLE
                        preview_view.visibility = View.GONE
                        pieChartContainer.visibility = View.GONE
                    }
                    R.id.classlist_ocr -> {
                        Permissions.requestPermissions(
                            context.activity!!,
                            Manifest.permission.CAMERA
                        )
                        classlistViewPager.visibility = View.VISIBLE
                        preview_view.visibility = View.VISIBLE
                        pieChartContainer.visibility = View.GONE
                    }
                    R.id.classlist_advertise -> {
                        AndroidNearby.startAdvertising()
                        Snackbar.make(
                            classlistViewPager,
                            "Discovering students...",
                            Snackbar.LENGTH_INDEFINITE
                        )
                            .setAnchorView(classlistNavigation)
                            .setAction("Stop") {
                                AndroidNearby.stopAdvertising()
                            }
                            .show()
                    }
                    R.id.classlist_analyze -> {
                        preview_view.visibility = View.GONE
                        classlistViewPager.visibility = View.GONE
                        val (entries, colors) = getPieChartData()
                            ?: return@setOnNavigationItemSelectedListener false
                        val pieData = PieData(
                            PieDataSet(
                                entries,
                                attendance.name
                            ).apply {
                                this.colors = colors
                            }
                        )
                        pieData.setValueTextSize(13f)
                        pieData.setValueFormatter(object : ValueFormatter() {
                            override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                                return value.toInt().suffix("student")
                            }
                        })
                        pieChart.isDrawHoleEnabled = false
                        pieChart.data = pieData
                        pieChart.legend.isEnabled = false
                        pieChart.description.text = ""
                        pieChartContainer.visibility = View.VISIBLE
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
                    if (prevClasslist != null && prevClasslist.id != classlist.id &&
                        displayDate == formatDate(prevClasslist.created.toDate())
                    )
                        displayDate = formatDate(classlist.created.toDate(), true)
                    toolbarClasslistToolbar?.subtitle =
                        displayDate + if (position == attendance.classlists.lastIndex) " (Latest)"
                        else ""
                }
            })
            classlistExport.setOnClickListener {
                drawer_layout_end.closeDrawer(Gravity.RIGHT)
                val file = context.context!!.filesDir.resolve(
                    "${attendance.name}_${attendance.created.toDate().toStringValue()}.csv"
                )
                val data =
                    getStudentStates()?.entries?.sortedBy { (k, _) -> k.id }
                        ?.map { (student, tag) ->
                            listOf(student.name, tag.name)
                        } ?: return@setOnClickListener
                val rows =
                    listOf(listOf("Name", "Status")) + data
                csvWriter().open(file) {
                    writeAll(rows)
                }
                MaterialAlertDialogBuilder(context.context!!)
                    .setTitle("Export classlist")
                    .setMessage("Classlist ${attendance.name} has been exported!")
                    .setPositiveButton("Share") { _, _ ->
                        startActivity(
                            Intent.createChooser(
                                Sharing.shareCSV(
                                    context.context!!,
                                    Intent.ACTION_SEND,
                                    file
                                ), "Share CSV"
                            )
                        )
                    }
                    .setNeutralButton("Open") { _, _ ->
                        startActivity(
                            Intent.createChooser(
                                Sharing.shareCSV(
                                    context.context!!,
                                    Intent.ACTION_VIEW,
                                    file
                                ), "Open CSV"
                            )
                        )
                    }.show()
            }
        }
        if (::callback.isInitialized)
            callback()
    }

    private fun getStudentStates(): Map<Student, Tag>? {
        val classlist = classlist ?: return null
        val data = classlist
            .studentState.mapNotNull { (k, v) ->
                val student = attendance.students.find { it.id == k } ?: return@mapNotNull null
                val tag = attendance.getParsedTags()
                    .find { it.id == v } ?: return@mapNotNull null
                student to tag
            }.toMap().toMutableMap()
        val defaultTag = attendance.getParsedTags().find { it.id == Tags.absent }!!
        attendance.students.forEach {
            if (data[it] == null) data[it] = defaultTag
        }
        return data
    }

    private fun getPieChartData(): Pair<List<PieEntry>, List<Int>>? {
        val studentData = getStudentStates() ?: return null
        val data = studentData.entries.groupBy { it.value }
            .map { (tag, students) -> tag to students.size }.toMap()
        val colors = mutableListOf<Int>()
        val entries = data.mapNotNull { (tag, v) ->
            if (v == 0) return@mapNotNull null
            colors += tag.color
            PieEntry(v.toFloat(), tag.name)
        }
        return (entries to colors.toList())
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


        val vibrator = context.context!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        with(context) {
            preview.setSurfaceProvider(preview_view.previewSurfaceProvider)
            imageAnalyzer.setAnalyzer(
                ContextCompat.getMainExecutor(context),
                TextAnalyzer {
                    attendance.students.filter { student ->
                        val idName =
                            if (student.shortName.length > 4) student.shortName.toLowerCase()
                            else student.name.toLowerCase()

                        idName in it.toLowerCase() || student.name.toLowerCase() in it.toLowerCase() || student.id.substringBefore(
                            "@"
                        ).toLowerCase() in it.toLowerCase()
                    }.forEach { student ->
                        println("Detected $student")
                        val classlist = classlist ?: return@TextAnalyzer
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


    fun setClasslist(attendance: Attendance, preserveIndex: Boolean = false) {
        val run = {
            this.attendance = attendance
            attendance.loadClasslists()
            with(context) {
                initializeFields(attendance)
                if (!attendance.isInitialized()) return@with
                this@ClasslistController.classlist = attendance.classlists.last()
                classlistViewPager.adapter = ClasslistPagerAdapter(
                    attendance,
                    this,
                    Preferences.getShowFullName(attendance.id)
                )
                if (preserveIndex) {
                    var index = attendance.classlists.indexOfFirst { it.id == classlist?.id }
                    if (index == -1) index = attendance.classlists.lastIndex
                    classlistViewPager.setCurrentItem(index, false)
                } else classlistViewPager.setCurrentItem(attendance.classlists.lastIndex, false)
                UserLoader.getUser()
            }
            attendance.addListener {
                this.classlist =
                    it.find { classlistInstance -> classlistInstance.id == classlist?.id }
                val classlistIndex =
                    it.indexOfFirst { classlistInstance -> classlistInstance.id == navigateClasslistId }
                if (classlistIndex != -1) {
                    context.classlistViewPager.setCurrentItem(classlistIndex, true)
                    navigateClasslistId = null
                }
                try {
                    with(context) {
                        if (pieChart.data == null) return@with
                        val dataset = pieChart.data.dataSet
                        dataset.clear()
                        val (entries, colors) = getPieChartData() ?: return@with
                        (dataset as PieDataSet).colors = colors
                        entries.forEach { entry -> dataset.addEntry(entry) }
                        pieChart.notifyDataSetChanged()
                        pieChart.invalidate()
                    }
                } catch (e: NullPointerException) {
                }
            }
        }
        if (contextInitialized() && context.view != null) run()
        else callback = run
    }

    private fun initializeFields(attendance: Attendance) {
        this.attendance = attendance
        if (context.view == null) return
        with(context) {
            toolbarClasslistToolbar.title = attendance.name
            attendanceName.text = attendance.name
            showFullNames.isChecked = Preferences.getShowFullName(attendance.id)
            showFullNames.setOnCheckedChangeListener { _, isChecked ->
                drawer_layout_end.closeDrawer(Gravity.RIGHT)
                Preferences.setFullName(attendance.id, isChecked)
                renderListeners.forEach { it(isChecked) }
            }
            detailsClose.setOnClickListener {
                drawer_layout_end.closeDrawer(Gravity.RIGHT)
            }
            val user = UserLoader.getUser()
            val accessLevel = attendance.getAccessLevel(user.email)
            if (accessLevel == AccessLevel.VIEWER) {
                classlistAdd.visibility = View.GONE
                // Disable OCR and Nearby
                classlistNavigation.menu.getItem(1).isEnabled = false
                classlistNavigation.menu.getItem(2).isEnabled = false
                classlistNavigation.menu.getItem(0).apply {
                    title = "View"
                    setIcon(R.drawable.ic_eye_24)
                }
            } else {
                classlistAdd.visibility = View.VISIBLE
                classlistNavigation.menu.getItem(0).apply {
                    title = "Tap"
                    setIcon(R.drawable.ic_baseline_touch_app_24)
                }
                classlistNavigation.menu.getItem(1).isEnabled = true
                classlistNavigation.menu.getItem(2).isEnabled = user.isMentorRep
            }
        }
    }

    fun attendanceUpdated(attendances: List<Attendance>) {
        if (::attendance.isInitialized && context.view != null) {
            val newAttendance = attendances.find { it.id == attendance.id }
            if (newAttendance == null) {
                Toast.makeText(
                    context.context!!,
                    "You no longer have access to this classlist.",
                    Toast.LENGTH_SHORT
                )
                    .show()
                Navigation.navigate(R.id.attendanceFragment)
                return
            }
            if (newAttendance == attendance) return
            setClasslist(newAttendance, true)
        }
    }

    fun addRenderListener(callback: (Boolean) -> Unit) {
        this.renderListeners += callback
    }

    fun onNearbyCompleted(user: User): Boolean {
        with(context) {
            val vibrator = context!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val student = Students.getStudentById(user.email) ?: return false
            val classlist = attendance.classlists[classlistViewPager.currentItem]
            if (student !in attendance.students) return false
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
