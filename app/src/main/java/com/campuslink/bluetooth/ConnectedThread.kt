package com.campuslink.bluetooth

import android.bluetooth.BluetoothSocket
import com.campuslink.core.CampusLog
import com.campuslink.core.Constants
import com.campuslink.domain.model.Packet
import com.campuslink.domain.model.PacketType
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException

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
    
    // AtomicBoolean prevents double-disconnect
    private val disconnected = java.util.concurrent.atomic.AtomicBoolean(false)
    private val sendChannel = Channel<Packet>(capacity = Channel.UNLIMITED)

    private val writerJob = scope.launch(Dispatchers.IO) {
        for (packet in sendChannel) {
            try { writeToStream(socket.outputStream, packet) }
            catch (e: IOException) { CampusLog.e("ConnThread","Write failed: ${e.message}"); disconnect(); break }
        }
    }
    private val readerJob = scope.launch(Dispatchers.IO) {
        try { while (isActive) { val p = readPacket(socket.inputStream); relayEngine.onPacketReceived(p, this@ConnectedThread) } }
        catch (e: IOException) { CampusLog.e("ConnThread","Read failed: ${e.message}"); disconnect() }
    }
    private val heartbeatJob = scope.launch(Dispatchers.IO) {
        while (isActive) {
            delay(Constants.HEARTBEAT_INTERVAL_MS)
            enqueue(Packet(PacketType.PING.name, ""))
            if (System.currentTimeMillis() - lastPongTime > Constants.HEARTBEAT_TIMEOUT_MS) {
                CampusLog.w("Heartbeat","Timeout $deviceAddress"); disconnect(); break
            }
        }
    }
    fun enqueue(packet: Packet) { sendChannel.trySend(packet) }
    fun updatePongTime() { lastPongTime = System.currentTimeMillis() }
    fun disconnect() {
        if (!disconnected.compareAndSet(false, true)) return
        writerJob.cancel(); readerJob.cancel(); heartbeatJob.cancel()
        sendChannel.close()
        try { socket.close() } catch (_: IOException) {}
        onDisconnected(this)
    }
    companion object {
        fun writeToStream(out: java.io.OutputStream, packet: Packet) {
            val json = Gson().toJson(packet).toByteArray(Charsets.UTF_8)
            out.write(java.nio.ByteBuffer.allocate(4).putInt(json.size).array())
            out.write(json); out.flush()
        }
        fun readPacket(inp: java.io.InputStream): Packet {
            val dis = java.io.DataInputStream(inp)
            val lenBuf = ByteArray(4); dis.readFully(lenBuf)
            val payload = ByteArray(java.nio.ByteBuffer.wrap(lenBuf).int); dis.readFully(payload)
            return Gson().fromJson(String(payload, Charsets.UTF_8), Packet::class.java)
        }
    }
}
