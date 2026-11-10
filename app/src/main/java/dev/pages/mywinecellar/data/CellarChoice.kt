package dev.pages.mywinecellar.data

/** Email dell'amministratore principale: come sul sito, senza assegnazione esplicita la sua cantina è quella principale. */
const val ADMIN_EMAIL = "admin.winecellar@aol.it"

/** Stato «bevuta» di una bottiglia per l'utente collegato. */
data class DrunkState(val dataApertura: String?, val dataFine: String?, val giudizio: String)

/** Una cantina mostrata nella schermata di scelta, con il numero di etichette visibili ai visitatori. */
data class CellarInfo(val slug: String, val nome: String, val etichette: Int)

/**
 * Cantine da proporre al cliente: solo quelle pubbliche (vista `cellars`, la stessa del sito) e abilitate all'app
 * (vista `app_cellars`). Se `cellars` non si legge (per esempio vista non ancora creata) si usano quelle dell'app,
 * come faceva l'app finora. Ordine alfabetico per nome, come sul sito.
 */
fun visibleCellars(appCellars: List<Cellar>, publicCellars: List<Cellar>?, counts: Map<String, Int>): List<CellarInfo> {
    val publicSlugs = publicCellars?.map { it.slug }?.toSet()
    return appCellars
        .filter { publicSlugs == null || it.slug in publicSlugs }
        .distinctBy { it.slug }
        .map { c ->
            val nome = c.nome.ifBlank { publicCellars?.firstOrNull { it.slug == c.slug }?.nome.orEmpty() }.ifBlank { c.slug }
            CellarInfo(c.slug, nome, counts[c.slug] ?: 0)
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nome })
}

/** Cosa mostrare all'apertura dell'app. */
sealed class StartDecision {
    /** Si apre direttamente la cantina [slug]; [asAdmin] = è la cantina dell'Admin collegato (anche se privata). */
    data class Open(val slug: String, val asAdmin: Boolean) : StartDecision()

    /** Si mostra la schermata «Scegli una cantina», con un eventuale avviso. */
    data class Choose(val note: String?) : StartDecision()
}

const val NOTE_CELLAR_GONE = "La cantina che avevi scelto non è più disponibile."

/**
 * Stesse regole del sito:
 * - l'Admin collegato entra nella propria cantina, anche se privata;
 * - chi non ha ancora scelto vede l'elenco delle cantine pubbliche;
 * - se la cantina scelta non è più pubblica (o non è più abilitata all'app) si torna alla scelta con un avviso;
 * - se l'elenco non si legge (per esempio senza rete) si riapre l'ultima cantina scelta, con il catalogo salvato.
 */
fun decideStart(savedCellar: String?, adminCellar: Cellar?, cellars: List<CellarInfo>?): StartDecision = when {
    adminCellar != null -> StartDecision.Open(adminCellar.slug, asAdmin = true)
    savedCellar.isNullOrBlank() -> StartDecision.Choose(null)
    cellars == null -> StartDecision.Open(savedCellar, asAdmin = false)
    cellars.any { it.slug == savedCellar } -> StartDecision.Open(savedCellar, asAdmin = false)
    else -> StartDecision.Choose(NOTE_CELLAR_GONE)
}
