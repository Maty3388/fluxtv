package com.fluxtv.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fluxtv.app.R
import com.fluxtv.app.databinding.ActivityDetailBinding
import com.fluxtv.app.models.Channel
import com.fluxtv.app.models.Episode
import com.fluxtv.app.models.Movie
import com.fluxtv.app.models.Serie

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding

    companion object {
        const val EXTRA_MOVIE = "movie"
        const val EXTRA_SERIE = "serie"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val movie = intent.getSerializableExtra(EXTRA_MOVIE) as? Movie
        val serie = intent.getSerializableExtra(EXTRA_SERIE) as? Serie

        if (movie != null) {
            binding.tvTitle.text = movie.title
            binding.tvMeta.text = "${movie.year}  ★ ${movie.rating}  •  ${movie.category}"
            binding.tvDescription.text = movie.description.ifEmpty { "Sin descripción disponible." }
            if (movie.posterUrl.isNotEmpty()) Glide.with(this).load(movie.posterUrl).into(binding.ivPoster)
            if (movie.backdropUrl.isNotEmpty()) Glide.with(this).load(movie.backdropUrl).into(binding.ivBackdrop)
            binding.btnPlay.setOnClickListener {
                val ch = Channel(movie.id, movie.title, movie.category, movie.posterUrl, movie.streamUrl)
                startActivity(Intent(this, PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_CHANNELS, arrayListOf(ch))
                    putExtra(PlayerActivity.EXTRA_INDEX, 0)
                })
            }
            binding.btnPlay.requestFocus()
        } else if (serie != null) {
            binding.tvTitle.text = serie.title
            binding.tvMeta.text = "${serie.year}  ★ ${serie.rating}  •  ${serie.category}  •  ${serie.episodes.size} episodios"
            binding.tvDescription.text = serie.description.ifEmpty { "Sin descripción disponible." }
            if (serie.posterUrl.isNotEmpty()) {
                Glide.with(this).load(serie.posterUrl).into(binding.ivPoster)
                Glide.with(this).load(serie.posterUrl).into(binding.ivBackdrop)
            }

            if (serie.episodes.isNotEmpty()) {
                binding.rvEpisodes.visibility = View.VISIBLE
                binding.rvEpisodes.layoutManager = LinearLayoutManager(this)
                binding.rvEpisodes.adapter = EpisodeAdapter(serie.episodes) { ep, position ->
                    val episodeChannels = serie.episodes.map {
                        Channel(serie.id + "_s${it.season}e${it.episode}", it.title, serie.category, serie.posterUrl, it.streamUrl)
                    }
                    startActivity(Intent(this, PlayerActivity::class.java).apply {
                        putExtra(PlayerActivity.EXTRA_CHANNELS, ArrayList(episodeChannels))
                        putExtra(PlayerActivity.EXTRA_INDEX, position)
                    })
                }
                binding.btnPlay.text = "▶ EPISODIO 1"
                binding.btnPlay.setOnClickListener {
                    val episodeChannels = serie.episodes.map {
                        Channel(serie.id + "_s${it.season}e${it.episode}", it.title, serie.category, serie.posterUrl, it.streamUrl)
                    }
                    startActivity(Intent(this, PlayerActivity::class.java).apply {
                        putExtra(PlayerActivity.EXTRA_CHANNELS, ArrayList(episodeChannels))
                        putExtra(PlayerActivity.EXTRA_INDEX, 0)
                    })
                }
            } else {
                binding.btnPlay.setOnClickListener {
                    val ch = Channel(serie.id, serie.title, serie.category, serie.posterUrl, serie.streamUrl)
                    startActivity(Intent(this, PlayerActivity::class.java).apply {
                        putExtra(PlayerActivity.EXTRA_CHANNELS, arrayListOf(ch))
                        putExtra(PlayerActivity.EXTRA_INDEX, 0)
                    })
                }
            }
            binding.btnPlay.requestFocus()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }
}

class EpisodeAdapter(private val items: List<Episode>, private val onClick: (Episode, Int) -> Unit)
    : RecyclerView.Adapter<EpisodeAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNum: TextView = view.findViewById(R.id.tvEpNum)
        val tvTitle: TextView = view.findViewById(R.id.tvEpTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_episode, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ep = items[position]
        holder.tvNum.text = "S${ep.season}E${ep.episode}"
        holder.tvTitle.text = ep.title
        holder.itemView.setOnClickListener { onClick(ep, position) }
    }

    override fun getItemCount() = items.size
}
