package com.simplecity.muzei.music.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
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

        if (albumName.equals(MediaStore.UNKNOWN_STRING)) {
            return false;
        }

        String path = null;

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
            String artworkPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
            if (artworkPath != null) {
                Uri uri = Uri.fromFile(new File(artworkPath));
                Log.i(TAG, "Artwork found @ " + uri);
                musicExtensionSource.publishArtwork(artistName, albumName, trackName, uri);
                return true;
            }
        }

        //2. Try to find the artwork in the MediaStore based on the trackId instead of the albumId
        Log.d(TAG, "Attempting to retrieve artwork from MediaStore _ID column");
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

        // 3. Try to find the artwork within the folder
        Log.d(TAG, "Attempting to retrieve artwork from folder");
        if (path != null) {
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash > 0) {
                String artworkPath = path.substring(0, lastSlash + 1) + "AlbumArt.jpg";
                File file = new File(artworkPath);
                if (file.exists()) {
                    Uri uri = Uri.fromFile(file);
                    Log.d("MusicExtensionUtils", "Artwork found @ " + uri);
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