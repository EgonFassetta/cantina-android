package dev.pages.mywinecellar.admin

import androidx.compose.runtime.Composable

/** Edizione per l'amministratore: area riservata con accesso a sola password. */
object AdminFeature {
    const val enabled = true

    @Composable
    fun Screen(onSessionChanged: () -> Unit, onCatalogChanged: () -> Unit) {
        AdminRoot(onSessionChanged = onSessionChanged, onCatalogChanged = onCatalogChanged)
    }
}
