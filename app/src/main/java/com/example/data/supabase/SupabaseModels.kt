package com.example.data.supabase

import org.json.JSONObject

data class SupabaseUser(
    val id: String,
    val email: String?,
    val phone: String? = null,
    val createdAt: String? = null,
    val userMetadata: Map<String, Any?> = emptyMap()
) {
    companion object {
        fun fromJson(json: JSONObject): SupabaseUser {
            val id = json.optString("id", "")
            val email = if (json.has("email") && !json.isNull("email")) json.optString("email") else null
            val phone = if (json.has("phone") && !json.isNull("phone")) json.optString("phone") else null
            val createdAt = json.optString("created_at", "")
            val metadataMap = mutableMapOf<String, Any?>()
            val metaObj = json.optJSONObject("user_metadata")
            if (metaObj != null) {
                val keys = metaObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    metadataMap[key] = metaObj.opt(key)
                }
            }
            return SupabaseUser(id, email, phone, createdAt, metadataMap)
        }
    }
}

data class SupabaseAuthSession(
    val accessToken: String,
    val tokenType: String = "bearer",
    val expiresIn: Long = 3600,
    val refreshToken: String? = null,
    val user: SupabaseUser? = null
) {
    companion object {
        fun fromJson(json: JSONObject): SupabaseAuthSession {
            val token = json.optString("access_token", "")
            val tokenType = json.optString("token_type", "bearer")
            val expiresIn = json.optLong("expires_in", 3600)
            val refresh = if (json.has("refresh_token")) json.optString("refresh_token") else null
            val userObj = json.optJSONObject("user")
            val user = userObj?.let { SupabaseUser.fromJson(it) }
            return SupabaseAuthSession(token, tokenType, expiresIn, refresh, user)
        }
    }
}

data class SupabaseProfile(
    val id: String,
    val username: String = "",
    val fullName: String = "",
    val avatarUrl: String? = null,
    val bio: String = "Verified KnotLink User",
    val profession: String = "🎓 Student",
    val email: String = "",
    val secondaryEmail: String = "",
    val isEmailVerified: Boolean = false,
    val isSecondaryEmailVerified: Boolean = false,
    val isVerified: Boolean = false,
    val birthDate: String = "",
    val joinedDate: String = "",
    val publicId: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val privacySettingsJson: String? = null,
    val privacySettings: String? = privacySettingsJson,
    val fcmToken: String? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("username", username)
            put("full_name", fullName)
            if (!avatarUrl.isNullOrBlank()) {
                put("avatar_url", avatarUrl)
            }
            put("bio", bio)
            put("profession", profession)
            put("email", email.trim().lowercase())
            if (secondaryEmail.isNotBlank()) {
                put("secondary_email", secondaryEmail.trim().lowercase())
            }
            put("is_email_verified", isEmailVerified)
            put("is_verified", isVerified)
            if (birthDate.isNotBlank()) {
                put("birth_date", birthDate)
            }
            put("is_online", isOnline)
            put("last_seen", lastSeen)
            if (!fcmToken.isNullOrBlank()) {
                put("fcm_token", fcmToken)
            }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): SupabaseProfile {
            val priv = if (json.has("privacy_settings") && !json.isNull("privacy_settings")) json.optString("privacy_settings") else null
            val token = if (json.has("fcm_token") && !json.isNull("fcm_token")) json.optString("fcm_token") else null
            return SupabaseProfile(
                id = json.optString("id", ""),
                username = json.optString("username", ""),
                fullName = json.optString("full_name", ""),
                avatarUrl = if (json.has("avatar_url") && !json.isNull("avatar_url")) json.optString("avatar_url") else null,
                bio = json.optString("bio", "Verified KnotLink User"),
                profession = json.optString("profession", "🎓 Student"),
                email = json.optString("email", ""),
                secondaryEmail = json.optString("secondary_email", ""),
                isEmailVerified = json.optBoolean("is_email_verified", false),
                isSecondaryEmailVerified = json.optBoolean("is_secondary_email_verified", false),
                isVerified = json.optBoolean("is_verified", false),
                birthDate = json.optString("birth_date", ""),
                joinedDate = json.optString("created_at", json.optString("joined_date", "")),
                publicId = json.optString("public_id", json.optString("username", "")),
                isOnline = json.optBoolean("is_online", false),
                lastSeen = json.optLong("last_seen", System.currentTimeMillis()),
                privacySettingsJson = priv,
                privacySettings = priv,
                fcmToken = token
            )
        }
    }
}

data class SupabaseMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val receiverId: String = "",
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timestampString: String = "",
    val status: String = "SENT", // SENT, DELIVERED, READ
    val isRead: Boolean = (status == "READ"),
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val messageType: String = if (!mediaType.isNullOrBlank()) mediaType else "TEXT",
    val replyToMessageId: String? = null,
    val replyToId: String? = replyToMessageId,
    val isForwarded: Boolean = false,
    val isEdited: Boolean = false,
    val isDeletedForEveryone: Boolean = false,
    val isPinned: Boolean = false,
    val clientMsgId: String? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            val longId = id.toLongOrNull()
            if (longId != null && longId > 0) {
                put("id", longId)
            }
            put("chat_id", chatId)
            put("sender_id", senderId)
            put("sender_name", senderName)
            put("recipient_id", receiverId)
            put("text", text)
            put("status", if (isRead) "READ" else status)
            put("message_type", messageType)
            put("created_at", timestamp)
            put("is_pinned", isPinned)
            if (!clientMsgId.isNullOrBlank()) {
                put("client_msg_id", clientMsgId)
            }
            if (!mediaUrl.isNullOrBlank()) {
                put("media_url", mediaUrl)
            }
            if (!mediaType.isNullOrBlank()) {
                put("media_type", mediaType)
            }
            val repLong = (replyToId ?: replyToMessageId)?.toLongOrNull()
            if (repLong != null && repLong > 0) {
                put("reply_to_id", repLong)
            } else if (!replyToId.isNullOrBlank()) {
                put("reply_to_id", replyToId)
            }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): SupabaseMessage {
            val status = json.optString("status", "sent").uppercase()
            val replyId = when {
                json.has("reply_to_id") && !json.isNull("reply_to_id") -> json.optString("reply_to_id")
                json.has("reply_to_message_id") && !json.isNull("reply_to_message_id") -> json.optString("reply_to_message_id")
                else -> null
            }
            val clientMsgId = when {
                json.has("client_msg_id") && !json.isNull("client_msg_id") -> json.optString("client_msg_id")
                json.has("clientMessageId") && !json.isNull("clientMessageId") -> json.optString("clientMessageId")
                else -> null
            }
            val mType = json.optString("message_type", json.optString("media_type", "TEXT"))
            val recId = json.optString("recipient_id", json.optString("receiver_id", ""))
            val ts = when {
                json.has("created_at") -> {
                    val raw = json.opt("created_at")
                    when (raw) {
                        is Number -> raw.toLong()
                        is String -> parseIsoTimestamp(raw)
                        else -> System.currentTimeMillis()
                    }
                }
                json.has("timestamp") -> json.optLong("timestamp", System.currentTimeMillis())
                else -> System.currentTimeMillis()
            }
            val msgId = json.opt("id")?.toString() ?: ""
            return SupabaseMessage(
                id = msgId,
                chatId = json.optString("chat_id", ""),
                senderId = json.optString("sender_id", ""),
                senderName = json.optString("sender_name", "User"),
                receiverId = recId,
                text = json.optString("body", json.optString("text", json.optString("content", ""))),
                timestamp = ts,
                timestampString = json.optString("timestamp_string", ""),
                status = status,
                isRead = (status == "READ" || json.optBoolean("is_read", false)),
                mediaUrl = if (json.has("media_url") && !json.isNull("media_url")) json.optString("media_url") else null,
                mediaType = if (json.has("media_type") && !json.isNull("media_type")) json.optString("media_type") else null,
                messageType = mType,
                replyToMessageId = replyId,
                replyToId = replyId,
                isForwarded = json.optBoolean("is_forwarded", false),
                isEdited = json.optBoolean("is_edited", false),
                isDeletedForEveryone = json.optBoolean("is_deleted", json.optBoolean("is_deleted_for_everyone", false)),
                isPinned = json.optBoolean("is_pinned", false),
                clientMsgId = clientMsgId
            )
        }
        private fun parseIsoTimestamp(value: String): Long {
            val raw = value.trim()
            raw.toLongOrNull()?.let { return it }
            return try {
                val base = raw.take(19)
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                var result = sdf.parse(base)?.time ?: System.currentTimeMillis()
                val dot = raw.indexOf('.')
                if (dot >= 0) {
                    val digits = raw.substring(dot + 1).takeWhile { it.isDigit() }.take(3)
                    if (digits.isNotEmpty()) result += digits.padEnd(3, '0').toLong()
                }
                result
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }

    }
}

data class SupabaseTyping(
    val chatId: String,
    val userId: String,
    val userName: String = "",
    val isTyping: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("chat_id", chatId)
            put("user_id", userId)
            put("user_name", userName)
            put("is_typing", isTyping)
            put("updated_at", updatedAt)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): SupabaseTyping {
            return SupabaseTyping(
                chatId = json.optString("chat_id", ""),
                userId = json.optString("user_id", ""),
                userName = json.optString("user_name", ""),
                isTyping = json.optBoolean("is_typing", false),
                updatedAt = json.optLong("updated_at", System.currentTimeMillis())
            )
        }
    }
}

data class SupabaseCallSession(
    val callId: String,
    val callerId: String,
    val receiverId: String,
    val callerName: String,
    val callerAvatar: String? = null,
    val callType: String = "audio",
    val status: String = "RINGING",
    val sdpOffer: String = "",
    val sdpAnswer: String = "",
    val iceCandidates: String = "[]",
    val startedAt: Long = System.currentTimeMillis(),
    val connectedAt: Long? = null,
    val endedAt: Long? = null,
    val id: String = callId
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", if (id.isNotBlank()) id else callId)
            put("caller_id", callerId)
            put("callee_id", receiverId)
            put("caller_name", callerName)
            put("caller_avatar", callerAvatar ?: "")
            put("call_type", callType.lowercase())
            put("status", status.lowercase())
            if (sdpOffer.isNotBlank()) put("sdp_offer", sdpOffer)
            if (sdpAnswer.isNotBlank()) put("sdp_answer", sdpAnswer)
            put("ice_candidates", iceCandidates.ifBlank { "[]" })
            put("created_at", formatIsoTimestamp(startedAt))
            if (connectedAt != null && connectedAt > 0) put("answered_at", formatIsoTimestamp(connectedAt))
            if (endedAt != null && endedAt > 0) put("ended_at", formatIsoTimestamp(endedAt))
        }
    }

    companion object {
        fun fromJson(json: JSONObject): SupabaseCallSession {
            val cid = json.optString("id", json.optString("call_id", ""))
            val started = parseTimestamp(
                json.optString("created_at", json.optString("started_at", ""))
            )
            val connected = parseNullableTimestamp(
                json.optString("answered_at", json.optString("connected_at", ""))
            )
            val ended = parseNullableTimestamp(json.optString("ended_at", ""))
            return SupabaseCallSession(
                callId = cid,
                callerId = json.optString("caller_id", ""),
                receiverId = json.optString("callee_id", json.optString("receiver_id", "")),
                callerName = json.optString("caller_name", "Caller"),
                callerAvatar = if (json.has("caller_avatar") && !json.isNull("caller_avatar")) json.optString("caller_avatar") else null,
                callType = json.optString("call_type", "audio"),
                status = json.optString("status", "ringing").uppercase(),
                sdpOffer = json.optString("sdp_offer", ""),
                sdpAnswer = json.optString("sdp_answer", ""),
                iceCandidates = json.optString("ice_candidates", "[]"),
                startedAt = started,
                connectedAt = connected,
                endedAt = ended,
                id = cid
            )
        }

        private fun formatIsoTimestamp(value: Long): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            return sdf.format(java.util.Date(value))
        }

        private fun parseTimestamp(value: String): Long {
            if (value.isBlank()) return System.currentTimeMillis()
            value.toLongOrNull()?.let { return it }
            return try {
                val normalized = value.take(23)
                val sdf = if (normalized.length >= 23)
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.US)
                else
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                sdf.parse(normalized.replace("Z", ""))?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }

        private fun parseNullableTimestamp(value: String): Long? {
            if (value.isBlank()) return null
            return parseTimestamp(value)
        }
    }
}

data class SupabaseStatusStory(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val mediaUrl: String,
    val caption: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("user_id", userId)
            put("user_name", userName)
            put("user_avatar", userAvatar ?: JSONObject.NULL)
            put("media_url", mediaUrl)
            put("caption", caption)
            put("created_at", createdAt)
            put("expires_at", expiresAt)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): SupabaseStatusStory {
            return SupabaseStatusStory(
                id = json.optString("id", ""),
                userId = json.optString("user_id", ""),
                userName = json.optString("user_name", "User"),
                userAvatar = if (json.has("user_avatar") && !json.isNull("user_avatar")) json.optString("user_avatar") else null,
                mediaUrl = json.optString("media_url", ""),
                caption = json.optString("caption", ""),
                createdAt = json.optLong("created_at", System.currentTimeMillis()),
                expiresAt = json.optLong("expires_at", System.currentTimeMillis() + 86400000)
            )
        }
    }
}
