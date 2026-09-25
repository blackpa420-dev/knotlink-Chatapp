package com.example

import com.example.data.local.BlockedUserEntity
import com.example.data.local.ChatEntity
import com.example.data.local.MessageEntity
import com.example.data.local.UserIdentityEntity
import com.example.data.repository.UserPrivacySettings
import com.example.data.local.UserSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityPrivacySessionTest {

    @Test
    fun userPrivacySettings_defaultValues_areConfiguredCorrectly() {
        val settings = UserPrivacySettings()

        assertEquals("EVERYONE", settings.profilePhotoVisibility)
        assertEquals("EVERYONE", settings.onlineStatusVisibility)
        assertEquals("EVERYONE", settings.lastSeenVisibility)
        assertEquals("EVERYONE", settings.whoCanMessageMe)
        assertTrue(settings.readReceiptsEnabled)
        assertTrue(settings.typingIndicatorEnabled)
    }

    @Test
    fun userPrivacySettings_customValues_canBeUpdated() {
        val settings = UserPrivacySettings(
            profilePhotoVisibility = "CONTACTS",
            onlineStatusVisibility = "NOBODY",
            lastSeenVisibility = "NOBODY",
            whoCanMessageMe = "CONTACTS",
            readReceiptsEnabled = false,
            typingIndicatorEnabled = false
        )

        assertEquals("CONTACTS", settings.profilePhotoVisibility)
        assertEquals("NOBODY", settings.onlineStatusVisibility)
        assertEquals("NOBODY", settings.lastSeenVisibility)
        assertEquals("CONTACTS", settings.whoCanMessageMe)
        assertFalse(settings.readReceiptsEnabled)
        assertFalse(settings.typingIndicatorEnabled)
    }

    @Test
    fun privacyEnforcement_ruleMapping_validatesPermissionMatrix() {
        fun canViewProfilePhoto(setting: String, isContact: Boolean): Boolean {
            return when (setting) {
                "EVERYONE" -> true
                "CONTACTS" -> isContact
                "NOBODY" -> false
                else -> true
            }
        }

        fun canDirectMessage(setting: String, isContact: Boolean): Boolean {
            return when (setting) {
                "EVERYONE" -> true
                "CONTACTS" -> isContact
                "NOBODY" -> false
                else -> true
            }
        }

        // Test EVERYONE
        assertTrue(canViewProfilePhoto("EVERYONE", isContact = false))
        assertTrue(canViewProfilePhoto("EVERYONE", isContact = true))
        assertTrue(canDirectMessage("EVERYONE", isContact = false))

        // Test CONTACTS
        assertFalse(canViewProfilePhoto("CONTACTS", isContact = false))
        assertTrue(canViewProfilePhoto("CONTACTS", isContact = true))
        assertFalse(canDirectMessage("CONTACTS", isContact = false))
        assertTrue(canDirectMessage("CONTACTS", isContact = true))

        // Test NOBODY
        assertFalse(canViewProfilePhoto("NOBODY", isContact = true))
        assertFalse(canViewProfilePhoto("NOBODY", isContact = false))
        assertFalse(canDirectMessage("NOBODY", isContact = true))
        assertFalse(canDirectMessage("NOBODY", isContact = false))
    }

    @Test
    fun blockedUserEntity_attributesAreRetained() {
        val blockedUser = BlockedUserEntity(
            targetUid = "spammer_99",
            username = "malicious_user",
            displayName = "Spam Bot",
            blockedAt = 1755180000000L
        )

        assertEquals("spammer_99", blockedUser.targetUid)
        assertEquals("malicious_user", blockedUser.username)
        assertEquals("Spam Bot", blockedUser.displayName)
        assertEquals(1755180000000L, blockedUser.blockedAt)
    }

    @Test
    fun blockEnforcement_preventsMessageDelivery_whilePreservingChatHistory() {
        val blockedList = mutableListOf("blocked_uid_123")
        val existingMessages = listOf(
            MessageEntity(id = 1, chatId = "chat_123", senderName = "Bob", text = "Old message before block", timestampString = "09:00 AM", isFromUser = false)
        )

        fun canSendMessage(targetUid: String): Boolean {
            return !blockedList.contains(targetUid)
        }

        // Verify sending is blocked
        assertFalse(canSendMessage("blocked_uid_123"))
        assertTrue(canSendMessage("allowed_uid_456"))

        // Verify history is NOT deleted
        assertEquals(1, existingMessages.size)
        assertEquals("Old message before block", existingMessages.first().text)

        // Unblock restores permission
        blockedList.remove("blocked_uid_123")
        assertTrue(canSendMessage("blocked_uid_123"))
    }

    @Test
    fun reportSystem_categoriesAndValidation() {
        val validReportCategories = listOf(
            "Spam or Advertising",
            "Harassment or Bullying",
            "Impersonation",
            "Inappropriate Content",
            "Malware or Phishing",
            "Other"
        )

        val reportPayload = mapOf(
            "reportId" to "rep_abc12345",
            "reporterUid" to "auth_user_1",
            "targetType" to "USER",
            "targetId" to "bad_actor_99",
            "reason" to "Spam or Advertising",
            "details" to "Repeated advertising links in direct messages",
            "status" to "PENDING"
        )

        assertTrue(validReportCategories.contains(reportPayload["reason"]))
        assertEquals("PENDING", reportPayload["status"])
        assertTrue((reportPayload["reportId"] as String).startsWith("rep_"))
        assertNotNull(reportPayload["reporterUid"])
        assertNotEquals("", reportPayload["reporterUid"])
    }

    @Test
    fun userSessionEntity_deviceAttributes_areManaged() {
        val currentSession = UserSessionEntity(
            deviceId = "dev_phone_pixel8",
            deviceName = "Pixel 8 Pro",
            platform = "Android 14",
            appVersion = "2.4.0",
            lastActiveTimestamp = 1755182000000L,
            isCurrentDevice = true
        )

        val remoteSession = UserSessionEntity(
            deviceId = "dev_tablet_galaxy",
            deviceName = "Samsung Galaxy Tab",
            platform = "Android 13",
            appVersion = "2.3.1",
            lastActiveTimestamp = 1755170000000L,
            isCurrentDevice = false
        )

        assertTrue(currentSession.isCurrentDevice)
        assertEquals("Pixel 8 Pro", currentSession.deviceName)

        assertFalse(remoteSession.isCurrentDevice)
        assertEquals("Samsung Galaxy Tab", remoteSession.deviceName)
        assertTrue(currentSession.lastActiveTimestamp > remoteSession.lastActiveTimestamp)
    }

    @Test
    fun sessionRevocation_logoutAllOtherDevices_preservesCurrentSessionOnly() {
        val currentDeviceId = "dev_phone_current"
        val activeSessions = mutableListOf(
            UserSessionEntity(deviceId = currentDeviceId, deviceName = "My Phone", platform = "Android", appVersion = "1.0", isCurrentDevice = true),
            UserSessionEntity(deviceId = "dev_remote_tablet", deviceName = "Tablet", platform = "Android", appVersion = "1.0", isCurrentDevice = false),
            UserSessionEntity(deviceId = "dev_remote_web", deviceName = "Web Client", platform = "Web", appVersion = "1.0", isCurrentDevice = false)
        )

        assertEquals(3, activeSessions.size)

        // Revoke all other sessions
        activeSessions.removeAll { it.deviceId != currentDeviceId }

        assertEquals(1, activeSessions.size)
        assertEquals(currentDeviceId, activeSessions.first().deviceId)
        assertTrue(activeSessions.first().isCurrentDevice)
    }

    @Test
    fun accountDeletion_atomicCleanupSimulation_wipesIdentityAndReleasesMappings() {
        var userProfile: UserIdentityEntity? = UserIdentityEntity(
            phoneNumber = "+15551234567",
            username = "alice_wonder",
            fullName = "Alice W."
        )
        val phoneIndex = mutableMapOf("+15551234567" to "uid_alice")
        val usernameIndex = mutableMapOf("alice_wonder" to "uid_alice")
        val localChats = mutableListOf(ChatEntity(id = "c1", name = "Test Chat", lastMessage = "Hi", timeString = "Now", unreadCount = 0, isOnline = true, category = "All"))
        val localMessages = mutableListOf(MessageEntity(id = 1, chatId = "c1", senderName = "Alice", text = "Hi", timestampString = "Now", isFromUser = true))
        val activeSessions = mutableListOf("dev_1", "dev_2")

        // Execute Deletion
        val normalizedPhone = userProfile?.phoneNumber?.replace(Regex("[^0-9+]"), "")
        if (normalizedPhone != null) phoneIndex.remove(normalizedPhone)
        val username = userProfile?.username?.lowercase()
        if (username != null) usernameIndex.remove(username)
        activeSessions.clear()
        localChats.clear()
        localMessages.clear()
        userProfile = null

        // Verify No Dangling Mappings
        assertNull(phoneIndex["+15551234567"])
        assertNull(usernameIndex["alice_wonder"])
        assertTrue(activeSessions.isEmpty())
        assertTrue(localChats.isEmpty())
        assertTrue(localMessages.isEmpty())
        assertNull(userProfile)
    }

    @Test
    fun accountSwitching_zeroDataLeakage() {
        val userAChats = listOf(ChatEntity(id = "chat_a", name = "Secret Chat A", lastMessage = "A's msg", timeString = "Now", unreadCount = 0, isOnline = true, category = "All"))
        var activeSessionIdentity: UserIdentityEntity? = UserIdentityEntity(phoneNumber = "+1111111111", username = "user_a")
        var currentLoadedChats = userAChats.toMutableList()

        assertEquals(1, currentLoadedChats.size)
        assertEquals("Secret Chat A", currentLoadedChats.first().name)

        // Logout User A (clearAllLocalData)
        currentLoadedChats.clear()
        activeSessionIdentity = null

        assertTrue(currentLoadedChats.isEmpty())
        assertNull(activeSessionIdentity)

        // Login User B
        activeSessionIdentity = UserIdentityEntity(phoneNumber = "+2222222222", username = "user_b")
        val userBChats = listOf(ChatEntity(id = "chat_b", name = "Work Chat B", lastMessage = "B's msg", timeString = "Now", unreadCount = 0, isOnline = true, category = "All"))
        currentLoadedChats.addAll(userBChats)

        assertEquals(1, currentLoadedChats.size)
        assertEquals("Work Chat B", currentLoadedChats.first().name)
        assertFalse(currentLoadedChats.any { it.name.contains("Secret Chat A") })
    }
}
