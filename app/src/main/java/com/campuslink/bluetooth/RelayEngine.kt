package com.campuslink.bluetooth

import com.campuslink.core.CampusLog
import com.campuslink.core.Constants
import com.campuslink.data.repository.ChatRepository
import com.campuslink.domain.model.AckPayload
import com.campuslink.domain.model.Message
import com.campuslink.domain.model.MessageStatus
import com.campuslink.domain.model.Packet
import com.campuslink.domain.model.PacketType
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelayEngine @Inject constructor(
    private val repository: ChatRepository,
    private val ackHandler: AckHandler,
    private val scope: CoroutineScope
) {
    var myUserId: String = ""

    // LRU Bounded Cache — prevents duplicate relay
    val processedIds: MutableMap<String, Long> = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(1000, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, Long>): Boolean =
                size > Constants.MAX_PROCESSED_IDS ||
                        (System.currentTimeMillis() - eldest.value > Constants.PROCESSED_ID_EXPIRY_MS)
        }
    )

    // Keeps track of all active connected threads (injected from BluetoothManager)
    var allThreads: () -> List<ConnectedThread> = { emptyList() }

    private val gson = Gson()

    fun onPacketReceived(packet: Packet, fromThread: ConnectedThread) {
        when (packet.type) {

            PacketType.MESSAGE.name -> {
                val msg = gson.fromJson(packet.payload, Message::class.java)

                // 1. Duplicate check — O(1)
                if (processedIds.containsKey(msg.messageId)) {
                    CampusLog.d("Relay", "Duplicate ignored: ${msg.messageId}")
                    return
                }
                // 2. Expiry check
                if (System.currentTimeMillis() > msg.expiry) {
                    CampusLog.w("Relay", "Expired: ${msg.messageId}")
                    return
                }
                // 3. TTL check
                if (msg.ttl <= 0) {
                    CampusLog.w("Relay", "TTL exhausted: ${msg.messageId}")
                    return
                }

                processedIds[msg.messageId] = System.currentTimeMillis()

                if (msg.receiverId == myUserId) {
                    // I am the destination
                    CampusLog.d("Relay", "Delivering to me: ${msg.messageId}")
                    scope.launch {
                        repository.saveMessage(msg.copy(status = MessageStatus.DELIVERED.name))
                    }
                    ackHandler.sendAck(fromThread, msg)
                } else {
                    // Relay to all peers EXCEPT who sent it
                    val relay = msg.copy(ttl = msg.ttl - 1, hopCount = msg.hopCount + 1)
                    val relayPacket = Packet(PacketType.MESSAGE.name, gson.toJson(relay))
                    CampusLog.d("Relay", "Forwarding ${msg.messageId} ttl=${relay.ttl}")
                    allThreads().filter { it != fromThread }.forEach { it.enqueue(relayPacket) }
                    scope.launch { repository.onMessageRelayed(relay.hopCount) }
                }
            }

            PacketType.ACK.name -> {
                val ack = gson.fromJson(packet.payload, AckPayload::class.java)
                if (ack.originalSenderId == myUserId) {
                    CampusLog.d("ACK", "Delivery confirmed for ${ack.messageId}")
                    scope.launch {
                        repository.updateMessageStatus(ack.messageId, MessageStatus.DELIVERED.name)
                    }
                } else {
                    // Forward ACK backwards toward original sender
                    allThreads().filter { it != fromThread }.forEach { it.enqueue(packet) }
                }
            }

            PacketType.PING.name -> {
                fromThread.enqueue(Packet(PacketType.PONG.name, ""))
            }

            PacketType.PONG.name -> {
                CampusLog.d("Heartbeat", "PONG from ${fromThread.deviceAddress}")
                fromThread.updatePongTime()
            }

            PacketType.HANDSHAKE.name -> {
                val hs = gson.fromJson(
                    packet.payload,
                    com.campuslink.domain.model.HandshakePayload::class.java
                )
                fromThread.remoteUserId = hs.userId
                CampusLog.d("RFCOMM", "Handshake from ${hs.username} @${hs.userId}")
                scope.launch {
                    repository.upsertUser(
                        com.campuslink.domain.model.User(
                            hs.userId,
                            hs.username,
                            hs.deviceAddress,
                            true
                        )
                    )
                    repository.onNodeConnected()
                    flushPendingMessages(hs.userId, fromThread)
                }
            }
        }
    }

    suspend fun sendWithRetry(packet: Packet, targetThread: ConnectedThread?) {
        Constants.RETRY_DELAYS_MS.forEachIndexed { attempt, delayMs ->
            try {
                targetThread?.enqueue(packet) ?: throw IOException("No thread")
                return
            } catch (e: IOException) {
                CampusLog.w("Retry", "Attempt ${attempt + 1} failed for ${packet.payload.take(40)}")
                delay(delayMs)
            }
        }
        CampusLog.e("Retry", "All retries exhausted — storing as PENDING")
        val msg = gson.fromJson(packet.payload, Message::class.java)
        repository.storePendingMessage(msg.copy(status = MessageStatus.PENDING.name))
    }

    suspend fun flushPendingMessages(userId: String, thread: ConnectedThread) {
        val pending = repository.getPendingMessagesFor(userId)
        CampusLog.d("StoreForward", "Flushing ${pending.size} pending for $userId")
        pending.forEach { msg ->
            thread.enqueue(Packet(PacketType.MESSAGE.name, gson.toJson(msg)))
        }
    }
}
