package com.simplecity.muzei.music;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity {

    public static String KEY_PREF_WIFI_ONLY = "pref_key_wifi_only";
    public static String KEY_PREF_ARTWORK_RESOLUTION = "pref_key_resolution";
    public static String KEY_PREF_SIZE_MEGA = "0";
    public static String KEY_PREF_SIZE_EXTRA_LARGE = "1";
    public static String KEY_PREF_SIZE_LARGE = "2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the preference fragment as the main content.
        getFragmentManager().beginTransaction().setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                .replace(android.R.id.content, new Prefs1Fragment())
                .commit();
    }

    /**
     * This fragment shows the preferences for the first header.
     */
    public static class Prefs1Fragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
