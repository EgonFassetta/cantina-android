package dev.pages.mywinecellar.admin

import kotlinx.coroutines.launch
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.pages.mywinecellar.BuildConfig
import dev.pages.mywinecellar.ui.Accent
import dev.pages.mywinecellar.ui.Gold
import dev.pages.mywinecellar.ui.InkSoft
import dev.pages.mywinecellar.ui.WineBright
import java.io.File

private val FORMATI = listOf("0,750 lt.", "1,5 lt.", "3 lt.", "4 lt.", "5 lt.", "7 lt.", "9 lt.")
private val COLORI = listOf("Rosso", "Bianco", "Rosè")
private val CARATTERISTICHE = listOf("Fermo", "Bollicine")
private val TIPI_FERMO = listOf("Secco", "Dolce")
private val TIPI_TUTTI = listOf("Secco", "Dolce", "Brut", "Extra Brut", "Extra Dry", "Dry")

private fun tipiPer(caratteristica: String) = if (caratteristica == "Fermo") TIPI_FERMO else TIPI_TUTTI

/** Solo nell'edizione di collaudo (server simulato) compare il pulsante "Usa foto di prova". */
private val isTestBuild: Boolean get() = BuildConfig.SUPABASE_URL.startsWith("http://10.0.2.2")

// ------------------------------------------------------------------ area Admin

@Composable
fun AdminRoot(onSessionChanged: () -> Unit, onCatalogChanged: () -> Unit, cellar: String) {
    val vm: AdminViewModel = viewModel()
    val st by vm.state.collectAsState()
    var showLabel by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    when {
        st.role == null -> AdminLogin(
            busy = st.busy, error = st.error,
            onLogin = { pwd -> vm.login(pwd, onSessionChanged) },
        )
        showLabel -> LabelFlowScreen(onClose = { changed ->
            showLabel = false
            if (changed) onCatalogChanged()
        })
        else -> AdminHome(
            role = st.role ?: "admin", email = st.email, busy = st.busy,
            onLabel = { showLabel = true },
            onLogout = { vm.logout(onSessionChanged) },
            onExport = { scope.launch { runCatching { exportCsv(ctx, cellar) } } },
        )
    }
}

@Composable
private fun AdminLogin(busy: Boolean, error: String?, onLogin: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Area Admin", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Accent)
        Text("Accesso riservato all'amministratore. Inserisci la password.", color = InkSoft)
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }
        Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = { onLogin(password) }) {
            Text(if (busy) "Accesso…" else "Entra")
        }
    }
}

@Composable
private fun AdminHome(role: String, email: String?, busy: Boolean, onLabel: () -> Unit, onLogout: () -> Unit, onExport: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Area Admin", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Accent)
        Text(
            "Collegato come ${if (role == "super_admin") "Super Admin" else "Admin"}" + (email?.let { " ($it)" } ?: ""),
            color = InkSoft,
        )
        HorizontalDivider(color = Gold)
        Button(modifier = Modifier.fillMaxWidth(), onClick = onLabel) { Text("📷 Aggiungi vino da etichetta") }
        OutlinedButton(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = onExport) { Text("📄 Esporta la cantina in CSV") }
        Text(
            "Fotografa l'etichetta: l'app legge i dati e te li fa controllare prima di salvare.",
            color = InkSoft, fontSize = 13.sp,
        )
        HorizontalDivider(color = Gold)
        OutlinedButton(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = onLogout) { Text("Esci dall'area Admin") }
    }
}

// ------------------------------------------------------------------ aggiungere un vino da una foto

@Composable
fun LabelFlowScreen(onClose: (changed: Boolean) -> Unit) {
    val vm: LabelViewModel = viewModel()
    val ui by vm.ui.collectAsState()
    LaunchedEffect(Unit) { vm.reset() }

    BackHandler {
        when (ui.stage) {
            LabelStage.REVIEW -> vm.backToPhotos()
            LabelStage.READING, LabelStage.SAVING -> {}
            else -> onClose(vm.consumeDirty())
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("📷 Aggiungi vino da etichetta", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Accent)
        when (ui.stage) {
            LabelStage.PHOTOS -> PhotosStep(vm, ui) { onClose(vm.consumeDirty()) }
            LabelStage.READING -> {
                Text(ui.status, color = InkSoft)
            }
            LabelStage.REVIEW, LabelStage.SAVING -> ReviewStep(vm, ui)
            LabelStage.DONE -> DoneStep(ui, onAnother = { vm.reset() }, onClose = { onClose(vm.consumeDirty()) })
        }
    }
}

@Composable
private fun PhotosStep(vm: LabelViewModel, ui: LabelUi, onCancel: () -> Unit) {
    val context = LocalContext.current
    var pendingKey by remember { mutableStateOf("front") }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingUri
        if (ok && uri != null) vm.addPhoto(context, pendingKey, uri)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.addPhoto(context, pendingKey, uri)
    }
    fun camera(key: String) {
        val file = File(context.cacheDir, "photos/$key-${System.currentTimeMillis()}.jpg").also { it.parentFile?.mkdirs() }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingKey = key
        pendingUri = uri
        takePicture.launch(uri)
    }
    fun gallery(key: String) {
        pendingKey = key
        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Text("Fotografa l'etichetta: leggo i dati e te li faccio controllare prima di salvare. Il retro è facoltativo ma aiuta (gradazione, formato).", color = InkSoft)
    listOf("front" to "Fronte (obbligatoria)", "back" to "Retro (facoltativo)").forEach { (key, label) ->
        val img = if (key == "front") ui.front else ui.back
        Text(label, fontWeight = FontWeight.SemiBold)
        if (img != null) {
            Image(
                bitmap = img.bitmap.asImageBitmap(), contentDescription = "Anteprima: $label",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(6.dp)),
            )
            OutlinedButton(onClick = { vm.removePhoto(key) }) { Text("Togli") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { camera(key) }) { Text("📷 Scatta") }
                OutlinedButton(onClick = { gallery(key) }) { Text("Scegli dalla galleria") }
                if (isTestBuild) OutlinedButton(onClick = { vm.addTestPhoto(key) }) { Text("Usa foto di prova") }
            }
        }
    }
    if (ui.status.isNotBlank()) Text(ui.status, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(enabled = ui.front != null, onClick = { vm.read() }) { Text("Leggi l'etichetta") }
        TextButton(onClick = onCancel) { Text("Annulla", color = WineBright) }
    }
}

@Composable
private fun ReviewStep(vm: LabelViewModel, ui: LabelUi) {
    val r = ui.reading
    val f = ui.fields
    val saving = ui.stage == LabelStage.SAVING

    val warn = buildString {
        if (r?.leggibilita == "bassa") append("Lettura difficile: controlla bene ogni campo. ")
        if (!r?.note.isNullOrBlank()) append(r?.note)
    }
    if (warn.isNotBlank()) Text(warn, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)

    val same = r?.simili.orEmpty().filter { it.stessaAnnata }
    val other = r?.simili.orEmpty().filter { !it.stessaAnnata }
    if (same.isNotEmpty()) {
        Text("Hai già questa bottiglia (stessa annata)", fontWeight = FontWeight.SemiBold)
        Text("Per non creare un doppione puoi aggiungere le bottiglie alla riga esistente.", color = InkSoft, fontSize = 13.sp)
        same.forEach { w ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOf(w.cantina, w.nome, w.annata).filter { it.isNotBlank() }.joinToString(" · ") +
                        " · ${w.quantita} bott." + (if (w.posizione.isNotBlank()) " · scaffale ${w.posizione}" else ""),
                    modifier = Modifier.weight(1f), fontSize = 14.sp,
                )
                OutlinedButton(enabled = !saving, onClick = { vm.save("aggiungi", w.id) }) { Text("Aggiungi qui") }
            }
        }
    }
    if (other.isNotEmpty()) {
        Text(
            "Stesso vino, altre annate già in cantina: " + other.joinToString(", ") { "${it.annata} (${it.quantita} bott.)" } +
                ". Questa sarà una nuova riga.",
            color = InkSoft, fontSize = 13.sp,
        )
    }

    Text("I campi vuoti non sono stati letti: completali o lasciali vuoti. Controlla tutto prima di salvare.", color = InkSoft, fontSize = 13.sp)

    TextField2("Produttore (cantina) *", f.cantina) { vm.setFields(f.copy(cantina = it)) }
    TextField2("Nome del vino", f.nome) { vm.setFields(f.copy(nome = it)) }
    TextField2("Annata *", f.annata, KeyboardType.Number) { vm.setFields(f.copy(annata = it.filter { c -> c.isDigit() }.take(4))) }
    TextField2("Tipologia / denominazione", f.tipo) { vm.setFields(f.copy(tipo = it)) }
    TextField2("Gradazione (%)", f.grado, KeyboardType.Decimal) { vm.setFields(f.copy(grado = it.take(5))) }
    Choice("Formato", FORMATI, f.formato, "Non indicato") { vm.setFields(f.copy(formato = it)) }
    TextField2("Regione", f.regione) { vm.setFields(f.copy(regione = it)) }
    TextField2("Paese", f.paese) { vm.setFields(f.copy(paese = it)) }
    Choice("Colore", COLORI, f.coloreVino, "Non indicato") { vm.setFields(f.copy(coloreVino = it)) }
    Choice("Caratteristica", CARATTERISTICHE, f.effervescenza, "Non indicata") {
        val tipo = if (f.dolcezza in tipiPer(it)) f.dolcezza else ""
        vm.setFields(f.copy(effervescenza = it, dolcezza = tipo))
    }
    Choice("Tipo vino", tipiPer(f.effervescenza), f.dolcezza, "Non indicato") { vm.setFields(f.copy(dolcezza = it)) }
    TextField2("Sito del produttore", f.sitoProduttore, KeyboardType.Uri) { vm.setFields(f.copy(sitoProduttore = it)) }

    HorizontalDivider(color = Gold)
    TextField2("Quante bottiglie *", ui.quantita, KeyboardType.Number) { vm.setQuantita(it) }
    TextField2("Scaffale * (es. 3.4 oppure Box1)", ui.posizione) { vm.setPosizione(it) }

    ui.duplicateMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }
    if (ui.status.isNotBlank()) Text(ui.status, color = if (saving) InkSoft else MaterialTheme.colorScheme.error, fontSize = 14.sp)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (ui.duplicateMessage != null) {
            Button(enabled = !saving, onClick = { vm.save("nuova", null, force = true) }) { Text("Crea comunque") }
        } else {
            Button(enabled = !saving, onClick = { vm.save("nuova", null) }) { Text("Crea nuova riga") }
        }
        OutlinedButton(enabled = !saving, onClick = { vm.backToPhotos() }) { Text("Cambia foto") }
    }
}

@Composable
private fun DoneStep(ui: LabelUi, onAnother: () -> Unit, onClose: () -> Unit) {
    Text("✓", fontSize = 40.sp, color = Accent)
    Text(ui.doneMessage)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onAnother) { Text("Aggiungi un'altra") }
        OutlinedButton(onClick = onClose) { Text("Chiudi") }
    }
}

@Composable
private fun TextField2(label: String, value: String, type: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Choice(label: String, options: List<String>, selected: String, empty: String, onSelect: (String) -> Unit) {
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
