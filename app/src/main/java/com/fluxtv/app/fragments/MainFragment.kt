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
import com.fluxtv.app.R
import com.fluxtv.app.activities.PlayerActivity
import com.fluxtv.app.models.Channel
import com.fluxtv.app.services.ApiService
import kotlinx.coroutines.*

// ─────────────────────────────────────────────────────────────────────────────
//  MainFragment (Mobile) — RecyclerView vertical de filas horizontales.
//  Reemplaza el viejo RowsSupportFragment (Leanback) que capturaba el scroll
//  táctil dentro de cada fila e impedía bajar de categoría con un solo swipe.
// ─────────────────────────────────────────────────────────────────────────────
class MainFragment : Fragment() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var allChannels = listOf<Channel>()
    var onChannelsLoaded: (() -> Unit)? = null

    private lateinit var rvCategories: RecyclerView
    private lateinit var categoryAdapter: MobileCategoryRowAdapter

    private val catOrder = listOf(
        "MUNDIAL 2026","EVENTOS","ARGENTINA","ARGENTINA INTERIOR","ARGENTINA 2",
        "DEPORTES","DEPORTES 2","NOTICIAS","NOTICIAS 2","MÚSICA","MÚSICA 2",
        "RELIGIÓN","INFANTILES","INFANTILES 2","CANALES 24/7","CANALES 24/7 2",
        "CINE","CINE 2","SERIES","SERIES 2","INTERNACIONAL","INTERNACIONAL 2",
        "COLOMBIA","CHILE","MEXICO","BRASIL","URUGUAY","DOCUMENTALES","PLUTOTV"
    )

    private val catColors = mapOf(
        "MUNDIAL 2026"  to 0xFF00FF88.toInt(), "EVENTOS" to 0xFFFF8C00.toInt(),
        "ARGENTINA"     to 0xFF00D4FF.toInt(), "ARGENTINA INTERIOR" to 0xFFA78BFA.toInt(),
        "ARGENTINA 2"   to 0xFF00D4FF.toInt(), "DEPORTES" to 0xFFF5A623.toInt(),
        "DEPORTES 2"    to 0xFFF5A623.toInt(), "NOTICIAS" to 0xFF22C55E.toInt(),
        "NOTICIAS 2"    to 0xFF22C55E.toInt(), "MÚSICA" to 0xFFAB47BC.toInt(),
        "INFANTILES"    to 0xFFFFB74D.toInt(), "CINE" to 0xFF7E57C2.toInt(),
        "SERIES"        to 0xFF26A69A.toInt(), "INTERNACIONAL" to 0xFFF43F5E.toInt(),
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rvCategories = RecyclerView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            layoutManager = LinearLayoutManager(requireContext())
            clipToPadding = false
            setPadding(0, (8 * resources.displayMetrics.density).toInt(), 0, (24 * resources.displayMetrics.density).toInt())
            // CLAVE: el RecyclerView vertical es el único que maneja el scroll vertical;
            // las filas internas son horizontales y no interceptan el gesto vertical.
            isNestedScrollingEnabled = true
        }
        return rvCategories
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        categoryAdapter = MobileCategoryRowAdapter(emptyList(), catColors) { ch, list ->
            startActivity(Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_CHANNELS, ArrayList(list))
                putExtra(PlayerActivity.EXTRA_INDEX, list.indexOf(ch).coerceAtLeast(0))
            })
        }
        rvCategories.adapter = categoryAdapter
        loadChannels()
    }

    fun loadFavorites() {
        scope.launch {
            val favs = withContext(Dispatchers.IO) {
                try { ApiService.getFavorites() } catch (_: Exception) { emptyList() }
            }
            if (favs.isEmpty()) {
                categoryAdapter.updateData(listOf("Sin favoritos" to emptyList()))
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

    fun getCategories(): List<String> =
        allChannels.filter { it.category != "ADULTOS" }
            .map { it.category }.distinct()
            .sortedBy { catOrder.indexOf(it).let { i -> if (i < 0) 999 else i } }

    fun filterCategories(cats: List<String>) {
        buildRows(allChannels.filter { it.category in cats })
    }

    fun loadChannels() = loadChannelsList(false)

    fun loadChannelsList(useList2: Boolean) {
        scope.launch {
            val cached = withContext(Dispatchers.IO) {
                try { if (useList2) ApiService.getCachedChannels2() else ApiService.getCachedChannels() } catch (_: Exception) { emptyList() }
            }
            if (cached.isNotEmpty()) { allChannels = cached; buildRows(allChannels) }

            val fresh = withContext(Dispatchers.IO) {
                try { if (useList2) ApiService.getChannels2() else ApiService.getChannels() } catch (_: Exception) { emptyList() }
            }
            if (fresh.isNotEmpty()) {
                allChannels = fresh
                buildRows(allChannels)
                onChannelsLoaded?.invoke()
            } else if (allChannels.isNotEmpty()) {
                onChannelsLoaded?.invoke()
            }
        }
    }

    private fun buildRows(channels: List<Channel>) {
        val isAdultFilter = channels.isNotEmpty() && channels.all { it.category == "ADULTOS" }
        val grouped = if (isAdultFilter) channels.groupBy { it.category }
        else channels.filter { it.category != "ADULTOS" }.groupBy { it.category }
        val sorted = catOrder.mapNotNull { grouped[it]?.let { chs -> it to chs.sortedBy { c -> c.number } } } +
                grouped.filter { it.key !in catOrder }.map { it.key to it.value.sortedBy { c -> c.number } }
        categoryAdapter.updateData(sorted)
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Adapter de filas por categoría — vertical, cada fila scrollea horizontal
// ─────────────────────────────────────────────────────────────────────────────
class MobileCategoryRowAdapter(
    private var rows: List<Pair<String, List<Channel>>>,
    private val catColors: Map<String, Int>,
    private val onChannelClick: (Channel, List<Channel>) -> Unit
) : RecyclerView.Adapter<MobileCategoryRowAdapter.RowVH>() {

    fun updateData(newRows: List<Pair<String, List<Channel>>>) {
        if (rows == newRows) return
        val oldSize = rows.size
        rows = newRows
        // Si la cantidad de filas no cambió, asumimos mismas categorías
        // y solo refrescamos contenido sin parpadeo total
        if (oldSize == newRows.size && oldSize > 0) {
            notifyItemRangeChanged(0, newRows.size)
        } else {
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = rows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowVH {
        val ctx = parent.context
        val dp = ctx.resources.displayMetrics.density
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (20 * dp).toInt() }
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
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * dp).toInt(), (6 * dp).toInt(), (16 * dp).toInt(), (10 * dp).toInt())
        }
        header.addView(View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((3 * dp).toInt(), (16 * dp).toInt()).apply { marginEnd = (8 * dp).toInt() }
            background = GradientDrawable().apply { cornerRadius = 2 * dp; setColor(color) }
        })
        header.addView(TextView(ctx).apply {
            text = cat; textSize = 12f; setTextColor(color)
            typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.08f
        })
        header.addView(TextView(ctx).apply {
            text = "${channels.size}"; textSize = 10f; setTextColor(0xFF4A5568.toInt())
            typeface = Typeface.MONOSPACE
            background = GradientDrawable().apply { cornerRadius = 10 * dp; setColor(0xFF141929.toInt()); setStroke(1, 0x0FFFFFFF.toInt()) }
            setPadding((6 * dp).toInt(), (1 * dp).toInt(), (6 * dp).toInt(), (1 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { marginStart = (6 * dp).toInt() }
        })
        root.addView(header)

        val rv = RecyclerView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
            clipToPadding = false
            setPadding((12 * dp).toInt(), 0, (12 * dp).toInt(), 0)
            overScrollMode = View.OVER_SCROLL_NEVER
            // No interceptar el scroll vertical del padre: solo consume eventos horizontales
            adapter = MobileChannelCardAdapter(channels, onChannelClick)
        }
        root.addView(rv)
    }

    class RowVH(view: View) : RecyclerView.ViewHolder(view)
}

// ─────────────────────────────────────────────────────────────────────────────
//  Cards de canal — touch-friendly, sin foco de D-pad (mobile = tap directo)
// ─────────────────────────────────────────────────────────────────────────────
class MobileChannelCardAdapter(
    private val channels: List<Channel>,
    private val onClick: (Channel, List<Channel>) -> Unit
) : RecyclerView.Adapter<MobileChannelCardAdapter.CardVH>() {

    override fun getItemCount() = channels.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardVH {
        val ctx = parent.context
        val dp = ctx.resources.displayMetrics.density
        val cardW = (118 * dp).toInt()
        val cardH = (108 * dp).toInt()
        val thumbH = (70 * dp).toInt()

        val card = FrameLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(cardW, cardH).apply { marginEnd = (8 * dp).toInt() }
            background = GradientDrawable().apply {
                cornerRadius = 12 * dp
                setColor(0xFF111827.toInt())
                setStroke(1, 0xFF0D1A2A.toInt())
            }
            isClickable = true
        }

        val thumb = FrameLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, thumbH)
            setBackgroundColor(0xFF0F1422.toInt())
        }
        val logo = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams((66 * dp).toInt(), (52 * dp).toInt(), Gravity.CENTER)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            tag = "logo"
        }
        thumb.addView(logo)

        val infoBar = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = thumbH }
            setBackgroundColor(0xFF111827.toInt())
            setPadding((7 * dp).toInt(), (5 * dp).toInt(), (7 * dp).toInt(), (5 * dp).toInt())
        }
        infoBar.addView(TextView(ctx).apply {
            textSize = 9f; setTextColor(0xFF4A5568.toInt()); typeface = Typeface.MONOSPACE; tag = "num"
        })
        infoBar.addView(TextView(ctx).apply {
            textSize = 10f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END; tag = "name"
        })

        card.addView(thumb)
        card.addView(infoBar)
        return CardVH(card)
    }

    override fun onBindViewHolder(holder: CardVH, position: Int) {
        val ch = channels[position]
        val ctx = holder.card.context
        val card = holder.card

        card.findViewWithTag<TextView>("num")?.text = "CH ${ch.number}"
        card.findViewWithTag<TextView>("name")?.text = ch.name

        val logo = card.findViewWithTag<ImageView>("logo")
        if (logo != null && ch.logoUrl.isNotBlank()) {
            Glide.with(ctx).load(ch.logoUrl).into(logo)
        } else {
            logo?.setImageDrawable(null)
        }

        card.setOnClickListener { onClick(ch, channels) }
    }

    class CardVH(val card: FrameLayout) : RecyclerView.ViewHolder(card)
}

