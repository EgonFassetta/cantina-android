package dev.pages.mywinecellar.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.pages.mywinecellar.data.ApiException
import dev.pages.mywinecellar.data.SharedPrefsSessionStore
import dev.pages.mywinecellar.data.SupabaseApi
import dev.pages.mywinecellar.data.Wine
import dev.pages.mywinecellar.data.filterWines
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartLine(val wine: Wine, val qty: Int)

data class AppState(
    val loading: Boolean = true,
    val error: String? = null,
    val wines: List<Wine> = emptyList(),
    val query: String = "",
    val onlyAvailable: Boolean = false,
    /** Email dell'account, oppure null se si usa l'app come ospite. */
    val email: String? = null,
    val favorites: Set<String> = emptySet(),
    val cart: Map<String, Int> = emptyMap(),
    val busy: Boolean = false,
    val showGuestHint: Boolean = false,
) {
    val isGuest: Boolean get() = email == null
    val visible: List<Wine> get() = filterWines(wines, query, onlyAvailable)
    val favoriteWines: List<Wine> get() = wines.filter { it.id in favorites }
    val cartLines: List<CartLine>
        get() = cart.mapNotNull { (id, q) -> wines.firstOrNull { it.id == id }?.let { CartLine(it, q) } }
    val cartCount: Int get() = cartLines.sumOf { it.qty }
    val cartTotal: Double get() = cartLines.sumOf { (it.wine.prezzoMostrato ?: 0.0) * it.qty }
}

/** Traduce gli errori tecnici in frasi comprensibili. */
fun friendlyError(e: Throwable): String = when (e) {
    is ApiException -> e.message ?: "Operazione non riuscita."
    is UnknownHostException, is ConnectException, is SocketTimeoutException ->
        "Connessione assente o instabile. Controlla la rete e riprova."
    is IOException -> "Problema di rete. Riprova tra poco."
    else -> e.message ?: "Operazione non riuscita."
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("app", Context.MODE_PRIVATE)
    private val api = SupabaseApi(SharedPrefsSessionStore(app))

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        load()
    }

    private fun say(text: String) {
        _messages.tryEmit(text)
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val wines = api.fetchWines()
                _state.update { it.copy(loading = false, wines = wines) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = friendlyError(e)) }
                return@launch
            }
            refreshUserData()
        }
    }

    /** Apre/rinnova la sessione e legge preferiti e carrello dell'utente. */
    private suspend fun refreshUserData() {
        try {
            val s = api.startSession()
            val favorites = api.fetchFavorites()
            val cart = api.fetchCart()
            _state.update { it.copy(email = s.email, favorites = favorites, cart = cart) }
        } catch (e: Exception) {
            say(friendlyError(e))
        }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }

    fun setOnlyAvailable(v: Boolean) = _state.update { it.copy(onlyAvailable = v) }

    // ------------------------------------------------------------ avviso agli ospiti

    private fun maybeShowGuestHint() {
        if (!_state.value.isGuest) return
        if (prefs.getBoolean("guest_hint_done", false)) return
        prefs.edit().putBoolean("guest_hint_done", true).apply()
        _state.update { it.copy(showGuestHint = true) }
    }

    fun dismissGuestHint() = _state.update { it.copy(showGuestHint = false) }

    // ------------------------------------------------------------ preferiti

    fun toggleFavorite(wineId: String) {
        val wasFavorite = wineId in _state.value.favorites
        _state.update {
            it.copy(favorites = if (wasFavorite) it.favorites - wineId else it.favorites + wineId)
        }
        maybeShowGuestHint()
        viewModelScope.launch {
            try {
                api.setFavorite(wineId, !wasFavorite)
            } catch (e: Exception) {
                _state.update {
                    it.copy(favorites = if (wasFavorite) it.favorites + wineId else it.favorites - wineId)
                }
                say(friendlyError(e))
            }
        }
    }

    // ------------------------------------------------------------ carrello

    /** Si può aggiungere solo se il vino è in vendita e non si supera la quantità disponibile. */
    fun canAdd(wine: Wine): Boolean = wine.disponibileVendita && (_state.value.cart[wine.id] ?: 0) < wine.quantita

    fun addToCart(wine: Wine) {
        val current = _state.value.cart[wine.id] ?: 0
        if (!wine.disponibileVendita) return
        if (current >= wine.quantita) {
            say("Hai già aggiunto tutte le bottiglie disponibili di questo vino.")
            return
        }
        setQty(wine.id, current + 1)
        say("Aggiunto al carrello.")
    }

    fun changeQty(wine: Wine, delta: Int) {
        val current = _state.value.cart[wine.id] ?: 0
        val next = (current + delta).coerceIn(0, wine.quantita.coerceAtLeast(1))
        if (next == current) return
        setQty(wine.id, next)
    }

    private fun setQty(wineId: String, qty: Int) {
        val before = _state.value.cart
        _state.update { st ->
            st.copy(cart = if (qty <= 0) st.cart - wineId else st.cart + (wineId to qty))
        }
        maybeShowGuestHint()
        viewModelScope.launch {
            try {
                api.setCartQty(wineId, qty)
            } catch (e: Exception) {
                _state.update { it.copy(cart = before) }
                say(friendlyError(e))
            }
        }
    }

    // ------------------------------------------------------------ richieste

    private fun runBusy(success: String?, block: suspend () -> Unit, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            var ok = false
            try {
                block()
                ok = true
                if (success != null) say(success)
            } catch (e: Exception) {
                say(friendlyError(e))
            } finally {
                _state.update { it.copy(busy = false) }
                onDone(ok)
            }
        }
    }

    fun sendCart(email: String, phone: String, onDone: (Boolean) -> Unit) = runBusy(
        success = "Richiesta inviata. Ti ricontatteremo presto.",
        block = {
            api.requestCart(email, phone, _state.value.cart)
            runCatching { api.clearCart() }
            _state.update { it.copy(cart = emptyMap()) }
        },
        onDone = onDone,
    )

    fun sendAvailability(wineId: String, email: String, phone: String, note: String, onDone: (Boolean) -> Unit) =
        runBusy("Richiesta inviata. Ti ricontatteremo presto.", { api.requestAvailability(wineId, email, phone, note) }, onDone)

    fun sendSupport(email: String, phone: String, message: String, onDone: (Boolean) -> Unit) =
        runBusy("Messaggio inviato. Grazie!", { api.sendSupport(email, phone, message) }, onDone)

    // ------------------------------------------------------------ account

    fun saveCellar(email: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                api.linkEmail(email)
                refreshUserData()
                say("Cantina salvata. Potrai ritrovarla accedendo con la tua email.")
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, friendlyError(e))
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun login(email: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                api.loginEmail(email)
                refreshUserData()
                say("Bentornato nella tua cantina.")
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, friendlyError(e))
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun logoutDevice() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                api.signOutDevice()
                prefs.edit().remove("guest_hint_done").apply()
                _state.update { it.copy(email = null, favorites = emptySet(), cart = emptyMap()) }
                refreshUserData()
                say("Sei uscito da questo dispositivo.")
            } catch (e: Exception) {
                say(friendlyError(e))
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun deleteAccount(onUnavailable: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                api.deleteAccount()
                prefs.edit().remove("guest_hint_done").apply()
                _state.update { it.copy(email = null, favorites = emptySet(), cart = emptyMap()) }
                refreshUserData()
                say("Account e dati eliminati.")
            } catch (e: ApiException) {
                if (e.message == "FUNZIONE_NON_DISPONIBILE") onUnavailable() else say(friendlyError(e))
            } catch (e: Exception) {
                say(friendlyError(e))
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}
