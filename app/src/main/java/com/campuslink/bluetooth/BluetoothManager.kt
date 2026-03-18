package com.campuslink.bluetooth

import android.bluetooth.BluetoothAdapter
import com.campuslink.core.CampusLog
import com.campuslink.data.repository.ChatRepository
import com.campuslink.data.session.SessionManager
import com.campuslink.domain.model.HandshakePayload
import com.campuslink.domain.model.Message
import com.campuslink.domain.model.MessageStatus
import com.campuslink.domain.model.Packet
import com.campuslink.domain.model.PacketType
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothManager @Inject constructor(
    val bluetoothAdapter: BluetoothAdapter,
    private val relayEngine: RelayEngine,
    private val bleDiscovery: BleDiscovery,
    private val repository: ChatRepository,
    private val sessionManager: SessionManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val connectedThreads = ConcurrentHashMap<String, ConnectedThread>()
    
    // Thread-safe set for connected MACs
    private val connectedAddresses: MutableSet<String> = ConcurrentHashMap.newKeySet()
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    private var serverThread: ServerThread? = null
    
    private var myUserId = ""; private var myUsername = ""
    private var myZone = "BLOCK_32"; private var myRole = "STUDENT"; private var myDept = ""

    init { relayEngine.allThreads = { connectedThreads.values.toList() } }

    fun start() {
        scope.launch {
            myUserId = sessionManager.getUserId() ?: return@launch
            myUsername = sessionManager.getUsername() ?: myUserId
            myZone = sessionManager.getZone()
            myRole = sessionManager.getRole()
            myDept = sessionManager.getDept()
            
            relayEngine.myUserId = myUserId
            relayEngine.myZone = myZone
            _isRunning.value = true
            
            bleDiscovery.startAdvertising(); bleDiscovery.startScanning()
            launch { bleDiscovery.discoveredMacs.collect { mac ->
                if (!connectedAddresses.contains(mac) && mac != bluetoothAdapter.address) {
                    connectedAddresses.add(mac); connectTo(mac)
                }
            }}
            serverThread = ServerThread(bluetoothAdapter, scope, relayEngine) { registerThread(it) }
            serverThread?.start()
            CampusLog.d("BTManager","Started userId=$myUserId")
        }
    }

    private fun connectTo(mac: String) {
        ConnectThread(bluetoothAdapter, mac, scope, relayEngine,
            onConnected = { registerThread(it) },
            onFailed = { connectedAddresses.remove(it) }
        ).start()
    }

    private fun registerThread(incoming: ConnectedThread) {
        val thread = ConnectedThread(incoming.socket, scope, relayEngine) { dead ->
            connectedThreads.remove(dead.deviceAddress)
            connectedAddresses.remove(dead.deviceAddress)
            scope.launch { repository.onNodeDisconnected() }
            CampusLog.d("BTManager","Removed ${dead.deviceAddress} active=${connectedThreads.size}")
        }
        connectedThreads[thread.deviceAddress] = thread
        
        val hs = HandshakePayload(myUserId, myUsername, bluetoothAdapter.address, myZone, myRole, myDept)
        thread.enqueue(Packet(PacketType.HANDSHAKE.name, gson.toJson(hs)))
        CampusLog.d("BTManager","Registered ${thread.deviceAddress} total=${connectedThreads.size}")
    }

    fun sendMessage(msg: Message) {
        scope.launch {
            val pkt = Packet(PacketType.MESSAGE.name, gson.toJson(msg))
            val threads = connectedThreads.values.toList()
            
            if (msg.targetType == com.campuslink.domain.model.MessageTargetType.USER.name) {
                val target = relayEngine.getBestThread(msg.receiverId)
                if (target != null) {
                    relayEngine.sendWithRetry(pkt, target)
                    return@launch
                }
            }
            
            // Flood to all for ZONE, BROADCAST, GROUP, or un-cached USER
            if (threads.isEmpty()) {
                repository.storePendingMessage(msg.copy(status = MessageStatus.PENDING.name))
            } else {
                threads.forEach { it.enqueue(pkt) }
            }
        }
    }

    fun stop() {
        _isRunning.value = false; bleDiscovery.stopAll(); serverThread?.stop()
        connectedThreads.values.forEach { it.disconnect() }
        connectedThreads.clear(); connectedAddresses.clear()
    }

    fun getConnectedPeerCount() = connectedThreads.size
}
