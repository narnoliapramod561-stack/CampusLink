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
import com.campuslink.domain.model.ConnectionEvent
import com.campuslink.domain.model.ErrorType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleDiscovery @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter
) {
    private val _discoveredMacs = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val discoveredMacs: SharedFlow<String> = _discoveredMacs

    private val _events = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<ConnectionEvent> = _events

    private var advertiseCallback: AdvertiseCallback? = null
    private var scanCallback: ScanCallback? = null
    private var leScanner: BluetoothLeScanner? = null

    // ── BUG B1 FIX ────────────────────────────────────────────────────────
    // MAC without colons = exactly 12 hex chars (e.g. "AABBCCDDEEFF")
    // We fit all 12 chars into the first 3 UUID segments (4+4+4 = 12 chars)
    // Last two segments are fixed zeros so they don't interfere with decoding
    // ──────────────────────────────────────────────────────────────────────
    fun encodeMacToUUID(adapter: BluetoothAdapter): UUID {
        return try {
            val mac = adapter.address.replace(":", "")   // exactly 12 chars
            UUID.fromString(
                "0000${mac.substring(0, 4)}-${mac.substring(4, 8)}-" +
                "${mac.substring(8, 12)}-0000-000000000000"
            )
        } catch (e: Exception) {
            CampusLog.e("BLE", "Failed to encode MAC to UUID: ${e.message}")
            UUID.randomUUID()
        }
    }

    // Decode: extract chars 4..16 from the clean UUID string = 12 hex chars = MAC
    fun decodeMacFromUUID(uuid: UUID): String {
        val clean = uuid.toString().replace("-", "")     // 32 hex chars
        return clean.substring(4, 16).chunked(2).joinToString(":")
    }

    fun startAdvertising() {
        val bleAdapter = bluetoothAdapter
        if (!bleAdapter.isEnabled) {
            CampusLog.e("BLE", "Bluetooth not enabled")
            return
        }
        val advertiser = bleAdapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            CampusLog.e("BLE", "BLE advertising not supported")
            _events.tryEmit(ConnectionEvent.Error(ErrorType.BLE_UNAVAILABLE, "Advertiser not available"))
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(false)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val uuid = encodeMacToUUID(bleAdapter)
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(uuid))
            .setIncludeDeviceName(false)
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                CampusLog.d("BLE", "Advertising started with UUID $uuid")
            }
            override fun onStartFailure(errorCode: Int) {
                CampusLog.e("BLE", "Advertising failed, errorCode=$errorCode")
                _events.tryEmit(ConnectionEvent.Error(ErrorType.BLE_UNAVAILABLE, "Advertise failed: $errorCode"))
            }
        }

        try {
            advertiser.startAdvertising(settings, data, advertiseCallback!!)
        } catch (e: SecurityException) {
            CampusLog.e("BLE", "SecurityException on advertise: ${e.message}")
            _events.tryEmit(ConnectionEvent.Error(ErrorType.PERMISSION_DENIED, e.message ?: ""))
        }
    }

    fun startScanning() {
        val bleAdapter = bluetoothAdapter
        leScanner = bleAdapter.bluetoothLeScanner
        if (leScanner == null) {
            CampusLog.e("BLE", "BLE scanning not supported")
            _events.tryEmit(ConnectionEvent.Error(ErrorType.BLE_UNAVAILABLE, "Scanner not available"))
            return
        }

        // ── BUG M3 FIX ────────────────────────────────────────────────────
        // Filter to only CampusLink UUIDs (start with "0000") to avoid
        // processing every BLE device on campus (earbuds, watches, etc.)
        // We use a loose prefix filter — any UUID starting with "0000" is ours
        // ──────────────────────────────────────────────────────────────────
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                result.scanRecord?.serviceUuids?.forEach { parcelUuid ->
                    try {
                        val uuidStr = parcelUuid.uuid.toString()
                        // Only process UUIDs that match our encoding pattern
                        if (!uuidStr.endsWith("-0000-000000000000")) return@forEach

                        val mac = decodeMacFromUUID(parcelUuid.uuid)
                        if (mac != bluetoothAdapter.address) {
                            CampusLog.d("BLE", "Discovered CampusLink peer MAC: $mac")
                            _discoveredMacs.tryEmit(mac)
                        }
                    } catch (e: Exception) {
                        // Not a CampusLink UUID, ignore silently
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                CampusLog.e("BLE", "Scan failed, errorCode=$errorCode")
                _events.tryEmit(ConnectionEvent.Error(ErrorType.BLE_UNAVAILABLE, "Scan failed: $errorCode"))
            }
        }

        try {
            leScanner?.startScan(null, scanSettings, scanCallback!!)
            CampusLog.d("BLE", "BLE scan started")
        } catch (e: SecurityException) {
            CampusLog.e("BLE", "SecurityException on scan: ${e.message}")
            _events.tryEmit(ConnectionEvent.Error(ErrorType.PERMISSION_DENIED, e.message ?: ""))
        }
    }

    fun stopAll() {
        try {
            advertiseCallback?.let { bluetoothAdapter.bluetoothLeAdvertiser?.stopAdvertising(it) }
        } catch (_: Exception) {}
        try {
            scanCallback?.let { leScanner?.stopScan(it) }
        } catch (_: Exception) {}
        CampusLog.d("BLE", "BLE stopped")
    }
}
