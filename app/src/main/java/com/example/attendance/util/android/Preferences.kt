package com.example.attendance.util.android

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

object Preferences {
    private lateinit var preferences: SharedPreferences
    fun init(context: Context) {
        preferences = context.getSharedPreferences("MainActivity", MODE_PRIVATE)
    }

    fun isDarkMode() = preferences.getBoolean("darkMode?", true)
    fun setDarkMode(darkMode: Boolean) =
        preferences.edit().putBoolean("darkMode?", darkMode).commit()

    fun getDeveloper() = preferences.getBoolean("developer", false)
    fun setDeveloper(developer: Boolean) =
        preferences.edit().putBoolean("developer", developer).commit()

    fun getShowFullName(id: String) = preferences.getBoolean(id + "_fullname", false)
    fun setFullName(id: String, state: Boolean) =
        preferences.edit().putBoolean(id + "_fullname", state).commit()
}
