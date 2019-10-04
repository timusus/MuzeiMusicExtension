package com.simplecity.muzei.music.service

import com.google.android.apps.muzei.api.provider.MuzeiArtProvider
import com.simplecity.muzei.music.MusicExtensionApplication

class MuzeiExtensionArtProvider : MuzeiArtProvider() {

    override fun onLoadRequested(initial: Boolean) {
        (context!!.applicationContext as MusicExtensionApplication).loadInitial()
    }
}