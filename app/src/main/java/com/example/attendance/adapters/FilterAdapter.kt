package com.example.attendance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.R
import com.example.attendance.models.FilterParam
import com.mancj.materialsearchbar.MaterialSearchBar
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter
import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import kotlinx.android.synthetic.main.filter_suggestion.view.*


class FilterAdapter(
    private val searchBar: MaterialSearchBar,
    inflater: LayoutInflater
) :
    SuggestionsAdapter<FilterParam, SuggestionHolder>(inflater) {

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
                val entities = searchBar.text.split(" ").toMutableList()
                val last = entities.last()
                if (suggestion.value.isEmpty()) {
                    entities[entities.lastIndex] = last.substringBefore(":") + ":${suggestion.key} "
                } else {
                    entities[entities.lastIndex] = suggestion.key.trim()
                }
                searchBar.text = entities.joinToString(" ")
                searchBar.searchEditText.setSelection(searchBar.text.length)
            }
        }
    }

    override fun getFilter() = object : Filter() {
        override fun performFiltering(constraints: CharSequence) = FilterResults()
            .apply {
                val lastEntity = constraints.split(" ").last()
                if (lastEntity.contains(":")) {
                    val (key, value) = lastEntity.split(":")
                    println("$key $value")
                    val filterParam = FilterParam.filterParams.firstOrNull {
                        it.key == "$key: "
                    } ?: return@apply run {
                        values = FilterParam.filterParams
                    }
                    val possibleValues = filterParam.possibleValues
                    val filteredValues = if (value.isBlank()) possibleValues
                    else FuzzySearch.extractSorted(value, possibleValues, 3).map { it.string!! }
                    values = filteredValues.map {
                        FilterParam(it, "", listOf(), filterParam.icon)
                    }
                } else {
                    values = if (lastEntity.isBlank()) FilterParam.filterParams
                    else FuzzySearch.extractSorted(
                        lastEntity,
                        FilterParam.filterParams.map { it.key },
                        3
                    ).map {
                        FilterParam.filterParams.find { param -> param.key == it.string }
                    }
                }
            }

        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            suggestions = results.values as? List<FilterParam> ?: return
            notifyDataSetChanged()
        }

    }
}

class SuggestionHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
