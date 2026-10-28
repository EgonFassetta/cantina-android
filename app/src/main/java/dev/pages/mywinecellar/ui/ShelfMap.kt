package dev.pages.mywinecellar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pages.mywinecellar.data.Wine
import dev.pages.mywinecellar.data.shelfKey
import dev.pages.mywinecellar.data.shelfLabel

/** Mappa degli scaffali, come sul sito: 9 scaffali × 9 posizioni e 6 box. */
@Composable
fun ShelfMapScreen(wines: List<Wine>, onOpen: (Wine) -> Unit) {
    val byPos: Map<String, List<Wine>> = remember(wines) {
        wines.filter { it.quantita > 0 }
            .mapNotNull { w -> shelfKey(w.posizione)?.let { it to w } }
            .groupBy({ it.first }, { it.second })
    }
    var selected by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("Mappa degli scaffali", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Accent)
        Text(
            "Tocca un riquadro per vedere le bottiglie.",
            fontSize = 14.sp,
            color = InkSoft,
        )
        Spacer(Modifier.height(10.dp))

        // Intestazione colonne
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(30.dp))
            (1..9).forEach { c ->
                Text("$c", modifier = Modifier.weight(1f), fontSize = 11.sp, color = InkSoft, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        (1..9).forEach { s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("S$s", Modifier.width(30.dp), fontSize = 11.sp, color = InkSoft)
                (1..9).forEach { c ->
                    val key = "$s.$c"
                    val list = byPos[key]
                    ShelfCell(
                        modifier = Modifier.weight(1f),
                        qty = list?.sumOf { it.quantita } ?: 0,
                        selected = selected == key,
                        onClick = { if (list != null) selected = key },
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("Box", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Accent)
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..6).forEach { n ->
                val key = "box$n"
                val list = byPos[key]
                ShelfCell(
                    modifier = Modifier.weight(1f),
                    qty = list?.sumOf { it.quantita } ?: 0,
                    selected = selected == key,
                    onClick = { if (list != null) selected = key },
                    label = "B$n",
                )
            }
        }

        selected?.let { key ->
            val list = byPos[key].orEmpty()
            Spacer(Modifier.height(12.dp))
            Text(shelfLabel(key), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Accent)
            list.forEach { w ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(w) }.padding(vertical = 6.dp),
                ) {
                    Text(
                        "${w.cantina} ${w.nome} · ${w.quantita} bott.",
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShelfCell(modifier: Modifier, qty: Int, selected: Boolean, onClick: () -> Unit, label: String? = null) {
    val filled = qty > 0
    val bg = when {
        selected -> WineBright
        filled -> Accent
        else -> Parchment
    }
    Box(
        modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .background(bg)
            .clickable(enabled = filled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (filled) "$qty" else (label ?: ""),
            fontSize = 11.sp,
            color = if (filled) OnBrand else InkSoft,
        )
    }
}
