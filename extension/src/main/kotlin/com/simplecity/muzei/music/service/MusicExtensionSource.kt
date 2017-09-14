package com.simplecity.muzei.music.service

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import com.google.android.apps.muzei.api.Artwork
import com.google.android.apps.muzei.api.MuzeiArtSource
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource
import com.simplecity.muzei.music.Constants
import com.simplecity.muzei.music.MusicExtensionApplication
import com.simplecity.muzei.music.R
import com.simplecity.muzei.music.activity.SettingsActivity
import com.simplecity.muzei.music.artwork.ArtworkProvider
import java.io.File
import javax.inject.Inject

/**
 * Remember to call this constructor from an empty constructor!
 */
class MusicExtensionSource : RemoteMuzeiArtSource("MusicExtensionSource") {

    private val TAG = "MusicExtensionSource"

    @Inject lateinit var artworkProvider: ArtworkProvider

    override fun onCreate() {
        super.onCreate()

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        (applicationContext as MusicExtensionApplication)
                .getAppComponent()
                .inject(this)
    }

    /**
     * Publishes the [com.google.android.apps.muzei.api.Artwork] to Muzei
     *
     * @param artistName the name of the artist
     * @param albumName  the name of the album
     * @param trackName  the name of the song
     * @param uri        the [android.net.Uri] to the album art
     */
    fun publishArtwork(artistName: String, albumName: String, trackName: String, uri: Uri?) {
        publishArtwork(Artwork.Builder()
                .title(trackName)
                .byline("$artistName - $albumName")
                .imageUri(uri)
                .viewIntent(Intent("android.intent.action.MUSIC_PLAYER"))
                .build())

        val editor = sharedPreferences.edit()
        editor.putString("lastTrackName", trackName)
        editor.putString("lastArtistName", artistName)
        editor.putString("lastAlbumName", albumName)
        editor.putString("lastUri", uri.toString())
        editor.apply()
    }

    @Throws(RemoteMuzeiArtSource.RetryException::class)
    override fun onTryUpdate(reason: Int) {
        if (reason == MuzeiArtSource.UPDATE_REASON_INITIAL) {
            val prefs = sharedPreferences
            val trackName = prefs.getString("lastTrackName", null)
            val artistName = prefs.getString("lastArtistName", null)
            val albumName = prefs.getString("lastAlbumName", null)
            val uri = prefs.getString("lastUri", null)
            if (artistName != null && albumName != null && trackName != null && uri != null) {
                publishArtwork(artistName, albumName, trackName, Uri.parse(uri))
            }
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        super.onHandleIntent(intent)

        if (intent != null && intent.action != null) {
            if (intent.action == Constants.EXTENSION_UPDATE_INTENT) {
                val extras = intent.extras
                if (extras != null) {
                    val artistName = extras.getString(Constants.KEY_ARTIST)
                    val albumName = extras.getString(Constants.KEY_ALBUM)
                    val trackName = extras.getString(Constants.KEY_TRACK)
                    if (artistName != null && albumName != null && trackName != null) {
                        artworkProvider.getArtwork(this, artistName, albumName, { uri ->
                            publishArtwork(artistName, albumName, trackName, uri)
                        })
                    }

                }
            } else if (intent.action == Constants.EXTENSION_CLEAR_INTENT) {
                val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                if (prefs.getBoolean(SettingsActivity.KEY_PREF_USE_DEFAULT_ARTWORK, false)) {
                    val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "default_wallpaper.jpg")
                    publishArtwork(Artwork.Builder()
                            .imageUri(if (file.exists()) Uri.fromFile(file) else null)
                            .build())
                }
            }
        }
    }
}