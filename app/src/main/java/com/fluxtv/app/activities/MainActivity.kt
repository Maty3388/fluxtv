package com.fluxtv.app.activities

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.fluxtv.app.BuildConfig
import com.fluxtv.app.R
import com.fluxtv.app.fragments.MainFragment
import com.fluxtv.app.fragments.TvMainFragment
import com.fluxtv.app.utils.Prefs
import com.fluxtv.app.services.ApiService
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var mainFragment: MainFragment
    private var tvFragment: TvMainFragment? = null
    private lateinit var tabsContainer: LinearLayout
    private lateinit var categoriesContainer: LinearLayout
    private var currentTab = 0
    private val scope = CoroutineScope(Dispatchers.Main)

    // Tabs: 0=Television, 1=Películas, 2=Series
    private val tabs = listOf("Televisión", "Películas", "Series")
    private var allCategories = listOf<String>()
    private var selectedCategory = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isTV = com.fluxtv.app.utils.DeviceUtils.isTV(this)
        if (isTV) {
            setContentView(R.layout.activity_main_tv)
            setupTV()
            return
        }
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main_mobile)

        // Setup info usuario
        val email = Prefs.getEmail(this)
        val exp = Prefs.getSubEnd(this)
        findViewById<TextView>(R.id.tvUserEmail)?.text = "👤 $email"
        findViewById<TextView>(R.id.tvVencimiento)?.text = "Vence: $exp"

        tabsContainer = findViewById(R.id.tabsContainer)
        categoriesContainer = findViewById(R.id.categoriesContainer)

        // Setup fragment
        mainFragment = MainFragment()
        mainFragment.onChannelsLoaded = { buildCategories() }
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContainer, mainFragment)
            .commit()

        setupTabs()
        setupBottomNav()
        setupSearch()
        // Check update
        scope.launch {
            val ver = withContext(Dispatchers.IO) { try { com.fluxtv.app.services.ApiService.getVersion() } catch(_:Exception) { null } }
            if (ver != null) com.fluxtv.app.utils.AutoUpdater.check(this@MainActivity, BuildConfig.VERSION_NAME, ver)
        }

        // Refresh button
        findViewById<ImageView>(R.id.btnRefresh)?.setOnClickListener {
            mainFragment.loadChannels()
        }
    }

    private fun setupTabs() {
        tabsContainer.removeAllViews()
        tabs.forEachIndexed { i, label ->
            val tab = TextView(this).apply {
                text = label
                textSize = 13f
                setPadding(32, 0, 32, 0)
                gravity = Gravity.CENTER
                typeface = if (i == currentTab) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setTextColor(if (i == currentTab) getColor(R.color.primary) else getColor(R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
                if (i == currentTab) {
                    setBackgroundResource(android.R.color.transparent)
                    // Underline via compound drawable
                }
                setOnClickListener { selectTab(i) }
            }
            tabsContainer.addView(tab)

            // Separador
            if (i < tabs.size - 1) {
                val sep = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                        setMargins(0, 8, 0, 8)
                    }
                    setBackgroundColor(getColor(R.color.border))
                }
                tabsContainer.addView(sep)
            }
        }
    }

    private fun selectTab(i: Int) {
        currentTab = i
        setupTabs()
        selectedCategory = "All"
        when (i) {
            0 -> { // Televisión
                mainFragment.filterCategory(null)
                buildCategories()
                findViewById<HorizontalScrollView>(R.id.categoriesScroll)?.visibility = android.view.View.VISIBLE
            }
            1 -> { // Películas
                startActivity(Intent(this, VodActivity::class.java).apply { putExtra("type", "movies") })
                currentTab = 0; setupTabs()
            }
            2 -> { // Series
                startActivity(Intent(this, VodActivity::class.java).apply { putExtra("type", "series") })
                currentTab = 0; setupTabs()
            }
        }
    }

    private fun buildCategories() {
        val cats = mainFragment.getCategories()
        allCategories = cats
        categoriesContainer.removeAllViews()
        val allList = listOf("All") + cats
        allList.forEach { cat ->
            val chip = TextView(this).apply {
                text = if (cat == "All") "✓ All" else cat
                textSize = 11f
                setPadding(24, 6, 24, 6)
                val sel = cat == selectedCategory
                setTextColor(if (sel) getColor(R.color.background) else getColor(R.color.text_secondary))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 40f
                    setColor(if (sel) getColor(R.color.primary) else getColor(R.color.surface))
                    if (!sel) setStroke(1, getColor(R.color.border))
                }
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(4, 4, 4, 4)
                }
                layoutParams = lp
                setOnClickListener {
                    selectedCategory = cat
                    buildCategories()
                    if (cat == "All") mainFragment.filterCategory(null)
                    else mainFragment.filterCategory(cat)
                }
            }
            categoriesContainer.addView(chip)
        }
    }

    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.navInicio)?.setOnClickListener {
            highlightMobileNav(R.id.navInicio)
            mainFragment.filterCategory(null)
            selectedCategory = "All"
            buildCategories()
            currentTab = 0; setupTabs()
        }
        findViewById<LinearLayout>(R.id.navMisListas)?.setOnClickListener {
            highlightMobileNav(R.id.navMisListas)
            startActivity(Intent(this, MisListasActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navExplorar)?.setOnClickListener {
            highlightMobileNav(R.id.navExplorar)
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navCuenta)?.setOnClickListener {
            highlightMobileNav(R.id.navCuenta)
            showAccountMenu()
        }
    }

    fun highlightMobileNav(activeId: Int) {
        val navIds = listOf(R.id.navInicio, R.id.navMisListas, R.id.navExplorar, R.id.navCuenta)
        navIds.forEach { id ->
            val nav = findViewById<LinearLayout>(id) ?: return@forEach
            val active = id == activeId
            val iv = nav.getChildAt(0) as? ImageView
            val tv = nav.getChildAt(1) as? TextView
            iv?.setColorFilter(getColor(if (active) R.color.primary else R.color.text_secondary))
            tv?.setTextColor(getColor(if (active) R.color.primary else R.color.text_secondary))
        }
    }

    private fun setupSearch() {
        findViewById<ImageView>(R.id.btnBuscar)?.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
    }

    private fun showAccountMenu() {
        val options = arrayOf("👤 Mi Cuenta", "🔥 Adultos", "⭐ Favoritos", "🕒 Historial", "🗑️ Borrar Caché", "🚪 Cerrar Sesión")
        android.app.AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, AccountActivity::class.java))
                    1 -> showPinDialog { mainFragment.filterCategory("ADULTOS"); highlightMobileNav(R.id.navInicio) }
                    2 -> { mainFragment.loadFavorites(); highlightMobileNav(R.id.navInicio) }
                    3 -> startActivity(Intent(this, HistorialActivity::class.java))
                    4 -> { Toast.makeText(this, "Caché borrado", Toast.LENGTH_SHORT).show() }
                    5 -> { Prefs.saveToken(this, ""); Prefs.clearProfileSelected(this); startActivity(Intent(this, LoginActivity::class.java)); finish() }
                }
            }.show()
    }

    private fun showPinDialog(onSuccess: () -> Unit) {
        val ctx = this
        val dialog = android.app.Dialog(ctx, android.R.style.Theme_Material_Dialog)
        dialog.setContentView(android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 48, 48, 32)
            setBackgroundColor(getColor(R.color.surface))
            val title = android.widget.TextView(ctx).apply {
                text = "🔒 Control Parental"; textSize = 16f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(getColor(R.color.text_primary)); gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 24 }
            }
            val input = android.widget.EditText(ctx).apply {
                hint = "PIN de 4 dígitos"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                setTextColor(getColor(R.color.text_primary)); setHintTextColor(getColor(R.color.text_hint))
                textSize = 18f; gravity = Gravity.CENTER; maxLines = 1
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 8 }
            }
            val tvError = android.widget.TextView(ctx).apply {
                setTextColor(getColor(R.color.error)); textSize = 12f; visibility = android.view.View.GONE
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 }
            }
            val btnOk = android.widget.Button(ctx).apply {
                text = "VERIFICAR"
                setBackgroundColor(getColor(R.color.primary))
                setTextColor(getColor(R.color.background))
                setOnClickListener {
                    val pin = input.text.toString()
                    if (pin.length < 4) { tvError.text = "El PIN debe tener al menos 4 dígitos"; tvError.visibility = android.view.View.VISIBLE; return@setOnClickListener }
                    scope.launch {
                        val res = withContext(Dispatchers.IO) { ApiService.verifyParentalPin(pin) }
                        if (res.optBoolean("success")) { dialog.dismiss(); onSuccess() }
                        else { tvError.text = res.optString("error", "PIN incorrecto"); tvError.visibility = android.view.View.VISIBLE }
                    }
                }
            }
            addView(title); addView(input); addView(tvError); addView(btnOk)
        })
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun setupTV() {
        // Layout TV: usar sidebar original
        val email = com.fluxtv.app.utils.Prefs.getEmail(this)
        val exp = com.fluxtv.app.utils.Prefs.getSubEnd(this)
        findViewById<android.widget.TextView>(R.id.tvUserEmail)?.text = "👤 $email"
        findViewById<android.widget.TextView>(R.id.tvVencimiento)?.text = exp

        tvFragment = TvMainFragment()
        supportFragmentManager.beginTransaction()
        tvFragment!!.onChannelsLoaded = { runOnUiThread { findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerLayout)?.stopShimmer(); findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerLayout)?.visibility = android.view.View.GONE } }
            .replace(R.id.mainContainer, tvFragment!!)
            .commit()

        // Botones sidebar TV
        fun highlightSidebar(activeId: Int) {
            val ids = listOf(R.id.btnMiCuenta, R.id.btnTv, R.id.btnPeliculas, R.id.btnSeries, R.id.btnAdultos, R.id.btnBuscar, R.id.btnFavoritos)
            ids.forEach { id ->
                val v = findViewById<android.widget.FrameLayout>(id) ?: return@forEach
                val active = id == activeId
                // Fondo con borde cyan si activo
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 10 * resources.displayMetrics.density
                    setColor(if (active) 0x1500E5FF.toInt() else android.graphics.Color.TRANSPARENT)
                    if (active) setStroke((1 * resources.displayMetrics.density).toInt(), 0x4400E5FF.toInt())
                }
                v.background = bg
                // Icono cyan si activo
                val iv = v.getChildAt(0) as? android.widget.ImageView
                iv?.setColorFilter(if (active) getColor(R.color.primary) else getColor(R.color.text_secondary))
            }
        }
        // Focus listeners para highlight con D-pad
        val sidebarBtns = listOf(R.id.btnMiCuenta, R.id.btnTv, R.id.btnPeliculas, R.id.btnSeries, R.id.btnAdultos, R.id.btnBuscar, R.id.btnFavoritos)
        sidebarBtns.forEach { id ->
            findViewById<android.widget.FrameLayout>(id)?.setOnFocusChangeListener { _, focused ->
                if (focused) highlightSidebar(id)
            }
            findViewById<android.widget.FrameLayout>(id)?.setOnKeyListener { _, keyCode, event ->
                if (event.action == android.view.KeyEvent.ACTION_DOWN && keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                    val rv = tvFragment?.view?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvCategories)
                    rv?.requestFocus() ?: findViewById<android.widget.FrameLayout>(R.id.mainContainer)?.requestFocus()
                    return@setOnKeyListener true
                }
                false
            }
        }

        highlightSidebar(R.id.btnTv)
        findViewById<android.widget.FrameLayout>(R.id.btnTv)?.setOnClickListener { tvFragment?.filterCategory(null); highlightSidebar(R.id.btnTv) }
        findViewById<android.widget.FrameLayout>(R.id.btnPeliculas)?.setOnClickListener { highlightSidebar(R.id.btnPeliculas); startActivity(android.content.Intent(this, VodActivity::class.java).apply { putExtra("type", "movies") }) }
        findViewById<android.widget.FrameLayout>(R.id.btnSeries)?.setOnClickListener { highlightSidebar(R.id.btnSeries); startActivity(android.content.Intent(this, VodActivity::class.java).apply { putExtra("type", "series") }) }
        findViewById<android.widget.FrameLayout>(R.id.btnAdultos)?.setOnClickListener { highlightSidebar(R.id.btnAdultos); showPinDialog { tvFragment?.filterCategory("ADULTOS") } }
        findViewById<android.widget.FrameLayout>(R.id.btnBuscar)?.setOnClickListener { highlightSidebar(R.id.btnBuscar); startActivity(android.content.Intent(this, SearchActivity::class.java)) }
        findViewById<android.widget.FrameLayout>(R.id.btnFavoritos)?.setOnClickListener { highlightSidebar(R.id.btnFavoritos); tvFragment?.loadFavorites() }
        findViewById<android.widget.FrameLayout>(R.id.btnMiCuenta)?.setOnClickListener { highlightSidebar(R.id.btnMiCuenta); startActivity(android.content.Intent(this, AccountActivity::class.java)) }
        findViewById<android.widget.FrameLayout>(R.id.btnLogout)?.setOnClickListener { com.fluxtv.app.utils.Prefs.saveToken(this, ""); com.fluxtv.app.utils.Prefs.clearProfileSelected(this); startActivity(android.content.Intent(this, LoginActivity::class.java)); finish() }
        findViewById<android.widget.FrameLayout>(R.id.btnClearCache)?.setOnClickListener { android.widget.Toast.makeText(this, "Caché borrado", android.widget.Toast.LENGTH_SHORT).show() }
        // Check update
        scope.launch {
            val ver = withContext(Dispatchers.IO) { try { com.fluxtv.app.services.ApiService.getVersion() } catch(_:Exception) { null } }
            if (ver != null) com.fluxtv.app.utils.AutoUpdater.check(this@MainActivity, BuildConfig.VERSION_NAME, ver)
        }
    }

    override fun onBackPressed() {
        val isTV = com.fluxtv.app.utils.DeviceUtils.isTV(this)
        if (isTV) {
            val sidebarIds = listOf(R.id.btnTv, R.id.btnMiCuenta, R.id.btnPeliculas, R.id.btnSeries, R.id.btnBuscar, R.id.btnFavoritos, R.id.btnAdultos, R.id.btnClearCache, R.id.btnLogout)
            val focus = currentFocus
            val inSidebar = focus != null && sidebarIds.any { findViewById<android.view.View>(it) == focus }
            if (inSidebar) {
                // En sidebar → salir de la app
                finishAffinity()
                return
            }
            // En contenido → volver al sidebar
            findViewById<android.widget.FrameLayout>(R.id.btnTv)?.requestFocus()
            return
        }
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        highlightMobileNav(R.id.navInicio)
        if (com.fluxtv.app.utils.SecurityUtils.isVpnActive(this)) {
            com.fluxtv.app.utils.SecurityUtils.showVpnDialog(this) {
                Prefs.saveToken(this, "")
                startActivity(android.content.Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }
    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
