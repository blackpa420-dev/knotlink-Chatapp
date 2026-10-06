package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserIdentityEntity::class,
        ContactEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        GroupMemberEntity::class,
        ReactionEntity::class,
        PinnedMessageEntity::class,
        BlockedUserEntity::class,
        UserSessionEntity::class,
        CallLogEntity::class,
        CachedProfileEntity::class,
        SyncStateEntity::class
    ],
    version = 15,
    exportSchema = false
)
abstract class BitChatDatabase : RoomDatabase() {
    abstract fun bitChatDao(): BitChatDao

    companion object {
    private val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            // Durable local idempotency boundary: one authoritative server UUID
            // can exist only once in Room. NULL remains allowed for optimistic rows.
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_messages_serverMessageId ON messages(serverMessageId)"
            )
        }
    }

    private val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS cached_profiles (uid TEXT NOT NULL PRIMARY KEY, username TEXT NOT NULL, fullName TEXT NOT NULL, avatarUrl TEXT, bio TEXT NOT NULL, profession TEXT NOT NULL, email TEXT NOT NULL, lastSeen INTEGER NOT NULL, isOnline INTEGER NOT NULL, updatedAt INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS sync_state (key TEXT NOT NULL PRIMARY KEY, lastSyncedAt INTEGER NOT NULL)"
            )
        }
    }

        @Volatile
        private var INSTANCE: BitChatDatabase? = null

        fun getDatabase(context: Context): BitChatDatabase {
            Log.d("BitChat_Debug", "BitChatDatabase.getDatabase called")
            return INSTANCE ?: synchronized(this) {
                try {
                    Log.d("BitChat_Debug", "Building Room database instance bitchat_database...")
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        BitChatDatabase::class.java,
                        "bitchat_database"
                    )
                     .addMigrations(MIGRATION_13_14, MIGRATION_14_15)
                    // Older builds existed without a complete migration chain.
                    // Rebuild only from legacy starting versions instead of crashing.
                    .fallbackToDestructiveMigrationFrom(false, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
                    .build()
                    INSTANCE = instance
                    Log.d("BitChat_Debug", "Room database instance built successfully")
                    instance
                } catch (e: Exception) {
                    Log.e("BitChat_Debug", "Error building BitChatDatabase: ${e.message}", e)
                    throw e
                }
            }
        }
    }
}

