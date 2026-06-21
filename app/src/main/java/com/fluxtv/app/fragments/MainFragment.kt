package com.fluxtv.app.fragments

import android.content.Intent
import android.os.Bundle
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.*
import androidx.leanback.widget.FocusHighlight
import com.bumptech.glide.Glide
import com.fluxtv.app.activities.PlayerActivity
import com.fluxtv.app.models.Channel
import com.fluxtv.app.activities.MainActivity
import com.fluxtv.app.services.ApiService
import kotlinx.coroutines.*

class MainFragment : RowsSupportFragment() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var allChannels = listOf<Channel>()
    var onChannelsLoaded: (() -> Unit)? = null
    private val catOrder = listOf("MUNDIAL 2026","EVENTOS","ARGENTINA","ARGENTINA INTERIOR","ARGENTINA 2","DEPORTES","DEPORTES 2","NOTICIAS","NOTICIAS 2","MÚSICA","MÚSICA 2","RELIGIÓN","INFANTILES","INFANTILES 2","CANALES 24/7","CANALES 24/7 2","CINE","CINE 2","SERIES","SERIES 2","INTERNACIONAL","INTERNACIONAL 2","COLOMBIA","CHILE","MEXICO","BRASIL","URUGUAY","DOCUMENTALES","PLUTOTV")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, row ->
            if (item is Channel) {
                val rowAdapter = (row as ListRow).adapter as ArrayObjectAdapter
                val list = (0 until rowAdapter.size()).map { rowAdapter.get(it) as Channel }
                val idx = list.indexOf(item).coerceAtLeast(0)
                startActivity(Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_CHANNELS, ArrayList(list))
                    putExtra(PlayerActivity.EXTRA_INDEX, idx)
                })
            }
        }
        loadChannels()
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setOnKeyListener { _, keyCode, event ->
            when {
                keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT && event.action == android.view.KeyEvent.ACTION_DOWN -> true
                keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_DOWN -> true
                else -> false
            }
        }
    }

    fun loadFavorites() {
        scope.launch {
            val favs = withContext(Dispatchers.IO) {
                try { com.fluxtv.app.services.ApiService.getFavorites() } catch (_: Exception) { emptyList() }
            }
            if (favs.isEmpty()) {
                val rowsAdapter = ArrayObjectAdapter(ListRowPresenter(FocusHighlight.ZOOM_FACTOR_SMALL).apply { shadowEnabled = false; selectEffectEnabled = false })
                val adapter = ArrayObjectAdapter(ChannelPresenter())
                rowsAdapter.add(ListRow(HeaderItem("Sin favoritos"), adapter))
                this@MainFragment.adapter = rowsAdapter
            } else {
                buildRows(favs)
            }
        }
    }

    fun filterCategory(category: String?) {
        val filtered = if (category == null) allChannels
                       else allChannels.filter { it.category == category || it.category == "$category 2" }
        buildRows(filtered)
    }

    fun filterCategories(cats: List<String>) {
        buildRows(allChannels.filter { it.category in cats })
    }

    fun loadChannels() {
        scope.launch {
            // Mostrar cache inmediatamente si existe
            val cached = withContext(Dispatchers.IO) {
                try { ApiService.getCachedChannels() } catch (_: Exception) { emptyList() }
            }
            if (cached.isNotEmpty()) {
                allChannels = cached
                val featuredCached = allChannels.filter { it.category == "MUNDIAL 2026" || it.category == "EVENTOS" }
                buildRows(allChannels, featuredCached)
            }
            // Refrescar desde la API en segundo plano
            val fresh = withContext(Dispatchers.IO) {
                try { ApiService.getChannels() } catch (_: Exception) { emptyList() }
            }
            if (fresh.isNotEmpty()) {
                allChannels = fresh
                val featured = allChannels.filter { it.category == "MUNDIAL 2026" || it.category == "EVENTOS" }
                buildRows(allChannels, featured)
                onChannelsLoaded?.invoke()
            } else if (allChannels.isNotEmpty()) {
                onChannelsLoaded?.invoke()
            }
        }
    }

    private fun buildRows(channels: List<Channel>, featured: List<Channel> = emptyList()) {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter(FocusHighlight.ZOOM_FACTOR_SMALL).apply { shadowEnabled = false; selectEffectEnabled = false })
        // Carrusel de destacados
        if (featured.isNotEmpty()) {
            val featAdapter = ArrayObjectAdapter(ChannelPresenter())
            featured.forEach { featAdapter.add(it) }
            rowsAdapter.add(ListRow(HeaderItem("⭐ DESTACADOS"), featAdapter))
        }
        val grouped = channels.filter { it.category != "ADULTOS" }.groupBy { it.category }
        val sorted = catOrder.mapNotNull { grouped[it]?.let { chs -> it to chs } } +
                     grouped.filter { it.key !in catOrder }.map { it.key to it.value }
        sorted.forEach { (cat, chs) ->
            val adapter = ArrayObjectAdapter(ChannelPresenter())
            chs.sortedBy { it.number }.forEach { adapter.add(it) }
            rowsAdapter.add(ListRow(HeaderItem(cat), adapter))
        }
        adapter = rowsAdapter
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

class ChannelPresenter : Presenter() {
    private val catGradients = mapOf(
        "MUNDIAL 2026"        to Pair(0xFF1B6B1B.toInt(), 0xFF0A3A0A.toInt()),
        "EVENTOS"             to Pair(0xFFB8620A.toInt(), 0xFF6B3500.toInt()),
        "ARGENTINA"           to Pair(0xFF1565C0.toInt(), 0xFF0A3A7A.toInt()),
        "ARGENTINA INTERIOR"  to Pair(0xFF1255A0.toInt(), 0xFF082E68.toInt()),
        "ARGENTINA 2"         to Pair(0xFF0F4590.toInt(), 0xFF062460.toInt()),
        "DEPORTES"            to Pair(0xFFB71C1C.toInt(), 0xFF6A0000.toInt()),
        "DEPORTES 2"          to Pair(0xFF9A1515.toInt(), 0xFF560000.toInt()),
        "NOTICIAS"            to Pair(0xFF283593.toInt(), 0xFF0D1660.toInt()),
        "NOTICIAS 2"          to Pair(0xFF1E2880.toInt(), 0xFF080E50.toInt()),
        "MÚSICA"              to Pair(0xFF6A1B9A.toInt(), 0xFF380060.toInt()),
        "MÚSICA 2"            to Pair(0xFF581580.toInt(), 0xFF2A0050.toInt()),
        "INFANTILES"          to Pair(0xFFE65100.toInt(), 0xFF8B3000.toInt()),
        "INFANTILES 2"        to Pair(0xFFD04800.toInt(), 0xFF7A2500.toInt()),
        "CINE"                to Pair(0xFF4A148C.toInt(), 0xFF1A0050.toInt()),
        "CINE 2"              to Pair(0xFF3A0F7A.toInt(), 0xFF120040.toInt()),
        "SERIES"              to Pair(0xFF00695C.toInt(), 0xFF003830.toInt()),
        "SERIES 2"            to Pair(0xFF005548.toInt(), 0xFF002820.toInt()),
        "CANALES 24/7"        to Pair(0xFF00838F.toInt(), 0xFF004550.toInt()),
        "CANALES 24/7 2"      to Pair(0xFF006878.toInt(), 0xFF003040.toInt()),
        "INTERNACIONAL"       to Pair(0xFF1565A0.toInt(), 0xFF083060.toInt()),
        "INTERNACIONAL 2"     to Pair(0xFF104A80.toInt(), 0xFF062040.toInt()),
        "COLOMBIA"            to Pair(0xFFB8860A.toInt(), 0xFF6B4A00.toInt()),
        "CHILE"               to Pair(0xFF8B0000.toInt(), 0xFF4A0000.toInt()),
        "MEXICO"              to Pair(0xFF1B6B1B.toInt(), 0xFF0A3500.toInt()),
        "BRASIL"              to Pair(0xFF2E7D32.toInt(), 0xFF0A4A0A.toInt()),
        "URUGUAY"             to Pair(0xFF1A5FA0.toInt(), 0xFF082E68.toInt()),
        "DOCUMENTALES"        to Pair(0xFF5D4037.toInt(), 0xFF2E1A0A.toInt()),
        "PLUTOTV"             to Pair(0xFF37474F.toInt(), 0xFF1A2530.toInt()),
    )

    override fun onCreateViewHolder(parent: android.view.ViewGroup): ViewHolder {
        val ctx = parent.context
        val dp = ctx.resources.displayMetrics.density
        val W = (160*dp).toInt()
        val H = (130*dp).toInt()

        val card = android.widget.FrameLayout(ctx).apply {
            layoutParams = android.view.ViewGroup.MarginLayoutParams(W, H).apply {
                setMargins((5*dp).toInt(), (5*dp).toInt(), (5*dp).toInt(), (5*dp).toInt())
            }
            isFocusable = true; isFocusableInTouchMode = true
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 12*dp
                setColor(0xFF0D1B2A.toInt())
            }
            elevation = 4*dp
        }

        val logo = android.widget.ImageView(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                (130*dp).toInt(), (85*dp).toInt()).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.TOP
                topMargin = (6*dp).toInt()
            }
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            tag = "logo"
        }

        val nameBar = android.widget.FrameLayout(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, (36*dp).toInt()).apply {
                gravity = android.view.Gravity.BOTTOM
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadii = floatArrayOf(0f,0f,0f,0f,12*dp,12*dp,12*dp,12*dp)
                setColor(0xCC000000.toInt())
            }
            tag = "namebar"
        }

        val name = android.widget.TextView(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT).apply {
                marginStart = (8*dp).toInt()
                marginEnd = (40*dp).toInt()
            }
            setTextColor(0xFFFFFFFF.toInt()); textSize = 10f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = android.view.Gravity.CENTER_VERTICAL
            tag = "name"
        }

        val liveBadge = android.widget.TextView(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
                marginEnd = (4*dp).toInt()
            }
            text = "● EN VIVO"
            textSize = 7f
            setTextColor(0xFFFF4444.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            tag = "live"
        }

        nameBar.addView(name)
        nameBar.addView(liveBadge)
        card.addView(logo)
        card.addView(nameBar)
        return ViewHolder(card)
    }

    override fun onBindViewHolder(vh: ViewHolder, item: Any) {
        val ch = item as Channel
        val card = vh.view as android.widget.FrameLayout
        val ctx = card.context
        val dp = ctx.resources.displayMetrics.density

        card.findViewWithTag<android.widget.TextView>("name")?.text = ch.name

        val (colorTop, colorBot) = catGradients[ch.category] ?: Pair(0xFF0D1B2A.toInt(), 0xFF060E1A.toInt())
        card.background = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(colorTop, colorBot)
        ).apply { cornerRadius = 12*dp }

        val logo = card.findViewWithTag<android.widget.ImageView>("logo")
        if (ch.logoUrl.isNotEmpty()) {
            com.bumptech.glide.Glide.with(card).load(ch.logoUrl).into(logo!!)
        } else {
            logo?.setImageDrawable(null)
        }

        val liveBadge = card.findViewWithTag<android.widget.TextView>("live")
        liveBadge?.clearAnimation()
        val anim = android.view.animation.AlphaAnimation(1f, 0.2f).apply {
            duration = 800; repeatMode = android.view.animation.Animation.REVERSE
            repeatCount = android.view.animation.Animation.INFINITE
        }
        liveBadge?.startAnimation(anim)

        card.setOnFocusChangeListener { v, focused ->
            val (ct, cb) = catGradients[ch.category] ?: Pair(0xFF0D1B2A.toInt(), 0xFF060E1A.toInt())
            v.background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(ct, cb)
            ).apply {
                cornerRadius = 12*dp
                if (focused) setStroke((3*dp).toInt(), 0xFFFFA500.toInt())
            }
            v.animate().scaleX(if (focused) 1.08f else 1f).scaleY(if (focused) 1.08f else 1f)
                .translationZ(if (focused) 10f else 0f).setDuration(120).start()
        }
    }

    override fun onUnbindViewHolder(vh: ViewHolder) {
        val card = vh.view as android.widget.FrameLayout
        card.findViewWithTag<android.widget.ImageView>("logo")?.setImageDrawable(null)
        card.findViewWithTag<android.widget.TextView>("live")?.clearAnimation()
    }
}
