package dev.pages.mywinecellar.admin

import androidx.compose.runtime.Composable

/** Edizione per i clienti: nessuna funzione riservata all'amministratore. */
object AdminFeature {
    const val enabled = false

    @Composable
    fun Screen(onSessionChanged: () -> Unit, onCatalogChanged: () -> Unit) {
    }
}
