package com.example.attendance.adapters.snapmit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.R
import com.example.attendance.fragments.snapmit.assignments.AssignmentFragment
import com.example.attendance.util.android.Navigation
import kotlinx.android.synthetic.main.submission_card.view.*
import kotlinx.serialization.UnstableDefault


@UnstableDefault
class AssignmentSubmissionAdapter(
    private val assignmentFragment: AssignmentFragment,
    hideOnLoad: View,
    noItems: View
) :
    RecyclerView.Adapter<AssignmentSubmissionAdapter.Card>() {
    private val assignmentsViewModel = assignmentFragment.assignmentsViewModel
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
        val submission = assignmentsViewModel.submissions.value[position]
        with(holder.view) {
            name.text = submission.name
            email.text = submission.owner
            numPages.text = submission.images.size.toString()
        }
        holder.view.setOnClickListener {
            assignmentsViewModel.currentSubmissionId = submission.id
            println(submission.id)
            Navigation.navigate(R.id.submissionViewFragment)
        }
    }

    override fun getItemCount(): Int {
        return assignmentsViewModel.submissions.value.size
    }

    inner class Card(var view: View) : RecyclerView.ViewHolder(view)

    init {
        hideOnLoad.animate().alpha(0F)
        assignmentsViewModel.submissions.observe({ assignmentFragment.lifecycle }) {
            if (it.isEmpty()) noItems.animate().alpha(1F)
            else noItems.animate().alpha(0F)
            notifyDataSetChanged()
        }
    }

}
