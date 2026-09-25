package com.example
 
import com.example.data.local.ChatEntity
import com.example.data.local.GroupMemberEntity
import com.example.data.local.MessageEntity
import com.example.data.local.PinnedMessageEntity
import com.example.data.local.ReactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedMessagingTest {

    @Test
    fun messageEntity_replyFields_areStoredCorrectly() {
        val originalMessage = MessageEntity(
            id = 101,
            chatId = "chat_abc",
            senderName = "Alice",
            text = "Hey there!",
            timestampString = "10:00 AM",
            isFromUser = false,
            serverMessageId = "srv_101"
        )

        val replyMessage = MessageEntity(
            id = 102,
            chatId = "chat_abc",
            senderName = "Bob",
            text = "Replying to you",
            timestampString = "10:01 AM",
            isFromUser = true,
            replyToMessageId = originalMessage.serverMessageId,
            replySnippet = originalMessage.text,
            replySenderName = originalMessage.senderName
        )

        assertEquals("srv_101", replyMessage.replyToMessageId)
        assertEquals("Hey there!", replyMessage.replySnippet)
        assertEquals("Alice", replyMessage.replySenderName)
    }

    @Test
    fun messageEntity_forwardFields_areStoredCorrectly() {
        val forwardedMsg = MessageEntity(
            id = 201,
            chatId = "chat_target",
            senderName = "Forwarder User",
            text = "Check this forwarded message",
            timestampString = "10:05 AM",
            isFromUser = true,
            isForwarded = true,
            forwardedFromMessageId = "srv_orig_99"
        )

        assertTrue(forwardedMsg.isForwarded)
        assertEquals("srv_orig_99", forwardedMsg.forwardedFromMessageId)
        assertEquals("Forwarder User", forwardedMsg.senderName)
    }

    @Test
    fun messageEntity_editFields_areStoredCorrectly() {
        val editTs = 1755180000000L
        val editedMsg = MessageEntity(
            id = 301,
            chatId = "chat_abc",
            senderName = "Bob",
            text = "Updated text after edit",
            timestampString = "10:10 AM",
            isFromUser = true,
            isEdited = true,
            editedAt = editTs
        )

        assertTrue(editedMsg.isEdited)
        assertEquals(editTs, editedMsg.editedAt)
        assertEquals("Updated text after edit", editedMsg.text)
    }

    @Test
    fun messageEntity_deleteFields_areStoredCorrectly() {
        val deleteTs = 1755181000000L
        val deletedForMeMsg = MessageEntity(
            id = 401,
            chatId = "chat_abc",
            senderName = "Alice",
            text = "Some message",
            timestampString = "10:15 AM",
            isFromUser = false,
            isDeletedForMe = true
        )

        val deletedForEveryoneMsg = MessageEntity(
            id = 402,
            chatId = "chat_abc",
            senderName = "Bob",
            text = "🚫 This message was deleted",
            timestampString = "10:16 AM",
            isFromUser = true,
            isDeletedForEveryone = true,
            deletedAt = deleteTs
        )

        assertTrue(deletedForMeMsg.isDeletedForMe)
        assertFalse(deletedForMeMsg.isDeletedForEveryone)

        assertTrue(deletedForEveryoneMsg.isDeletedForEveryone)
        assertEquals(deleteTs, deletedForEveryoneMsg.deletedAt)
        assertEquals("🚫 This message was deleted", deletedForEveryoneMsg.text)
    }

    @Test
    fun reactionEntity_and_pinnedMessageEntity_schemaIntegrity() {
        val reaction = ReactionEntity(
            messageId = "srv_msg_1",
            chatId = "chat_group_1",
            uid = "user_456",
            emoji = "🔥",
            createdAt = 1755182000000L
        )

        assertEquals("srv_msg_1", reaction.messageId)
        assertEquals("chat_group_1", reaction.chatId)
        assertEquals("user_456", reaction.uid)
        assertEquals("🔥", reaction.emoji)

        val pinned = PinnedMessageEntity(
            chatId = "chat_group_1",
            messageId = "srv_msg_1",
            pinnedByUid = "user_456",
            pinnedAt = 1755182500000L
        )

        assertEquals("chat_group_1", pinned.chatId)
        assertEquals("srv_msg_1", pinned.messageId)
        assertEquals("user_456", pinned.pinnedByUid)
    }

    @Test
    fun groupSystemEvent_and_mentions_schemaIntegrity() {
        val systemEvent = MessageEntity(
            id = 501,
            chatId = "group_123",
            senderName = "System",
            text = "Alice pinned a message",
            timestampString = "10:30 AM",
            isFromUser = false,
            messageType = "SYSTEM_EVENT",
            systemEventType = "PIN_UPDATE"
        )

        assertEquals("SYSTEM_EVENT", systemEvent.messageType)
        assertEquals("PIN_UPDATE", systemEvent.systemEventType)
        assertEquals("Alice pinned a message", systemEvent.text)

        val mentionMsg = MessageEntity(
            id = 502,
            chatId = "group_123",
            senderName = "Alice",
            text = "Hello @Bob and @Charlie",
            timestampString = "10:31 AM",
            isFromUser = true,
            mentionedUids = "uid_bob,uid_charlie"
        )

        assertEquals("uid_bob,uid_charlie", mentionMsg.mentionedUids)
    }
}
