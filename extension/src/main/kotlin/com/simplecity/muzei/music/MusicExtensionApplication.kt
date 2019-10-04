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

    fun publishArtwork(track: Track) {

        Log.i(TAG, "Publish artwork, track: $track")

        if (sharedPreferences.getBoolean(SettingsActivity.KEY_PREF_WIFI_ONLY, false)) {
            if (!NetworkUtils.isWifiOn(this)) {
                Log.i(TAG, "Not publishing artwork, WiFi required.")
                return
            }
        }

        ProviderContract.getProviderClient(this, "com.simplecity.muzei.music")
                .setArtwork(
                        Artwork.Builder()
                                .token(track.hashCode().toString())
                                .title(track.name)
                                .byline("${track.artistName} - ${track.albumName}")
                                .persistentUri(Uri.parse("https://artwork.shuttlemusicplayer.app/api/v1/artwork?artist=${track.artistName}&album=${track.albumName}"))
                                .build()
                )


        val editor = sharedPreferences.edit()
        editor.putString("lastTrackName", track.name)
        editor.putString("lastArtistName", track.artistName)
        editor.putString("lastAlbumName", track.albumName)
        editor.apply()
    }

    companion object {
        const val TAG = "MusicApp"
    }
}