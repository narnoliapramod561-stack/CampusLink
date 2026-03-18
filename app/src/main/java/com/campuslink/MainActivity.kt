package com.campuslink

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.campuslink.service.BluetoothForegroundService
import com.campuslink.ui.navigation.NavGraph
import com.campuslink.ui.theme.CampusLinkTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requiredPerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) else arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CampusLinkTheme { NavGraph() } }

        // FIX: Start service immediately if permissions already granted (app relaunch).
        // Old code only started in onRequestPermissionsResult — on second launch,
        // permissions are already granted so the dialog callback never fires,
        // meaning Bluetooth never started on relaunch.
        if (allPermissionsGranted()) {
            startBluetoothService()
        } else {
            requestPermissions(requiredPerms, 100)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startBluetoothService()
        }
    }

    private fun allPermissionsGranted(): Boolean =
        requiredPerms.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    private fun startBluetoothService() {
        startForegroundService(Intent(this, BluetoothForegroundService::class.java))
    }
}
