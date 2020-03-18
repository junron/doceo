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

    fun getTextScale() = preferences.getFloat("textScale", 1F)
    fun setTextScale(scale: Float) = preferences.edit().putFloat("textScale", scale).commit()
}
