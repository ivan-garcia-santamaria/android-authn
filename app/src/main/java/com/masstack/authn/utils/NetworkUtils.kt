package com.masstack.authn.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log

object NetworkUtils {
    private const val TAG = "NetworkUtils"

    /**
     * Check if device has network connectivity
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val hasValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

            Log.d(TAG, "Network check (API ${Build.VERSION.SDK_INT}+):")
            Log.d(TAG, "  Has internet capability: $hasInternet")
            Log.d(TAG, "  Has validated capability: $hasValidated")
            Log.d(TAG, "  Transport types: ${getTransportTypes(capabilities)}")

            hasInternet && hasValidated
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            val isConnected = networkInfo?.isConnected == true

            Log.d(TAG, "Network check (API <23):")
            Log.d(TAG, "  Is connected: $isConnected")
            @Suppress("DEPRECATION")
            Log.d(TAG, "  Type: ${networkInfo?.typeName}")

            isConnected
        }
    }

    private fun getTransportTypes(capabilities: NetworkCapabilities?): String {
        if (capabilities == null) return "none"

        val types = mutableListOf<String>()
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) types.add("WiFi")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) types.add("Cellular")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) types.add("Ethernet")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) types.add("VPN")
        }

        return types.joinToString(", ").ifEmpty { "unknown" }
    }

    /**
     * Get network type description
     */
    fun getNetworkType(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            getTransportTypes(capabilities)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.typeName ?: "None"
        }
    }
}
