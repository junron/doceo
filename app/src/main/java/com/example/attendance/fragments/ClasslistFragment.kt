package com.example.attendance.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.attendance.R
import com.example.attendance.adapters.ClasslistAdapter
import com.example.attendance.controllers.ClasslistController
import com.example.attendance.models.Attendance
import com.example.attendance.models.ClasslistInstance
import kotlinx.android.synthetic.main.fragment_classlist.*

class ClasslistFragment(
    val attendance: Attendance? = null,
    private var classlist: ClasslistInstance? = null,
    private var fullName: Boolean = false
) :
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
        val classlist = classlist ?: return
        val attendance = attendance ?: return
        val adapter = ClasslistAdapter(classlist, fullName)
        attendance.addListener {
            this.classlist =
                attendance.classlists.find { it.id == classlist.id } ?: return@addListener
            println("Updated: $classlist")
            adapter.dataChanged(classlist)
        }
        ClasslistController.addRenderListener {
            this.fullName = it
            adapter.fullName = it
            adapter.notifyDataSetChanged()
        }
        classListView.adapter = adapter
        classListView.layoutManager = GridLayoutManager(context, 2)
    }
}
