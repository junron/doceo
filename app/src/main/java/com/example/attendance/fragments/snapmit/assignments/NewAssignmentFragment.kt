package com.example.attendance.fragments.snapmit.assignments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.attendance.R
import com.example.attendance.controllers.FilterController
import com.example.attendance.models.Students
import com.example.attendance.models.snapmit.Assignment
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.onTextChange
import com.example.attendance.util.uuid
import com.example.attendance.viewmodels.AssignmentsViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.joestelmach.natty.Parser
import kotlinx.android.synthetic.main.fragment_new_assignment.*
import kotlinx.android.synthetic.main.fragment_new_assignment.view.*
import java.text.SimpleDateFormat
import java.util.*

class NewAssignmentFragment : Fragment() {
    private val viewModel: AssignmentsViewModel by activityViewModels()

    companion object {
        private var name: String? = null
        private var description: String = "No description provided"
        private var constraints: String? = null
        private var dueDate: Date? = null
        private var dueDateRaw: String = ""
        fun newInstance(): NewAssignmentFragment {
            val fragment = NewAssignmentFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_new_assignment, container, false)
        root.toolbarMain.apply {
            setNavigationIcon(R.drawable.ic_baseline_close_24)
            navigationIcon?.setTint(Color.WHITE)
            setNavigationOnClickListener {
                Navigation.navigate(R.id.assignmentsListFragment)
            }
        }
        with(root) {
            selectStudents.setOnClickListener {
                name = assignmentName.editText?.text.toString()
                constraints?.let {
                    FilterController.constraints = it
                }
                FilterController.callback = {
                    // Navigate back
                    Navigation.navigate(R.id.newAssignmentFragment)
                    constraints =
                        it ?: constraints
                }
                Navigation.navigate(R.id.filterFragment)
            }
            assignmentName.editText?.onTextChange {
                checkValidState()
            }
            assignmentDueDate.editText?.onTextChange {
                dueDateRaw = it
                refreshDateState()
            }
            assignmentDescription.editText?.onTextChange {
                description = it
            }
            assignmentDone.setOnClickListener {
                val assignment = Assignment(
                    uuid(),
                    false,
                    name!!,
                    emptyList(),
                    Students
                        .filterStudents(
                            constraints!!
                                .split(" "), Students.students
                        ).map {
                            it.id
                        },
                    FirebaseAuth.getInstance().currentUser?.email!!,
                    Timestamp(dueDate!!),
                    description
                )
                println("Created assignment: $assignment")
                viewModel.createAssignment(assignment)
                Navigation.navigate(R.id.assignmentsListFragment)
                constraints = null
                name = null
                description = "No description provided"
            }
        }
        return root
    }

    private fun checkValidState() {
        if (assignmentName.editText?.text.toString().isNotBlank())
            if (dueDate != null)
                if (constraints != null) {
                    assignmentDone.visibility = View.VISIBLE
                    return
                }
        assignmentDone.visibility = View.GONE
    }

    private fun refreshDateState() {
        val date = Parser().parse(assignmentDueDate.editText?.text.toString())
            .firstOrNull()
            ?.dates
            ?.firstOrNull()
            ?: run {
                assignmentDueDate.error = "Invalid date"
                dueDate = null
                return
            }
        if (date.after(Date())) {
            val sdf = SimpleDateFormat("d MMMM yyyy  HH:mm")
            val dateString = sdf.format(date)
            dueDate = date
            assignmentDueDate.helperText = dateString
            if (date.time - Date().time > 63_113_904_000) {
                assignmentDueDate.error = "Date is more than 2 years in the future.\nAre you sure?"
            }
        } else {
            assignmentDueDate.error = "Dates must be in the future"
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        assignmentName.editText?.setText(name)
        assignmentDescription.editText?.setText(
            if (description == "No description provided") "" else description
        )
        if (dueDateRaw.isNotEmpty()) {
            assignmentDueDate.editText?.setText(dueDateRaw)
            refreshDateState()
        }
        val c = constraints
        if (c != null) {
            val students = Students.filterStudents(c.split(" "), Students.students)
            selectStudents.text = "${students.size} students selected"
        } else {
            selectStudents.text = "Select students"
        }
    }

}
