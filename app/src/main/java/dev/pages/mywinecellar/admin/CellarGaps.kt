package dev.pages.mywinecellar.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pages.mywinecellar.data.Backend
import dev.pages.mywinecellar.data.SupabaseApi
import dev.pages.mywinecellar.ui.Accent
import dev.pages.mywinecellar.ui.Cream
import dev.pages.mywinecellar.ui.Gold
import dev.pages.mywinecellar.ui.GoldSoft
import dev.pages.mywinecellar.ui.InkSoft
import dev.pages.mywinecellar.ui.WineBright
import dev.pages.mywinecellar.ui.friendlyError
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

// ------------------------------------------------------------------ dati (risposta della funzione cellar-gaps del sito)

data class GapCategory(
    val id: String, val label: String, val stato: String, val mancano: Int, val bottiglie: Int, val obiettivo: Int,
    val quota: Double, val etichette: Int, val produttori: Int, val esempi: List<String>,
)

data class GapCourse(val portata: String, val categorie: List<String>, val stato: String, val bottiglie: Int, val serate: Int)
data class GapStyle(val stile: String, val categoria: String, val bottiglie: Int, val perche: String)

data class GapsReport(
    val bottiglie: Int, val etichette: Int, val nonClassificate: Int, val ospiti: Int, val bottigliePerPortata: Int,
    val categorie: List<GapCategory>, val portate: List<GapCourse>, val stili: List<GapStyle>,
)

data class GapSource(val url: String, val title: String)
data class GapPurchase(
    val stile: String, val regione: String, val categoria: String, val colma: Int, val perche: String,
    val abbinamenti: List<String>, val fascia: String, val bottiglie: Int, val fonti: List<GapSource>,
)

data class GapsSuggestions(val acquisti: List<GapPurchase>, val nota: String)

private fun JsonElement?.s(): String =
    (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull?.trim().orEmpty()

private fun JsonElement?.i(): Int = s().replace(',', '.').toDoubleOrNull()?.toInt() ?: 0
private fun JsonElement?.d(): Double = s().replace(',', '.').toDoubleOrNull() ?: 0.0
private fun JsonElement?.objs(): List<JsonObject> = (this as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
private fun JsonElement?.strs(): List<String> = (this as? JsonArray).orEmpty().map { it.s() }.filter { it.isNotEmpty() }

internal fun parseGapsReport(o: JsonObject): GapsReport {
    val tot = o["totale"] as? JsonObject
    return GapsReport(
        bottiglie = tot?.get("bottiglie").i(),
        etichette = tot?.get("etichette").i(),
        nonClassificate = (o["nonClassificati"] as? JsonObject)?.get("bottiglie").i(),
        ospiti = o["ospiti"].i(),
        bottigliePerPortata = o["bottigliePerPortata"].i(),
        categorie = o["categorie"].objs().map { c ->
            GapCategory(
                id = c["id"].s(), label = c["label"].s(), stato = c["stato"].s(), mancano = c["mancano"].i(),
                bottiglie = c["bottiglie"].i(), obiettivo = c["obiettivo"].i(), quota = c["quota"].d(),
                etichette = c["etichette"].i(), produttori = c["produttori"].i(), esempi = c["esempi"].strs(),
            )
        }.filter { it.id.isNotEmpty() },
        portate = o["portate"].objs().map { p ->
            GapCourse(p["portata"].s(), p["categorie"].strs(), p["stato"].s(), p["bottiglie"].i(), p["serate"].i())
        },
        stili = o["stili"].objs().map { x -> GapStyle(x["stile"].s(), x["categoria"].s(), x["bottiglie"].i(), x["perche"].s()) },
    )
}

internal fun parseGapsSuggestions(o: JsonObject): GapsSuggestions = GapsSuggestions(
    acquisti = o["acquisti"].objs().map { x ->
        GapPurchase(
            stile = x["stile"].s(), regione = x["regione"].s(),
            categoria = x["categoriaLabel"].s().ifEmpty { x["categoria"].s() },
            colma = x["colma"].i(), perche = x["perche"].s(), abbinamenti = x["abbinamenti"].strs(),
            fascia = x["fascia"].s(), bottiglie = x["bottiglie"].i(),
            fonti = x["fonti"].objs().map { f -> GapSource(f["url"].s(), f["title"].s()) }
                .filter { Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(it.url) },
        )
    },
    nota = o["nota"].s(),
)

/** Testo dello stato di una categoria, come sul sito. */
internal fun gapStatoText(c: GapCategory): String = when (c.stato) {
    "assente" -> "Assenti"
    "basso" -> "Pochi (mancano ${c.mancano})"
    else -> "Ok"
}

internal fun gapCourseText(stato: String): String = when (stato) {
    "scoperta" -> "Scoperta"
    "scarsa" -> "Scarsa"
    else -> "Ok"
}

private fun plur(n: Int, uno: String, molti: String) = "$n ${if (n == 1) uno else molti}"

private fun hostOf(url: String): String =
    runCatching { java.net.URI(url).host?.removePrefix("www.") }.getOrNull().orEmpty()

/** Budget proposti per i suggerimenti d'acquisto (valore inviato al server, testo mostrato), come sul sito. */
private val BUDGETS = listOf(
    "libero" to "Nessun limite", "basso" to "Fino a circa 15 €", "medio" to "Fino a circa 30 €", "alto" to "Anche oltre 30 €",
)

suspend fun SupabaseApi.gapsAnalyze(ospiti: Int): GapsReport =
    parseGapsReport(callFunction("cellar-gaps", buildJsonObject { put("action", "analyze"); put("ospiti", ospiti) },
        "Non riesco a leggere i dati della cantina."))

/** Obiettivi minimi per categoria: un campo vuoto si invia come null (il sito invia NaN, che diventa null). */
suspend fun SupabaseApi.gapsSaveTargets(obiettivi: Map<String, Int?>) {
    val ob = buildJsonObject { obiettivi.forEach { (k, v) -> put(k, v?.let { JsonPrimitive(it) } ?: JsonNull) } }
    callFunction("cellar-gaps", buildJsonObject { put("action", "save_targets"); put("obiettivi", ob) }, "Salvataggio non riuscito.")
}

suspend fun SupabaseApi.gapsSuggest(budget: String, note: String, ospiti: Int): GapsSuggestions =
    parseGapsSuggestions(
        callFunction(
            "cellar-gaps",
            buildJsonObject { put("action", "suggest"); put("budget", budget); put("note", note); put("ospiti", ospiti) },
            "Nessun suggerimento disponibile al momento.",
        ),
    )

// ------------------------------------------------------------------ schermata

/** «Cosa manca nella cantina» (solo Admin), come la sezione del sito. */
@Composable
fun CellarGapsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    BackHandler(onBack = onClose)

    var ospiti by remember { mutableStateOf("8") }
    var report by remember { mutableStateOf<GapsReport?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    var reportError by remember { mutableStateOf<String?>(null) }
    var targets by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var saveStatus by remember { mutableStateOf<String?>(null) }
    var showStyles by remember { mutableStateOf(false) }
    var budget by remember { mutableStateOf(BUDGETS.first()) }
    var note by remember { mutableStateOf("") }
    var suggesting by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<GapsSuggestions?>(null) }
    var suggestError by remember { mutableStateOf<String?>(null) }
    val guests = { ospiti.toIntOrNull()?.coerceIn(1, 30) ?: 8 }

    suspend fun analyze() {
        analyzing = true
        reportError = null
        try {
            val r = Backend.api(context).gapsAnalyze(guests())
            report = r
            targets = r.categorie.associate { it.id to it.obiettivo.toString() }
        } catch (e: Exception) {
            reportError = friendlyError(e)
        } finally {
            analyzing = false
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("🧭 Cosa manca nella cantina", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Accent, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Chiudi", color = WineBright) }
        }
        Text("Come è bilanciata la cantina per uso a tavola, quali portate riesci a coprire e cosa conviene comprare.", color = InkSoft)
        OutlinedTextField(
            value = ospiti, onValueChange = { ospiti = it.filter(Char::isDigit).take(2) },
            label = { Text("Ospiti di riferimento") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
        )
        Button(enabled = !analyzing, onClick = { scope.launch { analyze() } }) {
            Text(if (analyzing) "Analizzo…" else "Analizza la cantina")
        }
        reportError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        report?.let { r ->
            Text(
                "Totale: ${r.bottiglie} bottiglie · ${r.etichette} etichette" +
                    (if (r.nonClassificate > 0) " · ${r.nonClassificate} non classificate (completa colore e tipo nella scheda)" else ""),
                color = InkSoft,
            )
            Heading("Equilibrio per tipologia")
            r.categorie.forEach { c ->
                Card(colors = CardDefaults.cardColors(containerColor = Cream), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(c.label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(gapStatoText(c), color = if (c.stato == "ok") Accent else WineBright, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "${c.bottiglie} su ${c.obiettivo} · ${String.format(Locale.ITALY, "%.1f", c.quota)}% della cantina",
                            color = InkSoft, fontSize = 13.sp,
                        )
                        val prog = if (c.obiettivo > 0) (c.bottiglie.toFloat() / c.obiettivo).coerceIn(0f, 1f) else 1f
                        LinearProgressIndicator(progress = { prog }, modifier = Modifier.fillMaxWidth().height(6.dp), color = Accent, trackColor = GoldSoft)
                        Text(
                            plur(c.etichette, "etichetta", "etichette") + " · " + plur(c.produttori, "produttore", "produttori") +
                                (if (c.esempi.isNotEmpty()) " · es.: " + c.esempi.joinToString("; ") else ""),
                            color = InkSoft, fontSize = 13.sp,
                        )
                    }
                }
            }

            Heading("Portate che riesci a coprire")
            Text("Con ${r.ospiti} ospiti servono circa ${r.bottigliePerPortata} bottiglie a portata. Si contano le bottiglie non riservate.", color = InkSoft)
            r.portate.forEach { p ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.portata, fontWeight = FontWeight.SemiBold)
                        if (p.categorie.isNotEmpty()) Text(p.categorie.joinToString(", "), color = InkSoft, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(gapCourseText(p.stato), color = if (p.stato == "ok") Accent else WineBright, fontWeight = FontWeight.SemiBold)
                        Text("${p.bottiglie} bott. libere · ${plur(p.serate, "serata", "serate")}", color = InkSoft, fontSize = 13.sp)
                    }
                }
                HorizontalDivider(color = Gold.copy(alpha = 0.4f))
            }

            Heading("Obiettivi minimi")
            Text("Quante bottiglie vuoi avere almeno per ogni tipologia.", color = InkSoft)
            r.categorie.forEach { c ->
                OutlinedTextField(
                    value = targets[c.id].orEmpty(),
                    onValueChange = { v -> targets = targets + (c.id to v.filter(Char::isDigit).take(3)) },
                    label = { Text("${c.label} (minimo)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedButton(enabled = !analyzing, onClick = {
                saveStatus = "Salvo…"
                scope.launch {
                    saveStatus = try {
                        Backend.api(context).gapsSaveTargets(r.categorie.associate { it.id to targets[it.id]?.toIntOrNull()?.coerceIn(0, 500) })
                        analyze()
                        "Obiettivi salvati."
                    } catch (e: Exception) {
                        friendlyError(e)
                    }
                }
            }) { Text("Salva obiettivi") }
            saveStatus?.let { Text(it, color = InkSoft) }

            TextButton(onClick = { showStyles = !showStyles }) {
                Text((if (showStyles) "▾ " else "▸ ") + "Come ho classificato i vini", color = WineBright)
            }
            if (showStyles) {
                r.stili.forEach { x ->
                    val cat = r.categorie.firstOrNull { it.id == x.categoria }?.label ?: x.categoria.ifEmpty { "—" }
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("${x.stile} · $cat · ${x.bottiglie} bott.", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        if (x.perche.isNotBlank()) Text(x.perche, color = InkSoft, fontSize = 13.sp)
                    }
                }
            }

            Heading("Cosa comprare")
            EditChoice("Budget", BUDGETS.map { it.second }, budget.second, BUDGETS.first().second) { label ->
                budget = BUDGETS.firstOrNull { it.second == label } ?: BUDGETS.first()
            }
            OutlinedTextField(
                value = note, onValueChange = { note = it.take(200) },
                label = { Text("Note (facoltativo)") }, placeholder = { Text("Es. preferisco vini del Nord Italia") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Button(enabled = !suggesting, onClick = {
                suggesting = true
                suggestError = null
                suggestions = null
                scope.launch {
                    try {
                        suggestions = Backend.api(context).gapsSuggest(budget.first, note.trim(), guests())
                    } catch (e: Exception) {
                        suggestError = friendlyError(e)
                    } finally {
                        suggesting = false
                    }
                }
            }) { Text(if (suggesting) "Sto cercando…" else "Suggerisci acquisti") }
            if (suggesting) Text("Sto scegliendo gli acquisti e cercando i riferimenti sul web: può richiedere qualche secondo…", color = InkSoft)
            suggestError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            suggestions?.let { sg ->
                if (sg.acquisti.isEmpty()) Text("Nessun suggerimento disponibile al momento.", color = InkSoft)
                sg.acquisti.forEach { x ->
                    Card(colors = CardDefaults.cardColors(containerColor = Cream), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(x.stile + (if (x.regione.isNotBlank()) " · ${x.regione}" else ""), fontWeight = FontWeight.SemiBold)
                            Text(x.categoria + (if (x.colma > 0) " · mancano ${x.colma} bottiglie in questa categoria" else ""), color = WineBright, fontSize = 13.sp)
                            if (x.perche.isNotBlank()) Text(x.perche)
                            if (x.abbinamenti.isNotEmpty()) Text("Abbinamenti: " + x.abbinamenti.joinToString(", "), color = InkSoft, fontSize = 13.sp)
                            Text((if (x.fascia.isNotBlank()) "Fascia: ${x.fascia} · " else "") + "circa ${x.bottiglie} bottiglie", color = InkSoft, fontSize = 13.sp)
                            if (x.fonti.isNotEmpty()) {
                                Text("Per approfondire:", color = InkSoft, fontSize = 13.sp)
                                x.fonti.forEach { f ->
                                    Text(
                                        hostOf(f.url).ifEmpty { f.title.ifEmpty { "fonte" } }, color = WineBright, fontSize = 14.sp,
                                        modifier = Modifier.clickable { uriHandler.openUri(f.url) },
                                    )
                                }
                            }
                        }
                    }
                }
                if (sg.nota.isNotBlank()) Text(sg.nota, color = InkSoft, fontSize = 13.sp)
                Text(
                    "Suggerimenti generati da un’IA: verifica annate e prezzi prima di comprare. I link sono ricerche sul web, non offerte.",
                    color = InkSoft, fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Accent, modifier = Modifier.padding(top = 6.dp))
    HorizontalDivider(color = Gold)
}
