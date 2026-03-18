package com.campuslink.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import com.campuslink.core.CampusLog
import com.campuslink.core.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class ConnectThread(
    private val bluetoothAdapter: BluetoothAdapter,
    private val peerMacAddress: String,
    private val scope: CoroutineScope,
    private val relayEngine: RelayEngine,
    private val onConnected: (ConnectedThread) -> Unit,
    private val onFailed: (String) -> Unit
) {
    fun start() {
        scope.launch(Dispatchers.IO) {
            var socket: BluetoothSocket? = null
            try {
                bluetoothAdapter.cancelDiscovery()
                socket = bluetoothAdapter.getRemoteDevice(peerMacAddress)
                    .createInsecureRfcommSocketToServiceRecord(Constants.MY_APP_UUID)
                socket.connect()
                CampusLog.d("ConnectThread","Connected to $peerMacAddress")
                onConnected(ConnectedThread(socket, scope, relayEngine) {})
            } catch (e: IOException) {
                CampusLog.e("ConnectThread","Failed $peerMacAddress: ${e.message}")
                try { socket?.close() } catch (_: IOException) {}
                onFailed(peerMacAddress)
            }
        }
    }
}
