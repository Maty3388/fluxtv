package com.fluxtv.app.activities

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fluxtv.app.R
import com.fluxtv.app.databinding.ActivityMainBinding
import com.fluxtv.app.fragments.MainFragment
import com.fluxtv.app.services.ApiService
import com.fluxtv.app.BuildConfig
import com.fluxtv.app.utils.AutoUpdater
import com.fluxtv.app.utils.Prefs
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var mainFragment: MainFragment? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var sidebarExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainFragment = MainFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContainer, mainFragment!!)
            .commit()

        // Interceptar BACK antes que Leanback
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                toggleSidebar()
            }
        })

        setupSidebar()
        binding.btnProfile.setOnClickListener {
            startActivity(android.content.Intent(this, ProfileActivity::class.java))
        }
        
        // Mostrar vencimiento en header
        val subEnd = Prefs.getSubEnd(this)
        if (subEnd.isNotEmpty()) {
            binding.tvVencimiento.text = "Vence: $subEnd"
        }
        
        checkUpdate()
    }

    private fun setupSidebar() {
        // Íconos colapsados - al clickear expanden el sidebar
        binding.btnTvIcon.setOnClickListener { expandSidebar() }
        // Botón explorar/buscar en colapsado abre búsqueda directo
        binding.btnPeliculasIcon.setOnClickListener { startActivity(android.content.Intent(this, SearchActivity::class.java)) }
        binding.btnPeliculasIcon.setOnClickListener { expandSidebar() }
        binding.btnSeriesIcon.setOnClickListener { collapseSidebar(); mainFragment?.loadFavorites() }
        binding.btnAdultosIcon.setOnClickListener { expandSidebar() }
        binding.btnClearCacheIcon.setOnClickListener { expandSidebar() }
        binding.btnLogoutIcon.setOnClickListener { expandSidebar() }

        // Botones expandidos
        binding.btnTv.setOnClickListener {
            collapseSidebar()
            mainFragment?.filterCategory(null)
        }
        binding.btnPeliculas.setOnClickListener {
            collapseSidebar()
            mainFragment?.filterCategory("CINE")
        }
        binding.btnSeries.setOnClickListener {
            collapseSidebar()
            mainFragment?.loadFavorites()
        }
        binding.btnAdultos.setOnClickListener {
            collapseSidebar()
            mainFragment?.filterCategory("ADULTOS")
        }
        binding.btnClearCache.setOnClickListener {
            cacheDir.deleteRecursively()
            collapseSidebar()
            Toast.makeText(this, "Caché borrado", Toast.LENGTH_SHORT).show()
        }
        binding.btnLogout.setOnClickListener {
            Prefs.logout(this)
            startActivity(android.content.Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    fun toggleSidebar() {
        if (sidebarExpanded) collapseSidebar() else expandSidebar()
    }

    private fun expandSidebar() {
        sidebarExpanded = true
        binding.sidebarCollapsed.visibility = View.GONE
        binding.sidebarExpanded.visibility = View.VISIBLE
        binding.btnTv.requestFocus()
        
        // Navegación entre botones del sidebar
        val sidebarBtns = listOf(binding.btnTv, binding.btnPeliculas, binding.btnSeries, binding.btnAdultos, binding.btnClearCache, binding.btnLogout)
        sidebarBtns.forEachIndexed { i, btn ->
            btn.setOnKeyListener { _, keyCode, event ->
                if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                            sidebarBtns.getOrNull(i + 1)?.requestFocus(); true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                            sidebarBtns.getOrNull(i - 1)?.requestFocus(); true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            collapseSidebar(); true
                        }
                        else -> false
                    }
                } else false
            }
        }
    }

    private fun collapseSidebar() {
        sidebarExpanded = false
        binding.sidebarExpanded.visibility = View.GONE
        binding.sidebarCollapsed.visibility = View.VISIBLE
    }

    private fun checkUpdate() {
        scope.launch {
            try {
                val ver = withContext(Dispatchers.IO) { ApiService.getVersion() }
                if (ver != null) AutoUpdater.check(this@MainActivity, BuildConfig.VERSION_NAME, ver)
            } catch (_: Exception) {}
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT &&
            !sidebarExpanded) {
            // Solo bloquear si el foco está en el RecyclerView horizontal en posición 0
            val focused = currentFocus
            if (focused != null) {
                val parent = focused.parent
                if (parent is androidx.recyclerview.widget.RecyclerView) {
                    val pos = parent.getChildAdapterPosition(focused)
                    if (pos == 0) return true // bloquear solo en el primer item
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (sidebarExpanded) { collapseSidebar(); return true }
            android.app.AlertDialog.Builder(this)
                .setTitle("Salir").setMessage("¿Querés salir de Flux TV?")
                .setPositiveButton("Salir") { _,_ -> finish() }
                .setNegativeButton("Cancelar", null).show()
            return true
        }
        // Solo expandir sidebar si el foco está en el primer canal de la grilla
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && !sidebarExpanded) {
            val focused = currentFocus
            // Si el foco está en el fragment container, no abrir sidebar
            if (focused != null && focused.id != R.id.mainContainer) {
                expandSidebar(); return true
            }
            return true // bloquear escape
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
