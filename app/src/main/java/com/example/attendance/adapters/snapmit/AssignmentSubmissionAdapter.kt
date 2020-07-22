package com.example.attendance.adapters.snapmit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.R
import com.example.attendance.models.snapmit.Submission
import com.example.attendance.viewmodels.AssignmentsViewModel
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.android.synthetic.main.submission_card.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean


@UnstableDefault
class AssignmentSubmissionAdapter(
    private val assignmentsViewModel: AssignmentsViewModel,
    hideOnLoad: View,
    noItems: View
) :
    RecyclerView.Adapter<AssignmentSubmissionAdapter.Card>() {
    var submissions = listOf<Submission>()
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
        val submission = submissions[position]
        with(holder.view) {
            name.text = submission.name
            email.text = submission.owner
            numPages.text = submission.images.size.toString()
        }
        holder.view.setOnClickListener {
            assignmentsViewModel.currentSubmissionId = submission.id
            // TODO: Handle navigation
            // NavHostFragment.findNavController(Assignments2Fragment.assignments2Fragment)
            //     .navigate(R.id.action_nav_tassignments2_to_assignments3Fragment)
        }
    }

    override fun getItemCount(): Int {
        return submissions.size
    }

    inner class Card(var view: View) : RecyclerView.ViewHolder(view)

    init {
        val query = mapOf("id" to assignmentsViewModel.currentAssignmentId)
        FirebaseFunctions.getInstance().getHttpsCallable("getSubmissionsByAssignment")
            .call(query)
            .continueWith {
                hideOnLoad.animate().alpha(0f)
                assignmentsViewModel.viewModelScope.launch(Dispatchers.IO) {
                    val data = it.await().data.toString()
                    if (Json.parseJson(data).jsonObject["success"]?.boolean != true) return@launch
                    val response = Json.parse(UserSubmissionAdapter.ApiResponse.serializer(), data)
                    submissions = response.submissions
                    if (submissions.isEmpty()) noItems.animate().alpha(1f)
                    assignmentsViewModel.submissions.value = submissions
                    notifyDataSetChanged()
                }
            }
    }
}
