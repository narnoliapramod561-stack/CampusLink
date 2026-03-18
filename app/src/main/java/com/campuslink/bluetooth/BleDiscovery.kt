package com.campuslink.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import com.campuslink.core.CampusLog
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
        val uuid = encodeMacToUUID(bluetoothAdapter)
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
        scanCallback = object : ScanCallback() {
            override fun onScanResult(type: Int, result: ScanResult) {
                result.scanRecord?.serviceUuids?.forEach { parcelUuid ->
                    try {
                        if (!parcelUuid.uuid.toString().endsWith("-0000-000000000000")) return@forEach
                        val mac = decodeMacFromUUID(parcelUuid.uuid)
                        if (mac != bluetoothAdapter.address) _discoveredMacs.tryEmit(mac)
                    } catch (_: Exception) {}
                }
            }
            override fun onScanFailed(code: Int) {
                CampusLog.e("BLE", "Scan failed code=$code")
                _events.tryEmit(ConnectionEvent.Error(ErrorType.BLE_UNAVAILABLE, "Scan code=$code"))
            }
        }
        try { leScanner?.startScan(null, settings, scanCallback!!)
              CampusLog.d("BLE", "Scan started") }
        catch (e: SecurityException) {
            _events.tryEmit(ConnectionEvent.Error(ErrorType.PERMISSION_DENIED, e.message ?: ""))
        }
    }

    fun stopAll() {
        try { advertiseCallback?.let { bluetoothAdapter.bluetoothLeAdvertiser?.stopAdvertising(it) } } catch (_: Exception) {}
        try { scanCallback?.let { leScanner?.stopScan(it) } } catch (_: Exception) {}
    }
}
