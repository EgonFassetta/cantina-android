package dev.pages.mywinecellar.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pages.mywinecellar.data.DishSuggestion
import dev.pages.mywinecellar.data.EveningPlan
import dev.pages.mywinecellar.data.FoodResult
import dev.pages.mywinecellar.data.Wine
import dev.pages.mywinecellar.data.WineMatch
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val TIMES = listOf("veloce" to "Veloce (max 30 min)", "normale" to "Normale (circa 1 ora)", "lungo" to "Senza fretta")
private val POURS = listOf("poco" to "Poco", "normale" to "Normale", "tanto" to "Abbondante")
private val COURSES = listOf("Aperitivo", "Antipasto", "Primo", "Secondo", "Formaggi", "Dolce")

/** Foto → data URL JPEG ridotta (come sul sito). */
private fun imageToDataUrl(ctx: Context, uri: Uri): String? {
    val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
    val scale = min(1f, 1024f / max(bmp.width, bmp.height).toFloat())
    val small = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).roundToInt(), (bmp.height * scale).roundToInt(), true) else bmp
    val out = ByteArrayOutputStream()
    small.compress(Bitmap.CompressFormat.JPEG, 80, out)
    return "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}

/** «Cosa ci cucino?» dentro la scheda di una bottiglia. */
@Composable
fun CucinoSection(wine: Wine, vm: AppViewModel) {
    val ctx = LocalContext.current
    var ingredients by remember(wine.id) { mutableStateOf("") }
    var people by remember(wine.id) { mutableStateOf("2") }
    var time by remember(wine.id) { mutableStateOf("normale") }
    var busy by remember(wine.id) { mutableStateOf(false) }
    var result by remember(wine.id) { mutableStateOf<FoodResult?>(null) }
    var error by remember(wine.id) { mutableStateOf<String?>(null) }

    val photo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val dataUrl = runCatching { imageToDataUrl(ctx, uri) }.getOrNull()
        if (dataUrl == null) { error = "Non sono riuscito a leggere la foto."; return@rememberLauncherForActivityResult }
        busy = true; error = null
        vm.aiPhoto(dataUrl) { r ->
            busy = false
            r.onSuccess { found ->
                val base = ingredients.trim()
                ingredients = (listOfNotNull(base.takeIf { it.isNotEmpty() }) + found).joinToString(", ")
            }.onFailure { error = it.message }
        }
    }

    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Text("Cosa ci cucino?", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Accent)
        OutlinedTextField(
            value = ingredients,
            onValueChange = { ingredients = it },
            label = { Text("Ingredienti (facoltativo)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = people,
            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) people = it },
            label = { Text("Persone") },
            modifier = Modifier.width(120.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TIMES.forEach { (k, label) -> FilterChip(selected = time == k, onClick = { time = k }, label = { Text(label) }) }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !busy,
                onClick = {
                    busy = true; error = null
                    vm.aiFood(wine.id, ingredients, people.toIntOrNull() ?: 2, time) { r ->
                        busy = false
                        r.onSuccess { result = it }.onFailure { error = it.message }
                    }
                },
            ) { Text("Suggerisci piatti") }
            TextButton(enabled = !busy, onClick = { photo.launch("image/*") }) { Text("Foto del frigo") }
        }
        if (busy) Text("Un momento…", color = InkSoft, fontSize = 14.sp)
        error?.let { Text(it, color = WineBright, fontSize = 14.sp) }
        result?.let { r ->
            if (r.piatti.isEmpty()) Text(r.message ?: "Nessun suggerimento disponibile al momento.", color = InkSoft)
            r.piatti.forEach { DishCard(it) }
        }
    }
}

@Composable
private fun DishCard(d: DishSuggestion) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            d.nome + if (d.tempoMin > 0) "  · ${d.tempoMin} min" else "",
            fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Accent,
        )
        if (d.perche.isNotEmpty()) Text(d.perche, fontSize = 14.sp)
        if (d.ingredienti.isNotEmpty()) Text("Ingredienti: " + d.ingredienti.joinToString(", "), fontSize = 14.sp)
        if (d.daComprare.isNotEmpty()) Text("Da comprare: " + d.daComprare.joinToString(", "), fontSize = 14.sp, color = WineBright)
        d.passaggi.forEachIndexed { i, p -> Text("${i + 1}. $p", fontSize = 14.sp) }
    }
}

/** «Abbina a un piatto»: scrivi un piatto, l'app propone vini tra quelli in vendita. */
@Composable
fun DishPairingDialog(vm: AppViewModel, wines: List<Wine>, onOpen: (Wine) -> Unit, onDismiss: () -> Unit) {
    var dish by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var matches by remember { mutableStateOf<List<WineMatch>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val byId = remember(wines) { wines.associateBy { it.id } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abbina a un piatto") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = dish, onValueChange = { dish = it },
                    label = { Text("Che piatto prepari?") }, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    enabled = !busy && dish.isNotBlank(),
                    onClick = {
                        busy = true; error = null; matches = null
                        val candidates = wines.filter { it.quantita > 0 && it.disponibileVendita }
                        vm.aiDish(dish.trim(), candidates) { r ->
                            busy = false
                            r.onSuccess { matches = it }.onFailure { error = it.message }
                        }
                    },
                ) { Text("Trova vini") }
                if (busy) Text("Un momento…", color = InkSoft, fontSize = 14.sp)
                error?.let { Text(it, color = WineBright, fontSize = 14.sp) }
                matches?.let { list ->
                    if (list.isEmpty()) Text("Nessun abbinamento convincente tra le bottiglie disponibili.", color = InkSoft)
                    list.forEach { m ->
                        val w = byId[m.wineId] ?: return@forEach
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).then(Modifier.clickable { onOpen(w); onDismiss() })) {
                            Text("${w.cantina} ${w.nome} ${w.annata}".trim(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            if (m.reason.isNotEmpty()) Text(m.reason, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
    )
}

/** «Pianifica una serata»: un vino per portata, quantità calcolate dal server. */
@Composable
fun EveningDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    var guests by remember { mutableStateOf("6") }
    var pour by remember { mutableStateOf("normale") }
    var courses by remember { mutableStateOf(setOf("Primo", "Secondo", "Dolce")) }
    var menu by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var plan by remember { mutableStateOf<EveningPlan?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pianifica una serata") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = guests,
                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) guests = it },
                    label = { Text("Ospiti") }, modifier = Modifier.width(120.dp),
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    POURS.forEach { (k, label) -> FilterChip(selected = pour == k, onClick = { pour = k }, label = { Text(label) }) }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    COURSES.forEach { c ->
                        FilterChip(
                            selected = c in courses,
                            onClick = { courses = if (c in courses) courses - c else courses + c },
                            label = { Text(c) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = menu, onValueChange = { menu = it },
                    label = { Text("Menu (facoltativo)") }, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    enabled = !busy && courses.isNotEmpty(),
                    onClick = {
                        busy = true; error = null; plan = null
                        val ordered = COURSES.filter { it in courses }
                        vm.aiEvening(guests.toIntOrNull() ?: 6, pour, ordered, menu) { r ->
                            busy = false
                            r.onSuccess { plan = it }.onFailure { error = it.message }
                        }
                    },
                ) { Text("Prepara la serata") }
                if (busy) Text("Sto scegliendo i vini: può richiedere qualche secondo…", color = InkSoft, fontSize = 14.sp)
                error?.let { Text(it, color = WineBright, fontSize = 14.sp) }
                plan?.let { p ->
                    Spacer(Modifier.height(8.dp))
                    Text("Bottiglie in totale: ${p.totale} per ${p.ospiti} ospiti", fontWeight = FontWeight.SemiBold, color = Accent)
                    p.courses.forEach { c ->
                        Spacer(Modifier.height(6.dp))
                        Text(c.portata + if (c.piatto.isNotEmpty()) ": ${c.piatto}" else "", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        c.vini.forEach { v ->
                            Text("· ${v.cantina} ${v.nome} ${v.annata} — ${v.bottiglie} bott.", fontSize = 14.sp)
                            if (v.motivo.isNotEmpty()) Text(v.motivo, fontSize = 13.sp, color = InkSoft)
                        }
                        if (c.mancano > 0) Text("Mancano ${c.mancano} bottiglie per questa portata.", color = WineBright, fontSize = 13.sp)
                    }
                    if (p.nota.isNotEmpty()) Text(p.nota, fontSize = 13.sp, color = InkSoft)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
    )
}
