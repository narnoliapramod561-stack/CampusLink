package com.campuslink.simulation

import android.util.Log
import com.campuslink.domain.model.AckPayload
import com.campuslink.domain.model.Message
import com.campuslink.domain.model.MessageStatus
import com.campuslink.domain.model.Packet
import com.campuslink.domain.model.PacketType
import com.google.gson.Gson
import java.util.UUID

class VirtualDevice(val userId: String) {
    private val gson = Gson()
    private val processedIds = mutableSetOf<String>()

    init {
        FakeNetwork.register(userId) { packet -> handlePacket(packet) }
        Log.d("Sim", "VirtualDevice $userId registered.")
    }

    fun sendMessage(targetUserId: String, text: String) {
        val msg = Message(
            messageId = UUID.randomUUID().toString(),
            senderId = userId,
            receiverId = targetUserId,
            content = text,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENDING.name
        )
        val packet = Packet(PacketType.MESSAGE.name, gson.toJson(msg))
        
        Log.d("Sim", "$userId starting send of ${msg.messageId} to $targetUserId")
        
        val nextHop = getNextHop(userId)
        if (nextHop.isNotEmpty()) {
            Log.d("Sim", "$userId forwarding message to $nextHop")
            FakeNetwork.send(nextHop, packet)
        } else {
            Log.w("Sim", "$userId has no route to $targetUserId")
        }
    }

    private fun handlePacket(packet: Packet) {
        when (packet.type) {
            PacketType.MESSAGE.name -> {
                val msg = gson.fromJson(packet.payload, Message::class.java)
                if (!processedIds.add(msg.messageId)) return // Ignore duplicates

                if (msg.receiverId == userId) {
                    Log.d("Sim", "$userId received MESSAGE: ${msg.content} from ${msg.senderId}")
                    handleAck(msg, getPreviousHop(userId))
                } else {
                    Log.d("Sim", "$userId forwarding message towards ${msg.receiverId}")
                    val next = getNextHop(userId)
                    if (next.isNotEmpty()) {
                        val relay = msg.copy(ttl = msg.ttl - 1, hopCount = msg.hopCount + 1)
                        if (relay.ttl > 0) {
                            FakeNetwork.send(next, Packet(PacketType.MESSAGE.name, gson.toJson(relay)))
                        } else {
                            Log.w("Sim", "$userId dropping message: TTL exhausted")
                        }
                    } else {
                        Log.w("Sim", "$userId dropping message: no route")
                    }
                }
            }
            PacketType.ACK.name -> {
                val ack = gson.fromJson(packet.payload, AckPayload::class.java)
                if (ack.originalSenderId == userId) {
                    Log.d("Sim", "$userId received ACK for ${ack.messageId}")
                } else {
                    Log.d("Sim", "$userId forwarding ACK for ${ack.messageId}")
                    val prev = getPreviousHop(userId)
                    if (prev.isNotEmpty()) {
                        FakeNetwork.send(prev, packet)
                    } else {
                        Log.w("Sim", "$userId dropping ACK: no route")
                    }
                }
            }
        }
    }

    private fun handleAck(msg: Message, nextHop: String) {
        val ackPayload = AckPayload(
            messageId = msg.messageId,
            originalSenderId = msg.senderId,
            receiverUserId = msg.receiverId
        )
        val packet = Packet(PacketType.ACK.name, gson.toJson(ackPayload))
        if (nextHop.isNotEmpty()) {
            FakeNetwork.send(nextHop, packet)
        }
    }
}
