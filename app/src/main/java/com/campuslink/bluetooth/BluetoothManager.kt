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
    private val connectedThreads = ConcurrentHashMap<String, ConnectedThread>()
    private val connectedAddresses: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    private var serverThread: ServerThread? = null

    private var myUserId = ""
    private var myUsername = ""
    private var myZone = "BLOCK_32"
    private var myRole = "STUDENT"
    private var myDept = ""

    // FIX: Prevent double-start (HomeViewModel + ForegroundService both called start())
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

            bleDiscovery.startAdvertising()
            bleDiscovery.startScanning()

            launch {
                bleDiscovery.discoveredMacs.collect { mac ->
                    if (!connectedAddresses.contains(mac) && mac != bluetoothAdapter.address) {
                        CampusLog.d("BTManager", "Discovered peer: $mac")
                        connectedAddresses.add(mac)
                        connectTo(mac)
                    }
                }
            }

            // FIX: ServerThread now gets raw socket callback
            serverThread = ServerThread(bluetoothAdapter, scope) { socket ->
                registerSocket(socket)
            }
            serverThread?.start()
        }
    }

    private fun connectTo(mac: String) {
        // FIX: ConnectThread now gets raw socket callback
        ConnectThread(
            bluetoothAdapter = bluetoothAdapter,
            peerMacAddress = mac,
            scope = scope,
            onConnected = { socket -> registerSocket(socket) },
            onFailed = { failedMac ->
                connectedAddresses.remove(failedMac)
                CampusLog.w("BTManager", "Connection failed to $failedMac")
            }
        ).start()
    }

    // FIX: THIS is the ONLY place ConnectedThread is ever created.
    // Previously ServerThread made one, ConnectThread made one, and this
    // function made another — three ConnectedThreads per socket, 3 readers
    // fighting over one InputStream. Every single message was corrupted.
    private fun registerSocket(socket: BluetoothSocket) {
        val addr = socket.remoteDevice.address

        if (connectedThreads.containsKey(addr)) {
            CampusLog.d("BTManager", "Already registered $addr — closing duplicate socket")
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
                scope.launch {
                    repository.onNodeDisconnected()
                    if (dead.remoteUserId.isNotBlank()) {
                        repository.setUserOnline(dead.remoteUserId, false)
                    }
                }
                CampusLog.d("BTManager", "Peer left: ${dead.deviceAddress} | remaining=${connectedThreads.size}")
            }
        )

        connectedThreads[addr] = thread
        CampusLog.d("BTManager", "Registered: $addr | total=${connectedThreads.size}")

        val hs = HandshakePayload(myUserId, myUsername, bluetoothAdapter.address, myZone, myRole, myDept)
        thread.enqueue(Packet(PacketType.HANDSHAKE.name, gson.toJson(hs)))
    }

    fun sendMessage(msg: Message) {
        scope.launch {
            val pkt = Packet(PacketType.MESSAGE.name, gson.toJson(msg))
            val threads = connectedThreads.values.toList()

            when (msg.targetType) {
                MessageTargetType.USER.name -> {
                    val target = relayEngine.getBestThread(msg.receiverId)
                    if (target != null) {
                        relayEngine.sendWithRetry(pkt, target)
                        return@launch
                    }
                    if (threads.isEmpty()) repository.storePendingMessage(msg.copy(status = MessageStatus.PENDING.name))
                    else threads.forEach { it.enqueue(pkt) }
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
        bleDiscovery.stopAll()
        serverThread?.stop()
        connectedThreads.values.forEach { it.disconnect() }
        connectedThreads.clear()
        connectedAddresses.clear()
        CampusLog.d("BTManager", "Stopped")
    }

    fun getConnectedPeerCount() = connectedThreads.size
}
