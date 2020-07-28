package com.example.attendance.fragments.snapmit.assignments

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.adapters.snapmit.AssignmentListAdapter
import com.example.attendance.models.Students
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.SpacesItemDecoration
import com.example.attendance.util.auth.currentUserEmail
import com.example.attendance.viewmodels.AssignmentsViewModel
import kotlinx.android.synthetic.main.assignment_category.view.*
import kotlinx.android.synthetic.main.fragment_assignment_list.view.*

class AssignmentsListFragment : Fragment() {
    val assignmentsViewModel: AssignmentsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_assignment_list, container, false)
        val authorized =
            Students.createAssignmentAuthorized(currentUserEmail())
        with(inflater.inflate(R.layout.assignment_category, container, false)) {
            assignmentCategory.text = if (authorized) "Ongoing (0)" else "Assigned (0)"
            recycler.adapter = AssignmentListAdapter(
                this@AssignmentsListFragment,
                loading,
                no_items,
                0,
                authorized,
                assignmentCategory
            )
            applyLayoutManager(recycler)
            root.frameLayout.addView(this)
            expansionLayout.expand(true)
        }
        if (!authorized) {
            root.fab.visibility = View.GONE
            with(inflater.inflate(R.layout.assignment_category, container, false)) {
                assignmentCategory.text = "Completed (0)"
                recycler.adapter = AssignmentListAdapter(
                    this@AssignmentsListFragment,
                    loading,
                    no_items,
                    1,
                    false,
                    assignmentCategory
                )
                applyLayoutManager(recycler)
                root.frameLayout.addView(this)
            }
        }
        with(inflater.inflate(R.layout.assignment_category, container, false)) {
            assignmentCategory.text = if (authorized) "Past due (0)" else "Overdue (0)"
            recycler.adapter = AssignmentListAdapter(
                this@AssignmentsListFragment,
                loading,
                no_items,
                2,
                authorized,
                assignmentCategory
            )
            applyLayoutManager(recycler)
            root.frameLayout.addView(this)
        }
        MainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        root.toolbarMain.apply {
            setNavigationIcon(R.drawable.ic_baseline_menu_24)
            navigationIcon?.setTint(Color.WHITE)
            setNavigationOnClickListener {
                MainActivity.drawerLayout.openDrawer(Gravity.LEFT)
            }
        }

        root.fab
            .setOnClickListener {
                Navigation.navigate(R.id.newAssignmentFragment)
            }
        return root
    }

    private fun applyLayoutManager(recyclerView: RecyclerView) {
        val llm = LinearLayoutManager(context)
        llm.orientation = RecyclerView.VERTICAL
        recyclerView.layoutManager = llm
        recyclerView.addItemDecoration(SpacesItemDecoration(64))
    }

    companion object {
        fun newInstance(): AssignmentsListFragment {
            val fragment = AssignmentsListFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}

