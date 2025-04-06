package ru.krotarnya.diasync.app.fragment;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import ru.krotarnya.diasync.app.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}