package dev.pages.mywinecellar.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pages.mywinecellar.data.SortMode

@Composable
fun FiltersDialog(
    state: AppState,
    onApply: (colore: String?, caratteristica: String?, tipoVino: String?, regione: String?, sort: SortMode) -> Unit,
    onDismiss: () -> Unit,
) {
    var colore by remember { mutableStateOf(state.colore) }
    var caratteristica by remember { mutableStateOf(state.caratteristica) }
    var tipoVino by remember { mutableStateOf(state.tipoVino) }
    var regione by remember { mutableStateOf(state.regione) }
    var sort by remember { mutableStateOf(state.sort) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtri e ordinamento") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ChipGroup("Colore", state.colori, colore) { colore = it }
                ChipGroup("Caratteristica vino", state.caratteristiche, caratteristica) { caratteristica = it }
                ChipGroup("Tipo vino", state.tipiVino, tipoVino) { tipoVino = it }
                ChipGroup("Regione", state.regioni, regione) { regione = it }
                Text(
                    "Ordina per", fontSize = 13.sp, color = InkSoft,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortMode.values().forEach { mode ->
                        FilterChip(selected = sort == mode, onClick = { sort = mode }, label = { Text(mode.label) })
                    }
                }
                TextButton(
                    onClick = {
                        colore = null; caratteristica = null; tipoVino = null; regione = null
                        sort = SortMode.CANTINA
                    },
                ) { Text("Azzera tutto", color = WineBright) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(colore, caratteristica, tipoVino, regione, sort); onDismiss() }) {
                Text("Applica")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
private fun ChipGroup(title: String, options: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    if (options.isEmpty()) return
    Text(title, fontSize = 13.sp, color = InkSoft, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = selected.equals(option, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(if (isSelected) null else option) },
                label = { Text(option) },
            )
        }
    }
}
