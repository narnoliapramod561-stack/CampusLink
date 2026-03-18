package com.campuslink.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.campuslink.domain.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Upsert
    suspend fun upsert(message: Message)

    @Query("UPDATE messages SET status = :status WHERE messageId = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("SELECT * FROM messages WHERE (senderId = :userId OR receiverId = :userId) ORDER BY timestamp ASC")
    fun getConversation(userId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE receiverId = :userId AND status = 'PENDING'")
    suspend fun getPendingFor(userId: String): List<Message>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Message>>
}
