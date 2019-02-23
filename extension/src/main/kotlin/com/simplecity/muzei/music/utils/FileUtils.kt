package com.simplecity.muzei.music.utils

import android.util.Log
import java.io.File
import java.io.IOException

object FileUtils {

    private const val TAG = "FileUtils"

    /**
     * Checks to see whether the path exists, or creates it if not
     *
     * @param path the path to check or create
     *
     * @return [Boolean] whether the path exists and or was created
     */
    fun ensureFileExists(path: String): Boolean {
        val file = File(path)
        if (file.exists()) {
            return true
        } else {
            // Don't create the first directory in the path
            // (for example, do not create /sdcard if the SD card is not mounted)
            val secondSlash = path.indexOf('/', 1)
            if (secondSlash < 1) return false
            val directoryPath = path.substring(0, secondSlash)
            val directory = File(directoryPath)
            if (!directory.exists())
                return false
            file.parentFile.mkdirs()
            try {
                return file.createNewFile()
            } catch (e: IOException) {
                Log.e(TAG, "File creation failed", e)
            }

            return false
        }
    }
}