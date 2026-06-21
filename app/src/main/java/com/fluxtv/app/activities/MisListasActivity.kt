package com.fluxtv.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fluxtv.app.R
import com.fluxtv.app.databinding.ActivityMisListasBinding
import com.fluxtv.app.models.Channel
import com.fluxtv.app.utils.ListItem
import com.fluxtv.app.utils.MisListas

class MisListasActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMisListasBinding
    private var selectedList: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMisListasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnNew.setOnClickListener { showCreateDialog() }
        binding.rvListas.layoutManager = LinearLayoutManager(this)
        binding.rvItems.layoutManager = GridLayoutManager(this, 6)
        loadLists()
    }

    private fun showCreateDialog() {
        val input = EditText(this).apply {
            hint = "Nombre de la lista"
            setTextColor(getColor(R.color.text_primary))
            setHintTextColor(getColor(R.color.text_secondary))
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Nueva lista")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    MisListas.createList(this, name)
                    loadLists()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun loadLists() {
        val names = MisListas.getListNames(this)
        binding.rvListas.adapter = ListaAdapter(names,
            onClick = { name -> selectedList = name; loadItems(name) },
            onDelete = { name ->
                MisListas.deleteList(this, name)
                if (selectedList == name) { selectedList = null; binding.rvItems.visibility = View.GONE; binding.tvEmpty.visibility = View.VISIBLE }
                loadLists()
            }
        )
        if (names.isNotEmpty() && selectedList == null) {
            selectedList = names[0]
            loadItems(names[0])
        }
    }

    private fun loadItems(listName: String) {
        binding.tvTitle.text = "📑 $listName"
        val items = MisListas.getItems(this, listName)
        if (items.isEmpty()) {
            binding.rvItems.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = "Esta lista está vacía"
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvItems.visibility = View.VISIBLE
            binding.rvItems.adapter = ListaItemAdapter(items,
                onClick = { item ->
                    val ch = Channel(item.id, item.name, item.category, item.posterUrl, item.streamUrl)
                    startActivity(Intent(this, PlayerActivity::class.java).apply {
                        putExtra(PlayerActivity.EXTRA_CHANNELS, arrayListOf(ch))
                        putExtra(PlayerActivity.EXTRA_INDEX, 0)
                    })
                },
                onRemove = { item ->
                    MisListas.removeItem(this, listName, item.id)
                    loadItems(listName)
                }
            )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }
}

class ListaAdapter(private val names: List<String>, private val onClick: (String) -> Unit, private val onDelete: (String) -> Unit)
    : RecyclerView.Adapter<ListaAdapter.VH>() {
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvDelete: TextView = view.findViewById(R.id.tvDelete)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_lista, parent, false)
        return VH(v)
    }
    override fun onBindViewHolder(holder: VH, position: Int) {
        val name = names[position]
        holder.tvName.text = name
        holder.itemView.setOnClickListener { onClick(name) }
        holder.tvDelete.setOnClickListener { onDelete(name) }
    }
    override fun getItemCount() = names.size
}

class ListaItemAdapter(private val items: List<ListItem>, private val onClick: (ListItem) -> Unit, private val onRemove: (ListItem) -> Unit)
    : RecyclerView.Adapter<ListaItemAdapter.VH>() {
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
        val item = items[position]
        holder.tvTitle.text = item.name
        holder.tvYear.text = "Quitar ✕"
        holder.ivPoster.setImageDrawable(null)
        if (item.posterUrl.isNotEmpty()) Glide.with(holder.ivPoster).load(item.posterUrl).into(holder.ivPoster)
        holder.itemView.setOnClickListener { onClick(item) }
        holder.tvYear.setOnClickListener { onRemove(item) }
        holder.itemView.setOnFocusChangeListener { v, focused ->
            v.animate().scaleX(if (focused) 1.08f else 1f).scaleY(if (focused) 1.08f else 1f).setDuration(120).start()
        }
    }
    override fun getItemCount() = items.size
}
