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

    // ConcurrentHashMap for thread-safe peer tracking
    private val connectedThreads = ConcurrentHashMap<String, ConnectedThread>()

    // ── BUG B4 FIX ────────────────────────────────────────────────────────
    // mutableSetOf<String>() is NOT thread-safe.
    // BLE discovery callback and connect/disconnect callbacks run on
    // different IO threads concurrently → ConcurrentModificationException risk.
    // ConcurrentHashMap.newKeySet() is a proper concurrent set backed by CHM.
    // ──────────────────────────────────────────────────────────────────────
    private val connectedAddresses: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var serverThread: ServerThread? = null
    private var myUserId: String = ""
    private var myUsername: String = ""

    init {
        relayEngine.allThreads = { connectedThreads.values.toList() }
    }

    fun start() {
        scope.launch {
            myUserId = sessionManager.getUserId() ?: run {
                CampusLog.e("BTManager", "No user session found — cannot start")
                return@launch
            }
            myUsername = sessionManager.getUsername() ?: myUserId
            relayEngine.myUserId = myUserId

            _isRunning.value = true
            CampusLog.d("BTManager", "Starting with userId=$myUserId")

            // Start BLE advertising + scanning
            bleDiscovery.startAdvertising()
            bleDiscovery.startScanning()

            // Collect discovered peer MACs and connect
            launch {
                bleDiscovery.discoveredMacs.collect { mac ->
                    if (!connectedAddresses.contains(mac) && mac != bluetoothAdapter.address) {
                        CampusLog.d("BTManager", "New peer discovered: $mac — connecting...")
                        connectedAddresses.add(mac)
                        connectTo(mac)
                    }
                }
            }

            // Start RFCOMM server to accept incoming connections
            serverThread = ServerThread(bluetoothAdapter, scope, relayEngine) { thread ->
                registerThread(thread)
            }
            serverThread?.start()
        }
    }

    private fun connectTo(mac: String) {
        ConnectThread(
            bluetoothAdapter = bluetoothAdapter,
            peerMacAddress = mac,
            scope = scope,
            relayEngine = relayEngine,
            onConnected = { thread ->
                registerThread(thread)
            },
            onFailed = { failedMac ->
                // Remove from tracked set so BLE rediscovery can retry
                connectedAddresses.remove(failedMac)
                CampusLog.w("BTManager", "Connection failed to $failedMac — will retry on rediscovery")
            }
        ).start()
    }

    // ── BUG B2 FIX ────────────────────────────────────────────────────────
    // The original registerThread() created ConnectedThread in ServerThread
    // and ConnectThread with an onDisconnected lambda that only logged.
    // BluetoothManager never cleaned up the dead thread from connectedThreads.
    // Fix: create a NEW ConnectedThread here wrapping the accepted socket,
    // with a proper onDisconnected lambda that removes it from the map
    // and updates stats. This is the single authoritative place for cleanup.
    // ──────────────────────────────────────────────────────────────────────
    private fun registerThread(incomingThread: ConnectedThread) {
        // Wrap with cleanup-aware disconnect callback
        val thread = ConnectedThread(
            socket = incomingThread.socket,
            scope = scope,
            relayEngine = relayEngine,
            onDisconnected = { dead ->
                connectedThreads.remove(dead.deviceAddress)
                connectedAddresses.remove(dead.deviceAddress)
                scope.launch { repository.onNodeDisconnected() }
                CampusLog.d("BTManager", "Peer removed: ${dead.deviceAddress} | active=${connectedThreads.size}")
            }
        )

        connectedThreads[thread.deviceAddress] = thread
        CampusLog.d("BTManager", "Registered peer: ${thread.deviceAddress} | total=${connectedThreads.size}")

        // Send identity handshake immediately after connecting
        val handshake = HandshakePayload(myUserId, myUsername, bluetoothAdapter.address)
        thread.enqueue(Packet(PacketType.HANDSHAKE.name, gson.toJson(handshake)))
    }

    fun sendMessage(msg: Message) {
        val targetThread = connectedThreads.values.find { it.remoteUserId == msg.receiverId }
        val packet = Packet(PacketType.MESSAGE.name, gson.toJson(msg.copy(status = MessageStatus.SENDING.name)))

        scope.launch {
            if (targetThread != null) {
                // Direct delivery — with retry
                relayEngine.sendWithRetry(packet, targetThread)
            } else {
                // Flood-relay to all connected peers (DTN broadcast)
                val threads = connectedThreads.values.toList()
                if (threads.isEmpty()) {
                    CampusLog.w("BTManager", "No peers connected — storing ${msg.messageId} as PENDING")
                    repository.storePendingMessage(msg.copy(status = MessageStatus.PENDING.name))
                } else {
                    threads.forEach { it.enqueue(packet) }
                    CampusLog.d("BTManager", "Flooded ${msg.messageId} to ${threads.size} peers")
                }
            }
        }
    }

    fun stop() {
        _isRunning.value = false
        bleDiscovery.stopAll()
        serverThread?.stop()
        connectedThreads.values.forEach { it.disconnect() }
        connectedThreads.clear()
        connectedAddresses.clear()
        CampusLog.d("BTManager", "Stopped")
    }

    fun getConnectedPeerCount() = connectedThreads.size
}
