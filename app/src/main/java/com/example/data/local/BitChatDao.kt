package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BitChatDao {


    @Query("SELECT * FROM cached_profiles WHERE uid = :uid LIMIT 1")
    suspend fun getCachedProfile(uid: String): CachedProfileEntity?

    @Query("SELECT * FROM cached_profiles WHERE username = :username COLLATE NOCASE LIMIT 1")
    suspend fun getCachedProfileByUsername(username: String): CachedProfileEntity?

    @Query("SELECT * FROM cached_profiles WHERE email = :email COLLATE NOCASE LIMIT 1")
    suspend fun getCachedProfileByEmail(email: String): CachedProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedProfile(profile: CachedProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedProfiles(profiles: List<CachedProfileEntity>)

    @Query("SELECT * FROM cached_profiles")
    suspend fun getAllCachedProfiles(): List<CachedProfileEntity>

    @Query("DELETE FROM cached_profiles")
    suspend fun clearCachedProfiles()

    @Query("SELECT * FROM sync_state WHERE key = :key LIMIT 1")
    suspend fun getSyncState(key: String): SyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncState(state: SyncStateEntity)

    @Query("DELETE FROM sync_state WHERE key = :key")
    suspend fun deleteSyncState(key: String)

    @Query("DELETE FROM sync_state")
    suspend fun clearSyncStates()

    @Query("SELECT MAX(timestamp) FROM messages WHERE chatId = :chatId")
    suspend fun getLatestLocalMessageTimestamp(chatId: String): Long?

    @Query("SELECT * FROM user_identity WHERE id = 1 LIMIT 1")
    fun getUserIdentity(): Flow<UserIdentityEntity?>

    @Query("SELECT * FROM user_identity WHERE id = 1 LIMIT 1")
    fun getUserIdentitySync(): UserIdentityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserIdentity(user: UserIdentityEntity)

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Query("SELECT * FROM chats ORDER BY lastUpdated DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats ORDER BY lastUpdated DESC")
    suspend fun getAllChatsList(): List<ChatEntity>

    @Query("UPDATE messages SET chatId = :newChatId WHERE chatId = :oldChatId")
    suspend fun updateMessageChatId(oldChatId: String, newChatId: String)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    suspend fun getAllContactsList(): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC, id ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND text = :text AND timestampString = :timestampString LIMIT 1")
    suspend fun findExistingMessage(chatId: String, text: String, timestampString: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND text = :text AND abs(timestamp - :timestampMs) <= 30000 LIMIT 1")
    suspend fun findRecentMessage(chatId: String, text: String, timestampMs: Long): MessageEntity?

    @Query("SELECT * FROM messages WHERE serverMessageId = :serverMessageId LIMIT 1")
    suspend fun findMessageByServerId(serverMessageId: String): MessageEntity?

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearMessagesForChat(chatId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET isRead = 1, deliveryState = 'READ' WHERE chatId = :chatId AND isFromUser = 0")
    suspend fun markUserMessagesAsRead(chatId: String)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun resetChatUnreadCount(chatId: String)

    @Query("UPDATE messages SET deliveryState = 'READ', isRead = 1 WHERE serverMessageId IN (:serverMessageIds)")
    suspend fun markMessagesAsReadByServerIds(serverMessageIds: List<String>)

    @Query("""
        UPDATE messages SET
            deliveryState = CASE
                WHEN :deliveryState = 'READ' THEN 'READ'
                WHEN :deliveryState = 'DELIVERED' AND deliveryState != 'READ' THEN 'DELIVERED'
                WHEN :deliveryState = 'SENT' AND deliveryState NOT IN ('DELIVERED', 'READ') THEN 'SENT'
                WHEN :deliveryState = 'FAILED' AND deliveryState NOT IN ('DELIVERED', 'READ') THEN 'FAILED'
                ELSE deliveryState
            END,
            isRead = CASE
                WHEN :deliveryState = 'READ' THEN 1
                ELSE isRead
            END
        WHERE serverMessageId = :serverMessageId OR clientMessageId = :serverMessageId
    """)
    suspend fun updateMessageDeliveryStateByServerId(serverMessageId: String, deliveryState: String)

    @Query("UPDATE messages SET isRead = :isRead WHERE id = :messageId")
    suspend fun updateMessageReadStatus(messageId: Long, isRead: Boolean)

    @Query("DELETE FROM user_identity")
    suspend fun clearUserIdentity()

    @Query("DELETE FROM chats")
    suspend fun clearAllChats()

    @Query("DELETE FROM contacts")
    suspend fun clearAllContacts()

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()

    @Query("SELECT * FROM group_members WHERE chatId = :chatId")
    fun getMembersForChat(chatId: String): Flow<List<GroupMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMembers(members: List<GroupMemberEntity>)

    @Query("DELETE FROM group_members WHERE chatId = :chatId")
    suspend fun clearMembersForChat(chatId: String)

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: Long): MessageEntity?

    @Query("SELECT * FROM messages WHERE serverMessageId = :serverMessageId OR clientMessageId = :clientMessageId LIMIT 1")
    suspend fun getMessageByServerOrClientId(serverMessageId: String?, clientMessageId: String?): MessageEntity?

    @Query("UPDATE messages SET serverMessageId = :serverMessageId, syncStatus = 'SYNCED', deliveryState = 'SENT' WHERE id = :rowId OR (clientMessageId IS NOT NULL AND clientMessageId = :clientMessageId AND clientMessageId != '')")
    suspend fun updateMessageServerId(rowId: Long, clientMessageId: String, serverMessageId: String)

    @Query("UPDATE messages SET text = :newText, isEdited = 1, editedAt = :editedAt WHERE serverMessageId = :serverMessageId OR clientMessageId = :serverMessageId OR CAST(id AS TEXT) = :serverMessageId")
    suspend fun updateMessageText(serverMessageId: String, newText: String, editedAt: Long)

    @Query("UPDATE messages SET text = :newText, isEdited = 1, editedAt = :editedAt WHERE id = :rowId OR serverMessageId = :targetId OR clientMessageId = :targetId")
    suspend fun updateMessageTextWithRowId(rowId: Long, targetId: String, newText: String, editedAt: Long)

    @Query("UPDATE messages SET isDeletedForEveryone = 1, deletedAt = :deletedAt WHERE serverMessageId = :serverMessageId OR clientMessageId = :serverMessageId")
    suspend fun updateMessageDeletedForEveryone(serverMessageId: String, deletedAt: Long)

    @Query("UPDATE messages SET isDeletedForMe = 1 WHERE id = :messageId")
    suspend fun updateMessageDeletedForMe(messageId: Long)

    @Query("UPDATE messages SET isPinned = :isPinned, pinnedAt = :pinnedAt WHERE serverMessageId = :serverMessageId OR clientMessageId = :serverMessageId")
    suspend fun updateMessagePinnedStatus(serverMessageId: String, isPinned: Boolean, pinnedAt: Long?)

    @Query("UPDATE messages SET reactionsJson = :reactionsJson WHERE serverMessageId = :serverMessageId OR clientMessageId = :serverMessageId")
    suspend fun updateMessageReactionsSummary(serverMessageId: String, reactionsJson: String?)

    @Query("SELECT * FROM message_reactions WHERE messageId = :messageId")
    fun getReactionsForMessage(messageId: String): Flow<List<ReactionEntity>>

    @Query("SELECT * FROM message_reactions WHERE chatId = :chatId")
    fun getReactionsForChat(chatId: String): Flow<List<ReactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReaction(reaction: ReactionEntity)

    @Query("DELETE FROM message_reactions WHERE messageId = :messageId AND uid = :uid AND emoji = :emoji")
    suspend fun deleteReaction(messageId: String, uid: String, emoji: String)

    @Query("DELETE FROM message_reactions WHERE messageId = :messageId")
    suspend fun clearReactionsForMessage(messageId: String)

    @Query("SELECT * FROM pinned_messages WHERE chatId = :chatId ORDER BY pinnedAt DESC")
    fun getPinnedMessagesForChat(chatId: String): Flow<List<PinnedMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPinnedMessage(pinned: PinnedMessageEntity)

    @Query("DELETE FROM pinned_messages WHERE chatId = :chatId AND messageId = :messageId")
    suspend fun deletePinnedMessage(chatId: String, messageId: String)

    @Query("DELETE FROM pinned_messages WHERE chatId = :chatId")
    suspend fun clearPinnedMessagesForChat(chatId: String)

    @Query("UPDATE chats SET isMuted = :isMuted WHERE id = :chatId")
    suspend fun updateChatMuteStatus(chatId: String, isMuted: Boolean)

    @Query("UPDATE group_members SET role = :newRole WHERE chatId = :chatId AND uid = :uid")
    suspend fun updateGroupMemberRole(chatId: String, uid: String, newRole: String)

    @Query("DELETE FROM group_members WHERE chatId = :chatId AND uid = :uid")
    suspend fun deleteGroupMember(chatId: String, uid: String)

    @Query("UPDATE chats SET ownerUid = :newOwnerUid WHERE id = :chatId")
    suspend fun updateChatOwner(chatId: String, newOwnerUid: String)

    @Query("UPDATE chats SET name = :name, description = :description, avatarType = :avatarType, permissions = :permissions, lastUpdated = :lastUpdated WHERE id = :chatId")
    suspend fun updateGroupDetails(chatId: String, name: String, description: String?, avatarType: String, permissions: String?, lastUpdated: Long)

    @Query("UPDATE chats SET participantUids = :participantUids WHERE id = :chatId")
    suspend fun updateChatParticipants(chatId: String, participantUids: String)

    @Query("UPDATE user_identity SET privacySettingsJson = :privacySettingsJson WHERE id = 1")
    suspend fun updatePrivacySettings(privacySettingsJson: String)

    @Query("SELECT * FROM blocked_users ORDER BY blockedAt DESC")
    fun getAllBlockedUsers(): Flow<List<BlockedUserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedUser(blockedUser: BlockedUserEntity)

    @Query("DELETE FROM blocked_users WHERE targetUid = :targetUid")
    suspend fun deleteBlockedUser(targetUid: String)

    @Query("DELETE FROM blocked_users")
    suspend fun clearBlockedUsers()

    @Query("SELECT * FROM user_sessions ORDER BY lastActiveTimestamp DESC")
    fun getAllUserSessions(): Flow<List<UserSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSessions(sessions: List<UserSessionEntity>)

    @Query("DELETE FROM user_sessions WHERE deviceId = :deviceId")
    suspend fun deleteUserSession(deviceId: String)

    @Query("DELETE FROM user_sessions")
    suspend fun clearUserSessions()

    @Query("SELECT * FROM call_logs ORDER BY timestampMillis DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLogEntity)

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteCallLog(id: String)

    @Query("DELETE FROM call_logs WHERE id IN (:ids)")
    suspend fun deleteCallLogsByIds(ids: List<String>)

    @Query("DELETE FROM call_logs")
    suspend fun clearCallLogs()

    @Query("UPDATE call_logs SET durationSeconds = :duration WHERE id = :id")
    suspend fun updateCallLogDuration(id: String, duration: Int)
}
