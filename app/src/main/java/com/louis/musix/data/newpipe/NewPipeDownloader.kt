package com.louis.musix.data.newpipe

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.util.concurrent.TimeUnit

/**
 * Implémentation du Downloader requis par NewPipeExtractor.
 *
 * Points critiques :
 * 1. CookieJar — YouTube pose des cookies à la 1ère requête (recherche) et
 *    exige de les retrouver dans les suivantes. Sans CookieJar, les cookies sont
 *    perdus entre appels et YouTube répond "The page needs to be reloaded".
 *
 * 2. Priorité aux headers de NewPipeExtractor — pour les requêtes InnerTube
 *    (POST player), NewPipeExtractor envoie ses propres headers (User-Agent
 *    YouTube Android, X-Youtube-Client-Name, etc.). Ces headers doivent
 *    REMPLACER nos valeurs par défaut, pas s'y ajouter.
 *    Si on ajoute Firefox UA avant l'UA Android, YouTube voit les deux et
 *    traite la requête comme un navigateur → 0 streams retournés.
 */
object NewPipeDownloader : Downloader() {

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; rv:135.0) Gecko/20100101 Firefox/135.0"

    // CookieJar en mémoire : conserve les cookies YouTube entre les requêtes
    private val cookieJar = object : CookieJar {
        private val store = mutableMapOf<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            store.getOrPut(url.host) { mutableListOf() }.apply {
                cookies.forEach { new ->
                    removeIf { old -> old.name == new.name }
                    add(new)
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            store[url.host] ?: emptyList()
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val requestBuilder = okhttp3.Request.Builder()
            .url(request.url())

        // ── Étape 1 : poser nos valeurs par défaut ──────────────────────────────
        // Ces valeurs servent pour les requêtes GET (page YouTube, player JS…)
        // où NewPipeExtractor ne fournit pas ses propres headers.
        requestBuilder.header("User-Agent", USER_AGENT)          // .header() = set/replace
        requestBuilder.header("Accept-Language", "en-US,en;q=0.9")

        // ── Étape 2 : laisser NewPipeExtractor écraser nos valeurs par défaut ──
        // Pour les requêtes InnerTube POST (extraction stream), NewPipeExtractor
        // fournit ses propres headers (User-Agent YouTube Android, X-Goog-*, etc.)
        // → .header() remplace la valeur existante (User-Agent Firefox → UA YouTube)
        for ((name, values) in request.headers()) {
            val primary = values.firstOrNull() ?: continue
            requestBuilder.header(name, primary)        // remplace si déjà présent
            values.drop(1).forEach { extra ->
                requestBuilder.addHeader(name, extra)   // valeurs supplémentaires
            }
        }

        // ── Corps de la requête (POST InnerTube, etc.) ──────────────────────────
        val body = request.dataToSend()
        if (body != null) {
            requestBuilder.method(
                request.httpMethod(),
                body.toRequestBody("application/json; charset=UTF-8".toMediaType())
            )
        } else {
            requestBuilder.method(request.httpMethod(), null)
        }

        val response = client.newCall(requestBuilder.build()).execute()

        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            response.body?.string(),
            response.request.url.toString()
        )
    }
}
