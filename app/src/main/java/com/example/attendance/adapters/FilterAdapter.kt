package com.example.attendance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import com.example.attendance.R
import com.example.attendance.models.FilterParam
import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import kotlinx.android.synthetic.main.filter_suggestion.view.*


class FilterAdapter(
    private val searchBar: EditText
) : BaseAdapter() {
    private var suggestions = listOf<FilterParam>()

    fun updateFilters(constraints: String) {
        val lastEntity = constraints.split(" ").last()
        if (lastEntity.contains(":")) {
            val (key, value) = lastEntity.split(":")
            println("$key $value")
            val filterParam = FilterParam.filterParams.firstOrNull {
                it.key == "$key: "
            } ?: return run {
                suggestions = FilterParam.filterParams
            }
            val possibleValues = filterParam.possibleValues
            val filteredValues = if (value.isBlank()) possibleValues
            else FuzzySearch.extractSorted(value, possibleValues, 3).map { it.string!! }
            suggestions = filteredValues.map {
                FilterParam(it, "", listOf(), filterParam.icon)
            }
        } else {
            suggestions = if (lastEntity.isBlank()) FilterParam.filterParams
            else FuzzySearch.extractSorted(
                lastEntity,
                FilterParam.filterParams.map { it.key },
                3
            ).map {
                FilterParam.filterParams.find { param -> param.key == it.string }!!
            }
        }
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val suggestion = getItem(position)
        val view = LayoutInflater.from(parent.context).inflate(R.layout.filter_suggestion, null)
        with(view) {
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
                searchBar.setText(entities.joinToString(" "))
                searchBar.setSelection(searchBar.text.length)
            }
        }
        return view
    }

    override fun getItem(position: Int) = suggestions[position]

    override fun getItemId(position: Int) = getItem(position).hashCode().toLong()

    override fun getCount() = suggestions.size
}

