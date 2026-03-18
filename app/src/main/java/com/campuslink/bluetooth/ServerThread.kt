package com.campuslink.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
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
    // FIX: callback now receives raw BluetoothSocket, NOT a ConnectedThread.
    // Old code created ConnectedThread here, then BluetoothManager created
    // another one on the same socket → 2 readers → every packet corrupted.
    private val onSocketAccepted: (BluetoothSocket) -> Unit
) {
    private var serverSocket: BluetoothServerSocket? = null
    private var serverJob: Job? = null

    fun start() {
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                    Constants.BT_SERVICE_NAME, Constants.MY_APP_UUID
                )
                CampusLog.d("Server", "Listening for incoming RFCOMM connections...")
                while (isActive) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        CampusLog.d("Server", "Accepted connection from ${socket.remoteDevice.address}")
                        onSocketAccepted(socket)
                    } catch (e: IOException) {
                        if (isActive) CampusLog.e("Server", "Accept failed: ${e.message}")
                        break
                    }
                }
            } catch (e: IOException) {
                CampusLog.e("Server", "Failed to create server socket: ${e.message}")
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        try { serverSocket?.close() } catch (_: IOException) {}
        CampusLog.d("Server", "Server socket closed")
    }
}
