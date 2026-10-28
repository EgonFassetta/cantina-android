package dev.pages.mywinecellar.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Intent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.pages.mywinecellar.admin.AdminFeature
import dev.pages.mywinecellar.data.Wine
import androidx.compose.material.icons.filled.Settings
import java.util.Locale

private const val PRIVACY_URL = "https://mywinecellar.pages.dev/privacy.html"

/** Testo mostrato per la disponibilità (come sul sito: "Riservata" se tutte le bottiglie sono riservate). */
fun statoVino(w: Wine): String = when {
    w.inVendita -> "Disponibile"
    w.riservata -> "Riservata"
    else -> "Non disponibile"
}

fun euro(v: Double): String = String.format(Locale.ITALY, "€ %.2f", v)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CantinaApp(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var availabilityFor by remember { mutableStateOf<Wine?>(null) }
    var showSendCart by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var requestError by remember { mutableStateOf<String?>(null) }

    val selected = state.wines.firstOrNull { it.id == selectedId }
    BackHandler(enabled = selected != null) { selectedId = null }
    BackHandler(enabled = selected == null && tab != 0) { tab = 0 }

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
                actions = {
                    if (selected != null) {
                        IconButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, selected.titolo)
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    listOf(selected.titolo, selected.cantina, selected.annata)
                                        .filter { it.isNotBlank() }.joinToString(" · ") +
                                        "\nLa Cantina di Egon: https://mywinecellar.pages.dev/",
                                )
                            }
                            context.startActivity(Intent.createChooser(send, "Condividi il vino"))
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "Condividi")
                        }
                        val fav = selected.id in state.favorites
                        IconButton(onClick = { vm.toggleFavorite(selected.id) }) {
                            Icon(
                                if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (fav) "Rimuovi dai preferiti" else "Aggiungi ai preferiti",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Burgundy,
                    titleContentColor = OnBrand,
                    navigationIconContentColor = OnBrand,
                    actionIconContentColor = OnBrand,
                ),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Cream) {
                val colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = OnBrand,
                    selectedTextColor = Accent,
                    indicatorColor = Burgundy,
                    unselectedIconColor = InkSoft,
                    unselectedTextColor = InkSoft,
                )
                NavigationBarItem(
                    selected = tab == 0, colors = colors,
                    onClick = { tab = 0; selectedId = null },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Catalogo") },
                )
                NavigationBarItem(
                    selected = tab == 1, colors = colors,
                    onClick = { tab = 1; selectedId = null },
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                    label = { Text("Preferiti") },
                )
                NavigationBarItem(
                    selected = tab == 2, colors = colors,
                    onClick = { tab = 2; selectedId = null },
                    icon = {
                        BadgedBox(badge = { if (state.cartCount > 0) Badge { Text("${state.cartCount}") } }) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                        }
                    },
                    label = { Text("Carrello") },
                )
                NavigationBarItem(
                    selected = tab == 3, colors = colors,
                    onClick = { tab = 3; selectedId = null },
                    icon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                    label = { Text("Cantina") },
                )
                if (AdminFeature.enabled) {
                    NavigationBarItem(
                        selected = tab == 4, colors = colors,
                        onClick = { tab = 4; selectedId = null },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("Admin") },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Parchment,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.offline) OfflineBanner(onRetry = vm::load)
            state.email?.let { EmailBanner(it) }
            Box(Modifier.weight(1f).widthIn(max = 640.dp).fillMaxWidth()) {
                when {
                    selected != null -> WineDetailScreen(
                        vm = vm,
                        wine = selected,
                        qtyInCart = state.cart[selected.id] ?: 0,
                        canAdd = vm.canAdd(selected),
                        onAdd = { vm.addToCart(selected) },
                        onRequest = { availabilityFor = selected },
                        onOpenSite = { url -> uriHandler.openUri(url) },
                        others = state.wines
                            .filter { it.cantina.isNotBlank() && it.cantina == selected.cantina && it.id != selected.id }
                            .take(8),
                        onOpenOther = { selectedId = it.id },
                    )
                    tab == 4 && AdminFeature.enabled -> AdminFeature.Screen(
                        onSessionChanged = vm::reloadSession,
                        onCatalogChanged = vm::refresh,
                    )
                    tab == 3 -> AccountScreen(
                        state = state,
                        onStart = vm::startAccess,
                        onConfirmCode = vm::confirmCode,
                        onResendCode = vm::resendCode,
                        onCancelCode = vm::cancelCode,
                        onLogout = vm::logoutDevice,
                        onDelete = vm::deleteAccount,
                        onSupport = { showSupport = true },
                        onPrivacy = { uriHandler.openUri(PRIVACY_URL) },
                    )
                    state.loading -> LoadingView()
                    state.error != null -> ErrorView(state.error ?: "", onRetry = vm::load)
                    tab == 0 -> CatalogScreen(
                        vm = vm,
                        state = state,
                        onQuery = vm::setQuery,
                        onOnlyAvailable = vm::setOnlyAvailable,
                        onOpenFilters = { showFilters = true },
                        onRefresh = vm::refresh,
                        onOpen = { selectedId = it.id },
                        onToggleFavorite = { vm.toggleFavorite(it.id) },
                    )
                    tab == 1 -> FavoritesScreen(
                        state = state,
                        onOpen = { selectedId = it.id },
                        onToggleFavorite = { vm.toggleFavorite(it.id) },
                    )
                    else -> CartScreen(
                        state = state,
                        onChange = vm::changeQty,
                        onOpen = { selectedId = it.id },
                        onSend = { showSendCart = true },
                    )
                }
            }
        }
    }

    // ---- finestre di dialogo
    if (showFilters) {
        FiltersDialog(
            state = state,
            onApply = vm::applyFilters,
            onDismiss = { showFilters = false },
        )
    }
    availabilityFor?.let { wine ->
        RequestDialog(
            mode = RequestMode.AVAILABILITY,
            subtitle = wine.titolo,
            defaultEmail = state.email.orEmpty(),
            busy = state.busy,
            externalError = requestError,
            onDismiss = { availabilityFor = null; requestError = null },
            onSend = { email, phone, text ->
                requestError = null
                vm.sendAvailability(wine.id, email, phone, text) { ok, msg ->
                    if (ok) { availabilityFor = null; requestError = null } else requestError = msg
                }
            },
        )
    }
    if (showSendCart) {
        RequestDialog(
            mode = RequestMode.CART,
            subtitle = "${state.cartCount} bottiglie · ${state.cartTotalText.replaceFirstChar { it.lowercase() }}",
            defaultEmail = state.email.orEmpty(),
            busy = state.busy,
            externalError = requestError,
            onDismiss = { showSendCart = false; requestError = null },
            onSend = { email, phone, _ ->
                requestError = null
                vm.sendCart(email, phone) { ok, msg ->
                    if (ok) { showSendCart = false; requestError = null } else requestError = msg
                }
            },
        )
    }
    if (showSupport) {
        RequestDialog(
            mode = RequestMode.SUPPORT,
            subtitle = null,
            defaultEmail = state.email.orEmpty(),
            busy = state.busy,
            externalError = requestError,
            onDismiss = { showSupport = false; requestError = null },
            onSend = { email, phone, text ->
                requestError = null
                vm.sendSupport(email, phone, text) { ok, msg ->
                    if (ok) { showSupport = false; requestError = null } else requestError = msg
                }
            },
        )
    }
    if (state.showGuestHint) {
        AlertDialog(
            onDismissRequest = vm::dismissGuestHint,
            title = { Text("Stai usando l'app come ospite") },
            text = {
                Text(
                    "Preferiti, carrello e bottiglie bevute restano solo su questo dispositivo e si perdono " +
                        "se esci o cancelli i dati dell'app.\n\nPer memorizzarli e ritrovarli, entra con la tua email.",
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.dismissGuestHint(); selectedId = null; tab = 3 }) {
                    Text("Entra con la mia email")
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissGuestHint) { Text("Continua come ospite") }
            },
        )
    }
}

@Composable
private fun OfflineBanner(onRetry: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(WineBright.copy(alpha = 0.15f)).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Catalogo non aggiornato: sei offline.", fontSize = 13.sp, color = Ink, modifier = Modifier.weight(1f))
        TextButton(onClick = onRetry) { Text("Aggiorna", color = WineBright, fontSize = 13.sp) }
    }
}

@Composable
private fun EmailBanner(email: String) {
    Box(
        Modifier.fillMaxWidth().background(Gold.copy(alpha = 0.25f)).padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text("Sei collegato come $email", fontSize = 13.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Accent)
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogScreen(
    vm: AppViewModel,
    state: AppState,
    onQuery: (String) -> Unit,
    onOnlyAvailable: (Boolean) -> Unit,
    onOpenFilters: () -> Unit,
    onRefresh: () -> Unit,
    onOpen: (Wine) -> Unit,
    onToggleFavorite: (Wine) -> Unit,
) {
    val visible = state.visible
    val focus = LocalFocusManager.current
    var showDish by remember { mutableStateOf(false) }
    var showEvening by remember { mutableStateOf(false) }
    if (showDish) DishPairingDialog(vm, state.wines, onOpen) { showDish = false }
    if (showEvening) EveningDialog(vm) { showEvening = false }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = { showDish = true }) { Text("Abbina a un piatto") }
            TextButton(onClick = { showEvening = true }) { Text("Pianifica una serata") }
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            singleLine = true,
            placeholder = { Text("Cerca cantina, vino, annata…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQuery("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Cancella ricerca")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.moveFocus(FocusDirection.Exit) }),
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = !state.onlyAvailable, onClick = { onOnlyAvailable(false) }, label = { Text("Tutti") })
            FilterChip(selected = state.onlyAvailable, onClick = { onOnlyAvailable(true) }, label = { Text("Solo disponibili") })
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${visible.size} vini", color = InkSoft, fontSize = 13.sp)
            TextButton(onClick = onOpenFilters) {
                Text(
                    if (state.activeFilters > 0) "Filtri (${state.activeFilters})" else "Filtri",
                    color = WineBright, fontWeight = FontWeight.SemiBold,
                )
            }
        }
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            WineList(
                visible, state.favorites, onOpen, onToggleFavorite,
                emptyText = "Nessun vino trovato.",
                grouped = state.sort == dev.pages.mywinecellar.data.SortMode.CANTINA,
            )
        }
    }
}

@Composable
private fun FavoritesScreen(
    state: AppState,
    onOpen: (Wine) -> Unit,
    onToggleFavorite: (Wine) -> Unit,
) {
    val list = state.favoriteWines
    Column(Modifier.fillMaxSize()) {
        Text(
            "I tuoi preferiti",
            fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Accent,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        WineList(
            list, state.favorites, onOpen, onToggleFavorite,
            emptyText = "Nessun preferito. Tocca il cuore su un vino per salvarlo qui.",
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WineList(
    wines: List<Wine>,
    favorites: Set<String>,
    onOpen: (Wine) -> Unit,
    onToggleFavorite: (Wine) -> Unit,
    emptyText: String,
    grouped: Boolean = false,
) {
    if (wines.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(emptyText, color = InkSoft)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (grouped) {
                val groups = wines.groupBy { it.cantina.ifBlank { "Altre cantine" } }
                groups.forEach { (cantina, list) ->
                    stickyHeader(key = "cantina-$cantina") {
                        Box(Modifier.fillMaxWidth().background(Parchment).padding(vertical = 6.dp, horizontal = 4.dp)) {
                            Text(cantina, fontWeight = FontWeight.Bold, color = Accent, fontSize = 15.sp)
                        }
                    }
                    items(list, key = { it.id }) { wine ->
                        WineCard(
                            wine = wine,
                            isFavorite = wine.id in favorites,
                            onToggleFavorite = { onToggleFavorite(wine) },
                            onClick = { onOpen(wine) },
                        )
                    }
                }
            } else {
                items(wines, key = { it.id }) { wine ->
                    WineCard(
                        wine = wine,
                        isFavorite = wine.id in favorites,
                        onToggleFavorite = { onToggleFavorite(wine) },
                        onClick = { onOpen(wine) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WineCard(wine: Wine, isFavorite: Boolean, onToggleFavorite: () -> Unit, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Cream),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            WineThumb(wine, 76)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(wine.titolo, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val sub = listOf(wine.cantina, wine.annata).filter { it.isNotBlank() }.joinToString(" · ")
                if (sub.isNotBlank()) Text(sub, color = InkSoft, fontSize = 14.sp)
                val meta = listOf(wine.coloreVino, wine.caratteristica, wine.tipoVino, wine.regione)
                    .filter { it.isNotBlank() }.joinToString(" · ")
                if (meta.isNotBlank()) Text(meta, color = InkSoft, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    statoVino(wine),
                    color = if (wine.inVendita) Accent else InkSoft,
                    fontWeight = FontWeight.Medium, fontSize = 13.sp,
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Rimuovi dai preferiti" else "Aggiungi ai preferiti",
                    tint = if (isFavorite) WineBright else InkSoft,
                )
            }
        }
    }
}

@Composable
fun WineThumb(wine: Wine, sizeDp: Int) {
    val shape = RoundedCornerShape(6.dp)
    if (wine.immagineUrl != null) {
        AsyncImage(
            model = wine.immagineUrl,
            contentDescription = wine.titolo,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(sizeDp.dp).clip(shape),
        )
    } else {
        Box(Modifier.size(sizeDp.dp).clip(shape).background(Burgundy), contentAlignment = Alignment.Center) {
            Text("🍷", fontSize = (sizeDp / 2).sp)
        }
    }
}

@Composable
private fun WineDetailScreen(
    vm: AppViewModel,
    wine: Wine,
    qtyInCart: Int,
    canAdd: Boolean,
    onAdd: () -> Unit,
    onRequest: () -> Unit,
    onOpenSite: (String) -> Unit,
    others: List<Wine>,
    onOpenOther: (Wine) -> Unit,
) {
    var zoom by remember(wine.id) { mutableStateOf(false) }
    val scroll = remember(wine.id) { ScrollState(0) }
    if (zoom && wine.immagineUrl != null) {
        Dialog(onDismissRequest = { zoom = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                Modifier.fillMaxSize().background(Ink.copy(alpha = 0.92f)).clickable { zoom = false },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = wine.immagineUrl,
                    contentDescription = wine.titolo,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                )
            }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp)) {
        Box(
            Modifier.fillMaxWidth().then(if (wine.immagineUrl != null) Modifier.clickable { zoom = true } else Modifier),
            contentAlignment = Alignment.Center,
        ) { WineThumb(wine, 220) }
        Spacer(Modifier.height(16.dp))
        Text(wine.titolo, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Accent)
        val sub = listOf(wine.cantina, wine.annata).filter { it.isNotBlank() }.joinToString(" · ")
        if (sub.isNotBlank()) Text(sub, color = InkSoft, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            statoVino(wine),
            color = if (wine.inVendita) Accent else InkSoft,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        if (wine.inVendita) {
            if (canAdd) {
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Aggiungi al carrello") }
            } else {
                Text("Hai già aggiunto tutte le bottiglie disponibili.", color = InkSoft)
            }
            if (qtyInCart > 0) Text("Nel carrello: $qtyInCart", color = InkSoft, modifier = Modifier.padding(top = 6.dp))
        } else {
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) { Text("Richiedi disponibilità") }
        }
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
        DetailRow("Prezzo", wine.prezzoMostrato?.let { euro(it) } ?: "")
        DetailRow("Note", wine.nota)
        CucinoSection(wine, vm)
        if (wine.giudizio != null || wine.profilo.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("Profilo del vino", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Accent)
            wine.giudizio?.let { ProfileBar("Giudizio complessivo", it) }
            wine.profilo.forEach { (label, value) -> ProfileBar(label, value) }
        }
        if (wine.sitoProduttore.isNotBlank()) {
            val url = if (wine.sitoProduttore.startsWith("http")) wine.sitoProduttore else "https://${wine.sitoProduttore}"
            TextButton(onClick = { onOpenSite(url) }) { Text("Sito del produttore", color = WineBright) }
        }
        if (others.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("Altri vini di ${wine.cantina}", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Accent)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                others.forEach { other ->
                    Card(
                        onClick = { onOpenOther(other) },
                        colors = CardDefaults.cardColors(containerColor = Cream),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Column(Modifier.width(150.dp).padding(10.dp)) {
                            Text(other.titolo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (other.annata.isNotBlank()) Text(other.annata, color = InkSoft, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileBar(label: String, value: Int) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = InkSoft, fontSize = 13.sp)
            Text("$value / 100", fontSize = 13.sp)
        }
        LinearProgressIndicator(
            progress = { value.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Accent,
            trackColor = GoldSoft,
        )
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
