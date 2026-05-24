package br.com.seunome.mobulite.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class RealtimeClient(
    private val tokenProvider: () -> String?,
    private val onEvent: (type: String, payload: JSONObject?) -> Unit,
    private val onStateChanged: (connected: Boolean) -> Unit = {}
) {
    private var webSocket: WebSocket? = null

    companion object {
        // Shared across all instances — avoids spawning new thread pools on every reconnect
        private val client = OkHttpClient.Builder()
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }

    fun connect() {
        val token = tokenProvider() ?: return
        val encodedToken = URLEncoder.encode(token, "UTF-8")
        val request = Request.Builder()
            .url("${RetrofitClient.websocketUrl()}?token=$encodedToken")
            .build()

        disconnect()

        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    onStateChanged(true)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    runCatching {
                        val json = JSONObject(text)
                        val payload = json.optJSONObject("payload")
                        onEvent(json.optString("type"), payload)
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    onStateChanged(false)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    onStateChanged(false)
                }
            }
        )
    }

    fun subscribeRide(rideId: String) {
        send("subscribe_ride", JSONObject().put("rideId", rideId))
    }

    fun disconnect() {
        webSocket?.close(1000, "closed")
        webSocket = null
        onStateChanged(false)
    }

    private fun send(type: String, payload: JSONObject) {
        webSocket?.send(
            JSONObject()
                .put("type", type)
                .put("payload", payload)
                .toString()
        )
    }
}
