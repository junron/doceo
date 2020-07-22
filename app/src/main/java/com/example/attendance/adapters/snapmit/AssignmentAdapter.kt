package com.example.attendance.adapters.snapmit

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.FragmentNavigator
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.R
import com.example.attendance.models.snapmit.Assignment
import com.example.attendance.viewmodels.AssignmentsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.assignment_card.view.*
import kotlinx.android.synthetic.main.fragment_assignments2.view.name
import java.util.*

class AssignmentAdapter(
    private val viewModel: AssignmentsViewModel,
    hideOnInflate: View,
    showOnInflate: View,
    noItems: View
) : RecyclerView.Adapter<AssignmentAdapter.Card>() {
    var assignments = mutableListOf<Assignment>()

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
        val assignment = assignments[position]
        with(holder.view) {
            name.text = assignment.name
            numSubmissions.text = assignment.submissions.size.toString()
        }
        holder.view.setOnClickListener { v: View ->
            viewModel.currentAssignmentId = assignment.id
            val map: Map<View, String> =
                HashMap()
            val extras =
                FragmentNavigator.Extras.Builder()
                    .addSharedElement(v.findViewById(R.id.name), "name")
                    .addSharedElement(v.findViewById(R.id.code), "code")
                    .build()
            // TODO: handle navigation
            // NavHostFragment.findNavController(
            //     AssignmentsFragment.assignmentsFragment!!
            // )
            //     .navigate(R.id.action_nav_tassignments_to_assignments2Fragment, null, null, extras)
        }
    }

    override fun getItemCount(): Int {
        return assignments.size
    }

    inner class Card(var view: ViewGroup) : RecyclerView.ViewHolder(view)

    init {
        showOnInflate.alpha = 0f
        val db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser!!
        db.collection("assignments")
            .whereEqualTo("owner", user.email)
            .get()
            .addOnSuccessListener {
                hideOnInflate.animate().alpha(0f)
                showOnInflate.animate().alpha(1f)
                assignments = it.toObjects(Assignment::class.java)
                Log.d("ASSIGNMENTS", assignments.toString())
                viewModel.assignments.value = assignments
                notifyDataSetChanged()
            }
    }
}
