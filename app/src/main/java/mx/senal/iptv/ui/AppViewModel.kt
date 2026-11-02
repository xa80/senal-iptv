package mx.senal.iptv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.senal.iptv.data.Channel
import mx.senal.iptv.data.Playlist
import mx.senal.iptv.data.PlaylistType
import mx.senal.iptv.data.Programme
import mx.senal.iptv.data.Repository
import mx.senal.iptv.roku.RokuClient
import mx.senal.iptv.roku.RokuDevice

data class UiState(
    val playlists: List<Playlist> = emptyList(),
    val active: Playlist? = null,
    val channels: List<Channel> = emptyList(),
    val epg: Map<String, List<Programme>> = emptyMap(),
    val favorites: Set<String> = emptySet(),
    val loading: Boolean = false,
    val error: String? = null,
    val current: Channel? = null,
    // Roku
    val rokus: List<RokuDevice> = emptyList(),
    val searchingRoku: Boolean = false,
    val roku: RokuDevice? = null,
    val rokuMessage: String? = null,
) {
    val groups: List<String> get() = channels.map { it.group }.distinct()

    fun nowPlaying(c: Channel, now: Long = System.currentTimeMillis()): Programme? =
        c.epgId?.let { id -> epg[id]?.firstOrNull { it.isLive(now) } }

    fun nextUp(c: Channel, now: Long = System.currentTimeMillis()): Programme? =
        c.epgId?.let { id -> epg[id]?.firstOrNull { it.startMillis >= now } }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)
    private val rokuClient = RokuClient(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        _state.update { it.copy(playlists = repo.playlists(), favorites = repo.favorites()) }
        repo.activePlaylist()?.let { open(it) }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    fun addPlaylist(
        name: String, type: PlaylistType, url: String,
        user: String, pass: String, epg: String, onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val p = repo.addPlaylist(name, type, url, user, pass, epg)
                _state.update { it.copy(playlists = repo.playlists()) }
                open(p)
                onDone()
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = friendly(e)) }
            }
        }
    }

    fun deletePlaylist(p: Playlist) {
        repo.deletePlaylist(p.id)
        _state.update { it.copy(playlists = repo.playlists(), channels = emptyList(), epg = emptyMap(), active = null) }
        repo.activePlaylist()?.let { open(it) }
    }

    fun open(p: Playlist, refresh: Boolean = false) {
        repo.activePlaylistId = p.id
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(active = p, loading = true, error = null) }
            try {
                val chans = repo.channels(p, refresh).sortedBy { it.number }
                _state.update { it.copy(channels = chans, loading = false) }
                val guide = runCatching { repo.epg(p, chans) }.getOrDefault(emptyMap())
                _state.update { it.copy(epg = guide) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = friendly(e)) }
            }
        }
    }

    fun refresh() = _state.value.active?.let { open(it, refresh = true) }

    fun toggleFavorite(c: Channel) = _state.update { it.copy(favorites = repo.toggleFavorite(c.id)) }

    fun select(c: Channel) = _state.update { it.copy(current = c) }

    fun neighbor(step: Int): Channel? {
        val s = _state.value
        val list = s.channels
        val idx = list.indexOfFirst { it.id == s.current?.id }
        if (idx < 0 || list.isEmpty()) return null
        val next = list[(idx + step + list.size) % list.size]
        select(next)
        return next
    }

    // ---------- Roku ----------

    fun searchRokus() {
        viewModelScope.launch {
            _state.update { it.copy(searchingRoku = true, rokuMessage = null) }
            val found = runCatching { rokuClient.discover() }.getOrDefault(emptyList())
            _state.update {
                it.copy(
                    rokus = found, searchingRoku = false,
                    rokuMessage = if (found.isEmpty()) "No encontré ningún Roku en tu Wi‑Fi." else null,
                )
            }
        }
    }

    fun addRokuByIp(ip: String) {
        viewModelScope.launch {
            val d = rokuClient.info(ip.trim())
            if (d == null) _state.update { it.copy(rokuMessage = "No respondió ningún Roku en $ip.") }
            else _state.update { s -> s.copy(rokus = (s.rokus.filterNot { it.ip == d.ip } + d), rokuMessage = null) }
        }
    }

    fun connectRoku(d: RokuDevice) {
        _state.update { it.copy(roku = d) }
        _state.value.current?.let { castToRoku(it) }
    }

    fun disconnectRoku() = _state.update { it.copy(roku = null, rokuMessage = null) }

    fun castToRoku(c: Channel) {
        val d = _state.value.roku ?: return
        select(c)
        viewModelScope.launch {
            val ok = rokuClient.play(d, c.streamUrl, c.name)
            _state.update {
                it.copy(rokuMessage = if (ok) null else "El Roku no aceptó el canal. Prueba con «Duplicar pantalla».")
            }
        }
    }

    fun rokuKey(key: String) {
        val d = _state.value.roku ?: return
        viewModelScope.launch { rokuClient.key(d, key) }
    }

    fun rokuChannel(step: Int) {
        neighbor(step)?.let { castToRoku(it) }
    }

    private fun friendly(e: Exception): String = when {
        e is java.net.UnknownHostException -> "No se encontró el servidor. Revisa la dirección y tu internet."
        e is java.net.SocketTimeoutException -> "El servidor tardó demasiado en responder."
        e is IllegalArgumentException -> "La dirección no es válida. Debe empezar con http:// o https://"
        else -> e.message ?: "Ocurrió un error inesperado."
    }
}
