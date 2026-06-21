package com.fluxtv.app.activities

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.fluxtv.app.R
import com.fluxtv.app.databinding.ActivityVodBinding
import com.facebook.shimmer.ShimmerFrameLayout
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
        fragment.onLoaded = {
            binding.shimmerVod.stopShimmer()
            binding.shimmerVod.visibility = android.view.View.GONE
        }
        binding.shimmerVod.startShimmer()
        fragment.onCategoriesLoaded = { categories ->
            runOnUiThread {
                binding.filterContainer.removeAllViews()
                categories.forEach { (cat, rowIndex) ->
                    val btn = android.widget.Button(this).apply {
                        text = cat
                        textSize = 11f
                        setTextColor(getColor(R.color.text_secondary))
                        setBackgroundColor(getColor(R.color.surface2))
                        isFocusable = true
                        setPadding(20, 0, 20, 0)
                        val params = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.marginEnd = 8
                        layoutParams = params
                        setOnClickListener { fragment.setSelectedPosition(rowIndex, true) }
                        setOnFocusChangeListener { v, focused ->
                            setTextColor(if (focused) getColor(R.color.background) else getColor(R.color.text_secondary))
                            setBackgroundColor(if (focused) getColor(R.color.primary) else getColor(R.color.surface2))
                        }
                    }
                    binding.filterContainer.addView(btn)
                }
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.rvVod, fragment)
            .runOnCommit { fragment.load(type) }
            .commit()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }
}
