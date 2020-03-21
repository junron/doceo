package com.example.attendance.controllers

import androidx.fragment.app.Fragment

abstract class FragmentController {
    lateinit var context: Fragment
    open fun init(context: Fragment) {
        this.context = context
    }

    open fun restoreState() {}
}
