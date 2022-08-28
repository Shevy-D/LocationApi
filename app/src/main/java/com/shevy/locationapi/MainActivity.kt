package com.shevy.locationapi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.shevy.locationapi.databinding.ActivityMainBinding
import java.text.DateFormat
import java.util.*


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
    private var currentLocation: Location? = null

    private var isLocationUpdatesActive = false
    private lateinit var locationUpdateTime: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startLocationUpdatesButton = binding.startLocationUpdatesButton
        stopLocationUpdatesButton = binding.stopLocationUpdatesButton
        locationTextView = binding.locationTextView
        locationUpdateTimeTextView = binding.locationUpdateTimeTextView

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)

        startLocationUpdatesButton.setOnClickListener {
            startLocationUpdates()
        }

        stopLocationUpdatesButton.setOnClickListener {
            stopLocationUpdates()
        }

        buildLocationRequest()
        buildLocationCallBack()
        buildLocationSettingsRequest()
    }

    private fun stopLocationUpdates() {
        if (!isLocationUpdatesActive) {
            return
        }
        fusedLocationClient.removeLocationUpdates(locationCallBack).addOnCompleteListener(this) {
            isLocationUpdatesActive = false
            startLocationUpdatesButton.isEnabled = true
            stopLocationUpdatesButton.isEnabled = false
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode === CHECK_SETTINGS_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    Log.d("MainActivity", "User has agreed to change location settings")
                    startLocationUpdates()
                }
                RESULT_CANCELED -> {
                    Log.d("MainActivity", "User has not agreed to change location settings")
                    isLocationUpdatesActive = false
                    startLocationUpdatesButton.isEnabled = true
                    stopLocationUpdatesButton.isEnabled = false
                    updateLocationUi()
                }
            }
        }
    }

    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        locationSettingsRequest = builder.build()
    }

    private fun buildLocationCallBack() {
        locationCallBack = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                currentLocation = locationResult.lastLocation!!
                updateLocationUi()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateLocationUi() {
        if (currentLocation != null) {
            locationTextView.text = "${currentLocation!!.latitude}/${currentLocation!!.longitude}"
            locationUpdateTimeTextView.text = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date())
        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 2500
        locationRequest.fastestInterval = 1000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onResume() {
        super.onResume()
        if (isLocationUpdatesActive && checkLocationPermission()) {
            startLocationUpdates();
        } else if (!checkLocationPermission()) {
            requestLocationPermission();
        }
    }

    private fun requestLocationPermission() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (shouldProvideRationale) {
            showSnackBar(
                "Location permission is needed for app functionality",
                "OK"
            ) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION
                )
            }
        } else {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun showSnackBar(mainText: String, action: String, listener: View.OnClickListener) {
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_INDEFINITE)
            .setAction(action, listener).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isEmpty()) {
                Log.d("permissionResult", "Request wag cancelled")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isLocationUpdatesActive) {
                    startLocationUpdates()
                }
            } else {
                showSnackBar("Turn on location on settings", "Settings") {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri: Uri =
                        Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        val permissionState =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }
}