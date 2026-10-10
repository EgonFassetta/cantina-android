package dev.pages.mywinecellar.data

import java.util.Locale

enum class SortMode(val label: String) {
    CANTINA("Cantina"),
    NOME("Nome"),
    ANNATA("Annata più recente"),
    GIUDIZIO("Giudizio migliore"),
}

/**
 * Filtra l'elenco come sul sito:
 * "solo disponibili" = flag di vendita attivo e quantità maggiore di zero.
 * I filtri facoltativi (colore, caratteristica, tipo vino, regione) valgono solo se indicati.
 */
fun filterWines(
    all: List<Wine>,
    query: String,
    onlyAvailable: Boolean,
    colore: String? = null,
    caratteristica: String? = null,
    tipoVino: String? = null,
    regione: String? = null,
): List<Wine> {
    val q = query.trim().lowercase(Locale.ITALY)
    return all.filter { w ->
        (!onlyAvailable || w.inVendita) &&
            (q.isEmpty() || w.testoRicerca.contains(q)) &&
            (colore == null || w.coloreVino.equals(colore, ignoreCase = true)) &&
            (caratteristica == null || w.caratteristica.equals(caratteristica, ignoreCase = true)) &&
            (tipoVino == null || w.tipoVino.equals(tipoVino, ignoreCase = true)) &&
            (regione == null || w.regione.equals(regione, ignoreCase = true))
    }
}

fun sortWines(list: List<Wine>, mode: SortMode): List<Wine> {
    val text = String.CASE_INSENSITIVE_ORDER
    return when (mode) {
        SortMode.CANTINA -> list.sortedWith(
            compareBy<Wine, String>(text) { it.cantina }
                .thenBy(text) { it.titolo }
                .thenBy { it.annata },
        )
        SortMode.NOME -> list.sortedWith(
            compareBy<Wine, String>(text) { it.titolo }.thenBy(text) { it.cantina },
        )
        SortMode.ANNATA -> list.sortedByDescending { it.annata.toIntOrNull() ?: 0 }
        SortMode.GIUDIZIO -> list.sortedByDescending { it.giudizio ?: -1 }
    }
}

/** Voci distinte e ordinate di un campo, per costruire le liste dei filtri. */
fun distinctValues(wines: List<Wine>, pick: (Wine) -> String): List<String> =
    wines.map(pick).filter { it.isNotBlank() }.distinctBy { it.lowercase(Locale.ITALY) }.sortedBy { it.lowercase(Locale.ITALY) }
