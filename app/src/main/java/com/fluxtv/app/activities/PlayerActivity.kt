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
    }

    private fun initPlayer() {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15000, 60000, 2000, 5000).build()
        player = ExoPlayer.Builder(this).setLoadControl(loadControl).build()
        player?.setVideoTextureView(binding.textureView)
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> { showLoading(false); retries = 0; urlIdx = 0; loadTimer?.cancel() }
                    Player.STATE_BUFFERING -> { showLoading(true); startLoadTimer() }
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
                    scope.launch { delay(backoff); loadChannel(idx) }
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

    private fun getUrls(ch: Channel): List<String> {
        return ch.streamUrl.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun loadChannel(i: Int) {
        if (channels.isEmpty()) return
        val ch = channels[i]; idx = i; retries = 0; urlIdx = 0
        showLoading(true); hideControls()
        updateZapOverlay(ch, i)
        val urls = getUrls(ch)
        if (urls.isNotEmpty()) playUrl(urls[0])
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
            url.contains(".mpd") ->
                DashMediaSource.Factory(DefaultDataSource.Factory(this)).createMediaSource(MediaItem.fromUri(url))
            url.startsWith("rtsp://") ->
                RtspMediaSource.Factory().createMediaSource(MediaItem.fromUri(url))
            else -> ProgressiveMediaSource.Factory(dsf).createMediaSource(MediaItem.fromUri(url))
        }
        player?.stop(); player?.setMediaSource(src); player?.prepare(); player?.play()
        if (ch.id.isNotEmpty()) {
            val savedPos = com.fluxtv.app.utils.Prefs.getProgress(this, ch.id)
            if (savedPos > 0) {
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
                if (retries < 3) { retries++; scope.launch { delay(500); loadChannel(idx) } }
                else showLoading(false)
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
            KeyEvent.KEYCODE_BACK -> { finish(); true }
            else -> super.onKeyDown(keyCode, event)
        }
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
        loadTimer?.cancel(); controlsTimer?.cancel(); scope.cancel(); player?.release()
    }
}
