package mx.senal.iptv.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** Cliente mínimo de la API "player_api.php" que usan los paneles Xtream Codes. */
class XtreamClient(private val http: OkHttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    fun baseUrl(p: Playlist): String = p.url.trim().trimEnd('/')

    private fun api(p: Playlist, action: String? = null): String {
        val b = "${baseUrl(p)}/player_api.php".toHttpUrl().newBuilder()
            .addQueryParameter("username", p.username)
            .addQueryParameter("password", p.password)
        if (action != null) b.addQueryParameter("action", action)
        return b.build().toString()
    }

    private fun get(url: String): String {
        http.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) error("El servidor respondió ${r.code}")
            return r.body?.string() ?: error("Respuesta vacía del servidor")
        }
    }

    /** Verifica usuario y contraseña. Lanza un error con mensaje claro si fallan. */
    fun login(p: Playlist) {
        val root = json.parseToJsonElement(get(api(p))).jsonObject
        val info = root["user_info"] as? JsonObject ?: error("No es un servidor Xtream válido")
        val auth = info["auth"]?.jsonPrimitive?.contentOrNull
        if (auth != "1") error("Usuario o contraseña incorrectos")
    }

    fun liveChannels(p: Playlist): List<Channel> {
        val cats = (json.parseToJsonElement(get(api(p, "get_live_categories"))) as? JsonArray).orEmpty()
            .associate {
                val o = it.jsonObject
                (o["category_id"]?.jsonPrimitive?.contentOrNull ?: "") to
                    (o["category_name"]?.jsonPrimitive?.contentOrNull ?: "Sin categoría")
            }
        val streams = json.parseToJsonElement(get(api(p, "get_live_streams"))).jsonArray
        return streams.mapIndexedNotNull { i, el ->
            val o = el.jsonObject
            val streamId = o["stream_id"]?.jsonPrimitive?.contentOrNull ?: return@mapIndexedNotNull null
            Channel(
                id = "xt-$streamId",
                number = o["num"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: (i + 1),
                name = o["name"]?.jsonPrimitive?.contentOrNull ?: "Canal ${i + 1}",
                group = cats[o["category_id"]?.jsonPrimitive?.contentOrNull ?: ""] ?: "Sin categoría",
                logo = o["stream_icon"]?.jsonPrimitive?.contentOrNull?.ifBlank { null },
                // HLS (.m3u8) porque es el formato que mejor reproducen ExoPlayer y Roku.
                streamUrl = "${baseUrl(p)}/live/${p.username}/${p.password}/$streamId.m3u8",
                epgId = o["epg_channel_id"]?.jsonPrimitive?.contentOrNull?.ifBlank { null },
            )
        }
    }

    fun epgUrl(p: Playlist): String =
        "${baseUrl(p)}/xmltv.php".toHttpUrl().newBuilder()
            .addQueryParameter("username", p.username)
            .addQueryParameter("password", p.password)
            .build().toString()
}
