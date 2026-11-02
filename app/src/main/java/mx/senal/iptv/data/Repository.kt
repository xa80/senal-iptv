package mx.senal.iptv.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * Guarda las listas y favoritos en el teléfono y descarga canales y guía.
 * Señal no trae canales: todo viene de la lista que agrega el usuario.
 */
class Repository(private val context: Context) {

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val xtream = XtreamClient(http)
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences("senal", Context.MODE_PRIVATE)

    // ---------- Listas ----------

    fun playlists(): List<Playlist> =
        prefs.getString("playlists", null)?.let {
            runCatching { json.decodeFromString<List<Playlist>>(it) }.getOrNull()
        } ?: emptyList()

    private fun savePlaylists(list: List<Playlist>) {
        prefs.edit().putString("playlists", json.encodeToString(list)).apply()
    }

    var activePlaylistId: String?
        get() = prefs.getString("active", null)
        set(value) { prefs.edit().putString("active", value).apply() }

    fun activePlaylist(): Playlist? {
        val all = playlists()
        return all.firstOrNull { it.id == activePlaylistId } ?: all.firstOrNull()
    }

    /** Valida la lista contra el servidor antes de guardarla. */
    suspend fun addPlaylist(
        name: String, type: PlaylistType, url: String,
        username: String, password: String, epgUrl: String,
    ): Playlist = withContext(Dispatchers.IO) {
        val p = Playlist(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Mi lista" },
            type = type,
            url = url.trim(),
            username = username.trim(),
            password = password.trim(),
            epgUrl = epgUrl.trim(),
        )
        if (type == PlaylistType.XTREAM) xtream.login(p)
        val channels = loadChannelsFromNetwork(p)
        if (channels.isEmpty()) error("La lista no tiene canales")
        savePlaylists(playlists() + p)
        activePlaylistId = p.id
        p
    }

    fun deletePlaylist(id: String) {
        savePlaylists(playlists().filterNot { it.id == id })
        cacheFile(id).delete()
        if (activePlaylistId == id) activePlaylistId = playlists().firstOrNull()?.id
    }

    // ---------- Canales ----------

    private fun loadChannelsFromNetwork(p: Playlist): List<Channel> {
        val list = when (p.type) {
            PlaylistType.XTREAM -> xtream.liveChannels(p)
            PlaylistType.M3U -> {
                val text = download(p.url).bufferedReader().use { it.readText() }
                val r = M3uParser.parse(text)
                prefs.edit().putString("epg-${p.id}", r.epgUrl).apply()
                r.channels
            }
        }
        cacheFile(p.id).writeText(json.encodeToString(list))
        return list
    }

    suspend fun channels(p: Playlist, forceRefresh: Boolean = false): List<Channel> =
        withContext(Dispatchers.IO) {
            val cached = cacheFile(p.id)
            if (!forceRefresh && cached.exists()) {
                runCatching { json.decodeFromString<List<Channel>>(cached.readText()) }
                    .getOrNull()?.let { return@withContext it }
            }
            loadChannelsFromNetwork(p)
        }

    private fun cacheFile(id: String) = File(context.filesDir, "canales-$id.json")

    // ---------- Guía (EPG) ----------

    suspend fun epg(p: Playlist, channels: List<Channel>): Map<String, List<Programme>> =
        withContext(Dispatchers.IO) {
            val url = p.epgUrl.ifBlank {
                when (p.type) {
                    PlaylistType.XTREAM -> xtream.epgUrl(p)
                    PlaylistType.M3U -> prefs.getString("epg-${p.id}", null) ?: ""
                }
            }
            if (url.isBlank()) return@withContext emptyMap()
            val ids = channels.mapNotNull { it.epgId }.toSet()
            val now = System.currentTimeMillis()
            download(url).use { input ->
                XmltvParser.parse(input, ids, from = now - 6 * HOUR, to = now + 30 * HOUR)
            }
        }

    // ---------- Favoritos ----------

    fun favorites(): Set<String> = prefs.getStringSet("favs", emptySet())!!.toSet()

    fun toggleFavorite(channelId: String): Set<String> {
        val f = favorites().toMutableSet()
        if (!f.add(channelId)) f.remove(channelId)
        prefs.edit().putStringSet("favs", f).apply()
        return f
    }

    // ---------- Utilidades ----------

    /** Descarga y descomprime si viene en .gz (muchas guías XMLTV vienen así). */
    private fun download(url: String): InputStream {
        val resp = http.newCall(Request.Builder().url(url).build()).execute()
        if (!resp.isSuccessful) {
            resp.close()
            error("No se pudo descargar (${resp.code})")
        }
        val body = BufferedInputStream(resp.body!!.byteStream())
        body.mark(2)
        val b1 = body.read()
        val b2 = body.read()
        body.reset()
        return if (b1 == 0x1f && b2 == 0x8b) GZIPInputStream(body) else body
    }

    companion object {
        const val HOUR = 60 * 60 * 1000L
    }
}
