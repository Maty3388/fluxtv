package com.fluxtv.app.activities

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.fluxtv.app.databinding.ActivityAccountBinding
import com.fluxtv.app.utils.Prefs

class AccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvEmail.text = Prefs.getEmail(this)
        binding.tvSubEnd.text = Prefs.getSubEnd(this).ifEmpty { "Sin datos" }

        binding.btnClose.setOnClickListener { finish() }
        binding.btnLogout.setOnClickListener {
            Prefs.logout(this)
            startActivity(android.content.Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
        binding.btnClose.requestFocus()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }
}
