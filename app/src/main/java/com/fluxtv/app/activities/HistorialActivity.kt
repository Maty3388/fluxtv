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
import com.fluxtv.app.databinding.ActivityHistorialBinding
import com.fluxtv.app.models.Channel
import com.fluxtv.app.utils.HistoryEntry
import com.fluxtv.app.utils.WatchHistory

class HistorialActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistorialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnClear.setOnClickListener {
            WatchHistory.clear(this)
            loadHistory()
        }
        binding.rvList.layoutManager = GridLayoutManager(this, 6)
        loadHistory()
    }

    private fun loadHistory() {
        val items = WatchHistory.getAll(this)
        binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.rvList.adapter = HistoryAdapter(items) { entry ->
            val ch = Channel(entry.id, entry.name, entry.category, entry.posterUrl, entry.streamUrl)
            startActivity(Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_CHANNELS, arrayListOf(ch))
                putExtra(PlayerActivity.EXTRA_INDEX, 0)
            })
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }
}

class HistoryAdapter(private val items: List<HistoryEntry>, private val onClick: (HistoryEntry) -> Unit)
    : RecyclerView.Adapter<HistoryAdapter.VH>() {

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
        val e = items[position]
        holder.tvTitle.text = e.name
        holder.tvYear.text = when (e.type) {
            "movie" -> "🎬 Película"
            "serie" -> "📺 Serie"
            else -> "📡 Canal"
        }
        if (e.posterUrl.isNotEmpty()) Glide.with(holder.ivPoster).load(e.posterUrl).into(holder.ivPoster)
        holder.itemView.setOnClickListener { onClick(e) }
        holder.itemView.setOnFocusChangeListener { v, focused ->
            v.animate().scaleX(if (focused) 1.08f else 1f).scaleY(if (focused) 1.08f else 1f).setDuration(120).start()
        }
    }

    override fun getItemCount() = items.size
}
