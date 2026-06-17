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
        super.onCreate(savedInstanceState)
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
