package mx.senal.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import mx.senal.iptv.data.Channel
import mx.senal.iptv.ui.theme.Senal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Logo del canal; si no tiene o no carga, muestra una ficha con iniciales. */
@Composable
fun ChannelLogo(c: Channel, size: Dp = 52.dp) {
    val shape = RoundedCornerShape(12.dp)
    val color = Senal.Fichas[abs(c.name.hashCode()) % Senal.Fichas.size]
    val fallback: @Composable () -> Unit = {
        Box(
            Modifier.size(size).clip(shape).background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials(c.name), color = Senal.Fondo, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
    if (c.logo == null) {
        fallback()
    } else {
        SubcomposeAsyncImage(
            model = c.logo,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size).clip(shape).background(Senal.Superficie2),
            loading = { fallback() },
            error = { fallback() },
        )
    }
}

fun initials(name: String): String =
    name.split(' ', '-', '_').filter { it.isNotBlank() }.take(3)
        .joinToString("") { it.first().uppercase() }.ifBlank { "TV" }

private val hourFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
fun hhmm(millis: Long): String = hourFmt.format(Date(millis))
