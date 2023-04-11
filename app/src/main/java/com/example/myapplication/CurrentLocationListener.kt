package com.example.myapplication

import android.location.*
import android.os.Bundle
import androidx.compose.runtime.*
import java.util.*


class CurrentLocationListener(private val myLocation: (location: Location) -> Unit) : LocationListener {
    override fun onLocationChanged(location: Location) {
        myLocation.invoke(location)
    }

    override fun onProviderDisabled(provider: String) {}
    override fun onProviderEnabled(provider: String) {}

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
    }
}