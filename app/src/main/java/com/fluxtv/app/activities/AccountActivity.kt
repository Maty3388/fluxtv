package com.fluxtv.app.activities

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.fluxtv.app.databinding.ActivityAccountBinding
import com.fluxtv.app.services.ApiService
import com.fluxtv.app.utils.Prefs

class AccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvEmail.text = Prefs.getEmail(this)
        binding.tvSubEnd.text = Prefs.getSubEnd(this).ifEmpty { "Sin datos" }

        binding.btnPin.setOnClickListener { showPinDialog() }
        binding.btnClose.setOnClickListener { finish() }
        binding.btnLogout.setOnClickListener {
            Prefs.logout(this)
            startActivity(android.content.Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
        binding.btnClose.requestFocus()
    }

    private fun showPinDialog() {
        val dp = resources.displayMetrics.density
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24*dp).toInt(), (24*dp).toInt(), (24*dp).toInt(), (8*dp).toInt())
        }
        val tvInfo = TextView(this).apply {
            text = "Ingresá un PIN de 4 dígitos para proteger el contenido adulto"
            textSize = 13f
            setTextColor(android.graphics.Color.LTGRAY)
            setPadding(0, 0, 0, (16*dp).toInt())
        }
        val input = EditText(this).apply {
            hint = "Nuevo PIN (4 dígitos)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = android.view.Gravity.CENTER
            textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.GRAY)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFF1A1A2E.toInt())
                cornerRadius = 8*dp
                setStroke(1, 0xFF00E5FF.toInt())
            }
            setPadding((16*dp).toInt(), (12*dp).toInt(), (16*dp).toInt(), (12*dp).toInt())
        }
        val tvError = TextView(this).apply {
            text = ""
            textSize = 12f
            setTextColor(android.graphics.Color.RED)
            gravity = android.view.Gravity.CENTER
            setPadding(0, (8*dp).toInt(), 0, 0)
        }
        layout.addView(tvInfo)
        layout.addView(input)
        layout.addView(tvError)

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("🔒 Configurar PIN Parental")
            .setView(layout)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0xFF0A1825.toInt()))
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = input.text.toString()
                if (pin.length < 4) { tvError.text = "El PIN debe tener al menos 4 dígitos"; return@setOnClickListener }
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
                Thread {
                    try {
                        val json = ApiService.setParentalPin(pin)
                        runOnUiThread {
                            if (json.optBoolean("success", false)) {
                                dialog.dismiss()
                                Toast.makeText(this, "PIN configurado correctamente", Toast.LENGTH_SHORT).show()
                            } else {
                                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                                tvError.text = json.optString("error", "Error al guardar")
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            tvError.text = "Error de conexión"
                        }
                    }
                }.start()
            }
        }
        dialog.show()
        input.requestFocus()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }
}
