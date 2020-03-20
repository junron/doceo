package com.example.attendance.controllers

import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.fragment.app.Fragment
import com.example.attendance.R
import com.example.attendance.adapters.FilterAdapter
import com.example.attendance.adapters.createAdapter
import com.example.attendance.models.FilterParam
import com.example.attendance.models.students
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.onTextChange
import com.example.attendance.util.auth.UserLoader
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.mancj.materialsearchbar.MaterialSearchBar
import kotlinx.android.synthetic.main.fragment_main_content.*
import kotlinx.serialization.UnstableDefault

@UnstableDefault
object MainController : FragmentController {
    private lateinit var context: Fragment

    override fun init(context: Fragment) {
        MainController.context = context
        with(context) {
            val classListAdapter = createAdapter(students)
            classListView.adapter = classListAdapter
            FirebaseAuth.getInstance()
                .addAuthStateListener {
                    if (it.currentUser != null) {
                        val user = UserLoader.getUser()
                        Snackbar.make(parentView, "Welcome, ${user.name}!", Snackbar.LENGTH_LONG)
                            .show()
                    }
                }
            if (UserLoader.userExists()) {
                UserLoader.loadFirebaseUser {
                    println("Error: $it")
                }
            } else {
                Snackbar.make(parentView, "You are not signed in", Snackbar.LENGTH_INDEFINITE)
                    .apply {
                        setAction("Sign In") {
                            Navigation.navigate(R.id.signInFragment)
                        }
                        show()
                    }
            }
            filter.setOnClickListener {
                toolbarMain.visibility = GONE
                searchBar.visibility = VISIBLE
                searchBar.openSearch()
            }
            val adapter = FilterAdapter(searchBar, layoutInflater)
            searchBar.setCustomSuggestionAdapter(adapter)
            adapter.suggestions = FilterParam.filterParams
            searchBar.searchEditText.onTextChange {
                adapter.filter.filter(it)
                classListAdapter.filterStudents(it.split(" "))
            }
            searchBar.setOnSearchActionListener(object : MaterialSearchBar.OnSearchActionListener {
                override fun onButtonClicked(buttonCode: Int) {
                }

                override fun onSearchStateChanged(enabled: Boolean) {
                    if (enabled) {
                        toolbarMain.visibility = GONE
                        searchBar.visibility = VISIBLE
                    } else {
                        toolbarMain.visibility = VISIBLE
                        searchBar.visibility = GONE
                    }
                }

                override fun onSearchConfirmed(text: CharSequence?) {
                }

            })
        }
    }

    override fun restoreState() {
    }
}
