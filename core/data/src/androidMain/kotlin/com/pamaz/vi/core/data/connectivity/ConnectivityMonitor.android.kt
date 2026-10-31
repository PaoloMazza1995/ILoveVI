package com.pamaz.vi.core.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.pamaz.vi.core.database.PlatformContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

actual class PlatformConnectivityMonitor actual constructor(
    private val context: PlatformContext,
) : ConnectivityMonitor {

    /**
     * Requires `android.permission.ACCESS_NETWORK_STATE` (declared in `androidApp`'s manifest —
     * a library module here has no manifest of its own to declare it from). If that permission is
     * ever missing — a real crash this module caused once, `SecurityException: ... Neither user
     * ... nor current process has android.permission.ACCESS_NETWORK_STATE` — this falls back to a
     * single `true` emission (assume online) rather than letting the exception propagate and take
     * down the whole app: a wrong connectivity *signal* should never be worse than no signal.
     */
    actual override val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager =
            context.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        fun currentlyOnline(): Boolean = try {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: SecurityException) {
            true
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(currentlyOnline())
            }

            override fun onLost(network: Network) {
                trySend(currentlyOnline())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(currentlyOnline())
            }
        }

        trySend(currentlyOnline())
        try {
            connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        } catch (e: SecurityException) {
            awaitClose { }
        }
    }.distinctUntilChanged()
}
