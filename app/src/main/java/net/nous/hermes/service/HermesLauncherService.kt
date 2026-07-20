package net.nous.hermes.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import net.nous.hermes.net.HermesClient
import java.io.BufferedReader
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * HermesLauncherService
 * ---------------------
 * Android foreground Service that LAUNCHES and SUPERVISES the `hermes dashboard`
 * backend inside Termux, then exposes the session token to the rest of the app.
 *
 * Why a Service (not just spawning from an Activity):
 *  - Android 8+ kills background processes aggressively; a foreground service
 *    with a persistent notification keeps the Termux child alive while the app
 *    is in the background, giving the "Windows-like always-on" feel.
 *
 * Token handling (the key integration detail):
 *  - We SET HERMES_DASHBOARD_SESSION_TOKEN to a value we generate, before
 *    spawning. web_server.py line 139 reads this env var (falls back to a
 *    random token only if unset). Setting it makes the token DETERMINISTIC,
 *    so the app knows it without scraping process stdout.
 *  - The token is stored in EncryptedSharedPreferences (not plaintext).
 *
 * Loopback-only: dashboard is bound to 127.0.0.1 — never 0.0.0.0 on Android
 * unless you deliberately want the LAN OAuth gate.
 */
class HermesLauncherService : Service() {

    companion object {
        const val PORT = 9119
        const val HOST = "127.0.0.1"
        const val ACTION_START = "net.nous.hermes.action.START"
        const val ACTION_STOP = "net.nous.hermes.action.STOP"
        const val NOTIF_ID = 1
        const val CHANNEL_ID = "hermes_runtime"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var dashboardProcess: Process? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRuntime()
            ACTION_STOP -> stopRuntime()
        }
        return START_STICKY
    }

    private fun startRuntime() {
        startForeground(NOTIF_ID, buildNotification("Hermes runtime starting…"))

        scope.launch {
            val token = ensureToken()
            val hermesBin = findHermesBinary() ?: run {
                updateNotification("Termux `hermes` not found")
                return@launch
            }

            val pb = ProcessBuilder(
                hermesBin,
                "dashboard",
                "--host", HOST,
                "--port", PORT.toString(),
                "--no-open",
            )
            // Deterministic session token (see class KDoc).
            pb.environment()["HERMES_DASHBOARD_SESSION_TOKEN"] = token
            pb.redirectErrorStream(true)

            runCatching {
                dashboardProcess = pb.start()
            }.onFailure {
                updateNotification("Failed to start Hermes: ${it.message}")
                return@launch
            }

            // Poll /api/status until ready (public endpoint, no token needed).
            val ready = waitForReady(timeoutMs = 20_000)
            if (ready) {
                updateNotification("Hermes runtime online · :$PORT")
                // Token is now known to the app via getToken().
            } else {
                updateNotification("Hermes did not come up in time")
            }
        }
    }

    private fun stopRuntime() {
        dashboardProcess?.destroy()      // SIGTERM; backend cleans up
        dashboardProcess = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        dashboardProcess?.destroy()
        scope.cancel()
        super.onDestroy()
    }

    // ---- helpers ----

    private fun findHermesBinary(): String? {
        // Termux app private bin. Adjust PREFIX if your Termux layout differs.
        val candidates = listOf(
            "/data/data/com.termux/files/usr/bin/hermes",
            System.getenv("PREFIX")?.let { "$it/bin/hermes" },
        ).filterNotNull()
        return candidates.firstOrNull { File(it).canExecute() }
    }

    private fun ensureToken(): String {
        val prefs = EncryptedPrefs.get(applicationContext)
        var token = prefs.getString("session_token", null)
        if (token == null) {
            token = java.util.UUID.randomUUID().toString().replace("-", "") +
                    java.util.UUID.randomUUID().toString().replace("-", "")
            EncryptedPrefs.putToken(applicationContext, token)
        }
        return token
    }

    fun getToken(): String? =
        EncryptedPrefs.getToken(applicationContext)

    private suspend fun waitForReady(timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (pingStatus()) return true
            delay(500)
        }
        return false
    }

    private fun pingStatus(): Boolean = runCatching {
        val url = URL("http://$HOST:$PORT/api/status")
        (url.openConnection() as HttpURLConnection).run {
            requestMethod = "GET"
            connectTimeout = 1000
            readTimeout = 1000
            val ok = responseCode == 200
            disconnect()
            ok
        }
    }.getOrDefault(false)

    private fun buildNotification(text: String): Notification {
        createChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hermes Agent")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "Hermes Runtime", NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }
}
