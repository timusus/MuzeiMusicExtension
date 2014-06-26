package com.simplecity.muzei.music;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.simplecity.muzei.music.utils.MusicExtensionUtils;

public class SettingsActivity extends PreferenceActivity {

    public static String KEY_PREF_WIFI_ONLY = "pref_key_wifi_only";
    public static String KEY_PREF_ARTWORK_RESOLUTION = "pref_key_resolution";
    public static String KEY_PREF_SIZE_MEGA = "0";
    public static String KEY_PREF_SIZE_EXTRA_LARGE = "1";
    public static String KEY_PREF_SIZE_LARGE = "2";
    public static String KEY_PREF_NOTIFICATIONS = "pref_key_notifications";
    public static String KEY_PREF_DEFAULT_ARTWORK = "pref_key_default_artwork";
    public static String KEY_PREF_USE_DEFAULT_ARTWORK = "pref_key_use_default_artwork";
    private static final int PICK_IMAGE = 100;

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

            Preference defaultArtworkPreference = findPreference(KEY_PREF_DEFAULT_ARTWORK);
            if (defaultArtworkPreference != null) {
                defaultArtworkPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        try {
                            startActivityForResult(intent, PICK_IMAGE);
                        } catch (ActivityNotFoundException ignored) {
                            //Do nothing
                        }
                        return true;
                    }
                });
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE && data != null) {

                Uri selectedImageURI = data.getData();

                if (selectedImageURI != null) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
                    prefs.edit().putString(MusicExtensionUtils.KEY_DEFAULT_ARTWORK_URI, selectedImageURI.toString()).apply();
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
