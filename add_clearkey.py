import re

f = "app/src/main/java/com/fluxtv/app/activities/PlayerActivity.kt"
content = open(f, encoding="utf-8").read()

# 1) Agregar imports necesarios para ClearKey
old_imports = """import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.datasource.DefaultDataSource"""

new_imports = """import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.common.C
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import android.util.Base64"""

if old_imports not in content:
    raise SystemExit("No se encontró el bloque de imports esperado")
content = content.replace(old_imports, new_imports, 1)

# 2) Agregar función helper buildClearKeyLicense antes de "private fun getUrls"
helper = '''
    // Convierte HEX a Base64Url sin padding (formato requerido por ClearKey)
    private fun hexToBase64Url(hex: String): String {
        val clean = hex.trim().replace("-", "")
        val bytes = ByteArray(clean.length / 2)
        for (i in bytes.indices) {
            bytes[i] = ((Character.digit(clean[i * 2], 16) shl 4) + Character.digit(clean[i * 2 + 1], 16)).toByte()
        }
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    // drmKeys formato esperado: "kid1:key1,kid2:key2" (hex)
    private fun buildClearKeySessionManager(drmKeys: String): DefaultDrmSessionManager? {
        if (drmKeys.isBlank()) return null
        val keysJson = drmKeys.split(",").mapNotNull { pair ->
            val parts = pair.trim().split(":")
            if (parts.size != 2) return@mapNotNull null
            val kid = hexToBase64Url(parts[0])
            val k = hexToBase64Url(parts[1])
            """{"kty":"oct","kid":"$kid","k":"$k"}"""
        }
        if (keysJson.isEmpty()) return null
        val license = """{"keys":[${keysJson.joinToString(",")}],"type":"temporary"}"""
        return DefaultDrmSessionManager.Builder()
            .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
            .build(LocalMediaDrmCallback(license.toByteArray()))
    }

'''

marker = "    private fun getUrls(ch: Channel): List<String> {"
if marker not in content:
    raise SystemExit("No se encontró el marcador para insertar el helper")
content = content.replace(marker, helper + marker, 1)

# 3) Usar ClearKey en el caso .mpd
old_dash = """            url.contains(".mpd") ->
                DashMediaSource.Factory(DefaultDataSource.Factory(this)).createMediaSource(MediaItem.fromUri(url))"""

new_dash = """            url.contains(".mpd") -> {
                val dashFactory = DashMediaSource.Factory(DefaultDataSource.Factory(this))
                buildClearKeySessionManager(ch.drmKeys)?.let { dashFactory.setDrmSessionManagerProvider { it } }
                dashFactory.createMediaSource(MediaItem.fromUri(url))
            }"""

if old_dash not in content:
    raise SystemExit("No se encontró el bloque DASH esperado")
content = content.replace(old_dash, new_dash, 1)

open(f, "w", encoding="utf-8").write(content)
print("OK - ClearKey integrado")
