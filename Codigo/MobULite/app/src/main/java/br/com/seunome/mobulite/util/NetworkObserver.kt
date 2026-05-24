package br.com.seunome.mobulite.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetworkStatus { Available, Unavailable }

fun observeNetwork(context: Context): Flow<NetworkStatus> = callbackFlow {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(NetworkStatus.Available) }
        override fun onLost(network: Network) { trySend(NetworkStatus.Unavailable) }
    }
    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
    cm.registerNetworkCallback(request, callback)
    // emit initial state
    val active = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
    trySend(if (active?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) NetworkStatus.Available else NetworkStatus.Unavailable)
    awaitClose { cm.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()
