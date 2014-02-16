package com.simplecity.muzei.music;


import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.simplecity.muzei.music.utils.MusicExtensionUtils;

public class MusicExtensionSource extends RemoteMuzeiArtSource {

    private static final String TAG = "MusicExtensionSource";

    private static final String SOURCE_NAME = "MusicExtensionSource";

    private SharedPreferences mPrefs;

    /**
     * Remember to call this constructor from an empty constructor!
     */
    public MusicExtensionSource() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
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
                .build());

        mPrefs = getSharedPreferences();
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString("lastTrackName", trackName);
        editor.putString("lastArtistName", artistName);
        editor.putString("lastAlbumName", albumName);
        editor.putString("lastUri", uri.toString());
        editor.apply();
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        if (reason == UPDATE_REASON_INITIAL) {
            //Log.d(TAG, "Initial Update");
            mPrefs = getSharedPreferences();
            String trackName = mPrefs.getString("lastTrackName", null);
            String artistName = mPrefs.getString("lastArtistName", null);
            String albumName = mPrefs.getString("lastAlbumName", null);
            String uri = mPrefs.getString("lastUri", null);
            if (artistName != null && albumName != null && trackName != null && uri != null) {
                publishArtwork(artistName, albumName, trackName, Uri.parse(uri));
            }
        }
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
                    if (artistName != null && albumName != null && trackName != null) {
                        MusicExtensionUtils.updateMuzei(this, artistName, albumName, trackName);
                    }
                }
            }
        }
    }
}
