package dev.pages.mywinecellar.data

import dev.pages.mywinecellar.BuildConfig
import java.io.IOException
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Errore "leggibile" da mostrare all'utente. */
class ApiException(message: String, val status: Int = 0) : IOException(message)

private data class Resp(val code: Int, val body: String)

/** Comunicazione con Supabase: le stesse tabelle e funzioni del sito web. */
class SupabaseApi(
    private val store: SessionStore,
    private val baseUrl: String = BuildConfig.SUPABASE_URL,
    private val apiKey: String = BuildConfig.SUPABASE_KEY,
    private val http: OkHttpClient = OkHttpClient(),
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val mutex = Mutex()

    @Volatile
    var session: AuthSession? = store.load()
        private set

    // ---------------------------------------------------------------- catalogo (pubblico)

    /** Scarica tutto il catalogo (a pagine da 1000, come previsto dal database). */
    suspend fun fetchWines(): List<Wine> = parseWines(fetchWinesJson())

    /** Il catalogo in formato JSON grezzo (serve anche per salvarlo nella cache). */
    suspend fun fetchWinesJson(): String = withContext(Dispatchers.IO) {
        val result = mutableListOf<kotlinx.serialization.json.JsonElement>()
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
                    throw ApiException("Il server ha risposto con errore ${response.code}", response.code)
                }
                val rows = (json.parseToJsonElement(body) as? JsonArray) ?: JsonArray(emptyList())
                result += rows
                if (rows.size < pageSize) done = true else offset += pageSize
            }
        }
        JsonArray(result).toString()
    }

    // ---------------------------------------------------------------- sessione

    /** All'apertura dell'app: rinnova la sessione salvata, oppure ne crea una anonima. */
    suspend fun startSession(): AuthSession = mutex.withLock {
        val current = session
        if (current == null) {
            createAnonymousLocked()
        } else {
            try {
                refreshLocked(current)
            } catch (e: ApiException) {
                if (e.status in 400..499) createAnonymousLocked() else throw e
            } catch (e: IOException) {
                current // senza rete si continua con la sessione salvata
            }
        }
    }

    private suspend fun currentSession(): AuthSession = session ?: startSession()

    suspend fun refreshSession(): AuthSession = mutex.withLock {
        val current = session ?: return@withLock createAnonymousLocked()
        try {
            refreshLocked(current)
        } catch (e: ApiException) {
            if (e.status in 400..499) createAnonymousLocked() else throw e
        }
    }

    private suspend fun refreshLocked(current: AuthSession): AuthSession {
        val body = buildJsonObject { put("refresh_token", current.refreshToken) }.toString()
        val r = authPost("/auth/v1/token?grant_type=refresh_token", body, null)
        if (r.code !in 200..299) throw ApiException(errorMessage(r.body, "Sessione scaduta."), r.code)
        return saveSession(json.decodeFromString<AuthSession>(r.body))
    }

    private suspend fun createAnonymousLocked(): AuthSession {
        val r = authPost("/auth/v1/signup", "{}", null)
        if (r.code !in 200..299) throw ApiException(errorMessage(r.body, "Impossibile creare la sessione."), r.code)
        return saveSession(json.decodeFromString<AuthSession>(r.body))
    }

    private fun saveSession(s: AuthSession): AuthSession {
        session = s
        store.save(s)
        return s
    }

    private fun clearSession() {
        session = null
        store.save(null)
    }

    private suspend fun authPost(path: String, body: String, bearer: String?): Resp =
        withContext(Dispatchers.IO) {
            val b = Request.Builder()
                .url("$baseUrl$path")
                .header("apikey", apiKey)
                .header("Accept", "application/json")
                .post(body.toRequestBody(jsonType))
            if (bearer != null) b.header("Authorization", "Bearer $bearer")
            http.newCall(b.build()).execute().use { Resp(it.code, it.body?.string().orEmpty()) }
        }

    /** Invia una richiesta autenticata; se il token è scaduto lo rinnova e riprova una volta. */
    private suspend fun send(build: (String) -> Request): Resp {
        var retried = false
        while (true) {
            val token = currentSession().accessToken
            val r = withContext(Dispatchers.IO) {
                http.newCall(build(token)).execute().use { Resp(it.code, it.body?.string().orEmpty()) }
            }
            if (r.code == 401 && !retried) {
                retried = true
                refreshSession()
                continue
            }
            return r
        }
    }

    private suspend fun rest(method: String, path: String, bodyJson: String? = null, prefer: String? = null): Resp =
        send { token ->
            val b = Request.Builder()
                .url("$baseUrl/rest/v1/$path")
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
            if (prefer != null) b.header("Prefer", prefer)
            val rb = (bodyJson ?: "{}").toRequestBody(jsonType)
            when (method) {
                "POST" -> b.post(rb)
                "PATCH" -> b.patch(rb)
                "DELETE" -> b.delete()
                else -> b.get()
            }
            b.build()
        }

    private fun requireOk(r: Resp, fallback: String) {
        if (r.code !in 200..299) throw ApiException(errorMessage(r.body, fallback), r.code)
    }

    private fun enc(v: String): String = URLEncoder.encode(v, "UTF-8")

    private suspend fun userId(): String = currentSession().user?.id
        ?: throw ApiException("Sessione non disponibile.")

    // ---------------------------------------------------------------- preferiti

    suspend fun fetchFavorites(): Set<String> {
        val uid = userId()
        val r = rest("GET", "wine_user_state?select=wine_id&preferito=eq.true&user_id=eq.${enc(uid)}")
        requireOk(r, "Impossibile leggere i preferiti.")
        return json.parseToJsonElement(r.body).jsonArray
            .mapNotNull { it.jsonObject["wine_id"]?.jsonPrimitive?.contentOrNull }
            .toSet()
    }

    suspend fun setFavorite(wineId: String, value: Boolean) {
        val uid = userId()
        val patch = rest(
            "PATCH",
            "wine_user_state?user_id=eq.${enc(uid)}&wine_id=eq.${enc(wineId)}",
            """{"preferito":$value}""",
            "return=representation",
        )
        requireOk(patch, "Impossibile aggiornare i preferiti.")
        val updated = json.parseToJsonElement(patch.body).jsonArray.size
        if (updated == 0) {
            val row = buildJsonObject {
                put("user_id", uid)
                put("wine_id", wineId)
                put("preferito", value)
                put("bevuto", false)
                put("data_apertura", JsonNull)
                put("data_fine", JsonNull)
                put("giudizio_bevitore", "")
            }
            val ins = rest(
                "POST", "wine_user_state?on_conflict=user_id,wine_id", row.toString(),
                "resolution=merge-duplicates,return=minimal",
            )
            requireOk(ins, "Impossibile aggiornare i preferiti.")
        }
    }

    // ---------------------------------------------------------------- carrello

    suspend fun fetchCart(): Map<String, Int> {
        val uid = userId()
        val r = rest("GET", "wine_user_cart?select=wine_id,quantity&user_id=eq.${enc(uid)}")
        requireOk(r, "Impossibile leggere il carrello.")
        val out = linkedMapOf<String, Int>()
        json.parseToJsonElement(r.body).jsonArray.forEach { el ->
            val o = el.jsonObject
            val id = o["wine_id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val q = o["quantity"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()?.toInt() ?: 0
            if (q > 0) out[id] = q
        }
        return out
    }

    suspend fun setCartQty(wineId: String, qty: Int) {
        val uid = userId()
        if (qty <= 0) {
            val r = rest("DELETE", "wine_user_cart?user_id=eq.${enc(uid)}&wine_id=eq.${enc(wineId)}")
            requireOk(r, "Impossibile aggiornare il carrello.")
        } else {
            val row = buildJsonObject {
                put("user_id", uid)
                put("wine_id", wineId)
                put("quantity", qty)
            }
            val r = rest(
                "POST", "wine_user_cart?on_conflict=user_id,wine_id", row.toString(),
                "resolution=merge-duplicates,return=minimal",
            )
            requireOk(r, "Impossibile aggiornare il carrello.")
        }
    }

    suspend fun clearCart() {
        val uid = userId()
        val r = rest("DELETE", "wine_user_cart?user_id=eq.${enc(uid)}")
        requireOk(r, "Impossibile svuotare il carrello.")
    }

    // ---------------------------------------------------------------- funzioni del server

    private suspend fun callFunction(name: String, body: JsonObject, fallback: String): JsonObject {
        val r = send { token ->
            Request.Builder()
                .url("$baseUrl/functions/v1/$name")
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .post(body.toString().toRequestBody(jsonType))
                .build()
        }
        val obj = runCatching { json.parseToJsonElement(r.body).jsonObject }.getOrNull()
        val ok = obj?.get("ok")?.jsonPrimitive?.booleanOrNull == true
        if (r.code in 200..299 && ok) return obj!!
        val msg = obj?.get("error")?.jsonPrimitive?.contentOrNull
            ?: obj?.get("message")?.jsonPrimitive?.contentOrNull
        throw ApiException(msg ?: fallback, r.code)
    }

    /** Richiesta di disponibilità. */
    suspend fun requestAvailability(wineId: String, email: String, phone: String, note: String) {
        callFunction(
            "submit-request",
            buildJsonObject {
                put("type", "availability")
                put("wine_id", wineId)
                put("email", email)
                put("phone", phone)
                put("note", note)
            },
            "Invio non riuscito.",
        )
    }

    /** Richiesta dal carrello: il server ricalcola nomi e prezzi, noi indichiamo solo quali vini e quanti. */
    suspend fun requestCart(email: String, phone: String, items: Map<String, Int>) {
        val arr = kotlinx.serialization.json.buildJsonArray {
            items.forEach { (id, q) ->
                add(buildJsonObject { put("wine_id", id); put("quantity", q) })
            }
        }
        callFunction(
            "submit-request",
            buildJsonObject {
                put("type", "cart")
                put("email", email)
                put("phone", phone)
                put("items", arr)
            },
            "Invio non riuscito.",
        )
    }

    /** Messaggio all'assistenza. */
    suspend fun sendSupport(email: String, phone: String, message: String) {
        callFunction(
            "submit-request",
            buildJsonObject {
                put("type", "support")
                put("email", email)
                put("phone", phone)
                put("message", message)
            },
            "Invio non riuscito.",
        )
    }

    // ---------------------------------------------------------------- account con email

    /** Collega l'email alla sessione ospite ("Salva la mia cantina"). */
    suspend fun linkEmail(email: String) {
        try {
            callFunction(
                "customer-auth",
                buildJsonObject { put("action", "link"); put("email", email) },
                "Impossibile salvare la cantina.",
            )
        } catch (e: ApiException) {
            val m = e.message.orEmpty()
            val duplicate = e.status == 409 || e.status == 422 ||
                Regex("already|registered|esist|duplic|in uso|exists|error updating user", RegexOption.IGNORE_CASE)
                    .containsMatchIn(m)
            if (duplicate) {
                throw ApiException(
                    "Questa email non può essere usata: probabilmente è già registrata. " +
                        "Premi «Hai già un account? Accedi» oppure inserisci un'altra email.",
                    e.status,
                )
            }
            throw e
        }
        refreshSession() // ora il token contiene l'email
    }

    /** Accesso con una email già registrata. */
    suspend fun loginEmail(email: String) {
        val obj = callFunction(
            "customer-auth",
            buildJsonObject { put("action", "login"); put("email", email) },
            "Accesso non riuscito.",
        )
        val raw = obj["session"]?.jsonObject ?: throw ApiException("Accesso non riuscito.")
        val s = json.decodeFromJsonElement<AuthSession>(raw)
        mutex.withLock { saveSession(s) }
    }

    /** "Esci da questo dispositivo": chiude la sessione e ne apre una nuova, vuota, da ospite. */
    suspend fun signOutDevice() {
        val token = session?.accessToken
        if (token != null) {
            runCatching { authPost("/auth/v1/logout", "{}", token) }
        }
        mutex.withLock {
            clearSession()
            createAnonymousLocked()
        }
    }

    /** Elimina l'account e i dati collegati (funzione "delete-account" sul server). */
    suspend fun deleteAccount() {
        try {
            callFunction("delete-account", buildJsonObject { put("confirm", true) }, "Eliminazione non riuscita.")
        } catch (e: ApiException) {
            if (e.status == 404) throw ApiException("FUNZIONE_NON_DISPONIBILE", 404)
            throw e
        }
        mutex.withLock {
            clearSession()
            createAnonymousLocked()
        }
    }

    // ---------------------------------------------------------------- utilità

    private fun errorMessage(body: String, fallback: String): String {
        val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return fallback
        for (k in listOf("error_description", "msg", "message", "error")) {
            val v = obj[k]?.jsonPrimitive?.contentOrNull
            if (!v.isNullOrBlank()) return v
        }
        return fallback
    }
}
