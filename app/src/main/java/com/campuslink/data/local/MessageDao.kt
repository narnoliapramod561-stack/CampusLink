package com.campuslink.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.campuslink.domain.model.Message
import kotlinx.coroutines.flow.Flow

@Dao interface MessageDao {
    @Upsert suspend fun upsert(message: Message)
    @Query("UPDATE messages SET status = :status WHERE messageId = :id")
    suspend fun updateStatus(id: String, status: String)
    @Query("SELECT * FROM messages WHERE (senderId=:userId OR receiverId=:userId) ORDER BY timestamp ASC")
    fun getConversation(userId: String): Flow<List<Message>>
    @Query("SELECT * FROM messages WHERE receiverId=:userId AND status='PENDING'")
    suspend fun getPendingFor(userId: String): List<Message>
    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Message>>
    // WhatsApp-style: one row per conversation (latest message per partner)
    @Query("""
        SELECT * FROM messages WHERE messageId IN (
            SELECT messageId FROM (
                SELECT messageId,
                    CASE WHEN senderId=:myUserId THEN receiverId ELSE senderId END AS partner,
                    MAX(timestamp) AS maxTs
                FROM messages WHERE senderId=:myUserId OR receiverId=:myUserId
                GROUP BY partner
            )
        ) ORDER BY timestamp DESC
    """)
    fun getConversationPreviews(myUserId: String): Flow<List<Message>>

    // Messages for a zone (ZONE type, targetValue = zone name)
    @Query("SELECT * FROM messages WHERE targetType='ZONE' AND receiverId=:zone ORDER BY timestamp DESC LIMIT 100")
    fun getZoneMessages(zone: String): Flow<List<Message>>

    // Broadcast messages (BROADCAST type)
    @Query("SELECT * FROM messages WHERE targetType='BROADCAST' ORDER BY timestamp DESC LIMIT 50")
    fun getBroadcasts(): Flow<List<Message>>

    // Group messages
    @Query("SELECT * FROM messages WHERE targetType='GROUP' AND groupId=:groupId ORDER BY timestamp ASC")
    fun getGroupMessages(groupId: String): Flow<List<Message>>

    // Unread count per partner
    @Query("SELECT COUNT(*) FROM messages WHERE receiverId=:myId AND status!='DELIVERED' AND senderId=:partnerId")
    fun getUnreadCount(myId: String, partnerId: String): Flow<Int>

    // Emergency messages received
    @Query("SELECT * FROM messages WHERE priority='EMERGENCY' AND receiverId=:myId ORDER BY timestamp DESC")
    fun getEmergencyMessages(myId: String): Flow<List<Message>>

    // All PENDING messages (for admin to see queued)
    @Query("SELECT * FROM messages WHERE status='PENDING' ORDER BY timestamp ASC")
    fun getAllPending(): Flow<List<Message>>
}
