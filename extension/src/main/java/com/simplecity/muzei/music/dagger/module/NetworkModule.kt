package com.simplecity.muzei.music.dagger.module

import android.util.Log
import com.simplecity.muzei.music.BuildConfig
import com.simplecity.muzei.music.network.LastFmApi
import dagger.Module
import dagger.Provides
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.net.Proxy
import javax.inject.Named
import javax.inject.Singleton

@Module
class NetworkModule {

    companion object {

        private const val TAG = "NetworkModule"

        private const val LASTFM_BASE_URL = "LASTFM_BASE_URL"
    }

    @Provides
    @Named(LASTFM_BASE_URL)
    internal fun provideLastFmBaseUrlString(): String {
        return "http://ws.audioscrobbler.com/2.0/"
    }

    @Provides
    @Singleton
    internal fun provideGsonConverter(): Converter.Factory {
        return GsonConverterFactory.create()
    }

    @Provides
    @Singleton
    internal fun provideInterceptor(): Interceptor {
        val httpLoggingInterceptor = HttpLoggingInterceptor { s -> Log.i(TAG, s) }
        httpLoggingInterceptor.level = if (BuildConfig.DEBUG)  HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.NONE
        return httpLoggingInterceptor
    }

    @Provides
    @Singleton
    internal fun provideProxy(): Proxy? {
        return if (BuildConfig.PROXY_ENABLED) Proxy(Proxy.Type.HTTP, InetSocketAddress(BuildConfig.PROXY_IP, 8888)) else null
    }

    @Provides
    @Singleton
    internal fun provideOkHttpClient(interceptor: Interceptor, proxy: Proxy?): OkHttpClient {
        return OkHttpClient.Builder()
                .proxy(proxy)
                .addInterceptor(interceptor)
                .build()
    }

    @Provides
    @Singleton
    internal fun provideRetrofit(@Named(LASTFM_BASE_URL) baseUrl: String, okHttpClient: OkHttpClient, converter: Converter.Factory): Retrofit {
        return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(converter)
                .client(okHttpClient)
                .build()
    }

    @Provides
    @Singleton
    internal fun provideLastFmApi(retrofit: Retrofit): LastFmApi {
        return retrofit.create(LastFmApi::class.java)
    }

}