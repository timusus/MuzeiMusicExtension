package com.simplecity.muzei.music.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.commonsware.cwac.provider.StreamProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object MediaStoreUtils {

    private val TAG = "MediaStoreUtils"

    fun addArtworkToMediaStore(context: Context, bitmap: Bitmap?, artistName: String, albumName: String): Uri? {

        var uri: Uri? = null

        if (bitmap == null) return uri

        // First, save the artwork on the device
        val savePath = Environment.getExternalStorageDirectory().toString() + "/albumthumbs/" + System.currentTimeMillis().toString()
        if (FileUtils.ensureFileExists(savePath)) {
            var cursor: Cursor? = null
            try {
                val outputStream = FileOutputStream(savePath)
                var success = bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                outputStream.close()

                // Now that the artwork is saved, add it to the MediaStore

                val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DATA)

                cursor = context.applicationContext.contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        "${MediaStore.Audio.Albums.ALBUM} ='${albumName.replace("'".toRegex(), "''")}' " +
                                "AND ${MediaStore.Audio.Albums.ARTIST} ='${artistName.replace("'".toRegex(), "''")}'", null, null
                )

                if (cursor != null && cursor.moveToFirst()) {
                    val albumId = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))

                    val values = ContentValues()
                    values.put("album_id", albumId)
                    values.put("_data", savePath)

                    var newUri = context.applicationContext.contentResolver.insert(Uri.parse("content://media/external/audio/albumart"), values)
                    if (newUri == null) {
                        success = false
                        // Failed to insert into the database. Attempt to update existing entry (if there is one)
                        newUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId.toLong())
                        if (context.applicationContext.contentResolver.update(newUri, values, null, null) > 0) {
                            // Failed to insert into the database
                            success = true
                        }
                    }

                    val file = File(savePath)
                    if (success) {
                        uri = StreamProvider.getUriForFile(context.applicationContext.packageName + ".streamprovider", file)
                    } else {
                        // If we failed to either save the bitmap on the device, or save it to the database, delete the File we created
                        Log.e(TAG, "MediaStore database insertion failed")
                        file.delete()
                    }
                }

            } catch (e: IOException) {
                Log.e(TAG, "error creating file", e)
            } finally {
                cursor?.close()
            }
        }

        return uri
    }
}