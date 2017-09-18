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
import java.io.File
import java.io.FileOutputStream

class SettingsActivity : PreferenceActivity() {

    companion object {

        private val PICK_IMAGE = 100

        const val KEY_PREF_WIFI_ONLY = "pref_key_wifi_only"
        const val KEY_PREF_DEFAULT_ARTWORK = "pref_key_default_artwork"
        const val KEY_PREF_USE_DEFAULT_ARTWORK = "pref_key_use_default_artwork"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentManager.beginTransaction()
                .setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                .replace(android.R.id.content, PrefsFragment())
                .commit()
    }

    class PrefsFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.preferences)

            findPreference(KEY_PREF_DEFAULT_ARTWORK)?.let { defaultArtworkPreference ->
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

                val selectedImageUri = data.data

                if (selectedImageUri != null) {
                    if (selectedImageUri.authority != null) {
                        activity.contentResolver.openInputStream(selectedImageUri).use { inputStream ->
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            if (bitmap != null) {
                                saveImageToFile(activity, bitmap)
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

            FileOutputStream(file).use { fileOutputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream)
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