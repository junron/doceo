package com.example.attendance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.R
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter
import kotlinx.android.synthetic.main.filter_suggestion.view.*


class FilterAdapter(inflater: LayoutInflater?) :
    SuggestionsAdapter<Pair<String, String>, SuggestionHolder>(inflater) {
    private var entities = listOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionHolder {
        val view = layoutInflater.inflate(R.layout.filter_suggestion, null)
        return SuggestionHolder(view)
    }

    override fun getSingleViewHeight() = 45

    override fun onBindSuggestionHolder(
        suggestion: Pair<String, String>,
        holder: SuggestionHolder,
        position: Int
    ) {
        with(holder.itemView) {
            filterParam.text = suggestion.first
            filterValue.text = suggestion.second
            setOnClickListener {

            }
        }
    }

    override fun getFilter() = object : Filter() {
        override fun performFiltering(constraints: CharSequence): FilterResults {
            return FilterResults().apply {
                values = suggestions
            }
        }

        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            suggestions = results.values as? List<Pair<String, String>> ?: return
            notifyDataSetChanged()
        }

    }
}

class SuggestionHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
