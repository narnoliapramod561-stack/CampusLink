package com.campuslink.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.campuslink.domain.model.LpuGroup
import kotlinx.coroutines.flow.Flow

@Dao interface GroupDao {
    @Upsert suspend fun upsert(group: LpuGroup)
    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    fun getAll(): Flow<List<LpuGroup>>
    @Query("SELECT * FROM groups WHERE groupId = :id")
    suspend fun getById(id: String): LpuGroup?
    @Query("SELECT * FROM groups WHERE isChannel = 1")
    fun getChannels(): Flow<List<LpuGroup>>
    @Query("DELETE FROM groups WHERE groupId = :id")
    suspend fun delete(id: String)
}
