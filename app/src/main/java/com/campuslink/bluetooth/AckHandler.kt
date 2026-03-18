package com.campuslink.bluetooth

import com.campuslink.core.CampusLog
import com.campuslink.domain.model.AckPayload
import com.campuslink.domain.model.Message
import com.campuslink.domain.model.Packet
import com.campuslink.domain.model.PacketType
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AckHandler @Inject constructor() {
    private val gson = Gson()
    fun sendAck(toThread: ConnectedThread, msg: Message) {
        val ack = AckPayload(msg.messageId, msg.senderId, msg.receiverId)
        toThread.enqueue(Packet(PacketType.ACK.name, gson.toJson(ack)))
        CampusLog.d("ACK","Sent ACK for ${msg.messageId}")
    }
}
