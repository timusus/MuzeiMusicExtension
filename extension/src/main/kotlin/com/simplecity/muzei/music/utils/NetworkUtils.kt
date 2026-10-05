package com.simplecity.muzei.music.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object NetworkUtils {

    fun isWifiOn(context: Context): Boolean {

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            return isWifiLike(cm.getNetworkCapabilities(network))
        }

        @Suppress("DEPRECATION")
        val wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        @Suppress("DEPRECATION")
        return wifiNetwork != null && wifiNetwork.isConnectedOrConnecting
    }

    /**
     * @return true if the capabilities describe an internet-capable Wi-Fi or Ethernet network.
     */
    fun isWifiLike(capabilities: NetworkCapabilities?): Boolean {
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    }
}
