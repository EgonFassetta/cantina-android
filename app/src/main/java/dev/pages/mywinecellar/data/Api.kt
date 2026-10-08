package dev.pages.mywinecellar.data

import dev.pages.mywinecellar.BuildConfig
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Comunicazione con il database Supabase (le stesse tabelle del sito). */
class SupabaseApi(
    private val baseUrl: String = BuildConfig.SUPABASE_URL,
    private val apiKey: String = BuildConfig.SUPABASE_KEY,
    private val http: OkHttpClient = OkHttpClient(),
) {
    /** Scarica tutto il catalogo (a pagine da 1000, come previsto dal database). */
    suspend fun fetchWines(): List<Wine> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Wine>()
        var offset = 0
        val pageSize = 1000
        var done = false
        while (!done) {
            val url = "$baseUrl/rest/v1/wines?select=*&order=cantina.asc,nome.asc&limit=$pageSize&offset=$offset"
            val request = Request.Builder()
                .url(url)
                .header("apikey", apiKey)
                .header("Accept", "application/json")
                .build()
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("Il server ha risposto con errore ${response.code}")
                }
                val dtoCount = countRows(body)
                result += parseWines(body)
                if (dtoCount < pageSize) done = true else offset += pageSize
            }
        }
        result
    }

    /** Conta le righe di una risposta JSON (serve a capire se c'è un'altra pagina). */
    private fun countRows(body: String): Int =
        kotlinx.serialization.json.Json.parseToJsonElement(body).let { el ->
            (el as? kotlinx.serialization.json.JsonArray)?.size ?: 0
        }
}
