package com.fluxtv.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fluxtv.app.R
import com.fluxtv.app.databinding.ActivityVodBinding
import com.fluxtv.app.models.Channel
import com.fluxtv.app.models.Movie
import com.fluxtv.app.models.Serie
import com.fluxtv.app.services.ApiService
import kotlinx.coroutines.*

class VodActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVodBinding
    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        const val TYPE_MOVIES = "movies"
        const val TYPE_SERIES = "series"
        const val EXTRA_TYPE = "type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVodBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_MOVIES
        binding.tvTitle.text = if (type == TYPE_MOVIES) "🎬 Películas" else "📺 Series"
        binding.btnBack.setOnClickListener { finish() }

        binding.rvVod.layoutManager = GridLayoutManager(this, 6)

        scope.launch {
            if (type == TYPE_MOVIES) {
                val movies = withContext(Dispatchers.IO) {
                    try { ApiService.getMovies() } catch (_: Exception) { emptyList() }
                }
                binding.rvVod.adapter = MoviesAdapter(movies) { movie ->
                    val ch = Channel(movie.id, movie.title, movie.category,
                        movie.posterUrl, movie.streamUrl)
                    startActivity(Intent(this@VodActivity, PlayerActivity::class.java).apply {
                        putExtra(PlayerActivity.EXTRA_CHANNELS, arrayListOf(ch))
                        putExtra(PlayerActivity.EXTRA_INDEX, 0)
                    })
                }
            } else {
                val series = withContext(Dispatchers.IO) {
                    try { ApiService.getSeries() } catch (_: Exception) { emptyList() }
                }
                binding.rvVod.adapter = SeriesAdapter(series) { serie ->
                    val ch = Channel(serie.id, serie.title, serie.category,
                        serie.posterUrl, serie.streamUrl)
                    startActivity(Intent(this@VodActivity, PlayerActivity::class.java).apply {
                        putExtra(PlayerActivity.EXTRA_CHANNELS, arrayListOf(ch))
                        putExtra(PlayerActivity.EXTRA_INDEX, 0)
                    })
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

class MoviesAdapter(private val items: List<Movie>, private val onClick: (Movie) -> Unit)
    : RecyclerView.Adapter<MoviesAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivPoster: ImageView = view.findViewById(R.id.ivPoster)
        val tvTitle: TextView = view.findViewById(R.id.tvVodTitle)
        val tvYear: TextView = view.findViewById(R.id.tvVodYear)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_vod, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        holder.tvTitle.text = m.title
        holder.tvYear.text = "${m.year} ★${m.rating}"
        if (m.posterUrl.isNotEmpty()) Glide.with(holder.ivPoster).load(m.posterUrl).into(holder.ivPoster)
        holder.itemView.setOnClickListener { onClick(m) }
        holder.itemView.setOnFocusChangeListener { v, focused ->
            v.animate().scaleX(if (focused) 1.08f else 1f).scaleY(if (focused) 1.08f else 1f).setDuration(120).start()
        }
    }

    override fun getItemCount() = items.size
}

class SeriesAdapter(private val items: List<Serie>, private val onClick: (Serie) -> Unit)
    : RecyclerView.Adapter<SeriesAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivPoster: ImageView = view.findViewById(R.id.ivPoster)
        val tvTitle: TextView = view.findViewById(R.id.tvVodTitle)
        val tvYear: TextView = view.findViewById(R.id.tvVodYear)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_vod, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        holder.tvTitle.text = s.title
        holder.tvYear.text = s.year
        if (s.posterUrl.isNotEmpty()) Glide.with(holder.ivPoster).load(s.posterUrl).into(holder.ivPoster)
        holder.itemView.setOnClickListener { onClick(s) }
        holder.itemView.setOnFocusChangeListener { v, focused ->
            v.animate().scaleX(if (focused) 1.08f else 1f).scaleY(if (focused) 1.08f else 1f).setDuration(120).start()
        }
    }

    override fun getItemCount() = items.size
}
