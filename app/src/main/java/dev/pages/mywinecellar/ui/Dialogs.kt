package dev.pages.mywinecellar.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pages.mywinecellar.data.isValidEmail
import dev.pages.mywinecellar.data.isValidPhone

enum class RequestMode { CART, AVAILABILITY, SUPPORT }

/** Controlli identici a quelli del sito e del server (così si evitano invii inutili). */
fun validateRequest(mode: RequestMode, email: String, phone: String, text: String): String? {
    val e = email.trim()
    val p = phone.trim()
    when (mode) {
        RequestMode.CART ->
            if (e.isEmpty() || p.isEmpty()) return "Email e telefono sono entrambi obbligatori."
        RequestMode.AVAILABILITY ->
            if (e.isEmpty() && p.isEmpty()) return "Inserisci almeno un indirizzo email o un numero di telefono."
        RequestMode.SUPPORT -> {
            if (text.isBlank()) return "Scrivi un messaggio prima di inviare."
            if (e.isEmpty() && p.isEmpty()) return "Inserisci almeno un indirizzo email o un numero di telefono."
        }
    }
    if (e.isNotEmpty() && !isValidEmail(e)) return "L'indirizzo email non sembra valido."
    if (p.isNotEmpty() && !isValidPhone(p)) return "Il numero di telefono non sembra valido."
    return null
}

@Composable
fun RequestDialog(
    mode: RequestMode,
    subtitle: String?,
    defaultEmail: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSend: (email: String, phone: String, text: String) -> Unit,
) {
    var email by remember { mutableStateOf(defaultEmail) }
    var phone by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val title = when (mode) {
        RequestMode.CART -> "Invia la richiesta"
        RequestMode.AVAILABILITY -> "Richiedi disponibilità"
        RequestMode.SUPPORT -> "Scrivi all'assistenza"
    }
    val textLabel = when (mode) {
        RequestMode.AVAILABILITY -> "Nota (facoltativa)"
        RequestMode.SUPPORT -> "Messaggio"
        RequestMode.CART -> ""
    }
    val emailLabel = if (mode == RequestMode.CART) "Email (obbligatoria)" else "Email"
    val phoneLabel = if (mode == RequestMode.CART) "Telefono (obbligatorio)" else "Telefono"

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (!subtitle.isNullOrBlank()) Text(subtitle, color = InkSoft, fontSize = 14.sp)
                if (mode == RequestMode.AVAILABILITY || mode == RequestMode.SUPPORT) {
                    Text(
                        "Indica almeno un recapito, così possiamo risponderti.",
                        color = InkSoft, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp),
                    )
                }
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text(emailLabel) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text(phoneLabel) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (mode != RequestMode.CART) {
                    OutlinedTextField(
                        value = text, onValueChange = { text = it },
                        label = { Text(textLabel) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    val problem = validateRequest(mode, email, phone, text)
                    error = problem
                    if (problem == null) onSend(email.trim(), phone.trim(), text.trim())
                },
            ) { Text(if (busy) "Invio…" else "Invia") }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text("Annulla") }
        },
    )
}
