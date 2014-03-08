package com.simplecity.muzei.music;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

import com.simplecity.muzei.music.utils.MusicExtensionUtils;

public class SettingsActivity extends PreferenceActivity {

    public static String KEY_PREF_WIFI_ONLY = "pref_key_wifi_only";
    public static String KEY_PREF_ARTWORK_RESOLUTION = "pref_key_resolution";
    public static String KEY_PREF_SIZE_MEGA = "0";
    public static String KEY_PREF_SIZE_EXTRA_LARGE = "1";
    public static String KEY_PREF_SIZE_LARGE = "2";
    public static String KEY_PREF_NOTIFICATIONS = "pref_key_notifications";

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

            //Disable the Spotify notification access preference on API < 4.3 (Notification Listener Service not available)
            if (!MusicExtensionUtils.hasJellyBeanMR2()) {
                Preference spotifyPreference = findPreference(KEY_PREF_NOTIFICATIONS);
                if (spotifyPreference != null) {
                    spotifyPreference.setEnabled(false);
                }
            }
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
