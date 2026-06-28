package com.fluxtv.app.utils

import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings

object SecurityUtils {

    // Detección de VPN
    fun isVpnActive(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networks = cm.allNetworks
            networks.any { network ->
                val caps = cm.getNetworkCapabilities(network)
                caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) == true
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = cm.getNetworkInfo(17) // TYPE_VPN
            networkInfo?.isConnected == true
        }
    }

    fun showVpnDialog(ctx: Context, onDismiss: () -> Unit) {
        AlertDialog.Builder(ctx)
            .setTitle("⚠️ VPN Detectada")
            .setMessage("FluxTV no es compatible con VPN activa.\n\nDesactivá tu VPN para continuar.")
            .setPositiveButton("Entendido") { _, _ -> onDismiss() }
            .setCancelable(false)
            .show()
    }

    // Device ID único
    fun getDeviceId(ctx: Context): String {
        return Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }
}
