package com.simplecity.muzei.music.artwork

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.util.Log
import com.commonsware.cwac.provider.StreamProvider
import com.simplecity.muzei.music.activity.SettingsActivity
import com.simplecity.muzei.music.lastfm.LastFmAlbum
import com.simplecity.muzei.music.model.MediaStoreTrack
import com.simplecity.muzei.music.model.Track
import com.simplecity.muzei.music.network.LastFmApi
import com.simplecity.muzei.music.utils.BitmapUtils
import com.simplecity.muzei.music.utils.MediaStoreUtils
import com.simplecity.muzei.music.utils.NetworkUtils
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject

class ArtworkProvider @Inject constructor(private val lastFmApi: LastFmApi) {

    private val TAG = "ArtworkProvider"

    private var albumHttpCall: Call<LastFmAlbum>? = null
    private var artworkHttpCall: Call<ResponseBody>? = null

    fun getArtwork(context: Context, track: Track, handler: (Uri?) -> Unit) {

        val mediaStoreTrack = getTrackFromMediaStore(context, track)
        if (mediaStoreTrack != null) {
            // Look for artwork in the MediaStore / Folder Browser
            val uri = getArtworkFromMediaStore(context, mediaStoreTrack) ?: getFolderArtwork(context, mediaStoreTrack)

            if (uri == null) {
                if (canDownloadArtwork(context)) {
                    // Try Last.FM
                    getLastFmArtwork(context, mediaStoreTrack, handler)
                }
            } else {
                handler(uri)
            }
        } else {
            if (canDownloadArtwork(context)) {
                // Try Last.FM
                getLastFmArtwork(context, track, handler)
            }
        }
    }

    private fun canDownloadArtwork(context: Context): Boolean {
        val wifiOnly = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.KEY_PREF_WIFI_ONLY, false)
        return !wifiOnly || NetworkUtils.isWifiOn(context)
    }

    private fun getLastFmArtwork(context: Context, track: Track, handler: (Uri?) -> Unit) {
        albumHttpCall?.cancel()
        albumHttpCall = lastFmApi.getLastFmAlbum(track.artistName, track.albumName)
        albumHttpCall?.enqueue(object : Callback<LastFmAlbum> {
            override fun onResponse(call: Call<LastFmAlbum>, response: Response<LastFmAlbum>) {
                if (response.isSuccessful) {
                    response.body()?.imageUrl?.let { url ->
                        artworkHttpCall?.cancel()

                        // If our 'track' is not a MediaStoreTrack, we won't bother downloading it, since we can't store it in the
                        // MediaStore anyway, and Muzei presumably does its own caching.
                        if (track !is MediaStoreTrack) {
                            handler(Uri.parse(url))
                        } else {
                            // Out track is a MediaStore track. Lets download the bitmap, store it on disk and add an entry to the MediaStore
                            artworkHttpCall = lastFmApi.getArtwork(url)
                            artworkHttpCall?.enqueue(object : Callback<ResponseBody> {
                                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                    response.body()?.let { responseBody ->
                                        val bitmap = BitmapUtils.decodeSampledBitmapFromStream(responseBody.byteStream(), 1080, 1080)
                                        responseBody.close()
                                        val uri = MediaStoreUtils.addArtworkToMediaStore(context, bitmap, track)
                                        handler(uri)
                                    }
                                }

                                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                    handler(null)
                                }
                            })
                        }
                    }
                }
            }

            override fun onFailure(call: Call<LastFmAlbum>, t: Throwable?) {
                handler(null)
            }
        })
    }

    private fun getTrackFromMediaStore(context: Context, track: Track): MediaStoreTrack? {

        if (!permissionsGranted(context)) return null

        val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM
        )

        context.applicationContext.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.ALBUM} ='${track.albumName.replace("'".toRegex(), "''")}' " +
                        "AND ${MediaStore.Audio.Media.ARTIST} ='${track.artistName.replace("'".toRegex(), "''")}'",
                null,
                null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val mediaStoreTrack = MediaStoreTrack(
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)),
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)),
                        null
                )
                mediaStoreTrack.artworkPath = getArtworkPath(context, mediaStoreTrack)
                return mediaStoreTrack
            }
        }
        return null
    }

    private fun getArtworkPath(context: Context, mediaStoreTrack: MediaStoreTrack): String? {
        context.applicationContext.contentResolver.query(
                ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, mediaStoreTrack.albumId),
                arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART),
                null,
                null,
                null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART))
                return path
            }
        }

        return null
    }

    private fun getArtworkFromMediaStore(context: Context, mediaStoreTrack: MediaStoreTrack): Uri? {
        mediaStoreTrack.artworkPath?.let { artworkPath ->
            val file = File(artworkPath)
            if (file.exists()) {
                return StreamProvider.getUriForFile(context.applicationContext.packageName + ".streamprovider", File(artworkPath))
            }
        }

        return null
    }

    private fun getFolderArtwork(context: Context, track: MediaStoreTrack): Uri? {
        getTrackFromMediaStore(context, track)?.path?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val parent = file.parentFile
                if (parent != null && parent.exists() && parent.isDirectory) {
                    try {
                        parent.listFiles { artworkFile -> Pattern.compile("(folder|cover|album).*\\.(jpg|jpeg|png)", Pattern.CASE_INSENSITIVE).matcher(artworkFile.name).matches() }
                                .filter { artworkFile -> artworkFile.exists() && artworkFile.length() > 1024 }
                                .maxBy { artworkFile -> artworkFile.length() }
                                ?.let { artworkFile ->
                                    return StreamProvider.getUriForFile(context.applicationContext.packageName + ".streamprovider", artworkFile)
                                }
                    } catch (exception: NoSuchElementException) {
                        Log.e(TAG, "getFolderArtwork failed: $exception")
                    }
                }
            }
        }
        return null
    }

    private fun permissionsGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}