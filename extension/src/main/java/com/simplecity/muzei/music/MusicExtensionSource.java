package com.simplecity.muzei.music;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.simplecity.muzei.music.utils.MusicExtensionUtils;

public class MusicExtensionSource extends RemoteMuzeiArtSource {
    private static final String SOURCE_NAME = "MusicExtensionSource";

    /**
     * Remember to call this constructor from an empty constructor!
     */
    public MusicExtensionSource() {
        super(SOURCE_NAME);
    }

    /**
     * Publishes the {@link com.google.android.apps.muzei.api.Artwork} to Muzei
     *
     * @param artistName the name of the artist
     * @param albumName  the name of the album
     * @param trackName  the name of the song
     * @param uri        the {@link android.net.Uri} to the album art
     */
    public void publishArtwork(String artistName, String albumName, String trackName, Uri uri) {
        publishArtwork(new Artwork.Builder()
                .title(trackName)
                .byline(artistName + " - " + albumName)
                .imageUri(uri)
                .viewIntent(new Intent(Intent.ACTION_VIEW, uri))
                .build());
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        //Nothing to do
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(MusicExtensionUtils.EXTENSION_UPDATE_INTENT)) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    final String artistName = extras.getString("artist");
                    final String albumName = extras.getString("album");
                    final String trackName = extras.getString("track");
                    MusicExtensionUtils.updateMuzei(this, artistName, albumName, trackName);
                }
            }
        }
    }
}
