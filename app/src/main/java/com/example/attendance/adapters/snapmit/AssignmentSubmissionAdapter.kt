package com.example.attendance.adapters.snapmit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.R
import com.example.attendance.fragments.snapmit.assignments.AssignmentFragment
import com.example.attendance.models.snapmit.Submission
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.toDetailedString
import kotlinx.android.synthetic.main.submission_card.view.*


class AssignmentSubmissionAdapter(
    private val assignmentFragment: AssignmentFragment,
    hideOnLoad: View,
    noItems: View
) :
    RecyclerView.Adapter<AssignmentSubmissionAdapter.Card>() {
    private val assignmentsViewModel = assignmentFragment.assignmentsViewModel
    private var relevantSubmissions = listOf<Submission>()
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Card {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.submission_card, parent, false)
        return Card(view)
    }

    override fun onBindViewHolder(
        holder: Card,
        position: Int
    ) {
        val submission = relevantSubmissions[position]
        val assignment =
            assignmentsViewModel.assignments.value.find { it.id == submission.assignmentId }!!
        with(holder.view) {
            name.text = submission.name
            submittedAt.text = submission.submissionTime.toDate().toDetailedString()
            numPages.text = submission.images.size.toString()
            overdueChip.visibility = if (assignment.dueDate < submission.submissionTime)
                View.VISIBLE else View.GONE
        }
        holder.view.setOnClickListener {
            assignmentsViewModel.currentSubmissionId = submission.id
            Navigation.navigate(R.id.submissionViewFragment)
        }
    }

    override fun getItemCount(): Int {
        return relevantSubmissions.size
    }

    inner class Card(var view: View) : RecyclerView.ViewHolder(view)

    init {
        hideOnLoad.animate().alpha(0F)
        assignmentsViewModel.submissions.observe({ assignmentFragment.lifecycle }) {
            if (it.isEmpty()) noItems.animate().alpha(1F)
            else noItems.animate().alpha(0F)
            relevantSubmissions = assignmentsViewModel.submissions.value
                .filter { it.assignmentId == assignmentsViewModel.currentAssignmentId }
            assignmentFragment.updateCanSubmit(relevantSubmissions.isEmpty())
            notifyDataSetChanged()
        }
    }

}
