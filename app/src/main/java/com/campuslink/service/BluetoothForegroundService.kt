package com.campuslink.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.campuslink.bluetooth.BluetoothManager
import com.campuslink.bluetooth.RelayEngine
import com.campuslink.core.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BluetoothForegroundService : Service() {

    @Inject lateinit var bluetoothManager: BluetoothManager
    @Inject lateinit var relayEngine: RelayEngine

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val notif = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("CampusLink Active")
            .setContentText("LPU mesh relay running — searching for peers...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        startForeground(1, notif)

        // FIX: Wire the BluetoothManager reference into RelayEngine here,
        // after Hilt has injected both. This allows RelayEngine to call
        // bluetoothManager.onPeerIdentified() when a HANDSHAKE is received,
        // which updates the live connectedUserIds set for the Nearby screen.
        relayEngine.bluetoothManager = bluetoothManager

        bluetoothManager.start()
        return START_STICKY
    }

    override fun onDestroy() {
        bluetoothManager.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            "CampusLink Relay",
            NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}
