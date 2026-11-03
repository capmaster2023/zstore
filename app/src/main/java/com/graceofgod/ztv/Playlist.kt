package com.graceofgod.ztv

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.BufferedReader
import java.io.File
import java.text.Normalizer

enum class Kind(val label: String) { LIVE("Direct"), MOVIE("Films"), SERIES("Séries") }

/** Une entrée de la liste : chaîne en direct, film ou épisode. */
data class Channel(
    val index: Int,
    val name: String,
    val url: String,
    val logo: String?,
    val group: String,
    val kind: Kind,
    val seriesTitle: String?,
    val season: Int?,
    val episode: Int?,
    val searchKey: String,
)

data class Category(val name: String, val kind: Kind, val count: Int)

/** Une série = tous les épisodes qui portent le même nom. */
data class Show(val title: String, val logo: String?, val episodes: List<Channel>)

class ParsedPlaylist(val channels: List<Channel>) {
    private val byGroup: Map<Pair<Kind, String>, List<Channel>>
    val categories: Map<Kind, List<Category>>
    val byUrl: Map<String, Channel> = channels.associateBy { it.url }

    init {
        val map = LinkedHashMap<Pair<Kind, String>, MutableList<Channel>>()
        for (c in channels) map.getOrPut(c.kind to c.group) { ArrayList() }.add(c)
        byGroup = map
        categories = Kind.entries.associateWith { k ->
            map.entries.filter { it.key.first == k }.map { Category(it.key.second, k, it.value.size) }
        }
    }

    fun count(kind: Kind): Int = categories[kind]?.sumOf { it.count } ?: 0

    fun channels(kind: Kind, group: String): List<Channel> = byGroup[kind to group].orEmpty()

    fun shows(group: String): List<Show> {
        val map = LinkedHashMap<String, MutableList<Channel>>()
        for (c in channels(Kind.SERIES, group)) map.getOrPut(c.seriesTitle ?: c.name) { ArrayList() }.add(c)
        return map.map { (title, eps) ->
            Show(
                title = title,
                logo = eps.firstNotNullOfOrNull { it.logo },
                episodes = eps.sortedWith(compareBy<Channel>({ it.season ?: 0 }, { it.episode ?: 0 })),
            )
        }
    }

    /** Recherche par mots (sans accents, sans majuscules). */
    fun search(query: String, kind: Kind?, limit: Int = 300): List<Channel> {
        val words = normalize(query).split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        val out = ArrayList<Channel>()
        for (c in channels) {
            if (kind != null && c.kind != kind) continue
            if (words.all { it in c.searchKey }) {
                out.add(c)
                if (out.size >= limit) break
            }
        }
        return out
    }
}

sealed interface ParseResult {
    data class Ok(val channels: List<Channel>) : ParseResult
    data class Unknown(val preview: String) : ParseResult
}

private val DIACRITICS = Regex("\\p{Mn}+")

fun normalize(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(DIACRITICS, "").lowercase().trim()

object PlaylistParser {
    private val ATTR = Regex("""([A-Za-z0-9_-]+)="([^"]*)"""")
    private val SXE = Regex("""^(.*?)[\s._|-]*[Ss](\d{1,2})[\s._-]*[Ee](\d{1,3})""")
    private val MOVIE_EXT = listOf(".mp4", ".mkv", ".avi", ".mov", ".m4v", ".wmv", ".flv", ".webm", ".mpg")
    private val SERIES_WORDS = listOf("serie", "series", "saison", "season", "tv show", "feuilleton")
    private val MOVIE_WORDS = listOf("vod", "film", "movie", "cinema")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parseFile(file: File): ParseResult {
        val head = file.inputStream().use { input ->
            val buf = ByteArray(4096)
            val n = input.read(buf)
            if (n <= 0) "" else String(buf, 0, n, Charsets.UTF_8)
        }
        val first = head.trimStart('﻿', ' ', '\n', '\r', '\t').firstOrNull()
        val channels = if (first == '{' || first == '[') {
            parseJson(file.readText())
        } else {
            file.bufferedReader().use { parseM3u(it) }
        }
        return if (channels.isEmpty()) ParseResult.Unknown(head.take(600)) else ParseResult.Ok(channels)
    }

    /** Format M3U / M3U8 (#EXTINF …, group-title="…", tvg-logo="…"). Accepte aussi une simple liste de liens. */
    private fun parseM3u(reader: BufferedReader): List<Channel> {
        val out = ArrayList<Channel>()
        var name: String? = null
        var tvgName: String? = null
        var logo: String? = null
        var group: String? = null
        var extGroup: String? = null
        while (true) {
            val raw = reader.readLine() ?: break
            val line = raw.trim().trimStart('﻿')
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val attrs = HashMap<String, String>()
                    for (m in ATTR.findAll(line)) attrs[m.groupValues[1].lowercase()] = m.groupValues[2]
                    logo = attrs["tvg-logo"]?.takeIf { it.isNotBlank() }
                    group = attrs["group-title"]?.takeIf { it.isNotBlank() }
                    tvgName = attrs["tvg-name"]?.takeIf { it.isNotBlank() }
                    name = displayName(line)
                }
                line.startsWith("#EXTGRP:", ignoreCase = true) -> extGroup = line.substringAfter(':').trim()
                line.startsWith("#") -> Unit
                line.contains("://") -> {
                    val n = name ?: tvgName ?: line.substringBefore('?').substringAfterLast('/')
                    out.add(makeChannel(out.size, n, line, logo, group ?: extGroup ?: "Sans catégorie"))
                    name = null
                    tvgName = null
                    logo = null
                    group = null
                    extGroup = null
                }
            }
        }
        return out
    }

    /** Le nom affiché est après la première virgule qui n'est pas entre guillemets. */
    private fun displayName(line: String): String? {
        var inQuotes = false
        for (i in line.indices) {
            val ch = line[i]
            if (ch == '"') {
                inQuotes = !inQuotes
            } else if (ch == ',' && !inQuotes) {
                return line.substring(i + 1).trim().takeIf { it.isNotEmpty() }
            }
        }
        return null
    }

    /** Format JSON : on cherche les objets qui ont un lien (url, stream_url…) et un nom. */
    private fun parseJson(text: String): List<Channel> {
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() ?: return emptyList()
        val out = ArrayList<Channel>()

        fun visit(el: JsonElement, groupHint: String?) {
            when (el) {
                is JsonArray -> for (child in el) visit(child, groupHint)
                is JsonObject -> {
                    val url = el.str("url", "stream_url", "streamUrl", "link", "src", "file", "stream", "direct_source")
                    if (url != null && url.contains("://")) {
                        val name = el.str("name", "title", "stream_display_name", "channel", "label") ?: url.substringAfterLast('/')
                        val group = el.str("group", "group_title", "group-title", "category", "category_name", "genre", "categorie")
                            ?: groupHint ?: "Sans catégorie"
                        val logo = el.str("logo", "tvg_logo", "tvg-logo", "stream_icon", "icon", "image", "cover", "poster", "thumbnail")
                        out.add(makeChannel(out.size, name, url, logo, group))
                    } else {
                        val hint = el.str("category_name", "category", "group", "name", "title") ?: groupHint
                        for (child in el.values) {
                            if (child is JsonArray || child is JsonObject) visit(child, hint)
                        }
                    }
                }
                else -> Unit
            }
        }

        visit(root, null)
        return out
    }

    private fun JsonObject.str(vararg keys: String): String? {
        for (k in keys) {
            val v = this[k]
            if (v is JsonPrimitive && v.isString && v.content.isNotBlank()) return v.content
        }
        return null
    }

    fun detectKind(url: String, group: String): Kind {
        val u = url.lowercase().substringBefore('?')
        val g = normalize(group)
        val streamLike = u.endsWith(".ts") || u.endsWith(".m3u8")
        return when {
            "/live/" in u -> Kind.LIVE
            "/series/" in u -> Kind.SERIES
            "/movie/" in u || "/movies/" in u || MOVIE_EXT.any { u.endsWith(it) } -> Kind.MOVIE
            !streamLike && SERIES_WORDS.any { it in g } -> Kind.SERIES
            !streamLike && MOVIE_WORDS.any { it in g } -> Kind.MOVIE
            else -> Kind.LIVE
        }
    }

    private fun makeChannel(index: Int, name: String, url: String, logo: String?, group: String): Channel {
        val kind = detectKind(url, group)
        var seriesTitle: String? = null
        var season: Int? = null
        var episode: Int? = null
        if (kind != Kind.LIVE) {
            val m = SXE.find(name)
            if (m != null) {
                seriesTitle = m.groupValues[1].trim(' ', '-', '_', '.', '|', ':').ifBlank { null }
                season = m.groupValues[2].toIntOrNull()
                episode = m.groupValues[3].toIntOrNull()
            }
        }
        return Channel(
            index = index,
            name = name,
            url = url,
            logo = logo,
            group = group.trim(),
            kind = if (seriesTitle != null) Kind.SERIES else kind,
            seriesTitle = seriesTitle,
            season = season,
            episode = episode,
            searchKey = normalize(name),
        )
    }
}
