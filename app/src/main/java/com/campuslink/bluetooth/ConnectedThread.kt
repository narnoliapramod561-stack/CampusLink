package com.campuslink.bluetooth

import android.bluetooth.BluetoothSocket
import com.campuslink.core.CampusLog
import com.campuslink.core.Constants
import com.campuslink.domain.model.Packet
import com.campuslink.domain.model.PacketType
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class ConnectedThread(
    val socket: BluetoothSocket,
    private val scope: CoroutineScope,
    private val relayEngine: RelayEngine,
    private val onDisconnected: (ConnectedThread) -> Unit
) {
    val deviceAddress: String = socket.remoteDevice.address
    var remoteUserId: String = ""
    private var lastPongTime = System.currentTimeMillis()
    val connectedAt: Long = System.currentTimeMillis()

    private val disconnected = AtomicBoolean(false)
    private val sendChannel = Channel<Packet>(capacity = Channel.UNLIMITED)

    private val writerJob = scope.launch(Dispatchers.IO) {
        for (packet in sendChannel) {
            try { writeToStream(socket.outputStream, packet) }
            catch (e: IOException) {
                CampusLog.e("ConnThread", "Write failed: ${e.message}")
                disconnect(); break
            }
        }
    }

    private val readerJob = scope.launch(Dispatchers.IO) {
        try {
            while (isActive) {
                val p = readPacket(socket.inputStream)
                updatePongTime() // Keep heartbeat alive on any traffic
                relayEngine.onPacketReceived(p, this@ConnectedThread)
            }
        } catch (e: Exception) {
            CampusLog.e("ConnThread", "Read failed for $deviceAddress: ${e.message}")
            disconnect()
        }
    }

    private val heartbeatJob = scope.launch(Dispatchers.IO) {
        while (isActive) {
            delay(Constants.HEARTBEAT_INTERVAL_MS)
            enqueue(Packet(PacketType.PING.name, "")) // return ignored — timeout handles dead peers
            if (System.currentTimeMillis() - lastPongTime > Constants.HEARTBEAT_TIMEOUT_MS) {
                CampusLog.w("Heartbeat", "Timeout $deviceAddress"); disconnect(); break
            }
        }
    }

    fun enqueue(packet: Packet): Boolean {
        if (disconnected.get()) return false
        return sendChannel.trySend(packet).isSuccess
    }

    fun updatePongTime() { lastPongTime = System.currentTimeMillis() }

    fun disconnect() {
        if (!disconnected.compareAndSet(false, true)) return
        writerJob.cancel(); readerJob.cancel(); heartbeatJob.cancel()
        sendChannel.close()
        try { socket.close() } catch (_: IOException) {}
        onDisconnected(this)
    }

    companion object {
        private val gson = Gson()

        fun writeToStream(out: java.io.OutputStream, packet: Packet) {
            val json = gson.toJson(packet).toByteArray(Charsets.UTF_8)
            // Write 4-byte length prefix then the payload
            out.write(ByteBuffer.allocate(4).putInt(json.size).array())
            out.write(json)
            out.flush()
        }

        fun readPacket(inp: java.io.InputStream): Packet {
            val dis = DataInputStream(inp)

            // Read exactly 4 bytes for the length header
            val lenBuf = ByteArray(4)
            dis.readFully(lenBuf)
            val len = ByteBuffer.wrap(lenBuf).int

            // CRITICAL: Prevent OutOfMemoryError if stream alignment shifts due to noise
            if (len <= 0 || len > 10_000_000) throw IOException("Invalid packet length: $len")

            val payload = ByteArray(len)
            dis.readFully(payload)
            return gson.fromJson(String(payload, Charsets.UTF_8), Packet::class.java)
        }
    }
}
