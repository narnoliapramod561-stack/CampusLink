package com.campuslink.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.campuslink.bluetooth.BluetoothManager
import com.campuslink.core.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BluetoothForegroundService : Service() {
    @Inject lateinit var bluetoothManager: BluetoothManager
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val notif = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("CampusLink Active")
            .setContentText("Mesh relay running — ${bluetoothManager.getConnectedPeerCount()} peers connected")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true).build()
            
        startForeground(1, notif)
        bluetoothManager.start()
        return START_STICKY
    }
    
    override fun onDestroy() { 
        bluetoothManager.stop()
        super.onDestroy() 
    }
    
    override fun onBind(i: Intent?): IBinder? = null
    
    private fun createChannel() {
        val ch = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID, 
            "CampusLink Relay",
            NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }
}
