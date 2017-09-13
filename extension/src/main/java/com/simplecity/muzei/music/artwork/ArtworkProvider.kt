package com.simplecity.muzei.music.artwork

import android.content.Context
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import com.commonsware.cwac.provider.StreamProvider
import com.simplecity.muzei.music.activity.SettingsActivity
import com.simplecity.muzei.music.lastfm.LastFmAlbum
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

    fun getArtwork(context: Context, artistName: String, albumName: String, handler: (Uri?) -> Unit) {

        // Look for artwork in the MediaStore
        var uri = getArtworkFromMediaStore(context, artistName, albumName)

        if (uri == null) {
            // We didn't find any.. Try the folders
            uri = getFolderArtwork(context, artistName, albumName)
        }
        if (uri != null) {
            // We found local artwork, pass it to our handler.
            handler(uri)
        } else {
            // No artwork found. Download it from LastFM
            val wifiOnly = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.KEY_PREF_WIFI_ONLY, false)
            if (!wifiOnly || NetworkUtils.isWifiOn(context)) {
                getLastFmArtwork(context, artistName, albumName, handler)
            } else {
                handler(null)
            }
        }
    }

    private fun getLastFmArtwork(context: Context, artistName: String, albumName: String, handler: (Uri?) -> Unit) {

        albumHttpCall?.cancel()
        albumHttpCall = lastFmApi.getLastFmAlbum(artistName, albumName)
        albumHttpCall?.enqueue(object : Callback<LastFmAlbum> {
            override fun onResponse(call: Call<LastFmAlbum>, response: Response<LastFmAlbum>) {
                if (response.isSuccessful) {
                    response.body()?.imageUrl?.let { url ->
                        artworkHttpCall?.cancel()
                        artworkHttpCall = lastFmApi.getArtwork(url)
                        artworkHttpCall?.enqueue(object : Callback<ResponseBody> {
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                response.body()?.let { responseBody ->
                                    val bitmap = BitmapUtils.decodeSampledBitmapFromStream(responseBody.byteStream(), 1080, 1080)
                                    responseBody.close()
                                    handler(MediaStoreUtils.addArtworkToMediaStore(context, bitmap, artistName, albumName))
                                }
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                handler(null)
                            }
                        })
                    }
                }
            }

            override fun onFailure(call: Call<LastFmAlbum>, t: Throwable?) {
                handler(null)
            }
        })
    }

    /**
     * Try to get the album art from the MediaStore.Audio.Albums.ALBUM_ART column
     */
    private fun getArtworkFromMediaStore(context: Context, artistName: String, albumName: String): Uri? {
        var uri: Uri? = null

        val projection = arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ALBUM_ART)

        val cursor = context.applicationContext.contentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Albums.ALBUM} ='${albumName.replace("'".toRegex(), "''")}' " +
                        "AND ${MediaStore.Audio.Albums.ARTIST} ='${artistName.replace("'".toRegex(), "''")}'",
                null,
                null
        )

        if (cursor != null && cursor.moveToFirst()) {
            val artworkPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART))
            if (artworkPath != null) {
                val file = File(artworkPath)
                if (file.exists()) {
                    uri = StreamProvider.getUriForFile(context.applicationContext.packageName + ".streamprovider", File(artworkPath))
                    cursor.close()
                }
            }
        }
        cursor?.close()

        return uri
    }

    private fun getFolderArtwork(context: Context, artistName: String, albumName: String): Uri? {
        var uri: Uri? = null

        val projection = arrayOf(MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DATA)

        val cursor = context.applicationContext.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.ALBUM} ='${albumName.replace("'".toRegex(), "''")}' " +
                        "AND ${MediaStore.Audio.Media.ARTIST} ='${artistName.replace("'".toRegex(), "''")}'",
                null,
                null
        )

        if (cursor != null && cursor.moveToFirst()) {
            val filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
            if (filePath != null) {
                val file = File(filePath)
                if (file.exists()) {
                    val parent = file.parentFile
                    if (parent != null && parent.exists() && parent.isDirectory) {
                        try {
                            parent.listFiles { file -> Pattern.compile("(folder|cover|album).*\\.(jpg|jpeg|png)", Pattern.CASE_INSENSITIVE).matcher(file.name).matches() }
                                    .filter { file -> file.exists() && file.length() > 1024 }
                                    .maxBy { file -> file.length() }
                                    ?.let { artworkFile ->
                                        uri = StreamProvider.getUriForFile(context.applicationContext.packageName + ".streamprovider", artworkFile)
                                    }
                        } catch (exception: NoSuchElementException) {
                            Log.e(TAG, "getFolderArtwork failed: $exception")
                        }
                    }
                }
            }
        }
        cursor?.close()

        return uri
    }
}