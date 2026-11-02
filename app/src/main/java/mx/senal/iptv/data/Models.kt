package mx.senal.iptv.data

import kotlinx.serialization.Serializable

@Serializable
enum class PlaylistType { M3U, XTREAM }

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val type: PlaylistType,
    /** URL M3U (tipo M3U) o servidor base (tipo Xtream), p. ej. http://servidor.com:8080 */
    val url: String,
    val username: String = "",
    val password: String = "",
    /** URL XMLTV opcional. En Xtream se arma sola si viene vacía. */
    val epgUrl: String = "",
)

@Serializable
data class Channel(
    val id: String,
    val number: Int,
    val name: String,
    val group: String,
    val logo: String?,
    val streamUrl: String,
    /** Identificador del canal dentro de la guía XMLTV (tvg-id / epg_channel_id). */
    val epgId: String?,
)

data class Programme(
    val channelEpgId: String,
    val title: String,
    val description: String,
    val startMillis: Long,
    val stopMillis: Long,
) {
    fun isLive(now: Long) = now in startMillis until stopMillis
    fun progress(now: Long): Float =
        if (stopMillis <= startMillis) 0f
        else ((now - startMillis).toFloat() / (stopMillis - startMillis)).coerceIn(0f, 1f)
}
