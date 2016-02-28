package com.simplecity.muzei.music;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

import com.simplecity.muzei.music.utils.MusicExtensionUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
                        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                        intent.setType("image/*");
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

                    if (selectedImageURI.getAuthority() != null) {
                        InputStream inputStream = null;
                        try {
                            inputStream = getActivity().getContentResolver().openInputStream(selectedImageURI);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            if (bitmap != null) {
                                saveImageToFile(getActivity(), bitmap);
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } finally {
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
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


    public static void saveImageToFile(Context context, Bitmap bitmap) {

        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "default_wallpaper.jpg");
        if (file.exists()) {
            file.delete();
        }

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
