package com.simplecity.muzei.music.dagger.module

import com.simplecity.muzei.music.artwork.ArtworkProvider
import com.simplecity.muzei.music.network.LastFmApi
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ArtworkModule {

    @Provides
    @Singleton
    fun provideArtworkProvider(lastFmApi: LastFmApi): ArtworkProvider {
        return ArtworkProvider(lastFmApi)
    }
}