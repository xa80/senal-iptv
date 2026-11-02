package mx.senal.iptv.roku

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

data class RokuDevice(val ip: String, val name: String, val model: String) {
    val base get() = "http://$ip:8060"
}

/**
 * Habla con el Roku por la red local usando ECP (External Control Protocol, puerto 8060).
 * No requiere instalar nada en el Roku: usa el canal integrado "Play on Roku" (id 15985).
 */
class RokuClient(private val context: Context) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    /** Busca Rokus en el Wi‑Fi con SSDP (búsqueda "roku:ecp"). */
    suspend fun discover(timeoutMs: Int = 3500): List<RokuDevice> = withContext(Dispatchers.IO) {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifi.createMulticastLock("senal-ssdp").apply { setReferenceCounted(false); acquire() }
        val ips = linkedSetOf<String>()
        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = 500
                val msg = (
                    "M-SEARCH * HTTP/1.1\r\n" +
                        "Host: 239.255.255.250:1900\r\n" +
                        "Man: \"ssdp:discover\"\r\n" +
                        "ST: roku:ecp\r\n" +
                        "MX: 3\r\n\r\n"
                    ).toByteArray()
                val group = InetAddress.getByName("239.255.255.250")
                repeat(2) { socket.send(DatagramPacket(msg, msg.size, group, 1900)) }

                val buf = ByteArray(2048)
                val end = System.currentTimeMillis() + timeoutMs
                while (System.currentTimeMillis() < end) {
                    try {
                        val pkt = DatagramPacket(buf, buf.size)
                        socket.receive(pkt)
                        val text = String(pkt.data, 0, pkt.length)
                        val loc = text.lineSequence()
                            .firstOrNull { it.startsWith("LOCATION:", ignoreCase = true) }
                            ?.substringAfter(":")?.trim()
                        val ip = loc?.let { runCatching { it.toHttpUrl().host }.getOrNull() }
                            ?: pkt.address.hostAddress
                        if (ip != null) ips += ip
                    } catch (_: SocketTimeoutException) {
                    }
                }
            }
        } finally {
            lock.release()
        }
        ips.mapNotNull { info(it) }
    }

    /** Lee nombre y modelo. También sirve para agregar un Roku a mano por su IP. */
    suspend fun info(ip: String): RokuDevice? = withContext(Dispatchers.IO) {
        runCatching {
            http.newCall(Request.Builder().url("http://$ip:8060/query/device-info").build()).execute().use { r ->
                if (!r.isSuccessful) return@use null
                val xml = r.body?.string().orEmpty()
                fun tag(t: String) = Regex("<$t>(.*?)</$t>", RegexOption.DOT_MATCHES_ALL)
                    .find(xml)?.groupValues?.get(1)?.trim()?.ifBlank { null }
                RokuDevice(
                    ip = ip,
                    name = tag("user-device-name") ?: tag("friendly-device-name") ?: "Roku",
                    model = listOfNotNull(tag("model-name"), ip).joinToString(" · "),
                )
            }
        }.getOrNull()
    }

    /** Manda un stream al Roku. Devuelve true si el Roku aceptó la orden. */
    suspend fun play(device: RokuDevice, streamUrl: String, title: String): Boolean =
        withContext(Dispatchers.IO) {
            val format = when {
                streamUrl.contains(".m3u8", ignoreCase = true) -> "hls"
                streamUrl.contains(".mp4", ignoreCase = true) -> "mp4"
                streamUrl.contains(".mkv", ignoreCase = true) -> "mkv"
                else -> "hls"
            }
            fun url(path: String): HttpUrl = "${device.base}/$path".toHttpUrl().newBuilder()
                .addQueryParameter("t", "v")
                .addQueryParameter("u", streamUrl)
                .addQueryParameter("k", "(null)")
                .addQueryParameter("videoName", title)
                .addQueryParameter("videoFormat", format)
                .build()
            post(url("input/15985")) || post(url("launch/15985"))
        }

    /** Teclas del control: Up, Down, Left, Right, Select, Back, Home, Play, Rev, Fwd, VolumeUp, VolumeDown, VolumeMute. */
    suspend fun key(device: RokuDevice, key: String): Boolean = withContext(Dispatchers.IO) {
        post("${device.base}/keypress/$key".toHttpUrl())
    }

    private fun post(url: HttpUrl): Boolean = runCatching {
        http.newCall(
            Request.Builder().url(url).post(ByteArray(0).toRequestBody("text/plain".toMediaType())).build()
        ).execute().use { it.isSuccessful }
    }.getOrDefault(false)
}
