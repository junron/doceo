package com.example.attendance.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.attendance.fragments.ClasslistFragment
import com.example.attendance.models.ClasslistGroup

class ClasslistPagerAdapter(
    val classlistGroup: ClasslistGroup,
    parent: Fragment,
    private val fullName: Boolean
) :
    FragmentStateAdapter(parent) {
    val state = mutableMapOf<Int, Fragment>()
    override fun getItemCount() = classlistGroup.classlists.size

    override fun createFragment(position: Int): Fragment {
        val item = classlistGroup.classlists[position]
        val fragment = ClasslistFragment(classlistGroup, item, fullName)
        state[position] = fragment
        return fragment
    }
}
