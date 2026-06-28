package com.fluxtv.app.fragments

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fluxtv.app.R
import com.fluxtv.app.activities.PlayerActivity
import com.fluxtv.app.models.Channel
import com.fluxtv.app.services.ApiService
import com.fluxtv.app.utils.Prefs
import kotlinx.coroutines.*

class TvMainFragment : Fragment() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var allChannels = listOf<Channel>()
    var onChannelsLoaded: (() -> Unit)? = null
    private lateinit var rvCategories: RecyclerView
    private lateinit var categoryAdapter: CategoryRowAdapter

    private val catOrder = listOf("MUNDIAL 2026","EVENTOS","ARGENTINA","ARGENTINA INTERIOR","ARGENTINA 2","DEPORTES","DEPORTES 2","NOTICIAS","NOTICIAS 2","MÚSICA","INFANTILES","CINE","SERIES","INTERNACIONAL","COLOMBIA","CHILE","MEXICO","BRASIL","URUGUAY","DOCUMENTALES")

    private val catColors = mapOf(
        "MUNDIAL 2026" to 0xFF00FF88.toInt(),
        "EVENTOS" to 0xFFFF8C00.toInt(),
        "ARGENTINA" to 0xFF4FC3F7.toInt(),
        "ARGENTINA INTERIOR" to 0xFF4FC3F7.toInt(),
        "ARGENTINA 2" to 0xFF4FC3F7.toInt(),
        "DEPORTES" to 0xFFFF6B6B.toInt(),
        "DEPORTES 2" to 0xFFFF6B6B.toInt(),
        "NOTICIAS" to 0xFFCE93D8.toInt(),
        "NOTICIAS 2" to 0xFFCE93D8.toInt(),
        "MÚSICA" to 0xFFAB47BC.toInt(),
        "INFANTILES" to 0xFFFFB74D.toInt(),
        "CINE" to 0xFF7E57C2.toInt(),
        "SERIES" to 0xFF26A69A.toInt(),
        "INTERNACIONAL" to 0xFF42A5F5.toInt(),
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_tv_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeroBanner(view)
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
            if (event.action == android.view.KeyEvent.ACTION_DOWN && keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) {
                val lm = rvCategories.layoutManager as LinearLayoutManager
                if (lm.findFirstCompletelyVisibleItemPosition() == 0) {
                    view.findViewById<android.widget.FrameLayout>(R.id.heroBanner)?.requestFocus()
                    return@setOnKeyListener true
                }
            }
            false
        }

        loadChannels()
    }

    private fun setupHeroBanner(view: android.view.View) {
        scope.launch {
            val channels = withContext(Dispatchers.IO) {
                try { ApiService.getChannels() } catch (_: Exception) { emptyList() }
            }
            val banner2 = view.findViewById<android.widget.FrameLayout>(R.id.heroBanner)
            val evento = channels.firstOrNull { it.category.contains("MUNDIAL", ignoreCase = true) }
            if (evento == null) {
                banner2?.visibility = android.view.View.GONE
                return@launch
            }
            val banner = view.findViewById<android.widget.FrameLayout>(R.id.heroBanner) ?: return@launch
            val ctx = requireContext()
            val dp = ctx.resources.displayMetrics.density

            banner.removeAllViews()
            banner.visibility = android.view.View.VISIBLE
            banner.isFocusable = true
            banner.isFocusableInTouchMode = false
            banner.isClickable = true
            banner.background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xFF0a2010.toInt(), 0xFF051508.toInt())
            ).apply { cornerRadius = 14 * dp }

            // Glow border
            val border = android.view.View(ctx).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 14 * dp
                    setColor(android.graphics.Color.TRANSPARENT)
                    setStroke((1 * dp).toInt(), 0x2200FF88.toInt())
                }
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT)
            }

            val content = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding((16*dp).toInt(), (12*dp).toInt(), (16*dp).toInt(), (12*dp).toInt())
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT)
            }

            // Live badge
            val badgeWrap = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT).apply {
                    marginEnd = (16*dp).toInt()
                }
            }
            val liveBadge = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 20*dp; setColor(0x1800FF88.toInt())
                    setStroke(1, 0x3500FF88.toInt())
                }
                setPadding((8*dp).toInt(), (3*dp).toInt(), (10*dp).toInt(), (3*dp).toInt())
            }
            val dot = android.view.View(ctx).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(0xFF00FF88.toInt())
                }
                layoutParams = android.widget.LinearLayout.LayoutParams((6*dp).toInt(), (6*dp).toInt()).apply { marginEnd = (5*dp).toInt() }
            }
            val liveText = android.widget.TextView(ctx).apply {
                text = "EN VIVO"; textSize = 9f
                setTextColor(0xFF00FF88.toInt())
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                letterSpacing = 0.1f
            }
            liveBadge.addView(dot); liveBadge.addView(liveText)
            badgeWrap.addView(liveBadge)
            content.addView(badgeWrap)

            // Info
            val info = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            info.addView(android.widget.TextView(ctx).apply {
                text = evento.name
                textSize = 15f; setTextColor(android.graphics.Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            })
            info.addView(android.widget.TextView(ctx).apply {
                text = "⚡ Evento en vivo • Toca para ver"
                textSize = 11f; setTextColor(0xFF88AA88.toInt())
            })
            content.addView(info)

            // Botón ver
            val btn = android.widget.TextView(ctx).apply {
                text = "▶ VER"
                textSize = 11f; setTextColor(android.graphics.Color.BLACK)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = android.view.Gravity.CENTER
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 8*dp; setColor(0xFF00FF88.toInt())
                }
                setPadding((14*dp).toInt(), (8*dp).toInt(), (14*dp).toInt(), (8*dp).toInt())
                isFocusable = true; isClickable = true
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginStart = (12*dp).toInt()
                }
                setOnClickListener {
                    val list = channels.filter { it.category.contains("MUNDIAL", ignoreCase = true) }.ifEmpty { channels.filter { it.category.contains("Evento", ignoreCase = true) || it.category.contains("PPV", ignoreCase = true) } }
                    startActivity(android.content.Intent(requireContext(), com.fluxtv.app.activities.PlayerActivity::class.java).apply {
                        putExtra(com.fluxtv.app.activities.PlayerActivity.EXTRA_CHANNELS, ArrayList(list))
                        putExtra(com.fluxtv.app.activities.PlayerActivity.EXTRA_INDEX, 0)
                    })
                }
                setOnFocusChangeListener { _, focused ->
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 8*dp
                        setColor(if (focused) android.graphics.Color.WHITE else 0xFF00FF88.toInt())
                    }
                    setTextColor(android.graphics.Color.BLACK)
                }
            }
            content.addView(btn)

            banner.addView(border)
            banner.addView(content)
            
            val mundialList = channels.filter { it.category.contains("MUNDIAL", ignoreCase = true) }
            banner.setOnClickListener {
                startActivity(android.content.Intent(requireContext(), com.fluxtv.app.activities.PlayerActivity::class.java).apply {
                    putExtra(com.fluxtv.app.activities.PlayerActivity.EXTRA_CHANNELS, ArrayList(mundialList))
                    putExtra(com.fluxtv.app.activities.PlayerActivity.EXTRA_INDEX, 0)
                })
            }
            banner.setOnFocusChangeListener { v, focused ->
                v.background = android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(if (focused) 0xFF0a3a10.toInt() else 0xFF0a2010.toInt(), 0xFF051508.toInt())
                ).apply { 
                    cornerRadius = 14 * resources.displayMetrics.density
                    setStroke((if (focused) 2 else 1) * resources.displayMetrics.density.toInt(), 
                        if (focused) 0xFF00FF88.toInt() else 0x2200FF88.toInt())
                }
            }
        }
    }

    fun loadChannels() {
        scope.launch {
            val cached = withContext(Dispatchers.IO) {
                try { ApiService.getCachedChannels() } catch (_: Exception) { emptyList() }
            }
            if (cached.isNotEmpty()) {
                allChannels = cached
                buildRows(allChannels)
            }
            val fresh = withContext(Dispatchers.IO) {
                try { ApiService.getChannels() } catch (_: Exception) { emptyList() }
            }
            if (fresh.isNotEmpty()) {
                allChannels = fresh
                buildRows(allChannels)
                onChannelsLoaded?.invoke()
            }
        }
    }

    fun filterCategory(category: String?) {
        val filtered = if (category == null) allChannels
                       else allChannels.filter { it.category == category || it.category == "$category 2" }
        buildRows(filtered)
    }

    fun loadFavorites() {
        scope.launch {
            val favs = withContext(Dispatchers.IO) {
                try { ApiService.getFavorites() } catch (_: Exception) { emptyList() }
            }
            buildRows(if (favs.isEmpty()) allChannels else favs)
        }
    }

    fun getCategories(): List<String> {
        return allChannels.filter { it.category != "ADULTOS" }
            .map { it.category }.distinct()
            .sortedBy { catOrder.indexOf(it).let { i -> if (i < 0) 999 else i } }
    }

    private fun buildRows(channels: List<Channel>) {
        val isAdultFilter = channels.isNotEmpty() && channels.all { it.category == "ADULTOS" }
        val grouped = if (isAdultFilter) channels.groupBy { it.category }
                      else channels.filter { it.category != "ADULTOS" }.groupBy { it.category }
        val sorted = (catOrder.mapNotNull { grouped[it]?.let { chs -> it to chs } } +
                     grouped.filter { it.key !in catOrder }.map { it.key to it.value })
                     .filter { !it.first.contains("MUNDIAL", ignoreCase = true) }
        categoryAdapter.updateData(sorted)
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

// Adapter para las filas de categorías
class CategoryRowAdapter(
    private var rows: List<Pair<String, List<Channel>>>,
    private val catColors: Map<String, Int>,
    private val onChannelClick: (Channel, List<Channel>) -> Unit
) : RecyclerView.Adapter<CategoryRowAdapter.RowVH>() {

    fun updateData(newRows: List<Pair<String, List<Channel>>>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun getItemCount() = rows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowVH {
        val ctx = parent.context
        val dp = ctx.resources.displayMetrics.density
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (20 * dp).toInt()
            }
        }
        return RowVH(root)
    }

    override fun onBindViewHolder(holder: RowVH, position: Int) {
        val (cat, channels) = rows[position]
        val ctx = holder.itemView.context
        val dp = ctx.resources.displayMetrics.density
        val color = catColors[cat] ?: 0xFF00E5FF.toInt()
        val root = holder.itemView as LinearLayout
        root.removeAllViews()

        // Category header
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding((20*dp).toInt(), (8*dp).toInt(), (20*dp).toInt(), (10*dp).toInt())
        }

        // Color bar
        val bar = android.view.View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((3*dp).toInt(), (16*dp).toInt()).apply {
                marginEnd = (10*dp).toInt()
            }
            background = GradientDrawable().apply {
                cornerRadius = 2*dp
                setColor(color)
            }
        }

        val catTitle = TextView(ctx).apply {
            text = cat
            textSize = 12f
            setTextColor(color)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.15f
        }

        // Separator line
        val sep = android.view.View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, (1*dp).toInt(), 1f).apply {
                marginStart = (10*dp).toInt()
                marginEnd = (10*dp).toInt()
            }
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(color and 0x33FFFFFF or 0x33000000, Color.TRANSPARENT))
        }

        header.addView(bar)
        header.addView(catTitle)
        header.addView(sep)
        root.addView(header)

        // Horizontal RecyclerView de canales
        val rv = RecyclerView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
            clipToPadding = false
            setPadding((16*dp).toInt(), 0, (16*dp).toInt(), 0)
            overScrollMode = android.view.View.OVER_SCROLL_NEVER
            adapter = ChannelCardAdapter(channels, color, onChannelClick)
        }
        root.addView(rv)
    }

    class RowVH(view: View) : RecyclerView.ViewHolder(view)
}

// Adapter para las cards de canales
class ChannelCardAdapter(
    private val channels: List<Channel>,
    private val catColor: Int,
    private val onClick: (Channel, List<Channel>) -> Unit
) : RecyclerView.Adapter<ChannelCardAdapter.CardVH>() {

    override fun getItemCount() = channels.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardVH {
        val ctx = parent.context
        val dp = ctx.resources.displayMetrics.density
        val W = (110*dp).toInt()
        val H = (110*dp).toInt()

        val card = android.widget.FrameLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(W, H).apply {
                marginEnd = (8*dp).toInt()
            }
            isFocusable = true; isFocusableInTouchMode = false
            background = GradientDrawable().apply {
                cornerRadius = 12*dp
                setColor(0x220A1020)
                setStroke(1, 0xFF0D1A2A.toInt())
            }
            elevation = 4*dp
        }

        // Logo
        val logo = android.widget.ImageView(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                (70*dp).toInt(), (60*dp).toInt()).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.TOP
                topMargin = (8*dp).toInt()
            }
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            tag = "logo"
        }

        // Shimmer placeholder
        val shimmer = com.facebook.shimmer.ShimmerFrameLayout(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                (70*dp).toInt(), (60*dp).toInt()).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.TOP
                topMargin = (8*dp).toInt()
            }
            tag = "shimmer"
            val shimmerBg = android.widget.FrameLayout(ctx).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                background = GradientDrawable().apply {
                    cornerRadius = 8*dp
                    setColor(0x33FFFFFF)
                }
            }
            addView(shimmerBg)
            startShimmer()
        }

        // Name bar
        val nameBar = android.widget.FrameLayout(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (28*dp).toInt()).apply {
                gravity = android.view.Gravity.BOTTOM
            }
            background = GradientDrawable().apply {
                cornerRadii = floatArrayOf(0f,0f,0f,0f,12*dp,12*dp,12*dp,12*dp)
                setColor(0xCC000000.toInt())
            }
            tag = "namebar"
        }

        val name = TextView(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT).apply {
                marginStart = (6*dp).toInt()
                marginEnd = (16*dp).toInt()
            }
            setTextColor(Color.WHITE); textSize = 8f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = android.view.Gravity.CENTER_VERTICAL
            tag = "name"
        }

        // Live dot
        val liveDot = android.view.View(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                (5*dp).toInt(), (5*dp).toInt()).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
                marginEnd = (5*dp).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFFFF4444.toInt())
            }
            tag = "live"
        }

        // Number
        val num = TextView(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                topMargin = (5*dp).toInt()
                marginStart = (5*dp).toInt()
            }
            setTextColor(0xFF2A4A6A.toInt()); textSize = 7f
            typeface = android.graphics.Typeface.MONOSPACE
            background = GradientDrawable().apply {
                cornerRadius = 3*dp; setColor(0x66000000.toInt())
            }
            setPadding((3*dp).toInt(), (1*dp).toInt(), (3*dp).toInt(), (1*dp).toInt())
            tag = "num"
        }

        nameBar.addView(name); nameBar.addView(liveDot)
        card.addView(shimmer); card.addView(logo); card.addView(num); card.addView(nameBar)
        return CardVH(card)
    }

    override fun onBindViewHolder(holder: CardVH, position: Int) {
        val ch = channels[position]
        val card = holder.itemView as android.widget.FrameLayout
        val ctx = card.context
        val dp = ctx.resources.displayMetrics.density

        card.findViewWithTag<TextView>("name")?.text = ch.name
        card.findViewWithTag<TextView>("num")?.text = String.format("%02d", ch.number)

        // Background con color de categoría oscuro
        card.background = GradientDrawable().apply {
            cornerRadius = 12*dp
            setColor(0x220A1020)
            setStroke(1, androidx.core.graphics.ColorUtils.setAlphaComponent(catColor, 40))
        }

        val logo = card.findViewWithTag<android.widget.ImageView>("logo")
        val shimmerView = card.findViewWithTag<com.facebook.shimmer.ShimmerFrameLayout>("shimmer")
        if (ch.logoUrl.isNotEmpty()) {
            shimmerView?.visibility = android.view.View.VISIBLE
            shimmerView?.startShimmer()
            com.bumptech.glide.Glide.with(card).load(ch.logoUrl)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(e: com.bumptech.glide.load.engine.GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>, isFirstResource: Boolean): Boolean {
                        shimmerView?.stopShimmer(); shimmerView?.visibility = android.view.View.GONE; return false
                    }
                    override fun onResourceReady(resource: android.graphics.drawable.Drawable, model: Any, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?, dataSource: com.bumptech.glide.load.DataSource, isFirstResource: Boolean): Boolean {
                        shimmerView?.stopShimmer(); shimmerView?.visibility = android.view.View.GONE; return false
                    }
                }).into(logo!!)
        } else {
            logo?.setImageDrawable(null)
            shimmerView?.stopShimmer(); shimmerView?.visibility = android.view.View.GONE
        }

        card.setOnClickListener { onClick(ch, channels) }

        card.setOnFocusChangeListener { v, focused ->
            v.background = GradientDrawable().apply {
                cornerRadius = 12*dp
                setColor(if (focused) 0xFF0A1020.toInt() else 0x220A1020)
                setStroke(if (focused) (2*dp).toInt() else 1,
                    if (focused) 0xFFFFFFFF.toInt() else androidx.core.graphics.ColorUtils.setAlphaComponent(catColor, 40))
            }
            v.animate().scaleX(if (focused) 1.03f else 1f).scaleY(if (focused) 1.03f else 1f)
                .translationZ(if (focused) 4f else 0f).setDuration(100).start()
        }
    }

    override fun onViewRecycled(holder: CardVH) {
        val card = holder.itemView as android.widget.FrameLayout
        card.findViewWithTag<android.widget.ImageView>("logo")?.setImageDrawable(null)
    }

    class CardVH(view: View) : RecyclerView.ViewHolder(view)
}

