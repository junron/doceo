package com.example.attendance.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.attendance.fragments.ClasslistFragment
import com.example.attendance.models.Attendance

class ClasslistPagerAdapter(val attendance: Attendance, parent: Fragment) :
    FragmentStateAdapter(parent) {
    override fun getItemCount() = attendance.classlists.size

    override fun createFragment(position: Int): Fragment {
        val item = attendance.classlists[position]
        return ClasslistFragment(attendance, item)
    }
}
