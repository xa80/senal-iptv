package mx.senal.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.senal.iptv.data.PlaylistType
import mx.senal.iptv.ui.UiState
import mx.senal.iptv.ui.theme.Senal

@Composable
fun AddPlaylistScreen(
    state: UiState,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onSubmit: (name: String, type: PlaylistType, url: String, user: String, pass: String, epg: String) -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(PlaylistType.XTREAM) }
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var user by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var epg by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().background(Senal.Fondo).statusBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (canGoBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Senal.Texto)
                }
            }
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Senal.Acento),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.LiveTv, contentDescription = null, tint = Senal.SobreAcento) }
            Text("Señal", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Senal.Texto)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Agrega tu lista de canales", fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold, color = Senal.Texto)
            Text(
                "Usa los datos que te dio tu proveedor. Señal es un reproductor y no incluye canales.",
                fontSize = 15.sp, color = Senal.Tenue,
            )
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Senal.Superficie).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Segment("Xtream Codes", type == PlaylistType.XTREAM, Modifier.weight(1f)) { type = PlaylistType.XTREAM }
            Segment("URL M3U", type == PlaylistType.M3U, Modifier.weight(1f)) { type = PlaylistType.M3U }
        }

        Field("Nombre de la lista", name, { name = it }, placeholder = "Ej. Casa")
        if (type == PlaylistType.XTREAM) {
            Field("Servidor", url, { url = it }, placeholder = "http://servidor.com:8080", keyboard = KeyboardType.Uri)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field("Usuario", user, { user = it }, modifier = Modifier.weight(1f))
                Field("Contraseña", pass, { pass = it }, modifier = Modifier.weight(1f), password = true)
            }
        } else {
            Field("URL de la lista M3U", url, { url = it }, placeholder = "https://…/lista.m3u", keyboard = KeyboardType.Uri)
            Field("URL de la guía EPG (opcional)", epg, { epg = it }, placeholder = "https://…/guia.xml", keyboard = KeyboardType.Uri)
        }

        state.error?.let { Text(it, color = Senal.Vivo, fontSize = 14.sp) }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onSubmit(name, type, url, user, pass, epg) },
            enabled = !state.loading && url.isNotBlank() && (type == PlaylistType.M3U || (user.isNotBlank() && pass.isNotBlank())),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Senal.Acento, contentColor = Senal.SobreAcento),
        ) {
            if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), color = Senal.SobreAcento, strokeWidth = 2.5.dp)
            else Text("Conectar", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(
            "Solo usa listas de contenido que tengas derecho a ver.",
            fontSize = 12.sp, color = Senal.Tenue, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Senal.Superficie2 else Senal.Superficie,
            contentColor = if (selected) Senal.Texto else Senal.Tenue,
        ),
    ) { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold) }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    placeholder: String = "",
    keyboard: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        singleLine = true,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Senal.Acento,
            unfocusedBorderColor = Senal.Superficie2,
            focusedContainerColor = Senal.Superficie,
            unfocusedContainerColor = Senal.Superficie,
            focusedLabelColor = Senal.Acento,
            unfocusedLabelColor = Senal.Texto2,
            cursorColor = Senal.Acento,
            focusedTextColor = Senal.Texto,
            unfocusedTextColor = Senal.Texto,
        ),
    )
}
