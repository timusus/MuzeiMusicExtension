package com.simplecity.muzei.music;

import android.app.Application;
import android.os.StrictMode;

public class MusicExtensionApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //Todo: Switch to false for release version
        boolean debug = false;

        if (debug && BuildConfig.DEBUG) {

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());

            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build());
        }

    }
}