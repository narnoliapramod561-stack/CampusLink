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

    // All messages in a conversation with a specific user (sent or received)
    @Query("""
        SELECT * FROM messages 
        WHERE (senderId = :userId OR receiverId = :userId) 
        ORDER BY timestamp ASC
    """)
    fun getConversation(userId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE receiverId = :userId AND status = 'PENDING'")
    suspend fun getPendingFor(userId: String): List<Message>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Message>>

    // ── NEW: Conversation summaries for WhatsApp-style list ───────────────
    // For each unique chat partner, get their most recent message.
    // "partner" = the other person (not myUserId) in each conversation.
    // We union sent + received, group by partner, pick the latest message.
    @Query("""
        SELECT * FROM messages 
        WHERE messageId IN (
            SELECT messageId FROM (
                SELECT messageId,
                    CASE WHEN senderId = :myUserId THEN receiverId ELSE senderId END AS partner,
                    MAX(timestamp) AS maxTs
                FROM messages
                WHERE senderId = :myUserId OR receiverId = :myUserId
                GROUP BY partner
            )
        )
        ORDER BY timestamp DESC
    """)
    fun getConversationPreviews(myUserId: String): Flow<List<Message>>
}
