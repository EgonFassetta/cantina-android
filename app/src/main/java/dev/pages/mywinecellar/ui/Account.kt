package dev.pages.mywinecellar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pages.mywinecellar.BuildConfig
import dev.pages.mywinecellar.data.isValidEmail

@Composable
fun AccountScreen(
    state: AppState,
    onSave: (String, (Boolean, String?) -> Unit) -> Unit,
    onLogin: (String, (Boolean, String?) -> Unit) -> Unit,
    onLogout: () -> Unit,
    onDelete: (onUnavailable: () -> Unit) -> Unit,
    onSupport: () -> Unit,
    onPrivacy: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var loginMode by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleteUnavailable by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.isGuest) {
            Text(
                if (loginMode) "Accedi alla tua cantina" else "Salva la tua cantina",
                fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Burgundy,
            )
            Text(
                if (loginMode) {
                    "Inserisci l'email con cui hai salvato la cantina: ritroverai preferiti e carrello."
                } else {
                    "Hai già iniziato senza registrazione. Collega ora questa sessione a un account: " +
                        "preferiti e carrello resteranno invariati. Non devi scegliere una password."
                },
                color = InkSoft,
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }
            Button(
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val e = email.trim().lowercase()
                    if (!isValidEmail(e)) {
                        error = "Inserisci un indirizzo email valido."
                    } else {
                        error = null
                        val done: (Boolean, String?) -> Unit = { ok, msg -> if (!ok) error = msg }
                        if (loginMode) onLogin(e, done) else onSave(e, done)
                    }
                },
            ) { Text(if (loginMode) "Accedi" else "Salva la mia cantina") }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { loginMode = !loginMode; error = null },
            ) { Text(if (loginMode) "Non hai un account? Salva la tua cantina" else "Hai già un account? Accedi") }
        } else {
            Text("La tua cantina", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Burgundy)
            Text("Sei collegato come ${state.email}", color = Ink)
            Text(
                "Preferiti e carrello sono salvati nel tuo account e li ritrovi su ogni dispositivo.",
                color = InkSoft,
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { confirmLogout = true },
            ) { Text("Esci da questo dispositivo") }
        }

        HorizontalDivider(color = Gold, modifier = Modifier.padding(vertical = 6.dp))

        Text("Assistenza", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onSupport) { Text("Scrivici un messaggio") }
        TextButton(onClick = onPrivacy) { Text("Informativa sulla privacy", color = WineBright) }

        if (!state.isGuest) {
            TextButton(onClick = { confirmDelete = true }) {
                Text("Elimina il mio account", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Versione ${BuildConfig.VERSION_NAME}", color = InkSoft, fontSize = 12.sp)
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Uscire da questo dispositivo?") },
            text = {
                Text(
                    "Uscendo, preferiti e carrello non saranno più visibili su questo dispositivo finché non " +
                        "accedi di nuovo con la tua email. I tuoi dati restano salvati nel tuo account.",
                )
            },
            confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Sì, esci") } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Annulla") } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminare il tuo account?") },
            text = {
                Text(
                    "Verranno eliminati in modo definitivo l'account, i preferiti, il carrello e le richieste " +
                        "collegate alla tua email. L'operazione non si può annullare.",
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete { deleteUnavailable = true } }) {
                    Text("Elimina", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annulla") } },
        )
    }
    if (deleteUnavailable) {
        AlertDialog(
            onDismissRequest = { deleteUnavailable = false },
            title = { Text("Eliminazione non disponibile") },
            text = {
                Text(
                    "Al momento non riesco a eliminare l'account da qui. Scrivici un messaggio dalla sezione " +
                        "Assistenza indicando l'email dell'account e lo eliminiamo noi.",
                )
            },
            confirmButton = { TextButton(onClick = { deleteUnavailable = false }) { Text("Ho capito") } },
        )
    }
}
