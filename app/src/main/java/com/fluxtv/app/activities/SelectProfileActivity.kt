package com.fluxtv.app.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.fluxtv.app.databinding.ActivitySelectProfileBinding
import com.fluxtv.app.services.ApiService
import com.fluxtv.app.utils.Prefs
import kotlinx.coroutines.*

class SelectProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySelectProfileBinding
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ApiService.token = Prefs.getToken(this)
        loadProfiles()
    }

    private fun loadProfiles() {
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.profilesContainer.visibility = View.GONE
        binding.navGuide.visibility = View.GONE
        scope.launch {
            try {
                val data = withContext(Dispatchers.IO) { ApiService.getProfile() }
                val profiles = data.getJSONArray("profiles")
                binding.loadingIndicator.visibility = View.GONE
                binding.profilesContainer.visibility = View.VISIBLE
                binding.navGuide.visibility = View.VISIBLE
                renderProfiles(profiles)
            } catch (e: Exception) {
                binding.loadingIndicator.visibility = View.GONE
                Toast.makeText(this@SelectProfileActivity, "Error cargando perfiles", Toast.LENGTH_SHORT).show()
                Prefs.saveProfileSelected(this@SelectProfileActivity)
                startActivity(android.content.Intent(this@SelectProfileActivity, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun renderProfiles(profiles: org.json.JSONArray) {
        binding.profilesContainer.removeAllViews()
        val dp = resources.displayMetrics.density
        for (i in 0 until profiles.length()) {
            val profile = profiles.getJSONObject(i)
            val profileId = profile.getInt("id")
            val profileName = profile.optString("name", "Perfil ${i + 1}")
            val avatar = profile.optString("avatar", "👤")

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                setPadding((24*dp).toInt(), (24*dp).toInt(), (24*dp).toInt(), (24*dp).toInt())
                isFocusable = true; isFocusableInTouchMode = true
                layoutParams = LinearLayout.LayoutParams((120*dp).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = (24*dp).toInt()
                }
                setBackgroundColor(Color.parseColor("#060E1A"))
            }

            val tvAvatar = TextView(this).apply {
                text = avatar; textSize = 48f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            val tvName = TextView(this).apply {
                text = profileName; textSize = 14f
                setTextColor(Color.WHITE)
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (8*dp).toInt()
                }
            }

            card.addView(tvAvatar)
            card.addView(tvName)

            card.setOnFocusChangeListener { v, focused ->
                v.setBackgroundColor(if (focused) Color.parseColor("#001A2E") else Color.parseColor("#060E1A"))
                tvName.setTextColor(if (focused) Color.parseColor("#00E5FF") else Color.WHITE)
                if (focused) v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(120).start()
                else v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }

            val onClick = { onProfileSelected(profileId) }

            card.setOnClickListener { onClick() }
            card.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                    onClick(); true
                } else false
            }

            binding.profilesContainer.addView(card)
            if (i == 0) card.requestFocus()
        }
    }

    private fun onProfileSelected(profileId: Int) {
        val deviceId = Prefs.getDeviceId(this)
        scope.launch {
            try {
                val res = withContext(Dispatchers.IO) { ApiService.selectProfile(profileId, deviceId) }
                val success = res.optBoolean("success", false)
                val locked = res.optBoolean("profile_locked", false)
                when {
                    success -> {
                        Prefs.saveProfileSelected(this@SelectProfileActivity)
                        startActivity(Intent(this@SelectProfileActivity, MainActivity::class.java))
                        finish()
                    }
                    locked -> Toast.makeText(this@SelectProfileActivity, "Este perfil esta en uso en otro dispositivo", Toast.LENGTH_LONG).show()
                    else -> Toast.makeText(this@SelectProfileActivity, "Error al seleccionar perfil", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SelectProfileActivity, "Error de conexion", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }
    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
