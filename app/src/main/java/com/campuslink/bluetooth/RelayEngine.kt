package com.campuslink.bluetooth

import com.campuslink.core.CampusLog
import com.campuslink.core.Constants
import com.campuslink.data.repository.ChatRepository
import com.campuslink.domain.model.AckPayload
import com.campuslink.domain.model.HandshakePayload
import com.campuslink.domain.model.Message
import com.campuslink.domain.model.MessageStatus
import com.campuslink.domain.model.Packet
import com.campuslink.domain.model.PacketType
import com.campuslink.domain.model.User
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
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
    var allThreads: () -> List<ConnectedThread> = { emptyList() }
    private val gson = Gson()
    // LRU bounded cache — O(1) duplicate suppression
    val processedIds: MutableMap<String, Long> = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(1000, 0.75f, true) {
            override fun removeEldestEntry(e: Map.Entry<String, Long>) =
                size > Constants.MAX_PROCESSED_IDS ||
                (System.currentTimeMillis() - e.value > Constants.PROCESSED_ID_EXPIRY_MS)
        }
    )

    fun onPacketReceived(packet: Packet, fromThread: ConnectedThread) {
        when (packet.type) {
            PacketType.MESSAGE.name -> {
                val msg = gson.fromJson(packet.payload, Message::class.java)
                if (processedIds.containsKey(msg.messageId)) {
                    CampusLog.d("Relay","Dup: ${msg.messageId}"); return
                }
                if (System.currentTimeMillis() > msg.expiry) {
                    CampusLog.w("Relay","Expired: ${msg.messageId}"); return
                }
                if (msg.ttl <= 0) {
                    CampusLog.w("Relay","TTL=0: ${msg.messageId}"); return
                }
                processedIds[msg.messageId] = System.currentTimeMillis()
                if (msg.receiverId == myUserId) {
                    CampusLog.d("Relay","Delivering: ${msg.messageId} hops=${msg.hopCount}")
                    scope.launch {
                        repository.saveMessage(msg.copy(status = MessageStatus.DELIVERED.name))
                        repository.logDelivery(msg, success = true)
                    }
                    ackHandler.sendAck(fromThread, msg)
                } else {
                    val relay = msg.copy(ttl = msg.ttl - 1, hopCount = msg.hopCount + 1)
                    val pkt = Packet(PacketType.MESSAGE.name, gson.toJson(relay))
                    CampusLog.d("Relay","Forwarding ${msg.messageId} ttl=${relay.ttl}")
                    allThreads().filter { it != fromThread }.forEach { it.enqueue(pkt) }
                    scope.launch { repository.onMessageRelayed(relay.hopCount) }
                }
            }
            PacketType.ACK.name -> {
                val ack = gson.fromJson(packet.payload, AckPayload::class.java)
                if (ack.originalSenderId == myUserId) {
                    CampusLog.d("ACK","Delivered: ${ack.messageId}")
                    scope.launch { repository.updateMessageStatus(ack.messageId, MessageStatus.DELIVERED.name) }
                } else {
                    allThreads().filter { it != fromThread }.forEach { it.enqueue(packet) }
                }
            }
            PacketType.PING.name -> fromThread.enqueue(Packet(PacketType.PONG.name, ""))
            PacketType.PONG.name -> { fromThread.updatePongTime(); CampusLog.d("Heartbeat","PONG ${fromThread.deviceAddress}") }
            PacketType.HANDSHAKE.name -> {
                val hs = gson.fromJson(packet.payload, HandshakePayload::class.java)
                fromThread.remoteUserId = hs.userId
                CampusLog.d("RFCOMM","Handshake: ${hs.username} @${hs.userId}")
                scope.launch {
                    repository.upsertUser(User(hs.userId, hs.username, hs.deviceAddress, true))
                    repository.onNodeConnected()
                    flushPending(hs.userId, fromThread)
                }
            }
        }
    }

    suspend fun sendWithRetry(packet: Packet, target: ConnectedThread?) {
        Constants.RETRY_DELAYS_MS.forEachIndexed { i, ms ->
            try { target?.enqueue(packet) ?: throw IOException("No thread"); return }
            catch (e: IOException) { CampusLog.w("Retry","Attempt ${i+1}"); delay(ms) }
        }
        CampusLog.e("Retry","Exhausted — PENDING")
        val msg = gson.fromJson(packet.payload, Message::class.java)
        repository.storePendingMessage(msg.copy(status = MessageStatus.PENDING.name))
    }

    private suspend fun flushPending(userId: String, thread: ConnectedThread) {
        val pending = repository.getPendingMessagesFor(userId)
        CampusLog.d("StoreForward","Flushing ${pending.size} for $userId")
        pending.forEach { thread.enqueue(Packet(PacketType.MESSAGE.name, gson.toJson(it))) }
    }
}
