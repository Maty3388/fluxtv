package com.fluxtv.app.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.fluxtv.app.databinding.ActivitySelectProfileBinding
import com.fluxtv.app.utils.Prefs

class SelectProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySelectProfileBinding
    private val profiles = listOf(
        Pair("😜", "Perfil 1"),
        Pair("🙂", "Perfil 2"),
        Pair("😎", "Perfil 3")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dp = resources.displayMetrics.density

        profiles.forEachIndexed { idx, (avatar, name) ->
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
                text = name; textSize = 14f
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

            card.setOnClickListener {
                Prefs.saveProfileSelected(this)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }

            card.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                    Prefs.saveProfileSelected(this)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish(); true
                } else false
            }

            binding.profilesContainer.addView(card)
            if (idx == 0) card.requestFocus()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }
}
