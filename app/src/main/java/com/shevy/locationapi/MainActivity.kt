package com.shevy.locationapi

import android.Manifest
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.shevy.locationapi.databinding.ActivityMainBinding
import java.text.DateFormat
import java.util.*
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() {
    private val CHECK_SETTINGS_CODE = 111
    private val REQUEST_LOCATION_PERMISSION = 222

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

        startLocationUpdatesButton.setOnClickListener {
            startLocationUpdates()
        }

        buildLocationRequest()
        buildLocationCallBack()
        buildLocationSettingsRequest()
    }

    private fun startLocationUpdates() {
        isLocationUpdatesActive = true
        startLocationUpdatesButton.isEnabled = false
        stopLocationUpdatesButton.isEnabled = true

        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener(this) { locationSettingsResponse: LocationSettingsResponse? ->
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@addOnSuccessListener
            }
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallBack,
                Looper.myLooper()
            )
            updateLocationUi()
        }.addOnFailureListener { e: Exception ->
            val statusCode = (e as ApiException).statusCode
            when (statusCode) {
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                    val resolvableApiException = e as ResolvableApiException
                    resolvableApiException.startResolutionForResult(
                        this@MainActivity,
                        CHECK_SETTINGS_CODE
                    )
                } catch (sie: SendIntentException) {
                    sie.printStackTrace()
                }
                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                    val message = "Adjust location settings on your device"
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                    isLocationUpdatesActive = false
                    startLocationUpdatesButton.isEnabled = true
                    stopLocationUpdatesButton.isEnabled = false
                }
            }
            updateLocationUi()
        }
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

                updateLocationUi()
            }
        }
    }

    private fun updateLocationUi() {
        locationTextView.text = "${currentLocation.latitude}/${currentLocation.longitude}"
        locationUpdateTimeTextView.text = "${DateFormat.getDateInstance().format(Date())}"
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 3000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
}