package com.campuslink.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.campuslink.domain.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Upsert
    suspend fun upsert(user: User)

    @Query("UPDATE users SET isOnline = :online, lastSeen = :time WHERE userId = :id")
    suspend fun setOnline(id: String, online: Boolean, time: Long = System.currentTimeMillis())

    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAll(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE userId = :id")
    suspend fun getById(id: String): User?
}
