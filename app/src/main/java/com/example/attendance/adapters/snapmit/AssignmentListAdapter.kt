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
            numSubmissions.text = assignment.submissions.size.toString()
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

    inner class Card(var view: ViewGroup) : RecyclerView.ViewHolder(view)

    init {
        hideOnInflate.animate().alpha(0f)
        hideOnInflate.visibility = View.GONE
        val user = currentUserEmail()
        viewModel.assignments.observe({ fragment.lifecycle }) {
            relevantAssignments = it.filter { assignment ->
                if (ownerOnly && assignment.owner != user) return@filter false
                return@filter when (this.restrictions) {
                    0 -> assignment.dueDate.toDate().after(Date())
                    1 -> viewModel.submissions.value.any { it.assignmentId == assignment.id && user == it.owner }
                    2 -> assignment.dueDate.toDate().before(Date()) &&
                            viewModel.submissions.value.none { it.assignmentId == assignment.id && user == it.owner }
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
