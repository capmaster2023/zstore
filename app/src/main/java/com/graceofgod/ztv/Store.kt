package com.graceofgod.ztv

import android.app.Application
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

const val DEFAULT_UA = "VLC/3.0.20 LibVLC/3.0.20"

@Serializable
data class PlaylistSource(
    val id: String,
    val name: String,
    val url: String,
    val userAgent: String = DEFAULT_UA,
)

private val json = Json { ignoreUnknownKeys = true }

/** Tout ce qui est gardé sur le téléphone : listes, favoris, récents, positions de lecture. */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("ztv", Context.MODE_PRIVATE)
    private val dir = File(context.filesDir, "playlists").apply { mkdirs() }

    val sources = MutableStateFlow(readSources())
    val selectedId = MutableStateFlow(prefs.getString("selected", null) ?: sources.value.firstOrNull()?.id)
    val favorites = MutableStateFlow(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet())
    val recents = MutableStateFlow(readStrings("recents"))

    private fun readSources(): List<PlaylistSource> = runCatching {
        json.decodeFromString(ListSerializer(PlaylistSource.serializer()), prefs.getString("sources", "[]") ?: "[]")
    }.getOrDefault(emptyList())

    private fun readStrings(key: String): List<String> = runCatching {
        json.decodeFromString(ListSerializer(String.serializer()), prefs.getString(key, "[]") ?: "[]")
    }.getOrDefault(emptyList())

    fun current(): PlaylistSource? = sources.value.firstOrNull { it.id == selectedId.value }

    fun saveSource(src: PlaylistSource) {
        val list = sources.value.toMutableList()
        val i = list.indexOfFirst { it.id == src.id }
        if (i >= 0) list[i] = src else list.add(src)
        sources.value = list
        prefs.edit().putString("sources", json.encodeToString(ListSerializer(PlaylistSource.serializer()), list)).apply()
    }

    fun deleteSource(id: String) {
        val list = sources.value.filterNot { it.id == id }
        sources.value = list
        prefs.edit().putString("sources", json.encodeToString(ListSerializer(PlaylistSource.serializer()), list)).apply()
        cacheFile(id).delete()
        if (selectedId.value == id) select(list.firstOrNull()?.id)
    }

    fun select(id: String?) {
        selectedId.value = id
        prefs.edit().putString("selected", id).apply()
    }

    fun cacheFile(id: String) = File(dir, "$id.list")

    fun isFavorite(url: String) = url in favorites.value

    fun toggleFavorite(url: String) {
        val set = favorites.value.toMutableSet()
        if (!set.add(url)) set.remove(url)
        favorites.value = set
        prefs.edit().putStringSet("favorites", HashSet(set)).apply()
    }

    fun addRecent(url: String) {
        val list = (listOf(url) + recents.value.filterNot { it == url }).take(40)
        recents.value = list
        prefs.edit().putString("recents", json.encodeToString(ListSerializer(String.serializer()), list)).apply()
    }

    fun resumeMs(url: String): Long = prefs.getLong("pos_" + url.hashCode(), 0L)

    fun saveResume(url: String, ms: Long) {
        prefs.edit().putLong("pos_" + url.hashCode(), ms).apply()
    }
}

sealed interface LoadState {
    data object Idle : LoadState
    data class Loading(val message: String) : LoadState
    data class Ready(val data: ParsedPlaylist, val offline: Boolean = false) : LoadState
    data class Error(val message: String, val preview: String? = null) : LoadState
}

/** Ce que le lecteur doit jouer : une liste (pour zapper / enchaîner les épisodes) et la position de départ. */
data class PlayRequest(val items: List<Channel>, val start: Int, val userAgent: String)

class TvRepository(private val store: Store) {
    val state = MutableStateFlow<LoadState>(LoadState.Idle)
    var playRequest: PlayRequest? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /** Charge la liste choisie : depuis le cache, ou en la téléchargeant ([force] = actualiser). */
    suspend fun load(force: Boolean) {
        val src = store.current()
        if (src == null) {
            state.value = LoadState.Idle
            return
        }
        val file = store.cacheFile(src.id)
        var offline = false
        if (force || !file.exists() || file.length() == 0L) {
            state.value = LoadState.Loading("Téléchargement de la liste…")
            val downloaded = withContext(Dispatchers.IO) { runCatching { download(src, file) } }
            if (downloaded.isFailure) {
                if (!file.exists() || file.length() == 0L) {
                    state.value = LoadState.Error(downloaded.exceptionOrNull()?.message ?: "Téléchargement impossible")
                    return
                }
                offline = true
            }
        }
        state.value = LoadState.Loading("Lecture des chaînes…")
        val result = withContext(Dispatchers.Default) { runCatching { PlaylistParser.parseFile(file) } }
        state.value = result.fold(
            onSuccess = { r ->
                when (r) {
                    is ParseResult.Ok -> LoadState.Ready(ParsedPlaylist(r.channels), offline)
                    is ParseResult.Unknown -> LoadState.Error(
                        "Le format de cette liste n'est pas reconnu (ce n'est ni du M3U ni du JSON).",
                        r.preview,
                    )
                }
            },
            onFailure = { LoadState.Error(it.message ?: "Impossible de lire la liste") },
        )
    }

    private fun download(src: PlaylistSource, file: File) {
        val request = Request.Builder()
            .url(src.url.trim())
            .header("User-Agent", src.userAgent.ifBlank { DEFAULT_UA })
            .build()
        client.newCall(request).execute().use { res ->
            if (!res.isSuccessful) throw IOException("Le serveur a répondu « ${res.code} ». Vérifie le lien de la liste.")
            val body = res.body ?: throw IOException("Réponse vide du serveur")
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.outputStream().use { out -> body.byteStream().copyTo(out) }
            if (tmp.length() == 0L) throw IOException("La liste téléchargée est vide")
            file.delete()
            if (!tmp.renameTo(file)) throw IOException("Impossible d'enregistrer la liste")
        }
    }
}

class ZtvApp : Application() {
    lateinit var store: Store
        private set
    lateinit var repo: TvRepository
        private set

    override fun onCreate() {
        super.onCreate()
        store = Store(this)
        repo = TvRepository(store)
    }
}
