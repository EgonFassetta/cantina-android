package dev.pages.mywinecellar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.pages.mywinecellar.data.SupabaseApi
import dev.pages.mywinecellar.data.Wine
import dev.pages.mywinecellar.data.filterWines
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogState(
    val loading: Boolean = true,
    val error: String? = null,
    val wines: List<Wine> = emptyList(),
    val query: String = "",
    val onlyAvailable: Boolean = false,
) {
    val visible: List<Wine> get() = filterWines(wines, query, onlyAvailable)
}

class CatalogViewModel : ViewModel() {
    private val api = SupabaseApi()
    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val wines = api.fetchWines()
                _state.update { it.copy(loading = false, wines = wines) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = e.message ?: "Impossibile caricare il catalogo.")
                }
            }
        }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }

    fun setOnlyAvailable(v: Boolean) = _state.update { it.copy(onlyAvailable = v) }
}
