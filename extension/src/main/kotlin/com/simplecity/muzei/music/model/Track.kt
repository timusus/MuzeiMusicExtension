package com.simplecity.muzei.music.model

data class Track(val name: String, val artistName: String, val albumName: String) {

    companion object {

        fun build(name: String?, artistName: String?, albumName: String?): Track? {
            if (!name.isNullOrBlank() && !albumName.isNullOrBlank() && !artistName.isNullOrBlank()) {
                return Track(name, artistName, albumName)
            }
            return null
        }
    }
}
