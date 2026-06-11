// 文件说明：网络可达性监听与连接状态 Flow/回调。

package com.example.Lulu.util

import android.content.Context
import android.os.Build
import android.util.Log
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object NetworkMonitor {
    private const val TAG = "NetworkMonitor"

    fun observeNetworkStatus(context: Context): Flow<Boolean> = callbackFlow {
        val appContext = context.applicationContext
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var pendingOfflineCheck: Job? = null
        var lastKnownStatus: Boolean? = null

        fun isCurrentlyConnected(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && (
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    )
            } else {
                @Suppress("DEPRECATION")
                connectivityManager.activeNetworkInfo?.isConnected == true
            }
        }

        fun dispatchStatus(status: Boolean, reason: String) {
            if (lastKnownStatus == status) {
                return
            }
            lastKnownStatus = status
            Log.d(TAG, "status=$status reason=$reason")
            trySend(status)
        }

        fun refreshStatus(reason: String, debounceOffline: Boolean) {
            pendingOfflineCheck?.cancel()
            if (debounceOffline) {
                pendingOfflineCheck = launch {
                    delay(1200)
                    dispatchStatus(isCurrentlyConnected(), "$reason:debounced")
                }
            } else {
                dispatchStatus(isCurrentlyConnected(), reason)
            }
        }

        // 首帧必须立刻上报真实状态；若用 debounceOffline=true，约 1.2s 内 Flow 不 emit，
        // 依赖 collectAsState(initial=true) 的界面会误判「已联网」并跳过冷启动补拉，首页可能永久空白。
        refreshStatus(reason = "initial", debounceOffline = false)

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                refreshStatus(reason = "available", debounceOffline = false)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                refreshStatus(reason = "capabilities_changed", debounceOffline = false)
            }

            override fun onLost(network: Network) {
                refreshStatus(reason = "lost", debounceOffline = true)
            }

            override fun onUnavailable() {
                refreshStatus(reason = "unavailable", debounceOffline = true)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }

        awaitClose {
            pendingOfflineCheck?.cancel()
            runCatching {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            }
        }
    }
}
