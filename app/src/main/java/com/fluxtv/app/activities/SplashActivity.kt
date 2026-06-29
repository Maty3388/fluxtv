package com.fluxtv.app.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fluxtv.app.services.ApiService
import com.fluxtv.app.BuildConfig
import com.fluxtv.app.utils.AutoUpdater
import com.fluxtv.app.utils.Prefs
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (com.fluxtv.app.utils.DeviceUtils.isTV(this)) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        setContentView(com.fluxtv.app.R.layout.activity_splash)

        val letter   = findViewById<android.widget.ImageView>(com.fluxtv.app.R.id.splashLetter)
        val title    = findViewById<android.widget.TextView>(com.fluxtv.app.R.id.splashTitle)
        val subtitle = findViewById<android.widget.TextView>(com.fluxtv.app.R.id.splashSubtitle)
        val progress = findViewById<android.widget.ProgressBar>(com.fluxtv.app.R.id.splashProgress)

        letter.animate().alpha(1f).setDuration(200).withEndAction {
            val avd = letter.drawable as? android.graphics.drawable.AnimatedVectorDrawable
            avd?.start()
        }.start()
        title.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(900).start()
        subtitle.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(1100).start()
        progress.animate().alpha(1f).setDuration(300).setStartDelay(1300).start()

        scope.launch {
            try {
            delay(1200)
            val token = Prefs.getToken(this@SplashActivity)
            if (token.isNotEmpty()) {
                ApiService.token = token
                try {
                    val ver = withContext(Dispatchers.IO) { ApiService.getVersion() }
                    if (ver != null) AutoUpdater.check(this@SplashActivity, BuildConfig.VERSION_NAME, ver)
                } catch (_: Exception) {}
                // Chequear vencimiento proximo
                val days = Prefs.getDaysLeft(this@SplashActivity)
                if (days in 0..7) {
                    runOnUiThread {
                        androidx.appcompat.app.AlertDialog.Builder(this@SplashActivity)
                            .setTitle("⚠️ Suscripción próxima a vencer")
                            .setMessage("Tu suscripción vence en $days día(s). Contactá con soporte para renovar.")
                            .setPositiveButton("Entendido") { d, _ -> d.dismiss() }
                            .setCancelable(true)
                            .show()
                    }
                }
                if (Prefs.isProfileSelected(this@SplashActivity))
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                else
                    startActivity(Intent(this@SplashActivity, SelectProfileActivity::class.java))
            } else {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            finish()
            } catch (e: Exception) {
                android.util.Log.e("FluxTV", "Splash crash: ${e.message}", e)
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                finish()
            }
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
