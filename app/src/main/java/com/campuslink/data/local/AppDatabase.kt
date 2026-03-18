package com.campuslink.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.campuslink.domain.model.LpuGroup
import com.campuslink.domain.model.Message
import com.campuslink.domain.model.PerformanceLog
import com.campuslink.domain.model.User

@Database(
    entities = [Message::class, User::class, PerformanceLog::class, LpuGroup::class],
    version = 2,
    exportSchema = false
)
// @TypeConverters(Converters::class) // Will uncomment if Converters class exists, but it was not present before, and not defined in prompt. I will leave it out unless needed.
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun userDao(): UserDao
    abstract fun performanceLogDao(): PerformanceLogDao
    abstract fun groupDao(): GroupDao

    companion object {
        // Migration: add new columns to messages and users tables
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Messages table new columns
                db.execSQL("ALTER TABLE messages ADD COLUMN targetType TEXT NOT NULL DEFAULT 'USER'")
                db.execSQL("ALTER TABLE messages ADD COLUMN priority TEXT NOT NULL DEFAULT 'NORMAL'")
                db.execSQL("ALTER TABLE messages ADD COLUMN senderZone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE messages ADD COLUMN senderRole TEXT NOT NULL DEFAULT 'STUDENT'")
                db.execSQL("ALTER TABLE messages ADD COLUMN groupId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE messages ADD COLUMN pathHistory TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE messages ADD COLUMN deliveryConfidence REAL NOT NULL DEFAULT 1.0")
                // Users table new columns
                db.execSQL("ALTER TABLE users ADD COLUMN zone TEXT NOT NULL DEFAULT 'BLOCK_32'")
                db.execSQL("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'STUDENT'")
                db.execSQL("ALTER TABLE users ADD COLUMN reliabilityScore REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE users ADD COLUMN messagesRelayed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN department TEXT NOT NULL DEFAULT ''")
                // New groups table
                db.execSQL("""CREATE TABLE IF NOT EXISTS `groups` (
                    `groupId` TEXT PRIMARY KEY NOT NULL,
                    `name` TEXT NOT NULL,
                    `zone` TEXT NOT NULL DEFAULT '',
                    `createdBy` TEXT NOT NULL DEFAULT '',
                    `memberIds` TEXT NOT NULL DEFAULT '',
                    `createdAt` INTEGER NOT NULL,
                    `isChannel` INTEGER NOT NULL DEFAULT 0
                )""")
            }
        }
    }
}
