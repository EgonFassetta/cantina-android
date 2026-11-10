package dev.pages.mywinecellar.admin

import dev.pages.mywinecellar.data.ApiException
import dev.pages.mywinecellar.data.SupabaseApi
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Data e ora attuali in formato ISO (UTC), come new Date().toISOString() del sito. Funziona anche su Android 7. */
internal fun isoNow(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(java.util.Date())

/** Numero di righe restituite (con «return=representation»): 0 = il database ha rifiutato la modifica. */
private fun SupabaseApi.rowsIn(body: String): Int =
    runCatching { (json.parseToJsonElement(body) as? JsonArray)?.size ?: 0 }.getOrDefault(0)

/**
 * Salva i campi del vino (tabella wines), come il pulsante «Salva» della scheda sul sito.
 * Con le regole di sicurezza una modifica rifiutata non dà errore ma aggiorna 0 righe: in quel caso si segnala.
 */
suspend fun SupabaseApi.updateWine(id: String, fields: JsonObject) {
    val r = rest("PATCH", "wines?id=eq.${enc(id)}", fields.toString(), "return=representation")
    requireOk(r, "Salvataggio non riuscito.")
    if (rowsIn(r.body) == 0) {
        throw ApiException("Il database ha rifiutato la modifica (permessi insufficienti o vino non trovato). Rientra nell'area Admin.")
    }
}

/** Prezzo d'acquisto nella tabella privata wine_private (visibile solo all'Admin), come il sito. */
suspend fun SupabaseApi.saveWinePrivate(id: String, prezzoAcquisto: Double?) {
    val body = buildJsonObject {
        put("wine_id", id)
        put("prezzo_acquisto", prezzoAcquisto?.let { JsonPrimitive(it) } ?: JsonNull)
        put("updated_at", isoNow())
    }
    val r = rest("POST", "wine_private?on_conflict=wine_id", body.toString(), "resolution=merge-duplicates,return=representation")
    requireOk(r, "Salvataggio del prezzo d'acquisto non riuscito.")
    if (rowsIn(r.body) == 0) throw ApiException("Il database ha rifiutato il prezzo d'acquisto (permessi insufficienti).")
}

/** Carica un file in un contenitore pubblico dello storage e ne restituisce l'indirizzo pubblico. */
suspend fun SupabaseApi.uploadPublic(bucket: String, path: String, bytes: ByteArray, contentType: String): String {
    val up = send { token ->
        Request.Builder()
            .url("$baseUrl/storage/v1/object/$bucket/$path")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $token")
            .header("x-upsert", "true")
            .post(bytes.toRequestBody(contentType.toMediaType()))
            .build()
    }
    if (up.code !in 200..299) throw ApiException(errorMessage(up.body, "Caricamento del file non riuscito."), up.code)
    return "$baseUrl/storage/v1/object/public/$bucket/$path"
}

/** Cambia la foto della bottiglia: contenitore «etichette», poi collegamento al vino (come il sito). */
suspend fun SupabaseApi.changeWinePhoto(tenant: String, id: String, jpeg: ByteArray): String {
    val url = uploadPublic("etichette", storagePath(tenant, "$id-${System.currentTimeMillis()}.jpg"), jpeg, "image/jpeg")
    updateWine(id, buildJsonObject { put("immagine_url", url) })
    return url
}

/** Carica un allegato PDF nel contenitore «allegati» e lo registra nella tabella wine_attachments (come il sito). */
suspend fun SupabaseApi.uploadAttachment(tenant: String, wineId: String, fileName: String, pdf: ByteArray, categoria: String) {
    val rnd = UUID.randomUUID().toString().take(5)
    val path = storagePath(tenant, "$wineId/${System.currentTimeMillis()}-$rnd-${safeFileName(fileName)}")
    val url = uploadPublic("allegati", path, pdf, "application/pdf")
    val row = buildJsonObject {
        put("id", UUID.randomUUID().toString())
        put("wine_id", wineId)
        put("categoria", categoria)
        put("filename", fileName)
        put("url", url)
        put("uploaded_at", isoNow())
    }
    val r = rest("POST", "wine_attachments", row.toString(), "return=representation")
    requireOk(r, "Registrazione dell'allegato non riuscita.")
    if (rowsIn(r.body) == 0) throw ApiException("Il database ha rifiutato l'allegato (permessi insufficienti).")
}

/** Elimina un allegato dall'elenco del vino (come il sito, che non cancella il file dallo storage). */
suspend fun SupabaseApi.deleteAttachment(attachmentId: String) {
    val r = rest("DELETE", "wine_attachments?id=eq.${enc(attachmentId)}", null, "return=representation")
    requireOk(r, "Eliminazione dell'allegato non riuscita.")
    if (rowsIn(r.body) == 0) throw ApiException("Il database ha rifiutato l'eliminazione (permessi insufficienti).")
}
