package com.example.ui.viewmodel

import com.example.data.local.ChatEntity

data class ModernToastData(
    val message: String,
    val isError: Boolean = false
)

data class ActiveCallState(
    val isActive: Boolean = false,
    val isConnected: Boolean = false,
    val contactId: String = "",
    val contactName: String = "",
    val contactAvatar: String = "",
    val callType: String = "audio",
    val secondsElapsed: Int = 0,
    val isMuted: Boolean = false,
    val isSpeaker: Boolean = false,
    val callStatus: String = ""
)

data class CallLog(
    val id: String = "",
    val contactId: String = "",
    val contactName: String = "",
    val avatarType: String = "",
    val direction: String = "OUTGOING",
    val callType: String = "AUDIO",
    val timeString: String = "",
    val timestampMillis: Long = 0L,
    val durationSeconds: Int = 0
)

data class GroupMember(
    val id: String = "",
    val name: String = "",
    val nickname: String = "",
    val isOwner: Boolean = false,
    val isAdmin: Boolean = false,
    val canDeleteMessages: Boolean = true,
    val canPinMessages: Boolean = true,
    val canChangeGroupInfo: Boolean = true,
    val canInviteMembers: Boolean = true,
    val canMuteMembers: Boolean = false
)

data class GroupJoinRequest(
    val id: String = "",
    val userName: String = "",
    val requestTime: String = "",
    val userAvatar: String = ""
)

data class ScannedUser(
    val publicId: String = "",
    val name: String = "",
    val username: String = "",
    val bio: String = "",
    val profession: String = "",
    val mutualGroups: List<String> = emptyList(),
    val avatarType: String = "",
    val avatarUrl: String? = null,
    val uid: String = "",
    val id: String = uid.ifBlank { publicId }
)

sealed class ScannedUserResult {
    data class Success(val user: ScannedUser) : ScannedUserResult()
    object InvalidQr : ScannedUserResult()
    object UserNotFound : ScannedUserResult()
}

data class DeletedChatInfo(
    val chat: ChatEntity,
    val deletedAtMillis: Long = System.currentTimeMillis()
)
