package com.campuslink.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import com.campuslink.core.CampusLog
import com.campuslink.core.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException

class ServerThread(
    private val bluetoothAdapter: BluetoothAdapter,
    private val scope: CoroutineScope,
    private val relayEngine: RelayEngine,
    private val onClientConnected: (ConnectedThread) -> Unit
) {
    private var serverSocket: BluetoothServerSocket? = null
    private var serverJob: Job? = null
    fun start() {
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                    Constants.BT_SERVICE_NAME, Constants.MY_APP_UUID)
                CampusLog.d("Server","Listening...")
                while (isActive) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        CampusLog.d("Server","Accepted ${socket.remoteDevice.address}")
                        onClientConnected(ConnectedThread(socket, scope, relayEngine) {})
                    } catch (e: IOException) { if (isActive) CampusLog.e("Server","Accept: ${e.message}"); break }
                }
            } catch (e: IOException) { CampusLog.e("Server","Init: ${e.message}") }
        }
    }
    fun stop() { serverJob?.cancel(); try { serverSocket?.close() } catch (_: IOException) {} }
}
