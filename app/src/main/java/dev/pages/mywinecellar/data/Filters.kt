package dev.pages.mywinecellar.data

import java.util.Locale

/**
 * Filtra l'elenco come sul sito:
 * "solo disponibili" = flag di vendita attivo e quantità maggiore di zero.
 */
fun filterWines(all: List<Wine>, query: String, onlyAvailable: Boolean): List<Wine> {
    val q = query.trim().lowercase(Locale.ITALY)
    return all.filter { w ->
        (!onlyAvailable || w.inVendita) && (q.isEmpty() || w.testoRicerca.contains(q))
    }
}
