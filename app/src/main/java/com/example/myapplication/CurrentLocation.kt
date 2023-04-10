package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.*

@Composable
fun CurrentLocation(
    context: Context,
    location: (latitude: Double, longitude: Double, locality: String, countryName: String) -> Unit
) {
    val permissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    var setLocation by remember { mutableStateOf(false) }

    val launcherMultiplePermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val areGranted = permissionsMap.values.reduce { acc, next -> acc || next }
        setLocation = areGranted
    }

    if (
        permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    ) {
        setLocation = true
    } else {
        LaunchedEffect(Unit) {
            launcherMultiplePermissions.launch(permissions)
        }
    }

    if (setLocation) {
        val locationListener = MyLocationListener(context, location)
        val locationManager =
            (context as Activity).getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            900,
            20f,
            locationListener
        )
        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?.let { locationListener.onLocationChanged(it) }
    }

}

fun checkGPSIsOn(context: Context): Boolean {
    val locationManager =
        (context as Activity).getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    else return true
    return false
}

class MyLocationListener(
    private val context: Context,
    private val myLocation: (latitude: Double, longitude: Double, locality: String, countryName: String) -> Unit,
) : LocationListener {
    override fun onLocationChanged(location: Location) {
        val gcd = Geocoder(context, Locale.getDefault())
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gcd.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        myLocation.invoke(
                            location.latitude,
                            location.longitude,
                            addresses.first().locality ?: addresses.first().subLocality
                            ?: addresses.first().thoroughfare ?: addresses.first().subThoroughfare
                            ?: "Location was found",
                            addresses.first().countryName.orEmpty()
                        )
                    }
                }
            } else {
                val addresses: List<Address> =
                    gcd.getFromLocation(location.latitude, location.longitude, 1) ?: listOf()
                if (addresses.isNotEmpty()) {
                    myLocation.invoke(
                        location.latitude,
                        location.longitude,
                        addresses.first().locality ?: addresses.first().subLocality
                        ?: addresses.first().thoroughfare ?: addresses.first().subThoroughfare
                        ?: "Location was found",
                        addresses.first().countryName.orEmpty()
                    )
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("location__", e.message.orEmpty())
            myLocation.invoke(
                location.latitude,
                location.longitude,
                e.message.orEmpty(),
                ""
            )
        }

    }

    override fun onProviderDisabled(provider: String) {}
    override fun onProviderEnabled(provider: String) {}

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
    }
}