package com.example.attendance.util.android

import androidx.navigation.NavController
import com.google.android.material.bottomnavigation.BottomNavigationView

object Navigation {
    private lateinit var navMap: Map<Int, Int>
    private lateinit var navController: NavController

    fun init(
        navMap: Map<Int, Int>,
        navController: NavController,
        bottomNavView: BottomNavigationView
    ) {
        Navigation.navMap = navMap
        Navigation.navController = navController
        bottomNavView.setOnNavigationItemSelectedListener {
            val destination = navMap[it.itemId] ?: return@setOnNavigationItemSelectedListener false
            navController.navigate(destination)
            true
        }
    }

    fun navigate(id: Int) {
        navController.navigate(id)
    }
}
