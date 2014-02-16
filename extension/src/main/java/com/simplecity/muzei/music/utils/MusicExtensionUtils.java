package com.simplecity.muzei.music.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.simplecity.muzei.music.MusicExtensionSource;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class MusicExtensionUtils {

    private final static String TAG = "MusicExtensionUtils";

    public static final String SCROBBLE_META_CHANGED_INTENT = "com.android.music.metachanged";

    public static final String EXTENSION_UPDATE_INTENT = "com.simplecity.muzei.music.update";

    /**
     * Request queue for Volley
     */
    private static RequestQueue sRequestQueue;

    /**
     * Log or request VOLLEY_REQUEST_TAG
     */
    public static final String VOLLEY_REQUEST_TAG = "VolleyRequestTag";

    /**
     * Attempts to retrieve the artwork uri from the MediaStore. If that fails, the artwork is retrieved from Last.fm
     *
     * @param musicExtensionSource the {@link com.simplecity.muzei.music.MusicExtensionSource} with which to publish the artwork
     * @param artistName           the name of the artist
     * @param albumName            the name of the album
     * @param trackName            the name of the song
     */
    public static void updateMuzei(MusicExtensionSource musicExtensionSource, String artistName, String albumName, String trackName) {

        if (!updateFromMediaStore(musicExtensionSource, artistName, albumName, trackName)) {
            Log.d(TAG, "Update from MediaStore failed, attempting to retrieve from Last.fm");
            updateFromLastFM(musicExtensionSource, artistName, albumName, trackName);
        }
    }

    /**
     * Retrieves a uri for the artwork in the MediaStore, based on the passed in artist and album name
     *
     * @param musicExtensionSource the {@link com.simplecity.muzei.music.MusicExtensionSource} with which to publish the artwork
     * @param artistName           the name of the artist
     * @param albumName            the name of the album
     * @param trackName            the name of the song
     * @return true if a uri was found, false otherwise
     */
    private static boolean updateFromMediaStore(final MusicExtensionSource musicExtensionSource, final String artistName, final String albumName, final String trackName) {

        //1. Try to get the album art from the MediaStore.Audio.Albums.ALBUM_ART column
        Log.i(TAG, "Attempting to retrieve artwork from MediaStore ALBUM_ART column");
        String[] projection = new String[]{
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ART};

        Cursor cursor = musicExtensionSource.getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Audio.Albums.ALBUM + " ='" + albumName.replaceAll("'", "''") + "'"
                        + " AND "
                        + MediaStore.Audio.Albums.ARTIST + " ='" + artistName.replaceAll("'", "''") + "'",
                null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
            if (path != null) {
                Uri uri = Uri.fromFile(new File(path));
                Log.i(TAG, "Artwork found @ " + uri);
                musicExtensionSource.publishArtwork(artistName, albumName, trackName, uri);
                return true;
            }
        }

        //2. Try to find the artwork in the MediaStore based on the trackId instead of the albumId
        Log.d(TAG, "Attempting to retrieve artwork from MediaStore _ID column");
        projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM};

        cursor = musicExtensionSource.getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Audio.Albums.ALBUM + " ='" + albumName.replaceAll("'", "''") + "'"
                        + " AND "
                        + MediaStore.Audio.Albums.ARTIST + " ='" + artistName.replaceAll("'", "''") + "'",
                null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int songId = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
            Uri uri = Uri.parse("content://media/external/audio/media/" + songId + "/albumart");
            ParcelFileDescriptor pfd;
            try {
                pfd = musicExtensionSource.getApplicationContext().getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    //The artwork exists at this uri
                    Log.d("MusicExtensionUtils", "Artwork found @ " + uri);
                    musicExtensionSource.publishArtwork(artistName, albumName, trackName, uri);
                    try {
                        pfd.close();
                    } catch (IOException ignored) {
                    }
                    return true;
                }
            } catch (FileNotFoundException ignored) {
            }
        }
        return false;
    }

    /**
     * Retrieves the album artwork from Last.fm
     *
     * @param musicExtensionSource the {@link com.simplecity.muzei.music.MusicExtensionSource} with which to publish the artwork
     * @param artistName           the name of the artist
     * @param albumName            the name of the album
     * @param trackName            the name of the song
     */
    private static void updateFromLastFM(final MusicExtensionSource musicExtensionSource, final String artistName, final String albumName, final String trackName) {

        String URL = "http://ws.audioscrobbler.com/2.0/?";

        List<NameValuePair> params = new LinkedList<NameValuePair>();

        params.add(new BasicNameValuePair("method", "album.getInfo"));
        params.add(new BasicNameValuePair("autocorrect", "1"));
        params.add(new BasicNameValuePair("api_key", Config.LASTFM_API_KEY));
        params.add(new BasicNameValuePair("artist", artistName));
        params.add(new BasicNameValuePair("album", albumName));
        params.add(new BasicNameValuePair("format", "json"));

        String paramString = URLEncodedUtils.format(params, "utf-8");
        URL += paramString;

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, URL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            JSONObject albumObject = response.getJSONObject("album");
                            JSONArray imagesArray = albumObject.getJSONArray("image");
                            boolean megaImageFound = false;
                            String uri = "";
                            for (int i = 0; i < imagesArray.length(); i++) {
                                JSONObject sizeObject = imagesArray.getJSONObject(i);
                                if (sizeObject.getString("size").equals("mega")) {
                                    uri = sizeObject.getString("#text");
                                    megaImageFound = true;
                                }
                            }
                            if (!megaImageFound) {
                                for (int i = 0; i < imagesArray.length(); i++) {
                                    JSONObject sizeObject = imagesArray.getJSONObject(i);
                                    if (sizeObject.getString("size").equals("extralarge")) {
                                        uri = sizeObject.getString("#text");
                                    }
                                }
                            }

                            musicExtensionSource.publishArtwork(artistName, albumName, trackName, Uri.parse(uri));

                            //Todo: Add the downloaded album art to the MediaStore, since we're only downloading it because it wasn't found there.

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Error: ", "error" + error.getMessage());
            }
        }
        );
        addToRequestQueue(musicExtensionSource.getApplicationContext(), req);
    }

    private static RequestQueue getRequestQueue(Context context) {
        //lazy initialise the request queue, the queue instance will be
        //created when it is accessed for the first time
        if (sRequestQueue == null) {
            sRequestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }

        return sRequestQueue;
    }

    private static <T> void addToRequestQueue(Context context, Request<T> req) {
        //Set the default tag if the tag is empty
        req.setTag(VOLLEY_REQUEST_TAG);

        getRequestQueue(context).add(req);
    }

    /**
     * Cancels all pending requests by the specified VOLLEY_REQUEST_TAG, it is important
     * to specify a VOLLEY_REQUEST_TAG so that the pending/ongoing requests can be cancelled.
     *
     * @param tag
     */
    private void cancelPendingRequests(Object tag) {
        if (sRequestQueue != null) {
            sRequestQueue.cancelAll(tag);
        }
    }
}