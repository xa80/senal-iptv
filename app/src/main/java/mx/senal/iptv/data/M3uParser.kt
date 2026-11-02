package mx.senal.iptv.data

/**
 * Lee listas M3U/M3U8 extendidas:
 *   #EXTM3U url-tvg="http://.../guia.xml"
 *   #EXTINF:-1 tvg-id="canal.mx" tvg-logo="http://..." group-title="Noticias",Nombre del canal
 *   http://servidor/stream.m3u8
 */
object M3uParser {

    data class Result(val channels: List<Channel>, val epgUrl: String?)

    private val attrRegex = Regex("""([\w-]+)="([^"]*)"""")

    fun parse(text: String): Result {
        val channels = mutableListOf<Channel>()
        var epgUrl: String? = null
        var pendingInfo: String? = null
        var pendingGroup: String? = null
        var number = 1

        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTM3U", ignoreCase = true) -> {
                    val attrs = attrs(line)
                    epgUrl = attrs["url-tvg"] ?: attrs["x-tvg-url"]
                }
                line.startsWith("#EXTINF", ignoreCase = true) -> pendingInfo = line
                line.startsWith("#EXTGRP:", ignoreCase = true) -> pendingGroup = line.substringAfter(":").trim()
                line.startsWith("#") -> Unit
                else -> {
                    val info = pendingInfo
                    val attrs = if (info != null) attrs(info) else emptyMap()
                    val name = info?.let { nameOf(it) }?.ifBlank { null }
                        ?: attrs["tvg-name"]
                        ?: "Canal $number"
                    val group = attrs["group-title"]?.ifBlank { null } ?: pendingGroup ?: "Sin categoría"
                    channels += Channel(
                        id = "m3u-$number",
                        number = attrs["tvg-chno"]?.toIntOrNull() ?: number,
                        name = name,
                        group = group,
                        logo = attrs["tvg-logo"]?.ifBlank { null },
                        streamUrl = line,
                        epgId = attrs["tvg-id"]?.ifBlank { null },
                    )
                    number++
                    pendingInfo = null
                    pendingGroup = null
                }
            }
        }
        return Result(channels, epgUrl)
    }

    private fun attrs(line: String): Map<String, String> =
        attrRegex.findAll(line).associate { it.groupValues[1].lowercase() to it.groupValues[2] }

    /** El nombre va después de la última coma que no está dentro de comillas. */
    private fun nameOf(info: String): String {
        var inQuotes = false
        var lastComma = -1
        info.forEachIndexed { i, c ->
            if (c == '"') inQuotes = !inQuotes
            if (c == ',' && !inQuotes) lastComma = i
        }
        return if (lastComma >= 0) info.substring(lastComma + 1).trim() else ""
    }
}
