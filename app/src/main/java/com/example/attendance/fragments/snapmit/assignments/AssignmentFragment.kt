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

        updateDisplay(root)
        assignmentsViewModel.assignments.observe({ lifecycle }) {
            if (assignmentsViewModel.currentAssignmentId in it.map { assignment -> assignment.id }) {
                updateDisplay()
            } else {
                Snackbar.make(
                    container!!,
                    "You no longer have access to this assignment",
                    Snackbar.LENGTH_LONG
                ).show()
                assignmentsViewModel.currentAssignmentId = null
                Navigation.navigate(R.id.assignmentsListFragment)
            }
        }

        root.submit_button.setOnClickListener {
            Navigation.navigate(R.id.submitFragment)
        }
        root.delete_button
            .setOnClickListener {
                val assignmentId = assignmentsViewModel.currentAssignmentId!!
                assignmentsViewModel.deleteAssignment()
                assignmentsViewModel.currentAssignmentId = null
                Snackbar.make(
                    container!!,
                    "Assignment removed",
                    Snackbar.LENGTH_LONG
                ).setAction("Undo") {
                    assignmentsViewModel.restoreAssignment(assignmentId)
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

    private fun updateDisplay(root: View? = view) {
        // NPE if assignment doesn't exist
        root ?: return
        val assignment = assignmentsViewModel.getAssignment()!!
        with(root) {
            toolbarMain.title = assignment.name
            toolbarMain.subtitle = "Due " + assignment.dueDate.toDate().toDetailedString()
            description.text = assignment.description
            val llm = LinearLayoutManager(context)
            llm.orientation = RecyclerView.VERTICAL
            recycler.layoutManager = llm
            recycler.adapter = AssignmentSubmissionAdapter(
                this@AssignmentFragment,
                loading,
                no_items
            )
            recycler.addItemDecoration(SpacesItemDecoration(64))
            if (assignment.owner == FirebaseAuth.getInstance().currentUser?.email) {
                submit_button.visibility = View.GONE
            } else {
                delete_button.visibility = View.GONE
            }
        }
    }

    fun updateCanSubmit(visible: Boolean) {
        if (assignmentsViewModel.getAssignment()?.owner ==
            FirebaseAuth.getInstance().currentUser?.email
        ) return
        submit_button.visibility = if (visible) View.VISIBLE else View.GONE
    }

    companion object {
        fun newInstance(): AssignmentFragment {
            val fragment = AssignmentFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}
