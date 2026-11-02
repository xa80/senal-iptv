package mx.senal.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.senal.iptv.data.Playlist
import mx.senal.iptv.data.PlaylistType
import mx.senal.iptv.ui.UiState
import mx.senal.iptv.ui.theme.Senal

@Composable
fun SettingsScreen(
    state: UiState,
    onOpen: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit,
    onAdd: () -> Unit,
) {
    var toDelete by remember { mutableStateOf<Playlist?>(null) }

    Column(
        Modifier.fillMaxSize().background(Senal.Fondo).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Ajustes", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Senal.Texto)
        Text("MIS LISTAS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Senal.Tenue)

        state.playlists.forEach { p ->
            val active = p.id == state.active?.id
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(Senal.Superficie)
                    .then(if (active) Modifier.border(1.dp, Senal.Acento, shape) else Modifier)
                    .clickable { onOpen(p) }.padding(start = 14.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Senal.Texto)
                    Text(
                        (if (p.type == PlaylistType.XTREAM) "Xtream Codes" else "M3U") + if (active) " · En uso" else "",
                        fontSize = 12.sp, color = if (active) Senal.Acento else Senal.Tenue,
                    )
                }
                IconButton(onClick = { toDelete = p }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Eliminar ${p.name}", tint = Senal.Tenue)
                }
            }
        }

        Button(
            onClick = onAdd, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Senal.Acento, contentColor = Senal.SobreAcento),
        ) { Text("Agregar otra lista", fontWeight = FontWeight.ExtraBold) }

        Text("ACERCA DE", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Senal.Tenue, modifier = Modifier.padding(top = 12.dp))
        Text(
            "Señal 0.1 · Reproductor de listas IPTV. No incluye ni vende canales; " +
                "cada usuario es responsable del contenido de las listas que agrega.",
            fontSize = 13.sp, color = Senal.Tenue,
        )
    }

    toDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("¿Eliminar «${p.name}»?") },
            text = { Text("Se borra del teléfono junto con sus canales guardados.") },
            confirmButton = { TextButton(onClick = { onDelete(p); toDelete = null }) { Text("Eliminar", color = Senal.Vivo) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } },
            containerColor = Senal.Superficie,
        )
    }
}
