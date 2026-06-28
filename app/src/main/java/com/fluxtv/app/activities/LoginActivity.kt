package com.fluxtv.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.fluxtv.app.databinding.ActivityLoginBinding
import com.fluxtv.app.services.ApiService
import com.fluxtv.app.utils.Prefs
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Verificar VPN
        if (com.fluxtv.app.utils.SecurityUtils.isVpnActive(this)) {
            com.fluxtv.app.utils.SecurityUtils.showVpnDialog(this) { finish() }
            return
        }
        binding.btnLogin.setOnClickListener { doLogin() }
    }

    private fun doLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        if (email.isEmpty() || password.isEmpty()) {
            binding.tvError.text = "Completá todos los campos"
            binding.tvError.visibility = View.VISIBLE
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tvError.text = "Ingresá un email válido"
            binding.tvError.visibility = View.VISIBLE
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        binding.tvError.visibility = View.GONE
        scope.launch {
            val token = withContext(Dispatchers.IO) {
                try { ApiService.login(email, password, com.fluxtv.app.utils.SecurityUtils.getDeviceId(this@LoginActivity)) } catch (_: Exception) { null }
            }
            if (isDestroyed) return@launch
            binding.progressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true
            if (token != null) {
                ApiService.token = token
                Prefs.saveToken(this@LoginActivity, token)
                Prefs.saveEmail(this@LoginActivity, email)
                Prefs.saveSubEnd(this@LoginActivity, ApiService.subEnd)
                startActivity(Intent(this@LoginActivity, SelectProfileActivity::class.java))
                finish()
            } else {
                binding.tvError.text = when {
                    ApiService.loginError.contains("vencida", ignoreCase = true) -> "Tu acceso de prueba ha vencido"
                    ApiService.loginError.contains("bloqueado", ignoreCase = true) || ApiService.loginError.contains("blocked", ignoreCase = true) -> "Tu cuenta ha sido bloqueada"
                    else -> "Credenciales incorrectas"
                }
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
