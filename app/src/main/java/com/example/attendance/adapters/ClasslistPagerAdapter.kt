package com.example.attendance.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.attendance.fragments.ClasslistFragment
import com.example.attendance.models.Attendance

class ClasslistPagerAdapter(
    val attendance: Attendance,
    parent: Fragment,
    private val fullName: Boolean
) :
    FragmentStateAdapter(parent) {
    val state = mutableMapOf<Int, Fragment>()
    override fun getItemCount() = attendance.classlists.size

    override fun createFragment(position: Int): Fragment {
        val item = attendance.classlists[position]
        val fragment = ClasslistFragment(attendance, item, fullName)
        state[position] = fragment
        return fragment
    }
}
