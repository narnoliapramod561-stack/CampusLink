package com.campuslink.data.repository

import com.campuslink.data.local.MessageDao
import com.campuslink.data.local.UserDao
import com.campuslink.domain.model.Message
import com.campuslink.domain.model.MessageStatus
import com.campuslink.domain.model.NetworkStats
import com.campuslink.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val userDao: UserDao
) {
    private val _stats = MutableStateFlow(NetworkStats())
    val networkStats: StateFlow<NetworkStats> = _stats.asStateFlow()

    val messages: Flow<List<Message>> = messageDao.getAll()
    val users: Flow<List<User>> = userDao.getAll()

    suspend fun saveMessage(msg: Message) {
        messageDao.upsert(msg)
        _stats.update { it.copy(messagesSent = it.messagesSent + 1) }
    }

    suspend fun updateMessageStatus(id: String, status: String) {
        messageDao.updateStatus(id, status)
        if (status == MessageStatus.DELIVERED.name) {
            _stats.update { it.copy(messagesDelivered = it.messagesDelivered + 1) }
        }
    }

    suspend fun storePendingMessage(msg: Message) = messageDao.upsert(msg)

    suspend fun getPendingMessagesFor(userId: String) = messageDao.getPendingFor(userId)

    suspend fun upsertUser(user: User) = userDao.upsert(user)

    suspend fun setUserOnline(userId: String, online: Boolean) =
        userDao.setOnline(userId, online)

    fun getConversation(userId: String) = messageDao.getConversation(userId)

    fun onMessageRelayed(hopCount: Int) {
        _stats.update {
            val total = it.messagesRelayed + 1
            it.copy(
                messagesRelayed = total,
                avgHopCount = (it.avgHopCount * it.messagesRelayed + hopCount) / total
            )
        }
    }

    fun onNodeConnected() = _stats.update { it.copy(activeNodes = it.activeNodes + 1) }
    fun onNodeDisconnected() = _stats.update { it.copy(activeNodes = maxOf(0, it.activeNodes - 1)) }
}
