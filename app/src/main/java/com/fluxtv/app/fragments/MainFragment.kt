package com.fluxtv.app.fragments

import android.content.Intent
import android.os.Bundle
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import androidx.leanback.widget.FocusHighlight
import com.bumptech.glide.Glide
import com.fluxtv.app.activities.PlayerActivity
import com.fluxtv.app.models.Channel
import com.fluxtv.app.activities.MainActivity
import com.fluxtv.app.services.ApiService
import kotlinx.coroutines.*

class MainFragment : BrowseSupportFragment() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var allChannels = listOf<Channel>()
    private val catOrder = listOf("MUNDIAL 2026","EVENTOS","ARGENTINA","ARGENTINA INTERIOR","ARGENTINA 2","DEPORTES","DEPORTES 2","NOTICIAS","NOTICIAS 2","MÚSICA","MÚSICA 2","RELIGIÓN","INFANTILES","INFANTILES 2","CANALES 24/7","CANALES 24/7 2","CINE","CINE 2","SERIES","SERIES 2","INTERNACIONAL","INTERNACIONAL 2","COLOMBIA","CHILE","MEXICO","BRASIL","URUGUAY","DOCUMENTALES","PLUTOTV")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "ZENITH TV"
        headersState = HEADERS_DISABLED
        brandColor = 0xFF030810.toInt()
        searchAffordanceColor = 0xFF00E5FF.toInt()
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
                keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_DOWN -> {
                    
                    true
                }
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
            allChannels = withContext(Dispatchers.IO) {
                try { ApiService.getChannels() } catch (e: Exception) { emptyList() }
            }
            buildRows(allChannels)
        }
    }

    private fun buildRows(channels: List<Channel>) {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter(FocusHighlight.ZOOM_FACTOR_SMALL).apply { shadowEnabled = false; selectEffectEnabled = false })
        val grouped = channels.filter { it.category != "ADULTOS" }.groupBy { it.category }
        val sorted = catOrder.mapNotNull { grouped[it]?.let { chs -> it to chs } } +
                     grouped.filter { it.key !in catOrder }.map { it.key to it.value }
        sorted.forEach { (cat, chs) ->
            val adapter = ArrayObjectAdapter(ChannelPresenter())
            chs.forEach { adapter.add(it) }
            rowsAdapter.add(ListRow(HeaderItem(cat), adapter))
        }
        adapter = rowsAdapter
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

class ChannelPresenter : Presenter() {
    private val catColors = mapOf(
        "MUNDIAL 2026" to 0xFF1A4A1A.toInt(),
        "EVENTOS" to 0xFF4A1A00.toInt(),
        "ARGENTINA" to 0xFF00204A.toInt(),
        "ARGENTINA INTERIOR" to 0xFF002040.toInt(),
        "ARGENTINA 2" to 0xFF00183A.toInt(),
        "DEPORTES" to 0xFF4A0000.toInt(),
        "DEPORTES 2" to 0xFF3A0000.toInt(),
        "NOTICIAS" to 0xFF1A1A4A.toInt(),
        "NOTICIAS 2" to 0xFF15154A.toInt(),
        "MÚSICA" to 0xFF3A004A.toInt(),
        "MÚSICA 2" to 0xFF30003A.toInt(),
        "INFANTILES" to 0xFF4A3A00.toInt(),
        "CINE" to 0xFF2A0040.toInt(),
        "CINE 2" to 0xFF200030.toInt(),
        "SERIES" to 0xFF004A3A.toInt(),
        "CANALES 24/7" to 0xFF004040.toInt(),
        "INTERNACIONAL" to 0xFF1A3A4A.toInt(),
    )

    override fun onCreateViewHolder(parent: android.view.ViewGroup): ViewHolder {
        val ctx = parent.context
        val dp = ctx.resources.displayMetrics.density
        val card = android.widget.FrameLayout(ctx).apply {
            layoutParams = android.view.ViewGroup.LayoutParams((150*dp).toInt(), (115*dp).toInt()).also {
                (it as? android.view.ViewGroup.MarginLayoutParams)?.setMargins((4*dp).toInt(),(4*dp).toInt(),(4*dp).toInt(),(4*dp).toInt())
            }
            isFocusable = true; isFocusableInTouchMode = true
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 14*dp
                setColor(0xFF060E1A.toInt())
            }
            tag = "card"
        }
        val logo = android.widget.ImageView(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams((100*dp).toInt(), (72*dp).toInt()).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.TOP
                topMargin = (8*dp).toInt()
            }
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            tag = "logo"
        }
        val nameOverlay = android.widget.LinearLayout(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, (34*dp).toInt()).apply {
                gravity = android.view.Gravity.BOTTOM
            }
            setBackgroundColor(0xCC000000.toInt())
            setPadding((8*dp).toInt(), 0, (8*dp).toInt(), 0)
            gravity = android.view.Gravity.CENTER_VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadii = floatArrayOf(0f,0f,0f,0f,14*dp,14*dp,14*dp,14*dp)
                setColor(0xCC000000.toInt())
            }
        }
        val name = android.widget.TextView(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            setTextColor(0xFFFFFFFF.toInt()); textSize = 10f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; tag = "name"
        }
        nameOverlay.addView(name); card.addView(logo); card.addView(nameOverlay)
        return ViewHolder(card)
    }

    override fun onBindViewHolder(vh: ViewHolder, item: Any) {
        val ch = item as Channel
        val card = vh.view as android.widget.FrameLayout
        card.findViewWithTag<android.widget.TextView>("name")?.text = ch.name
        // Color de fondo por categoría
        val bgColor = catColors[ch.category] ?: 0xFF060E1A.toInt()
        (card.background as? android.graphics.drawable.GradientDrawable)?.setColor(bgColor)
        val logo = card.findViewWithTag<android.widget.ImageView>("logo")
        if (ch.logoUrl.isNotEmpty()) Glide.with(card).load(ch.logoUrl).into(logo!!)
        else logo?.setImageDrawable(null)
        // Efecto foco
        card.setOnFocusChangeListener { v, focused ->
            (v.background as? android.graphics.drawable.GradientDrawable)?.setStroke(
                if (focused) 3 else 0,
                if (focused) 0xFF00E5FF.toInt() else 0x00000000
            )
            v.animate().scaleX(if (focused) 1.08f else 1f).scaleY(if (focused) 1.08f else 1f)
                .translationZ(if (focused) 8f else 0f).setDuration(120).start()
        }
    }

    override fun onUnbindViewHolder(vh: ViewHolder) {
        (vh.view as android.widget.FrameLayout).findViewWithTag<android.widget.ImageView>("logo")?.setImageDrawable(null)
    }
}
