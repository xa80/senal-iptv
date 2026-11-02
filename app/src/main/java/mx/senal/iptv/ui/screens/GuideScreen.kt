package mx.senal.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.senal.iptv.data.Channel
import mx.senal.iptv.data.Repository.Companion.HOUR
import mx.senal.iptv.ui.UiState
import mx.senal.iptv.ui.theme.Senal

private val PER_HOUR: Dp = 160.dp
private val ROW_H: Dp = 72.dp
private const val HOURS = 8
private const val MAX_ROWS = 150

@Composable
fun GuideScreen(state: UiState, onOpen: (Channel) -> Unit) {
    val now = System.currentTimeMillis()
    // La ventana empieza una hora antes de la hora en punto actual.
    val windowStart = remember(now / HOUR) { (now / HOUR) * HOUR - HOUR }
    val windowEnd = windowStart + HOURS * HOUR
    val rows = remember(state.channels, state.epg) {
        state.channels.filter { it.epgId != null && state.epg.containsKey(it.epgId) }.take(MAX_ROWS)
    }
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    val density = LocalDensity.current

    fun x(t: Long): Dp = PER_HOUR * ((t - windowStart).toFloat() / HOUR)

    LaunchedEffect(Unit) {
        // Arranca mostrando la hora actual cerca del borde izquierdo.
        hScroll.scrollTo(with(density) { (x(now) - 40.dp).coerceAtLeast(0.dp).roundToPx() })
    }

    Column(Modifier.fillMaxSize().background(Senal.Fondo)) {
        Text(
            "Guía", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Senal.Texto,
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 12.dp),
        )

        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (state.channels.isEmpty()) "Agrega una lista para ver la guía."
                    else "Tu lista no trae guía de programación, o todavía se está cargando.",
                    color = Senal.Tenue, textAlign = TextAlign.Center,
                )
            }
            return
        }

        Row(Modifier.fillMaxSize().verticalScroll(vScroll)) {
            // Columna fija de logos
            Column(Modifier.width(76.dp)) {
                Box(Modifier.height(36.dp))
                rows.forEach { c ->
                    Box(
                        Modifier.height(ROW_H).fillMaxWidth().clickable { onOpen(c) },
                        contentAlignment = Alignment.Center,
                    ) { ChannelLogo(c, size = 44.dp) }
                }
            }
            // Línea de tiempo con scroll horizontal
            Box(Modifier.horizontalScroll(hScroll)) {
                Column(Modifier.width(PER_HOUR * HOURS)) {
                    Box(Modifier.height(36.dp).fillMaxWidth()) {
                        for (h in 0 until HOURS) {
                            Text(
                                hhmm(windowStart + h * HOUR), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Senal.Tenue,
                                modifier = Modifier.offset(x = PER_HOUR * h).padding(start = 8.dp, top = 10.dp),
                            )
                        }
                    }
                    rows.forEach { c ->
                        Box(Modifier.height(ROW_H).fillMaxWidth()) {
                            c.epgId?.let { state.epg[it] }.orEmpty()
                                .filter { it.stopMillis > windowStart && it.startMillis < windowEnd }
                                .forEach { p ->
                                    val start = maxOf(p.startMillis, windowStart)
                                    val end = minOf(p.stopMillis, windowEnd)
                                    val live = p.isLive(now)
                                    val shape = RoundedCornerShape(10.dp)
                                    Column(
                                        Modifier.offset(x = x(start)).width(x(end) - x(start)).fillMaxHeight()
                                            .padding(horizontal = 3.dp, vertical = 6.dp)
                                            .clip(shape)
                                            .background(if (live) Senal.Superficie2 else Senal.Superficie)
                                            .then(if (live) Modifier.border(1.dp, Senal.Acento, shape) else Modifier)
                                            .clickable(enabled = live) { onOpen(c) }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(p.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Senal.Texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(hhmm(p.startMillis), fontSize = 11.sp, color = if (live) Senal.Texto2 else Senal.Tenue, maxLines = 1)
                                    }
                                }
                        }
                    }
                }
                // Línea de "ahora"
                Box(
                    Modifier.offset(x = x(now)).width(2.dp)
                        .height(36.dp + ROW_H * rows.size).background(Senal.Acento),
                )
            }
        }
    }
}
