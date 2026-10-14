package dev.pages.mywinecellar.admin

import dev.pages.mywinecellar.data.ApiException
import dev.pages.mywinecellar.data.AuthSession
import dev.pages.mywinecellar.data.SupabaseApi
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Campi di un vino letti dall'etichetta (modificabili prima di salvare). */
data class LabelFields(
    val cantina: String = "",
    val nome: String = "",
    val tipo: String = "",
    val annata: String = "",
    val grado: String = "",
    val formato: String = "",
    val regione: String = "",
    val paese: String = "",
    val coloreVino: String = "",
    val effervescenza: String = "",
    val dolcezza: String = "",
    val sitoProduttore: String = "",
)

/** Un vino già in cantina che somiglia a quello fotografato. */
data class SimilarWine(
    val id: String,
    val cantina: String,
    val nome: String,
    val annata: String,
    val quantita: Int,
    val posizione: String,
    val stessaAnnata: Boolean,
)

data class LabelReading(
    val campi: LabelFields,
    val leggibilita: String,
    val note: String,
    val simili: List<SimilarWine>,
)

data class LabelSaveResult(
    val ok: Boolean,
    val duplicato: Boolean,
    val message: String?,
    val id: String?,
    val quantita: Int,
    val aggiunta: Int,
)

private fun JsonObject.text(key: String): String =
    this[key]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonPrimitive?.contentOrNull?.trim().orEmpty()

private fun JsonObject.number(key: String): Int =
    this[key]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()?.toInt() ?: 0

internal fun parseLabelReading(obj: JsonObject): LabelReading {
    val c = obj["campi"] as? JsonObject ?: JsonObject(emptyMap())
    val fields = LabelFields(
        cantina = c.text("cantina"), nome = c.text("nome"), tipo = c.text("tipo"), annata = c.text("annata"),
        grado = c.text("grado"), formato = c.text("formato"), regione = c.text("regione"), paese = c.text("paese"),
        coloreVino = c.text("colore_vino"), effervescenza = c.text("effervescenza"),
        dolcezza = c.text("dolcezza"), sitoProduttore = c.text("sito_produttore"),
    )
    val simili = (obj["simili"] as? JsonArray ?: JsonArray(emptyList())).mapNotNull { el ->
        val o = el as? JsonObject ?: return@mapNotNull null
        val id = o.text("id")
        if (id.isEmpty()) null else SimilarWine(
            id = id, cantina = o.text("cantina"), nome = o.text("nome"), annata = o.text("annata"),
            quantita = o.number("quantita"), posizione = o.text("posizione"),
            stessaAnnata = o.text("corrispondenza") == "stessa-annata",
        )
    }
    return LabelReading(fields, obj.text("leggibilita"), obj.text("note"), simili)
}

internal fun labelFieldsToJson(f: LabelFields): JsonObject = buildJsonObject {
    put("cantina", f.cantina); put("nome", f.nome); put("tipo", f.tipo); put("annata", f.annata)
    put("grado", f.grado); put("formato", f.formato); put("regione", f.regione); put("paese", f.paese)
    put("colore_vino", f.coloreVino); put("effervescenza", f.effervescenza)
    put("dolcezza", f.dolcezza); put("sito_produttore", f.sitoProduttore)
}

/** Accesso amministratore con la sola password (come sul sito): il server capisce da solo chi è. Restituisce il ruolo. */
suspend fun SupabaseApi.adminLogin(password: String): String {
    val body = buildJsonObject { put("password", password) }
    val r = send { token ->
        Request.Builder()
            .url("$baseUrl/functions/v1/admin-login")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .post(body.toString().toRequestBody(jsonType))
            .build()
    }
    val obj = runCatching { json.parseToJsonElement(r.body).jsonObject }.getOrNull()
    val ok = obj?.get("ok")?.jsonPrimitive?.booleanOrNull == true
    if (r.code !in 200..299 || !ok) {
        throw ApiException(obj?.text("error")?.ifEmpty { null } ?: "Password errata.", r.code)
    }
    val s = obj!!["session"]?.jsonObject ?: throw ApiException("Accesso non riuscito.")
    val access = s.text("access_token")
    val refresh = s.text("refresh_token")
    if (access.isEmpty() || refresh.isEmpty()) throw ApiException("Accesso non riuscito.")
    mutex.withLock { saveSession(AuthSession(access, refresh, null)) }
    refreshSession() // completa la sessione con i dati dell'utente
    return obj.text("role").ifEmpty { "admin" }
}

/** Legge la foto (o le due foto) dell'etichetta con la funzione "label-reader" del sito. */
suspend fun SupabaseApi.labelRead(images: List<String>): LabelReading {
    val body = buildJsonObject {
        put("action", "read")
        put("images", buildJsonArray { images.forEach { add(JsonPrimitive(it)) } })
    }
    return parseLabelReading(callFunction("label-reader", body, "Non sono riuscito a leggere la foto."))
}

/** Salva il vino: modo "nuova" (nuova riga) oppure "aggiungi" (più bottiglie a una riga esistente). */
suspend fun SupabaseApi.labelSave(
    modo: String,
    campi: LabelFields,
    quantita: Int,
    posizione: String,
    wineId: String?,
    forza: Boolean,
): LabelSaveResult {
    val body = buildJsonObject {
        put("action", "save")
        put("modo", modo)
        put("campi", labelFieldsToJson(campi))
        put("quantita", quantita)
        put("posizione", posizione)
        if (modo == "aggiungi" && wineId != null) put("wineId", wineId)
        if (forza) put("forza", true)
    }
    val r = send { token ->
        Request.Builder()
            .url("$baseUrl/functions/v1/label-reader")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .post(body.toString().toRequestBody(jsonType))
            .build()
    }
    val obj = runCatching { json.parseToJsonElement(r.body).jsonObject }.getOrNull()
    val ok = obj?.get("ok")?.jsonPrimitive?.booleanOrNull == true
    val message = obj?.text("message")?.ifEmpty { null } ?: obj?.text("error")?.ifEmpty { null }
    if (obj?.get("duplicato")?.jsonPrimitive?.booleanOrNull == true && !ok) {
        return LabelSaveResult(false, true, message, null, 0, 0)
    }
    if (r.code !in 200..299 || !ok) throw ApiException(message ?: "Salvataggio non riuscito.", r.code)
    return LabelSaveResult(true, false, message, obj!!.text("id"), obj.number("quantita"), obj.number("aggiunta"))
}

/** True se il vino ha già una foto. */
suspend fun SupabaseApi.wineHasImage(id: String): Boolean {
    val r = rest("GET", "wines?select=immagine_url&id=eq.${enc(id)}")
    if (r.code !in 200..299) return false
    val first = runCatching { json.parseToJsonElement(r.body).jsonArray.firstOrNull() as? JsonObject }.getOrNull()
    return !first?.text("immagine_url").isNullOrEmpty()
}

/** Carica la foto nel contenitore pubblico "etichette" e la collega al vino. */
suspend fun SupabaseApi.attachWinePhoto(id: String, bytes: ByteArray) {
    val path = "$id-${System.currentTimeMillis()}.jpg"
    val up = send { token ->
        Request.Builder()
            .url("$baseUrl/storage/v1/object/etichette/$path")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $token")
            .header("x-upsert", "true")
            .post(bytes.toRequestBody("image/jpeg".toMediaType()))
            .build()
    }
    if (up.code !in 200..299) throw ApiException(errorMessage(up.body, "Caricamento della foto non riuscito."), up.code)
    val url = "$baseUrl/storage/v1/object/public/etichette/$path"
    val patch = rest(
        "PATCH", "wines?id=eq.${enc(id)}",
        buildJsonObject { put("immagine_url", url) }.toString(),
        "return=representation",
    )
    requireOk(patch, "Collegamento della foto al vino non riuscito.")
    val updated = runCatching { json.parseToJsonElement(patch.body).jsonArray.size }.getOrDefault(0)
    if (updated == 0) throw ApiException("Il database ha rifiutato la modifica (permessi insufficienti).")
}
