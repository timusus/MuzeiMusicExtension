package com.simplecity.muzei.music.utils

import android.content.Context
import android.net.ConnectivityManager

object NetworkUtils {

    fun isWifiOn(context: Context): Boolean {

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        //Check the state of the wifi network
        val wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        return wifiNetwork != null && wifiNetwork.isConnectedOrConnecting
    }
}