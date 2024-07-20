import android.net.Uri
import com.arthenica.mobileffmpeg.ExecuteCallback
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File

/*
package com.example.mp4tomp3converter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mp4tomp3converter.databinding.ActivityRecentConversionsBinding

class RecentConversionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecentConversionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecentConversionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recentConversions = getRecentConversions()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, recentConversions)
        binding.recentConversionsList.adapter = adapter
    }

    private fun getRecentConversions(): List<String> {
        val prefs = getSharedPreferences("recent_conversions", Context.MODE_PRIVATE)
        val recentConversionsSet = prefs.getStringSet("conversions", setOf())
        return recentConversionsSet?.toList() ?: emptyList()
    }
}

*/


