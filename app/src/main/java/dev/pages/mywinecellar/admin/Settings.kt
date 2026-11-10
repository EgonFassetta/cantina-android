package dev.pages.mywinecellar.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pages.mywinecellar.data.ApiException
import dev.pages.mywinecellar.data.Backend
import dev.pages.mywinecellar.data.DEFAULT_TENANT
import dev.pages.mywinecellar.data.SupabaseApi
import dev.pages.mywinecellar.data.isValidEmail
import dev.pages.mywinecellar.ui.Accent
import dev.pages.mywinecellar.ui.Cream
import dev.pages.mywinecellar.ui.Gold
import dev.pages.mywinecellar.ui.InkSoft
import dev.pages.mywinecellar.ui.WineBright
import dev.pages.mywinecellar.ui.friendlyError
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

// ------------------------------------------------------------------ dati e regole (come il sito)

const val DEFAULT_SITE_TITLE = "La cantina di Egon"
const val DEFAULT_REQUEST_LIMIT = 5
const val SITE_URL = "https://mywinecellar.pages.dev/"

/** Chiave di un'impostazione: la cantina principale usa la chiave semplice, le altre «cantina/chiave» (come il sito). */
fun settingKey(key: String, tenant: String): String = if (tenant == DEFAULT_TENANT) key else "$tenant/$key"

/** Indirizzo da condividere con i visitatori (sempre con ?c=, come il sito). */
fun cellarUrl(slug: String): String = "$SITE_URL?c=$slug"

/** Indirizzo della cantina: da 3 a 30 caratteri, lettere minuscole, numeri e trattino (stessa regola del sito). */
private val SLUG_RE = Regex("^[a-z0-9][a-z0-9-]{1,28}[a-z0-9]$")
fun isValidSlug(s: String): Boolean = SLUG_RE.matches(s)

data class AdminAccount(val email: String, val role: String)
data class AdminCellarInfo(val email: String, val nome: String, val slug: String, val vini: Int, val appAbilitata: Boolean)
data class AdminsOverview(val admins: List<AdminAccount>, val cellars: Map<String, AdminCellarInfo>, val principale: String)

private fun JsonElement?.txt(): String =
    (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull?.trim().orEmpty()

internal fun parseAdmins(o: JsonObject): List<AdminAccount> =
    (o["admins"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        .map { AdminAccount(it["email"].txt(), it["role"].txt()) }.filter { it.email.isNotEmpty() }

internal fun parseCellars(o: JsonObject): Pair<Map<String, AdminCellarInfo>, String> {
    val list = (o["cantine"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.map {
        AdminCellarInfo(
            email = it["email"].txt().lowercase(), nome = it["nome"].txt(), slug = it["slug"].txt().lowercase(),
            vini = it["vini"].txt().toDoubleOrNull()?.toInt() ?: 0,
            appAbilitata = (it["app_abilitata"] as? JsonPrimitive)?.booleanOrNull == true,
        )
    }
    return list.associateBy { it.email } to o["principale"].txt().lowercase().ifEmpty { DEFAULT_TENANT }
}

/** Controllo dei dati generali, come il pulsante «Salva» delle impostazioni del sito. Null = tutto a posto. */
fun settingsError(title: String, mail: String, oldMail: String, superAdmin: Boolean, limit: String): String? = when {
    title.isBlank() -> "Inserisci un titolo per il sito."
    mail.isNotBlank() && !isValidEmail(mail) -> "Inserisci un indirizzo email valido."
    mail.isBlank() && oldMail.isNotBlank() -> "L'email per le richieste non può essere vuota."
    superAdmin && (limit.trim().toIntOrNull() ?: 0) !in 1..200 -> "Il limite di richieste deve essere un numero da 1 a 200."
    else -> null
}

/** Controllo del nuovo amministratore (come il sito). Null = tutto a posto. */
fun newAdminError(email: String, password: String, slug: String): String? = when {
    !isValidEmail(email) -> "Inserisci un indirizzo email valido."
    password.length < 12 -> "La password deve avere almeno 12 caratteri."
    slug.isNotBlank() && !isValidSlug(slug) -> "Indirizzo non valido: da 3 a 30 caratteri, lettere minuscole, numeri e trattino."
    else -> null
}

// ------------------------------------------------------------------ chiamate al server

suspend fun SupabaseApi.getSetting(key: String): String? {
    val r = rest("GET", "site_settings?select=value&key=eq.${enc(key)}")
    if (r.code !in 200..299) return null
    val row = runCatching { (json.parseToJsonElement(r.body) as? JsonArray)?.firstOrNull() as? JsonObject }.getOrNull()
    return row?.get("value").txt().ifEmpty { null }
}

/** Salva un'impostazione (tabella site_settings), come il sito; se il database rifiuta lo segnala. */
suspend fun SupabaseApi.saveSetting(key: String, value: String) {
    val body = buildJsonObject { put("key", key); put("value", value) }
    val r = rest("POST", "site_settings?on_conflict=key", body.toString(), "resolution=merge-duplicates,return=representation")
    requireOk(r, "Salvataggio non riuscito.")
    val n = runCatching { (json.parseToJsonElement(r.body) as? JsonArray)?.size ?: 0 }.getOrDefault(0)
    if (n == 0) throw ApiException("Il database ha rifiutato la modifica (permessi insufficienti).")
}

/** Cantina pubblica (true) o privata (false), letta con admin-cellar «my_cellar». */
suspend fun SupabaseApi.isMyCellarPublic(): Boolean =
    (callFunction("admin-cellar", buildJsonObject { put("action", "my_cellar") }, "Impostazione non disponibile.")["pubblica"] as? JsonPrimitive)
        ?.booleanOrNull == true

suspend fun SupabaseApi.setMyCellarPublic(pubblica: Boolean): Boolean =
    (callFunction("admin-cellar", buildJsonObject { put("action", "set_public"); put("pubblica", pubblica) }, "Salvataggio non riuscito.")["pubblica"] as? JsonPrimitive)
        ?.booleanOrNull == true

suspend fun SupabaseApi.adminsOverview(): AdminsOverview {
    val admins = parseAdmins(callFunction("admin-manage", buildJsonObject { put("action", "list") }, "Impossibile caricare gli amministratori."))
    // admin-cellar «list» può mancare (funzione non pubblicata): in quel caso l'elenco resta senza cantine, come sul sito.
    val (cellars, main) = runCatching {
        parseCellars(callFunction("admin-cellar", buildJsonObject { put("action", "list") }, "Elenco cantine non disponibile."))
    }.getOrDefault(emptyMap<String, AdminCellarInfo>() to DEFAULT_TENANT)
    return AdminsOverview(admins, cellars, main)
}

suspend fun SupabaseApi.addAdmin(email: String, password: String) {
    callFunction("admin-manage", buildJsonObject { put("action", "add"); put("email", email); put("password", password) }, "Aggiunta non riuscita.")
}

suspend fun SupabaseApi.removeAdmin(email: String) {
    callFunction("admin-manage", buildJsonObject { put("action", "remove"); put("email", email) }, "Rimozione non riuscita.")
}

suspend fun SupabaseApi.setAdminCellar(email: String, nome: String, slug: String) {
    callFunction("admin-cellar", buildJsonObject { put("action", "set"); put("email", email); put("nome", nome); put("slug", slug) }, "Salvataggio non riuscito.")
}

suspend fun SupabaseApi.setAppEnabled(email: String, on: Boolean): Boolean =
    (callFunction("admin-cellar", buildJsonObject { put("action", "set"); put("email", email); put("app_abilitata", on) }, "Salvataggio non riuscito.")["app_abilitata"] as? JsonPrimitive)
        ?.booleanOrNull == true

// ------------------------------------------------------------------ schermata

private fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("Indirizzo della cantina", text))
}

/**
 * «⚙️ Impostazioni» (punto 10) e, per il Super Admin, «Amministratori» (punto 11): come la finestra Impostazioni del sito.
 * [tenant] e [cellarName] = cantina dell'Admin collegato; [myEmail] serve per non proporre la rimozione di se stessi.
 */
@Composable
fun AdminSettingsScreen(role: String, myEmail: String?, tenant: String, cellarName: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val superAdmin = role == "super_admin"
    BackHandler(onBack = onClose)

    var loaded by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(DEFAULT_SITE_TITLE) }
    var mail by remember { mutableStateOf("") }
    var oldMail by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf(DEFAULT_REQUEST_LIMIT.toString()) }
    var oldTitle by remember { mutableStateOf(DEFAULT_SITE_TITLE) }
    var oldLimit by remember { mutableStateOf(DEFAULT_REQUEST_LIMIT) }
    var pubblica by remember { mutableStateOf<Boolean?>(null) }
    var publicHint by remember { mutableStateOf("Caricamento…") }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val api = Backend.api(context)
        runCatching { api.getSetting(settingKey("site_title", tenant)) }.getOrNull()?.let { title = it; oldTitle = it }
        runCatching { api.getSetting(settingKey("admin_notification_email", tenant)) }.getOrNull()?.let { mail = it; oldMail = it }
        if (superAdmin) {
            val n = runCatching { api.getSetting("request_limit_per_hour") }.getOrNull()?.toIntOrNull()
            val v = if (n != null && n in 1..200) n else DEFAULT_REQUEST_LIMIT
            limit = v.toString(); oldLimit = v
        }
        loaded = true
        try {
            pubblica = api.isMyCellarPublic()
            publicHint = ""
        } catch (e: Exception) {
            publicHint = "Impostazione non disponibile: " + friendlyError(e)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("⚙️ Impostazioni", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Accent, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Chiudi", color = WineBright) }
        }
        OutlinedTextField(
            value = title, onValueChange = { title = it.take(80) }, label = { Text("Titolo del sito") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = mail, onValueChange = { mail = it.trim().take(120) }, label = { Text("Email a cui arrivano le richieste") },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
        )
        if (superAdmin) {
            OutlinedTextField(
                value = limit, onValueChange = { limit = it.filter(Char::isDigit).take(3) },
                label = { Text("Richieste massime all'ora per ogni visitatore") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
            )
            Text("Vale per ogni tipo di richiesta (disponibilità, carrello, assistenza). Valore consigliato: 5.", color = InkSoft, fontSize = 13.sp)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }
        Button(enabled = loaded && !saving, onClick = {
            val err = settingsError(title.trim(), mail.trim(), oldMail, superAdmin, limit)
            if (err != null) { error = err; return@Button }
            error = null
            saving = true
            scope.launch {
                try {
                    val api = Backend.api(context)
                    val t = title.trim()
                    if (t != oldTitle) { api.saveSetting(settingKey("site_title", tenant), t); oldTitle = t }
                    val m = mail.trim()
                    if (m.isNotEmpty() && m != oldMail) { api.saveSetting(settingKey("admin_notification_email", tenant), m); oldMail = m }
                    val l = limit.trim().toIntOrNull()
                    if (superAdmin && l != null && l != oldLimit) { api.saveSetting("request_limit_per_hour", l.toString()); oldLimit = l }
                    status = "Impostazioni salvate."
                } catch (e: Exception) {
                    error = "Salvataggio non riuscito: " + friendlyError(e)
                } finally {
                    saving = false
                }
            }
        }) { Text(if (saving) "Salvo…" else "Salva") }
        status?.let { Text(it, color = InkSoft) }

        HorizontalDivider(color = Gold)
        Text("La tua cantina: $cellarName", fontWeight = FontWeight.SemiBold)
        Text("Indirizzo da condividere con i visitatori:", color = InkSoft, fontSize = 13.sp)
        Text(cellarUrl(tenant), color = WineBright)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { copyText(context, cellarUrl(tenant)); status = "Indirizzo copiato." }) { Text("Copia indirizzo") }
            OutlinedButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, cellarUrl(tenant)) }
                context.startActivity(Intent.createChooser(send, "Condividi l'indirizzo della cantina"))
            }) { Text("Condividi") }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Cantina pubblica", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Switch(
                modifier = Modifier.semantics { contentDescription = "Cantina pubblica (interruttore)" },
                checked = pubblica == true, enabled = pubblica != null && !saving,
                onCheckedChange = { want ->
                    pubblica = null
                    publicHint = "Salvo…"
                    scope.launch {
                        try {
                            val now = Backend.api(context).setMyCellarPublic(want)
                            pubblica = now
                            publicHint = ""
                            status = if (now) "La tua cantina ora è pubblica." else "La tua cantina ora è privata."
                        } catch (e: Exception) {
                            pubblica = !want
                            publicHint = friendlyError(e)
                        }
                    }
                },
            )
        }
        Text(
            publicHint.ifEmpty {
                if (pubblica == true) "Pubblica: compare nella scelta delle cantine e i visitatori vedono i vini non esclusi dalla vetrina."
                else "Privata: la vedi solo tu. I visitatori non la trovano e non ne vedono i vini."
            },
            color = InkSoft, fontSize = 13.sp,
        )

        if (superAdmin) {
            HorizontalDivider(color = Gold)
            AdminsSection(myEmail = myEmail)
        }
    }
}

@Composable
private fun AdminsSection(myEmail: String?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var overview by remember { mutableStateOf<AdminsOverview?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AdminCellarInfo?>(null) }
    var toRemove by remember { mutableStateOf<String?>(null) }
    var newEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newNome by remember { mutableStateOf("") }
    var newSlug by remember { mutableStateOf("") }
    val me = myEmail.orEmpty().lowercase()

    suspend fun reload() {
        try {
            overview = Backend.api(context).adminsOverview()
        } catch (e: Exception) {
            error = friendlyError(e)
        }
    }
    LaunchedEffect(Unit) { reload() }
    fun act(block: suspend () -> Unit) {
        busy = true
        error = null
        scope.launch {
            try { block() } catch (e: Exception) { error = friendlyError(e) } finally { busy = false }
        }
    }

    Text("Amministratori", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Accent)
    val ov = overview
    if (ov == null && error == null) Text("Caricamento…", color = InkSoft)
    if (ov != null) ov.admins.forEach { a ->
        val c = ov.cellars[a.email.lowercase()]
        Card(colors = CardDefaults.cardColors(containerColor = Cream), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(a.email, fontWeight = FontWeight.SemiBold)
                        Text(if (a.role == "super_admin") "Super Admin" else "Admin", color = InkSoft, fontSize = 13.sp)
                    }
                    if (a.role == "admin" && a.email.lowercase() != me) {
                        TextButton(enabled = !busy, onClick = { toRemove = a.email }) { Text("Rimuovi", color = WineBright) }
                    }
                }
                if (c != null) {
                    Text("Cantina «${c.nome}» · ?c=${c.slug} · ${c.vini} vini", color = InkSoft, fontSize = 13.sp)
                    val isMain = c.slug == ov.principale
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when {
                                isMain -> "Visibile nell'app (la cantina principale lo è sempre)"
                                c.slug.isEmpty() -> "Visibile nell'app (assegna prima un indirizzo)"
                                else -> "Visibile nell'app"
                            },
                            fontSize = 14.sp, modifier = Modifier.weight(1f),
                        )
                        Switch(
                            modifier = Modifier.semantics { contentDescription = "Visibile nell'app: ${a.email}" },
                            checked = isMain || c.appAbilitata, enabled = !busy && !isMain && c.slug.isNotEmpty(),
                            onCheckedChange = { want ->
                                act {
                                    val now = Backend.api(context).setAppEnabled(a.email, want)
                                    overview = ov.copy(cellars = ov.cellars + (c.email to c.copy(appAbilitata = now)))
                                }
                            },
                        )
                    }
                    TextButton(enabled = !busy, onClick = { editing = c }) { Text("Modifica cantina", color = WineBright) }
                }
            }
        }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }

    Text("Aggiungi un amministratore", fontWeight = FontWeight.SemiBold)
    OutlinedTextField(
        value = newEmail, onValueChange = { newEmail = it.trim().lowercase().take(120) }, label = { Text("Email del nuovo amministratore") },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = newPassword, onValueChange = { newPassword = it.take(200) }, label = { Text("Password (almeno 12 caratteri)") },
        singleLine = true, visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = newNome, onValueChange = { newNome = it.take(80) }, label = { Text("Nome della cantina (facoltativo)") },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = newSlug, onValueChange = { newSlug = it.trim().lowercase().take(30) }, label = { Text("Indirizzo della cantina (facoltativo)") },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    Text(
        "Ogni amministratore ha la propria cantina, separata dalle altre. Se lasci vuoti nome e indirizzo, li ricava il server. " +
            "Comunica tu la password al nuovo amministratore.",
        color = InkSoft, fontSize = 13.sp,
    )
    Button(enabled = !busy, onClick = {
        val err = newAdminError(newEmail, newPassword, newSlug)
        if (err != null) { error = err; return@Button }
        act {
            val api = Backend.api(context)
            api.addAdmin(newEmail, newPassword)
            var warn: String? = null
            if (newNome.isNotBlank() || newSlug.isNotBlank()) {
                try { api.setAdminCellar(newEmail, newNome.trim(), newSlug) } catch (e: Exception) {
                    warn = "Amministratore aggiunto, ma la cantina non è stata impostata: ${friendlyError(e)}. Puoi correggerla con «Modifica cantina»."
                }
            }
            newEmail = ""; newPassword = ""; newNome = ""; newSlug = ""
            reload()
            error = warn
        }
    }) { Text("Aggiungi amministratore") }

    editing?.let { c ->
        var nome by remember(c.email) { mutableStateOf(c.nome) }
        var slug by remember(c.email) { mutableStateOf(c.slug) }
        var editError by remember(c.email) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Cantina di ${c.email}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nome, onValueChange = { nome = it.take(80) }, label = { Text("Nome della cantina") }, singleLine = true)
                    OutlinedTextField(value = slug, onValueChange = { slug = it.trim().lowercase().take(30) }, label = { Text("Indirizzo della cantina") }, singleLine = true)
                    Text("Cambiare l'indirizzo cambia il link da condividere con i visitatori.", color = InkSoft, fontSize = 13.sp)
                    editError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (!isValidSlug(slug)) { editError = "Indirizzo non valido: da 3 a 30 caratteri, lettere minuscole, numeri e trattino."; return@TextButton }
                    editing = null
                    act { Backend.api(context).setAdminCellar(c.email, nome.trim(), slug); reload() }
                }) { Text("Salva cantina") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Annulla") } },
        )
    }
    toRemove?.let { email ->
        AlertDialog(
            onDismissRequest = { toRemove = null },
            title = { Text("Rimuovere l'amministratore?") },
            text = { Text("Rimuovere i poteri di amministratore a $email?") },
            confirmButton = {
                TextButton(onClick = {
                    toRemove = null
                    act { Backend.api(context).removeAdmin(email); reload() }
                }) { Text("Sì, rimuovi") }
            },
            dismissButton = { TextButton(onClick = { toRemove = null }) { Text("Annulla") } },
        )
    }
}
