package com.example.myapplication.ui.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.androidisland.ezpermission.EzPermission
import com.example.myapplication.service.LocationUpdatesService
import com.example.myapplication.R
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.view.LocationList
import com.example.myapplication.viewModel.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val vm by viewModel<MainViewModel>()

    private var mService: LocationUpdatesService? = null
    private var mBound: Boolean = false

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            val lat = intent?.getStringExtra("pinned_location_lat")
            val long = intent?.getStringExtra("pinned_location_long")
            vm.getLocation(lat?.toDouble() ?: 0.0, long?.toDouble() ?: 0.0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EzPermission.with(this)
            .permissions(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .request { granted, _, _ ->
                if (granted.isNotEmpty()) {
                    if (getLocationMode() == 3)
                        initializeService()
                    else
                        showAlertDialog(this@MainActivity)
                }
            }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocationList()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction("NotifyUser")
        broadcastReceiver.let {
            LocalBroadcastManager.getInstance(this).registerReceiver(it, intentFilter)
        }
    }

    override fun onPause() {
        broadcastReceiver.let {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(it)
        }
        super.onPause()
    }

    private fun showAlertDialog(context: Context) {
        val result = getActivityResult { initializeService() }

        val builder = AlertDialog.Builder(context)
        builder.setTitle(resources.getString(R.string.app_name))
            .setMessage("Please select High accuracy Location Mode from Mode Settings")
            .setPositiveButton(resources.getString(android.R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                result.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setCancelable(false)
            .show()
    }

    private fun ComponentActivity.getActivityResult(result: (Intent?) -> Unit) =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val resultCode = it.resultCode
            val data = it.data

            if (resultCode == Activity.RESULT_OK)
                result.invoke(data)
        }

    private fun getLocationMode(): Int {
        return Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE)
    }

    private var mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationUpdatesService.LocalBinder
            mService = binder.service
            mBound = true
            mService?.requestLocationUpdates()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
            mBound = false
        }
    }

    private fun initializeService() {
        bindService(
            Intent(this, LocationUpdatesService::class.java),
            mServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        if (mBound) {
            unbindService(mServiceConnection)
            mBound = false
        }
        super.onStop()
    }
}