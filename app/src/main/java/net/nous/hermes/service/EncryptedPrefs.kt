package net.nous.hermes.service

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Thin wrapper over EncryptedSharedPreferences for storing the Hermes
 * dashboard session token on-device. The token is the only credential the
 * Android app holds; it must never be written to plaintext prefs or logs.
 */
object EncryptedPrefs {
    private const val FILE = "hermes_secure"

    fun get(context: Context): SharedPreferences {
        val master = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE,
            master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Read the dashboard session token (or null if not yet generated). */
    fun getToken(context: Context): String? =
        get(context).getString("session_token", null)

    /** Persist a generated session token. */
    fun putToken(context: Context, token: String) {
        get(context).edit().putString("session_token", token).apply()
    }
}
