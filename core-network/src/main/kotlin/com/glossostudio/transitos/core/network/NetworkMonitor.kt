package com.glossostudio.transitos.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/** Whether the device currently has a network that can reach the internet. */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}

/** [NetworkMonitor] backed by [ConnectivityManager] network callbacks. */
class ConnectivityManagerNetworkMonitor(private val context: Context) : NetworkMonitor {
    override val isOnline: Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService<ConnectivityManager>()
        if (manager == null) {
            // Without the service we cannot tell; assume online so nothing waits on it.
            trySend(true)
            awaitClose()
            return@callbackFlow
        }
        val networks = mutableSetOf<Network>()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networks += network
                trySend(true)
            }

            override fun onLost(network: Network) {
                networks -= network
                trySend(networks.isNotEmpty())
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        manager.registerNetworkCallback(request, callback)
        trySend(manager.isCurrentlyOnline())
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }
        .distinctUntilChanged()
        .conflate()

    private fun ConnectivityManager.isCurrentlyOnline(): Boolean = getNetworkCapabilities(activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
