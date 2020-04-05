package com.example.attendance.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.attendance.R
import com.example.attendance.adapters.ClasslistAdapter
import com.example.attendance.models.Attendance
import com.example.attendance.models.ClasslistInstance
import com.example.attendance.util.isToday
import com.example.attendance.util.isYesterday
import kotlinx.android.synthetic.main.fragment_classlist.*
import kotlinx.android.synthetic.main.fragment_main_content.*
import java.text.SimpleDateFormat
import java.util.*

class ClasslistFragment(val attendance: Attendance, private var classlist: ClasslistInstance) :
    Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_classlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ClasslistAdapter(classlist)
        attendance.addListener {
            this.classlist =
                attendance.classlists.find { it.id == classlist.id } ?: return@addListener
            adapter.dataChanged(classlist)
        }
        classListView.adapter = adapter
        classListView.layoutManager = GridLayoutManager(context, 2)
        parentFragment?.toolbarClasslistToolbar?.subtitle = formatDate(classlist.created.toDate())
    }


    private fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("dd MMM")
        val sdf2 = SimpleDateFormat("hh:mm a")
        val day: String = when {
            date.isToday() -> "Today"
            date.isYesterday() -> "Yesterday"
            else -> sdf.format(date)
        }
        return "$day at ${sdf2.format(date)}"
    }
}
