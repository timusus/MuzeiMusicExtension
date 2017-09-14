package com.simplecity.muzei.music

import android.app.Application
import android.os.StrictMode
import com.simplecity.muzei.music.dagger.component.AppComponent
import com.simplecity.muzei.music.dagger.component.DaggerAppComponent

class MusicExtensionApplication : Application() {

    private lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()

        appComponent = DaggerAppComponent.builder().build()

        enableStrictMode()
    }

    fun getAppComponent(): AppComponent {
        return appComponent
    }

    fun enableStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build())

            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build())
        }
    }
}