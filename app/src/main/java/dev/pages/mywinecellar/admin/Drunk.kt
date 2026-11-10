package dev.pages.mywinecellar.admin

import dev.pages.mywinecellar.data.SupabaseApi
import dev.pages.mywinecellar.data.Wine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Avviso prima di segnare come bevuta (stesso testo del sito). */
fun drinkWarningText(quantita: Int): String =
    if (quantita > 0) "Proseguendo, la quantità verrà ridotta di 1 unità (da $quantita a ${quantita - 1})."
    else "La quantità è già 0 e resterà invariata."

/** Avviso prima di ripristinare (stesso testo del sito). */
fun restoreWarningText(quantita: Int): String =
    "Proseguendo, la quantità verrà aumentata di 1 unità (da $quantita a ${quantita + 1})."

/** Data di oggi in formato italiano gg/mm/aaaa. */
fun todayIt(): String = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(java.util.Date())

/** «gg/mm/aaaa» (anche con - o .) → «aaaa-mm-gg»; null se la data non esiste. */
fun parseDateIt(s: String): String? {
    val m = Regex("^\\s*(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})\\s*$").find(s) ?: return null
    val (d, mo, y) = m.destructured.toList().map { it.toInt() }
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        isLenient = false
        clear()
        set(y, mo - 1, d)
    }
    return runCatching { cal.timeInMillis; "%04d-%02d-%02d".format(Locale.ROOT, y, mo, d) }.getOrNull()
}

/** Giorni tra due date «aaaa-mm-gg» (null se una manca o non è valida), come il sito. */
fun daysBetween(a: String?, b: String?): Int? {
    if (a == null || b == null) return null
    val f = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }
    val x = runCatching { f.parse(a)?.time }.getOrNull() ?: return null
    val y = runCatching { f.parse(b)?.time }.getOrNull() ?: return null
    return ((y - x) / 86_400_000L).toInt()
}

/** Testo dello stato di una bottiglia bevuta, come sul sito. */
fun drunkText(w: Wine): String {
    val days = daysBetween(w.dataApertura, w.dataFine)
    val a = w.dataApertura?.let { dateIt(it) } ?: "—"
    val b = w.dataFine?.let { dateIt(it) } ?: "—"
    return "Aperta il $a, finita il $b" + (days?.let { " — $it giorn${if (it == 1) "o" else "i"}" } ?: "") + "."
}

/**
 * «Segna come bevuta»: la quantità scende di 1 (se è maggiore di 0) e si salvano date e stato.
 * Se lo stato non si salva, la quantità torna com'era (come il sito).
 */
suspend fun SupabaseApi.markDrunk(w: Wine, apertura: String, fine: String, preferito: Boolean) {
    val newQty = (w.quantita - 1).coerceAtLeast(0)
    if (newQty != w.quantita) updateWine(w.id, buildJsonObject { put("quantita", newQty) })
    try {
        saveDrunkState(w.id, buildJsonObject { put("bevuto", true); put("data_apertura", apertura); put("data_fine", fine) }, preferito)
    } catch (e: Exception) {
        if (newQty != w.quantita) runCatching { updateWine(w.id, buildJsonObject { put("quantita", w.quantita) }) }
        throw e
    }
}

/** «Ripristina come disponibile»: la quantità sale di 1 e le date si cancellano (come il sito). */
suspend fun SupabaseApi.restoreDrunk(w: Wine, preferito: Boolean) {
    updateWine(w.id, buildJsonObject { put("quantita", w.quantita + 1) })
    try {
        saveDrunkState(w.id, buildJsonObject { put("bevuto", false); put("data_apertura", JsonNull); put("data_fine", JsonNull) }, preferito)
    } catch (e: Exception) {
        runCatching { updateWine(w.id, buildJsonObject { put("quantita", w.quantita) }) }
        throw e
    }
}

/** Giudizio del bevitore (solo per le bottiglie bevute). */
suspend fun SupabaseApi.saveDrinkerNote(w: Wine, giudizio: String, preferito: Boolean) =
    saveDrunkState(w.id, buildJsonObject { put("giudizio_bevitore", giudizio) }, preferito)
