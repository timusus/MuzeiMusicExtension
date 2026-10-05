package com.simplecity.muzei.music

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.preference.PreferenceManager
import android.util.Log
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.ProviderContract
import com.simplecity.muzei.music.activity.SettingsActivity
import com.simplecity.muzei.music.model.Track
import com.simplecity.muzei.music.utils.NetworkUtils

class MusicExtensionApplication : Application() {

    lateinit var sharedPreferences: SharedPreferences

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    fun loadInitial() {
        val trackName = sharedPreferences.getString("lastTrackName", null)
        val artistName = sharedPreferences.getString("lastArtistName", null)
        val albumName = sharedPreferences.getString("lastAlbumName", null)

        if (trackName != null && artistName != null && albumName != null) {
            Track.build(trackName, artistName, albumName)?.let { track ->
                publishArtwork(track)
            }
        }
    }

    /**
     * The track is persisted either way.
     *
     * @return true if the artwork was set, false if publishing was skipped.
     */
    fun publishArtwork(track: Track): Boolean {

        Log.i(TAG, "Publish artwork, track: $track")

        // Persist the track even if publishing is skipped, so it is published on the next load.
        persistTrack(track)

        if (sharedPreferences.getBoolean(SettingsActivity.KEY_PREF_WIFI_ONLY, false)) {
            if (!NetworkUtils.isWifiOn(this)) {
                Log.i(TAG, "Not publishing artwork, WiFi required.")
                return false
            }
        }

        ProviderContract.getProviderClient(this, "com.simplecity.muzei.music")
                .setArtwork(
                        Artwork.Builder()
                                .token(track.hashCode().toString())
                                .title(track.name)
                                .byline("${track.artistName} - ${track.albumName}")
                                .persistentUri(
                                        Uri.Builder()
                                                .scheme("https")
                                                .authority("artwork.shuttlemusicplayer.app")
                                                .appendEncodedPath("api/v1/artwork")
                                                .appendQueryParameter("artist", track.artistName)
                                                .appendQueryParameter("album", track.albumName)
                                                .build()
                                )
                                .build()
                )

        return true
    }

    private fun persistTrack(track: Track) {
        sharedPreferences.edit()
                .putString("lastTrackName", track.name)
                .putString("lastArtistName", track.artistName)
                .putString("lastAlbumName", track.albumName)
                .apply()
    }

    companion object {
        const val TAG = "MusicApp"
    }
}