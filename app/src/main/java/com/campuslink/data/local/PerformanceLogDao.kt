package com.campuslink.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.campuslink.domain.model.PerformanceLog
import kotlinx.coroutines.flow.Flow

@Dao interface PerformanceLogDao {
    @Upsert suspend fun insert(log: PerformanceLog)
    @Query("SELECT * FROM perf_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecent(): Flow<List<PerformanceLog>>
    @Query("SELECT AVG(latencyMs) FROM perf_logs WHERE timestamp > :since")
    suspend fun avgLatencySince(since: Long): Long?
    @Query("SELECT AVG(CASE WHEN success=1 THEN 1.0 ELSE 0.0 END) FROM perf_logs WHERE timestamp > :since")
    suspend fun successRateSince(since: Long): Float?
    @Query("SELECT COUNT(*) FROM perf_logs")
    suspend fun count(): Int
    @Query("DELETE FROM perf_logs")
    suspend fun clearAll()
}
