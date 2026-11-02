package mx.senal.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.senal.iptv.data.Channel
import mx.senal.iptv.ui.UiState
import mx.senal.iptv.ui.theme.Senal

private const val TODOS = "Todos"
private const val FAVORITOS = "Favoritos"

@Composable
fun ChannelsScreen(
    state: UiState,
    onOpen: (Channel) -> Unit,
    onRoku: () -> Unit,
    onRefresh: () -> Unit,
) {
    var category by rememberSaveable { mutableStateOf(TODOS) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val shown = remember(state.channels, state.favorites, category, query) {
        state.channels.filter { c ->
            val inCat = when (category) {
                TODOS -> true
                FAVORITOS -> c.id in state.favorites
                else -> c.group == category
            }
            inCat && (query.isBlank() || c.name.contains(query, ignoreCase = true))
        }
    }

    Column(Modifier.fillMaxSize().background(Senal.Fondo)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Lista: ${state.active?.name ?: "—"}", fontSize = 13.sp, color = Senal.Tenue, fontWeight = FontWeight.SemiBold)
                Text("En vivo", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Senal.Texto)
            }
            IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                Icon(if (searching) Icons.Outlined.Close else Icons.Outlined.Search, contentDescription = "Buscar canal", tint = Senal.Texto)
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar lista", tint = Senal.Texto)
            }
            IconButton(onClick = onRoku) {
                Icon(Icons.Outlined.Cast, contentDescription = "Enviar a Roku", tint = Senal.Acento)
            }
        }

        if (searching) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Buscar canal") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Senal.Acento, unfocusedBorderColor = Senal.Superficie2,
                    focusedContainerColor = Senal.Superficie, unfocusedContainerColor = Senal.Superficie,
                    focusedTextColor = Senal.Texto, unfocusedTextColor = Senal.Texto, cursorColor = Senal.Acento,
                ),
            )
        }

        val cats = listOf(TODOS, FAVORITOS) + state.groups
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(cats) { cat ->
                FilterChip(
                    selected = cat == category,
                    onClick = { category = cat },
                    label = { Text(cat, maxLines = 1) },
                    shape = RoundedCornerShape(999.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Senal.Fondo,
                        labelColor = Senal.Texto2,
                        selectedContainerColor = Senal.Acento,
                        selectedLabelColor = Senal.SobreAcento,
                    ),
                )
            }
        }

        when {
            state.loading && state.channels.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Senal.Acento)
            }
            state.error != null && state.channels.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(state.error, color = Senal.Texto2, textAlign = TextAlign.Center)
            }
            shown.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (category == FAVORITOS) "Aún no tienes favoritos. Ábrelos desde el reproductor." else "No hay canales aquí.",
                    color = Senal.Tenue, textAlign = TextAlign.Center,
                )
            }
            else -> LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                items(shown, key = { it.id }) { c ->
                    ChannelRow(c, state, onClick = { onOpen(c) })
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(c: Channel, state: UiState, onClick: () -> Unit) {
    val now = System.currentTimeMillis()
    val prog = state.nowPlaying(c, now)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "${c.number}", modifier = Modifier.width(28.dp), textAlign = TextAlign.End,
            fontSize = 13.sp, color = Senal.Tenue, fontWeight = FontWeight.SemiBold,
        )
        ChannelLogo(c)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(c.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Senal.Texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                prog?.title ?: c.group, fontSize = 13.sp, color = Senal.Tenue,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (prog != null) {
                LinearProgressIndicator(
                    progress = { prog.progress(now) },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = Senal.Acento, trackColor = Senal.Superficie2,
                )
            }
        }
        if (c.id in state.favorites) {
            Icon(Icons.Filled.Favorite, contentDescription = "Favorito", tint = Senal.Acento, modifier = Modifier.size(20.dp))
        }
    }
}
