package com.example.attendance.util.android

import android.view.Gravity
import androidx.core.view.iterator
import androidx.navigation.NavController
import com.example.attendance.MainActivity
import com.google.android.material.navigation.NavigationView

object Navigation {
    private lateinit var navMap: Map<Int, Int>
    private lateinit var navController: NavController

    fun init(
        navMap: Map<Int, Int>,
        navController: NavController,
        navigationView: NavigationView
    ) {
        Navigation.navMap = navMap
        Navigation.navController = navController
        navigationView.menu.iterator().forEach { menuItem ->
            menuItem.setOnMenuItemClickListener {
                val destination = navMap[it.itemId] ?: return@setOnMenuItemClickListener false
                navController.navigate(destination)
                MainActivity.drawerLayout.closeDrawer(Gravity.LEFT)
                true
            }
        }
    }

    fun navigate(id: Int) {
        navController.navigate(id)
    }
}
