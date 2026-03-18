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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleDiscovery @Inject constructor(private val bluetoothAdapter: BluetoothAdapter) {

    // Emits the Bluetooth Classic MAC of each discovered CampusLink peer
    private val _discoveredMacs = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val discoveredMacs: SharedFlow<String> = _discoveredMacs

    private val _events = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<ConnectionEvent> = _events

    private var advertiseCallback: AdvertiseCallback? = null
    private var scanCallback: ScanCallback? = null
    private var leScanner: BluetoothLeScanner? = null

    // ── BUG FIX ──────────────────────────────────────────────────────────────
    // The previous version tried to put both addServiceUuid() AND addServiceData()
    // in the same advertising packet. A 128-bit UUID alone takes 18 bytes
    // (2 header + 16 UUID). Service data with same UUID takes 20 bytes
    // (2 header + 16 UUID + 2 data-length + userId bytes).
    // Total = 38+ bytes > 31-byte BLE packet hard limit.
    // Result: ADVERTISE_FAILED_DATA_TOO_LARGE (error code 1).
    // The callback logged it silently and nobody could ever discover anyone.
    //
    // Fix: Back to the proven MAC-encoding approach.
    // Encode the 12-char Bluetooth Classic MAC into a single 128-bit Service UUID.
    // Total advertising cost = 18 bytes (well within limit).
    // The peer's real userId is obtained from the HANDSHAKE after RFCOMM connects.
    // ─────────────────────────────────────────────────────────────────────────

    // MAC without colons = exactly 12 hex chars (e.g. "AABBCCDDEEFF")
    // Encode into UUID: "0000XXXX-XXXX-XXXX-0000-000000000000"
    // The 12 MAC chars fill the first 3 UUID segments (4+4+4 = 12)
    private fun encodeMacToUUID(adapter: BluetoothAdapter): UUID {
        return try {
            val mac = adapter.address.replace(":", "")
            UUID.fromString(
                "0000${mac.substring(0, 4)}-${mac.substring(4, 8)}-" +
                "${mac.substring(8, 12)}-0000-000000000000"
            )
        } catch (e: Exception) {
            CampusLog.e("BLE", "UUID encode failed: ${e.message}")
            UUID.randomUUID()
        }
    }

    // Extract 12 MAC hex chars from positions 4–16 of the clean UUID string
    private fun decodeMacFromUUID(uuid: UUID): String {
        val clean = uuid.toString().replace("-", "") // 32 hex chars
        return clean.substring(4, 16).chunked(2).joinToString(":")
    }

    fun startAdvertising() {
        if (!bluetoothAdapter.isEnabled) {
            CampusLog.e("BLE", "Bluetooth not enabled — cannot advertise")
            return
        }
        val advertiser = bluetoothAdapter.bluetoothLeAdvertiser ?: run {
            CampusLog.e("BLE", "BLE advertising not supported on this device")
            _events.tryEmit(ConnectionEvent.Error(ErrorType.BLE_UNAVAILABLE, "No advertiser"))
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(false)  // We use RFCOMM, not GATT
            .setTimeout(0)          // Advertise indefinitely
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val myUUID = encodeMacToUUID(bluetoothAdapter)
        // Single service UUID in packet = 18 bytes total, well within 31-byte limit
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(myUUID))
            .setIncludeDeviceName(false)
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                CampusLog.d("BLE", "Advertising started with UUID=$myUUID")
            }
            override fun onStartFailure(errorCode: Int) {
                // errorCode 1 = DATA_TOO_LARGE, 2 = TOO_MANY_ADVERTISERS,
                // 3 = ALREADY_STARTED, 4 = INTERNAL_ERROR, 5 = FEATURE_UNSUPPORTED
                CampusLog.e("BLE", "Advertise failed errorCode=$errorCode")
                _events.tryEmit(ConnectionEvent.Error(ErrorType.BLE_UNAVAILABLE, "Advertise error $errorCode"))
            }
        }

        try {
            advertiser.startAdvertising(settings, data, advertiseCallback!!)
        } catch (e: SecurityException) {
            CampusLog.e("BLE", "Permission denied for advertising: ${e.message}")
            _events.tryEmit(ConnectionEvent.Error(ErrorType.PERMISSION_DENIED, e.message ?: ""))
        }
    }

    fun startScanning() {
        leScanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            CampusLog.e("BLE", "BLE scanning not supported")
            _events.tryEmit(ConnectionEvent.Error(ErrorType.BLE_UNAVAILABLE, "No scanner"))
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                result.scanRecord?.serviceUuids?.forEach { parcelUuid ->
                    try {
                        val uuidStr = parcelUuid.uuid.toString()
                        // Our encoded UUIDs always end with "-0000-000000000000"
                        if (!uuidStr.endsWith("-0000-000000000000")) return@forEach
                        val mac = decodeMacFromUUID(parcelUuid.uuid)
                        // Ignore our own MAC
                        if (mac.equals(bluetoothAdapter.address, ignoreCase = true)) return@forEach
                        CampusLog.d("BLE", "Discovered CampusLink peer: $mac")
                        _discoveredMacs.tryEmit(mac)
                    } catch (_: Exception) {
                        // Not a CampusLink UUID format — ignore silently
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                CampusLog.e("BLE", "Scan failed errorCode=$errorCode")
                _events.tryEmit(ConnectionEvent.Error(ErrorType.BLE_UNAVAILABLE, "Scan error $errorCode"))
            }
        }

        try {
            // No filter — we check UUID format in the callback
            // Using a ServiceUUID filter might miss devices on some Android versions
            leScanner?.startScan(null, settings, scanCallback!!)
            CampusLog.d("BLE", "BLE scan started")
        } catch (e: SecurityException) {
            CampusLog.e("BLE", "Permission denied for scanning: ${e.message}")
            _events.tryEmit(ConnectionEvent.Error(ErrorType.PERMISSION_DENIED, e.message ?: ""))
        }
    }

    fun stopAll() {
        try {
            advertiseCallback?.let {
                bluetoothAdapter.bluetoothLeAdvertiser?.stopAdvertising(it)
            }
        } catch (_: Exception) {}
        try {
            scanCallback?.let { leScanner?.stopScan(it) }
        } catch (_: Exception) {}
        CampusLog.d("BLE", "BLE advertising and scanning stopped")
    }
}
