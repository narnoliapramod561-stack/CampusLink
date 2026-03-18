package com.campuslink.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.os.ParcelUuid
import com.campuslink.core.CampusLog
import com.campuslink.core.Constants
import com.campuslink.domain.model.ConnectionEvent
import com.campuslink.domain.model.ErrorType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleDiscovery @Inject constructor(private val bluetoothAdapter: BluetoothAdapter) {
    private val _discoveredMacs = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val discoveredMacs: SharedFlow<String> = _discoveredMacs
    private val _events = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<ConnectionEvent> = _events

    private var advertiseCallback: AdvertiseCallback? = null
    private var scanCallback: ScanCallback? = null
    private var leScanner: BluetoothLeScanner? = null

    fun startAdvertising() {
        if (!bluetoothAdapter.isEnabled) return
        val advertiser = bluetoothAdapter.bluetoothLeAdvertiser ?: run {
            _events.tryEmit(ConnectionEvent.Error(ErrorType.BLE_UNAVAILABLE, "No advertiser"))
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        // FIX: Use the app's constant UUID so peers know this is CampusLink.
        // Removed the adapter.address encoding hack — adapter.address always returns
        // 02:00:00:00:00:00 on Android 6+ for privacy, causing all connections to fail.
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(Constants.MY_APP_UUID))
            .setIncludeDeviceName(false)
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) =
                CampusLog.d("BLE", "Advertising CampusLink Presence")
            override fun onStartFailure(code: Int) {
                CampusLog.e("BLE", "Advertise failed code=$code")
                _events.tryEmit(ConnectionEvent.Error(ErrorType.BLE_UNAVAILABLE, "code=$code"))
            }
        }
        try { advertiser.startAdvertising(settings, data, advertiseCallback!!) }
        catch (e: SecurityException) { _events.tryEmit(ConnectionEvent.Error(ErrorType.PERMISSION_DENIED, e.message ?: "")) }
    }

    fun startScanning() {
        leScanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            _events.tryEmit(ConnectionEvent.Error(ErrorType.BLE_UNAVAILABLE, "No scanner"))
            return
        }

        // FIX: Filter only for devices broadcasting the CampusLink UUID.
        // Real MAC is extracted directly from ScanResult.device.address — no UUID encoding tricks.
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(Constants.MY_APP_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val mac = result.device.address
                if (mac != bluetoothAdapter.address) {
                    _discoveredMacs.tryEmit(mac)
                }
            }
            override fun onScanFailed(code: Int) {
                CampusLog.e("BLE", "Scan failed code=$code")
                _events.tryEmit(ConnectionEvent.Error(ErrorType.BLE_UNAVAILABLE, "Scan code=$code"))
            }
        }
        try {
            leScanner?.startScan(listOf(filter), settings, scanCallback!!)
            CampusLog.d("BLE", "Scan started")
        }
        catch (e: SecurityException) { _events.tryEmit(ConnectionEvent.Error(ErrorType.PERMISSION_DENIED, e.message ?: "")) }
    }

    fun stopAll() {
        try { advertiseCallback?.let { bluetoothAdapter.bluetoothLeAdvertiser?.stopAdvertising(it) } } catch (_: Exception) {}
        try { scanCallback?.let { leScanner?.stopScan(it) } } catch (_: Exception) {}
    }
}
