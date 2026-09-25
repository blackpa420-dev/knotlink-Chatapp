package com.example.data.supabase

object SupabaseConfig {
    var PROJECT_URL = "https://ykroccabyaxauaxelkhx.supabase.co"
    var ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inlrcm9jY2FieWF4YXVheGVsa2h4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk3NTI1MzQsImV4cCI6MjEwNTMyODUzNH0._1UGI-hMViZZuyny0TvRfEwR-ddfRmU6f61j2kXnmGI"

    // Tables
    const val TABLE_PROFILES = "profiles"
    const val TABLE_CHATS = "chats"
    const val TABLE_MESSAGES = "messages"
    const val TABLE_TYPING_STATUS = "typing_status"
    const val TABLE_CALL_SESSIONS = "call_sessions"
    const val TABLE_STATUS_STORIES = "status_stories"
    const val TABLE_ABUSE_REPORTS = "abuse_reports"
    const val TABLE_CONTACTS = "contacts"
    const val TABLE_BLOCKED_USERS = "blocked_users"

    // Storage Buckets
    const val BUCKET_AVATARS = "avatars"
    const val BUCKET_CHAT_MEDIA = "chat_media"

    val REST_BASE_URL: String get() = if (PROJECT_URL.isNotBlank()) "$PROJECT_URL/rest/v1" else ""
    val AUTH_BASE_URL: String get() = if (PROJECT_URL.isNotBlank()) "$PROJECT_URL/auth/v1" else ""
    val STORAGE_BASE_URL: String get() = if (PROJECT_URL.isNotBlank()) "$PROJECT_URL/storage/v1" else ""
    val REALTIME_WS_URL: String get() {
        if (PROJECT_URL.isBlank()) return ""
        val wsUrl = PROJECT_URL.replace("https://", "wss://").replace("http://", "ws://")
        return "$wsUrl/realtime/v1/websocket?apikey=$ANON_KEY&vsn=1.0.0"
    }
}

