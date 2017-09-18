package com.simplecity.muzei.music.model

class MediaStoreTrack(name: String, artistName: String, albumName: String, val albumId: Long, val path: String, var artworkPath: String?)
    : Track(name, artistName, albumName)