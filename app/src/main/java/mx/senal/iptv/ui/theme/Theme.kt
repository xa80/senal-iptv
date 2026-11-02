package mx.senal.iptv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Senal {
    val Fondo = Color(0xFF0D1117)
    val Superficie = Color(0xFF161B24)
    val Superficie2 = Color(0xFF2A3241)
    val Borde = Color(0xFF1F2633)
    val Texto = Color(0xFFF2F4F8)
    val Texto2 = Color(0xFFC5CDD8)
    val Tenue = Color(0xFF9BA6B6)
    val Acento = Color(0xFFFF8A3D)
    val SobreAcento = Color(0xFF1A0E05)
    val Vivo = Color(0xFFE5484D)

    /** Colores para las fichas de canales sin logo. */
    val Fichas = listOf(
        Color(0xFF7FB2FF), Color(0xFF7FE0A8), Color(0xFFF5C542), Color(0xFFFF9EC4),
        Color(0xFFB9A3FF), Color(0xFFFFB27F),
    )
}

@Composable
fun SenalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Senal.Acento,
            onPrimary = Senal.SobreAcento,
            background = Senal.Fondo,
            onBackground = Senal.Texto,
            surface = Senal.Fondo,
            onSurface = Senal.Texto,
            surfaceVariant = Senal.Superficie,
            onSurfaceVariant = Senal.Tenue,
            outline = Senal.Superficie2,
            error = Senal.Vivo,
        ),
        content = content,
    )
}
