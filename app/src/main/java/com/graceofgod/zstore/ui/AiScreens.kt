package com.graceofgod.zstore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.graceofgod.zstore.data.AiJob
import com.graceofgod.zstore.data.AiJobBody
import com.graceofgod.zstore.data.AiModel
import com.graceofgod.zstore.data.StoreApp
import com.graceofgod.zstore.ui.theme.Z
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun effortLabel(e: String) = when (e) {
    "low" -> "Basse"
    "medium" -> "Moyenne"
    "high" -> "Haute"
    "xhigh" -> "Très haute"
    "max" -> "Max"
    else -> e
}

fun effortHint(e: String) = when (e) {
    "low" -> "Très rapide, pour les petites retouches"
    "medium" -> "Bon équilibre vitesse / qualité"
    "high" -> "Réfléchit bien avant d'écrire (recommandé)"
    "xhigh" -> "Pour les apps complexes, plus lent"
    "max" -> "Réflexion maximale, le plus lent et le plus cher"
    else -> ""
}

fun aiStatusLabel(s: String) = when (s) {
    "queued" -> "En attente"
    "thinking" -> "Réflexion"
    "writing" -> "Écrit le code"
    "pushing" -> "Envoi GitHub"
    "done" -> "Code envoyé"
    else -> "Échec"
}

// ---------------------------------------------------------------- Onglet IA (liste)

@Composable
fun AiTab(deps: Deps, onOpenJob: (Int) -> Unit) {
    var jobs by remember { mutableStateOf<List<AiJob>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            deps.repo.aiJobs().onSuccess { jobs = it; error = null }.onFailure { error = it.message }
            delay(if (jobs?.any { it.isActive } == true) 4_000L else 30_000L)
        }
    }
    val list = jobs
    when {
        list == null && error != null -> CenterMessage("Erreur", error)
        list == null -> Loading()
        list.isEmpty() -> CenterMessage(
            "Crée ta première app avec Claude",
            "Décris l'app que tu veux : Claude écrit tout le code, GitHub le compile, et l'APK arrive dans ta store.",
        )
        else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
            items(list, key = { it.id }) { j ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenJob(j.id) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = if (j.status == "failure") Z.Red else Z.Lime)
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                    ) {
                        Text(
                            j.appName ?: j.prompt.ifBlank { "Correction" },
                            style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${modeLabel(j.mode)} · ${j.modelLabel} · ${effortLabel(j.effort)}",
                            color = Z.Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                        )
                    }
                    val (label, color) = jobBadge(j)
                    Text(label, color = color, style = MaterialTheme.typography.labelLarge)
                }
                HorizontalDivider(color = Z.Line, modifier = Modifier.padding(start = 20.dp))
            }
        }
    }
}

private fun modeLabel(m: String) = when (m) {
    "create" -> "Nouvelle app"
    "modify" -> "Modification"
    else -> "Correction"
}

private fun jobBadge(j: AiJob): Pair<String, androidx.compose.ui.graphics.Color> = when {
    j.status == "failure" -> "Échec" to Z.Red
    j.isActive -> aiStatusLabel(j.status) to Z.Blue
    j.buildStatus == "success" -> "APK prêt" to Z.Lime
    j.buildStatus == "failure" -> "Build échoué" to Z.Red
    else -> "Compile…" to Z.Blue
}

// ---------------------------------------------------------------- Nouvelle demande

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiNewScreen(deps: Deps, appId: Int?, buildId: Int?, onBack: () -> Unit, onStarted: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    val catalog by deps.repo.catalog.collectAsState()
    var models by remember { mutableStateOf<List<AiModel>>(emptyList()) }
    var configured by remember { mutableStateOf(true) }
    var modelId by rememberSaveable { mutableStateOf("claude-opus-5-5") }
    var effort by rememberSaveable { mutableStateOf("high") }
    var target by remember { mutableStateOf<StoreApp?>(appId?.let { id -> catalog.firstOrNull { it.id == id } }) }
    var prompt by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    val fix = buildId != null

    LaunchedEffect(Unit) {
        deps.repo.aiModels().onSuccess { r ->
            models = r.models
            configured = r.configured
            r.models.firstOrNull { it.id == modelId }?.let { if (effort !in it.efforts) effort = it.defaultEffort }
        }.onFailure { error = it.message }
    }
    val model = models.firstOrNull { it.id == modelId }
    val editable = catalog.filter { it.githubRepo != null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (fix) "Corriger avec Claude" else "Créer avec Claude", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Z.Bg),
            )
        },
        containerColor = Z.Bg,
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            if (!configured) {
                Surface(color = Z.Amber.copy(alpha = 0.12f), shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(
                        "Ajoute ta clé API Claude (anthropic_api_key) dans config.php sur le serveur pour activer l'IA.",
                        color = Z.Amber, modifier = Modifier.padding(14.dp),
                    )
                }
            }

            if (fix) {
                Text(
                    "Claude va lire le code du repo et les erreurs du build #$buildId, puis envoyer une version corrigée qui sera recompilée automatiquement.",
                    color = Z.Muted,
                )
            } else {
                SectionTitle("Quoi ?")
                Box {
                    Surface(onClick = { menu = true }, color = Z.Surface, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            val t = target
                            if (t != null) AppIcon(t.iconUrl, 32.dp)
                            Text(
                                t?.let { "Modifier « ${it.name} »" } ?: "Nouvelle app",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .padding(start = if (t != null) 12.dp else 0.dp)
                                    .weight(1f),
                            )
                            Text("Changer", color = Z.Lime, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Nouvelle app") }, onClick = { target = null; menu = false })
                        editable.forEach { a ->
                            DropdownMenuItem(text = { Text("Modifier « ${a.name} »") }, onClick = { target = a; menu = false })
                        }
                    }
                }
                if (editable.isEmpty()) {
                    Text(
                        "Seules les apps qui ont un repo GitHub (créées par l'IA ou compilées depuis du code) peuvent être modifiées.",
                        color = Z.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            SectionTitle(if (fix) "Précision (optionnel)" else "Décris ce que tu veux")
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                placeholder = {
                    Text(
                        when {
                            fix -> "Ex. : garde le même design, corrige seulement les erreurs"
                            target != null -> "Ex. : ajoute un mode sombre et un écran de paramètres"
                            else -> "Ex. : une app de minuteur de prière avec 3 durées, une sonnerie douce et un historique des sessions. Couleurs bleu nuit et or."
                        }
                    )
                },
                minLines = if (fix) 2 else 6,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("Modèle")
            if (models.isEmpty() && error == null) {
                CircularProgressIndicator(Modifier.size(24.dp))
            }
            models.forEach { m ->
                val selected = m.id == modelId
                Surface(
                    onClick = {
                        modelId = m.id
                        if (effort !in m.efforts) effort = m.defaultEffort
                    },
                    color = if (selected) Z.Lime.copy(alpha = 0.08f) else Z.Surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .border(1.dp, if (selected) Z.Lime else Z.Line, RoundedCornerShape(14.dp)),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (selected) Z.Lime else Z.Line)
                        )
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(m.label, style = MaterialTheme.typography.titleMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
                            Text(m.description, color = Z.Muted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (model != null) {
                SectionTitle("Réflexion")
                val idx = model.efforts.indexOf(effort).coerceAtLeast(0)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(effortLabel(effort), style = MaterialTheme.typography.titleLarge, color = Z.Lime, modifier = Modifier.weight(1f))
                    if (effort == model.defaultEffort) Pill("Par défaut", Z.Muted)
                }
                Slider(
                    value = idx.toFloat(),
                    onValueChange = { v -> effort = model.efforts[v.toInt().coerceIn(0, model.efforts.lastIndex)] },
                    valueRange = 0f..model.efforts.lastIndex.toFloat(),
                    steps = (model.efforts.size - 2).coerceAtLeast(0),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    model.efforts.forEach { Text(effortLabel(it), color = Z.Muted, style = MaterialTheme.typography.labelSmall) }
                }
                Text(effortHint(effort), color = Z.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            }

            error?.let { Text(it, color = Z.Red, modifier = Modifier.padding(top = 12.dp)) }
            Spacer(Modifier.height(20.dp))
            Button(
                enabled = !busy && configured && model != null && (fix || prompt.isNotBlank()),
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        deps.repo.createAiJob(
                            AiJobBody(prompt.trim(), modelId, effort, appId = if (fix) null else target?.id, buildId = buildId)
                        ).onSuccess { onStarted(it.id) }.onFailure { error = it.message }
                        busy = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else {
                    Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp))
                    Text(if (fix) "  Corriger" else if (target != null) "  Modifier l'app" else "  Créer l'app")
                }
            }
            Text(
                "Le coût dépend du modèle et de la réflexion (facturé sur ton compte API Anthropic).",
                color = Z.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp),
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ---------------------------------------------------------------- Suivi d'une demande

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiJobScreen(
    deps: Deps,
    jobId: Int,
    onBack: () -> Unit,
    onOpenBuild: (Int) -> Unit,
    onFix: (Int) -> Unit,
) {
    var job by remember { mutableStateOf<AiJob?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(jobId) {
        while (true) {
            deps.repo.aiJob(jobId).onSuccess { job = it; error = null }.onFailure { error = it.message }
            val j = job
            // Continue à suivre tant que le code ou la compilation tourne
            if (j != null && !j.isActive && (j.status == "failure" || j.buildStatus == "success" || j.buildStatus == "failure")) {
                if (j.buildStatus == "success") deps.repo.refreshCatalog()
                break
            }
            delay(3_000L)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Claude #$jobId", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Z.Bg),
            )
        },
        containerColor = Z.Bg,
    ) { pad ->
        val j = job
        when {
            j == null && error != null -> Box(Modifier.padding(pad)) { CenterMessage("Erreur", error) }
            j == null -> Box(Modifier.padding(pad)) { Loading() }
            else -> Column(
                Modifier
                    .padding(pad)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Text(j.appName ?: modeLabel(j.mode), style = MaterialTheme.typography.headlineSmall)
                Text("${j.modelLabel} · réflexion ${effortLabel(j.effort).lowercase()}", color = Z.Muted)
                if (j.prompt.isNotBlank()) {
                    Surface(color = Z.Surface, shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(top = 14.dp)) {
                        Text(j.prompt, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                val order = listOf("queued", "thinking", "writing", "pushing", "done")
                val pos = order.indexOf(j.status)
                val failed = j.status == "failure"
                Surface(color = Z.Surface, shape = RoundedCornerShape(18.dp), modifier = Modifier.padding(top = 16.dp)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        AiStep("Réflexion", done = pos > 1, active = pos in 0..1)
                        AiStep("Écriture du code", done = pos > 2, active = pos == 2)
                        AiStep("Envoi sur GitHub", done = pos > 3, active = pos == 3)
                        AiStep(
                            "Compilation de l'APK",
                            done = j.buildStatus == "success",
                            active = pos == 4 && (j.buildStatus == null || j.buildStatus == "queued" || j.buildStatus == "running"),
                            failed = j.buildStatus == "failure",
                            last = true,
                        )
                        if (j.isActive && j.progress != null) {
                            Text(j.progress, color = Z.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
                        }
                        if (failed) {
                            Text(j.error ?: "Échec", color = Z.Red, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }

                j.summary?.takeIf { it.isNotBlank() }?.let {
                    SectionTitle("Ce que Claude a fait")
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
                if (j.files.isNotEmpty()) {
                    SectionTitle("Fichiers (${j.files.size})")
                    j.files.forEach { f -> Text(f, color = Z.Muted, style = MaterialTheme.typography.bodyMedium, fontFamily = com.graceofgod.zstore.ui.theme.Mono) }
                }
                if (j.inputTokens + j.outputTokens > 0) {
                    Text(
                        "Tokens : ${j.inputTokens} en entrée · ${j.outputTokens} en sortie",
                        color = Z.Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 14.dp),
                    )
                }

                Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    j.buildId?.let { bid ->
                        OutlinedButton(onClick = { onOpenBuild(bid) }) { Text("Voir le build") }
                        if (j.buildStatus == "failure") {
                            Button(onClick = { onFix(bid) }) {
                                Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp))
                                Text("  Corriger avec Claude")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun AiStep(label: String, done: Boolean, active: Boolean, failed: Boolean = false, last: Boolean = false) {
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
