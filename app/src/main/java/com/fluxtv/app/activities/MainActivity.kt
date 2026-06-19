package com.fluxtv.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.fluxtv.app.BuildConfig
import com.fluxtv.app.R
import com.fluxtv.app.databinding.ActivityMainBinding
import com.fluxtv.app.fragments.MainFragment
import com.fluxtv.app.services.ApiService
import com.facebook.shimmer.ShimmerFrameLayout
import com.fluxtv.app.utils.AutoUpdater
import com.fluxtv.app.utils.Prefs
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var mainFragment: MainFragment? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var selectedItem = 0

    private var isMobile = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.fluxtv.app.services.ApiService.appContext = applicationContext
        isMobile = !com.fluxtv.app.utils.DeviceUtils.isTV(this)

        if (isMobile) {
            setContentView(R.layout.activity_main_mobile)
            mainFragment = MainFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainContainer, mainFragment!!)
                .commit()
            setupMobileNav()
            highlightMobileNav(R.id.navInicio)
            checkUpdate()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainFragment = MainFragment()
        mainFragment!!.onChannelsLoaded = {
            binding.shimmerLayout.stopShimmer()
            binding.shimmerLayout.visibility = View.GONE
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContainer, mainFragment!!)
            .commit()
        binding.shimmerLayout.startShimmer()

        // Mostrar datos usuario
        binding.tvUserEmail.text = "👤 " + Prefs.getEmail(this)
        val subEnd = Prefs.getSubEnd(this)
        if (subEnd.isNotEmpty()) binding.tvVencimiento.text = subEnd

        setupSidebar()
        setupNavigation()
        checkUpdate()
    }

    private fun setupMobileNav() {
        findViewById<View>(R.id.navInicio).setOnClickListener {
            mainFragment?.filterCategory(null)
            highlightMobileNav(R.id.navInicio)
        }
        findViewById<View>(R.id.navPeliculas).setOnClickListener {
            startActivity(Intent(this, VodActivity::class.java).apply { putExtra(VodActivity.EXTRA_TYPE, VodActivity.TYPE_MOVIES) })
        }
        findViewById<View>(R.id.navSeries).setOnClickListener {
            startActivity(Intent(this, VodActivity::class.java).apply { putExtra(VodActivity.EXTRA_TYPE, VodActivity.TYPE_SERIES) })
        }
        findViewById<View>(R.id.navBuscar).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<View>(R.id.navMas).setOnClickListener { showMoreMenu() }
        findViewById<View>(R.id.btnMoreMobile).setOnClickListener { showMoreMenu() }
    }

    private fun highlightMobileNav(activeId: Int) {
        val navIds = listOf(R.id.navInicio, R.id.navPeliculas, R.id.navSeries, R.id.navBuscar, R.id.navMas)
        navIds.forEach { id ->
            val layout = findViewById<android.widget.LinearLayout>(id)
            val icon = layout.getChildAt(0) as? android.widget.ImageView
            val label = layout.getChildAt(1) as? android.widget.TextView
            val active = id == activeId
            icon?.setColorFilter(if (active) getColor(R.color.primary) else getColor(R.color.text_secondary))
            label?.setTextColor(if (active) getColor(R.color.primary) else getColor(R.color.text_secondary))
        }
    }

    private fun showMoreMenu() {
        val options = arrayOf("👤 Mi Cuenta", "🔥 Adultos", "⭐ Favoritos", "🕒 Historial", "📑 Mis Listas", "🗑️ Borrar Caché", "🚪 Cerrar Sesión")
        android.app.AlertDialog.Builder(this)
            .setTitle("Más opciones")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, AccountActivity::class.java))
                    1 -> { mainFragment?.filterCategory("ADULTOS"); highlightMobileNav(R.id.navInicio) }
                    2 -> { mainFragment?.loadFavorites(); highlightMobileNav(R.id.navInicio) }
                    3 -> startActivity(Intent(this, HistorialActivity::class.java))
                    4 -> startActivity(Intent(this, MisListasActivity::class.java))
                    5 -> { cacheDir.deleteRecursively(); Toast.makeText(this, "Caché borrado", Toast.LENGTH_SHORT).show() }
                    6 -> {
                        Prefs.logout(this)
                        startActivity(Intent(this, LoginActivity::class.java))
                        finishAffinity()
                    }
                }
            }.show()
    }

    private fun setupSidebar() {
        binding.btnTv.setOnClickListener { selectItem(0); mainFragment?.filterCategory(null) }
        binding.btnPeliculas.setOnClickListener { selectItem(1); startActivity(Intent(this, VodActivity::class.java).apply { putExtra(VodActivity.EXTRA_TYPE, VodActivity.TYPE_MOVIES) }) }
        binding.btnSeries.setOnClickListener { selectItem(2); startActivity(Intent(this, VodActivity::class.java).apply { putExtra(VodActivity.EXTRA_TYPE, VodActivity.TYPE_SERIES) }) }
        binding.btnAdultos.setOnClickListener { selectItem(3); mainFragment?.filterCategory("ADULTOS") }
        binding.btnBuscar.setOnClickListener { startActivity(Intent(this, SearchActivity::class.java)) }
        binding.btnFavoritos.setOnClickListener { selectItem(5); mainFragment?.loadFavorites() }
        binding.btnClearCache.setOnClickListener {
            cacheDir.deleteRecursively()
            Toast.makeText(this, "Caché borrado", Toast.LENGTH_SHORT).show()
        }
        binding.btnLogout.setOnClickListener {
            Prefs.logout(this)
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
        binding.btnMiCuenta.setOnClickListener { startActivity(Intent(this, AccountActivity::class.java)) }
        selectItem(0)
    }

    private fun setupNavigation() {
        val items = listOf(binding.btnTv, binding.btnPeliculas, binding.btnSeries,
            binding.btnAdultos, binding.btnBuscar, binding.btnFavoritos,
            binding.btnClearCache, binding.btnLogout)

        items.forEach { btn ->
            btn.setOnFocusChangeListener { v, focused ->
                v.setBackgroundColor(if (focused) getColor(R.color.surface2) else android.graphics.Color.TRANSPARENT)
                val tv = (v as? android.view.ViewGroup)?.getChildAt(1) as? android.widget.TextView
                val iv = (v as? android.view.ViewGroup)?.getChildAt(0) as? android.widget.ImageView
                tv?.setTextColor(if (focused) getColor(R.color.primary) else getColor(R.color.text_primary))
                iv?.setColorFilter(if (focused) getColor(R.color.primary) else getColor(R.color.text_secondary))
                if (focused) v.animate().translationX(4f).setDuration(100).start()
                else v.animate().translationX(0f).setDuration(100).start()
            }
        }

        items.forEachIndexed { i, btn ->
            btn.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_DOWN -> { items.getOrNull(i+1)?.requestFocus(); true }
                        KeyEvent.KEYCODE_DPAD_UP -> { items.getOrNull(i-1)?.requestFocus() ?: binding.btnMiCuenta.requestFocus(); true }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> { binding.mainContainer.requestFocus(); true }
                        else -> false
                    }
                } else false
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isViewInSidebar(currentFocus)) {
                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("Salir").setMessage("¿Querés salir de Flux TV?")
                        .setPositiveButton("Salir") { _,_ -> finish() }
                        .setNegativeButton("Cancelar", null).show()
                } else {
                    binding.btnTv.requestFocus()
                }
            }
        })
    }

    private fun selectItem(idx: Int) {
        val items = listOf(binding.btnTv, binding.btnPeliculas, binding.btnSeries,
            binding.btnAdultos, binding.btnBuscar, binding.btnFavoritos)
        items.forEach { btn ->
            btn.setOnFocusChangeListener { v, focused ->
                v.setBackgroundColor(if (focused) getColor(R.color.surface2) else android.graphics.Color.TRANSPARENT)
                val tv = (v as? android.view.ViewGroup)?.getChildAt(1) as? android.widget.TextView
                val iv = (v as? android.view.ViewGroup)?.getChildAt(0) as? android.widget.ImageView
                tv?.setTextColor(if (focused) getColor(R.color.primary) else getColor(R.color.text_primary))
                iv?.setColorFilter(if (focused) getColor(R.color.primary) else getColor(R.color.text_secondary))
                if (focused) v.animate().translationX(4f).setDuration(100).start()
                else v.animate().translationX(0f).setDuration(100).start()
            }
        }

        items.forEachIndexed { i, btn ->
            val tv = btn.getChildAt(1) as? android.widget.TextView
            val iv = btn.getChildAt(0) as? android.widget.ImageView
            val active = i == idx
            tv?.setTextColor(if (active) getColor(R.color.primary) else getColor(R.color.text_primary))
            iv?.setColorFilter(if (active) getColor(R.color.primary) else getColor(R.color.text_secondary))
            btn.setBackgroundColor(if (active) getColor(R.color.surface2) else getColor(R.color.surface))
        }
        selectedItem = idx
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
        if (isMobile) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_DOWN &&
            event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            val focused = currentFocus
            if (!isViewInSidebar(focused)) {
                // Verificar si está en posición 0 de un RecyclerView
                val parent = focused?.parent
                if (parent is androidx.recyclerview.widget.RecyclerView) {
                    val pos = parent.getChildAdapterPosition(focused)
                    if (pos == 0 || pos == RecyclerView.NO_POSITION) return true
                } else {
                    // En Leanback headers u otros views, bloquear escape al sidebar
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun isViewInSidebar(view: View?): Boolean {
        if (view == null || isMobile) return false
        var parent = view.parent
        while (parent != null) {
            if (parent === binding.sidebar) return true
            parent = (parent as? View)?.parent
        }
        return false
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
