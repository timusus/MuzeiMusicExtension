package com.simplecity.muzei.music.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.commonsware.cwac.provider.StreamProvider
import com.simplecity.muzei.music.model.MediaStoreTrack
import java.io.File
import java.io.FileOutputStream

object MediaStoreUtils {

    private const val TAG = "MediaStoreUtils"

    fun addArtworkToMediaStore(context: Context, bitmap: Bitmap?, track: MediaStoreTrack): Uri? {

        var uri: Uri? = null

        if (bitmap == null) return uri

        // First, save the artwork on the device
        val savePath = Environment.getExternalStorageDirectory().toString() + "/albumthumbs/" + System.currentTimeMillis().toString()
        if (FileUtils.ensureFileExists(savePath)) {
            val outputStream = FileOutputStream(savePath)
            outputStream.use {
                var success = bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)

                // Now that the artwork is saved, add it to the MediaStore
                val values = ContentValues()
                values.put("album_id", track.albumId)
                values.put("_data", savePath)

                var newUri = context.applicationContext.contentResolver.insert(Uri.parse("content://media/external/audio/albumart"), values)
                if (newUri == null) {
                    success = false
                    // Failed to insert into the database. Attempt to update existing entry (if there is one)
                    newUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), track.albumId)
                    try {
                        if (context.applicationContext.contentResolver.update(newUri, values, null, null) > 0) {
                            // Failed to insert into the database
                            success = true
                        }
                    } catch (e: SQLiteException) {
                        Log.e(TAG, "Failed to update MediaStore row. Uri; $newUri")
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
        }

        return uri
    }
}