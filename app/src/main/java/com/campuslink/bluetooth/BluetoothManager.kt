package com.campuslink.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import com.campuslink.core.CampusLog
import com.campuslink.data.repository.ChatRepository
import com.campuslink.data.session.SessionManager
import com.campuslink.domain.model.HandshakePayload
import com.campuslink.domain.model.Message
import com.campuslink.domain.model.MessageStatus
import com.campuslink.domain.model.MessageTargetType
import com.campuslink.domain.model.Packet
import com.campuslink.domain.model.PacketType
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
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
    private val connectedThreads = ConcurrentHashMap<String, ConnectedThread>() // key = MAC address
    private val connectedAddresses: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // FIX: Expose the set of currently connected userIds so NearbyViewModel
    // can show ONLY users who are physically in range right now, not stale DB rows.
    private val _connectedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val connectedUserIds: StateFlow<Set<String>> = _connectedUserIds.asStateFlow()

    private var serverThread: ServerThread? = null
    private var myUserId = ""
    private var myUsername = ""
    private var myZone = "BLOCK_32"
    private var myRole = "STUDENT"
    private var myDept = ""

    private val startGuard = AtomicBoolean(false)

    init {
        relayEngine.allThreads = { connectedThreads.values.toList() }
    }

    fun start() {
        if (!startGuard.compareAndSet(false, true)) {
            CampusLog.d("BTManager", "start() already running — ignored")
            return
        }
        scope.launch {
            myUserId = sessionManager.getUserId() ?: run { startGuard.set(false); return@launch }
            myUsername = sessionManager.getUsername() ?: myUserId
            myZone = sessionManager.getZone()
            myRole = sessionManager.getRole()
            myDept = sessionManager.getDept()

            relayEngine.myUserId = myUserId
            relayEngine.myZone = myZone
            _isRunning.value = true

            CampusLog.d("BTManager", "Starting — userId=$myUserId zone=$myZone")

            // Pass the userId to the new advertising method
            bleDiscovery.startAdvertising(myUserId)
            bleDiscovery.startScanning()

            launch {
                bleDiscovery.discoveredPeers.collect { peer ->
                    if (!connectedAddresses.contains(peer.mac)) {
                        CampusLog.d("BTManager", "Discovered peer: ${peer.userId} at ${peer.mac}")
                        connectedAddresses.add(peer.mac)
                        
                        // 1. Immediately push them to the UI so you get the "Option to Connect"
                        repository.upsertUser(com.campuslink.domain.model.User(
                            userId = peer.userId,
                            username = "Discovered Device", // This will be overwritten with their real name upon Handshake
                            deviceAddress = peer.mac,
                            isOnline = false // False means they are discovered but not yet handshaked
                        ))

                        // 2. Attempt the background connection
                        connectTo(peer.mac)
                    }
                }
            }

            serverThread = ServerThread(bluetoothAdapter, scope) { socket ->
                registerSocket(socket)
            }
            serverThread?.start()
        }
    }

    private fun connectTo(mac: String) {
        ConnectThread(
            bluetoothAdapter = bluetoothAdapter,
            peerMacAddress = mac,
            scope = scope,
            onConnected = { socket -> registerSocket(socket) },
            onFailed = { failedMac ->
                connectedAddresses.remove(failedMac)
                CampusLog.w("BTManager", "Connection failed to $failedMac — will retry on rediscovery")
            }
        ).start()
    }

    private fun registerSocket(socket: BluetoothSocket) {
        val addr = socket.remoteDevice.address

        if (connectedThreads.containsKey(addr)) {
            CampusLog.d("BTManager", "Already have connection to $addr — closing duplicate")
            try { socket.close() } catch (_: Exception) {}
            return
        }

        val thread = ConnectedThread(
            socket = socket,
            scope = scope,
            relayEngine = relayEngine,
            onDisconnected = { dead ->
                connectedThreads.remove(dead.deviceAddress)
                connectedAddresses.remove(dead.deviceAddress)

                // FIX: Remove this user from the live connected set so they
                // disappear from the Nearby screen immediately.
                val disconnectedUserId = dead.remoteUserId
                if (disconnectedUserId.isNotBlank()) {
                    _connectedUserIds.update { it - disconnectedUserId }
                }

                scope.launch {
                    // FIX: Mark user offline in DB so isOnline=false persists.
                    // Without this, a user who walked away stayed "In range" forever.
                    if (disconnectedUserId.isNotBlank()) {
                        repository.setUserOnline(disconnectedUserId, false)
                    }
                    repository.onNodeDisconnected()
                }
                CampusLog.d("BTManager", "Peer disconnected: $addr userId=$disconnectedUserId | remaining=${connectedThreads.size}")
            }
        )

        connectedThreads[addr] = thread
        CampusLog.d("BTManager", "Registered peer: $addr | total=${connectedThreads.size}")

        // Send our identity so the peer can register us and vice versa
        val hs = HandshakePayload(myUserId, myUsername, bluetoothAdapter.address, myZone, myRole, myDept)
        thread.enqueue(Packet(PacketType.HANDSHAKE.name, gson.toJson(hs)))
    }

    // Called from RelayEngine when a HANDSHAKE packet is received from a peer.
    // This is the moment we learn the peer's userId from their MAC address.
    fun onPeerIdentified(macAddress: String, userId: String) {
        val thread = connectedThreads[macAddress] ?: return
        thread.remoteUserId = userId

        // FIX: Add to live connected set — this makes them appear in Nearby immediately
        _connectedUserIds.update { it + userId }
        CampusLog.d("BTManager", "Peer identified: $macAddress → $userId | online set: ${_connectedUserIds.value}")
    }

    fun sendMessage(msg: Message) {
        scope.launch {
            val pkt = Packet(PacketType.MESSAGE.name, gson.toJson(msg))
            val threads = connectedThreads.values.toList()

            when (msg.targetType) {
                MessageTargetType.USER.name -> {
                    val target = relayEngine.getBestThread(msg.receiverId)
                    if (target != null) {
                        CampusLog.d("BTManager", "Sending ${msg.messageId} direct to ${msg.receiverId}")
                        relayEngine.sendWithRetry(pkt, target)
                        return@launch
                    }
                    if (threads.isEmpty()) {
                        CampusLog.w("BTManager", "No peers — storing ${msg.messageId} as PENDING")
                        repository.storePendingMessage(msg.copy(status = MessageStatus.PENDING.name))
                    } else {
                        CampusLog.d("BTManager", "Flooding ${msg.messageId} to ${threads.size} peers")
                        threads.forEach { it.enqueue(pkt) }
                    }
                }
                else -> {
                    if (threads.isEmpty()) repository.storePendingMessage(msg.copy(status = MessageStatus.PENDING.name))
                    else threads.forEach { it.enqueue(pkt) }
                }
            }
        }
    }

    fun stop() {
        _isRunning.value = false
        startGuard.set(false)
        // FIX: Mark all currently connected users offline when Bluetooth stops
        scope.launch {
            _connectedUserIds.value.forEach { userId ->
                repository.setUserOnline(userId, false)
            }
        }
        _connectedUserIds.value = emptySet()
        bleDiscovery.stopAll()
        serverThread?.stop()
        connectedThreads.values.forEach { it.disconnect() }
        connectedThreads.clear()
        connectedAddresses.clear()
        CampusLog.d("BTManager", "Stopped — all peers marked offline")
    }

    fun getConnectedPeerCount() = connectedThreads.size
    fun getConnectedUserIds(): Set<String> = _connectedUserIds.value
}
