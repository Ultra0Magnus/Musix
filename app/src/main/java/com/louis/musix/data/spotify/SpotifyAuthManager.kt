package com.louis.musix.data.spotify

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.louis.musix.data.spotify.model.SpotifyTokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom

private const val TAG = "Musix.SpotifyAuth"

private const val CLIENT_ID    = "50fa02008df5469fbdeb8407ec15ff80"
private const val REDIRECT_URI = "musix://callback"
private const val SCOPES       =
    "user-library-read playlist-read-private playlist-read-collaborative"

private const val PREF_FILE   = "spotify_prefs"
private const val KEY_ACCESS  = "access_token"
private const val KEY_REFRESH = "refresh_token"
private const val KEY_EXPIRES = "expires_at"

/**
 * Gère le flux OAuth2 PKCE de Spotify :
 *  - Génère le code_verifier / code_challenge
 *  - Ouvre le navigateur pour l'autorisation
 *  - Échange le code contre des tokens (via OkHttp)
 *  - Stocke les tokens dans SharedPreferences
 *  - Rafraîchit le token automatiquement
 */
class SpotifyAuthManager(private val context: Context) {

    private val prefs      = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    private val json       = Json { ignoreUnknownKeys = true }
    private val httpClient = OkHttpClient()

    /** code_verifier PKCE conservé jusqu'à l'échange de code. */
    private var pendingVerifier: String? = null

    /** Code d'autorisation reçu via le deep link musix://callback. */
    private val _pendingCode = MutableStateFlow<String?>(null)
    val pendingCode: StateFlow<String?> = _pendingCode.asStateFlow()

    /** Erreur OAuth reçue via le deep link (ex : access_denied). */
    private val _pendingError = MutableStateFlow<String?>(null)
    val pendingError: StateFlow<String?> = _pendingError.asStateFlow()

    // ── PKCE helpers ──────────────────────────────────────────────────────────

    private fun generateVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        )
    }

    private fun generateChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(
            digest,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        )
    }

    // ── Lancement OAuth ───────────────────────────────────────────────────────

    fun launchAuth() {
        pendingVerifier  = generateVerifier()
        val challenge    = generateChallenge(pendingVerifier!!)

        val authUrl = Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("client_id",             CLIENT_ID)
            .appendQueryParameter("response_type",         "code")
            .appendQueryParameter("redirect_uri",          REDIRECT_URI)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge",        challenge)
            .appendQueryParameter("scope",                 SCOPES)
            .build()

        Log.d(TAG, "Lancement OAuth → $authUrl")
        context.startActivity(
            Intent(Intent.ACTION_VIEW, authUrl)
                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        )
    }

    // ── Callbacks depuis MainActivity ─────────────────────────────────────────

    /** Appelé par MainActivity quand musix://callback?code=xxx est reçu. */
    fun handleCallback(code: String) {
        Log.d(TAG, "Code OAuth reçu")
        _pendingCode.value = code
    }

    /** Appelé par MainActivity quand musix://callback?error=xxx est reçu. */
    fun handleCallbackError(error: String) {
        Log.w(TAG, "Erreur OAuth : $error")
        _pendingError.value = error
    }

    fun clearPendingCode()  { _pendingCode.value  = null }
    fun clearPendingError() { _pendingError.value = null }

    // ── Échange code → tokens ─────────────────────────────────────────────────

    suspend fun exchangeCode(code: String): Boolean = withContext(Dispatchers.IO) {
        val verifier = pendingVerifier
            ?: run { Log.e(TAG, "code_verifier manquant"); return@withContext false }

        val body = FormBody.Builder()
            .add("grant_type",    "authorization_code")
            .add("code",          code)
            .add("redirect_uri",  REDIRECT_URI)
            .add("client_id",     CLIENT_ID)
            .add("code_verifier", verifier)
            .build()

        val ok = callTokenEndpoint(body)
        if (ok) pendingVerifier = null
        ok
    }

    // ── Refresh token ─────────────────────────────────────────────────────────

    suspend fun refreshAccessToken(): Boolean = withContext(Dispatchers.IO) {
        val refreshToken = prefs.getString(KEY_REFRESH, null)
            ?: return@withContext false

        val body = FormBody.Builder()
            .add("grant_type",    "refresh_token")
            .add("refresh_token", refreshToken)
            .add("client_id",     CLIENT_ID)
            .build()

        callTokenEndpoint(body)
    }

    private fun callTokenEndpoint(body: FormBody): Boolean {
        return try {
            val response = httpClient.newCall(
                Request.Builder()
                    .url("https://accounts.spotify.com/api/token")
                    .post(body)
                    .build(),
            ).execute()

            val bodyStr = response.body?.string() ?: return false
            if (!response.isSuccessful) {
                Log.e(TAG, "Token KO ${response.code}: $bodyStr")
                return false
            }
            saveTokens(json.decodeFromString<SpotifyTokenResponse>(bodyStr))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Token exception : ${e.message}")
            false
        }
    }

    // ── Token valide (avec refresh auto) ─────────────────────────────────────

    suspend fun getValidToken(): String? {
        val expiresAt   = prefs.getLong(KEY_EXPIRES, 0L)
        val accessToken = prefs.getString(KEY_ACCESS, null)
        val remaining   = (expiresAt - System.currentTimeMillis()) / 1000
        Log.d(TAG, "getValidToken : token=${if (accessToken != null) "présent" else "absent"}, expire dans ${remaining}s")
        // Marge de 60 s pour éviter une expiration en pleine requête
        if (accessToken != null && System.currentTimeMillis() < expiresAt - 60_000L) {
            return accessToken
        }
        Log.d(TAG, "Token expiré ou absent → refresh")
        return if (refreshAccessToken()) prefs.getString(KEY_ACCESS, null) else null
    }

    fun isConnected(): Boolean  = prefs.getString(KEY_REFRESH, null) != null

    fun disconnect() = prefs.edit().clear().apply()

    // ── Stockage ──────────────────────────────────────────────────────────────

    private fun saveTokens(resp: SpotifyTokenResponse) {
        val expiresAt = System.currentTimeMillis() + resp.expiresIn * 1_000L
        prefs.edit()
            .putString(KEY_ACCESS, resp.accessToken)
            .putLong(KEY_EXPIRES, expiresAt)
            .apply()
        resp.refreshToken?.let { rt ->
            prefs.edit().putString(KEY_REFRESH, rt).apply()
        }
        Log.d(TAG, "Tokens sauvegardés, expire dans ${resp.expiresIn}s")
    }
}
