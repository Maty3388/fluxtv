package com.fluxtv.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.fluxtv.app.BuildConfig
import com.fluxtv.app.R
import com.fluxtv.app.databinding.ActivityMainBinding
import com.fluxtv.app.fragments.MainFragment
import com.fluxtv.app.services.ApiService
import com.fluxtv.app.utils.AutoUpdater
import com.fluxtv.app.utils.Prefs
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var mainFragment: MainFragment? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var selectedItem = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainFragment = MainFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContainer, mainFragment!!)
            .commit()

        // Mostrar datos usuario
        binding.tvUserEmail.text = Prefs.getEmail(this)
        val subEnd = Prefs.getSubEnd(this)
        if (subEnd.isNotEmpty()) binding.tvVencimiento.text = subEnd

        setupSidebar()
        setupNavigation()
        checkUpdate()
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
        binding.btnMiCuenta.setOnClickListener { selectItem(-1) }
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
                binding.btnTv.requestFocus()
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
        if (view == null) return false
        var parent = view.parent
        while (parent != null) {
            if (parent === binding.sidebar) return true
            parent = (parent as? View)?.parent
        }
        return false
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
