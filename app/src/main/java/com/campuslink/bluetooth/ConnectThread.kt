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
    // FIX: same reason as ServerThread — raw socket passed to BluetoothManager
    private val onConnected: (BluetoothSocket) -> Unit,
    private val onFailed: (String) -> Unit
) {
    fun start() {
        scope.launch(Dispatchers.IO) {
            var socket: BluetoothSocket? = null
            try {
                bluetoothAdapter.cancelDiscovery()
                val device = bluetoothAdapter.getRemoteDevice(peerMacAddress)
                
                try {
                    // Attempt 1: Standard Service Record Connection
                    socket = device.createInsecureRfcommSocketToServiceRecord(Constants.MY_APP_UUID)
                    socket.connect()
                } catch (e: IOException) {
                    CampusLog.w("ConnectThread", "Standard connect failed, attempting reflection fallback...")
                    
                    // Attempt 2: Reflection Fallback (Bypasses OS BLE-to-Classic MAC routing blocks)
                    val method = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
                    socket = method.invoke(device, 1) as BluetoothSocket
                    socket.connect()
                }
                
                CampusLog.d("ConnectThread", "Successfully connected to $peerMacAddress")
                socket?.let { onConnected(it) } ?: throw IOException("Socket is null after connect")
                
            } catch (e: Exception) {
                CampusLog.e("ConnectThread", "All connection attempts failed to $peerMacAddress: ${e.message}")
                try { socket?.close() } catch (_: IOException) {}
                onFailed(peerMacAddress)
            }
        }
    }
}
