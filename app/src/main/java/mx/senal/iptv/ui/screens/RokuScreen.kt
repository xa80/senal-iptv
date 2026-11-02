package mx.senal.iptv.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.senal.iptv.roku.RokuDevice
import mx.senal.iptv.ui.UiState
import mx.senal.iptv.ui.theme.Senal

@Composable
fun RokuScreen(
    state: UiState,
    onSearch: () -> Unit,
    onAddIp: (String) -> Unit,
    onConnect: (RokuDevice) -> Unit,
    onDisconnect: () -> Unit,
    onKey: (String) -> Unit,
    onChannel: (Int) -> Unit,
) {
    LaunchedEffect(Unit) { if (state.roku == null && state.rokus.isEmpty()) onSearch() }

    Column(
        Modifier.fillMaxSize().background(Senal.Fondo).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Enviar a Roku", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Senal.Texto)
        val roku = state.roku
        if (roku == null) DeviceList(state, onSearch, onAddIp, onConnect)
        else Remote(state, roku, onDisconnect, onKey, onChannel)
    }
}

@Composable
private fun DeviceList(
    state: UiState,
    onSearch: () -> Unit,
    onAddIp: (String) -> Unit,
    onConnect: (RokuDevice) -> Unit,
) {
    val context = LocalContext.current
    var ip by rememberSaveable { mutableStateOf("") }

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Senal.Superficie).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.Wifi, contentDescription = null, tint = Senal.Acento)
        Text(
            "Tu celular y tu Roku deben estar en la misma red Wi‑Fi. No necesitas instalar nada en el Roku.",
            fontSize = 14.sp, color = Senal.Texto2,
        )
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("DISPOSITIVOS ENCONTRADOS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Senal.Tenue, modifier = Modifier.weight(1f))
        if (state.searchingRoku) CircularProgressIndicator(Modifier.size(20.dp), color = Senal.Acento, strokeWidth = 2.dp)
        else TextButton(onClick = onSearch) { Text("Buscar de nuevo", color = Senal.Acento, fontWeight = FontWeight.Bold) }
    }

    state.rokus.forEach { d ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, Senal.Superficie2, RoundedCornerShape(14.dp))
                .background(Senal.Superficie).clickable { onConnect(d) }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Senal.Superficie2), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Tv, contentDescription = null, tint = Senal.Texto)
            }
            Column(Modifier.weight(1f)) {
                Text(d.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Senal.Texto)
                Text(d.model, fontSize = 12.sp, color = Senal.Tenue)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Senal.Tenue)
        }
    }
    state.rokuMessage?.let { Text(it, color = Senal.Texto2, fontSize = 14.sp) }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, Senal.Superficie2, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("¿Tu Roku no aparece?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Senal.Texto)
        Text(
            "Escribe su IP (en el Roku: Configuración › Red › Acerca de) o duplica la pantalla del celular.",
            fontSize = 13.sp, color = Senal.Tenue,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ip, onValueChange = { ip = it }, singleLine = true,
                placeholder = { Text("192.168.1.45") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Senal.Acento, unfocusedBorderColor = Senal.Superficie2,
                    focusedTextColor = Senal.Texto, unfocusedTextColor = Senal.Texto, cursorColor = Senal.Acento,
                ),
            )
            Button(
                onClick = { onAddIp(ip) }, enabled = ip.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Senal.Acento, contentColor = Senal.SobreAcento),
            ) { Text("Agregar", fontWeight = FontWeight.Bold) }
        }
        TextButton(onClick = {
            // Abre los ajustes de transmisión del teléfono (Miracast) cuando el modelo los ofrece.
            runCatching { context.startActivity(Intent(Settings.ACTION_CAST_SETTINGS)) }
        }) { Text("Duplicar pantalla", color = Senal.Acento, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun Remote(
    state: UiState,
    roku: RokuDevice,
    onDisconnect: () -> Unit,
    onKey: (String) -> Unit,
    onChannel: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Senal.Superficie).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.current?.let { ChannelLogo(it) }
        Column(Modifier.weight(1f)) {
            Text("EN ${roku.name.uppercase()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Senal.Acento)
            Text(state.current?.name ?: "Elige un canal y tócalo para enviarlo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Senal.Texto)
            state.current?.let { c -> state.nowPlaying(c)?.let { Text(it.title, fontSize = 13.sp, color = Senal.Tenue) } }
        }
    }
    state.rokuMessage?.let { Text(it, color = Senal.Texto2, fontSize = 14.sp) }

    // Cruceta
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(232.dp).clip(CircleShape).background(Senal.Superficie)) {
            PadKey(Icons.Filled.KeyboardArrowUp, "Arriba", Modifier.align(Alignment.TopCenter).padding(top = 10.dp)) { onKey("Up") }
            PadKey(Icons.Filled.KeyboardArrowDown, "Abajo", Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)) { onKey("Down") }
            PadKey(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Izquierda", Modifier.align(Alignment.CenterStart).padding(start = 10.dp)) { onKey("Left") }
            PadKey(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Derecha", Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)) { onKey("Right") }
            Button(
                onClick = { onKey("Select") },
                modifier = Modifier.align(Alignment.Center).size(76.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Senal.Acento, contentColor = Senal.SobreAcento),
            ) { Text("OK", fontWeight = FontWeight.ExtraBold) }
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SquareKey(Icons.AutoMirrored.Outlined.Undo, "Regresar", Modifier.weight(1f)) { onKey("Back") }
        SquareKey(Icons.Outlined.Home, "Inicio", Modifier.weight(1f)) { onKey("Home") }
        SquareKey(Icons.Outlined.PlayArrow, "Reproducir o pausar", Modifier.weight(1f)) { onKey("Play") }
        SquareKey(Icons.AutoMirrored.Outlined.VolumeDown, "Bajar volumen", Modifier.weight(1f)) { onKey("VolumeDown") }
        SquareKey(Icons.AutoMirrored.Outlined.VolumeUp, "Subir volumen", Modifier.weight(1f)) { onKey("VolumeUp") }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { onChannel(-1) }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Text("Canal −", color = Senal.Texto, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(onClick = { onChannel(1) }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Text("Canal +", color = Senal.Texto, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
        Text("Desconectar", color = Senal.Texto, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PadKey(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick, modifier = modifier.size(56.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Senal.Superficie, contentColor = Senal.Texto),
    ) { Icon(icon, contentDescription = label, modifier = Modifier.size(30.dp)) }
}

@Composable
private fun SquareKey(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick, modifier = modifier.height(56.dp), shape = RoundedCornerShape(14.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Senal.Superficie, contentColor = Senal.Texto),
    ) { Icon(icon, contentDescription = label) }
}
