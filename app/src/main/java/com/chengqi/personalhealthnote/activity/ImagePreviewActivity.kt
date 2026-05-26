package com.chengqi.personalhealthnote.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.chengqi.personalhealthnote.adapter.ImagePreviewAdapter
import com.chengqi.personalhealthnote.databinding.ActivityImagePreviewBinding

class ImagePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagePreviewBinding
    private var isBarVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imagePaths = intent.getStringArrayListExtra("image_paths") ?: arrayListOf()
        val currentPosition = intent.getIntExtra("current_position", 0)

        if (imagePaths.isEmpty()) {
            finish()
            return
        }

        val adapter = ImagePreviewAdapter(imagePaths)
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(currentPosition, false)

        updateCount(currentPosition, imagePaths.size)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateCount(position, imagePaths.size)
            }
        })

        binding.ivBack.setOnClickListener { finish() }

        binding.rootLayout.setOnClickListener {
            isBarVisible = !isBarVisible
            binding.layoutTopBar.visibility = if (isBarVisible) View.VISIBLE else View.GONE
        }
    }

    private fun updateCount(current: Int, total: Int) {
        binding.tvImageCount.text = "${current + 1}/$total"
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.viewPager.adapter = null
    }
}
