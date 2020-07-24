package com.example.attendance.adapters.snapmit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.R
import com.example.attendance.fragments.snapmit.assignments.AssignmentsListFragment
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.toShortString
import kotlinx.android.synthetic.main.assignment_card.view.*

class AssignmentListAdapter(
    private val fragment: AssignmentsListFragment,
    hideOnInflate: View,
    showOnInflate: View,
    noItems: View
) : RecyclerView.Adapter<AssignmentListAdapter.Card>() {
    private val viewModel = fragment.assignmentsViewModel

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
        val assignment = viewModel.assignments.value[position]
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

    fun clear() {
        viewModel.assignments.postValue(ArrayList(viewModel.assignments.value.size))
    }

    override fun getItemCount(): Int {
        return viewModel.assignments.value.size
    }

    inner class Card(var view: ViewGroup) : RecyclerView.ViewHolder(view)

    init {
        hideOnInflate.animate().alpha(0f)
        showOnInflate.animate().alpha(1f)
        viewModel.assignments.observe({ fragment.lifecycle }) {
            if (it.isEmpty()) noItems.animate().alpha(1F)
            else noItems.animate().alpha(0F)
            notifyDataSetChanged()
        }
    }
}
