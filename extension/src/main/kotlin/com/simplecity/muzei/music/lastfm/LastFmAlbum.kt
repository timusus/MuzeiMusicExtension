package com.simplecity.muzei.music.lastfm

import com.google.gson.annotations.SerializedName
import java.util.*

class LastFmAlbum {

    @SerializedName("album")
    var album: Album? = null

    val imageUrl: String?
        get() = getBestImageUrl(album?.images)

    class Album {
        var name: String? = null

        @SerializedName("image")
        var images: List<LastFmImage> = ArrayList()
    }

    class LastFmImage {
        @SerializedName("#text")
        var url: String? = null

        var size: String? = null
    }

    private fun getBestImageUrl(images: List<LastFmAlbum.LastFmImage>?): String? {
        return images?.maxBy { image ->
            when (image.size) {
                "mega" -> 3
                "extralarge" -> 2
                "large" -> 1
                "medium" -> 0
                else -> {
                    -1
                }
            }
        }?.url
    }
}