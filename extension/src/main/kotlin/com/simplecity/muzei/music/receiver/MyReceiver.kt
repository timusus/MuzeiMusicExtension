package com.simplecity.muzei.music.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplecity.muzei.music.Constants
import com.simplecity.muzei.music.service.MusicExtensionSource

class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        intent.extras?.let { extras ->
            val artistName = extras.getString(Constants.KEY_ARTIST)
            val albumName = extras.getString(Constants.KEY_ALBUM)
            val trackName = extras.getString(Constants.KEY_TRACK)

            if (artistName != null && albumName != null && trackName != null) {
                val updateIntent = Intent(context, MusicExtensionSource::class.java)
                updateIntent.action = Constants.EXTENSION_UPDATE_INTENT
                updateIntent.putExtra(Constants.KEY_ARTIST, artistName)
                updateIntent.putExtra(Constants.KEY_ALBUM, albumName)
                updateIntent.putExtra(Constants.KEY_TRACK, trackName)
                context.startService(updateIntent)
            }

            if (extras.containsKey("playing")) {
                if (!extras.getBoolean("playing", true)) {

                    // The song has been paused. Set the Muzei artwork to something else.
                    val clearIntent = Intent(context, MusicExtensionSource::class.java)
                    clearIntent.action = Constants.EXTENSION_CLEAR_INTENT
                    context.startService(clearIntent)
                }
            }
        }
    }
}