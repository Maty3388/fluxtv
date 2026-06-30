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
import com.fluxtv.app.activities.DetailActivity
import com.fluxtv.app.activities.VodActivity
import com.fluxtv.app.models.Movie
import com.fluxtv.app.models.Serie
import com.fluxtv.app.services.ApiService
import kotlinx.coroutines.*

// ─────────────────────────────────────────────────────────────────────────────
//  VodMobileFragment — RecyclerView vertical de filas horizontales (Movies/Series)
//  Para mobile, evitando el bug de scroll atrapado de RowsSupportFragment (Leanback).
// ─────────────────────────────────────────────────────────────────────────────
class VodMobileFragment : Fragment() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var type = VodActivity.TYPE_MOVIES
    var onCategoriesLoaded: ((List<Pair<String, Int>>) -> Unit)? = null
    var onLoaded: (() -> Unit)? = null

    private lateinit var rv: RecyclerView
    private lateinit var rowAdapter: VodRowAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rv = RecyclerView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            layoutManager = LinearLayoutManager(requireContext())
            clipToPadding = false
            setPadding(0, (8 * resources.displayMetrics.density).toInt(), 0, (24 * resources.displayMetrics.density).toInt())
        }
        return rv
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rowAdapter = VodRowAdapter { item ->
            when (item) {
                is Movie -> startActivity(Intent(requireContext(), DetailActivity::class.java).apply {
                    putExtra(DetailActivity.EXTRA_MOVIE, item)
                })
                is Serie -> startActivity(Intent(requireContext(), DetailActivity::class.java).apply {
                    putExtra(DetailActivity.EXTRA_SERIE, item)
                })
            }
        }
        rv.adapter = rowAdapter
    }

    fun setSelectedPosition(rowIndex: Int, smooth: Boolean) {
        if (smooth) rv.smoothScrollToPosition(rowIndex) else rv.scrollToPosition(rowIndex)
    }

    fun load(vodType: String) {
        type = vodType
        scope.launch {
            if (!isAdded) return@launch
            val rows = mutableListOf<Pair<String, List<Any>>>()

            if (type == VodActivity.TYPE_MOVIES) {
                val movies = withContext(Dispatchers.IO) {
                    try { ApiService.getMovies() } catch (_: Exception) { emptyList() }
                }
                val progressIds = com.fluxtv.app.utils.Prefs.getAllProgressIds(requireContext())
                val continuing = movies.filter { progressIds.contains(it.id) }
                if (continuing.isNotEmpty()) rows.add("▶ CONTINUAR VIENDO" to continuing)

                val featured = movies.filter { it.featured }
                if (featured.isNotEmpty()) rows.add("⭐ DESTACADAS" to featured)

                val categoryIndices = mutableListOf<Pair<String, Int>>()
                movies.groupBy { it.category }.forEach { (cat, items) ->
                    val label = cat.ifEmpty { "OTRAS" }
                    categoryIndices.add(label to rows.size)
                    rows.add(label to items)
                }
                onCategoriesLoaded?.invoke(categoryIndices)
            } else {
                val series = withContext(Dispatchers.IO) {
                    try { ApiService.getSeries() } catch (_: Exception) { emptyList() }
                }
                val featured = series.filter { it.featured }
                if (featured.isNotEmpty()) rows.add("⭐ DESTACADAS" to featured)

                val categoryIndices = mutableListOf<Pair<String, Int>>()
                series.groupBy { it.category }.forEach { (cat, items) ->
                    val label = cat.ifEmpty { "OTRAS" }
                    categoryIndices.add(label to rows.size)
                    rows.add(label to items)
                }
                onCategoriesLoaded?.invoke(categoryIndices)
            }

            if (!isAdded) return@launch
            rowAdapter.updateData(rows)
            onLoaded?.invoke()
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Adapter de filas verticales (cada fila = RecyclerView horizontal de posters)
// ─────────────────────────────────────────────────────────────────────────────
class VodRowAdapter(
    private val onClick: (Any) -> Unit
) : RecyclerView.Adapter<VodRowAdapter.RowVH>() {

    private var rows: List<Pair<String, List<Any>>> = emptyList()

    fun updateData(newRows: List<Pair<String, List<Any>>>) {
        if (rows == newRows) return
        rows = newRows
        notifyDataSetChanged()
    }

    override fun getItemCount() = rows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowVH {
        val ctx = parent.context
        val dp = ctx.resources.displayMetrics.density
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = (18 * dp).toInt() }
        }
        return RowVH(root)
    }

    override fun onBindViewHolder(holder: RowVH, position: Int) {
        val (title, items) = rows[position]
        val ctx = holder.itemView.context
        val dp = ctx.resources.displayMetrics.density
        val root = holder.itemView as LinearLayout
        root.removeAllViews()

        root.addView(TextView(ctx).apply {
            text = title; textSize = 13f; setTextColor(0xFF00D4FF.toInt())
            typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.05f
            setPadding((16 * dp).toInt(), (4 * dp).toInt(), (16 * dp).toInt(), (8 * dp).toInt())
        })

        root.addView(RecyclerView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
            clipToPadding = false
            setPadding((12 * dp).toInt(), 0, (12 * dp).toInt(), 0)
            overScrollMode = View.OVER_SCROLL_NEVER
            adapter = VodPosterAdapter(items, onClick)
        })
    }

    class RowVH(view: View) : RecyclerView.ViewHolder(view)
}

// ─────────────────────────────────────────────────────────────────────────────
//  Cards de poster (Movie o Serie)
// ─────────────────────────────────────────────────────────────────────────────
class VodPosterAdapter(
    private val items: List<Any>,
    private val onClick: (Any) -> Unit
) : RecyclerView.Adapter<VodPosterAdapter.PosterVH>() {

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosterVH {
        val ctx = parent.context
        val dp = ctx.resources.displayMetrics.density
        val card = FrameLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams((110 * dp).toInt(), (165 * dp).toInt())
                .apply { marginEnd = (8 * dp).toInt() }
            background = GradientDrawable().apply { cornerRadius = 8 * dp; setColor(0xFF1F0F2E.toInt()) }
            isClickable = true
        }
        val poster = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.CENTER_CROP
            tag = "poster"
        }
        val nameOverlay = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { gravity = Gravity.BOTTOM }
            setBackgroundColor(0xCC000000.toInt())
            setPadding((6 * dp).toInt(), (4 * dp).toInt(), (6 * dp).toInt(), (4 * dp).toInt())
        }
        nameOverlay.addView(TextView(ctx).apply {
            setTextColor(Color.WHITE); textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END
            tag = "name"
        })
        card.addView(poster)
        card.addView(nameOverlay)
        return PosterVH(card)
    }

    override fun onBindViewHolder(holder: PosterVH, position: Int) {
        val item = items[position]
        val card = holder.card
        val ctx = card.context

        val (title, posterUrl) = when (item) {
            is Movie -> item.title to item.posterUrl
            is Serie -> item.title to item.posterUrl
            else -> "" to ""
        }
        card.findViewWithTag<TextView>("name")?.text = title

        val poster = card.findViewWithTag<ImageView>("poster")
        poster?.setImageDrawable(null)
        if (posterUrl.isNotEmpty()) Glide.with(ctx).load(posterUrl).into(poster!!)

        card.setOnClickListener { onClick(item) }
    }

    class PosterVH(val card: FrameLayout) : RecyclerView.ViewHolder(card)
}

