package dev.pages.mywinecellar.data

import java.util.Locale
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/** Il database non è uniforme: numeri, testi e booleani arrivano in forme diverse. Li leggiamo sempre come testo. */
object FlexString : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        return when (element) {
            is JsonNull -> null
            is JsonPrimitive -> element.content
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) encoder.encodeNull() else encoder.encodeString(value)
    }
}

/** Riga della tabella "wines" così come arriva da Supabase. */
@Serializable
data class WineDto(
    @Serializable(with = FlexString::class) val id: String? = null,
    @Serializable(with = FlexString::class) val cantina: String? = null,
    @Serializable(with = FlexString::class) val tipo: String? = null,
    @Serializable(with = FlexString::class) val nome: String? = null,
    @Serializable(with = FlexString::class) val annata: String? = null,
    @Serializable(with = FlexString::class) val quantita: String? = null,
    @Serializable(with = FlexString::class) val formato: String? = null,
    @Serializable(with = FlexString::class) val grado: String? = null,
    @Serializable(with = FlexString::class) val prezzo: String? = null,
    @SerialName("valore mercato") @Serializable(with = FlexString::class) val valoreMercato: String? = null,
    @SerialName("immagine_url") @Serializable(with = FlexString::class) val immagineUrl: String? = null,
    @SerialName("colore_vino") @Serializable(with = FlexString::class) val coloreVino: String? = null,
    @Serializable(with = FlexString::class) val effervescenza: String? = null,
    @Serializable(with = FlexString::class) val dolcezza: String? = null,
    @Serializable(with = FlexString::class) val paese: String? = null,
    @Serializable(with = FlexString::class) val regione: String? = null,
    @SerialName("sito_produttore") @Serializable(with = FlexString::class) val sitoProduttore: String? = null,
    @SerialName("disponibile_vendita") @Serializable(with = FlexString::class) val disponibileVendita: String? = null,
    @SerialName("giudizio_complessivo") @Serializable(with = FlexString::class) val giudizioComplessivo: String? = null,
    @Serializable(with = FlexString::class) val morbidezza: String? = null,
    @Serializable(with = FlexString::class) val struttura: String? = null,
    @Serializable(with = FlexString::class) val tannicita: String? = null,
    @Serializable(with = FlexString::class) val acidita: String? = null,
    @Serializable(with = FlexString::class) val mineralita: String? = null,
)

/** Vino pronto per essere mostrato nell'app. */
data class Wine(
    val id: String,
    val cantina: String,
    val nome: String,
    val tipologia: String,
    val annata: String,
    val quantita: Int,
    val formato: String,
    val grado: String?,
    val prezzo: Double?,
    val valoreMercato: Double?,
    val immagineUrl: String?,
    val coloreVino: String,
    val caratteristica: String,
    val tipoVino: String,
    val paese: String,
    val regione: String,
    val sitoProduttore: String,
    val disponibileVendita: Boolean,
    /** Giudizio complessivo e profilo del vino, tutti su una scala da 0 a 100 (null = non indicato). */
    val giudizio: Int? = null,
    val morbidezza: Int? = null,
    val struttura: Int? = null,
    val tannicita: Int? = null,
    val acidita: Int? = null,
    val mineralita: Int? = null,
) {
    /** Le caratteristiche indicate, nell'ordine in cui le mostra il sito. */
    val profilo: List<Pair<String, Int>>
        get() = listOfNotNull(
            morbidezza?.let { "Morbidezza" to it },
            struttura?.let { "Struttura" to it },
            tannicita?.let { "Tannicità" to it },
            acidita?.let { "Acidità" to it },
            mineralita?.let { "Mineralità" to it },
        )

    /** Titolo mostrato: il nome, oppure la tipologia se il nome manca. */
    val titolo: String get() = nome.ifBlank { tipologia }.ifBlank { "Vino senza nome" }

    /** Disponibile alla vendita = flag attivo e almeno una bottiglia. */
    val inVendita: Boolean get() = disponibileVendita && quantita > 0

    val prezzoMostrato: Double? get() = valoreMercato ?: prezzo

    val testoRicerca: String by lazy {
        listOf(cantina, nome, tipologia, annata, regione, paese, coloreVino, caratteristica, tipoVino)
            .joinToString(" ")
            .lowercase(Locale.ITALY)
    }
}

private fun String?.clean(): String = this?.trim().orEmpty()

private fun String?.toDoubleLoose(): Double? = this?.trim()?.replace(',', '.')?.toDoubleOrNull()

/** Valore da 0 a 100, oppure null se manca o non è un numero. */
private fun String?.toScore(): Int? = toDoubleLoose()?.toInt()?.coerceIn(0, 100)

private fun String?.toBoolLoose(): Boolean = when (this?.trim()?.lowercase(Locale.ROOT)) {
    "true", "t", "1", "si", "sì", "yes" -> true
    else -> false
}

private fun formatGrado(raw: String?): String? {
    val v = raw.toDoubleLoose() ?: return null
    if (v <= 0.0) return null
    val s = if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.ITALY, "%.1f", v)
    return s
}

fun WineDto.toWine(): Wine? {
    val wid = id.clean()
    if (wid.isEmpty()) return null
    return Wine(
        id = wid,
        cantina = cantina.clean(),
        nome = nome.clean(),
        tipologia = tipo.clean(),
        annata = annata.clean().removeSuffix(".0"),
        quantita = quantita.toDoubleLoose()?.toInt() ?: 0,
        formato = formato.clean(),
        grado = formatGrado(grado),
        prezzo = prezzo.toDoubleLoose(),
        valoreMercato = valoreMercato.toDoubleLoose(),
        immagineUrl = immagineUrl.clean().ifEmpty { null },
        coloreVino = coloreVino.clean(),
        caratteristica = effervescenza.clean(),
        tipoVino = dolcezza.clean(),
        paese = paese.clean(),
        regione = regione.clean(),
        sitoProduttore = sitoProduttore.clean(),
        disponibileVendita = disponibileVendita.toBoolLoose(),
        giudizio = giudizioComplessivo.toScore(),
        morbidezza = morbidezza.toScore(),
        struttura = struttura.toScore(),
        tannicita = tannicita.toScore(),
        acidita = acidita.toScore(),
        mineralita = mineralita.toScore(),
    )
}

private val parser = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

/** Trasforma la risposta JSON del database in un elenco di vini. */
fun parseWines(body: String): List<Wine> =
    parser.decodeFromString<List<WineDto>>(body).mapNotNull { it.toWine() }
