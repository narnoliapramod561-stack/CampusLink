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
    private val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(
        Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.ACCESS_FINE_LOCATION
    ) else arrayOf(
        Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        requestPermissions(perms, 100)
        setContent { 
            CampusLinkTheme { 
                NavGraph() 
            } 
        }
    }
    
    override fun onRequestPermissionsResult(code: Int, permissions: Array<String>, grants: IntArray) {
        super.onRequestPermissionsResult(code, permissions, grants)
        if (grants.all { it == PackageManager.PERMISSION_GRANTED }) {
            startForegroundService(Intent(this, BluetoothForegroundService::class.java))
        }
    }
}
