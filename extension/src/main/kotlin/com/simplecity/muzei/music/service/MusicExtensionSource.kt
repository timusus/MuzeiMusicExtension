package com.simplecity.muzei.music.service

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import com.commonsware.cwac.provider.StreamProvider
import com.google.android.apps.muzei.api.Artwork
import com.google.android.apps.muzei.api.MuzeiArtSource
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource
import com.simplecity.muzei.music.Constants
import com.simplecity.muzei.music.MusicExtensionApplication
import com.simplecity.muzei.music.R
import com.simplecity.muzei.music.activity.SettingsActivity
import com.simplecity.muzei.music.artwork.ArtworkProvider
import com.simplecity.muzei.music.model.Track
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
     */
    private fun publishArtwork(track: Track, uri: Uri?) {
        publishArtwork(Artwork.Builder()
                .title(track.name)
                .byline("${track.artistName} - ${track.albumName}")
                .imageUri(uri)
                .viewIntent(Intent("android.intent.action.MUSIC_PLAYER"))
                .build())

        val editor = sharedPreferences.edit()
        editor.putString("lastTrackName", track.name)
        editor.putString("lastArtistName", track.artistName)
        editor.putString("lastAlbumName", track.albumName)
        editor.putString("lastUri", uri.toString())
        editor.apply()
    }

    @Throws(RemoteMuzeiArtSource.RetryException::class)
    override fun onTryUpdate(reason: Int) {
        if (reason == MuzeiArtSource.UPDATE_REASON_INITIAL) {
            val prefs = sharedPreferences
            val track = Track.build(prefs.getString("lastTrackName", null), prefs.getString("lastUri", null), prefs.getString("lastArtistName", null))
            val uri = prefs.getString("lastUri", null)
            if (track != null && uri != null) {
                publishArtwork(track, Uri.parse(uri))
            }
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        super.onHandleIntent(intent)

        if (intent != null && intent.action != null) {

            if (intent.action == Constants.EXTENSION_UPDATE_INTENT) {

                val extras = intent.extras
                if (extras != null) {
                    val track = Track.build(extras.getString(Constants.KEY_TRACK), extras.getString(Constants.KEY_ARTIST), extras.getString(Constants.KEY_ALBUM))
                    if (track != null) {
                        artworkProvider.getArtwork(this, track, { uri ->
                            publishArtwork(track, uri)
                        })
                    }
                }
            } else if (intent.action == Constants.EXTENSION_CLEAR_INTENT) {
                val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                if (prefs.getBoolean(SettingsActivity.KEY_PREF_USE_DEFAULT_ARTWORK, false)) {
                    val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "default_wallpaper.jpg")

                    var uri: Uri? = null
                    if (file.exists()) {
                        uri = StreamProvider.getUriForFile(applicationContext.packageName + ".streamprovider", file)
                    }

                    val artwork = Artwork.Builder()
                            .imageUri(uri)
                            .build()
                    publishArtwork(artwork)
                }
            }
        }
    }
}