package com.simplecity.muzei.music;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteController;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.simplecity.muzei.music.utils.MusicExtensionUtils;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@TargetApi(Build.VERSION_CODES.KITKAT)
public class NLService extends NotificationListenerService implements RemoteController.OnClientUpdateListener {

    private String TAG = this.getClass().getSimpleName();

    private RemoteController mRemoteController;

    @SuppressWarnings("NewApi")
    @Override
    public void onCreate() {
        super.onCreate();

        if (MusicExtensionUtils.hasKitKat()) {

            mRemoteController = new RemoteController(this, this);
            mRemoteController.setArtworkConfiguration(1024, 1024);
            ((AudioManager) getSystemService(Context.AUDIO_SERVICE))
                    .registerRemoteController(mRemoteController);
        }
    }

    @SuppressWarnings("NewApi")
    @Override
    public void onDestroy() {

        if (MusicExtensionUtils.hasKitKat()) {
            ((AudioManager) getSystemService(Context.AUDIO_SERVICE))
                    .unregisterRemoteController(mRemoteController);
        }

        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {

        if (statusBarNotification.getPackageName().equals(MusicExtensionUtils.SPOTIFY_PACKAGE_NAME)) {
            List<String> text = getText(statusBarNotification.getNotification());
            if (text == null || text.size() < 3) {
                Intent intent = new Intent(NLService.this, MusicExtensionSource.class);
                intent.setAction(MusicExtensionUtils.EXTENSION_CLEAR_INTENT);
                startService(intent);
                return;
            }
            Intent intent = new Intent(this, MusicExtensionSource.class);
            intent.setAction(MusicExtensionUtils.EXTENSION_UPDATE_INTENT);
            Bundle bundle = new Bundle();
            bundle.putString("track", text.get(0));
            bundle.putString("album", text.get(1));
            bundle.putString("artist", text.get(2));
            intent.putExtras(bundle);
            startService(intent);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {
        //Nothing to do
    }

    /**
     * Retrieves a list of text provided to the notification
     *
     * @param notification
     * @return an {@link List} containing the text items set in the notification
     * <p/>
     * See <a href="http://stackoverflow.com/questions/9293617/retrieve-text-from-a-remoteviews-object">Stackoverflow Post</a>
     */
    public static List<String> getText(Notification notification) {
        // We have to extract the information from the view
        RemoteViews views = notification.bigContentView;
        if (views == null) views = notification.contentView;
        if (views == null) return null;

        // Use reflection to examine the m_actions member of the given RemoteViews object.
        // It's not pretty, but it works.
        List<String> text = new ArrayList<String>();
        try {
            Field field = views.getClass().getDeclaredField("mActions");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(views);

            // Find the setText() and setTime() reflection actions
            for (Parcelable p : actions) {
                Parcel parcel = Parcel.obtain();
                p.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);

                // The tag tells which type of action it is (2 is ReflectionAction, from the source)
                int tag = parcel.readInt();
                if (tag != 2) continue;

                // View ID
                parcel.readInt();

                String methodName = parcel.readString();
                if (methodName.equals("setText")) {
                    // Parameter type (10 = Character Sequence)
                    parcel.readInt();

                    // Store the actual string
                    String t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim();
                    text.add(t);
                }

                // Save times. Comment this section out if the notification time isn't important
                else if (methodName.equals("setTime")) {
                    // Parameter type (5 = Long)
                    parcel.readInt();

                    String t = new SimpleDateFormat("h:mm a").format(new Date(parcel.readLong()));
                    text.add(t);
                }

                parcel.recycle();
            }
        }

        // It's not usually good style to do this, but then again, neither is the use of reflection...
        catch (Exception e) {
            Log.e("NotificationClassifier", e.toString());
        }

        return text;
    }

    @Override
    public void onClientChange(boolean clearing) {
        if (clearing) {
            Intent intent = new Intent(NLService.this, MusicExtensionSource.class);
            intent.setAction(MusicExtensionUtils.EXTENSION_CLEAR_INTENT);
            startService(intent);
        }
    }

    @Override
    public void onClientPlaybackStateUpdate(int state) {
        if (state != RemoteControlClient.PLAYSTATE_PLAYING) {
            Intent intent = new Intent(NLService.this, MusicExtensionSource.class);
            intent.setAction(MusicExtensionUtils.EXTENSION_CLEAR_INTENT);
            startService(intent);
        }
    }

    @Override
    public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed) {
        if (state != RemoteControlClient.PLAYSTATE_PLAYING) {
            Intent intent = new Intent(NLService.this, MusicExtensionSource.class);
            intent.setAction(MusicExtensionUtils.EXTENSION_CLEAR_INTENT);
            startService(intent);
        }
    }

    @Override
    public void onClientTransportControlUpdate(int transportControlFlags) {
        if(transportControlFlags == RemoteControlClient.FLAG_KEY_MEDIA_PAUSE || transportControlFlags == RemoteControlClient.FLAG_KEY_MEDIA_STOP){
            Intent intent = new Intent(NLService.this, MusicExtensionSource.class);
            intent.setAction(MusicExtensionUtils.EXTENSION_CLEAR_INTENT);
            startService(intent);
        }
    }

    @Override
    public void onClientMetadataUpdate(RemoteController.MetadataEditor metadataEditor) {

        String artist = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_ARTIST, null);
        String albumArtist = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, null);
        String album = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_ALBUM, null);
        String track = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_TITLE, null);

        //Some music players don't provide the artist tag
        if (artist == null) {
            artist = albumArtist;
        }

        if (artist != null && album != null && track != null) {

            //Pandora add ' (Explicit)' to explicit albums. Remove that.
            if (album.contains(" (Explicit)")) {
                album = album.replace(" (Explicit)", "");
            }

            Intent intent = new Intent(NLService.this, MusicExtensionSource.class);
            intent.setAction(MusicExtensionUtils.EXTENSION_UPDATE_INTENT);
            Bundle bundle = new Bundle();
            bundle.putString("artist", artist);
            bundle.putString("album", album);
            bundle.putString("track", track);
            intent.putExtras(bundle);
            startService(intent);
        } else {
            Intent intent = new Intent(NLService.this, MusicExtensionSource.class);
            intent.setAction(MusicExtensionUtils.EXTENSION_CLEAR_INTENT);
            startService(intent);
        }
    }
}