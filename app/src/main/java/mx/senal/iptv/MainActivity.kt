package mx.senal.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import mx.senal.iptv.ui.AppViewModel
import mx.senal.iptv.ui.screens.AddPlaylistScreen
import mx.senal.iptv.ui.screens.ChannelsScreen
import mx.senal.iptv.ui.screens.GuideScreen
import mx.senal.iptv.ui.screens.PlayerScreen
import mx.senal.iptv.ui.screens.RokuScreen
import mx.senal.iptv.ui.screens.SettingsScreen
import mx.senal.iptv.ui.theme.Senal
import mx.senal.iptv.ui.theme.SenalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SenalTheme { SenalApp() } }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("canales", "En vivo", Icons.Outlined.LiveTv),
    Tab("guia", "Guía", Icons.Outlined.GridView),
    Tab("roku", "Roku", Icons.Outlined.Cast),
    Tab("ajustes", "Ajustes", Icons.Outlined.Settings),
)

@Composable
fun SenalApp(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    // Se decide una sola vez al abrir la app.
    val start = remember { if (vm.state.value.playlists.isEmpty()) "agregar" else "canales" }

    fun go(r: String) = nav.navigate(r) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        containerColor = Senal.Fondo,
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar(containerColor = Senal.Fondo) {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = { go(t.route) },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Senal.Acento,
                                selectedTextColor = Senal.Acento,
                                indicatorColor = Senal.Superficie,
                                unselectedIconColor = Senal.Tenue,
                                unselectedTextColor = Senal.Tenue,
                            ),
                        )
                    }
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().background(Senal.Fondo).padding(pad)) {
            NavHost(nav, startDestination = start) {
                composable("agregar") {
                    AddPlaylistScreen(
                        state = state,
                        canGoBack = state.playlists.isNotEmpty(),
                        onBack = { nav.popBackStack() },
                        onSubmit = { name, type, url, user, pass, epg ->
                            vm.addPlaylist(name, type, url, user, pass, epg) {
                                nav.navigate("canales") { popUpTo("agregar") { inclusive = true } }
                            }
                        },
                    )
                }
                composable("canales") {
                    ChannelsScreen(
                        state = state,
                        onOpen = { vm.select(it); nav.navigate("reproductor") },
                        onRoku = { go("roku") },
                        onRefresh = { vm.refresh() },
                    )
                }
                composable("reproductor") {
                    PlayerScreen(
                        state = state,
                        onBack = { nav.popBackStack() },
                        onToggleFavorite = vm::toggleFavorite,
                        onCast = { c ->
                            if (state.roku != null) vm.castToRoku(c)
                            nav.navigate("roku")
                        },
                        onNeighbor = { vm.neighbor(it) },
                    )
                }
                composable("guia") {
                    GuideScreen(state = state, onOpen = { vm.select(it); nav.navigate("reproductor") })
                }
                composable("roku") {
                    RokuScreen(
                        state = state,
                        onSearch = vm::searchRokus,
                        onAddIp = vm::addRokuByIp,
                        onConnect = vm::connectRoku,
                        onDisconnect = vm::disconnectRoku,
                        onKey = vm::rokuKey,
                        onChannel = vm::rokuChannel,
                    )
                }
                composable("ajustes") {
                    SettingsScreen(
                        state = state,
                        onOpen = { vm.open(it); go("canales") },
                        onDelete = vm::deletePlaylist,
                        onAdd = { nav.navigate("agregar") },
                    )
                }
            }
        }
    }
}
