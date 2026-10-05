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

    private class Session(val controller: MediaController, val callback: MediaController.Callback)

    /**
     * Active sessions, kept in the order returned by getActiveSessions (system priority order).
     */
    private var sessions = linkedMapOf<MediaSession.Token, Session>()

    private var lastPublishedTrack: Track? = null

    private lateinit var mediaSessionManager: MediaSessionManager

    private fun createCallback(controller: MediaController) = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publishPlayingTrack(controller)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publishPlayingTrack(controller)
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

    override fun onListenerDisconnected() {
        removeSessionStateChangeListener()
        clearMediaControllers()

        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        removeSessionStateChangeListener()
        clearMediaControllers()

        super.onDestroy()
    }

    private fun removeSessionStateChangeListener() {
        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsChangedListener)
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to remove session change listener")
        }
    }

    private fun clearMediaControllers() {
        sessions.values.toList().forEach { unregisterCallback(it) }
        sessions.clear()
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
            Log.e(TAG, "Failed to get active sessions (notification listener access not granted?)")
        }
    }

    /**
     * Registers a callback on every active controller, and unregisters the callback
     * for any controller which is no longer in the list.
     */
    private fun updateMediaControllers(controllers: List<MediaController>) {
        val activeTokens = controllers.map { it.sessionToken }.toSet()

        sessions.filterKeys { it !in activeTokens }.values.forEach { unregisterCallback(it) }

        // Rebuild the map in the order of the given list, reusing existing sessions.
        val updated = linkedMapOf<MediaSession.Token, Session>()
        controllers.forEach { controller ->
            updated[controller.sessionToken] = sessions[controller.sessionToken]
                    ?: Session(controller, createCallback(controller)).also { registerCallback(it) }
        }
        sessions = updated
    }

    /**
     * Publishes the track from a playing controller. The [preferred] controller (the one whose
     * callback just fired) is tried first, then the others in system priority order. A playing
     * controller without valid metadata is skipped. If nothing is playing, the last published
     * artwork is left alone.
     */
    private fun publishPlayingTrack(preferred: MediaController? = null) {
        val playing = sessions.values
                .map { it.controller }
                .filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                .sortedBy { if (it.sessionToken == preferred?.sessionToken) 0 else 1 }

        for (controller in playing) {
            try {
                val metadata = controller.metadata ?: continue
                val track = Track.build(
                        metadata.getString(MediaMetadata.METADATA_KEY_TITLE),
                        metadata.getString(MediaMetadata.METADATA_KEY_ARTIST),
                        metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
                ) ?: continue

                if (track == lastPublishedTrack) {
                    return
                }

                if ((applicationContext as MusicExtensionApplication).publishArtwork(track)) {
                    lastPublishedTrack = track
                }
                return
            } catch (e: RuntimeException) {
                Log.e(TAG, "An error occurred reading the media metadata: $e")
            }
        }
    }

    private fun registerCallback(session: Session) {
        Log.i(TAG, "Registering callback for ${session.controller.packageName}")

        session.controller.registerCallback(session.callback)
    }

    private fun unregisterCallback(session: Session) {
        Log.i(TAG, "Unregistering callback for ${session.controller.packageName}")

        session.controller.unregisterCallback(session.callback)
    }
}
