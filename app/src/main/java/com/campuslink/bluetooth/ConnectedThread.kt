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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
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

    // ── BUG B5 FIX ────────────────────────────────────────────────────────
    // Guard against disconnect() being called multiple times concurrently
    // (e.g. heartbeat timeout + IOException on read firing simultaneously)
    // Without this, onDisconnected() is called twice → double removal from
    // connectedThreads, double onNodeDisconnected(), corrupted stats.
    // ──────────────────────────────────────────────────────────────────────
    private val disconnected = AtomicBoolean(false)

    // Channel-based send queue — prevents concurrent OutputStream corruption
    private val sendChannel = Channel<Packet>(capacity = Channel.UNLIMITED)

    private val writerJob = scope.launch(Dispatchers.IO) {
        for (packet in sendChannel) {
            try {
                writeToStream(socket.outputStream, packet)
            } catch (e: IOException) {
                CampusLog.e("ConnThread", "Write failed: ${e.message}")
                disconnect()
                break
            }
        }
    }

    private val readerJob = scope.launch(Dispatchers.IO) {
        try {
            while (isActive) {
                val packet = readPacket(socket.inputStream)
                relayEngine.onPacketReceived(packet, this@ConnectedThread)
            }
        } catch (e: IOException) {
            CampusLog.e("ConnThread", "Read failed: ${e.message}")
            disconnect()
        }
    }

    private val heartbeatJob = scope.launch(Dispatchers.IO) {
        while (isActive) {
            kotlinx.coroutines.delay(Constants.HEARTBEAT_INTERVAL_MS)
            enqueue(Packet(PacketType.PING.name, ""))
            CampusLog.d("Heartbeat", "PING sent to $deviceAddress")
            if (System.currentTimeMillis() - lastPongTime > Constants.HEARTBEAT_TIMEOUT_MS) {
                CampusLog.w("Heartbeat", "Timeout — disconnecting $deviceAddress")
                disconnect()
                break
            }
        }
    }

    fun enqueue(packet: Packet) {
        sendChannel.trySend(packet)
    }

    fun updatePongTime() {
        lastPongTime = System.currentTimeMillis()
    }

    fun disconnect() {
        // ── B5 FIX: compareAndSet ensures this block runs exactly once ───
        if (!disconnected.compareAndSet(false, true)) return

        writerJob.cancel()
        readerJob.cancel()
        heartbeatJob.cancel()
        sendChannel.close()
        try { socket.close() } catch (_: IOException) {}
        onDisconnected(this)
    }

    companion object {
        fun writeToStream(outputStream: OutputStream, packet: Packet) {
            val json = Gson().toJson(packet).toByteArray(Charsets.UTF_8)
            val lenBytes = ByteBuffer.allocate(4).putInt(json.size).array()
            outputStream.write(lenBytes)   // 4 bytes: big-endian length
            outputStream.write(json)       // N bytes: JSON payload
            outputStream.flush()
        }

        fun readPacket(inputStream: InputStream): Packet {
            val dis = DataInputStream(inputStream)
            val lenBuf = ByteArray(4)
            dis.readFully(lenBuf)                          // blocks until exactly 4 bytes
            val len = ByteBuffer.wrap(lenBuf).int
            val payloadBuf = ByteArray(len)
            dis.readFully(payloadBuf)                      // blocks until exactly len bytes
            return Gson().fromJson(String(payloadBuf, Charsets.UTF_8), Packet::class.java)
        }
    }
}
