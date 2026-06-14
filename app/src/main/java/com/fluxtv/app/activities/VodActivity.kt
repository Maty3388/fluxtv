package com.fluxtv.app.activities

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.fluxtv.app.R
import com.fluxtv.app.databinding.ActivityVodBinding
import com.fluxtv.app.fragments.VodFragment

class VodActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVodBinding

    companion object {
        const val TYPE_MOVIES = "movies"
        const val TYPE_SERIES = "series"
        const val EXTRA_TYPE = "type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVodBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_MOVIES
        binding.tvTitle.text = if (type == TYPE_MOVIES) "🎬 Películas" else "📺 Series"
        binding.btnBack.setOnClickListener { finish() }

        val fragment = VodFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.rvVod, fragment)
            .commit()
        fragment.load(type)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }
}
