package com.shevy.locationapi

import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.shevy.locationapi.databinding.ActivityMainBinding
import java.text.DateFormat
import java.util.*
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var startLocationUpdatesButton: Button
    private lateinit var stopLocationUpdatesButton: Button
    private lateinit var locationTextView: TextView
    private lateinit var locationUpdateTimeTextView: TextView

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var settingsClient: SettingsClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationSettingsRequest: LocationSettingsRequest
    private lateinit var locationCallBack: LocationCallback
    private lateinit var currentLocation: Location

    private var isLocationUpdatesActive by Delegates.notNull<Boolean>()
    private lateinit var locationUpdateTime: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startLocationUpdatesButton = binding.startLocationUpdatesButton
        stopLocationUpdatesButton = binding.stopLocationUpdatesButton
        locationTextView = binding.locationTextView

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)

        buildLocationRequest()
        buildLocationCallBack()
        buildLocationSettingsRequest()
    }

    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        locationSettingsRequest = builder.build()
    }

    private fun buildLocationCallBack() {
        locationCallBack = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                currentLocation = p0.lastLocation!!

                locationTextView.text = "${currentLocation.latitude}/${currentLocation.longitude}"
                locationUpdateTimeTextView.text = "${DateFormat.getDateInstance().format(Date())}"
            }
        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 3000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
}