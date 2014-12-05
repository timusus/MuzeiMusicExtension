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
                Intent intent1 = new Intent(context, MusicExtensionSource.class);
                intent1.setAction(MusicExtensionUtils.EXTENSION_UPDATE_INTENT);
                intent1.putExtra(Constants.KEY_ARTIST, artistName);
                intent1.putExtra(Constants.KEY_ALBUM, albumName);
                intent1.putExtra(Constants.KEY_TRACK, trackName);
                context.startService(intent1);
            }

            if (extras.containsKey("playing")) {
                if (!extras.getBoolean("playing", true)) {
                    //The song has been paused. Set the Muzei artwork to something else.
                    Intent intent1 = new Intent(context, MusicExtensionSource.class);
                    intent1.setAction(MusicExtensionUtils.EXTENSION_CLEAR_INTENT);
                    context.startService(intent1);
                }
            }
        }
    }
}