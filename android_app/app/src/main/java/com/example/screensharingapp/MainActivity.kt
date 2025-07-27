package com.example.screensharingapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.screensharingapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.broadcasterButton.setOnClickListener {
            startActivity(Intent(this, BroadcasterActivity::class.java))
        }

        binding.viewerButton.setOnClickListener {
            startActivity(Intent(this, ViewerActivity::class.java))
        }
    }
}

