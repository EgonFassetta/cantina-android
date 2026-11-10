package dev.pages.mywinecellar.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pages.mywinecellar.data.Wine
import dev.pages.mywinecellar.ui.Accent
import dev.pages.mywinecellar.ui.Cream
import dev.pages.mywinecellar.ui.Gold
import dev.pages.mywinecellar.ui.InkSoft
import dev.pages.mywinecellar.ui.WineBright
import java.util.Calendar

private val MESI = listOf(
    "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
    "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre",
)

/** Colore delle bottiglie finite (verde foglia, come sul sito). */
private val Leaf = Color(0xFF5E7D3A)

/** «Novembre 2026» (mese da 0 a 11). */
fun monthLabel(year: Int, month: Int): String = "${MESI[month]} $year"

/** Celle del mese a partire da lunedì: null = casella vuota prima del giorno 1. */
fun monthCells(year: Int, month: Int): List<Int?> {
    val cal = Calendar.getInstance().apply { clear(); set(year, month, 1) }
    val startWeekday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // lunedì = 0
    val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return List(startWeekday) { null } + (1..days).toList()
}

private fun ym(year: Int, month: Int) = "%04d-%02d".format(java.util.Locale.ROOT, year, month + 1)

/** Bottiglie finite nel mese (data di fine), come il sito; in ordine di apertura. */
fun drunkInMonth(wines: List<Wine>, year: Int, month: Int): List<Wine> =
    wines.filter { it.bevuto && it.dataFine?.startsWith(ym(year, month)) == true }.sortedBy { it.dataApertura.orEmpty() }

/** Per ogni giorno «aaaa-mm-gg»: bottiglie aperte e finite quel giorno. */
fun drunkByDate(wines: List<Wine>): Map<String, Pair<List<Wine>, List<Wine>>> {
    val opened = wines.filter { it.bevuto && it.dataApertura != null }.groupBy { it.dataApertura!! }
    val closed = wines.filter { it.bevuto && it.dataFine != null }.groupBy { it.dataFine!! }
    return (opened.keys + closed.keys).associateWith { (opened[it].orEmpty()) to (closed[it].orEmpty()) }
}

/** «Bottiglie bevute nel mese»: calendario con aperture e fine, come la pagina del sito. */
@Composable
fun DrunkCalendarScreen(wines: List<Wine>, onOpenWine: (String) -> Unit, onClose: () -> Unit) {
    val now = remember { Calendar.getInstance() }
    var year by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(now.get(Calendar.MONTH)) }
    var panel by remember { mutableStateOf<String?>(null) } // "mese" oppure una data aaaa-mm-gg
    BackHandler { if (panel != null) panel = null else onClose() }

    val byDate = remember(wines) { drunkByDate(wines) }
    val monthList = drunkInMonth(wines, year, month)
    val today = remember { "%04d-%02d-%02d".format(java.util.Locale.ROOT, now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH)) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("📅 Bottiglie bevute nel mese", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Accent, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Chiudi", color = WineBright) }
        }
        when (val p = panel) {
            null -> {
                Text("Aperture e fine bottiglie, mese per mese", color = InkSoft)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Cream),
                    modifier = Modifier.fillMaxWidth().clickable { panel = "mese" },
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${monthList.size}", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = WineBright)
                        Spacer(Modifier.width(12.dp))
                        Text("bottiglie bevute in ${monthLabel(year, month)}", color = InkSoft)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { if (month == 0) { month = 11; year -= 1 } else month -= 1 },
                        modifier = Modifier.semantics { contentDescription = "Mese precedente" },
                    ) { Text("←", fontSize = 20.sp) }
                    Text(monthLabel(year, month), fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f), color = Accent)
                    TextButton(
                        onClick = { if (month == 11) { month = 0; year += 1 } else month += 1 },
                        modifier = Modifier.semantics { contentDescription = "Mese successivo" },
                    ) { Text("→", fontSize = 20.sp) }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom").forEach {
                        Text(it, color = InkSoft, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    }
                }
                monthCells(year, month).chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (0 until 7).forEach { i ->
                            val day = week.getOrNull(i)
                            Box(Modifier.weight(1f).aspectRatio(1f)) {
                                if (day != null) {
                                    val date = "${ym(year, month)}-%02d".format(java.util.Locale.ROOT, day)
                                    val info = byDate[date]
                                    val has = info != null && (info.first.isNotEmpty() || info.second.isNotEmpty())
                                    Column(
                                        Modifier.fillMaxSize()
                                            .border(if (date == today) 2.dp else 1.dp, if (date == today) WineBright else Gold, RoundedCornerShape(4.dp))
                                            .background(if (has) Cream else Color.Transparent, RoundedCornerShape(4.dp))
                                            .then(if (has) Modifier.clickable { panel = date } else Modifier)
                                            .padding(4.dp),
                                    ) {
                                        Text("$day", fontSize = 13.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            if (info?.first?.isNotEmpty() == true) Dot(WineBright)
                                            if (info?.second?.isNotEmpty() == true) Dot(Leaf)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Dot(WineBright); Text(" bottiglia aperta", fontSize = 13.sp, color = InkSoft) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Dot(Leaf); Text(" bottiglia finita", fontSize = 13.sp, color = InkSoft) }
                }
            }
            "mese" -> {
                Text("Bevute in ${monthLabel(year, month)}", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Accent)
                Text("${monthList.size} ${if (monthList.size == 1) "bottiglia" else "bottiglie"}, con la relativa data di apertura", color = InkSoft)
                WineLines(monthList, onOpenWine)
                TextButton(onClick = { panel = null }) { Text("← Torna al calendario", color = WineBright) }
            }
            else -> {
                val info = byDate[p]
                Text(dateIt(p), fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Accent)
                if (!info?.first.isNullOrEmpty()) {
                    Text("Aperte questo giorno", color = InkSoft)
                    WineLines(info!!.first, onOpenWine)
                }
                if (!info?.second.isNullOrEmpty()) {
                    Text("Finite questo giorno", color = InkSoft)
                    WineLines(info!!.second, onOpenWine)
                }
                TextButton(onClick = { panel = null }) { Text("← Torna al calendario", color = WineBright) }
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(8.dp).background(color, CircleShape))
}

@Composable
private fun WineLines(list: List<Wine>, onOpenWine: (String) -> Unit) {
    if (list.isEmpty()) Text("Nessuna bottiglia.", color = InkSoft)
    list.forEach { w ->
        Column(Modifier.fillMaxWidth().clickable { onOpenWine(w.id) }.padding(vertical = 8.dp)) {
            Text(w.titolo, fontWeight = FontWeight.SemiBold)
            Text(
                listOf(w.cantina, w.annata).filter { it.isNotBlank() }.joinToString(" · ") +
                    (w.dataApertura?.let { " · aperta il ${dateIt(it)}" } ?: ""),
                color = InkSoft, fontSize = 14.sp,
            )
        }
        HorizontalDivider(color = Gold.copy(alpha = 0.4f))
    }
}
