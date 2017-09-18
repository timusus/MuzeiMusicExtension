package com.simplecity.muzei.music.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.simplecity.muzei.music.Constants
import com.simplecity.muzei.music.MusicExtensionApplication
import com.simplecity.muzei.music.receiver.MyReceiver


class NotificationListenerService : android.service.notification.NotificationListenerService() {

    private val TAG = this.javaClass.simpleName

    private var mediaController: MediaController? = null

    private var mediaControllerCallback: MediaController.Callback? = null

    override fun onCreate() {
        super.onCreate()

        mediaControllerCallback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(playbackState: PlaybackState) {

                when (playbackState.state) {
                    PlaybackState.STATE_PLAYING -> {
                        mediaController?.metadata?.let { metadata ->
                            val intent = Intent(this@NotificationListenerService, MusicExtensionSource::class.java)
                            val bundle = Bundle()
                            bundle.putString(Constants.KEY_TRACK, metadata.getString(MediaMetadata.METADATA_KEY_TITLE))
                            bundle.putString(Constants.KEY_ALBUM, metadata.getString(MediaMetadata.METADATA_KEY_ALBUM))
                            bundle.putString(Constants.KEY_ARTIST, metadata.getString(MediaMetadata.METADATA_KEY_ARTIST))
                            intent.putExtras(bundle)
                            intent.action = Constants.EXTENSION_UPDATE_INTENT
                            startService(intent)
                        }
                    }
                    PlaybackState.STATE_STOPPED,
                    PlaybackState.STATE_PAUSED,
                    PlaybackState.STATE_ERROR -> {
                        val intent = Intent(this@NotificationListenerService, MusicExtensionSource::class.java)
                        intent.action = Constants.EXTENSION_CLEAR_INTENT
                        startService(intent)
                    }
                }
            }

            override fun onMetadataChanged(metadata: MediaMetadata) {
                val intent = Intent(this@NotificationListenerService, MusicExtensionSource::class.java)
                intent.action = Constants.EXTENSION_UPDATE_INTENT
                val bundle = Bundle()
                bundle.putString(Constants.KEY_TRACK, metadata.getString(MediaMetadata.METADATA_KEY_TITLE))
                bundle.putString(Constants.KEY_ALBUM, metadata.getString(MediaMetadata.METADATA_KEY_ALBUM))
                bundle.putString(Constants.KEY_ARTIST, metadata.getString(MediaMetadata.METADATA_KEY_ARTIST))
                intent.putExtras(bundle)
                startService(intent)
            }
        }

        val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        registerActiveMediaControllerCallback(mediaSessionManager)

        mediaSessionManager.addOnActiveSessionsChangedListener({
            registerActiveMediaControllerCallback(mediaSessionManager)
        }, ComponentName(this, NotificationListenerService::class.java))
    }

    override fun onBind(intent: Intent?): IBinder {

        (application as MusicExtensionApplication).notificationsEnabled = true

        // Turn off our BroadcastReceiver since the NotificationListenerService is active
        toggleReceiver(false)

        return super.onBind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {

        (application as MusicExtensionApplication).notificationsEnabled = false

        // Turn on our BroadcastReceiver since the NotificationListenerService is not active
        toggleReceiver(true)

        return super.onUnbind(intent)
    }

    private fun toggleReceiver(enabled: Boolean) {
        val componentName = ComponentName(applicationContext, MyReceiver::class.java)
        applicationContext.packageManager.setComponentEnabledSetting(
                componentName,
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP)
    }

    override fun onDestroy() {
        unregisterCallback(mediaController)

        super.onDestroy()
    }

    private fun getActiveMediaController(mediaSessionManager: MediaSessionManager): MediaController? {
        return try {
            val mediaControllers = mediaSessionManager.getActiveSessions(ComponentName(this, NotificationListenerService::class.java))
            mediaControllers.firstOrNull()
        } catch (e: SecurityException) {
            null
        }
    }

    private fun registerActiveMediaControllerCallback(mediaSessionManager: MediaSessionManager) {
        getActiveMediaController(mediaSessionManager)?.let { mediaController ->
            registerCallback(mediaController)
        }
    }

    private fun registerCallback(mediaController: MediaController) {

        unregisterCallback(this.mediaController)

        Log.i(TAG, "Registering callback for ${mediaController.packageName}")

        this.mediaController = mediaController
        mediaController.registerCallback(mediaControllerCallback)
    }

    private fun unregisterCallback(mediaController: MediaController?) {

        Log.i(TAG, "Unregistering callback for ${mediaController?.packageName}")

        mediaControllerCallback?.let { mediaControllerCallback ->
            mediaController?.unregisterCallback(mediaControllerCallback)
        }
    }
}