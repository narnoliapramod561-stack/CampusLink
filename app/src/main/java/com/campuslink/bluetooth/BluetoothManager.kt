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
import java.util.UUID
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
    private val connectedAddresses = mutableSetOf<String>()

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
            myUserId = sessionManager.getUserId() ?: return@launch
            myUsername = sessionManager.getUsername() ?: return@launch
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

            // Start RFCOMM server
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
                connectedAddresses.remove(failedMac)
                CampusLog.w("BTManager", "Connection failed to $failedMac, will retry on rediscovery")
            }
        ).start()
    }

    private fun registerThread(thread: ConnectedThread) {
        connectedThreads[thread.deviceAddress] = thread
        CampusLog.d("BTManager", "Registered thread for ${thread.deviceAddress}")

        // Send handshake immediately after connecting
        val handshake = HandshakePayload(myUserId, myUsername, bluetoothAdapter.address)
        thread.enqueue(Packet(PacketType.HANDSHAKE.name, gson.toJson(handshake)))

        // Override disconnect callback to clean up
        scope.launch {
            // Thread will call onDisconnected already via ConnectedThread internals
        }
    }

    fun sendMessage(msg: Message) {
        val targetThread = connectedThreads.values.find { it.remoteUserId == msg.receiverId }
        val packet = Packet(PacketType.MESSAGE.name, gson.toJson(msg.copy(status = MessageStatus.SENDING.name)))

        scope.launch {
            if (targetThread != null) {
                relayEngine.sendWithRetry(packet, targetThread)
            } else {
                // Flood-relay to all — DTN store-and-forward
                val threads = connectedThreads.values.toList()
                if (threads.isEmpty()) {
                    CampusLog.w("BTManager", "No peers connected — storing message as PENDING")
                    repository.storePendingMessage(msg.copy(status = MessageStatus.PENDING.name))
                } else {
                    threads.forEach { it.enqueue(packet) }
                    CampusLog.d("BTManager", "Flooded message ${msg.messageId} to ${threads.size} peers")
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
