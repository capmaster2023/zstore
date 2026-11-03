package com.graceofgod.ztv

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private val Bg = Color(0xFF0E0F12)
private val Surface1 = Color(0xFF16181D)
private val Surface2 = Color(0xFF1E2128)
private val Lime = Color(0xFFC6FF3D)
private val Muted = Color(0xFF9AA0AC)
private val Red = Color(0xFFFF5C5C)

private val Colors = darkColorScheme(
    primary = Lime,
    onPrimary = Bg,
    background = Bg,
    surface = Bg,
    surfaceVariant = Surface1,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    onSurface = Color(0xFFF2F3F5),
    onSurfaceVariant = Muted,
    outline = Color(0xFF2A2E37),
    error = Red,
)

@Composable
fun ZtvRoot(app: ZtvApp) {
    MaterialTheme(colorScheme = Colors) {
        val nav = rememberNavController()
        val selectedId by app.store.selectedId.collectAsState()
        LaunchedEffect(selectedId) {
            if (selectedId != null) app.repo.load(force = false)
        }
        val play: (PlayRequest) -> Unit = { req ->
            app.repo.playRequest = req
            nav.navigate("player")
        }
        NavHost(navController = nav, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    app,
                    onOpenCategory = { kind, group -> nav.navigate("cat/${kind.name}/${Uri.encode(group)}") },
                    onOpenLists = { nav.navigate("lists") },
                    onAddList = { nav.navigate("edit") },
                    onPlay = play,
                )
            }
            composable(
                "cat/{kind}/{group}",
                listOf(
                    navArgument("kind") { type = NavType.StringType },
                    navArgument("group") { type = NavType.StringType },
                ),
            ) { e ->
                val kind = runCatching { Kind.valueOf(e.arguments?.getString("kind") ?: "") }.getOrDefault(Kind.LIVE)
                val group = e.arguments?.getString("group").orEmpty()
                CategoryScreen(
                    app, kind, group,
                    onBack = { nav.popBackStack() },
                    onPlay = play,
                    onOpenShow = { title -> nav.navigate("show/${Uri.encode(group)}/${Uri.encode(title)}") },
                )
            }
            composable(
                "show/{group}/{title}",
                listOf(
                    navArgument("group") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                ),
            ) { e ->
                ShowScreen(
                    app,
                    group = e.arguments?.getString("group").orEmpty(),
                    title = e.arguments?.getString("title").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onPlay = play,
                )
            }
            composable("player") { PlayerScreen(app, onBack = { nav.popBackStack() }) }
            composable("lists") {
                ListsScreen(
                    app,
                    onBack = { nav.popBackStack() },
                    onAdd = { nav.navigate("edit") },
                    onEdit = { id -> nav.navigate("edit?id=$id") },
                )
            }
            composable(
                "edit?id={id}",
                listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { e ->
                EditListScreen(app, e.arguments?.getString("id"), onDone = { nav.popBackStack() })
            }
        }
    }
}

// ---------------------------------------------------------------- Accueil

private const val TAB_FAV = "FAV"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    app: ZtvApp,
    onOpenCategory: (Kind, String) -> Unit,
    onOpenLists: () -> Unit,
    onAddList: () -> Unit,
    onPlay: (PlayRequest) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state by app.repo.state.collectAsState()
    val sources by app.store.sources.collectAsState()
    val selectedId by app.store.selectedId.collectAsState()
    val favorites by app.store.favorites.collectAsState()
    var tab by rememberSaveable { mutableStateOf(Kind.LIVE.name) }
    var query by rememberSaveable { mutableStateOf("") }
    val source = sources.firstOrNull { it.id == selectedId }

    if (sources.isEmpty()) {
        Onboarding(onAddList)
        return
    }

    val ready = state as? LoadState.Ready
    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = {
                    Column(Modifier.clickable(onClick = onOpenLists)) {
                        Text(source?.name ?: "Z-TV", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val data = ready?.data
                        if (data != null) {
                            Text(
                                "${data.count(Kind.LIVE)} chaînes · ${data.count(Kind.MOVIE)} films · ${data.count(Kind.SERIES)} épisodes" +
                                    if (ready?.offline == true) " · hors ligne" else "",
                                fontSize = 12.sp, color = Muted,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { app.repo.load(force = true) } }) {
                        Icon(Icons.Filled.Refresh, "Actualiser la liste")
                    }
                    IconButton(onClick = onOpenLists) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistPlay, "Mes listes")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Surface1) {
                val tabs = listOf(
                    Triple(Kind.LIVE.name, "Direct", Icons.Filled.LiveTv),
                    Triple(Kind.MOVIE.name, "Films", Icons.Filled.Movie),
                    Triple(Kind.SERIES.name, "Séries", Icons.Filled.VideoLibrary),
                    Triple(TAB_FAV, "Favoris", Icons.Filled.Favorite),
                )
                for ((key, label, icon) in tabs) {
                    NavigationBarItem(
                        selected = tab == key,
                        onClick = { tab = key },
                        icon = { Icon(icon, null) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Lime, selectedIconColor = Bg),
                    )
                }
            }
        },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            SearchField(query, "Rechercher une chaîne, un film, une série…") { query = it }
            when (val s = state) {
                LoadState.Idle -> CenterMessage("Choisis une liste", null, "Mes listes", onOpenLists)
                is LoadState.Loading -> Loading(s.message)
                is LoadState.Error -> ErrorView(
                    s,
                    onRetry = { scope.launch { app.repo.load(force = true) } },
                    onLists = onOpenLists,
                )
                is LoadState.Ready -> {
                    val data = s.data
                    val ua = source?.userAgent ?: DEFAULT_UA
                    if (query.trim().length >= 2) {
                        SearchResults(app, data, query, if (tab == TAB_FAV) null else Kind.valueOf(tab), favorites, ua, onPlay)
                    } else if (tab == TAB_FAV) {
                        FavoritesView(app, data, favorites, ua, onPlay)
                    } else {
                        val kind = Kind.valueOf(tab)
                        CategoryList(data.categories[kind].orEmpty(), kind) { onOpenCategory(kind, it.name) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Onboarding(onAdd: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Z-TV", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Lime)
        Text(
            "Ajoute ta liste (lien M3U ou JSON) : chaînes en direct, films et séries sont classés par catégorie, avec recherche et favoris.",
            color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(onClick = onAdd, modifier = Modifier.height(52.dp)) {
            Icon(Icons.Filled.Add, null)
            Text("  Ajouter ma liste", fontSize = 17.sp)
        }
    }
}

@Composable
private fun SearchField(value: String, placeholder: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) { Icon(Icons.Filled.Close, "Effacer") }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Surface1, focusedContainerColor = Surface1),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun Loading(message: String) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(message, color = Muted, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun CenterMessage(title: String, body: String?, action: String?, onAction: (() -> Unit)?) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (body != null) Text(body, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        if (action != null && onAction != null) {
            Button(onClick = onAction, modifier = Modifier.padding(top = 16.dp)) { Text(action) }
        }
    }
}

@Composable
private fun ErrorView(error: LoadState.Error, onRetry: () -> Unit, onLists: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Text("Impossible de charger la liste", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(error.message, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onRetry) { Text("Réessayer") }
            OutlinedButton(onClick = onLists) { Text("Modifier la liste") }
        }
        val preview = error.preview
        if (preview != null) {
            Text(
                "Début de la réponse du serveur (envoie une capture pour que je l'adapte) :",
                color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            Text(
                preview,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFFB8BDC7),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0A0B0D))
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun CategoryList(categories: List<Category>, kind: Kind, onOpen: (Category) -> Unit) {
    if (categories.isEmpty()) {
        CenterMessage("Aucune catégorie « ${kind.label} »", "Cette liste ne contient rien dans cette rubrique.", null, null)
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(categories, key = { it.name }) { cat ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(cat) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(cat.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${cat.count}", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Muted)
            }
            HorizontalDivider(color = Surface2, modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
private fun SearchResults(
    app: ZtvApp,
    data: ParsedPlaylist,
    query: String,
    kind: Kind?,
    favorites: Set<String>,
    ua: String,
    onPlay: (PlayRequest) -> Unit,
) {
    var results by remember { mutableStateOf<List<Channel>?>(null) }
    LaunchedEffect(query, data, kind) {
        delay(250)
        results = withContext(Dispatchers.Default) { data.search(query, kind) }
    }
    val list = results
    when {
        list == null -> Loading("Recherche…")
        list.isEmpty() -> CenterMessage("Aucun résultat", "Essaie un autre mot, ou change d'onglet.", null, null)
        else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            itemsIndexed(list, key = { _, c -> c.index }) { i, c ->
                ChannelRow(
                    c, c.url in favorites, showGroup = true,
                    onClick = {
                        val zap = if (c.kind == Kind.LIVE) list.filter { it.kind == Kind.LIVE } else listOf(c)
                        onPlay(PlayRequest(zap, zap.indexOf(c).coerceAtLeast(0), ua))
                    },
                    onToggleFavorite = { app.store.toggleFavorite(c.url) },
                )
            }
        }
    }
}

@Composable
private fun FavoritesView(
    app: ZtvApp,
    data: ParsedPlaylist,
    favorites: Set<String>,
    ua: String,
    onPlay: (PlayRequest) -> Unit,
) {
    val recents by app.store.recents.collectAsState()
    val favList = remember(favorites, data) { favorites.mapNotNull { data.byUrl[it] }.sortedBy { it.index } }
    val recentList = remember(recents, data) { recents.mapNotNull { data.byUrl[it] } }
    if (favList.isEmpty() && recentList.isEmpty()) {
        CenterMessage("Pas encore de favoris", "Appuie sur ♡ à côté d'une chaîne ou d'un film pour le retrouver ici.", null, null)
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        if (recentList.isNotEmpty()) {
            item { SectionTitle("Récemment regardés") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(recentList, key = { it.index }) { c ->
                        Column(
                            Modifier
                                .size(width = 110.dp, height = 120.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Surface1)
                                .clickable { onPlay(PlayRequest(listOf(c), 0, ua)) }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Logo(c.logo, c.name, Modifier.size(64.dp))
                            Text(
                                c.name, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
        }
        if (favList.isNotEmpty()) {
            item { SectionTitle("Favoris") }
            itemsIndexed(favList, key = { _, c -> "f" + c.index }) { _, c ->
                ChannelRow(
                    c, true, showGroup = true,
                    onClick = {
                        val list = if (c.kind == Kind.LIVE) favList.filter { it.kind == Kind.LIVE } else listOf(c)
                        onPlay(PlayRequest(list, list.indexOf(c).coerceAtLeast(0), ua))
                    },
                    onToggleFavorite = { app.store.toggleFavorite(c.url) },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        color = Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun Logo(url: String?, name: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.trim().take(1).uppercase(), color = Muted, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
            )
        }
    }
}

@Composable
private fun ChannelRow(
    c: Channel,
    favorite: Boolean,
    showGroup: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Logo(c.logo, c.name, Modifier.size(52.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(c.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (showGroup) {
                Text(
                    "${c.kind.label} · ${c.group}", color = Muted, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                "Favori",
                tint = if (favorite) Lime else Muted,
            )
        }
    }
}

@Composable
private fun PosterCard(title: String, image: String?, subtitle: String?, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface2),
            contentAlignment = Alignment.Center,
        ) {
            Text(title.trim().take(1).uppercase(), color = Muted, fontSize = 32.sp, fontWeight = FontWeight.Black)
            if (!image.isNullOrBlank()) {
                AsyncImage(
                    model = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = Muted, maxLines = 1)
    }
}

// ---------------------------------------------------------------- Une catégorie

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
    )
}

@Composable
private fun CategoryScreen(
    app: ZtvApp,
    kind: Kind,
    group: String,
    onBack: () -> Unit,
    onPlay: (PlayRequest) -> Unit,
    onOpenShow: (String) -> Unit,
) {
    val state by app.repo.state.collectAsState()
    val favorites by app.store.favorites.collectAsState()
    var filter by rememberSaveable { mutableStateOf("") }
    val data = (state as? LoadState.Ready)?.data
    val ua = app.store.current()?.userAgent ?: DEFAULT_UA

    Scaffold(containerColor = Bg, topBar = { BackTopBar(group, onBack) }) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            if (data == null) {
                Loading("Chargement…")
                return@Column
            }
            SearchField(filter, "Filtrer dans « $group »") { filter = it }
            val f = normalize(filter)
            when (kind) {
                Kind.LIVE -> {
                    val all = remember(data, group) { data.channels(Kind.LIVE, group) }
                    val list = remember(all, f) { if (f.isEmpty()) all else all.filter { f in it.searchKey } }
                    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                        itemsIndexed(list, key = { _, c -> c.index }) { i, c ->
                            ChannelRow(
                                c, c.url in favorites, showGroup = false,
                                onClick = { onPlay(PlayRequest(list, i, ua)) },
                                onToggleFavorite = { app.store.toggleFavorite(c.url) },
                            )
                        }
                    }
                }
                Kind.MOVIE -> {
                    val all = remember(data, group) { data.channels(Kind.MOVIE, group) }
                    val list = remember(all, f) { if (f.isEmpty()) all else all.filter { f in it.searchKey } }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(110.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        itemsIndexed(list, key = { _, c -> c.index }) { _, c ->
                            PosterCard(c.name, c.logo, if (c.url in favorites) "♥ Favori" else null) {
                                onPlay(PlayRequest(listOf(c), 0, ua))
                            }
                        }
                    }
                }
                Kind.SERIES -> {
                    val shows = remember(data, group) { data.shows(group) }
                    val list = remember(shows, f) { if (f.isEmpty()) shows else shows.filter { f in normalize(it.title) } }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(110.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        itemsIndexed(list, key = { _, s -> s.title }) { _, s ->
                            val count = s.episodes.size
                            PosterCard(s.title, s.logo, if (count > 1) "$count épisodes" else null) {
                                if (count == 1) onPlay(PlayRequest(s.episodes, 0, ua)) else onOpenShow(s.title)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Une série

@Composable
private fun ShowScreen(app: ZtvApp, group: String, title: String, onBack: () -> Unit, onPlay: (PlayRequest) -> Unit) {
    val state by app.repo.state.collectAsState()
    val data = (state as? LoadState.Ready)?.data
    val ua = app.store.current()?.userAgent ?: DEFAULT_UA
    val show = remember(data, group, title) { data?.shows(group)?.firstOrNull { it.title == title } }

    Scaffold(containerColor = Bg, topBar = { BackTopBar(title, onBack) }) { pad ->
        if (show == null) {
            Box(Modifier.padding(pad)) { Loading("Chargement…") }
            return@Scaffold
        }
        val episodes = show.episodes
        LazyColumn(
            Modifier.padding(pad),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            var lastSeason: Int? = -1
            episodes.forEachIndexed { i, ep ->
                if (ep.season != lastSeason) {
                    lastSeason = ep.season
                    item(key = "s${ep.season}") { SectionTitle(ep.season?.let { "Saison $it" } ?: "Épisodes") }
                }
                item(key = ep.index) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPlay(PlayRequest(episodes, i, ua)) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            ep.episode?.let { "E%02d".format(it) } ?: "▶",
                            color = Lime, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp),
                        )
                        Text(ep.name, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider(color = Surface2, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Mes listes

@Composable
private fun ListsScreen(app: ZtvApp, onBack: () -> Unit, onAdd: () -> Unit, onEdit: (String) -> Unit) {
    val sources by app.store.sources.collectAsState()
    val selectedId by app.store.selectedId.collectAsState()
    Scaffold(containerColor = Bg, topBar = { BackTopBar("Mes listes", onBack) }) { pad ->
        LazyColumn(
            Modifier.padding(pad),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(sources, key = { it.id }) { src ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            app.store.select(src.id)
                            onBack()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                        if (src.id == selectedId) Icon(Icons.Filled.Check, null, tint = Lime)
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(src.name, fontWeight = FontWeight.SemiBold)
                        Text(maskUrl(src.url), color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { onEdit(src.id) }) { Icon(Icons.Filled.Edit, "Modifier") }
                    IconButton(onClick = { app.store.deleteSource(src.id) }) { Icon(Icons.Filled.Delete, "Supprimer", tint = Red) }
                }
                HorizontalDivider(color = Surface2)
            }
            item {
                Button(
                    onClick = onAdd,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Icon(Icons.Filled.Add, null)
                    Text("  Ajouter une liste")
                }
            }
        }
    }
}

/** Cache l'identifiant et le mot de passe souvent présents dans le lien. */
private fun maskUrl(url: String): String {
    val u = runCatching { Uri.parse(url) }.getOrNull() ?: return url
    val host = u.host ?: return url
    return "${u.scheme}://$host/…"
}

@Composable
private fun EditListScreen(app: ZtvApp, id: String?, onDone: () -> Unit) {
    val existing = remember(id) { app.store.sources.value.firstOrNull { it.id == id } }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "Ma liste") }
    var url by rememberSaveable { mutableStateOf(existing?.url ?: "") }
    var userAgent by rememberSaveable { mutableStateOf(existing?.userAgent ?: DEFAULT_UA) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(containerColor = Bg, topBar = { BackTopBar(if (existing == null) "Ajouter une liste" else "Modifier la liste", onDone) }) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Nom") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = url, onValueChange = { url = it.trim() }, label = { Text("Lien de la liste (M3U ou JSON)") },
                placeholder = { Text("https://…") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            )
            Text(
                "Le lien contient souvent ton identifiant et ton mot de passe : il reste uniquement sur ce téléphone.",
                color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp),
            )
            OutlinedTextField(
                value = userAgent, onValueChange = { userAgent = it }, label = { Text("User-Agent (avancé)") }, singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )
            Text(
                "Certains serveurs n'acceptent que certains lecteurs. Laisse la valeur par défaut si tu ne sais pas.",
                color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp),
            )
            error?.let { Text(it, color = Red, modifier = Modifier.padding(top = 12.dp)) }
            Button(
                onClick = {
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        error = "Le lien doit commencer par http:// ou https://"
                        return@Button
                    }
                    val src = PlaylistSource(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        name = name.trim().ifBlank { "Ma liste" },
                        url = url.trim(),
                        userAgent = userAgent.trim().ifBlank { DEFAULT_UA },
                    )
                    app.store.saveSource(src)
                    app.store.select(src.id)
                    scope.launch { app.repo.load(force = true) }
                    onDone()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(52.dp),
            ) { Text("Enregistrer et charger", fontSize = 17.sp) }
        }
    }
}
