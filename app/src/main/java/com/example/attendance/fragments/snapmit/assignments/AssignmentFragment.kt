package com.example.attendance.fragments.snapmit.assignments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.R
import com.example.attendance.adapters.snapmit.AssignmentSubmissionAdapter
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.SpacesItemDecoration
import com.example.attendance.util.toDetailedString
import com.example.attendance.viewmodels.AssignmentsViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_assignment.*
import kotlinx.android.synthetic.main.fragment_assignment.view.*

class AssignmentFragment : Fragment() {
    companion object {
        lateinit var assignmentUUID : String;
        fun newInstance(): AssignmentFragment {
            val fragment = AssignmentFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
    val assignmentsViewModel: AssignmentsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_assignment, container, false)
        root.toolbarMain.apply {
            setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
            navigationIcon?.setTint(Color.WHITE)
            setNavigationOnClickListener {
                assignmentsViewModel.currentAssignmentId = null
                Navigation.navigate(R.id.assignmentsListFragment)
            }
        }
        // NPE if assignment doesn't exist
        val assignment = assignmentsViewModel.getAssignment()!!
        assignmentUUID = assignment.id;
        root.toolbarMain.title = assignment.name
        root.toolbarMain.subtitle = "Due " + assignment.dueDate.toDate().toDetailedString()
        root.description.text = assignment.description
        val recycler = root.recycler
        val llm = LinearLayoutManager(root.context)
        llm.orientation = RecyclerView.VERTICAL
        recycler.layoutManager = llm
        recycler.adapter = AssignmentSubmissionAdapter(
            this,
            root.loading,
            root.no_items
        )
        recycler.addItemDecoration(SpacesItemDecoration(64))
        if (assignment.owner == FirebaseAuth.getInstance().currentUser?.email) {
            root.submit_button.visibility = View.GONE
        } else {
            root.delete_button.visibility = View.GONE
        }
        root.submit_button.setOnClickListener {
            Navigation.navigate(R.id.submitFragment)
        }
        root.delete_button
            .setOnClickListener {
                assignmentsViewModel.deleteAssignment()
                assignmentsViewModel.currentAssignmentId = null
                Snackbar.make(
                    container!!,
                    "Assignment removed",
                    Snackbar.LENGTH_LONG
                ).setAction("Undo") {
                    assignmentsViewModel.restoreAssignment(assignment.id)
                    Snackbar.make(
                        container,
                        "Assignment restored",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }.show()
                Navigation.navigate(R.id.assignmentsListFragment)
            }
        return root
    }

    fun updateCanSubmit(visible: Boolean) {
        if (assignmentsViewModel.getAssignment()?.owner ==
            FirebaseAuth.getInstance().currentUser?.email
        ) return
        submit_button.visibility = if (visible) View.VISIBLE else View.GONE
    }

}
