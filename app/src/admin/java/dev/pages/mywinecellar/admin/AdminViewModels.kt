package dev.pages.mywinecellar.admin

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.pages.mywinecellar.data.Backend
import dev.pages.mywinecellar.ui.friendlyError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ------------------------------------------------------------------ accesso amministratore

data class AdminState(
    val role: String? = null,
    val email: String? = null,
    val busy: Boolean = false,
    val error: String? = null,
)

class AdminViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("admin", Context.MODE_PRIVATE)
    private val api = Backend.api(app)

    private val _state = MutableStateFlow(AdminState())
    val state: StateFlow<AdminState> = _state.asStateFlow()

    init {
        // Il ruolo salvato vale solo se la sessione dell'app è ancora quella di un account con email.
        val email = api.session?.email
        val role = prefs.getString("role", null)
        if (email != null && role != null) _state.value = AdminState(role = role, email = email)
        else prefs.edit().remove("role").apply()
    }

    fun login(password: String, onSessionChanged: () -> Unit) {
        if (password.isBlank()) {
            _state.update { it.copy(error = "Inserisci la password.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                val role = api.adminLogin(password)
                prefs.edit().putString("role", role).apply()
                _state.value = AdminState(role = role, email = api.session?.email)
                onSessionChanged()
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = friendlyError(e)) }
            }
        }
    }

    fun logout(onSessionChanged: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                api.signOutDevice()
            } catch (_: Exception) {
            }
            prefs.edit().remove("role").apply()
            _state.value = AdminState()
            onSessionChanged()
        }
    }
}

// ------------------------------------------------------------------ aggiungere un vino da una foto dell'etichetta

enum class LabelStage { PHOTOS, READING, REVIEW, SAVING, DONE }

data class LabelUi(
    val stage: LabelStage = LabelStage.PHOTOS,
    val front: PreparedImage? = null,
    val back: PreparedImage? = null,
    val status: String = "",
    val reading: LabelReading? = null,
    val fields: LabelFields = LabelFields(),
    val quantita: String = "1",
    val posizione: String = "",
    /** Messaggio del server quando esiste già la stessa bottiglia: si può creare comunque. */
    val duplicateMessage: String? = null,
    val doneMessage: String = "",
    val busy: Boolean = false,
)

class LabelViewModel(app: Application) : AndroidViewModel(app) {
    private val api = Backend.api(app)

    private val _ui = MutableStateFlow(LabelUi())
    val ui: StateFlow<LabelUi> = _ui.asStateFlow()

    /** Se è stato salvato qualcosa, alla chiusura l'elenco dei vini va aggiornato. */
    var dirty = false
        private set

    fun reset() {
        _ui.value = LabelUi()
    }

    fun addPhoto(context: Context, key: String, uri: Uri) {
        _ui.update { it.copy(status = "Preparo la foto…") }
        viewModelScope.launch {
            try {
                val img = withContext(Dispatchers.Default) { prepareLabelImage(context, uri) }
                setPhoto(key, img)
            } catch (e: Exception) {
                _ui.update { it.copy(status = e.message ?: "Non riesco a leggere questa foto.") }
            }
        }
    }

    fun addTestPhoto(key: String) {
        viewModelScope.launch { setPhoto(key, withContext(Dispatchers.Default) { sampleLabelImage() }) }
    }

    private fun setPhoto(key: String, img: PreparedImage) {
        _ui.update { if (key == "front") it.copy(front = img, status = "") else it.copy(back = img, status = "") }
    }

    fun removePhoto(key: String) {
        _ui.update { if (key == "front") it.copy(front = null) else it.copy(back = null) }
    }

    fun read() {
        val front = _ui.value.front ?: return
        val images = listOfNotNull(front.dataUrl, _ui.value.back?.dataUrl)
        _ui.update { it.copy(stage = LabelStage.READING, status = "Sto leggendo la foto: può richiedere una decina di secondi.") }
        viewModelScope.launch {
            try {
                val r = api.labelRead(images)
                val f = if (r.campi.formato.isBlank()) r.campi.copy(formato = DEFAULT_FORMATO) else r.campi
                _ui.update {
                    it.copy(stage = LabelStage.REVIEW, reading = r, fields = f, status = "", duplicateMessage = null)
                }
            } catch (e: Exception) {
                _ui.update { it.copy(stage = LabelStage.PHOTOS, status = friendlyError(e)) }
            }
        }
    }

    fun backToPhotos() {
        _ui.update { it.copy(stage = LabelStage.PHOTOS, reading = null, status = "", duplicateMessage = null) }
    }

    fun setFields(f: LabelFields) = _ui.update { it.copy(fields = f) }
    fun setQuantita(v: String) = _ui.update { it.copy(quantita = v.filter { c -> c.isDigit() }.take(3)) }
    fun setPosizione(v: String) = _ui.update { it.copy(posizione = v.take(8)) }

    fun save(modo: String, wineId: String?, force: Boolean = false) {
        val st = _ui.value
        val qty = st.quantita.toIntOrNull() ?: 0
        if (qty !in 1..120) {
            _ui.update { it.copy(status = "Indica quante bottiglie aggiungere (da 1 a 120).") }
            return
        }
        _ui.update { it.copy(stage = LabelStage.SAVING, status = "Salvo…", duplicateMessage = null) }
        viewModelScope.launch {
            try {
                val r = api.labelSave(modo, st.fields, qty, st.posizione, wineId, force)
                if (r.duplicato) {
                    _ui.update { it.copy(stage = LabelStage.REVIEW, status = "", duplicateMessage = r.message ?: "Esiste già una riga uguale.") }
                    return@launch
                }
                val id = r.id.orEmpty()
                var photoNote = ""
                val front = st.front
                if (front != null && id.isNotEmpty()) {
                    try {
                        // la foto del fronte diventa l'immagine del vino (per una riga esistente solo se non ne ha già una)
                        if (modo != "aggiungi" || !api.wineHasImage(id)) api.attachWinePhoto(id, front.bytes)
                    } catch (e: Exception) {
                        photoNote = " La foto però non è stata caricata: aggiungila dalla scheda del vino."
                    }
                }
                dirty = true
                val titolo = listOf(st.fields.cantina, st.fields.nome.takeIf { it.isNotBlank() && it != st.fields.cantina })
                    .filterNotNull().filter { it.isNotBlank() }.joinToString(" · ") +
                    (if (st.fields.annata.isNotBlank()) " ${st.fields.annata}" else "")
                val msg = if (modo == "aggiungi") {
                    "Aggiunte ${bott(r.aggiunta)} a «$titolo»: ora ${bott(r.quantita)}."
                } else {
                    "Creata «$titolo»: ${bott(r.quantita.takeIf { it > 0 } ?: qty)}, scaffale ${st.posizione}."
                }
                _ui.update { it.copy(stage = LabelStage.DONE, doneMessage = msg + photoNote, status = "") }
            } catch (e: Exception) {
                _ui.update { it.copy(stage = LabelStage.REVIEW, status = friendlyError(e)) }
            }
        }
    }

    fun consumeDirty(): Boolean {
        val d = dirty
        dirty = false
        return d
    }

    private fun bott(n: Int) = "$n ${if (n == 1) "bottiglia" else "bottiglie"}"

    companion object {
        const val DEFAULT_FORMATO = "0,750 lt."
    }
}
