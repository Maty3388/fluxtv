package com.fluxtv.app.fragments

import android.content.Intent
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.*
import com.bumptech.glide.Glide
import com.fluxtv.app.activities.PlayerActivity
import com.fluxtv.app.activities.VodActivity
import com.fluxtv.app.models.Channel
import com.fluxtv.app.models.Movie
import com.fluxtv.app.models.Serie
import com.fluxtv.app.services.ApiService
import kotlinx.coroutines.*

class VodFragment : RowsSupportFragment() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var type = VodActivity.TYPE_MOVIES
    var onCategoriesLoaded: ((List<Pair<String, Int>>) -> Unit)? = null

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setOnKeyListener { _, keyCode, event ->
            keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT && event.action == android.view.KeyEvent.ACTION_DOWN
        }

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            when (item) {
                is Movie -> startActivity(Intent(requireContext(), com.fluxtv.app.activities.DetailActivity::class.java).apply {
                    putExtra(com.fluxtv.app.activities.DetailActivity.EXTRA_MOVIE, item)
                })
                is Serie -> startActivity(Intent(requireContext(), com.fluxtv.app.activities.DetailActivity::class.java).apply {
                    putExtra(com.fluxtv.app.activities.DetailActivity.EXTRA_SERIE, item)
                })
            }
        }
    }

    var onLoaded: (() -> Unit)? = null

    fun load(vodType: String) {
        type = vodType
        scope.launch {
            if (!isAdded) return@launch
            val rowsAdapter = ArrayObjectAdapter(ListRowPresenter(FocusHighlight.ZOOM_FACTOR_SMALL).apply {
                shadowEnabled = false; selectEffectEnabled = false
            })

            if (type == VodActivity.TYPE_MOVIES) {
                val movies = withContext(Dispatchers.IO) {
                    try { ApiService.getMovies() } catch (_: Exception) { emptyList() }
                }
                val progressIds = com.fluxtv.app.utils.Prefs.getAllProgressIds(requireContext())
                val continuing = movies.filter { it.id in progressIds }
                if (continuing.isNotEmpty()) {
                    val adapter = ArrayObjectAdapter(MoviePresenter())
                    continuing.forEach { adapter.add(it) }
                    rowsAdapter.add(ListRow(HeaderItem("▶ CONTINUAR VIENDO"), adapter))
                }
                val featured = movies.filter { it.featured }
                if (featured.isNotEmpty()) {
                    val adapter = ArrayObjectAdapter(MoviePresenter())
                    featured.forEach { adapter.add(it) }
                    rowsAdapter.add(ListRow(HeaderItem("⭐ DESTACADAS"), adapter))
                }
                val categoryIndices = mutableListOf<Pair<String, Int>>()
                val byCategory = movies.groupBy { it.category }
                byCategory.forEach { (cat, items) ->
                    val adapter = ArrayObjectAdapter(MoviePresenter())
                    items.forEach { adapter.add(it) }
                    categoryIndices.add(cat.ifEmpty { "OTRAS" } to rowsAdapter.size())
                    rowsAdapter.add(ListRow(HeaderItem(cat.ifEmpty { "OTRAS" }), adapter))
                }
                onCategoriesLoaded?.invoke(categoryIndices)
            } else {
                val series = withContext(Dispatchers.IO) {
                    try { ApiService.getSeries() } catch (_: Exception) { emptyList() }
                }
                val featured = series.filter { it.featured }
                if (featured.isNotEmpty()) {
                    val adapter = ArrayObjectAdapter(SeriePresenter())
                    featured.forEach { adapter.add(it) }
                    rowsAdapter.add(ListRow(HeaderItem("⭐ DESTACADAS"), adapter))
                }
                val categoryIndices = mutableListOf<Pair<String, Int>>()
                val byCategory = series.groupBy { it.category }
                byCategory.forEach { (cat, items) ->
                    val adapter = ArrayObjectAdapter(SeriePresenter())
                    items.forEach { adapter.add(it) }
                    categoryIndices.add(cat.ifEmpty { "OTRAS" } to rowsAdapter.size())
                    rowsAdapter.add(ListRow(HeaderItem(cat.ifEmpty { "OTRAS" }), adapter))
                }
                onCategoriesLoaded?.invoke(categoryIndices)
            }
            if (!isAdded) return@launch
            adapter = rowsAdapter
            onLoaded?.invoke()
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

class MoviePresenter : Presenter() {
    override fun onCreateViewHolder(parent: android.view.ViewGroup): ViewHolder {
        val ctx = parent.context
        val dp = ctx.resources.displayMetrics.density
        val card = android.widget.FrameLayout(ctx).apply {
            layoutParams = android.view.ViewGroup.LayoutParams((120*dp).toInt(), (180*dp).toInt())
            isFocusable = true; isFocusableInTouchMode = true
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 8*dp; setColor(0xFF1F0F2E.toInt())
            }
            (layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.setMargins((4*dp).toInt(),(4*dp).toInt(),(4*dp).toInt(),(4*dp).toInt())
        }
        val poster = android.widget.ImageView(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP; tag = "poster"
        }
        val nameOverlay = android.widget.LinearLayout(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = android.view.Gravity.BOTTOM
            }
            setBackgroundColor(0xCC000000.toInt())
            setPadding((6*dp).toInt(), (4*dp).toInt(), (6*dp).toInt(), (4*dp).toInt())
            orientation = android.widget.LinearLayout.VERTICAL
        }
        val name = android.widget.TextView(ctx).apply {
            setTextColor(0xFFFFFFFF.toInt()); textSize = 10f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; tag = "name"
        }
        nameOverlay.addView(name); card.addView(poster); card.addView(nameOverlay)
        return ViewHolder(card)
    }

    override fun onBindViewHolder(vh: ViewHolder, item: Any) {
        val m = item as Movie
        val card = vh.view as android.widget.FrameLayout
        card.findViewWithTag<android.widget.TextView>("name")?.text = m.title
        val poster = card.findViewWithTag<android.widget.ImageView>("poster")
        poster?.setImageDrawable(null)
        if (m.posterUrl.isNotEmpty()) Glide.with(card).load(m.posterUrl).into(poster!!)
    }

    override fun onUnbindViewHolder(vh: ViewHolder) {
        (vh.view as android.widget.FrameLayout).findViewWithTag<android.widget.ImageView>("poster")?.setImageDrawable(null)
    }
}

class SeriePresenter : Presenter() {
    override fun onCreateViewHolder(parent: android.view.ViewGroup): ViewHolder {
        val ctx = parent.context
        val dp = ctx.resources.displayMetrics.density
        val card = android.widget.FrameLayout(ctx).apply {
            layoutParams = android.view.ViewGroup.LayoutParams((120*dp).toInt(), (180*dp).toInt())
            isFocusable = true; isFocusableInTouchMode = true
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 8*dp; setColor(0xFF1F0F2E.toInt())
            }
            (layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.setMargins((4*dp).toInt(),(4*dp).toInt(),(4*dp).toInt(),(4*dp).toInt())
        }
        val poster = android.widget.ImageView(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP; tag = "poster"
        }
        val nameOverlay = android.widget.LinearLayout(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = android.view.Gravity.BOTTOM
            }
            setBackgroundColor(0xCC000000.toInt())
            setPadding((6*dp).toInt(), (4*dp).toInt(), (6*dp).toInt(), (4*dp).toInt())
            orientation = android.widget.LinearLayout.VERTICAL
        }
        val name = android.widget.TextView(ctx).apply {
            setTextColor(0xFFFFFFFF.toInt()); textSize = 10f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; tag = "name"
        }
        nameOverlay.addView(name); card.addView(poster); card.addView(nameOverlay)
        return ViewHolder(card)
    }

    override fun onBindViewHolder(vh: ViewHolder, item: Any) {
        val s = item as Serie
        val card = vh.view as android.widget.FrameLayout
        card.findViewWithTag<android.widget.TextView>("name")?.text = s.title
        val poster = card.findViewWithTag<android.widget.ImageView>("poster")
        poster?.setImageDrawable(null)
        if (s.posterUrl.isNotEmpty()) Glide.with(card).load(s.posterUrl).into(poster!!)
    }

    override fun onUnbindViewHolder(vh: ViewHolder) {
        (vh.view as android.widget.FrameLayout).findViewWithTag<android.widget.ImageView>("poster")?.setImageDrawable(null)
    }
}
