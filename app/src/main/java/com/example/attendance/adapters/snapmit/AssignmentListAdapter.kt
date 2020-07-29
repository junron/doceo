package com.example.attendance.adapters.snapmit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.R
import com.example.attendance.fragments.snapmit.assignments.AssignmentsListFragment
import com.example.attendance.models.snapmit.Assignment
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.auth.currentUserEmail
import com.example.attendance.util.toShortString
import kotlinx.android.synthetic.main.assignment_card.view.*
import java.text.SimpleDateFormat
import java.util.*

class AssignmentListAdapter(
    private val fragment: AssignmentsListFragment,
    hideOnInflate: View,
    noItems: View,
    // 0 - current
    // 1 - completed
    // 2 - past due
    private val restrictions: Int,
    private val ownerOnly: Boolean,
    private val titleElement: TextView
) : RecyclerView.Adapter<AssignmentListAdapter.Card>() {
    private val viewModel = fragment.assignmentsViewModel
    private var relevantAssignments = listOf<Assignment>()
    private val user = currentUserEmail()


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Card {
        val viewGroup = LayoutInflater.from(parent.context)
            .inflate(R.layout.assignment_card, parent, false) as ViewGroup
        return Card(viewGroup)
    }

    override fun onBindViewHolder(
        holder: Card,
        position: Int
    ) {
        val assignment = relevantAssignments[position]
        with(holder.view) {
            name.text = assignment.name
            if (user == assignment.owner) {
                numSubmissions.text = assignment.submissions.size.toString()
                status.text = "Submissions"
            } else {
                // TODO: watch submissions too
                val submissions = getSubmissions(assignment.id)
                val submitted = submissions.isNotEmpty()
                if (submitted) {
                    val sdf = SimpleDateFormat("d MMMM yyyy  HH:mm")
                    status.text = "Submitted at"
                    numSubmissions.text = sdf.format(submissions.first().submissionTime.toDate())
                } else {
                    status.text = "Status"
                    numSubmissions.text = "Not submitted"
                }
                if (assignment.dueDate.toDate().after(Date()) ||
                    (submitted && submissions.first().submissionTime.toDate()
                        .before(assignment.dueDate.toDate()))
                ) {
                    overdueChip.visibility = View.GONE
                }
            }
            dueDate.text = assignment.dueDate.toDate().toShortString()
        }
        holder.view.setOnClickListener {
            viewModel.currentAssignmentId = assignment.id
            Navigation.navigate(R.id.assignmentFragment)
        }
    }

    override fun getItemCount(): Int {
        return relevantAssignments.size
    }

    private fun getSubmissions(id: String) =
        viewModel.submissions.value.filter { it.assignmentId == id && user == it.owner }


    inner class Card(var view: ViewGroup) : RecyclerView.ViewHolder(view)

    init {
        hideOnInflate.animate().alpha(0f)
        hideOnInflate.visibility = View.GONE
        viewModel.assignments.observe({ fragment.lifecycle }) {
            relevantAssignments = it.filter { assignment ->
                if (ownerOnly && assignment.owner != user) return@filter false
                return@filter when (this.restrictions) {
                    0 -> assignment.dueDate.toDate().after(Date())
                    1 -> getSubmissions(assignment.id).isNotEmpty()
                    2 -> assignment.dueDate.toDate()
                        .before(Date()) && getSubmissions(assignment.id).isEmpty()
                    else -> false
                }
            }
            titleElement.text =
                titleElement.text.replace(Regex("\\([0-9]+\\)"), "(${relevantAssignments.size})")
            if (relevantAssignments.isEmpty()) {
                noItems.visibility = View.VISIBLE
                noItems.animate().alpha(1F)
            } else {
                noItems.animate().alpha(0F)
                noItems.visibility = View.GONE
            }
            notifyDataSetChanged()
        }
    }
}
