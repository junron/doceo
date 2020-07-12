package com.example.attendance.controllers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.adapters.attendance.ClasslistPagerAdapter
import com.example.attendance.controllers.classlist.Camera
import com.example.attendance.controllers.classlist.export
import com.example.attendance.controllers.classlist.formatDate
import com.example.attendance.controllers.classlist.renderChart
import com.example.attendance.models.*
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.Permissions
import com.example.attendance.util.android.Preferences
import com.example.attendance.util.android.nearby.AndroidNearby
import com.example.attendance.util.auth.User
import com.example.attendance.util.auth.UserLoader
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_main_content.*
import kotlinx.serialization.UnstableDefault

// Controller for classlist holder controls
@SuppressLint("RtlHardcoded")
object ClasslistController : FragmentController() {
    lateinit var classlistGroup: ClasslistGroup
    private lateinit var callback: () -> Unit
    private val renderListeners = mutableListOf<(Boolean) -> Unit>()
    private lateinit var classlist: Classlist
    private var navigateClasslistId: String? = null

    @UnstableDefault
    override fun init(context: Fragment) {
        super.init(context)
        MainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        with(context) {
            toolbarClasslistToolbar.apply {
                setNavigationIcon(R.drawable.ic_baseline_close_24)
                navigationIcon?.setTint(Color.WHITE)
                setNavigationOnClickListener {
                    Camera.closeCamera()
                    Navigation.navigate(R.id.attendanceFragment)
                }
            }
            if (!UserLoader.getUser().isMentorRep) classlistNavigation.menu.getItem(2).isEnabled =
                false
            classlistNavigation.setOnNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.classlist_tap -> {
                        Camera.closeCamera()
                        classlistViewPager.visibility = View.VISIBLE
                        preview_view.visibility = View.GONE
                        pieChartContainer.visibility = View.GONE
                    }
                    R.id.classlist_ocr -> {
                        Permissions.requestPermissions(
                            context.activity!!,
                            Manifest.permission.CAMERA
                        )
                        Camera.classlist = classlist
                        Camera.classlistGroup = classlistGroup
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
                        renderChart(this, classlistGroup, classlist)
                    }
                }
                true
            }
            classlistMore.setOnClickListener {
                drawer_layout_end.openDrawer(Gravity.RIGHT)
            }
            classlistAdd.setOnClickListener {
                navigateClasslistId = classlistGroup.newClasslist()
            }
            classlistViewPager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    val prevClasslist = this@ClasslistController.classlist
                    classlist = this@ClasslistController.classlistGroup.classlists[position]
                    var displayDate = formatDate(classlist.created.toDate())
                    if (prevClasslist.id != classlist.id &&
                        displayDate == formatDate(prevClasslist.created.toDate())
                    ) {
                        displayDate = formatDate(classlist.created.toDate(), true)
                    }
                    toolbarClasslistToolbar?.subtitle =
                        displayDate + if (position == classlistGroup.classlists.lastIndex) " (Latest)"
                        else ""
                }
            })
            classlistExport.setOnClickListener {
                drawer_layout_end.closeDrawer(Gravity.RIGHT)
                export(context.context!!, classlistGroup, classlist)
            }
        }
        if (::callback.isInitialized)
            callback()
    }

    @UnstableDefault
    fun setClasslistGroup(classlistGroup: ClasslistGroup, preserveIndex: Boolean = false) {
        val run = {
            this.classlistGroup = classlistGroup
            classlistGroup.loadClasslists()
            with(context) {
                // Render info about classlist group
                initializeFields(classlistGroup)
                if (!classlistGroup.isInitialized()) return@with
                this@ClasslistController.classlist = classlistGroup.classlists.last()
                classlistViewPager.adapter =
                    ClasslistPagerAdapter(
                        classlistGroup,
                        this,
                        Preferences.getShowFullName(classlistGroup.id)
                    )
                if (preserveIndex) {
                    var index = classlistGroup.classlists.indexOfFirst { it.id == classlist.id }
                    if (index == -1) index = classlistGroup.classlists.lastIndex
                    classlistViewPager.setCurrentItem(index, false)
                } else classlistViewPager.setCurrentItem(classlistGroup.classlists.lastIndex, false)
                // TODO: WHY???
                UserLoader.getUser()
            }
            classlistGroup.addListener { classlists ->
                // Update classlist
                val targetClasslistId = navigateClasslistId ?: classlist.id
                var classlistIndex =
                    classlists.indexOfFirst { classlistInstance -> classlistInstance.id == targetClasslistId }
                // Classlist not found
                if (classlistIndex == -1) {
                    if (classlists.isNotEmpty()) classlistIndex = classlists.lastIndex
                    else return@addListener
                }
                this.classlist = classlists[classlistIndex]
                // println("Classlist updated: $classlist")
                context.classlistViewPager.setCurrentItem(classlistIndex, true)
                navigateClasslistId = null
                try {
                    if (context.classlistNavigation.selectedItemId == R.id.classlist_analyze)
                        renderChart(context, classlistGroup, classlist)
                } catch (e: NullPointerException) {
                }
            }
        }
        if (contextInitialized() && context.view != null) run()
        else callback = run
    }

    @UnstableDefault
    private fun initializeFields(classlistGroup: ClasslistGroup) {
        this.classlistGroup = classlistGroup
        if (context.view == null) return
        with(context) {
            toolbarClasslistToolbar.title = classlistGroup.name
            attendanceName.text = classlistGroup.name
            showFullNames.isChecked = Preferences.getShowFullName(classlistGroup.id)
            showFullNames.setOnCheckedChangeListener { _, isChecked ->
                drawer_layout_end.closeDrawer(Gravity.RIGHT)
                Preferences.setFullName(classlistGroup.id, isChecked)
                renderListeners.forEach { it(isChecked) }
            }
            detailsClose.setOnClickListener {
                drawer_layout_end.closeDrawer(Gravity.RIGHT)
            }
            val user = UserLoader.getUser()
            val accessLevel = classlistGroup.getAccessLevel(user.email)
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

    @UnstableDefault
    fun classlistGroupUpdated(classlistGroups: List<ClasslistGroup>) {
        if (::classlistGroup.isInitialized && context.view != null) {
            val newAttendance = classlistGroups.find { it.id == classlistGroup.id }
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
            if (newAttendance == classlistGroup) return
            setClasslistGroup(newAttendance, true)
        }
    }

    fun addRenderListener(callback: (Boolean) -> Unit) {
        this.renderListeners += callback
    }

    fun onNearbyCompleted(user: User): Boolean {
        with(context) {
            val vibrator = context!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val student = Students.getStudentById(user.email) ?: return false
            val classlist = classlistGroup.classlists[classlistViewPager.currentItem]
            if (student !in classlistGroup.students) return false
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
                    //deprecated in API level 26
                    vibrator.vibrate(250)
                }
            }
        }
        return true
    }
}
