package com.campuslink.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.campuslink.domain.model.User
import kotlinx.coroutines.flow.Flow

@Dao interface UserDao {
    @Upsert suspend fun upsert(user: User)
    @Query("UPDATE users SET isOnline=:online, lastSeen=:time WHERE userId=:id")
    suspend fun setOnline(id: String, online: Boolean, time: Long = System.currentTimeMillis())
    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAll(): Flow<List<User>>
    @Query("SELECT * FROM users WHERE userId=:id")
    suspend fun getById(id: String): User?
    @Query("UPDATE users SET rssi=:rssi WHERE userId=:id")
    suspend fun updateRssi(id: String, rssi: Int)

    @Query("SELECT * FROM users WHERE zone=:zone AND isOnline=1")
    fun getUsersInZone(zone: String): Flow<List<User>>

    @Query("SELECT COUNT(*) FROM users WHERE zone=:zone AND isOnline=1")
    fun countInZone(zone: String): Flow<Int>

    @Query("UPDATE users SET reliabilityScore=:score, messagesRelayed=messagesRelayed+1 WHERE userId=:id")
    suspend fun updateReliability(id: String, score: Float)

    @Query("SELECT * FROM users WHERE role='ADMIN' OR role='FACULTY'")
    fun getStaff(): Flow<List<User>>
}
