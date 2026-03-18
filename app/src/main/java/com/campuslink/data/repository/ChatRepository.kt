package com.campuslink.data.repository

import com.campuslink.data.local.MessageDao
import com.campuslink.data.local.PerformanceLogDao
import com.campuslink.data.local.UserDao
import com.campuslink.domain.model.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val perfLogDao: PerformanceLogDao
) {
    private val _stats = MutableStateFlow(NetworkStats())
    val networkStats: StateFlow<NetworkStats> = _stats.asStateFlow()
    val messages: Flow<List<Message>> = messageDao.getAll()
    val users: Flow<List<User>> = userDao.getAll()

    fun getConversationPreviews(myUserId: String): Flow<List<ConversationPreview>> =
        combine(messageDao.getConversationPreviews(myUserId), userDao.getAll()) { msgs, users ->
            val userMap = users.associateBy { it.userId }
            msgs.map { msg ->
                val pid = if (msg.senderId == myUserId) msg.receiverId else msg.senderId
                val p = userMap[pid]
                ConversationPreview(pid, p?.username ?: pid, msg.content, msg.timestamp, p?.isOnline ?: false)
            }
        }

    fun getRecentLogs(): Flow<List<PerformanceLog>> = perfLogDao.getRecent()

    suspend fun logDelivery(msg: Message, success: Boolean) {
        val log = PerformanceLog(
            id = java.util.UUID.randomUUID().toString(),
            messageId = msg.messageId,
            senderId = msg.senderId,
            receiverId = msg.receiverId,
            hops = msg.hopCount,
            latencyMs = System.currentTimeMillis() - msg.timestamp,
            routePath = "${msg.senderId}→${msg.receiverId}",
            timestamp = System.currentTimeMillis(),
            success = success
        )
        perfLogDao.insert(log)
        // Update rolling stats
        val since = System.currentTimeMillis() - 3_600_000L
        val avgLat = perfLogDao.avgLatencySince(since) ?: 0L
        val rate = perfLogDao.successRateSince(since) ?: 1f
        _stats.update { it.copy(avgLatencyMs = avgLat, relaySuccessRate = rate) }
    }

    suspend fun ensureUserExists(userId: String) {
        if (userDao.getById(userId) == null)
            userDao.upsert(User(userId, userId, "", false))
    }

    suspend fun saveMessage(msg: Message) {
        messageDao.upsert(msg)
        _stats.update { it.copy(messagesSent = it.messagesSent + 1) }
    }

    suspend fun updateMessageStatus(id: String, status: String) {
        messageDao.updateStatus(id, status)
        if (status == MessageStatus.DELIVERED.name)
            _stats.update { it.copy(messagesDelivered = it.messagesDelivered + 1) }
    }

    suspend fun storePendingMessage(msg: Message) {
        messageDao.upsert(msg)
        _stats.update { it.copy(messagesPending = it.messagesPending + 1) }
    }

    suspend fun getPendingMessagesFor(uid: String) = messageDao.getPendingFor(uid)
    suspend fun upsertUser(user: User) = userDao.upsert(user)
    suspend fun setUserOnline(uid: String, online: Boolean) = userDao.setOnline(uid, online)
    fun getConversation(uid: String) = messageDao.getConversation(uid)

    fun onMessageRelayed(hops: Int) = _stats.update {
        val t = it.messagesRelayed + 1
        it.copy(messagesRelayed = t, avgHopCount = (it.avgHopCount * it.messagesRelayed + hops) / t)
    }
    fun onNodeConnected() = _stats.update { it.copy(activeNodes = it.activeNodes + 1) }
    fun onNodeDisconnected() = _stats.update { it.copy(activeNodes = maxOf(0, it.activeNodes - 1)) }
    suspend fun clearLogs() { perfLogDao.clearAll() }
}
