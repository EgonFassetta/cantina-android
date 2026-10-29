package dev.pages.mywinecellar.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Contorno di una regione: id e percorso SVG (stessi dati del sito). */
@Serializable
data class GeoRegion(val id: String, val d: String)

@Serializable
data class GeoMapInfo(
    val viewBoxIt: String,
    val viewBoxFr: String,
    val itIdToRegione: Map<String, String>,
    val frRegioneToAdmin: Map<String, String>,
    val frAdminNames: Map<String, String>,
)

class GeoAssets(val it: List<GeoRegion>, val fr: List<GeoRegion>, val info: GeoMapInfo)

fun loadGeoAssets(context: Context): GeoAssets {
    val json = Json { ignoreUnknownKeys = true }
    fun read(name: String): String = context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
    return GeoAssets(
        it = json.decodeFromString(read("geo_it.json")),
        fr = json.decodeFromString(read("geo_fr.json")),
        info = json.decodeFromString(read("geo_map.json")),
    )
}

/** Bottiglie per regione geografica, come buildGeoCounts() sul sito. */
fun geoCounts(wines: List<Wine>, info: GeoMapInfo): Pair<Map<String, Int>, Map<String, Int>> {
    val itCounts = HashMap<String, Int>()
    val frCounts = HashMap<String, Int>()
    val itNameToId = info.itIdToRegione.entries.associate { it.value to it.key }
    wines.filter { it.quantita > 0 }.forEach { w ->
        val r = w.regione.trim()
        if (r.isEmpty()) return@forEach
        val itId = itNameToId[r]
        if (itId != null) {
            itCounts[itId] = (itCounts[itId] ?: 0) + w.quantita
            return@forEach
        }
        val adm = info.frRegioneToAdmin[r]
        if (adm != null) frCounts[adm] = (frCounts[adm] ?: 0) + w.quantita
    }
    return itCounts to frCounts
}
