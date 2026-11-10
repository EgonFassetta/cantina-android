package dev.pages.mywinecellar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pages.mywinecellar.data.CellarInfo

/** Testo del contatore, come sul sito: «1 etichetta», «190 etichette». */
fun etichetteText(n: Int): String = if (n == 1) "1 etichetta" else "$n etichette"

/**
 * Prima schermata per chi non ha ancora scelto una cantina (come la prima pagina del sito):
 * solo le cantine aperte al pubblico, con il numero di etichette. Si cambia poi con «Cambia cantina».
 */
@Composable
fun CellarChooserScreen(
    state: AppState,
    onChoose: (String) -> Unit,
    onRetry: () -> Unit,
) {
    var pending by remember { mutableStateOf<String?>(null) }
    val choose: (String) -> Unit = { slug ->
        if (slug != state.cellar && state.cart.isNotEmpty()) pending = slug else onChoose(slug)
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 260.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Scegli una cantina", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Accent)
                val note = state.chooserNote?.let { "$it " }.orEmpty()
                val sub = when {
                    state.cellars.isEmpty() && state.cellarsLoading -> "Caricamento delle cantine…"
                    state.cellars.isEmpty() && state.cellarsError -> "Non riesco a caricare l'elenco delle cantine. Riprova tra poco."
                    state.cellars.isEmpty() -> "Al momento non ci sono cantine aperte al pubblico."
                    else -> "Queste sono le cantine aperte al pubblico. Puoi cambiarla quando vuoi con «Cambia cantina»."
                }
                Text(note + sub, color = InkSoft, fontSize = 15.sp)
                if (state.cellars.isEmpty() && state.cellarsLoading) {
                    CircularProgressIndicator(Modifier.padding(top = 8.dp).size(28.dp))
                }
                if (state.cellars.isEmpty() && state.cellarsError && !state.cellarsLoading) {
                    Button(onClick = onRetry) { Text("Riprova") }
                }
            }
        }
        items(state.cellars, key = { it.slug }) { c ->
            CellarCard(c, current = state.canCloseChooser && c.slug == state.cellar, onClick = { choose(c.slug) })
        }
    }

    pending?.let { target ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Cambiare cantina?") },
            text = { Text("Il carrello verrà svuotato.") },
            confirmButton = { TextButton(onClick = { onChoose(target); pending = null }) { Text("Cambia") } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Annulla") } },
        )
    }
}

@Composable
private fun CellarCard(c: CellarInfo, current: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Cream),
        border = BorderStroke(if (current) 2.dp else 1.dp, if (current) WineBright else Gold),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (current) Text("Cantina attuale", color = InkSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(c.nome, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
            Text(etichetteText(c.etichette), color = InkSoft, fontSize = 14.sp)
            Text(
                "Entra →",
                color = WineBright, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
