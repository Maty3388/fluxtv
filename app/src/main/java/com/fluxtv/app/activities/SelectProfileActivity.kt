package com.fluxtv.app.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.fluxtv.app.databinding.ActivitySelectProfileBinding
import com.fluxtv.app.services.ApiService
import com.fluxtv.app.utils.Prefs

class SelectProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySelectProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ApiService.token = Prefs.getToken(this)
        loadProfiles()
    }

    private fun loadProfiles() {
        Thread {
            try {
                val data = ApiService.getProfile()
                val profiles = data.getJSONArray("profiles")
                runOnUiThread { renderProfiles(profiles) }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error cargando perfiles", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun renderProfiles(profiles: org.json.JSONArray) {
        binding.profilesContainer.removeAllViews()
        val dp = resources.displayMetrics.density
        val avatars = listOf("😜", "🙂", "😎", "🎮", "🌟")

        for (i in 0 until profiles.length()) {
            val profile = profiles.getJSONObject(i)
            val profileId = profile.getInt("id")
            val profileName = profile.optString("name", "Perfil ${i + 1}")
            val avatar = avatars.getOrElse(i) { "👤" }

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                setPadding((24*dp).toInt(), (24*dp).toInt(), (24*dp).toInt(), (24*dp).toInt())
                isFocusable = true; isFocusableInTouchMode = true
                layoutParams = LinearLayout.LayoutParams((160*dp).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
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
        Thread {
            try {
                val res = ApiService.selectProfile(profileId, deviceId)
                val success = res.optBoolean("success", false)
                val locked = res.optBoolean("profile_locked", false)
                runOnUiThread {
                    when {
                        success -> {
                            Prefs.saveProfileSelected(this)
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        locked -> {
                            Toast.makeText(this, "Este perfil esta en uso en otro dispositivo", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(this, "Error al seleccionar perfil", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error de conexion", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }
}
