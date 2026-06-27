package com.fluxtv.app.activities

import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.common.C
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import android.util.Base64
import com.bumptech.glide.Glide
import com.fluxtv.app.databinding.ActivityPlayerBinding
import com.fluxtv.app.models.Channel
import kotlinx.coroutines.*

@UnstableApi
class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var channels: List<Channel> = listOf()
    private var idx = 0
    private val scope = CoroutineScope(Dispatchers.Main)
    private var retries = 0
    private var loadTimer: CountDownTimer? = null
    private var controlsTimer: CountDownTimer? = null
    private var urlIdx = 0 // índice de URL múltiple
    private var networkMonitor: com.fluxtv.app.utils.NetworkMonitor? = null
    private var wasDisconnected = false
    private var isRetrying = false
    private var wasReady = false

    companion object {
        const val EXTRA_CHANNELS = "channels"
        const val EXTRA_INDEX = "index"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        @Suppress("UNCHECKED_CAST")
        channels = (intent.getSerializableExtra(EXTRA_CHANNELS) as? ArrayList<Channel>) ?: arrayListOf()
        idx = intent.getIntExtra(EXTRA_INDEX, 0)

        initPlayer()
        loadChannel(idx)

        networkMonitor = com.fluxtv.app.utils.NetworkMonitor(this).apply {
            onDisconnected = {
                wasDisconnected = true
                runOnUiThread {
                    android.widget.Toast.makeText(this@PlayerActivity, "Sin conexión a internet", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            onConnected = {
                if (wasDisconnected && !isRetrying) {
                    wasDisconnected = false
                    runOnUiThread {
                        android.widget.Toast.makeText(this@PlayerActivity, "Conexión restaurada", android.widget.Toast.LENGTH_SHORT).show()
                        retries = 0; urlIdx = 0
                        loadChannel(idx)
                    }
                }
            }
            start()
        }
    }

    private fun initPlayer() {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(30000, 120000, 5000, 10000)
            .setBackBuffer(30000, true)
            .build()
        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .build()
        player?.setVideoTextureView(binding.textureView)
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> { showLoading(false); retries = 0; urlIdx = 0; loadTimer?.cancel(); isRetrying = false; wasReady = true }
                    Player.STATE_BUFFERING -> { showLoading(true); if (retries == 0) startLoadTimer() }
                    Player.STATE_ENDED -> nextChannel()
                    else -> {}
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                loadTimer?.cancel()
                // Intentar URL alternativa
                val urls = getUrls(channels[idx])
                if (urlIdx < urls.size - 1) {
                    urlIdx++
                    playUrl(urls[urlIdx])
                } else if (retries < 5) {
                    val backoff = (1000L * (retries + 1)).coerceAtMost(8000L)
                    retries++; urlIdx = 0
                    if (!isRetrying) { isRetrying = true; scope.launch { delay(backoff); loadChannel(idx); isRetrying = false } }
                } else {
                    // Ultimo intento: refrescar datos del canal desde la API (token/url puede haber cambiado)
                    scope.launch {
                        val fresh = withContext(Dispatchers.IO) {
                            try {
                                com.fluxtv.app.services.ApiService.getChannels()
                                    .firstOrNull { it.id == channels[idx].id }
                            } catch (_: Exception) { null }
                        }
                        if (fresh != null && fresh.streamUrl.isNotEmpty() && fresh.streamUrl != channels[idx].streamUrl) {
                            channels = channels.toMutableList().also { it[idx] = fresh }
                            retries = 0; urlIdx = 0
                            loadChannel(idx)
                        } else {
                            retries = 0; showLoading(false)
                        }
                    }
                }
            }
        })
    }


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

    private fun getUrls(ch: Channel): List<String> {
        return ch.streamUrl.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun loadChannel(i: Int) {
        loadTimer?.cancel()
        wasReady = false
        if (channels.isEmpty()) return
        val ch = channels[i]; idx = i; retries = 0; urlIdx = 0
        showLoading(true); hideControls()
        updateZapOverlay(ch, i)
        val urls = getUrls(ch)
        if (urls.isNotEmpty()) playUrl(urls[0])
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try { com.fluxtv.app.services.ApiService.startWatching(ch.id, ch.name) } catch (_: Exception) {}
        }

        // Registrar en historial
        if (ch.id.isNotEmpty()) {
            val type = when {
                ch.id.contains("_s") && ch.id.contains("e") -> "serie"
                ch.isLive -> "channel"
                else -> "movie"
            }
            com.fluxtv.app.utils.WatchHistory.add(this, com.fluxtv.app.utils.HistoryEntry(
                ch.id, ch.name, ch.category, ch.logoUrl, ch.streamUrl, type, System.currentTimeMillis()
            ))
        }
    }

    private fun playUrl(url: String) {
        val ch = channels[idx]
        val headers = mapOf("User-Agent" to "Mozilla/5.0") + ch.headers
        val dsf = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(headers)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
        val src = when {
            url.contains(".m3u8") || url.contains(".ts") ->
                HlsMediaSource.Factory(dsf).createMediaSource(MediaItem.fromUri(url))
            url.contains(".mpd") -> {
                val dashFactory = DashMediaSource.Factory(dsf)
                buildClearKeySessionManager(ch.drmKeys)?.let { drmMgr -> dashFactory.setDrmSessionManagerProvider { drmMgr } }
                dashFactory.createMediaSource(MediaItem.fromUri(url))
            }
            url.startsWith("rtsp://") ->
                RtspMediaSource.Factory().createMediaSource(MediaItem.fromUri(url))
            else -> ProgressiveMediaSource.Factory(dsf).createMediaSource(MediaItem.fromUri(url))
        }
        player?.stop(); player?.setMediaSource(src); player?.prepare(); player?.play()
        if (ch.id.isNotEmpty()) {
            val savedPos = com.fluxtv.app.utils.Prefs.getProgress(this, ch.id)
            if (savedPos > 0 && !ch.isLive) {
                player?.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            player?.seekTo(savedPos)
                            player?.removeListener(this)
                        }
                    }
                })
            }
        }
    }

    private fun updateZapOverlay(ch: Channel, i: Int) {
        binding.tvNumber.text = "${i + 1}"
        binding.tvName.text = ch.name
        binding.tvCategory.text = ch.category
        if (ch.logoUrl.isNotEmpty()) Glide.with(this).load(ch.logoUrl).into(binding.ivLogo)

        // Actualizar controles también
        binding.tvControlName.text = ch.name
        binding.tvControlCategory.text = ch.category
        binding.tvControlNumber.text = "${i + 1} / ${channels.size}"
        if (ch.logoUrl.isNotEmpty()) Glide.with(this).load(ch.logoUrl).into(binding.ivControlLogo)
    }

    private fun showLoading(show: Boolean) {
        binding.layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showControls() {
        binding.layoutControls.visibility = View.VISIBLE
        controlsTimer?.cancel()
        controlsTimer = object : CountDownTimer(4000, 4000) {
            override fun onTick(ms: Long) {}
            override fun onFinish() { hideControls() }
        }.start()
    }

    private fun hideControls() {
        binding.layoutControls.visibility = View.GONE
        controlsTimer?.cancel()
    }

    private fun startLoadTimer() {
        loadTimer?.cancel()
        loadTimer = object : CountDownTimer(15000, 15000) {
            override fun onTick(ms: Long) {}
            override fun onFinish() {
                if (!isRetrying && retries < 3) { isRetrying = true; retries++; scope.launch { delay(500); loadChannel(idx); isRetrying = false } }
                else if (retries >= 3) showLoading(false)
            }
        }.start()
    }

    private fun nextChannel() { if (channels.isNotEmpty()) loadChannel((idx + 1) % channels.size) }
    private fun prevChannel() { if (channels.isNotEmpty()) loadChannel((idx - 1 + channels.size) % channels.size) }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_CHANNEL_UP -> { nextChannel(); true }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_CHANNEL_DOWN -> { prevChannel(); true }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (binding.layoutControls.visibility == View.VISIBLE) hideControls()
                else showControls(); true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_MENU -> { showOptionsMenu(); true }
            KeyEvent.KEYCODE_BACK -> { finish(); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    // ===== Menu de opciones: Velocidad / Volumen / Calidad =====
    private fun showOptionsMenu() {
        val items = arrayOf("⏩ Velocidad de reproducción", "🔊 Volumen", "🎬 Calidad")
        android.app.AlertDialog.Builder(this)
            .setTitle("Opciones")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showSpeedSelector()
                    1 -> showVolumeSelector()
                    2 -> showQualitySelector()
                }
            }.show()
    }

    private fun showSpeedSelector() {
        val speeds = floatArrayOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
        val labels = speeds.map { "${it}x" }.toTypedArray()
        val current = player?.playbackParameters?.speed ?: 1f
        val checked = speeds.indexOfFirst { it == current }.coerceAtLeast(2)
        android.app.AlertDialog.Builder(this)
            .setTitle("Velocidad de reproducción")
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                player?.setPlaybackSpeed(speeds[which])
                dialog.dismiss()
            }.show()
    }

    private fun showVolumeSelector() {
        val levels = intArrayOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)
        val labels = levels.map { "$it%" }.toTypedArray()
        val currentVol = ((player?.volume ?: 1f) * 100).toInt()
        val checked = levels.indexOfFirst { it == currentVol }.coerceAtLeast(levels.size - 1)
        android.app.AlertDialog.Builder(this)
            .setTitle("Volumen")
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                player?.volume = levels[which] / 100f
                dialog.dismiss()
            }.show()
    }

    private fun showQualitySelector() {
        val tracks = player?.currentTracks
        val videoGroup = tracks?.groups?.firstOrNull { it.type == androidx.media3.common.C.TRACK_TYPE_VIDEO }
        if (videoGroup == null || videoGroup.length <= 1) {
            android.widget.Toast.makeText(this, "Solo hay una calidad disponible", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val qualities = mutableListOf("Automática")
        for (i in 0 until videoGroup.length) {
            val fmt = videoGroup.getTrackFormat(i)
            qualities.add("${fmt.height}p (${(fmt.bitrate / 1000)}kbps)")
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Calidad de video")
            .setItems(qualities.toTypedArray()) { _, which ->
                val trackSelector = player?.trackSelector as? androidx.media3.exoplayer.trackselection.DefaultTrackSelector
                if (which == 0) {
                    trackSelector?.setParameters(trackSelector.buildUponParameters().clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_VIDEO))
                } else {
                    val fmt = videoGroup.getTrackFormat(which - 1)
                    val override = androidx.media3.common.TrackSelectionOverride(videoGroup.mediaTrackGroup, which - 1)
                    trackSelector?.setParameters(trackSelector.buildUponParameters().setOverrideForType(override))
                }
            }.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            val ch = channels.getOrNull(idx)
            val pos = player?.currentPosition ?: 0L
            val dur = player?.duration ?: 0L
            if (ch != null && ch.id.isNotEmpty()) {
                com.fluxtv.app.utils.Prefs.saveProgress(this, ch.id, pos, dur)
            }
        } catch (_: Exception) {}
        try { com.fluxtv.app.services.ApiService.stopWatching() } catch (_: Exception) {}
        loadTimer?.cancel(); controlsTimer?.cancel(); scope.cancel(); player?.release(); networkMonitor?.stop()
    }
}
