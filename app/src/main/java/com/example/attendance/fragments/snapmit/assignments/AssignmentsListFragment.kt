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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.adapters.snapmit.AssignmentListAdapter
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.SpacesItemDecoration
import com.example.attendance.viewmodels.AssignmentsViewModel
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
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler)
        recyclerView.adapter =
            AssignmentListAdapter(
                this,
                root.loading,
                root.fab,
                root.no_items
            )
        MainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        root.toolbarMain.apply {
            setNavigationIcon(R.drawable.ic_baseline_menu_24)
            navigationIcon?.setTint(Color.WHITE)
            setNavigationOnClickListener {
                MainActivity.drawerLayout.openDrawer(Gravity.LEFT)
            }
        }
        val llm = LinearLayoutManager(container!!.context)
        llm.orientation = RecyclerView.VERTICAL
        recyclerView.layoutManager = llm
        recyclerView.addItemDecoration(SpacesItemDecoration(64))
        root.fab
            .setOnClickListener {
                Navigation.navigate(R.id.newAssignmentFragment)
            }

        var swipeContainer = root.findViewById(R.id.swipe) as SwipeRefreshLayout
        swipeContainer.setOnRefreshListener {
            assignmentsViewModel.appendOwnAssignments(true).continueWith {
                swipeContainer.isRefreshing = false
            }
        }

        return root
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

