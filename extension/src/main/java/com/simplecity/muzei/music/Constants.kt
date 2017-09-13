package com.simplecity.muzei.music

object Constants {

    const val KEY_ARTIST = "artist"
    const val KEY_ALBUM = "album"
    const val KEY_TRACK = "track"

    //Please do not use this key, go to Last FM and create you own!!
    const val LASTFM_API_KEY = "deb5e4fefed2a96884d938493baec43c"

    // Used in the notification listener service to determine if notification was filled by Spotify
    const val SPOTIFY_PACKAGE_NAME = "com.spotify.mobile.android.ui"

    // Used in the notification listener service to determine if notification was filled by Spotify
    const val SPOTIFY_ALT_PACKAGE_NAME = "com.spotify.music"

    // Tells the MusicExtensionSource to update itself
    const val EXTENSION_UPDATE_INTENT = "com.simplecity.muzei.music.update"

    // Tells the MusicExtensionSource to clear itself
    const val EXTENSION_CLEAR_INTENT = "com.simplecity.muzei.music.clear"
}