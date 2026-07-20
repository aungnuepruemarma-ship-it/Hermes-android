package net.nous.hermes.net

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * WebSocket client for the `hermes dashboard` backend.
 *
 * AUTH: the FastAPI WS routes (/api/pty, /api/ws, /api/pub, /api/events)
 * authenticate via a `?token=<_SESSION_TOKEN>` QUERY PARAM, not a header —
 * the browser/WS upgrade cannot set Authorization, so the backend reads the
 * token from the query string (web_server.py ~lines 7565, 7897, 7933, 7801).
 * The SAME token value as the REST X-Hermes-Session-Token header.
 *
 * Host header: also required on WS upgrade for the DNS-rebinding defence.
 *
 * Channels (verified present on 0.16.0):
 *   /api/pty    — live PTY byte stream (📜 terminal view)
 *   /api/ws     — agent websocket channel
 *   /api/pub    — pub/sub events
 * (Note: /api/events appeared in source comments but returns 404 on 0.16.0;
 *  use /api/pub for event streaming.)
 */
class HermesWebSocket(
    private val baseUrl: String,        // "http://127.0.0.1:9119" (ws:// scheme added below)
    private val sessionToken: String,
    private val hostHeader: String,     // "127.0.0.1:9119"
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // stream forever
        .build()

    /**
     * Open [channel] (e.g. "pty", "ws", "pub", "events") and emit every text
     * frame as a String. Call close() on the returned AutoCloseable to detach.
     */
    fun open(channel: String): Flow<String> = callbackFlow {
        val wsBase = baseUrl.replace("https://", "wss://").replace("http://", "ws://")
        val url = "$wsBase/api/$channel?token=${sessionToken}"
        val request = Request.Builder()
            .url(url)
            .header("Host", hostHeader)
            .build()

        val socket: WebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                trySend(text)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(t) // propagate as flow completion cause
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                close()
            }
        })

        awaitClose { socket.close(1000, "client close") }
    }

    fun send(channel: String, text: String) {
        // For command channels (/api/ws, /api/pty input) you open a persistent
        // socket; this helper is provided for one-shot sends if needed.
        val wsBase = baseUrl.replace("https://", "wss://").replace("http://", "ws://")
        val url = "$wsBase/api/$channel?token=${sessionToken}"
        val request = Request.Builder().url(url).header("Host", hostHeader).build()
        val socket = client.newWebSocket(request, object : WebSocketListener() {})
        socket.send(text)
    }
}
