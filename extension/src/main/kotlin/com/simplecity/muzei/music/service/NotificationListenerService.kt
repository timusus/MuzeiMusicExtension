package com.simplecity.muzei.music.service

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import com.simplecity.muzei.music.MusicExtensionApplication
import com.simplecity.muzei.music.model.Track


class NotificationListenerService : android.service.notification.NotificationListenerService() {

    private val TAG = this.javaClass.simpleName

    private val mediaControllers = mutableMapOf<MediaSession.Token, MediaController>()

    private var lastPublishedTrack: Track? = null

    private lateinit var mediaSessionManager: MediaSessionManager

    private val mediaControllerCallback: MediaController.Callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publishPlayingTrack()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publishPlayingTrack()
        }
    }

    private val activeSessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateMediaControllers(controllers.orEmpty())
        publishPlayingTrack()
    }

    override fun onCreate() {
        super.onCreate()

        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        refreshMediaControllers()

        addSessionStateChangeListener()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        refreshMediaControllers()

        addSessionStateChangeListener()
    }

    override fun onDestroy() {
        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsChangedListener)
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to remove session change listener")
        }

        mediaControllers.values.toList().forEach { unregisterCallback(it) }
        mediaControllers.clear()

        super.onDestroy()
    }

    private fun addSessionStateChangeListener() {
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(activeSessionsChangedListener, ComponentName(this, NotificationListenerService::class.java))
            Log.i(TAG, "Successfully added session change listener")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to add session change listener")
        }
    }

    private fun refreshMediaControllers() {
        try {
            val controllers = mediaSessionManager.getActiveSessions(ComponentName(this, NotificationListenerService::class.java))
            updateMediaControllers(controllers)
            publishPlayingTrack()
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to get active sessions")
        }
    }

    /**
     * Registers a callback on every active controller, and unregisters the callback
     * for any controller which is no longer in the list.
     */
    private fun updateMediaControllers(controllers: List<MediaController>) {
        val activeTokens = controllers.map { it.sessionToken }.toSet()

        mediaControllers.keys.filter { it !in activeTokens }.forEach { token ->
            mediaControllers.remove(token)?.let { unregisterCallback(it) }
        }

        controllers.forEach { controller ->
            if (!mediaControllers.containsKey(controller.sessionToken)) {
                registerCallback(controller)
                mediaControllers[controller.sessionToken] = controller
            }
        }
    }

    /**
     * Publishes the track from the controller which is currently playing. If nothing is
     * playing, the last published artwork is left alone.
     */
    private fun publishPlayingTrack() {
        val controller = mediaControllers.values.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: return

        try {
            val metadata = controller.metadata ?: return
            val track = Track.build(
                    metadata.getString(MediaMetadata.METADATA_KEY_TITLE),
                    metadata.getString(MediaMetadata.METADATA_KEY_ARTIST),
                    metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ) ?: return

            if (track == lastPublishedTrack) {
                return
            }

            lastPublishedTrack = track
            (applicationContext as MusicExtensionApplication).publishArtwork(track)
        } catch (e: RuntimeException) {
            Log.e(TAG, "An error occurred reading the media metadata: $e")
        }
    }

    private fun registerCallback(mediaController: MediaController) {
        Log.i(TAG, "Registering callback for ${mediaController.packageName}")

        mediaController.registerCallback(mediaControllerCallback)
    }

    private fun unregisterCallback(mediaController: MediaController) {
        Log.i(TAG, "Unregistering callback for ${mediaController.packageName}")

        mediaController.unregisterCallback(mediaControllerCallback)
    }
}
