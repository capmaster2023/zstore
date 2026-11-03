package com.graceofgod.zstore.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.graceofgod.zstore.data.AppVersion
import com.graceofgod.zstore.data.StoreApp
import com.graceofgod.zstore.install.InstallState
import com.graceofgod.zstore.ui.theme.Z
import com.graceofgod.zstore.util.Pkg
import com.graceofgod.zstore.util.formatPrice
import com.graceofgod.zstore.util.formatSize
import com.graceofgod.zstore.util.openUrl
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- Catalogue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(deps: Deps, onOpenApp: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val catalog by deps.repo.catalog.collectAsState()
    val loading by deps.repo.loading.collectAsState()
    val states by deps.installer.states.collectAsState()
    val tick by deps.installer.installedTick.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var askPermission by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (catalog.isEmpty()) deps.repo.refreshCatalog().onFailure { error = it.message }
    }
    val installed = remember(catalog, tick) { Pkg.installedMap(context, catalog.map { it.packageName }) }
    val visible = catalog.filter { it.published || deps.isAdmin }.filter {
        query.isBlank() || it.name.contains(query, true) || (it.shortDesc ?: "").contains(query, true)
    }

    if (askPermission) {
        InstallPermissionDialog(
            onDismiss = { askPermission = false },
            onOpenSettings = { askPermission = false; context.startActivity(deps.installer.installPermissionIntent()) },
        )
    }

    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = { scope.launch { deps.repo.refreshCatalog().onFailure { error = it.message } } },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Z", style = MaterialTheme.typography.displaySmall, color = Z.Lime)
                        Text("-Store", style = MaterialTheme.typography.displaySmall)
                    }
                    Text("Les apps de Grace of God Studio", color = Z.Muted, modifier = Modifier.padding(top = 2.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Rechercher une app") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, "Effacer") }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Z.Surface, focusedContainerColor = Z.Surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp),
                    )
                }
            }
            if (error != null && catalog.isEmpty()) {
                item {
                    Box(Modifier.padding(top = 60.dp)) {
                        CenterMessage("Store indisponible", error, "Réessayer") {
                            error = null
                            scope.launch { deps.repo.refreshCatalog().onFailure { error = it.message } }
                        }
                    }
                }
            } else if (!loading && visible.isEmpty()) {
                item {
                    Box(Modifier.padding(top = 60.dp)) {
                        CenterMessage(
                            if (query.isBlank()) "Aucune app pour l'instant" else "Aucun résultat",
                            if (query.isBlank() && deps.isAdmin) "Va dans Admin pour ajouter ton premier APK." else null,
                        )
                    }
                }
            }
            items(visible, key = { it.id }) { app ->
                AppRow(
                    app = app,
                    installedVersion = installed[app.packageName],
                    state = states[app.packageName],
                    onClick = { onOpenApp(app.id) },
                    onInstall = { requestInstall(context, deps.installer, app) { askPermission = true } },
                    onOpen = { Pkg.open(context, app.packageName) },
                    onBuy = { onOpenApp(app.id) },
                )
            }
        }
    }
}

@Composable
fun AppRow(
    app: StoreApp,
    installedVersion: Long?,
    state: InstallState?,
    onClick: () -> Unit,
    onInstall: () -> Unit,
    onOpen: () -> Unit,
    onBuy: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.iconUrl, 60.dp)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            Text(app.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                app.shortDesc ?: app.packageName,
                color = Z.Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!app.published) Pill("Brouillon", Z.Amber)
                if (app.isFree) Pill("Gratuit", Z.Muted) else Pill(formatPrice(app.priceCents, app.currency), Z.Lime)
                if (state is InstallState.Failed) Pill("Échec", Z.Red)
            }
        }
        AppActionButton(app, installedVersion, state, compact = true, onInstall = onInstall, onOpen = onOpen, onBuy = onBuy)
    }
}

// ---------------------------------------------------------------- Fiche app

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    deps: Deps,
    appId: Int,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onEdit: (Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val catalog by deps.repo.catalog.collectAsState()
    val states by deps.installer.states.collectAsState()
    val tick by deps.installer.installedTick.collectAsState()
    var app by remember { mutableStateOf(deps.repo.cached(appId)) }
    var versions by remember { mutableStateOf<List<AppVersion>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var askPermission by remember { mutableStateOf(false) }
    var buying by remember { mutableStateOf(false) }

    LaunchedEffect(appId, catalog) {
        deps.repo.appDetail(appId)
            .onSuccess { app = it.app; versions = it.versions }
            .onFailure { if (app == null) error = it.message }
    }

    if (askPermission) {
        InstallPermissionDialog(
            onDismiss = { askPermission = false },
            onOpenSettings = { askPermission = false; context.startActivity(deps.installer.installPermissionIntent()) },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
                actions = {
                    if (deps.isAdmin) IconButton(onClick = { onEdit(appId) }) { Icon(Icons.Filled.Edit, "Modifier") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Z.Bg),
            )
        },
    ) { pad ->
        val a = app
        when {
            a == null && error != null -> Box(Modifier.padding(pad)) { CenterMessage("Introuvable", error) }
            a == null -> Box(Modifier.padding(pad)) { Loading() }
            else -> {
                val installedVersion = remember(a, tick) { Pkg.installedVersion(context, a.packageName) }
                val state = states[a.packageName]
                Column(
                    Modifier
                        .padding(pad)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(a.iconUrl, 92.dp)
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(a.name, style = MaterialTheme.typography.headlineSmall)
                            Text("Grace of God Studio", color = Z.Lime, style = MaterialTheme.typography.bodyMedium)
                            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (!a.published) Pill("Brouillon", Z.Amber)
                                if (installedVersion != null) Pill("Installée", Z.Blue)
                            }
                        }
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Stat(a.versionName ?: "—", "Version")
                        Stat(formatSize(a.sizeBytes), "Taille")
                        Stat(formatPrice(a.priceCents, a.currency), "Prix")
                        Stat("${a.downloads}", "Téléch.")
                    }

                    AppActionButton(
                        app = a,
                        installedVersion = installedVersion,
                        state = state,
                        compact = false,
                        onInstall = { requestInstall(context, deps.installer, a) { askPermission = true } },
                        onOpen = { Pkg.open(context, a.packageName) },
                        onBuy = {
                            if (deps.user == null) onLogin()
                            else if (!buying) {
                                buying = true
                                scope.launch {
                                    deps.repo.checkout(a.id)
                                        .onSuccess { r ->
                                            if (r.owned) deps.repo.refreshCatalog()
                                            r.url?.let { openUrl(context, it) }
                                        }
                                        .onFailure { deps.repo.toast(it.message ?: "Paiement impossible") }
                                    buying = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state is InstallState.Failed) {
                        Surface(color = Z.Red.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(state.message, color = Z.Red, modifier = Modifier.weight(1f))
                                TextButton(onClick = { deps.installer.set(a.packageName, null) }) { Text("OK") }
                            }
                        }
                    }
                    if (installedVersion != null) {
                        OutlinedButton(
                            onClick = { Pkg.uninstall(context, a.packageName) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                        ) { Text("Désinstaller") }
                    }
                    if (!a.isFree && !a.owned) {
                        Text(
                            "Paiement sécurisé par Stripe. L'app reste liée à ton compte Z-Store.",
                            color = Z.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp),
                        )
                    }

                    if (!a.description.isNullOrBlank() || !a.shortDesc.isNullOrBlank()) {
                        SectionTitle("À propos")
                        Text(a.description?.takeIf { it.isNotBlank() } ?: a.shortDesc.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    }

                    versions.firstOrNull()?.changelog?.takeIf { it.isNotBlank() }?.let {
                        SectionTitle("Nouveautés")
                        Text(it, style = MaterialTheme.typography.bodyLarge)
                    }

                    if (versions.isNotEmpty()) {
                        SectionTitle("Versions")
                        versions.forEach { v ->
                            Row(Modifier.padding(vertical = 8.dp)) {
                                Text(v.versionName ?: "—", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(110.dp))
                                Text("code ${v.versionCode}", color = Z.Muted, modifier = Modifier.weight(1f))
                                Text(v.createdAt?.take(10) ?: "", color = Z.Muted)
                            }
                            HorizontalDivider(color = Z.Line)
                        }
                    }

                    SectionTitle("Infos")
                    Text(a.packageName, color = Z.Muted, style = MaterialTheme.typography.bodyMedium)
                    a.minSdk?.let { Text("Android ${sdkToAndroid(it)} ou plus récent", color = Z.Muted, style = MaterialTheme.typography.bodyMedium) }
                    Spacer(Modifier.size(40.dp))
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Text(label, color = Z.Muted, style = MaterialTheme.typography.labelSmall)
    }
}

fun sdkToAndroid(sdk: Int): String = when (sdk) {
    in 0..25 -> "7"
    26, 27 -> "8"
    28 -> "9"
    29 -> "10"
    30 -> "11"
    31, 32 -> "12"
    33 -> "13"
    34 -> "14"
    35 -> "15"
    else -> "16"
}

// ---------------------------------------------------------------- Mes apps

@Composable
fun LibraryScreen(deps: Deps, onOpenApp: (Int) -> Unit) {
    val context = LocalContext.current
    val catalog by deps.repo.catalog.collectAsState()
    val states by deps.installer.states.collectAsState()
    val tick by deps.installer.installedTick.collectAsState()
    var askPermission by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (catalog.isEmpty()) deps.repo.refreshCatalog() }

    val installed = remember(catalog, tick) { Pkg.installedMap(context, catalog.map { it.packageName }) }
    val mine = catalog.filter { installed.containsKey(it.packageName) }
    val updates = mine.filter { it.owned && (it.versionCode ?: 0L) > (installed[it.packageName] ?: Long.MAX_VALUE) }
    val notInstalledOwned = catalog.filter { !it.isFree && it.owned && !installed.containsKey(it.packageName) && it.hasVersion }

    if (askPermission) {
        InstallPermissionDialog(
            onDismiss = { askPermission = false },
            onOpenSettings = { askPermission = false; context.startActivity(deps.installer.installPermissionIntent()) },
        )
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp)) {
                Text("Mes apps", style = MaterialTheme.typography.displaySmall)
                if (updates.isNotEmpty()) {
                    Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${updates.size} mise${if (updates.size > 1) "s" else ""} à jour disponible${if (updates.size > 1) "s" else ""}",
                            color = Z.Lime, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                        )
                        Button(onClick = {
                            if (!deps.installer.canInstall()) askPermission = true
                            else updates.forEach { deps.installer.install(it) }
                        }) { Text("Tout mettre à jour") }
                    }
                }
            }
        }
        if (mine.isEmpty() && notInstalledOwned.isEmpty()) {
            item {
                Box(Modifier.padding(top = 60.dp)) {
                    CenterMessage("Aucune app installée", "Les apps que tu installes depuis Z-Store apparaîtront ici avec leurs mises à jour.")
                }
            }
        }
        if (mine.isNotEmpty()) item { SectionTitle("Installées", Modifier.padding(horizontal = 20.dp)) }
        items(mine.sortedByDescending { a -> updates.any { it.id == a.id } }, key = { "i${it.id}" }) { app ->
            AppRow(
                app, installed[app.packageName], states[app.packageName],
                onClick = { onOpenApp(app.id) },
                onInstall = { requestInstall(context, deps.installer, app) { askPermission = true } },
                onOpen = { Pkg.open(context, app.packageName) },
                onBuy = { onOpenApp(app.id) },
            )
        }
        if (notInstalledOwned.isNotEmpty()) item { SectionTitle("Achetées, pas installées", Modifier.padding(horizontal = 20.dp)) }
        items(notInstalledOwned, key = { "o${it.id}" }) { app ->
            AppRow(
                app, null, states[app.packageName],
                onClick = { onOpenApp(app.id) },
                onInstall = { requestInstall(context, deps.installer, app) { askPermission = true } },
                onOpen = { Pkg.open(context, app.packageName) },
                onBuy = { onOpenApp(app.id) },
            )
        }
    }
}
