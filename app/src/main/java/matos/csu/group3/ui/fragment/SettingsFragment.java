package matos.csu.group3.ui.fragment;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import matos.csu.group3.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // Find preferences by key
        SwitchPreferenceCompat syncPreference = findPreference("sync_onedrive");
        SwitchPreferenceCompat autoPlayMotionPreference = findPreference("auto_play_motion");

        if (syncPreference != null) {
            syncPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isEnabled = (boolean) newValue;
                // Handle enabling/disabling OneDrive sync
                return true;
            });
        }

        if (autoPlayMotionPreference != null) {
            autoPlayMotionPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isEnabled = (boolean) newValue;
                // Handle auto-play setting change
                return true;
            });
        }
    }
}

