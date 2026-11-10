package dev.pages.mywinecellar.admin

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import dev.pages.mywinecellar.BuildConfig
import dev.pages.mywinecellar.data.Backend
import dev.pages.mywinecellar.data.Wine
import dev.pages.mywinecellar.data.WineAttachment
import dev.pages.mywinecellar.ui.Accent
import dev.pages.mywinecellar.ui.Gold
import dev.pages.mywinecellar.ui.InkSoft
import dev.pages.mywinecellar.ui.WineBright
import dev.pages.mywinecellar.ui.friendlyError
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Solo nell'edizione di collaudo (server simulato) compaiono i pulsanti «di prova». */
private val isTestBuildEdit: Boolean get() = BuildConfig.SUPABASE_URL.startsWith("http://10.0.2.2")

/** Un PDF minimo valido, per la prova automatica del caricamento allegati. */
private val SAMPLE_PDF: ByteArray = (
    "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n" +
        "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 200 200]>>endobj\ntrailer<</Root 1 0 R>>\n%%EOF\n"
    ).toByteArray()

// ------------------------------------------------------------------ modifica della scheda

/** Tutti i campi che l'Admin può modificare sul sito (scheda e modifica rapida della riga), in un unico modulo. */
@Composable
fun WineEditScreen(wine: Wine, onClose: (changed: Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var f by remember(wine.id) { mutableStateOf(formFrom(wine)) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    BackHandler(enabled = !saving) { onClose(false) }

    fun save() {
        val patch = buildWinePatch(wine, f).getOrElse { error = it.message; return }
        error = null
        saving = true
        scope.launch {
            try {
                val api = Backend.api(context)
                api.updateWine(wine.id, patch.fields)
                if (patch.privateChanged) api.saveWinePrivate(wine.id, patch.prezzoAcquisto)
                onClose(true)
            } catch (e: Exception) {
                error = "Salvataggio non riuscito: " + friendlyError(e)
            } finally {
                saving = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("✏️ Modifica scheda", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Accent)
        Text(wine.titolo + (if (wine.cantina.isNotBlank()) " · ${wine.cantina}" else ""), color = InkSoft)

        Section("Bottiglia")
        Field("Annata", f.annata, KeyboardType.Number) { f = f.copy(annata = it.filter(Char::isDigit).take(4)) }
        Field("Bottiglie", f.quantita, KeyboardType.Number) { f = f.copy(quantita = it.filter(Char::isDigit).take(4)) }
        Field("Scaffale (es. 3.4 oppure Box1)", f.posizione) { f = f.copy(posizione = it.take(8)) }
        Field("Prezzo €", f.prezzo, KeyboardType.Decimal) { f = f.copy(prezzo = it.take(10)) }
        EditChoice("Formato", EDIT_FORMATI, f.formato, "—") { f = f.copy(formato = it) }
        EditChoice("Grado", EDIT_GRADI, f.grado, "—") { f = f.copy(grado = it) }

        Section("Vino")
        EditChoice("Colore vino", EDIT_COLORI, f.coloreVino, "—") { f = f.copy(coloreVino = it) }
        EditChoice("Caratteristica vino", EDIT_CARATTERISTICHE, f.caratteristica, "—") {
            // Se il tipo scelto non è più ammesso (es. Brut con Fermo) il campo si svuota, come sul sito.
            f = f.copy(caratteristica = it, tipoVino = f.tipoVino.takeIf { t -> t in tipiVinoPer(it) }.orEmpty())
        }
        EditChoice("Tipo vino", tipiVinoPer(f.caratteristica), f.tipoVino, "—") { f = f.copy(tipoVino = it) }

        Section("Origine")
        EditChoice("Paese", EDIT_PAESI, f.paese, "—") {
            f = f.copy(paese = it, regione = f.regione.takeIf { r -> r in regioniPer(it) }.orEmpty())
        }
        EditChoice("Regione", regioniPer(f.paese), f.regione, "—") { f = f.copy(regione = it) }
        Field("Sito del produttore", f.sitoProduttore, KeyboardType.Uri) { f = f.copy(sitoProduttore = it.take(300)) }

        Section("Giudizio")
        Score("Giudizio complessivo", f.giudizio) { f = f.copy(giudizio = it) }
        Section("Caratteristiche vino")
        EDIT_PROFILO.forEach { (field, label) ->
            Score(label, f.profilo[field]) { v -> f = f.copy(profilo = f.profilo + (field to v)) }
        }

        Section("Prezzi")
        Field("Prezzo acquisto € (lo vedi solo tu)", f.prezzoAcquisto, KeyboardType.Decimal) { f = f.copy(prezzoAcquisto = it.take(10)) }
        Field("Valore mercato €", f.valoreMercato, KeyboardType.Decimal) { f = f.copy(valoreMercato = it.take(10)) }
        Check("Disponibile alla vendita", f.disponibileVendita) { f = f.copy(disponibileVendita = it) }
        Check("Escludi dalla vetrina (lo vedi solo tu)", f.esclusoVetrina) { f = f.copy(esclusoVetrina = it) }

        Section("Note")
        OutlinedTextField(
            value = f.nota, onValueChange = { f = f.copy(nota = it.take(2000)) },
            label = { Text("Note") }, minLines = 3, modifier = Modifier.fillMaxWidth(),
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !saving, onClick = { save() }) { Text(if (saving) "Salvo…" else "Salva") }
            TextButton(enabled = !saving, onClick = { onClose(false) }) { Text("Annulla", color = WineBright) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(4.dp))
    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Accent)
    HorizontalDivider(color = Gold)
}

@Composable
private fun Field(label: String, value: String, type: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Check(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!value) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = value, onCheckedChange = onChange)
        Text(label)
    }
}

/** Cursore da 0 a 100; finché non si tocca resta «non indicato» e non viene salvato. */
@Composable
private fun Score(label: String, value: Int?, onChange: (Int?) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = InkSoft, fontSize = 14.sp)
            Text(value?.let { "$it / 100" } ?: "non indicato", fontSize = 14.sp)
        }
        Slider(
            value = (value ?: 50).toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..100f,
        )
    }
}

@Composable
internal fun EditChoice(label: String, options: List<String>, selected: String, empty: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = InkSoft, fontSize = 13.sp)
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected.ifBlank { empty })
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(empty) }, onClick = { onSelect(""); open = false })
            options.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { onSelect(o); open = false }) }
        }
    }
}

// ------------------------------------------------------------------ foto e allegati (nella scheda del vino)

private fun displayName(context: Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: "allegato.pdf"

private const val MAX_PDF_BYTES = 20 * 1024 * 1024

/**
 * Strumenti dell'Admin nella scheda del vino, come sul sito: modifica della scheda, foto della bottiglia,
 * allegati PDF (caricamento ed eliminazione). [tenant] = cantina dell'Admin (cartella nello storage).
 */
@Composable
fun AdminWineTools(
    wine: Wine,
    attachments: List<WineAttachment>,
    tenant: String,
    onEdit: () -> Unit,
    onWineChanged: () -> Unit,
    onAttachmentsChanged: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var busy by remember { mutableStateOf(false) }
    var status by remember(wine.id) { mutableStateOf<String?>(null) }
    var categoria by remember { mutableStateOf(ATTACH_CATEGORIES.first()) }
    var toDelete by remember { mutableStateOf<WineAttachment?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    fun act(done: String, block: suspend () -> Unit, after: () -> Unit) {
        busy = true
        status = null
        scope.launch {
            status = try {
                block(); after(); done
            } catch (e: Exception) {
                "Operazione non riuscita: " + friendlyError(e)
            } finally {
                busy = false
            }
        }
    }

    fun uploadPhoto(prepare: suspend () -> PreparedImage) = act("Foto aggiornata.", {
        val img = withContext(Dispatchers.Default) { prepare() }
        Backend.api(context).changeWinePhoto(tenant, wine.id, img.bytes)
    }, onWineChanged)

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingUri
        if (ok && uri != null) uploadPhoto { prepareLabelImage(context, uri) }
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) uploadPhoto { prepareLabelImage(context, uri) }
    }
    fun uploadPdfs(files: List<Pair<String, suspend () -> ByteArray>>) = act(
        if (files.size == 1) "Allegato caricato." else "${files.size} allegati caricati.",
        {
            val api = Backend.api(context)
            files.forEach { (name, read) ->
                val bytes = read()
                if (bytes.size > MAX_PDF_BYTES) throw IOException("«$name» è troppo grande (massimo 20 MB).")
                if (!(bytes.size > 4 && String(bytes, 0, 4) == "%PDF")) throw IOException("«$name» non è un PDF.")
                api.uploadAttachment(tenant, wine.id, name, bytes, categoria)
            }
        },
        onAttachmentsChanged,
    )
    val pickPdfs = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            uploadPdfs(uris.map { u ->
                val read: suspend () -> ByteArray = {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(u)?.use { it.readBytes() } ?: throw IOException("File non leggibile.")
                    }
                }
                displayName(context, u) to read
            })
        }
    }

    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color = Gold)
        Text("Strumenti Admin", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Accent)
        Button(enabled = !busy, onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("✏️ Modifica scheda") }
        if (wine.esclusoVetrina) Text("Esclusa dalla vetrina: la vedi solo tu.", color = InkSoft, fontSize = 14.sp)
        wine.prezzoAcquisto?.let { Text("Prezzo acquisto: " + dev.pages.mywinecellar.ui.euro(it), color = InkSoft, fontSize = 14.sp) }

        Text("Foto della bottiglia", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = !busy, onClick = {
                val file = File(context.cacheDir, "photos/bottiglia-${System.currentTimeMillis()}.jpg").also { it.parentFile?.mkdirs() }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                pendingUri = uri
                takePicture.launch(uri)
            }) { Text("📷 Scatta") }
            OutlinedButton(enabled = !busy, onClick = {
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text("Scegli foto") }
        }
        if (isTestBuildEdit) {
            OutlinedButton(enabled = !busy, onClick = { uploadPhoto { sampleLabelImage() } }) { Text("Usa foto bottiglia di prova") }
        }

        Text("Allegati", fontWeight = FontWeight.SemiBold)
        if (attachments.isEmpty()) Text("Nessun allegato caricato.", color = InkSoft, fontSize = 14.sp)
        attachments.forEach { a ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(a.categoria.ifBlank { "Allegato" }, color = Accent, fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    a.filename, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).clickable { uriHandler.openUri(a.url) },
                )
                TextButton(enabled = !busy, onClick = { toDelete = a }) { Text("Elimina", color = WineBright) }
            }
        }
        EditChoice("Categoria del nuovo allegato", ATTACH_CATEGORIES, categoria, ATTACH_CATEGORIES.first()) {
            categoria = it.ifBlank { ATTACH_CATEGORIES.first() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = !busy, onClick = { pickPdfs.launch("application/pdf") }) { Text("Carica PDF") }
            if (isTestBuildEdit) {
                OutlinedButton(enabled = !busy, onClick = {
                    val read: suspend () -> ByteArray = { SAMPLE_PDF }
                    uploadPdfs(listOf("prova.pdf" to read))
                }) {
                    Text("Usa PDF di prova")
                }
            }
        }
        if (busy) Text("Un momento…", color = InkSoft, fontSize = 14.sp)
        status?.let { Text(it, color = if (it.startsWith("Operazione")) MaterialTheme.colorScheme.error else InkSoft, fontSize = 14.sp) }
    }

    toDelete?.let { a ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Eliminare questo allegato?") },
            text = { Text(a.filename) },
            confirmButton = {
                TextButton(onClick = {
                    toDelete = null
                    act("Allegato eliminato.", { Backend.api(context).deleteAttachment(a.id) }, onAttachmentsChanged)
                }) { Text("Sì, elimina") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Annulla") } },
        )
    }
}
