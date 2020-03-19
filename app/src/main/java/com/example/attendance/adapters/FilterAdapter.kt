package com.example.attendance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.R
import com.example.attendance.models.FilterParam
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter
import kotlinx.android.synthetic.main.filter_suggestion.view.*


class FilterAdapter(inflater: LayoutInflater?) :
    SuggestionsAdapter<FilterParam, SuggestionHolder>(inflater) {
    private var entities = listOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionHolder {
        val view = layoutInflater.inflate(R.layout.filter_suggestion, null)
        return SuggestionHolder(view)
    }

    override fun getSingleViewHeight() = 45

    override fun onBindSuggestionHolder(
        suggestion: FilterParam,
        holder: SuggestionHolder,
        position: Int
    ) {
        with(holder.itemView) {
            filterParam.text = suggestion.key
            filterValue.text = suggestion.value
            icon.setImageResource(suggestion.icon)
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
            suggestions = results.values as? List<FilterParam> ?: return
            notifyDataSetChanged()
        }

    }
}

class SuggestionHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
