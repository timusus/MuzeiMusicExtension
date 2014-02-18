package com.simplecity.muzei.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.simplecity.muzei.music.utils.MusicExtensionUtils;

public class MyReceiver extends BroadcastReceiver {

    private static final String TAG = "MyReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

//        Bundle bundle = intent.getExtras();
//        if (bundle != null) {
//            Log.d(TAG, "Called by: " + intent.getAction());
//            for (String key : bundle.keySet()) {
//                Object value = bundle.get(key);
//                if (value != null) {
//                    Log.d(TAG, String.format("%s %s (%s)", key,
//                            value.toString(), value.getClass().getName()));
//                }
//            }
//        }

        String action = intent.getAction();
        if (action != null) {
            if (action.equals(MusicExtensionUtils.META_CHANGED_INTENT)
                    || action.equals(MusicExtensionUtils.PLAYSTATE_CHANGED_INTENT)
                    || action.equals(MusicExtensionUtils.PLAYER_PRO_TRIAL_META_CHANGED_INTENT)
                    || action.equals(MusicExtensionUtils.PLAYER_PRO_META_CHANGED_INTENT)
                    || action.equals(MusicExtensionUtils.RDIO_PLAYSTATE_CHANGED_INTENT)
                    || action.equals(MusicExtensionUtils.DOUBLETWIST_META_CHANGED_INTENT)
                    || action.equals(MusicExtensionUtils.ROCKETPLAYER_META_CHANGED_INTENT)
                    || action.equals(MusicExtensionUtils.ANDROID_MUSIC_PLAYER_META_CHANGED_INTENT)
                    || action.equals(MusicExtensionUtils.SAMSUNG_META_CHANGED_INTENT)
                    || action.equals(MusicExtensionUtils.RHAPSODY_META_CHANGED_INTENT)
                    || action.equals(MusicExtensionUtils.MIUI_META_CHANGED_INTENT)
                    || action.equals(MusicExtensionUtils.HTC_META_CHANGED_INTENT)
                    ) {
                Intent intent1 = new Intent(context, MusicExtensionSource.class);
                intent1.setAction(MusicExtensionUtils.EXTENSION_UPDATE_INTENT);
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    intent1.putExtras(extras);
                    context.startService(intent1);
                }
            }
        }
    }
}