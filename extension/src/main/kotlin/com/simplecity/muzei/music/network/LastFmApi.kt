package com.simplecity.muzei.music.network

import com.simplecity.muzei.music.Constants
import com.simplecity.muzei.music.lastfm.LastFmAlbum
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface LastFmApi {

    @GET("?api_key=${Constants.LASTFM_API_KEY}&format=json&autocorrect=1&method=album.getInfo")
    fun getLastFmAlbum(@Query("artist") artist: String, @Query("album") album: String): Call<LastFmAlbum>

    @GET
    fun getArtwork(@Url url: String): Call<ResponseBody>

}