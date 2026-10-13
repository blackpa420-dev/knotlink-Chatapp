package com.example.util

/**
 * Internal identifiers (account UUIDs, chat id hashes, UUID-derived placeholder handles) must never be
 * shown to people. These helpers turn anything that looks like one into a friendly fallback.
 */
private val UUID_LIKE = Regex("^@?[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}(\\.link)?$")
private val HEX_LIKE = Regex("^@?[0-9a-fA-F]{12,}(\\.link)?$")

fun looksLikeInternalId(value: String?): Boolean {
    val v = value?.trim().orEmpty()
    if (v.isEmpty()) return false
    return UUID_LIKE.matches(v) || HEX_LIKE.matches(v) || v.startsWith("chat_") || v.startsWith("user_")
}

/** A person's display name, never an id. */
fun safeDisplayName(raw: String?, fallback: String = "KnotLink User"): String {
    val v = raw?.trim().orEmpty()
    return if (v.isEmpty() || looksLikeInternalId(v)) fallback else v
}

/** "@handle" for display, or null when the handle is blank or just an id. */
fun safeHandle(raw: String?): String? {
    val v = raw?.trim().orEmpty()
    if (v.isEmpty() || looksLikeInternalId(v)) return null
    val withAt = if (v.startsWith("@")) v else "@$v"
    return withAt.takeIf { it != "@" }
}
