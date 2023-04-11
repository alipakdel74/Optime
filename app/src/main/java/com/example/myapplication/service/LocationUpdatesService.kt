package com.example.myapplication.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.location.LocationManager
import android.media.RingtoneManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.myapplication.CurrentLocationListener
import com.example.myapplication.R
import com.example.myapplication.ui.activity.MainActivity
import java.util.*

class LocationUpdatesService : Service() {

    private var mChangingConfiguration = false

    private val mBinder = LocalBinder()

    private var mServiceHandler: Handler? = null

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        val locationManager =
            applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationListener = CurrentLocationListener { location ->
            requestLocation(location)
        }

        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            0L,
            0f,
            locationListener
        )

    }

    private fun requestLocation(location: Location) {
        mServiceHandler?.removeCallbacksAndMessages(null)
        mServiceHandler = null
        mServiceHandler = Handler(mainLooper)
        mServiceHandler?.postDelayed({
            onNewLocation(location)
            requestLocation(location)
        }, 5000)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val startedFromNotification =
            intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false)

        if (startedFromNotification)
            stopSelf()

        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mChangingConfiguration = true
    }

    override fun onBind(intent: Intent?): IBinder {
        stopForeground(STOP_FOREGROUND_REMOVE)
        mChangingConfiguration = false
        return mBinder
    }

    override fun onRebind(intent: Intent?) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        mChangingConfiguration = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (!mChangingConfiguration)
            startForeground(12345678, serviceNotification())
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        mServiceHandler?.removeCallbacksAndMessages(null)
    }

    fun requestLocationUpdates() {
        startService(Intent(applicationContext, LocationUpdatesService::class.java))
    }

    private fun onNewLocation(location: Location) {
        Log.e(TAG, "New location: $location")

        if (isAppIsInBackground(applicationContext)) {
            sendNotification(location)
        } else {
            val pushNotification = Intent("NotifyUser")
            pushNotification.putExtra("pinned_location_name", location.provider.toString())
            pushNotification.putExtra("pinned_location_lat", location.latitude.toString())
            pushNotification.putExtra("pinned_location_long", location.longitude.toString())
            LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun serviceNotification(): Notification {
        val notification = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        Intent(this, LocationUpdatesService::class.java)
            .putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val mChannel = NotificationChannel(
                "location_service_channel",
                name,
                NotificationManager.IMPORTANCE_HIGH
            )
            notification.createNotificationChannel(mChannel)
        }

        val builder = NotificationCompat.Builder(this, "location_service_channel")
            .setContentTitle("Location Service")
            .setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setWhen(System.currentTimeMillis())

        val locationListener = CurrentLocationListener { location ->
            builder.setContentText("${location.latitude} , ${location.longitude}")
        }
        val locationManager =
            applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            9,
            1f,
            locationListener
        )
        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
            locationListener.onLocationChanged(it)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            builder.priority = Notification.PRIORITY_HIGH

        return builder.build()
    }

    inner class LocalBinder : Binder() {
        internal val service: LocationUpdatesService
            get() = this@LocationUpdatesService
    }

    private fun isAppIsInBackground(context: Context): Boolean {
        var isInBackground = true

        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.let {
                val runningProcesses = it.runningAppProcesses
                for (processInfo in runningProcesses) {
                    if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        for (activeProcess in processInfo.pkgList) {
                            if (activeProcess == context.packageName) {
                                isInBackground = false
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return isInBackground
    }

    @SuppressLint("MissingPermission")
    private fun sendNotification(location: Location) {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentText("${location.latitude} - ${location.longitude}")
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setBubbleMetadata(
                pendingIntent?.let {
                    NotificationCompat.BubbleMetadata.Builder(
                        it,
                        IconCompat.createWithResource(this, R.mipmap.ic_launcher)
                    ).build()
                }
            )

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(1 /* ID of notification */, notificationBuilder.build())
    }

    companion object {
        private const val EXTRA_STARTED_FROM_NOTIFICATION = "started_from_notification"
        private const val TAG = "LocationUpdatesService"
    }

}