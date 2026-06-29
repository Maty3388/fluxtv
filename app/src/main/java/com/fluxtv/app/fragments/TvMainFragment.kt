package com.fluxtv.app.fragments

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.fluxtv.app.R
import com.fluxtv.app.activities.PlayerActivity
import com.fluxtv.app.models.Channel
import com.fluxtv.app.services.ApiService
import kotlinx.coroutines.*

class TvMainFragment : Fragment() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var allChannels = listOf<Channel>()
    var onChannelsLoaded: (() -> Unit)? = null

    private lateinit var rvCategories: RecyclerView
    private lateinit var categoryAdapter: CategoryRowAdapter

    private val catOrder = listOf(
        "MUNDIAL 2026","EVENTOS","ARGENTINA","ARGENTINA INTERIOR","ARGENTINA 2",
        "DEPORTES","DEPORTES 2","NOTICIAS","NOTICIAS 2","MÚSICA",
        "INFANTILES","CINE","SERIES","INTERNACIONAL",
        "COLOMBIA","CHILE","MEXICO","BRASIL","URUGUAY","DOCUMENTALES"
    )

    private val catColors = mapOf(
        "MUNDIAL 2026"       to 0xFF00FF88.toInt(),
        "EVENTOS"            to 0xFFFF8C00.toInt(),
        "ARGENTINA"          to 0xFF00D4FF.toInt(),
        "ARGENTINA INTERIOR" to 0xFFA78BFA.toInt(),
        "ARGENTINA 2"        to 0xFF00D4FF.toInt(),
        "DEPORTES"           to 0xFFF5A623.toInt(),
        "DEPORTES 2"         to 0xFFF5A623.toInt(),
        "NOTICIAS"           to 0xFF22C55E.toInt(),
        "NOTICIAS 2"         to 0xFF22C55E.toInt(),
        "MÚSICA"             to 0xFFAB47BC.toInt(),
        "INFANTILES"         to 0xFFFFB74D.toInt(),
        "CINE"               to 0xFF7E57C2.toInt(),
        "SERIES"             to 0xFF26A69A.toInt(),
        "INTERNACIONAL"      to 0xFFF43F5E.toInt(),
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_tv_main, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvCategories = view.findViewById(R.id.rvCategories)
        categoryAdapter = CategoryRowAdapter(emptyList(), catColors) { ch, list ->
            startActivity(Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_CHANNELS, ArrayList(list))
                putExtra(PlayerActivity.EXTRA_INDEX, list.indexOf(ch).coerceAtLeast(0))
            })
        }
        rvCategories.layoutManager = LinearLayoutManager(requireContext())
        rvCategories.adapter = categoryAdapter
        rvCategories.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                val lm = rvCategories.layoutManager as LinearLayoutManager
                if (lm.findFirstCompletelyVisibleItemPosition() == 0) {
                    view.findViewById<FrameLayout>(R.id.heroBanner)?.requestFocus()
                    return@setOnKeyListener true
                }
            }
            false
        }
        setupHeroBanner(view)
        loadChannels()
    }

    private fun setupHeroBanner(root: View) {
        scope.launch {
            val channels = withContext(Dispatchers.IO) {
                try { ApiService.getChannels() } catch (_: Exception) { emptyList() }
            }
            val banner = root.findViewById<FrameLayout>(R.id.heroBanner) ?: return@launch
            val evento = channels.firstOrNull { it.category.contains("MUNDIAL", ignoreCase = true) }
                ?: channels.firstOrNull { it.category.contains("EVENTO", ignoreCase = true) }
            if (evento == null) { banner.visibility = View.GONE; return@launch }

            val ctx = requireContext()
            val dp = ctx.resources.displayMetrics.density
            banner.removeAllViews()
            banner.visibility = View.VISIBLE
            banner.isFocusable = true
            banner.isClickable = true
            banner.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xFF0A2010.toInt(), 0xFF060E09.toInt())
            ).apply { cornerRadius = 12 * dp; setStroke((1 * dp).toInt(), 0x2200FF88.toInt()) }

            val content = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((14*dp).toInt(),(10*dp).toInt(),(14*dp).toInt(),(10*dp).toInt())
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }

            val badge = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply { cornerRadius = 20*dp; setColor(0x1800FF88.toInt()); setStroke(1,0x3300FF88.toInt()) }
                setPadding((8*dp).toInt(),(3*dp).toInt(),(10*dp).toInt(),(3*dp).toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = (14*dp).toInt() }
            }
            val dot = View(ctx).apply {
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xFF00FF88.toInt()) }
                layoutParams = LinearLayout.LayoutParams((6*dp).toInt(),(6*dp).toInt()).apply { marginEnd = (5*dp).toInt() }
            }
            badge.addView(dot)
            badge.addView(TextView(ctx).apply { text = "EN VIVO"; textSize = 9f; setTextColor(0xFF00FF88.toInt()); typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.1f })
            content.addView(badge)

            val info = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            info.addView(TextView(ctx).apply { text = evento.name; textSize = 14f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; maxLines = 1; ellipsize = TextUtils.TruncateAt.END })
            info.addView(TextView(ctx).apply { text = "⚡ Evento especial en vivo"; textSize = 10f; setTextColor(0xFF6B7A6B.toInt()); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = (2*dp).toInt() } })
            content.addView(info)

            val mundialList = channels.filter { it.category.contains("MUNDIAL", ignoreCase = true) }.ifEmpty { channels.filter { it.category.contains("EVENTO", ignoreCase = true) } }
            val btn = TextView(ctx).apply {
                text = "▶  VER"; textSize = 11f; setTextColor(Color.BLACK); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
                background = GradientDrawable().apply { cornerRadius = 8*dp; setColor(0xFF00FF88.toInt()) }
                setPadding((14*dp).toInt(),(8*dp).toInt(),(14*dp).toInt(),(8*dp).toInt())
                isFocusable = true; isClickable = true
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = (12*dp).toInt() }
                setOnClickListener { startActivity(Intent(requireContext(), PlayerActivity::class.java).apply { putExtra(PlayerActivity.EXTRA_CHANNELS, ArrayList(mundialList)); putExtra(PlayerActivity.EXTRA_INDEX, 0) }) }
                setOnFocusChangeListener { _, focused -> background = GradientDrawable().apply { cornerRadius = 8*dp; setColor(if (focused) Color.WHITE else 0xFF00FF88.toInt()) } }
            }
            content.addView(btn)
            banner.addView(content)

            banner.setOnClickListener { startActivity(Intent(requireContext(), PlayerActivity::class.java).apply { putExtra(PlayerActivity.EXTRA_CHANNELS, ArrayList(mundialList)); putExtra(PlayerActivity.EXTRA_INDEX, 0) }) }
            banner.setOnFocusChangeListener { v, focused ->
                v.background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(if (focused) 0xFF0A3018.toInt() else 0xFF0A2010.toInt(), 0xFF060E09.toInt())).apply {
                    cornerRadius = 12 * resources.displayMetrics.density
                    setStroke((if (focused) 2 else 1) * resources.displayMetrics.density.toInt(), if (focused) 0xFF00FF88.toInt() else 0x2200FF88.toInt())
                }
            }
        }
    }

    fun loadChannels() {
        scope.launch {
            val cached = withContext(Dispatchers.IO) { try { ApiService.getCachedChannels() } catch (_: Exception) { emptyList() } }
            if (cached.isNotEmpty()) { allChannels = cached; buildRows(allChannels) }
            val fresh = withContext(Dispatchers.IO) { try { ApiService.getChannels() } catch (_: Exception) { emptyList() } }
            if (fresh.isNotEmpty()) { allChannels = fresh; buildRows(allChannels); onChannelsLoaded?.invoke() }
        }
    }

    fun filterCategory(category: String?) {
        val filtered = if (category == null) allChannels else allChannels.filter { it.category == category || it.category == "$category 2" }
        buildRows(filtered)
    }

    fun loadFavorites() {
        scope.launch {
            val favs = withContext(Dispatchers.IO) { try { ApiService.getFavorites() } catch (_: Exception) { emptyList() } }
            buildRows(if (favs.isEmpty()) allChannels else favs)
        }
    }

    fun getCategories(): List<String> =
        allChannels.filter { it.category != "ADULTOS" }.map { it.category }.distinct()
            .sortedBy { catOrder.indexOf(it).let { i -> if (i < 0) 999 else i } }

    private fun buildRows(channels: List<Channel>) {
        val isAdultFilter = channels.isNotEmpty() && channels.all { it.category == "ADULTOS" }
        val grouped = if (isAdultFilter) channels.groupBy { it.category } else channels.filter { it.category != "ADULTOS" }.groupBy { it.category }
        val sorted = (catOrder.mapNotNull { grouped[it]?.let { chs -> it to chs } } + grouped.filter { it.key !in catOrder }.map { it.key to it.value }).filter { !it.first.contains("MUNDIAL", ignoreCase = true) }
        categoryAdapter.updateData(sorted)
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

class CategoryRowAdapter(
    private var rows: List<Pair<String, List<Channel>>>,
    private val catColors: Map<String, Int>,
    private val onChannelClick: (Channel, List<Channel>) -> Unit
) : RecyclerView.Adapter<CategoryRowAdapter.RowVH>() {

    fun updateData(newRows: List<Pair<String, List<Channel>>>) { rows = newRows; notifyDataSetChanged() }
    override fun getItemCount() = rows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowVH {
        val ctx = parent.context
        val dp = ctx.resources.displayMetrics.density
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = (22*dp).toInt() }
        }
        return RowVH(root)
    }

    override fun onBindViewHolder(holder: RowVH, position: Int) {
        val (cat, channels) = rows[position]
        val ctx = holder.itemView.context
        val dp = ctx.resources.displayMetrics.density
        val color = catColors[cat] ?: 0xFF00D4FF.toInt()
        val root = holder.itemView as LinearLayout
        root.removeAllViews()

        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding((20*dp).toInt(),(6*dp).toInt(),(20*dp).toInt(),(10*dp).toInt())
        }
        header.addView(View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((3*dp).toInt(),(18*dp).toInt()).apply { marginEnd = (10*dp).toInt() }
            background = GradientDrawable().apply { cornerRadius = 2*dp; setColor(color) }
        })
        header.addView(TextView(ctx).apply { text = cat; textSize = 11f; setTextColor(color); typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.12f })
        header.addView(TextView(ctx).apply {
            text = "${channels.size}"; textSize = 10f; setTextColor(0xFF4A5568.toInt()); typeface = Typeface.MONOSPACE
            background = GradientDrawable().apply { cornerRadius = 10*dp; setColor(0xFF141929.toInt()); setStroke(1,0x0FFFFFFF.toInt()) }
            setPadding((8*dp).toInt(),(2*dp).toInt(),(8*dp).toInt(),(2*dp).toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = (8*dp).toInt() }
        })
        header.addView(View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0,(1*dp).toInt(),1f).apply { marginStart = (10*dp).toInt() }
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(color and 0x00FFFFFF or 0x22000000, Color.TRANSPARENT))
        })
        root.addView(header)

        root.addView(RecyclerView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
            clipToPadding = false
            setPadding((16*dp).toInt(), 0, (16*dp).toInt(), 0)
            overScrollMode = View.OVER_SCROLL_NEVER
            adapter = ChannelCardAdapter(channels, color, onChannelClick)
        })
    }

    class RowVH(view: View) : RecyclerView.ViewHolder(view)
}

class ChannelCardAdapter(
    private val channels: List<Channel>,
    private val catColor: Int,
    private val onClick: (Channel, List<Channel>) -> Unit
) : RecyclerView.Adapter<ChannelCardAdapter.CardVH>() {

    override fun getItemCount() = channels.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardVH {
        val ctx = parent.context
        val dp = ctx.resources.displayMetrics.density
        val cardW = (126*dp).toInt(); val cardH = (116*dp).toInt()
        val thumbH = (76*dp).toInt(); val infoH = (40*dp).toInt()

        val card = FrameLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(cardW, cardH).apply { marginEnd = (10*dp).toInt() }
            isFocusable = true; isFocusableInTouchMode = false
            background = GradientDrawable().apply { cornerRadius = 12*dp; setColor(0xFF111827.toInt()); setStroke(1, 0xFF0D1A2A.toInt()) }
            elevation = 4*dp; // clipToOutline = true
        }

        val thumb = FrameLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, thumbH)
            setBackgroundColor(0xFF0F1422.toInt()); tag = "thumb"
        }
        val logo = android.widget.ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams((72*dp).toInt(),(56*dp).toInt(), Gravity.CENTER)
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE; tag = "logo"
        }
        val overlay = View(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(0x00000000); tag = "overlay"
        }
        val playIcon = TextView(ctx).apply {
            text = "▶"; textSize = 18f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            alpha = 0f; tag = "play"
        }
        thumb.addView(logo); thumb.addView(overlay); thumb.addView(playIcon)

        val infoBar = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, infoH).apply { topMargin = thumbH }
            setBackgroundColor(0xFF111827.toInt())
            setPadding((8*dp).toInt(),(5*dp).toInt(),(8*dp).toInt(),(4*dp).toInt()); tag = "infobar"
        }
        infoBar.addView(TextView(ctx).apply { textSize = 9f; setTextColor(0xFF4A5568.toInt()); typeface = Typeface.MONOSPACE; tag = "num" })
        infoBar.addView(TextView(ctx).apply { textSize = 10f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; maxLines = 1; ellipsize = TextUtils.TruncateAt.END; tag = "name" })

        val liveRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = (2*dp).toInt() }
        }
        liveRow.addView(View(ctx).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xFF22C55E.toInt()) }
            layoutParams = LinearLayout.LayoutParams((5*dp).toInt(),(5*dp).toInt()).apply { marginEnd = (4*dp).toInt() }
        })
        liveRow.addView(TextView(ctx).apply { text = "LIVE"; textSize = 8f; setTextColor(0xFF22C55E.toInt()); typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.05f })
        infoBar.addView(liveRow)

        card.addView(thumb); card.addView(infoBar)
        return CardVH(card)
    }

    override fun onBindViewHolder(holder: CardVH, position: Int) {
        val ch = channels[position]
        val ctx = holder.card.context
        val dp = ctx.resources.displayMetrics.density
        val card = holder.card

        card.findViewWithTag<TextView>("num")?.text = "CH ${ch.number}"
        card.findViewWithTag<TextView>("name")?.text = ch.name

        val logo = card.findViewWithTag<android.widget.ImageView>("logo")
        if (logo != null && !ch.logoUrl.isNullOrBlank()) {
            Glide.with(ctx).load(ch.logoUrl).into(logo)
        } else { logo?.setImageDrawable(null) }

        card.setOnClickListener { onClick(ch, channels) }
        card.setOnFocusChangeListener { _, focused ->
            card.background = GradientDrawable().apply {
                cornerRadius = 12*dp
                setColor(0xFF111827.toInt())
                setStroke((if (focused) 3 else 1)*dp.toInt(), if (focused) 0xFF00D4FF.toInt() else 0xFF0D1A2A.toInt())
            }
            card.elevation = if (focused) 16*dp else 4*dp
            card.findViewWithTag<View>("overlay")?.setBackgroundColor(0x00000000)
            card.findViewWithTag<TextView>("play")?.alpha = 0f
            card.findViewWithTag<TextView>("num")?.setTextColor(0xFF4A5568.toInt())
            card.findViewWithTag<TextView>("name")?.setTextColor(android.graphics.Color.WHITE)
            card.findViewWithTag<LinearLayout>("infobar")?.setBackgroundColor(0xFF111827.toInt())
            val scale = if (focused) 1.06f else 1f
            card.animate().scaleX(scale).scaleY(scale).setDuration(150).start()
        }
    }

    class CardVH(val card: FrameLayout) : RecyclerView.ViewHolder(card)
}
