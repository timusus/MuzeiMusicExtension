package com.simplecity.muzei.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.simplecity.muzei.music.utils.MusicExtensionUtils;

public class MyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(MusicExtensionUtils.SCROBBLE_META_CHANGED_INTENT)) {
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
