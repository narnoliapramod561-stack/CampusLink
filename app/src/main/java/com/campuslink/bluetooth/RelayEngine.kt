package com.campuslink.bluetooth

import com.campuslink.core.CampusLog
import com.campuslink.core.Constants
import com.campuslink.data.repository.ChatRepository
import com.campuslink.domain.model.*
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelayEngine @Inject constructor(
    private val repository: ChatRepository,
    private val ackHandler: AckHandler,
    private val scope: CoroutineScope
) {
    var myUserId: String = ""
    var myZone: String = "BLOCK_32"
    var allThreads: () -> List<ConnectedThread> = { emptyList() }

    // FIX: Reference to BluetoothManager so HANDSHAKE can call onPeerIdentified.
    // This is set by BluetoothManager after construction (avoids circular DI).
    var bluetoothManager: BluetoothManager? = null

    private val gson = Gson()

    // LRU bounded cache — O(1) duplicate suppression
    val processedIds: MutableMap<String, Long> = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(1000, 0.75f, true) {
            override fun removeEldestEntry(e: Map.Entry<String, Long>) =
                size > Constants.MAX_PROCESSED_IDS ||
                (System.currentTimeMillis() - e.value > Constants.PROCESSED_ID_EXPIRY_MS)
        }
    )

    // Route memory cache
    private val routeCache = ConcurrentHashMap<String, RouteMemory>()

    fun onDeliverySuccess(receiverId: String, viaUserId: String) {
        val existing = routeCache[receiverId]
        routeCache[receiverId] = RouteMemory(
            receiverId = receiverId,
            bestNextHop = viaUserId,
            successCount = (existing?.successCount ?: 0) + 1,
            lastUpdated = System.currentTimeMillis()
        )
    }

    fun getBestThread(receiverId: String): ConnectedThread? {
        val cached = routeCache[receiverId]
        if (cached != null && System.currentTimeMillis() - cached.lastUpdated < 300_000L) {
            val cachedThread = allThreads().find { it.remoteUserId == cached.bestNextHop }
            if (cachedThread != null) {
                CampusLog.d("RouteCache", "Using cached route to $receiverId via ${cached.bestNextHop}")
                return cachedThread
            }
        }
        return allThreads().find { it.remoteUserId == receiverId }
    }

    fun calculateAdaptiveTtl(baseTtl: Int): Int {
        val peerCount = allThreads().size
        return when {
            peerCount >= 5 -> minOf(baseTtl, 3)
            peerCount >= 3 -> minOf(baseTtl, 5)
            else           -> baseTtl
        }
    }

    private fun appendPath(existing: String, userId: String): String {
        val list = if (existing.isEmpty()) mutableListOf()
                   else gson.fromJson(existing, Array<String>::class.java).toMutableList()
        if (!list.contains(userId)) list.add(userId)
        return gson.toJson(list)
    }

    private fun isLooping(pathHistory: String, myUserId: String): Boolean {
        if (pathHistory.isEmpty()) return false
        return try {
            gson.fromJson(pathHistory, Array<String>::class.java).contains(myUserId)
        } catch (_: Exception) { false }
    }

    fun onPacketReceived(packet: Packet, fromThread: ConnectedThread) {
        when (packet.type) {

            PacketType.MESSAGE.name -> {
                val msg = gson.fromJson(packet.payload, Message::class.java)

                if (processedIds.containsKey(msg.messageId)) {
                    CampusLog.d("Relay", "Dup dropped: ${msg.messageId}"); return
                }
                if (System.currentTimeMillis() > msg.expiry) {
                    CampusLog.w("Relay", "Expired: ${msg.messageId}"); return
                }
                if (msg.ttl <= 0) {
                    CampusLog.w("Relay", "TTL=0: ${msg.messageId}"); return
                }
                if (isLooping(msg.pathHistory, myUserId)) {
                    CampusLog.d("Relay", "Loop detected: ${msg.messageId}"); return
                }

                processedIds[msg.messageId] = System.currentTimeMillis()

                when (msg.targetType) {
                    MessageTargetType.BROADCAST.name -> {
                        scope.launch { repository.saveMessage(msg.copy(status = MessageStatus.DELIVERED.name)) }
                        relay(msg, fromThread)
                        return
                    }
                    MessageTargetType.ZONE.name -> {
                        if (myZone == msg.receiverId) {
                            scope.launch { repository.saveMessage(msg.copy(status = MessageStatus.DELIVERED.name)) }
                        }
                        relay(msg, fromThread)
                        return
                    }
                    MessageTargetType.GROUP.name -> {
                        scope.launch {
                            val group = repository.getGroupById(msg.groupId)
                            if (group != null) {
                                val members = try {
                                    gson.fromJson(group.memberIds, Array<String>::class.java).toList()
                                } catch (_: Exception) { emptyList() }
                                if (myUserId in members) {
                                    repository.saveMessage(msg.copy(status = MessageStatus.DELIVERED.name))
                                }
                            }
                        }
                        relay(msg, fromThread)
                        return
                    }
                }

                // USER type
                if (msg.receiverId == myUserId) {
                    CampusLog.d("Relay", "Delivering to me: ${msg.messageId} hops=${msg.hopCount}")
                    scope.launch {
                        repository.saveMessage(msg.copy(status = MessageStatus.DELIVERED.name))
                        repository.logDelivery(msg, success = true)
                    }
                    ackHandler.sendAck(fromThread, msg)
                } else {
                    relay(msg, fromThread)
                }
            }

            PacketType.ACK.name -> {
                val ack = gson.fromJson(packet.payload, AckPayload::class.java)
                if (ack.originalSenderId == myUserId) {
                    CampusLog.d("ACK", "Delivery confirmed: ${ack.messageId}")
                    scope.launch { repository.updateMessageStatus(ack.messageId, MessageStatus.DELIVERED.name) }
                    val viaUserId = fromThread.remoteUserId
                    if (viaUserId.isNotBlank()) onDeliverySuccess(ack.receiverUserId, viaUserId)
                } else {
                    allThreads().filter { it != fromThread }.forEach { it.enqueue(packet) }
                }
            }

            PacketType.PING.name -> fromThread.enqueue(Packet(PacketType.PONG.name, ""))

            PacketType.PONG.name -> {
                fromThread.updatePongTime()
                CampusLog.d("Heartbeat", "PONG from ${fromThread.deviceAddress}")
            }

            PacketType.HANDSHAKE.name -> {
                val hs = gson.fromJson(packet.payload, HandshakePayload::class.java)
                CampusLog.d("RFCOMM", "Handshake received: ${hs.username} @${hs.userId} zone=${hs.zone}")

                // FIX: Tell BluetoothManager we now know this socket's userId.
                // This updates the live connectedUserIds set → peer appears in Nearby immediately.
                fromThread.remoteUserId = hs.userId
                bluetoothManager?.onPeerIdentified(fromThread.deviceAddress, hs.userId)

                scope.launch {
                    repository.upsertUser(
                        User(
                            userId        = hs.userId,
                            username      = hs.username,
                            deviceAddress = hs.deviceAddress,
                            isOnline      = true,
                            zone          = hs.zone,
                            role          = hs.role,
                            department    = hs.department
                        )
                    )
                    repository.onNodeConnected()
                    flushPending(hs.userId, fromThread)
                }
            }
        }
    }

    private fun relay(msg: Message, fromThread: ConnectedThread) {
        val relayed = msg.copy(
            ttl         = msg.ttl - 1,
            hopCount    = msg.hopCount + 1,
            pathHistory = appendPath(msg.pathHistory, myUserId)
        )
        if (relayed.ttl <= 0) return
        val pkt = Packet(PacketType.MESSAGE.name, gson.toJson(relayed))
        CampusLog.d("Relay", "Forwarding ${msg.messageId} ttl=${relayed.ttl} to ${allThreads().size - 1} peers")
        allThreads().filter { it != fromThread }.forEach { it.enqueue(pkt) }
        scope.launch { repository.onMessageRelayed(relayed.hopCount) }
    }

    suspend fun sendWithRetry(packet: Packet, target: ConnectedThread?) {
        Constants.RETRY_DELAYS_MS.forEachIndexed { i, ms ->
            try {
                // FIX: Check Boolean result — trySend() on a closed channel returns false
                // without throwing, so retries never triggered and PENDING was never set.
                val success = target?.enqueue(packet) ?: false
                if (!success) throw IOException("Channel full or closed")
                return
            } catch (e: IOException) {
                CampusLog.w("Retry", "Attempt ${i + 1} failed"); delay(ms)
            }
        }
        CampusLog.e("Retry", "All retries exhausted — storing as PENDING")
        val msg = gson.fromJson(packet.payload, Message::class.java)
        repository.storePendingMessage(msg.copy(status = MessageStatus.PENDING.name))
    }

    private suspend fun flushPending(userId: String, thread: ConnectedThread) {
        val pending = repository.getPendingMessagesFor(userId)
        CampusLog.d("StoreForward", "Flushing ${pending.size} pending messages for $userId")
        pending.forEach { thread.enqueue(Packet(PacketType.MESSAGE.name, gson.toJson(it))) }
    }
}
