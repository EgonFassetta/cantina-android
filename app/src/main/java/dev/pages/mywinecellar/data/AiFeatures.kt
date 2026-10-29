package dev.pages.mywinecellar.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Piatto proposto da «Cosa ci cucino?». */
data class DishSuggestion(
    val nome: String,
    val tempoMin: Int,
    val perche: String,
    val ingredienti: List<String>,
    val daComprare: List<String>,
    val passaggi: List<String>,
)

data class FoodResult(val piatti: List<DishSuggestion>, val message: String?)

/** Vino consigliato per un piatto (id del vino + motivo). */
data class WineMatch(val wineId: String, val reason: String)

data class EveningWine(
    val id: String,
    val cantina: String,
    val nome: String,
    val annata: String,
    val formato: String,
    val bottiglie: Int,
    val motivo: String,
)

data class EveningCourse(val portata: String, val piatto: String, val vini: List<EveningWine>, val mancano: Int)

data class EveningPlan(val ospiti: Int, val totale: Int, val nota: String, val courses: List<EveningCourse>)

// ---- lettura delle risposte JSON (stessi campi che usa il sito) ----

internal fun JsonObject.str(key: String): String = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
internal fun JsonObject.int(key: String): Int = this[key]?.jsonPrimitive?.intOrNull ?: 0
internal fun JsonObject.strList(key: String): List<String> =
    (this[key] as? JsonArray)?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }.orEmpty()
internal fun JsonObject.objList(key: String): List<JsonObject> =
    (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

fun parseFood(o: JsonObject): FoodResult = FoodResult(
    piatti = o.objList("piatti").map { d ->
        DishSuggestion(
            nome = d.str("nome"),
            tempoMin = d.int("tempo_min"),
            perche = d.str("perche"),
            ingredienti = d.strList("ingredienti"),
            daComprare = d.strList("da_comprare"),
            passaggi = d.strList("passaggi"),
        )
    },
    message = o["message"]?.jsonPrimitive?.contentOrNull,
)

fun parseWineMatches(o: JsonObject): List<WineMatch> =
    o.objList("recommendations").mapNotNull { r ->
        val id = r.str("wineId").ifBlank { return@mapNotNull null }
        WineMatch(id, r.str("reason"))
    }

fun parseEvening(o: JsonObject): EveningPlan {
    val p = o["piano"]?.jsonObject ?: JsonObject(emptyMap())
    return EveningPlan(
        ospiti = p.int("ospiti"),
        totale = p.int("totaleBottiglie"),
        nota = p.str("nota"),
        courses = p.objList("portate").map { c ->
            EveningCourse(
                portata = c.str("portata"),
                piatto = c.str("piatto"),
                mancano = c.int("mancano"),
                vini = c.objList("vini").map { v ->
                    EveningWine(
                        id = v.str("id"),
                        cantina = v.str("cantina"),
                        nome = v.str("nome"),
                        annata = v.str("annata"),
                        formato = v.str("formato"),
                        bottiglie = v.int("bottiglie"),
                        motivo = v.str("motivo"),
                    )
                },
            )
        },
    )
}
