package dev.pages.mywinecellar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pages.mywinecellar.data.Wine

@Composable
fun CartScreen(
    state: AppState,
    onChange: (Wine, Int) -> Unit,
    onOpen: (Wine) -> Unit,
    onSend: () -> Unit,
) {
    val lines = state.cartLines
    Column(Modifier.fillMaxSize()) {
        Text(
            "Il tuo carrello",
            fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Burgundy,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        if (lines.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Il carrello è vuoto. Apri un vino disponibile e tocca «Aggiungi al carrello».",
                    color = InkSoft,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(lines, key = { it.wine.id }) { line ->
                    CartRow(line, onChange = { delta -> onChange(line.wine, delta) }, onOpen = { onOpen(line.wine) })
                }
            }
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(state.cartTotalText, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text(
                    "Nessun pagamento nell'app: invii una richiesta e ti ricontattiamo.",
                    color = InkSoft, fontSize = 13.sp, modifier = Modifier.padding(bottom = 10.dp),
                )
                Button(onClick = onSend, modifier = Modifier.fillMaxWidth()) { Text("Invia richiesta") }
            }
        }
    }
}

@Composable
private fun CartRow(line: CartLine, onChange: (Int) -> Unit, onOpen: () -> Unit) {
    val w = line.wine
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = Cream),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            WineThumb(w, 56)
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(w.titolo, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val sub = listOf(w.cantina, w.annata).filter { it.isNotBlank() }.joinToString(" · ")
                if (sub.isNotBlank()) Text(sub, color = InkSoft, fontSize = 13.sp)
                Text(
                    w.prezzoMostrato?.let { "${euro(it)} cad." } ?: "Prezzo su richiesta",
                    color = InkSoft, fontSize = 13.sp,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { onChange(-1) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(36.dp)) {
                    Text("−", fontSize = 18.sp)
                }
                Text("${line.qty}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
                OutlinedButton(onClick = { onChange(1) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(36.dp)) {
                    Text("+", fontSize = 18.sp)
                }
            }
        }
    }
}
