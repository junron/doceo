package com.example.attendance.adapters.snapmit

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.R
import com.example.attendance.models.snapmit.Submission
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.android.synthetic.main.submission_card.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean


@UnstableDefault
class UserSubmissionAdapter(
    hideOnLoad: View,
    noItems: View
) :
    RecyclerView.Adapter<UserSubmissionAdapter.Card>() {
    var submissions: List<Submission> = listOf()
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

        holder.view.setOnClickListener {
            // historyViewModel.submission.setValue(
            //     submission
            // )
            Log.d("HISTORY", submission.toString())
            // Log.d(
            //     "HISTORY",
            //     java.lang.String.valueOf(com.example.snapmit.ui.history.HistoryFragment.historyViewModel.submission.getValue())
            // )
            // Navigation.navigate(R.id.history2fragment)
        }
        with(holder.view) {
            name.text = submission.name
            email.text = submission.owner
            numPages.text = submission.images.size.toString()
        }
    }

    override fun getItemCount(): Int {
        return submissions.size
    }

    inner class Card(var view: View) : RecyclerView.ViewHolder(view)

    @Serializable
    private inner class ApiResponse(
        val success: Boolean,
        val submissions: List<Submission>
    )

    init {
        FirebaseFunctions.getInstance().getHttpsCallable("getSubmissionsByUser")
            .call()
            .continueWith {
                hideOnLoad.animate().alpha(0f)
                // TODO: Use viewmodel scope
                GlobalScope.launch(Dispatchers.IO) {
                    val data = it.await().data.toString()
                    if (Json.parseJson(data).jsonObject["success"]?.boolean != true) return@launch
                    val response = Json.parse(ApiResponse.serializer(), data)
                    submissions = response.submissions
                    if (submissions.isEmpty()) noItems.animate().alpha(1f)
                }
            }
    }
}
