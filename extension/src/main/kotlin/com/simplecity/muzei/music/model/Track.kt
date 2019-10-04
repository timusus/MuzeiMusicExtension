package com.simplecity.muzei.music.model

open class Track(val name: String, val artistName: String, val albumName: String) {

    companion object {

        fun build(name: String?, artistName: String?, albumName: String?): Track? {
            if (name != null && albumName != null && artistName != null) {
                return Track(name, artistName, albumName)
            }
            return null
        }
    }

    override fun toString(): String {
        return "Track(name='$name', albumName='$albumName', artistName='$artistName')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Track

        if (name != other.name) return false
        if (artistName != other.artistName) return false
        if (albumName != other.albumName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + artistName.hashCode()
        result = 31 * result + albumName.hashCode()
        return result
    }
}