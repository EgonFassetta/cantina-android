package dev.pages.mywinecellar.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.pages.mywinecellar.data.Wine

/** Scheda «Mappa»: scaffali oppure vista geografica. */
@Composable
fun MapTabScreen(wines: List<Wine>, onOpen: (Wine) -> Unit) {
    var geo by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp)) {
            FilterChip(selected = !geo, onClick = { geo = false }, label = { Text("Scaffali") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = geo, onClick = { geo = true }, label = { Text("Geografica") })
        }
        if (geo) GeoScreen(wines, onOpen) else ShelfMapScreen(wines, onOpen)
    }
}
