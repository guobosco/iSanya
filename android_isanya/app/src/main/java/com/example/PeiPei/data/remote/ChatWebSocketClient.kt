// 文件说明：聊天 WebSocket 客户端，负责长连接与实时消息。

package com.example.Lulu.data.remote

import android.util.Log
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URLEncoder

class ChatWebSocketClient(
    private val onTextMessage: (String) -> Unit,
    private val onConnectedChanged: (Boolean) -> Unit
) {
    private var webSocket: WebSocket? = null

    fun connect() {
        val token = AuthSession.getAccessToken() ?: return
        disconnect()
        val encodedToken = URLEncoder.encode(token, Charsets.UTF_8.name())
        val url = "${RetrofitClient.wsBaseUrl}ws/chat?token=$encodedToken"
        val request = Request.Builder().url(url).build()
        webSocket = RetrofitClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onConnectedChanged(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                onTextMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onConnectedChanged(false)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                onConnectedChanged(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("ChatWebSocketClient", "socket failed: ${t.message}", t)
                onConnectedChanged(false)
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "client close")
        webSocket = null
        onConnectedChanged(false)
    }

    fun sendPing() {
        webSocket?.send("{\"type\":\"ping\"}")
    }
}
