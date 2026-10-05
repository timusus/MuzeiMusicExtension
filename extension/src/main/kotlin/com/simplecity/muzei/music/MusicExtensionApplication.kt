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
     * @return true if the artwork was set and the track persisted, false if publishing was skipped.
     */
    fun publishArtwork(track: Track): Boolean {

        Log.i(TAG, "Publish artwork, track: $track")

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


        val editor = sharedPreferences.edit()
        editor.putString("lastTrackName", track.name)
        editor.putString("lastArtistName", track.artistName)
        editor.putString("lastAlbumName", track.albumName)
        editor.apply()

        return true
    }

    companion object {
        const val TAG = "MusicApp"
    }
}