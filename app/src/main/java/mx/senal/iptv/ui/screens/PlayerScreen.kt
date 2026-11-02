package mx.senal.iptv.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import mx.senal.iptv.data.Channel
import mx.senal.iptv.data.Programme
import mx.senal.iptv.ui.UiState
import mx.senal.iptv.ui.theme.Senal

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    state: UiState,
    onBack: () -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onCast: (Channel) -> Unit,
    onNeighbor: (Int) -> Unit,
) {
    val channel = state.current
    if (channel == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val context = LocalContext.current
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var playError by remember { mutableStateOf<String?>(null) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playError = "No se pudo reproducir este canal (${error.errorCodeName})."
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    LaunchedEffect(channel.streamUrl) {
        playError = null
        player.setMediaItem(MediaItem.fromUri(channel.streamUrl))
        player.prepare()
    }
    LaunchedEffect(fullscreen) {
        (context as? Activity)?.requestedOrientation =
            if (fullscreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    val video: @Composable (Modifier) -> Unit = { mod ->
        Box(mod.background(Color.Black)) {
            AndroidView(
                factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
                modifier = Modifier.fillMaxSize(),
            )
            Row(Modifier.align(Alignment.TopStart).padding(12.dp)) {
                Text(
                    "EN VIVO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Senal.Vivo)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            IconButton(onClick = { fullscreen = !fullscreen }, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    if (fullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                    contentDescription = "Pantalla completa", tint = Color.White,
                )
            }
            playError?.let {
                Text(
                    it, color = Color.White, fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                        .clip(RoundedCornerShape(10.dp)).background(Color(0xCC000000)).padding(12.dp),
                )
            }
        }
    }

    if (fullscreen) {
        video(Modifier.fillMaxSize())
        return
    }

    Column(Modifier.fillMaxSize().background(Senal.Fondo).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a canales", tint = Senal.Texto)
            }
            Text(channel.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Senal.Texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        video(Modifier.fillMaxWidth().aspectRatio(16f / 9f))

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChannelLogo(channel)
                Column {
                    Text("Canal ${channel.number} · ${channel.group}", fontSize = 13.sp, color = Senal.Tenue, fontWeight = FontWeight.SemiBold)
                    Text(
                        state.nowPlaying(channel)?.title ?: channel.name,
                        fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Senal.Texto,
                    )
                }
            }

            val fav = channel.id in state.favorites
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onToggleFavorite(channel) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (fav) Senal.Acento else Senal.Texto2),
                ) {
                    Icon(if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = null)
                    Text("  Favorito", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { player.pause(); onCast(channel) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Senal.Acento, contentColor = Senal.SobreAcento),
                ) {
                    Icon(Icons.Outlined.Cast, contentDescription = null)
                    Text("  Enviar a Roku", fontWeight = FontWeight.ExtraBold)
                }
            }

            val now = System.currentTimeMillis()
            val current = state.nowPlaying(channel, now)
            val next = state.nextUp(channel, now)
            if (current != null || next != null) {
                Text("PROGRAMACIÓN", fontSize = 13.sp, color = Senal.Tenue, fontWeight = FontWeight.Bold)
                current?.let { ProgrammeCard("AHORA", it, highlighted = true) }
                next?.let { ProgrammeCard("DESPUÉS", it, highlighted = false) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onNeighbor(-1) }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = Senal.Texto)
                    Text("Anterior", color = Senal.Texto, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { onNeighbor(1) }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Siguiente", color = Senal.Texto, fontWeight = FontWeight.Bold)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Senal.Texto)
                }
            }
        }
    }
}

@Composable
private fun ProgrammeCard(label: String, p: Programme, highlighted: Boolean) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .then(if (highlighted) Modifier.background(Senal.Superficie) else Modifier.border(1.dp, Senal.Borde, shape))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (highlighted) Senal.Acento else Senal.Tenue)
            Text("${hhmm(p.startMillis)} – ${hhmm(p.stopMillis)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Senal.Tenue)
        }
        Text(p.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Senal.Texto)
        if (highlighted && p.description.isNotBlank()) {
            Text(p.description, fontSize = 13.sp, color = Senal.Tenue, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}
