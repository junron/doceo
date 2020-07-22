package com.example.attendance.fragments.snapmit.slideshow;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.example.snapmit.R;

public class SlideshowFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }
}
