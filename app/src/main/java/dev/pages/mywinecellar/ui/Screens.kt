package dev.pages.mywinecellar.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.pages.mywinecellar.data.Wine
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CantinaApp(vm: CatalogViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = state.wines.firstOrNull { it.id == selectedId }

    BackHandler(enabled = selected != null) { selectedId = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selected?.titolo ?: "La Cantina di Egon",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (selected != null) {
                        IconButton(onClick = { selectedId = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Burgundy,
                    titleContentColor = Parchment,
                    navigationIconContentColor = Parchment,
                ),
            )
        },
        containerColor = Parchment,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                selected != null -> WineDetailScreen(selected)
                state.loading -> LoadingView()
                state.error != null -> ErrorView(state.error ?: "", onRetry = vm::load)
                else -> CatalogScreen(
                    state = state,
                    onQuery = vm::setQuery,
                    onOnlyAvailable = vm::setOnlyAvailable,
                    onOpen = { selectedId = it.id },
                )
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Burgundy)
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Non riesco a caricare il catalogo", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(message, color = InkSoft)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Riprova") }
    }
}

@Composable
private fun CatalogScreen(
    state: CatalogState,
    onQuery: (String) -> Unit,
    onOnlyAvailable: (Boolean) -> Unit,
    onOpen: (Wine) -> Unit,
) {
    val visible = state.visible
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            singleLine = true,
            placeholder = { Text("Cerca cantina, vino, annata…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = !state.onlyAvailable,
                onClick = { onOnlyAvailable(false) },
                label = { Text("Tutti") },
            )
            FilterChip(
                selected = state.onlyAvailable,
                onClick = { onOnlyAvailable(true) },
                label = { Text("Solo disponibili") },
            )
        }
        Text(
            text = "${visible.size} vini",
            color = InkSoft,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Nessun vino trovato.", color = InkSoft)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(visible, key = { it.id }) { wine ->
                    WineCard(wine = wine, onClick = { onOpen(wine) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WineCard(wine: Wine, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Cream),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            WineThumb(wine, 76)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    wine.titolo,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val sub = listOf(wine.cantina, wine.annata).filter { it.isNotBlank() }.joinToString(" · ")
                if (sub.isNotBlank()) Text(sub, color = InkSoft, fontSize = 14.sp)
                val meta = listOf(wine.coloreVino, wine.caratteristica, wine.tipoVino, wine.regione)
                    .filter { it.isNotBlank() }.joinToString(" · ")
                if (meta.isNotBlank()) Text(meta, color = InkSoft, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (wine.inVendita) "Disponibile" else "Non disponibile",
                    color = if (wine.inVendita) Burgundy else InkSoft,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun WineThumb(wine: Wine, sizeDp: Int) {
    val shape = RoundedCornerShape(6.dp)
    if (wine.immagineUrl != null) {
        AsyncImage(
            model = wine.immagineUrl,
            contentDescription = wine.titolo,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(sizeDp.dp).clip(shape),
        )
    } else {
        Box(
            Modifier.size(sizeDp.dp).clip(shape).background(Burgundy),
            contentAlignment = Alignment.Center,
        ) {
            Text("🍷", fontSize = (sizeDp / 2).sp)
        }
    }
}

@Composable
private fun WineDetailScreen(wine: Wine) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            WineThumb(wine, 220)
        }
        Spacer(Modifier.height(16.dp))
        Text(wine.titolo, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Burgundy)
        val sub = listOf(wine.cantina, wine.annata).filter { it.isNotBlank() }.joinToString(" · ")
        if (sub.isNotBlank()) Text(sub, color = InkSoft, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            if (wine.inVendita) "Disponibile" else "Non disponibile",
            color = if (wine.inVendita) Burgundy else InkSoft,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Gold)
        DetailRow("Tipologia", wine.tipologia)
        DetailRow("Colore", wine.coloreVino)
        DetailRow("Caratteristica vino", wine.caratteristica)
        DetailRow("Tipo vino", wine.tipoVino)
        DetailRow("Formato", wine.formato)
        DetailRow("Gradazione", wine.grado?.let { "$it% vol." } ?: "")
        DetailRow("Regione", wine.regione)
        DetailRow("Paese", wine.paese)
        DetailRow("Prezzo", wine.prezzoMostrato?.let { String.format(Locale.ITALY, "€ %.2f", it) } ?: "")
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = InkSoft, fontSize = 13.sp)
        Text(value, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
    HorizontalDivider(color = Gold.copy(alpha = 0.4f))
}
