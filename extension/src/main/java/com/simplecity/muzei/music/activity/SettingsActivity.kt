package com.simplecity.muzei.music.activity

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment
import android.view.MenuItem
import com.simplecity.muzei.music.R
import com.simplecity.muzei.music.utils.VersionUtils
import java.io.*

class SettingsActivity : PreferenceActivity() {

    companion object {

        private val PICK_IMAGE = 100

        const val KEY_PREF_WIFI_ONLY = "pref_key_wifi_only"
        const val KEY_PREF_NOTIFICATIONS = "pref_key_notifications"
        const val KEY_PREF_DEFAULT_ARTWORK = "pref_key_default_artwork"
        const val KEY_PREF_USE_DEFAULT_ARTWORK = "pref_key_use_default_artwork"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display the preference fragment as the main content.
        fragmentManager.beginTransaction().setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                .replace(android.R.id.content, Prefs1Fragment())
                .commit()
    }

    class Prefs1Fragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences)

            //Disable the Spotify notification access preference on API < 4.3 (Notification Listener Service not available)
            if (!VersionUtils.hasJellyBeanMR2()) {
                val spotifyPreference = findPreference(KEY_PREF_NOTIFICATIONS)
                if (spotifyPreference != null) {
                    spotifyPreference.isEnabled = false
                }
            }

            val defaultArtworkPreference = findPreference(KEY_PREF_DEFAULT_ARTWORK)
            if (defaultArtworkPreference != null) {
                defaultArtworkPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                    intent.type = "image/*"
                    try {
                        startActivityForResult(intent, PICK_IMAGE)
                    } catch (ignored: ActivityNotFoundException) {
                        //Do nothing
                    }
                    true
                }
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE && data != null) {

                val selectedImageURI = data.data

                if (selectedImageURI != null) {

                    if (selectedImageURI.authority != null) {
                        var inputStream: InputStream? = null
                        try {
                            inputStream = activity.contentResolver.openInputStream(selectedImageURI)
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            if (bitmap != null) {
                                saveImageToFile(activity, bitmap)
                            }
                        } catch (e: FileNotFoundException) {
                            e.printStackTrace()
                        } finally {
                            if (inputStream != null) {
                                try {
                                    inputStream.close()
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }

                            }
                        }
                    }
                }
            }
        }

        private fun saveImageToFile(context: Context, bitmap: Bitmap) {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "default_wallpaper.jpg")
            if (file.exists()) {
                file.delete()
            }

            var fileOutputStream: FileOutputStream? = null
            try {
                fileOutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}