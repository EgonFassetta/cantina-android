package dev.pages.mywinecellar.admin

import androidx.compose.runtime.Composable
import dev.pages.mywinecellar.data.Wine

/** Edizione per l'amministratore: area riservata con accesso a sola password. */
object AdminFeature {
    const val enabled = true

    @Composable
    fun Screen(onSessionChanged: () -> Unit, onCatalogChanged: () -> Unit, cellar: String, wines: List<Wine> = emptyList()) {
        AdminRoot(onSessionChanged = onSessionChanged, onCatalogChanged = onCatalogChanged, cellar = cellar, wines = wines)
    }
}
