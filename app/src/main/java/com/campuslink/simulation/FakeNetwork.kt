package com.campuslink.simulation

import com.campuslink.domain.model.Packet

object FakeNetwork {
    private val devices = mutableMapOf<String, (Packet) -> Unit>()

    fun register(userId: String, receiver: (Packet) -> Unit) {
        devices[userId] = receiver
    }

    fun send(to: String, packet: Packet) {
        Thread {
            Thread.sleep(200)
            devices[to]?.invoke(packet)
        }.start()
    }
}
