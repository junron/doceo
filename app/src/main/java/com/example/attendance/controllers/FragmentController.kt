package com.example.attendance.controllers

import androidx.fragment.app.Fragment

interface FragmentController {
    fun init(context: Fragment)
    fun restoreState()
}
