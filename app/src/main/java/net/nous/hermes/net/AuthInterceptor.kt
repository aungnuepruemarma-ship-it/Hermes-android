package net.nous.hermes.net

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Injects the two headers the `hermes dashboard` backend requires on
 * loopback binds:
 *
 *  1. X-Hermes-Session: <token>   (or legacy "Authorization: Bearer <token>")
 *  1. X-Hermes-Session-Token: <token>   (the backend's real header name,
 *     confirmed at web_server.py line 140: _SESSION_HEADER_NAME =
 *     "X-Hermes-Session-Token"). Legacy "Authorization: Bearer <token>" is
 *     also accepted. The backend checks this on every /api/ route except
 *     /api/status (verified: GET /api/tools/toolsets returned 401 without it,
 *     200 with it).
 *
 *  2. Host: 127.0.0.1:<port>
 *     — the backend enforces a Host-header allow-list (DNS-rebinding defence,
 *       GHSA-ppp5-vxwm-4cf7). A missing/wrong Host yields HTTP 400. Retrofit
 *       would otherwise send the real interface; we pin it explicitly.
 *
 * The token is supplied by the launcher service. The launcher spawns the
 * dashboard with HERMES_DASHBOARD_SESSION_TOKEN=<known> in its environment
 * (web_server.py line 139: token = os.environ.get("HERMES_DASHBOARD_...TOKEN")
 * or secrets.token_urlsafe(32)). Setting it makes the token deterministic so
 * the Android app does not need to scrape process stdout.
 */
class AuthInterceptor(
    private val sessionToken: String,
    private val hostHeader: String, // e.g. "127.0.0.1:9119"
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request().newBuilder()
            .header("X-Hermes-Session-Token", sessionToken)
            .header("Host", hostHeader)
            .build()
        return chain.proceed(req)
    }
}
