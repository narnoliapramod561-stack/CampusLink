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

// New data class to hold the extracted BLE data
data class DiscoveredPeer(val mac: String, val userId: String)

@Singleton
class BleDiscovery @Inject constructor(private val bluetoothAdapter: BluetoothAdapter) {
    private val _discoveredPeers = MutableSharedFlow<DiscoveredPeer>(extraBufferCapacity = 64)
    val discoveredPeers: SharedFlow<DiscoveredPeer> = _discoveredPeers
    
    private val _events = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<ConnectionEvent> = _events
    
    private var advertiseCallback: AdvertiseCallback? = null
    private var scanCallback: ScanCallback? = null
    private var leScanner: BluetoothLeScanner? = null

    // Now accepts myUserId so it can be broadcasted in the beacon
    fun startAdvertising(myUserId: String) {
        if (!bluetoothAdapter.isEnabled) return
        val advertiser = bluetoothAdapter.bluetoothLeAdvertiser ?: return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()
            
        // Inject the userId into the BLE payload (Max 20 bytes)
        val userIdBytes = myUserId.toByteArray(Charsets.UTF_8)
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(Constants.MY_APP_UUID))
            .addServiceData(ParcelUuid(Constants.MY_APP_UUID), userIdBytes)
            .setIncludeDeviceName(false)
            .build()
            
        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) = CampusLog.d("BLE", "Advertising CampusLink Presence")
            override fun onStartFailure(code: Int) = CampusLog.e("BLE", "Advertise failed code=$code")
        }
        try { advertiser.startAdvertising(settings, data, advertiseCallback!!) }
        catch (e: Exception) { CampusLog.e("BLE", "Adv Error: ${e.message}") }
    }

    fun startScanning() {
        leScanner = bluetoothAdapter.bluetoothLeScanner ?: return
        
        // Only wake up for devices broadcasting the CampusLink UUID
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(Constants.MY_APP_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val mac = result.device.address
                if (mac != bluetoothAdapter.address) {
                    // Extract the userId from the payload so we can show them in the UI instantly
                    val serviceData = result.scanRecord?.getServiceData(ParcelUuid(Constants.MY_APP_UUID))
                    val peerUserId = serviceData?.let { String(it, Charsets.UTF_8) } ?: "UnknownPeer"
                    
                    _discoveredPeers.tryEmit(DiscoveredPeer(mac, peerUserId))
                }
            }
            override fun onScanFailed(code: Int) = CampusLog.e("BLE", "Scan failed code=$code")
        }
        try { 
            leScanner?.startScan(listOf(filter), settings, scanCallback!!)
            CampusLog.d("BLE", "Scan started") 
        }
        catch (e: Exception) { CampusLog.e("BLE", "Scan Error: ${e.message}") }
    }

    fun stopAll() {
        try { advertiseCallback?.let { bluetoothAdapter.bluetoothLeAdvertiser?.stopAdvertising(it) } } catch (_: Exception) {}
        try { scanCallback?.let { leScanner?.stopScan(it) } } catch (_: Exception) {}
    }
}
