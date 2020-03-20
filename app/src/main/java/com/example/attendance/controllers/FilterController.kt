package com.example.attendance.controllers

import androidx.fragment.app.Fragment
import com.example.attendance.R
import com.example.attendance.adapters.FilterAdapter
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.onTextChange
import kotlinx.android.synthetic.main.fragment_filter.*

object FilterController : FragmentController {
    private lateinit var context: Fragment
    private lateinit var constraints: String
    private lateinit var adapter: FilterAdapter

    override fun init(context: Fragment) {
        FilterController.context = context
        with(context) {
            adapter = FilterAdapter(filterEditText)
            suggestions.adapter = adapter
            adapter.updateFilters("")
            filterEditText.requestFocus()
            filterEditText.onTextChange {
                adapter.updateFilters(it)
                constraints = it
            }
            back.setOnClickListener {
                Navigation.navigate(R.id.mainContent)
            }
            filterDone.setOnClickListener {
                Navigation.navigate(R.id.mainContent)
                MainController.updateFilters(filterEditText.text.toString())
            }
        }
    }

    override fun restoreState() {
        if (!::constraints.isInitialized) return
        with(context) {
            filterEditText.setText(constraints)
            adapter.updateFilters(constraints)
        }
    }
}
