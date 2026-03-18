package com.campuslink.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import com.campuslink.core.CampusLog
import com.campuslink.core.Constants
import com.campuslink.domain.model.ConnectionEvent
import com.campuslink.domain.model.ErrorType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.UUID
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

    fun encodeMacToUUID(adapter: BluetoothAdapter): UUID {
        return try {
            val mac = adapter.address.replace(":", "")   // exactly 12 chars
            UUID.fromString(
                "0000${mac.substring(0,4)}-${mac.substring(4,8)}-" +
                "${mac.substring(8,12)}-0000-000000000000"
            )
        } catch (e: Exception) {
            CampusLog.e("BLE", "UUID encode error: ${e.message}")
            UUID.randomUUID()
        }
    }

    fun decodeMacFromUUID(uuid: UUID): String {
        val clean = uuid.toString().replace("-", "")   // 32 chars
        return clean.substring(4, 16).chunked(2).joinToString(":")
    }

    fun startAdvertising() {
        if (!bluetoothAdapter.isEnabled) return
        val advertiser = bluetoothAdapter.bluetoothLeAdvertiser ?: run {
            _events.tryEmit(ConnectionEvent.Error(ErrorType.BLE_UNAVAILABLE, "No advertiser"))
            return
        }
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(false).setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH).build()
        // FIX: Use the fixed app-wide UUID to identify CampusLink devices.
        // Previous code used encodeMacToUUID(bluetoothAdapter) which encodes
        // bluetoothAdapter.address — but on Android 6+, the OS always returns
        // the fake MAC "02:00:00:00:00:00" for privacy.  Every device encoded
        // the same fake MAC, the decoder recovered that same fake address, and
        // the equality check "if (mac != bluetoothAdapter.address)" was always
        // false, so no peer MAC was ever emitted and no connection was ever made.
        val uuid = Constants.MY_APP_UUID
        val data = AdvertiseData.Builder().addServiceUuid(ParcelUuid(uuid))
            .setIncludeDeviceName(false).build()
        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(s: AdvertiseSettings?) =
                CampusLog.d("BLE", "Advertising UUID=$uuid")
            override fun onStartFailure(code: Int) {
                CampusLog.e("BLE", "Advertise failed code=$code")
                _events.tryEmit(ConnectionEvent.Error(ErrorType.BLE_UNAVAILABLE, "code=$code"))
            }
        }
        try { advertiser.startAdvertising(settings, data, advertiseCallback!!) }
        catch (e: SecurityException) {
            _events.tryEmit(ConnectionEvent.Error(ErrorType.PERMISSION_DENIED, e.message ?: ""))
        }
    }

    fun startScanning() {
        leScanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            _events.tryEmit(ConnectionEvent.Error(ErrorType.BLE_UNAVAILABLE, "No scanner"))
            return
        }
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        // FIX: Filter by the fixed CampusLink service UUID so we only wake up
        // for peer devices running this app.  And use result.device.address (the
        // real Bluetooth MAC reported by the OS in the scan record) instead of
        // decoding it from the UUID payload.  The decode approach broke because
        // every device encoded the fake "02:00:00:00:00:00" address (Android 6+
        // privacy restriction), so nothing was ever emitted.
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(Constants.MY_APP_UUID))
            .build()
        scanCallback = object : ScanCallback() {
            override fun onScanResult(type: Int, result: ScanResult) {
                val mac = result.device.address
                if (!mac.isNullOrBlank()) {
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
        } catch (e: SecurityException) {
            _events.tryEmit(ConnectionEvent.Error(ErrorType.PERMISSION_DENIED, e.message ?: ""))
        }
    }

    fun stopAll() {
        try { advertiseCallback?.let { bluetoothAdapter.bluetoothLeAdvertiser?.stopAdvertising(it) } } catch (_: Exception) {}
        try { scanCallback?.let { leScanner?.stopScan(it) } } catch (_: Exception) {}
    }
}
