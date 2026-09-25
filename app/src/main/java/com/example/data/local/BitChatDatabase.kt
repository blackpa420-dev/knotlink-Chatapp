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
    version = 14,
    exportSchema = false
)
abstract class BitChatDatabase : RoomDatabase() {
    abstract fun bitChatDao(): BitChatDao

    companion object {
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
                    .addMigrations(MIGRATION_13_14)
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

