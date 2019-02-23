package com.simplecity.muzei.music.dagger.component

import com.simplecity.muzei.music.service.MusicExtensionSource
import com.simplecity.muzei.music.dagger.module.AppModule
import com.simplecity.muzei.music.dagger.module.ArtworkModule
import com.simplecity.muzei.music.dagger.module.NetworkModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, NetworkModule::class, ArtworkModule::class])

interface AppComponent {

    fun inject(target: MusicExtensionSource)

}