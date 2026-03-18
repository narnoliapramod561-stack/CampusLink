package com.campuslink.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.campuslink.domain.model.Message
import com.campuslink.domain.model.User
import com.campuslink.domain.model.PerformanceLog

@Database(
    entities = [Message::class, User::class, PerformanceLog::class],
    version = 1, exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun userDao(): UserDao
    abstract fun performanceLogDao(): PerformanceLogDao
}
