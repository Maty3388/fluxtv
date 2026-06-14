package com.fluxtv.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

/**
 * Monitorea el estado de la conexion a internet y notifica cambios.
 */
class NetworkMonitor(private val ctx: Context) {
    private val cm = ctx.getSystemService(Context.CONNECTIVITY_MANAGER) as ConnectivityManager
    private var callback: ConnectivityManager.NetworkCallback? = null
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null

    fun isConnected(): Boolean {
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun start() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { onConnected?.invoke() }
            override fun onLost(network: Network) { onDisconnected?.invoke() }
        }
        try {
            cm.registerNetworkCallback(request, callback!!)
        } catch (_: Exception) {}
    }

    fun stop() {
        callback?.let {
            try { cm.unregisterNetworkCallback(it) } catch (_: Exception) {}
        }
        callback = null
    }
}
