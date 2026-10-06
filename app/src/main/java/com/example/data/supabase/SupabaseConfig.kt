package com.example.data.supabase

object SupabaseConfig {
    var PROJECT_URL = "https://qxeggscatuwdyerqsvyf.supabase.co"
    var ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InF4ZWdnc2NhdHV3ZHllcnFzdnlmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTEzMDUzNDYsImV4cCI6MjEwNjg4MTM0Nn0.u4YeOh0R4GxfSxvxeYiJ6jhO13JSNTYFSWkJijMrItk"

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

