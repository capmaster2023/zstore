package com.graceofgod.zstore.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.graceofgod.zstore.data.AppUpdateBody
import com.graceofgod.zstore.data.AppVersion
import com.graceofgod.zstore.data.Build
import com.graceofgod.zstore.data.SalesResponse
import com.graceofgod.zstore.data.StoreApp
import com.graceofgod.zstore.ui.theme.Mono
import com.graceofgod.zstore.ui.theme.Z
import com.graceofgod.zstore.util.ApkInfo
import com.graceofgod.zstore.util.Pkg
import com.graceofgod.zstore.util.centsToInput
import com.graceofgod.zstore.util.formatPrice
import com.graceofgod.zstore.util.formatSize
import com.graceofgod.zstore.util.openUrl
import com.graceofgod.zstore.util.parsePriceToCents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Z.Bg),
    )
}

// ---------------------------------------------------------------- Accueil admin

@Composable
fun AdminScreen(
    deps: Deps,
    onOpenAppAdmin: (Int) -> Unit,
    onUpload: () -> Unit,
    onNewBuild: () -> Unit,
    onOpenBuild: (Int) -> Unit,
    onNewAi: () -> Unit,
    onOpenAiJob: (Int) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        floatingActionButton = {
            when (tab) {
                0 -> ExtendedFloatingActionButton(onClick = onUpload, icon = { Icon(Icons.Filled.UploadFile, null) }, text = { Text("Ajouter un APK") })
                1 -> ExtendedFloatingActionButton(onClick = onNewAi, icon = { Icon(Icons.Filled.AutoAwesome, null) }, text = { Text("Créer avec Claude") })
                2 -> ExtendedFloatingActionButton(onClick = onNewBuild, icon = { Icon(Icons.Filled.Code, null) }, text = { Text("Compiler du code") })
                else -> Unit
            }
        },
        containerColor = Z.Bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        Column(Modifier.padding(pad)) {
            Text("Admin", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(start = 20.dp, top = 28.dp, bottom = 8.dp))
            TabRow(selectedTabIndex = tab, containerColor = Z.Bg) {
                listOf("Apps", "IA", "Builds", "Ventes").forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
                }
            }
            when (tab) {
                0 -> AdminAppsTab(deps, onOpenAppAdmin)
                1 -> AiTab(deps, onOpenAiJob)
                2 -> BuildsTab(deps, onOpenBuild)
                else -> SalesTab(deps)
            }
        }
    }
}

@Composable
private fun AdminAppsTab(deps: Deps, onOpen: (Int) -> Unit) {
    val catalog by deps.repo.catalog.collectAsState()
    LaunchedEffect(Unit) { deps.repo.refreshCatalog() }
    if (catalog.isEmpty()) {
        CenterMessage("Ta store est vide", "Ajoute un APK déjà compilé, ou envoie du code source pour que GitHub le compile.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        items(catalog, key = { it.id }) { a ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(a.id) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(a.iconUrl, 48.dp)
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(a.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${a.versionName ?: "—"} · ${a.downloads} téléch.", color = Z.Muted, style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (a.published) Pill("En ligne", Z.Lime) else Pill("Brouillon", Z.Amber)
                    Text(formatPrice(a.priceCents, a.currency), color = Z.Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun BuildsTab(deps: Deps, onOpen: (Int) -> Unit) {
    var builds by remember { mutableStateOf<List<Build>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            deps.repo.builds().onSuccess { builds = it; error = null }.onFailure { error = it.message }
            delay(if (builds?.any { it.isActive } == true) 5_000L else 30_000L)
        }
    }
    val list = builds
    when {
        list == null && error != null -> CenterMessage("Erreur", error)
        list == null -> Loading()
        list.isEmpty() -> CenterMessage("Aucune compilation", "Appuie sur « Compiler du code » et envoie le zip d'un projet Android ou Capacitor.")
        else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
            items(list, key = { it.id }) { b ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(b.id) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusDot(b.status)
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                    ) {
                        Text(b.appName ?: b.repo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "#${b.id} · ${b.createdAt?.take(16) ?: ""}" + (b.versionName?.let { " · v$it" } ?: ""),
                            color = Z.Muted, style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(statusLabel(b.status), color = statusColor(b.status), style = MaterialTheme.typography.labelLarge)
                }
                HorizontalDivider(color = Z.Line, modifier = Modifier.padding(start = 20.dp))
            }
        }
    }
}

@Composable
private fun SalesTab(deps: Deps) {
    var data by remember { mutableStateOf<SalesResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { deps.repo.sales().onSuccess { data = it }.onFailure { error = it.message } }
    val d = data
    when {
        d == null && error != null -> CenterMessage("Erreur", error)
        d == null -> Loading()
        else -> LazyColumn(contentPadding = PaddingValues(20.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val total = d.totals.sumOf { it.amountCents }
                    BigStat(formatPrice(total, d.totals.firstOrNull()?.currency ?: "cad").replace("Gratuit", "0 $"), "Revenus", Modifier.weight(1f))
                    BigStat("${d.totals.sumOf { it.count }}", "Ventes", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                    BigStat("${d.downloads}", "Téléchargements", Modifier.weight(1f))
                    BigStat("${d.users}", "Comptes", Modifier.weight(1f))
                }
                SectionTitle("Dernières ventes")
                if (d.sales.isEmpty()) Text("Aucune vente pour l'instant.", color = Z.Muted)
            }
            items(d.sales, key = { it.id }) { s ->
                Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.appName, style = MaterialTheme.typography.titleMedium)
                        Text("${s.email} · ${s.createdAt.take(10)}", color = Z.Muted, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(formatPrice(s.amountCents, s.currency), color = Z.Lime, style = MaterialTheme.typography.titleMedium)
                }
                HorizontalDivider(color = Z.Line)
            }
        }
    }
}

@Composable
private fun BigStat(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(color = Z.Surface, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
            Text(label, color = Z.Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun statusLabel(s: String) = when (s) {
    "queued" -> "En attente"
    "running" -> "Compile…"
    "success" -> "Réussi"
    else -> "Échec"
}

fun statusColor(s: String) = when (s) {
    "queued" -> Z.Muted
    "running" -> Z.Blue
    "success" -> Z.Lime
    else -> Z.Red
}

@Composable
private fun StatusDot(status: String) {
    val active = status == "queued" || status == "running"
    val alpha = if (active) {
        val t = rememberInfiniteTransition(label = "pulse")
        t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a").value
    } else 1f
    Box(
        Modifier
            .size(12.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(statusColor(status))
    )
}

// ---------------------------------------------------------------- Ajouter un APK

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadApkScreen(deps: Deps, appId: Int?, onBack: () -> Unit, onDone: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val catalog by deps.repo.catalog.collectAsState()
    val target = appId?.let { id -> catalog.firstOrNull { it.id == id } }

    var file by remember { mutableStateOf<File?>(null) }
    var info by remember { mutableStateOf<ApkInfo?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf(target?.name ?: "") }
    var shortDesc by rememberSaveable { mutableStateOf(target?.shortDesc ?: "") }
    var description by rememberSaveable { mutableStateOf(target?.description ?: "") }
    var price by rememberSaveable { mutableStateOf(target?.let { centsToInput(it.priceCents) } ?: "") }
    var published by rememberSaveable { mutableStateOf(target?.published ?: true) }
    var changelog by rememberSaveable { mutableStateOf("") }
    var progress by remember { mutableStateOf<Float?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val existing = info?.let { i -> catalog.firstOrNull { it.packageName == i.packageName } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            parseError = null
            error = null
            val f = runCatching { deps.repo.copyToCache(uri, "upload.apk") }.getOrNull()
            val parsed = f?.let { withContext(Dispatchers.IO) { Pkg.parseApk(context, it) } }
            if (parsed == null) {
                parseError = "Ce fichier n'est pas un APK valide."
                file = null; info = null
                return@launch
            }
            if (target != null && target.packageName != parsed.packageName) {
                parseError = "Cet APK (${parsed.packageName}) n'est pas « ${target.name} » (${target.packageName})."
                file = null; info = null
                return@launch
            }
            file = f
            info = parsed
            val ex = catalog.firstOrNull { it.packageName == parsed.packageName }
            if (ex != null) {
                name = ex.name; shortDesc = ex.shortDesc ?: ""; description = ex.description ?: ""
                price = centsToInput(ex.priceCents); published = ex.published
            } else if (name.isBlank()) {
                name = parsed.label
            }
        }
    }

    Scaffold(topBar = { BackBar(if (target != null) "Nouvelle version" else "Ajouter un APK", onBack) }, containerColor = Z.Bg) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            val i = info
            Surface(
                onClick = { picker.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream", "*/*")) },
                color = Z.Surface,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    val iconBytes = i?.iconPng
                    if (iconBytes != null) {
                        val bmp = remember(iconBytes) { BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.size)?.asImageBitmap() }
                        if (bmp != null) Image(bmp, null, Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)))
                    } else {
                        Icon(Icons.Filled.UploadFile, null, tint = Z.Lime, modifier = Modifier.size(40.dp))
                    }
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        if (i == null) {
                            Text("Choisir un fichier APK", style = MaterialTheme.typography.titleMedium)
                            Text("Le package, la version et l'icône sont lus automatiquement.", color = Z.Muted, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(i.label, style = MaterialTheme.typography.titleMedium)
                            Text(i.packageName, color = Z.Muted, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "v${i.versionName ?: "?"} (code ${i.versionCode}) · ${formatSize(file?.length())}",
                                color = Z.Muted, style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            parseError?.let { Text(it, color = Z.Red, modifier = Modifier.padding(top = 8.dp)) }
            if (i != null && existing != null) {
                val newer = i.versionCode > (existing.versionCode ?: 0L)
                Text(
                    if (newer) "Mise à jour de « ${existing.name} » (actuellement code ${existing.versionCode ?: "—"})."
                    else "⚠ versionCode ${i.versionCode} n'est pas plus grand que la version publiée (${existing.versionCode}). Les utilisateurs ne verront pas de mise à jour.",
                    color = if (newer) Z.Lime else Z.Amber,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            SectionTitle("Fiche dans la store")
            Field(name, { name = it }, "Nom de l'app")
            Field(shortDesc, { shortDesc = it }, "Description courte")
            Field(description, { description = it }, "Description complète", singleLine = false, minLines = 4)
            Field(price, { price = it }, "Prix en $ (vide = gratuit)", keyboard = KeyboardType.Decimal)
            Field(changelog, { changelog = it }, "Nouveautés de cette version", singleLine = false, minLines = 2)
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Publier dans la store", style = MaterialTheme.typography.titleMedium)
                    Text("Sinon l'app reste en brouillon, visible par toi seulement.", color = Z.Muted, style = MaterialTheme.typography.bodyMedium)
                }
                Switch(checked = published, onCheckedChange = { published = it })
            }

            error?.let { Text(it, color = Z.Red, modifier = Modifier.padding(bottom = 8.dp)) }
            val p = progress
            if (p != null) {
                LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                Text(
                    if (p >= 1f) "Enregistrement sur le serveur…" else "Envoi ${(p * 100).toInt()} %",
                    color = Z.Muted, modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                Button(
                    enabled = i != null && file != null,
                    onClick = {
                        val cents = parsePriceToCents(price)
                        if (cents == null || (cents in 1..49)) {
                            error = "Prix invalide (minimum 0,50 $, ou vide pour gratuit)"; return@Button
                        }
                        val f = file ?: return@Button
                        val inf = info ?: return@Button
                        error = null
                        progress = 0f
                        scope.launch {
                            deps.repo.uploadApk(
                                f, inf, appId, name.ifBlank { inf.label }, shortDesc, description, cents, published, changelog,
                            ) { v -> scope.launch { progress = v } }
                                .onSuccess { deps.repo.toast("« ${it.app.name} » v${inf.versionName ?: inf.versionCode} publié ✔"); onDone(it.app.id) }
                                .onFailure { error = it.message }
                            progress = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) { Text("Envoyer dans Z-Store") }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
    )
}

// ---------------------------------------------------------------- Compiler du code

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBuildScreen(deps: Deps, appId: Int?, onBack: () -> Unit, onStarted: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    val catalog by deps.repo.catalog.collectAsState()
    var selected by remember { mutableStateOf<StoreApp?>(appId?.let { id -> catalog.firstOrNull { it.id == id } }) }
    var repo by rememberSaveable { mutableStateOf(selected?.let { it.githubRepo ?: slug(it.name) } ?: "") }
    var changelog by rememberSaveable { mutableStateOf("") }
    var private by rememberSaveable { mutableStateOf(true) }
    var zip by remember { mutableStateOf<File?>(null) }
    var zipName by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<Float?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { deps.repo.copyToCache(uri, "source.zip") }
                .onSuccess { zip = it; zipName = uri.lastPathSegment?.substringAfterLast('/') ?: "source.zip" }
                .onFailure { error = it.message }
        }
    }

    Scaffold(topBar = { BackBar("Compiler du code", onBack) }, containerColor = Z.Bg) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "Envoie le zip d'un projet Android (Gradle) ou Capacitor. GitHub le compile, le signe avec ta clé, et l'APK arrive dans ta store.",
                color = Z.Muted,
            )

            SectionTitle("App")
            Box {
                Surface(onClick = { menu = true }, color = Z.Surface, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        val s = selected
                        if (s != null) AppIcon(s.iconUrl, 32.dp)
                        Text(
                            s?.name ?: "Nouvelle app",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = if (s != null) 12.dp else 0.dp).weight(1f),
                        )
                        Text("Changer", color = Z.Lime, style = MaterialTheme.typography.labelLarge)
                    }
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Nouvelle app") }, onClick = { selected = null; repo = ""; menu = false })
                    catalog.forEach { a ->
                        DropdownMenuItem(text = { Text(a.name) }, onClick = {
                            selected = a; repo = a.githubRepo ?: slug(a.name); menu = false
                        })
                    }
                }
            }

            SectionTitle("Repo GitHub")
            val locked = selected?.githubRepo != null
            OutlinedTextField(
                value = repo,
                onValueChange = { repo = slug(it, keepTrailing = true) },
                enabled = !locked,
                prefix = { Text("capmaster2023/", color = Z.Muted) },
                singleLine = true,
                supportingText = {
                    Text(if (locked) "Cette app compile toujours dans ce repo." else "Créé automatiquement s'il n'existe pas.")
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (!locked) {
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Repo privé", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Switch(checked = private, onCheckedChange = { private = it })
                }
            }

            SectionTitle("Code source")
            Surface(
                onClick = { picker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                color = Z.Surface, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Code, null, tint = Z.Lime)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(zipName ?: "Choisir le zip du projet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            zip?.let { formatSize(it.length()) } ?: "node_modules, build et .gradle sont ignorés automatiquement",
                            color = Z.Muted, style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Field(changelog, { changelog = it }, "Nouveautés (optionnel)", singleLine = false, minLines = 2)
            Text(
                "Rappel : augmente versionCode dans app/build.gradle à chaque nouvelle version.",
                color = Z.Muted, style = MaterialTheme.typography.bodyMedium,
            )

            error?.let { Text(it, color = Z.Red, modifier = Modifier.padding(top = 10.dp)) }
            Spacer(Modifier.height(16.dp))
            val p = progress
            if (p != null) {
                LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                Text(
                    if (p >= 1f) "Envoi du code vers GitHub…" else "Envoi au serveur ${(p * 100).toInt()} %",
                    color = Z.Muted, modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                Button(
                    enabled = zip != null && repo.isNotBlank(),
                    onClick = {
                        val z = zip ?: return@Button
                        error = null
                        progress = 0f
                        scope.launch {
                            deps.repo.createBuild(z, repo.trim('-'), selected?.id, changelog, private) { v -> scope.launch { progress = v } }
                                .onSuccess { onStarted(it.id) }
                                .onFailure { error = it.message }
                            progress = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) { Text("Envoyer et compiler") }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

fun slug(s: String, keepTrailing: Boolean = false): String {
    val r = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9._-]+"), "-")
    return if (keepTrailing) r.trimStart('-') else r.trim('-')
}

// ---------------------------------------------------------------- Détail d'un build

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildDetailScreen(deps: Deps, buildId: Int, onBack: () -> Unit, onOpenApp: (Int) -> Unit, onRebuild: (Int?) -> Unit, onFixWithAi: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var build by remember { mutableStateOf<Build?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(buildId, refreshKey) {
        while (true) {
            deps.repo.build(buildId).onSuccess { build = it; error = null }.onFailure { error = it.message }
            val b = build
            if (b != null && !b.isActive) {
                if (b.status == "success") deps.repo.refreshCatalog()
                break
            }
            delay(4_000L)
        }
    }

    Scaffold(topBar = {
        BackBar("Build #$buildId", onBack) {
            build?.runUrl?.let { url -> IconButton(onClick = { openUrl(context, url) }) { Icon(Icons.Filled.OpenInNew, "Voir sur GitHub") } }
        }
    }, containerColor = Z.Bg) { pad ->
        val b = build
        when {
            b == null && error != null -> Box(Modifier.padding(pad)) { CenterMessage("Erreur", error) }
            b == null -> Box(Modifier.padding(pad)) { Loading() }
            else -> Column(
                Modifier
                    .padding(pad)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Text(b.appName ?: b.repo, style = MaterialTheme.typography.headlineSmall)
                Text("capmaster2023/${b.repo}" + (b.commitSha?.let { " · ${it.take(7)}" } ?: ""), color = Z.Muted)

                Surface(color = Z.Surface, shape = RoundedCornerShape(18.dp), modifier = Modifier.padding(top = 18.dp)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Step("Code envoyé sur GitHub", done = b.commitSha != null, active = false, failed = b.commitSha == null && b.status == "failure")
                        Step(
                            "Compilation", done = b.status == "success",
                            active = b.isActive, failed = b.commitSha != null && b.status == "failure",
                        )
                        Step("Signature et publication dans Z-Store", done = b.status == "success", active = false, failed = false, last = true)
                    }
                }

                when (b.status) {
                    "success" -> {
                        Surface(color = Z.Lime.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(top = 16.dp)) {
                            Column(Modifier.padding(16.dp).fillMaxWidth()) {
                                Text("APK prêt ✔", color = Z.Lime, style = MaterialTheme.typography.titleLarge)
                                Text("Version ${b.versionName ?: "?"} (code ${b.versionCode ?: "?"}) ajoutée à « ${b.appName ?: b.repo} ».")
                                b.appId?.let { id ->
                                    Button(onClick = { onOpenApp(id) }, modifier = Modifier.padding(top = 12.dp)) { Text("Voir / publier l'app") }
                                }
                            }
                        }
                    }
                    "failure" -> {
                        Row(Modifier.padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Erreurs", style = MaterialTheme.typography.titleLarge, color = Z.Red, modifier = Modifier.weight(1f))
                            IconButton(onClick = { copy(context, b.errorLog ?: ""); deps.repo.toast("Erreurs copiées") }) {
                                Icon(Icons.Filled.ContentCopy, "Copier")
                            }
                        }
                        Surface(color = Color(0xFF0A0B0D), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            SelectionContainer {
                                Text(
                                    highlightLog(b.errorLog ?: "Aucun détail."),
                                    fontFamily = Mono, fontSize = 11.5.sp, lineHeight = 16.sp,
                                    modifier = Modifier
                                        .horizontalScroll(rememberScrollState())
                                        .padding(14.dp),
                                )
                            }
                        }
                        Button(
                            onClick = { onFixWithAi(b.id) },
                            enabled = b.commitSha != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                                .height(50.dp),
                        ) {
                            Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp))
                            Text("  Corriger avec Claude")
                        }
                        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    deps.repo.retryBuild(b.id).onSuccess { build = it; refreshKey++ }.onFailure { deps.repo.toast(it.message ?: "Erreur") }
                                }
                            }) {
                                Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp)); Text("  Relancer")
                            }
                            OutlinedButton(onClick = { onRebuild(b.appId) }) { Text("Envoyer un zip corrigé") }
                        }
                    }
                    else -> {
                        Text(
                            if (b.status == "queued") "En attente d'une machine GitHub… (souvent moins d'une minute)"
                            else "Compilation en cours. Un projet Android prend en général 3 à 8 minutes. Tu peux quitter cet écran.",
                            color = Z.Muted, modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun Step(label: String, done: Boolean, active: Boolean, failed: Boolean, last: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = if (last) 0.dp else 14.dp)) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            when {
                active -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Z.Blue)
                failed -> Box(Modifier.size(14.dp).clip(CircleShape).background(Z.Red))
                done -> Box(Modifier.size(14.dp).clip(CircleShape).background(Z.Lime))
                else -> Box(Modifier.size(14.dp).clip(CircleShape).background(Z.Line))
            }
        }
        Text(
            label,
            modifier = Modifier.padding(start = 12.dp),
            color = when {
                failed -> Z.Red
                done || active -> Z.Text
                else -> Z.Muted
            },
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private fun highlightLog(log: String) = androidx.compose.ui.text.buildAnnotatedString {
    log.lines().forEachIndexed { i, line ->
        val isErr = Regex("^(\\d+:)?(e: |.*error:|.*ERROR|.*FAILURE|.*What went wrong|.*Unresolved reference|.*Exception)", RegexOption.IGNORE_CASE)
            .containsMatchIn(line)
        val isHeader = line.startsWith("===")
        val color = when {
            isHeader -> Z.Lime
            isErr -> Color(0xFFFF8A8A)
            else -> Color(0xFFB8BDC7)
        }
        pushStyle(androidx.compose.ui.text.SpanStyle(color = color))
        append(line)
        pop()
        if (i < log.lines().lastIndex) append('\n')
    }
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Erreurs Z-Store", text))
}

// ---------------------------------------------------------------- Modifier une app

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAppEditScreen(
    deps: Deps,
    appId: Int,
    onBack: () -> Unit,
    onNewApk: (Int) -> Unit,
    onNewBuild: (Int) -> Unit,
    onDeleted: () -> Unit,
    onEditWithAi: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var app by remember { mutableStateOf<StoreApp?>(null) }
    var versions by remember { mutableStateOf<List<AppVersion>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var shortDesc by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var published by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(appId) {
        deps.repo.appDetail(appId).onSuccess {
            app = it.app; versions = it.versions
            name = it.app.name; shortDesc = it.app.shortDesc ?: ""; description = it.app.description ?: ""
            price = centsToInput(it.app.priceCents); published = it.app.published
        }.onFailure { error = it.message }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Supprimer « $name » ?",
            text = "L'app, toutes ses versions et ses APK seront supprimés de Z-Store. Les achats associés aussi. Le repo GitHub n'est pas touché.",
            confirm = "Supprimer",
            danger = true,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                scope.launch { deps.repo.deleteApp(appId).onSuccess { deps.repo.toast("App supprimée"); onDeleted() }.onFailure { error = it.message } }
            },
        )
    }

    Scaffold(topBar = {
        BackBar("Modifier", onBack) {
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Supprimer", tint = Z.Red) }
        }
    }, containerColor = Z.Bg) { pad ->
        val a = app
        if (a == null) {
            Box(Modifier.padding(pad)) { if (error != null) CenterMessage("Erreur", error) else Loading() }
            return@Scaffold
        }
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(a.iconUrl, 64.dp)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(a.packageName, color = Z.Muted, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${a.versionName ?: "aucune version"} · ${a.downloads} téléch." + (a.githubRepo?.let { " · repo $it" } ?: ""),
                        color = Z.Muted, style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onNewApk(appId) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.UploadFile, null, Modifier.size(18.dp)); Text(" APK")
                }
                OutlinedButton(onClick = { onNewBuild(appId) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Code, null, Modifier.size(18.dp)); Text(" Compiler")
                }
            }
            if (a.githubRepo != null) {
                Button(onClick = { onEditWithAi(appId) }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp)); Text("  Modifier avec Claude")
                }
            }

            SectionTitle("Fiche")
            Field(name, { name = it }, "Nom")
            Field(shortDesc, { shortDesc = it }, "Description courte")
            Field(description, { description = it }, "Description complète", singleLine = false, minLines = 4)
            Field(price, { price = it }, "Prix en $ (vide = gratuit)", keyboard = KeyboardType.Decimal)
            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Publiée dans la store", style = MaterialTheme.typography.titleMedium)
                    Text(if (a.hasVersion) "Visible par tout le monde" else "Ajoute une version avant de publier", color = Z.Muted, style = MaterialTheme.typography.bodyMedium)
                }
                Switch(checked = published, onCheckedChange = { published = it }, enabled = a.hasVersion)
            }
            error?.let { Text(it, color = Z.Red, modifier = Modifier.padding(vertical = 6.dp)) }
            Button(
                enabled = !saving && name.isNotBlank(),
                onClick = {
                    val cents = parsePriceToCents(price)
                    if (cents == null || cents in 1..49) {
                        error = "Prix invalide (minimum 0,50 $, ou vide pour gratuit)"; return@Button
                    }
                    saving = true
                    error = null
                    scope.launch {
                        deps.repo.updateApp(appId, AppUpdateBody(name.trim(), shortDesc.trim(), description.trim(), cents, published))
                            .onSuccess { app = it.app; deps.repo.toast("Enregistré ✔") }
                            .onFailure { error = it.message }
                        saving = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(50.dp),
            ) { Text(if (saving) "Enregistrement…" else "Enregistrer") }

            if (versions.isNotEmpty()) {
                SectionTitle("Versions")
                versions.forEach { v ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Row {
                            Text(v.versionName ?: "—", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text("code ${v.versionCode} · ${formatSize(v.sizeBytes)}", color = Z.Muted)
                        }
                        v.changelog?.takeIf { it.isNotBlank() }?.let { Text(it, color = Z.Muted, style = MaterialTheme.typography.bodyMedium) }
                    }
                    HorizontalDivider(color = Z.Line)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
