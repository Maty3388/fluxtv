package com.fluxtv.app.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fluxtv.app.databinding.ActivitySearchBinding
import com.fluxtv.app.models.Channel
import com.fluxtv.app.services.ApiService
import kotlinx.coroutines.*

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private val scope = CoroutineScope(Dispatchers.Main)
    private var allChannels = listOf<Channel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { search(s.toString()) }
        })

        binding.etSearch.setOnEditorActionListener { _, _, _ ->
            search(binding.etSearch.text.toString()); true
        }

        binding.rvResults.layoutManager = LinearLayoutManager(this)
        scope.launch {
            allChannels = withContext(Dispatchers.IO) {
                try { ApiService.getChannels() } catch (_: Exception) { ApiService.getCachedChannels() }
            }
            binding.tvResults.text = "${allChannels.size} canales disponibles"
            binding.etSearch.requestFocus()
        }
    }

    private fun search(query: String) {
        if (query.length < 2) {
            binding.tvResults.text = "Escribí al menos 2 caracteres"
            binding.rvResults.adapter = null
            return
        }
        val results = allChannels.filter { it.name.contains(query, ignoreCase = true) }
        binding.tvResults.text = "${results.size} resultados para \"$query\""
        binding.rvResults.adapter = SearchAdapter(results) { ch, idx ->
            startActivity(Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_CHANNELS, ArrayList(results))
                putExtra(PlayerActivity.EXTRA_INDEX, idx)
            })
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

class SearchAdapter(
    private val channels: List<Channel>,
    private val onClick: (Channel, Int) -> Unit
) : RecyclerView.Adapter<SearchAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(android.R.id.text1)
        val tvCat: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        view.isFocusable = true
        view.setBackgroundResource(android.R.drawable.list_selector_background)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ch = channels[position]
        holder.tvName.text = ch.name
        holder.tvName.setTextColor(0xFFFFFFFF.toInt())
        holder.tvCat.text = ch.category
        holder.tvCat.setTextColor(0xFF00E5FF.toInt())
        holder.itemView.setOnClickListener { onClick(ch, position) }
        holder.itemView.setOnFocusChangeListener { v, focused ->
            v.setBackgroundColor(if (focused) 0xFF001A2E.toInt() else 0xFF060E1A.toInt())
        }
    }

    override fun getItemCount() = channels.size
}
