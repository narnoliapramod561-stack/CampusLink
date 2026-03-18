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

    fun sendAck(toThread: ConnectedThread, originalMsg: Message) {
        val ack = AckPayload(
            messageId = originalMsg.messageId,
            originalSenderId = originalMsg.senderId,
            receiverUserId = originalMsg.receiverId
        )
        val packet = Packet(PacketType.ACK.name, gson.toJson(ack))
        toThread.enqueue(packet)
        CampusLog.d("ACK", "Sent ACK for ${originalMsg.messageId} back to ${toThread.deviceAddress}")
    }
}
