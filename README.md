# Señal — reproductor IPTV para Android

App para ver tus listas IPTV (M3U o Xtream Codes) en el celular y mandarlas a un Roku TV por Wi‑Fi, sin instalar nada en el Roku.

**Señal no incluye canales.** Cada usuario agrega la lista de su proveedor.

## Qué hace (versión 0.1)
- Agregar listas M3U o Xtream Codes, con varias listas guardadas.
- Canales en vivo por categoría, buscador y favoritos.
- Reproductor (ExoPlayer) con pantalla completa y canal anterior/siguiente.
- Guía de programación (XMLTV), con lo que está al aire y lo que sigue.
- **Enviar a Roku**: busca Rokus en tu red (SSDP), manda el canal con ECP (canal integrado "Play on Roku") y muestra un control remoto.
- Respaldo: duplicar pantalla (Miracast) o agregar el Roku por su IP.

## Cómo obtener el APK
Cada vez que se sube código a `main`, GitHub Actions compila la app y deja el APK en la sección **Releases** del repositorio. Descárgalo desde el celular y ábrelo para instalarlo (Android pedirá permitir "instalar apps desconocidas").

## Tecnología
Kotlin · Jetpack Compose · Media3/ExoPlayer · OkHttp · kotlinx.serialization · Coil.
