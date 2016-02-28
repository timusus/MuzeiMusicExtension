package com.simplecity.muzei.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.simplecity.muzei.music.utils.Constants;
import com.simplecity.muzei.music.utils.MusicExtensionUtils;

public class MyReceiver extends BroadcastReceiver {

    private static final String TAG = "MyReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle extras = intent.getExtras();
        if (extras != null) {
            final String artistName = extras.getString(Constants.KEY_ARTIST);
            final String albumName = extras.getString(Constants.KEY_ALBUM);
            final String trackName = extras.getString(Constants.KEY_TRACK);

            if (artistName != null && albumName != null && trackName != null) {
                Intent updateIntent = new Intent(context, MusicExtensionSource.class);
                updateIntent.setAction(MusicExtensionUtils.EXTENSION_UPDATE_INTENT);
                updateIntent.putExtra(Constants.KEY_ARTIST, artistName);
                updateIntent.putExtra(Constants.KEY_ALBUM, albumName);
                updateIntent.putExtra(Constants.KEY_TRACK, trackName);
                context.startService(updateIntent);
            }

            if (extras.containsKey("playing")) {
                if (!extras.getBoolean("playing", true)) {

                    //The song has been paused. Set the Muzei artwork to something else.
                    Intent clearIntent = new Intent(context, MusicExtensionSource.class);
                    clearIntent.setAction(MusicExtensionUtils.EXTENSION_CLEAR_INTENT);
                    context.startService(clearIntent);
                }
            }
        }
    }
}