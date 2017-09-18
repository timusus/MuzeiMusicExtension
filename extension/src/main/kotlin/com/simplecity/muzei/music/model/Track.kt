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

}