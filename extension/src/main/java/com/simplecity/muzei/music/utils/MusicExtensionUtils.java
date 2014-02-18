package com.simplecity.muzei.music.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.simplecity.muzei.music.MusicExtensionSource;
import com.simplecity.muzei.music.SettingsActivity;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

public class MusicExtensionUtils {

    private final static String TAG = "MusicExtensionUtils";

    // Generic meta changed intent for most music players
    public static final String META_CHANGED_INTENT = "com.android.music.metachanged";

    // PlayerPro intents
    public static final String PLAYER_PRO_TRIAL_META_CHANGED_INTENT = "com.tbig.playerpro.metachanged";
    public static final String PLAYER_PRO_META_CHANGED_INTENT = "com.tbig.playerprotrial.metachanged";

    // Rdio track information is only contained within the Playstate Changed intent
    public static final String RDIO_PLAYSTATE_CHANGED_INTENT = "com.rdio.android.playstatechanged";

    // Generic play state intent for most music players.
    // PowerAMP provides track data via this intent rather than metachanged (unless 'send metachanged' is on
    public static final String PLAYSTATE_CHANGED_INTENT = "com.android.music.playstatechanged";

    // Doubletwist meta changed intent
    public static final String DOUBLETWIST_META_CHANGED_INTENT = "com.doubleTwist.androidPlayer.metachanged";

    // Rocketplayer meta changed intent
    public static final String ROCKETPLAYER_META_CHANGED_INTENT = "com.jrtstudio.AnotherMusicPlayer.metachanged";

    // Android Music Player intent (JRT)
    public static final String ANDROID_MUSIC_PLAYER_META_CHANGED_INTENT = "com.jrtstudio.music.metachanged";

    // Samsung intent
    public static final String SAMSUNG_META_CHANGED_INTENT = "com.sec.android.app.music.metachanged";

    // Rhapsody intent
    public static final String RHAPSODY_META_CHANGED_INTENT = "com.rhapsody.metachanged";

    // MIUI intent
    public static final String MIUI_META_CHANGED_INTENT = "com.miui.player.playstatechanged";

    // HTC intent
    public static final String HTC_META_CHANGED_INTENT = "com.htc.music.metachanged";

    // Used in the notification listener service to determine if notification was filled by Spotify
    public static final String SPOTIFY_PACKAGE_NAME = "com.spotify.mobile.android.ui";

    // Tells the MusicExtensionSource to update itself
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

        if (musicExtensionSource == null || artistName == null || albumName == null || trackName == null) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(musicExtensionSource.getApplicationContext());
        boolean wifiOnly = prefs.getBoolean(SettingsActivity.KEY_PREF_WIFI_ONLY, false);

        if (!updateFromMediaStore(musicExtensionSource, artistName, albumName, trackName)) {

            //If we've specified Wi-Fi only and the Wi-Fi is not connected, don't download from last.fm
            if (wifiOnly && !isWifiOn(musicExtensionSource.getApplicationContext())) {
                return;
            }

            //Log.d(TAG, "Update from MediaStore failed, attempting to retrieve from Last.fm");
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

        if (artistName == null || albumName == null || trackName == null) {
            return false;
        }

        if (albumName.equals(MediaStore.UNKNOWN_STRING)) {
            return false;
        }

        String path = null;

        //1. Try to get the album art from the MediaStore.Audio.Albums.ALBUM_ART column
        //Log.i(TAG, "Attempting to retrieve artwork from MediaStore ALBUM_ART column");
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
            String artworkPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
            if (artworkPath != null) {
                Uri uri = Uri.fromFile(new File(artworkPath));
                //Log.i(TAG, "Artwork found @ " + uri);
                musicExtensionSource.publishArtwork(artistName, albumName, trackName, uri);
                return true;
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        //2. Try to find the artwork in the MediaStore based on the trackId instead of the albumId
        //Log.d(TAG, "Attempting to retrieve artwork from MediaStore _ID column");
        projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
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
            path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            Uri uri = Uri.parse("content://media/external/audio/media/" + songId + "/albumart");
            ParcelFileDescriptor pfd;
            try {
                pfd = musicExtensionSource.getApplicationContext().getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    //The artwork exists at this uri
                    //Log.d("MusicExtensionUtils", "Artwork found @ " + uri);
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
        if (cursor != null) {
            cursor.close();
        }

        // 3. Try to find the artwork within the folder
        //Log.d(TAG, "Attempting to retrieve artwork from folder");

        //Todo: Check other common paths/filetypes, such as Folder.jpg

        if (path != null) {
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash > 0) {
                String artworkPath = path.substring(0, lastSlash + 1) + "AlbumArt.jpg";
                File file = new File(artworkPath);
                if (file.exists()) {
                    Uri uri = Uri.fromFile(file);
                    //Log.d("MusicExtensionUtils", "Artwork found @ " + uri);
                    musicExtensionSource.publishArtwork(artistName, albumName, trackName, uri);
                    return true;
                }
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
    private static void updateFromLastFM(final MusicExtensionSource musicExtensionSource,
                                         final String artistName, final String albumName, final String trackName) {

        //Cancel any prending Volley requests, as we don't care about the previous track anymore.
        cancelPendingRequests();

        if (albumName.equals(MediaStore.UNKNOWN_STRING)) {
            return;
        }

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

                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(musicExtensionSource.getApplicationContext());
                        String preferred_size = prefs.getString(SettingsActivity.KEY_PREF_ARTWORK_RESOLUTION, SettingsActivity.KEY_PREF_SIZE_MEGA);

                        try {
                            JSONObject albumObject = response.getJSONObject("album");
                            JSONArray imagesArray = albumObject.getJSONArray("image");
                            boolean megaImageFound = false;
                            boolean extraLargeImageFound = false;
                            boolean largeImageFound = false;
                            String uri = "";
                            for (int i = 0; i < imagesArray.length(); i++) {
                                JSONObject sizeObject = imagesArray.getJSONObject(i);
                                if (sizeObject.getString("size").equals("mega") && preferred_size.equals(SettingsActivity.KEY_PREF_SIZE_MEGA)) {
                                    uri = sizeObject.getString("#text");
                                    megaImageFound = true;
                                }
                            }
                            if (!megaImageFound) {
                                for (int i = 0; i < imagesArray.length(); i++) {
                                    JSONObject sizeObject = imagesArray.getJSONObject(i);
                                    if (sizeObject.getString("size").equals("extralarge") && preferred_size.equals(SettingsActivity.KEY_PREF_SIZE_EXTRA_LARGE)) {
                                        uri = sizeObject.getString("#text");
                                        extraLargeImageFound = true;
                                    }
                                }
                            }
                            if (!megaImageFound && !extraLargeImageFound) {
                                for (int i = 0; i < imagesArray.length(); i++) {
                                    JSONObject sizeObject = imagesArray.getJSONObject(i);
                                    if (sizeObject.getString("size").equals("large") && preferred_size.equals(SettingsActivity.KEY_PREF_SIZE_LARGE)) {
                                        uri = sizeObject.getString("#text");
                                    }
                                }
                            }

                            //Log.i(TAG, "Obtained image uri from Last.fm: " + uri);

                            musicExtensionSource.publishArtwork(artistName, albumName, trackName, Uri.parse(uri));

                            //Add the artwork to the MediaStore, since we're only here because it wasn't found there

                            ImageRequest imageRequest = new ImageRequest(uri, new Response.Listener<Bitmap>() {
                                @Override
                                public void onResponse(Bitmap bitmap) {

                                    //First, save the artwork on the device
                                    String savePath = Environment.getExternalStorageDirectory() + "/albumthumbs/" + String.valueOf(System.currentTimeMillis());
                                    if (ensureFileExists(savePath)) {
                                        try {

                                            OutputStream outputStream = new FileOutputStream(savePath);
                                            boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
                                            outputStream.close();

                                            //Now that the artwork is saved, add it to the MediaStore

                                            String[] projection = new String[]{
                                                    MediaStore.Audio.Media._ID,
                                                    MediaStore.Audio.Media.ALBUM_ID,
                                                    MediaStore.Audio.Media.ARTIST,
                                                    MediaStore.Audio.Media.ALBUM,
                                                    MediaStore.Audio.Media.DATA
                                            };

                                            Cursor cursor = musicExtensionSource.getApplicationContext().getContentResolver().query(
                                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                                    projection,
                                                    MediaStore.Audio.Albums.ALBUM + " ='" + albumName.replaceAll("'", "''") + "'"
                                                            + " AND "
                                                            + MediaStore.Audio.Albums.ARTIST + " ='" + artistName.replaceAll("'", "''") + "'",
                                                    null, null);

                                            if (cursor != null && cursor.moveToFirst()) {
                                                int albumId = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));

                                                ContentValues values = new ContentValues();
                                                values.put("album_id", albumId);
                                                values.put("_data", savePath);

                                                Uri newuri = musicExtensionSource.getApplicationContext().getContentResolver().insert(Uri.parse("content://media/external/audio/albumart"), values);
                                                if (newuri == null) {
                                                    //Failed to insert into the database
                                                    success = false;
                                                }

                                                //If we failed to either save the bitmap on the device, or save it to the database, delete the File we created
                                                if (!success) {
                                                    File f = new File(savePath);
                                                    f.delete();
                                                }
                                            }
                                            if (cursor != null) {
                                                cursor.close();
                                            }

                                        } catch (FileNotFoundException e) {
                                            Log.e(TAG, "error creating file", e);
                                        } catch (IOException e) {
                                            Log.e(TAG, "error creating file", e);
                                        }

                                    }
                                }
                            }, 0, 0, Bitmap.Config.RGB_565, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    //Error response for the image download
                                    Log.e("Error: ", "error" + error.getMessage());
                                }
                            }
                            );

                            addToRequestQueue(musicExtensionSource.getApplicationContext(), imageRequest);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Error response for the Uri retrieval
                Log.e("Error: ", "error" + error.getMessage());
            }
        }
        );

        addToRequestQueue(musicExtensionSource.getApplicationContext(), req);
    }

    /**
     * Checks to see whether the path exists, or creates it if not
     *
     * @param path the path to check or create
     * @return {@link boolean} whether the path exists and or was created
     */
    private static boolean ensureFileExists(String path) {
        File file = new File(path);
        if (file.exists()) {
            return true;
        } else {
            //Don't create the first directory in the path
            //(for example, do not create /sdcard if the SD card is not mounted)
            int secondSlash = path.indexOf('/', 1);
            if (secondSlash < 1) return false;
            String directoryPath = path.substring(0, secondSlash);
            File directory = new File(directoryPath);
            if (!directory.exists())
                return false;
            file.getParentFile().mkdirs();
            try {
                return file.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "File creation failed", e);
            }
            return false;
        }
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
     * Cancels all pending requests
     */
    private static void cancelPendingRequests() {
        sRequestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

    private static boolean isWifiOn(Context context) {

        final ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        //Check the state of the wifi network
        final NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiNetwork != null && wifiNetwork.isConnectedOrConnecting();
    }

    public static boolean hasJellyBeanMR2() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }
}