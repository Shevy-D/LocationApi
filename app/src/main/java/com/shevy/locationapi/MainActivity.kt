package com.shevy.locationapi

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.shevy.locationapi.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var startLocationUpdatesButton: Button
    private lateinit var stopLocationUpdatesButton: Button
    private lateinit var locationTextView: TextView
    private lateinit var locationUpdateTimeTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startLocationUpdatesButton = binding.startLocationUpdatesButton
        stopLocationUpdatesButton = binding.stopLocationUpdatesButton
    }
}