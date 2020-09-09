package com.example.attendance.fragments.snapmit.slideshow

import android.graphics.Color
import android.os.Bundle
import androidx.core.graphics.drawable.DrawableCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.attendance.R
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.Preferences

class SlideshowFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        if (Preferences.isDarkMode()) {
            findPreference<Preference>("backBtnUwu")?.icon?.let {
                DrawableCompat.setTint(it, Color.WHITE)
            }
        }
        findPreference<Preference>("backBtnUwu")?.setOnPreferenceClickListener {
            Navigation.navigate(R.id.settings)
            true
        }
    }
}
