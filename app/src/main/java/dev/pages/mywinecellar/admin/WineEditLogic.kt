package dev.pages.mywinecellar.admin

import dev.pages.mywinecellar.data.Wine
import dev.pages.mywinecellar.data.shelfKey
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

// Valori ammessi: gli stessi elenchi del sito.
internal val EDIT_FORMATI = listOf("0,750 lt.", "1,5 lt.", "3 lt.", "4 lt.", "5 lt.", "7 lt.", "9 lt.")
internal val EDIT_COLORI = listOf("Rosso", "Bianco", "Rosè")
internal val EDIT_CARATTERISTICHE = listOf("Fermo", "Bollicine")
internal val EDIT_PAESI = listOf("Italia", "Francia")
internal val EDIT_REGIONI = mapOf(
    "Italia" to listOf(
        "Abruzzo", "Basilicata", "Calabria", "Campania", "Emilia-Romagna",
        "Friuli-Venezia Giulia", "Lazio", "Liguria", "Lombardia", "Marche",
        "Molise", "Piemonte", "Puglia", "Sardegna", "Sicilia", "Toscana",
        "Trentino-Alto Adige", "Umbria", "Valle d'Aosta", "Veneto",
    ),
    "Francia" to listOf(
        "Bordeaux", "Borgogna", "Champagne", "Valle della Loira", "Alsazia",
        "Rodano (Côtes du Rhône)", "Linguadoca-Rossiglione", "Provenza",
        "Beaujolais", "Giura", "Savoia", "Sud-Ovest", "Corsica",
    ),
)

/** Gradazioni proposte, come sul sito: da 10 a 20 a mezzi gradi. */
internal val EDIT_GRADI: List<String> = (20..40).map { formatGradoValue(it / 2.0) }

/** Caratteristiche del vino (campo nel database, etichetta), nell'ordine del sito. */
internal val EDIT_PROFILO = listOf(
    "morbidezza" to "Morbidezza", "struttura" to "Struttura", "tannicita" to "Tannicità",
    "acidita" to "Acidità", "mineralita" to "Mineralità",
)

/** Categorie degli allegati, come sul sito. */
internal val ATTACH_CATEGORIES = listOf("Scheda Tecnica", "Valutazione Assaggio", "Altro")

/** Un vino «Fermo» può essere solo Secco o Dolce; le altre voci sono per le bollicine (regola del sito). */
internal fun tipiVinoPer(caratteristica: String): List<String> =
    if (caratteristica == "Fermo") listOf("Secco", "Dolce") else listOf("Secco", "Dolce", "Brut", "Extra Brut", "Extra Dry", "Dry")

internal fun regioniPer(paese: String): List<String> = EDIT_REGIONI[paese].orEmpty()

internal fun formatGradoValue(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else v.toString().replace('.', ',')

private fun parseDecimal(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

/** Prezzo: vuoto = nessun prezzo; altrimenti un numero non negativo. Restituisce false se non è valido. */
private fun parsePrice(s: String): Pair<Boolean, Double?> {
    if (s.isBlank()) return true to null
    val v = parseDecimal(s) ?: return false to null
    return (v >= 0) to v
}

/**
 * Posizione come la salva il sito: "Box1".."Box6" come testo, oppure scaffale.posto (es. 3.4) come numero.
 * Null se non valida.
 */
internal fun positionValue(input: String): JsonPrimitive? {
    val key = shelfKey(input) ?: return null
    return if (key.startsWith("box")) JsonPrimitive("Box" + key.removePrefix("box")) else JsonPrimitive(key.toDouble())
}

/** Posizione da mostrare nel modulo: "3.4" oppure "Box2". */
internal fun positionText(posizione: String): String {
    val key = shelfKey(posizione) ?: return posizione
    return if (key.startsWith("box")) "Box" + key.removePrefix("box") else key
}

/** Valori del modulo «Modifica scheda» (tutti come testo, tranne cursori e caselle). */
data class WineForm(
    val annata: String,
    val quantita: String,
    val posizione: String,
    val prezzo: String,
    val formato: String,
    val grado: String,
    val coloreVino: String,
    val caratteristica: String,
    val tipoVino: String,
    val paese: String,
    val regione: String,
    val sitoProduttore: String,
    val giudizio: Int?,
    val profilo: Map<String, Int?>,
    val valoreMercato: String,
    val prezzoAcquisto: String,
    val disponibileVendita: Boolean,
    val esclusoVetrina: Boolean,
    val nota: String,
)

private fun priceText(v: Double?): String = v?.let { formatGradoValue(it) }.orEmpty()

fun formFrom(w: Wine): WineForm = WineForm(
    annata = w.annata,
    quantita = w.quantita.toString(),
    posizione = positionText(w.posizione),
    prezzo = priceText(w.prezzo),
    formato = w.formato,
    grado = w.grado.orEmpty(),
    coloreVino = w.coloreVino,
    caratteristica = w.caratteristica,
    tipoVino = w.tipoVino,
    paese = w.paese,
    regione = w.regione,
    sitoProduttore = w.sitoProduttore,
    giudizio = w.giudizio,
    profilo = mapOf(
        "morbidezza" to w.morbidezza, "struttura" to w.struttura, "tannicita" to w.tannicita,
        "acidita" to w.acidita, "mineralita" to w.mineralita,
    ),
    valoreMercato = priceText(w.valoreMercato),
    prezzoAcquisto = priceText(w.prezzoAcquisto),
    disponibileVendita = w.disponibileVendita,
    esclusoVetrina = w.esclusoVetrina,
    nota = w.nota,
)

/** Cosa salvare: i campi del vino (tabella wines) e, se è cambiato, il prezzo d'acquisto (tabella wine_private). */
data class WinePatch(val fields: JsonObject, val privateChanged: Boolean, val prezzoAcquisto: Double?)

const val POSITION_ERROR = "Posizione non valida. Usa il formato scaffale.posto (es. 3.4) oppure Box1–Box6."

/** Controlla il modulo e prepara i dati da salvare, con gli stessi nomi di campo del sito. Errore = messaggio. */
fun buildWinePatch(orig: Wine, f: WineForm): Result<WinePatch> {
    fun fail(msg: String) = Result.failure<WinePatch>(IllegalArgumentException(msg))
    val annata = f.annata.trim().toIntOrNull() ?: return fail("Annata non valida.")
    val quantita = f.quantita.trim().toIntOrNull()?.takeIf { it >= 0 } ?: return fail("Numero di bottiglie non valido.")
    val pos = positionValue(f.posizione) ?: return fail(POSITION_ERROR)
    val (okPrezzo, prezzo) = parsePrice(f.prezzo)
    val (okValore, valore) = parsePrice(f.valoreMercato)
    val (okAcquisto, acquisto) = parsePrice(f.prezzoAcquisto)
    if (!okPrezzo || !okValore || !okAcquisto) return fail("Prezzo non valido: usa un numero, per esempio 12,50.")
    val grado = if (f.grado.isBlank()) null else parseDecimal(f.grado) ?: return fail("Gradazione non valida.")
    val tipoVino = f.tipoVino.takeIf { it.isNotBlank() && it in tipiVinoPer(f.caratteristica) }
    val regione = f.regione.takeIf { it.isNotBlank() && it in regioniPer(f.paese) }

    fun text(v: String?): JsonPrimitive = v?.trim()?.takeIf { it.isNotEmpty() }?.let { JsonPrimitive(it) } ?: JsonNull
    fun num(v: Double?): JsonPrimitive = v?.let { JsonPrimitive(it) } ?: JsonNull

    val fields = buildJsonObject {
        put("annata", JsonPrimitive(annata))
        put("quantita", JsonPrimitive(quantita))
        put("posizione", pos)
        put("prezzo", num(prezzo))
        put("formato", JsonPrimitive(f.formato.ifBlank { EDIT_FORMATI.first() }))
        put("grado", num(grado))
        put("colore_vino", text(f.coloreVino))
        put("effervescenza", text(f.caratteristica))
        put("dolcezza", text(tipoVino))
        put("paese", text(f.paese))
        put("regione", text(regione))
        put("sito_produttore", text(f.sitoProduttore))
        // I cursori si salvano solo se hanno un valore (il sito scriverebbe 50 anche dove non è mai stato indicato).
        f.giudizio?.let { put("giudizio_complessivo", JsonPrimitive(it.coerceIn(0, 100))) }
        EDIT_PROFILO.forEach { (field, _) -> f.profilo[field]?.let { put(field, JsonPrimitive(it.coerceIn(0, 100))) } }
        put("valore mercato", num(valore))
        put("disponibile_vendita", JsonPrimitive(f.disponibileVendita))
        if (f.esclusoVetrina != orig.esclusoVetrina) put("escluso_vetrina", JsonPrimitive(f.esclusoVetrina))
        put("nota", JsonPrimitive(f.nota))
    }
    return Result.success(WinePatch(fields, privateChanged = acquisto != orig.prezzoAcquisto, prezzoAcquisto = acquisto))
}

/** Percorso nello storage, come il sito: la cantina principale nella radice, le altre in una cartella con il loro nome. */
fun storagePath(tenant: String, path: String): String =
    if (tenant == dev.pages.mywinecellar.data.DEFAULT_TENANT) path else "$tenant/$path"

/** Nome di file sicuro per lo storage (come sul sito). */
fun safeFileName(name: String): String = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
