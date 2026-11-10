package dev.pages.mywinecellar.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pages.mywinecellar.data.ApiException
import dev.pages.mywinecellar.data.Backend
import dev.pages.mywinecellar.data.SupabaseApi
import dev.pages.mywinecellar.data.Wine
import dev.pages.mywinecellar.ui.Accent
import dev.pages.mywinecellar.ui.Cream
import dev.pages.mywinecellar.ui.Gold
import dev.pages.mywinecellar.ui.InkSoft
import dev.pages.mywinecellar.ui.WineBright
import dev.pages.mywinecellar.ui.euro
import dev.pages.mywinecellar.ui.friendlyError
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Le tre tabelle del Centro messaggi, con titolo e testo per l'elenco vuoto, come sul sito. */
enum class MsgKind(val table: String, val title: String, val empty: String) {
    ORDERS("cart_requests", "Ordini ricevuti", "Nessun ordine ricevuto."),
    AVAILABILITY("availability_requests", "Richieste di disponibilità", "Nessuna richiesta di disponibilità."),
    SUPPORT("support_requests", "Messaggi di supporto", "Nessun messaggio di supporto."),
}

/** Un messaggio da mostrare: titolo, testo, contatti (come le righe del sito). */
data class MsgItem(val id: String, val title: String, val text: String, val email: String, val phone: String)

private fun JsonObject.str(key: String): String =
    this[key]?.takeIf { it !is JsonNull }?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }?.trim().orEmpty()

/** Data in formato italiano (gg/mm/aaaa) da un valore come 2026-11-10T09:30:00Z. */
internal fun dateIt(iso: String): String {
    val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})").find(iso) ?: return iso
    val (y, mo, d) = m.destructured
    return "$d/$mo/$y"
}

/** Righe del database → messaggi, con le stesse informazioni che mostra il sito. */
internal fun parseMessages(kind: MsgKind, rows: JsonArray, winesById: Map<String, Wine>): List<MsgItem> =
    rows.mapNotNull { el ->
        val o = el as? JsonObject ?: return@mapNotNull null
        val id = o.str("id").ifEmpty { return@mapNotNull null }
        val date = dateIt(o.str("creato_il"))
        val email = o.str("contatto_email")
        val phone = o.str("contatto_telefono")
        when (kind) {
            MsgKind.ORDERS -> {
                val items = (o["items"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
                    .joinToString(", ") { "${it.str("nome")} x${it.str("quantita")}" }
                val totale = o.str("totale").replace(',', '.').toDoubleOrNull()
                MsgItem(id, "$date · ${totale?.let { euro(it) } ?: "–"}", items, email, phone)
            }
            MsgKind.AVAILABILITY -> {
                val w = winesById[o.str("wine_id")]
                val label = if (w != null) "${w.titolo} (${w.cantina}, ${w.annata})" else "Vino non più presente"
                MsgItem(id, "$label · $date", o.str("note").ifEmpty { "(nessuna nota)" }, email, phone)
            }
            MsgKind.SUPPORT -> MsgItem(id, date, o.str("messaggio"), email, phone)
        }
    }

/** Legge i messaggi (dal più recente), con la sessione dell'Admin come il sito. */
suspend fun SupabaseApi.fetchMessages(kind: MsgKind): JsonArray {
    val r = rest("GET", "${kind.table}?select=*&order=creato_il.desc")
    requireOk(r, "Impossibile leggere i messaggi.")
    return runCatching { json.parseToJsonElement(r.body) as? JsonArray }.getOrNull() ?: JsonArray(emptyList())
}

/** Elimina un messaggio; se il database rifiuta (0 righe) lo segnala. */
suspend fun SupabaseApi.deleteMessage(kind: MsgKind, id: String) {
    val r = rest("DELETE", "${kind.table}?id=eq.${enc(id)}", null, "return=representation")
    requireOk(r, "Impossibile eliminare il messaggio.")
    val n = runCatching { (json.parseToJsonElement(r.body) as? JsonArray)?.size ?: 0 }.getOrDefault(0)
    if (n == 0) throw ApiException("Il database ha rifiutato l'eliminazione (permessi insufficienti).")
}

/** Centro messaggi dell'Admin: ordini, richieste di disponibilità e messaggi di supporto, come sul sito. */
@Composable
fun MessageCenterScreen(wines: List<Wine>, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val winesById = remember(wines) { wines.associateBy { it.id } }
    var data by remember { mutableStateOf<Map<MsgKind, Result<List<MsgItem>>>>(emptyMap()) }
    var toDelete by remember { mutableStateOf<Pair<MsgKind, MsgItem>?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    BackHandler(onBack = onClose)

    suspend fun load(kind: MsgKind) {
        val r = runCatching { parseMessages(kind, Backend.api(context).fetchMessages(kind), winesById) }
        data = data + (kind to r)
    }
    LaunchedEffect(Unit) { MsgKind.entries.forEach { load(it) } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("📬 Centro messaggi", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Accent, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Chiudi", color = WineBright) }
        }
        status?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }
        MsgKind.entries.forEach { kind ->
            Text(kind.title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Accent)
            HorizontalDivider(color = Gold)
            val r = data[kind]
            when {
                r == null -> Text("Caricamento…", color = InkSoft)
                r.isFailure -> Text("Errore: " + friendlyError(r.exceptionOrNull()!!), color = MaterialTheme.colorScheme.error)
                r.getOrNull().isNullOrEmpty() -> Text(kind.empty, color = InkSoft)
                else -> r.getOrNull()!!.forEach { m ->
                    Card(colors = CardDefaults.cardColors(containerColor = Cream), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(m.title, fontWeight = FontWeight.SemiBold)
                                if (m.text.isNotBlank()) Text(m.text)
                                if (m.email.isNotBlank()) {
                                    Text(m.email, color = WineBright, fontSize = 14.sp,
                                        modifier = Modifier.clickable { uriHandler.openUri("mailto:${m.email}") })
                                }
                                if (m.phone.isNotBlank()) {
                                    Text(m.phone, color = WineBright, fontSize = 14.sp,
                                        modifier = Modifier.clickable { uriHandler.openUri("tel:${m.phone.filter { c -> c.isDigit() || c == '+' }}") })
                                }
                            }
                            TextButton(onClick = { toDelete = kind to m }) { Text("Elimina", color = WineBright) }
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { (kind, m) ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Eliminare questo messaggio?") },
            text = { Text(m.title) },
            confirmButton = {
                TextButton(onClick = {
                    toDelete = null
                    scope.launch {
                        status = try {
                            Backend.api(context).deleteMessage(kind, m.id)
                            load(kind)
                            null
                        } catch (e: Exception) {
                            "Impossibile eliminare: " + friendlyError(e)
                        }
                    }
                }) { Text("Sì, elimina") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Annulla") } },
        )
    }
}
