//package com.simplecity.muzei.music.service
//
//import android.app.Notification
//import android.content.ComponentName
//import android.content.Context
//import android.content.Intent
//import android.media.AudioManager
//import android.media.MediaMetadataRetriever
//import android.media.RemoteControlClient
//import android.media.RemoteController
//import android.media.session.MediaSessionManager
//import android.os.Bundle
//import android.os.Parcel
//import android.os.Parcelable
//import android.service.notification.NotificationListenerService
//import android.service.notification.StatusBarNotification
//import android.support.v4.media.MediaMetadataCompat
//import android.support.v4.media.session.MediaControllerCompat
//import android.support.v4.media.session.PlaybackStateCompat
//import android.text.TextUtils
//import android.util.Log
//import android.widget.RemoteViews
//import com.simplecity.muzei.music.Constants
//import com.simplecity.muzei.music.utils.VersionUtils
//import java.text.SimpleDateFormat
//import java.util.*
//import java.util.List
//
//class NLService : NotificationListenerService() {
//
//    private val TAG = this.javaClass.simpleName
//
//    private var mRemoteController: RemoteController? = null
//
//    private var mMediaController: MediaControllerCompat? = null
//
//    private var mMediaControllerCallback: MediaControllerCompat.Callback? = null
//
//    override fun onCreate() {
//        super.onCreate()
//
//        if (VersionUtils.hasKitKat() && !VersionUtils.hasLollipop()) {
//
//            val updateListener = object : RemoteController.OnClientUpdateListener {
//                override fun onClientChange(clearing: Boolean) {
//                    if (clearing) {
//                        val intent = Intent(this@NLService, MusicExtensionSource::class.java)
//                        intent.action = Constants.EXTENSION_CLEAR_INTENT
//                        startService(intent)
//                    }
//                }
//
//                override fun onClientPlaybackStateUpdate(state: Int) {
//                    if (state != RemoteControlClient.PLAYSTATE_PLAYING) {
//                        val intent = Intent(this@NLService, MusicExtensionSource::class.java)
//                        intent.action = Constants.EXTENSION_CLEAR_INTENT
//                        startService(intent)
//                    }
//                }
//
//                override fun onClientPlaybackStateUpdate(state: Int, stateChangeTimeMs: Long, currentPosMs: Long, speed: Float) {
//                    if (state != RemoteControlClient.PLAYSTATE_PLAYING) {
//                        val intent = Intent(this@NLService, MusicExtensionSource::class.java)
//                        intent.action = Constants.EXTENSION_CLEAR_INTENT
//                        startService(intent)
//                    }
//                }
//
//                override fun onClientTransportControlUpdate(transportControlFlags: Int) {
//                    if (transportControlFlags == RemoteControlClient.FLAG_KEY_MEDIA_PAUSE || transportControlFlags == RemoteControlClient.FLAG_KEY_MEDIA_STOP) {
//                        val intent = Intent(this@NLService, MusicExtensionSource::class.java)
//                        intent.action = Constants.EXTENSION_CLEAR_INTENT
//                        startService(intent)
//                    }
//                }
//
//                override fun onClientMetadataUpdate(metadataEditor: RemoteController.MetadataEditor) {
//
//                    var artist: String? = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_ARTIST, null)
//                    val albumArtist = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, null)
//                    var album: String? = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_ALBUM, null)
//                    val track = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_TITLE, null)
//
//                    //Some music players don't provide the artist tag
//                    if (artist == null) {
//                        artist = albumArtist
//                    }
//
//                    if (artist != null && album != null && track != null) {
//
//                        //Pandora adds '(Explicit)' to explicit albums. Remove that.
//                        if (album.contains(" (Explicit)")) {
//                            album = album.replace(" (Explicit)", "")
//                        }
//
//                        val intent = Intent(this@NLService, MusicExtensionSource::class.java)
//                        intent.action = Constants.EXTENSION_UPDATE_INTENT
//                        val bundle = Bundle()
//                        bundle.putString(Constants.KEY_ARTIST, artist)
//                        bundle.putString(Constants.KEY_ALBUM, album)
//                        bundle.putString(Constants.KEY_TRACK, track)
//                        intent.putExtras(bundle)
//                        startService(intent)
//                    } else {
//                        val intent = Intent(this@NLService, MusicExtensionSource::class.java)
//                        intent.action = Constants.EXTENSION_CLEAR_INTENT
//                        startService(intent)
//                    }
//                }
//            }
//
//            mRemoteController = RemoteController(this, updateListener)
//            mRemoteController!!.setArtworkConfiguration(1024, 1024)
//            (getSystemService(Context.AUDIO_SERVICE) as AudioManager).registerRemoteController(mRemoteController)
//        }
//
//        if (VersionUtils.hasLollipop()) {
//
//            mMediaControllerCallback = object : MediaControllerCompat.Callback() {
//                override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
//                    if (state.state == PlaybackStateCompat.STATE_STOPPED
//                            || state.state == PlaybackStateCompat.STATE_PAUSED
//                            || state.state == PlaybackStateCompat.STATE_ERROR) {
//                        val intent = Intent(this@NLService, MusicExtensionSource::class.java)
//                        intent.action = Constants.EXTENSION_UPDATE_INTENT
//                        startService(intent)
//                    }
//                }
//
//                override fun onMetadataChanged(metadata: MediaMetadataCompat) {
//                    val intent = Intent(this@NLService, MusicExtensionSource::class.java)
//                    intent.action = Constants.EXTENSION_UPDATE_INTENT
//                    val bundle = Bundle()
//                    bundle.putString(Constants.KEY_TRACK, metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
//                    bundle.putString(Constants.KEY_ALBUM, metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))
//                    bundle.putString(Constants.KEY_ARTIST, metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
//                    intent.putExtras(bundle)
//                    startService(intent)
//                }
//            }
//
//            val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
//            val mediaControllers = mediaSessionManager.getActiveSessions(ComponentName(this, NLService::class.java))
//            if (mediaControllers != null && mediaControllers.size != 0) {
//                mMediaController = mediaControllers[0]
//                mMediaController!!.registerCallback(mMediaControllerCallback!!)
//            }
//        }
//
//    }
//
//    override fun onDestroy() {
//
//        if (VersionUtils.hasKitKat() && !VersionUtils.hasLollipop()) {
//            (getSystemService(Context.AUDIO_SERVICE) as AudioManager)
//                    .unregisterRemoteController(mRemoteController)
//        }
//
//        if (VersionUtils.hasLollipop() && mMediaController != null) {
//            mMediaController!!.unregisterCallback(mMediaControllerCallback!!)
//        }
//
//        super.onDestroy()
//    }
//
//    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
//
//        if (statusBarNotification.packageName == Constants.SPOTIFY_PACKAGE_NAME || statusBarNotification.packageName == Constants.SPOTIFY_ALT_PACKAGE_NAME) {
//            val text = getText(statusBarNotification.notification)
//            if (text == null || text.size < 3) {
//                Log.d(TAG, "Notification text null")
//                val intent = Intent(this@NLService, MusicExtensionSource::class.java)
//                intent.action = Constants.EXTENSION_CLEAR_INTENT
//                startService(intent)
//                return
//            }
//
//            val intent = Intent(this, MusicExtensionSource::class.java)
//            intent.action = Constants.EXTENSION_UPDATE_INTENT
//            val bundle = Bundle()
//            bundle.putString(Constants.KEY_TRACK, text[0])
//            bundle.putString(Constants.KEY_ALBUM, text[1])
//            bundle.putString(Constants.KEY_ARTIST, text[2])
//            intent.putExtras(bundle)
//            startService(intent)
//        }
//    }
//
//    override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {
//        //Nothing to do
//    }
//
//    companion object {
//
//        /**
//         * Retrieves a list of text provided to the notification
//         *
//         * @param notification
//         * @return an [List] containing the text items set in the notification
//         *
//         *
//         * See [Stackoverflow Post](http://stackoverflow.com/questions/9293617/retrieve-text-from-a-remoteviews-object)
//         */
//        fun getText(notification: Notification): List<String>? {
//            // We have to extract the information from the view
//            var views: RemoteViews? = notification.bigContentView
//            if (views == null) views = notification.contentView
//            if (views == null) return null
//
//            // Use reflection to examine the m_actions member of the given RemoteViews object.
//            // It's not pretty, but it works.
//            val text = ArrayList<String>()
//            try {
//                val field = views.javaClass.getDeclaredField("mActions")
//                field.isAccessible = true
//
//                val actions = field.get(views) as ArrayList<Parcelable>
//
//                // Find the setText() and setTime() reflection actions
//                for (p in actions) {
//                    val parcel = Parcel.obtain()
//                    p.writeToParcel(parcel, 0)
//                    parcel.setDataPosition(0)
//
//                    // The tag tells which type of action it is (2 is ReflectionAction, from the source)
//                    val tag = parcel.readInt()
//                    if (tag != 2) continue
//
//                    // View ID
//                    parcel.readInt()
//
//                    val methodName = parcel.readString()
//                    if (methodName == "setText") {
//                        // Parameter type (10 = Character Sequence)
//                        parcel.readInt()
//
//                        // Store the actual string
//                        val t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim { it <= ' ' }
//                        text.add(t)
//                    } else if (methodName == "setTime") {
//                        // Parameter type (5 = Long)
//                        parcel.readInt()
//
//                        val t = SimpleDateFormat("h:mm a").format(Date(parcel.readLong()))
//                        text.add(t)
//                    }// Save times. Comment this section out if the notification time isn't important
//
//                    parcel.recycle()
//                }
//            } catch (e: Exception) {
//                Log.e("NotificationClassifier", e.toString())
//            }
//            // It's not usually good style to do this, but then again, neither is the use of reflection...
//
//            return text
//        }
//    }
//}