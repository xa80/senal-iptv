package mx.senal.iptv.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Lee guías XMLTV:
 *   <programme start="20261102200000 -0600" stop="20261102220000 -0600" channel="canal.mx">
 *     <title>…</title><desc>…</desc>
 *   </programme>
 * Solo guarda programas dentro de la ventana [from, to] para no llenar la memoria.
 */
object XmltvParser {

    fun parse(
        input: InputStream,
        wantedIds: Set<String>,
        from: Long,
        to: Long,
    ): Map<String, List<Programme>> {
        val out = HashMap<String, MutableList<Programme>>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        var channel: String? = null
        var start = 0L
        var stop = 0L
        var title = ""
        var desc = ""
        var inProgramme = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "programme" -> {
                        channel = parser.getAttributeValue(null, "channel")
                        start = parseTime(parser.getAttributeValue(null, "start"))
                        stop = parseTime(parser.getAttributeValue(null, "stop"))
                        title = ""
                        desc = ""
                        inProgramme = true
                    }
                    "title" -> if (inProgramme && title.isEmpty()) title = parser.nextText().trim()
                    "desc" -> if (inProgramme && desc.isEmpty()) desc = parser.nextText().trim()
                }
                XmlPullParser.END_TAG -> if (parser.name == "programme") {
                    val id = channel
                    if (id != null && (wantedIds.isEmpty() || id in wantedIds) && stop > from && start < to) {
                        out.getOrPut(id) { mutableListOf() } += Programme(id, title, desc, start, stop)
                    }
                    inProgramme = false
                }
            }
            event = parser.next()
        }
        out.values.forEach { list -> list.sortBy { it.startMillis } }
        return out
    }

    private val formats = listOf("yyyyMMddHHmmss Z", "yyyyMMddHHmmss", "yyyyMMddHHmm")

    private fun parseTime(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        val v = value.trim()
        for (pattern in formats) {
            try {
                val f = SimpleDateFormat(pattern, Locale.US)
                return f.parse(v)?.time ?: continue
            } catch (_: Exception) {
            }
        }
        return 0L
    }
}
