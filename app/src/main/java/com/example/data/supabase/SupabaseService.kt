package com.example.data.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

object SupabaseService {
    private const val TAG = "SupabaseService"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val callCandidateMutexes = ConcurrentHashMap<String, Mutex>()
    private val localCallCandidates = ConcurrentHashMap<String, JSONArray>()
    private data class ProfileCacheEntry(val profile: SupabaseProfile?, val cachedAt: Long)
    private const val PROFILE_CACHE_TTL_MS = 60_000L
    private val profileCache = ConcurrentHashMap<String, ProfileCacheEntry>()
    private var allProfilesCache: List<SupabaseProfile>? = null
    private var allProfilesCacheAt: Long = 0L
    private val presenceUpdateTimes = ConcurrentHashMap<String, Long>()
    private const val PRESENCE_HEARTBEAT_TTL_MS = 15_000L

    private fun getCachedProfile(key: String): SupabaseProfile? {
        val normalized = key.trim().lowercase()
        if (normalized.isBlank()) return null
        val entry = profileCache[normalized] ?: return null
        if (System.currentTimeMillis() - entry.cachedAt > PROFILE_CACHE_TTL_MS) {
            profileCache.remove(normalized)
            return null
        }
        return entry.profile
    }

    private fun cacheProfile(profile: SupabaseProfile?) {
        if (profile == null) return
        val now = System.currentTimeMillis()
        val keys = listOf(profile.id, profile.username, profile.email)
        for (key in keys) {
            val normalized = key.trim().lowercase()
            if (normalized.isNotBlank()) profileCache[normalized] = ProfileCacheEntry(profile, now)
        }
    }

    private var currentSession: SupabaseAuthSession? = null
    private var prefs: android.content.SharedPreferences? = null
    private val sessionRefreshLock = Any()

    // Authenticated requests must never silently fall back to ANON_KEY.
    // A 401 triggers one refresh-token attempt, then the original failure is surfaced.
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val request = chain.request()
            val authHeader = request.header("Authorization")
            val isAuthenticatedRequest =
                !authHeader.isNullOrBlank() &&
                authHeader != "Bearer ${SupabaseConfig.ANON_KEY}"

            val response = chain.proceed(request)
            if (response.code != 401 || !isAuthenticatedRequest) {
                return@addInterceptor response
            }

            if (currentSession?.refreshToken.isNullOrBlank()) {
                return@addInterceptor response
            }

            response.close()
            val refreshedToken = refreshSessionBlocking()
            if (refreshedToken.isNullOrBlank()) {
                return@addInterceptor chain.proceed(request)
            }

            val retryRequest = request.newBuilder()
                .removeHeader("Authorization")
                .addHeader("Authorization", "Bearer $refreshedToken")
                .build()
            chain.proceed(retryRequest)
        }
        .build()

    fun init(context: android.content.Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences("bitchat_supabase_prefs", android.content.Context.MODE_PRIVATE)
            restoreSession()
        }
    }

    fun isJwtValid(jwt: String?): Boolean {
        if (jwt.isNullOrBlank() || jwt == SupabaseConfig.ANON_KEY) return false
        return try {
            val parts = jwt.split(".")
            if (parts.size != 3) return false
            val payloadBytes = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING)
            val payloadJson = String(payloadBytes, Charsets.UTF_8)
            val obj = JSONObject(payloadJson)
            val exp = obj.optLong("exp", 0L)
            if (exp > 0L) {
                val nowSec = System.currentTimeMillis() / 1000
                exp > (nowSec + 30) // valid if at least 30 seconds remain
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun restoreSession() {
        val token = prefs?.getString("access_token", null)
        val refreshToken = prefs?.getString("refresh_token", null)
        val uid = prefs?.getString("user_id", null)
        val email = prefs?.getString("user_email", null)
        if (!token.isNullOrBlank() && !uid.isNullOrBlank()) {
            currentSession = SupabaseAuthSession(
                accessToken = token,
                refreshToken = refreshToken,
                user = SupabaseUser(id = uid, email = email ?: "")
            )
            Log.d(TAG, "Restored persisted Supabase session for user: $uid")
        }
    }

    private fun persistSession(session: SupabaseAuthSession?) {
        val editor = prefs?.edit() ?: return
        if (session != null &&
            session.accessToken.isNotBlank() &&
            session.accessToken != SupabaseConfig.ANON_KEY &&
            !session.user?.id.isNullOrBlank()
        ) {
            editor.putString("access_token", session.accessToken)
            editor.putString("refresh_token", session.refreshToken)
            editor.putString("user_id", session.user?.id)
            editor.putString("user_email", session.user?.email)
        } else if (session == null) {
            editor.remove("access_token")
            editor.remove("refresh_token")
            editor.remove("user_id")
            editor.remove("user_email")
        }
        editor.apply()
    }

    fun setSession(session: SupabaseAuthSession?) {
        currentSession = session
        persistSession(session)
    }

    fun getSession(): SupabaseAuthSession? = currentSession

    fun getAccessToken(): String {
        val token = currentSession?.accessToken
        // Return the current session token even when it is near/just past expiry.
        // The HTTP interceptor can then perform a single refresh-token exchange.
        if (!token.isNullOrBlank() && token != SupabaseConfig.ANON_KEY) {
            return token
        }
        return SupabaseConfig.ANON_KEY
    }

    fun getCurrentUserId(): String? {
        val session = currentSession ?: return null
        if (!isJwtValid(session.accessToken)) return null
        return session.user?.id
    }

    suspend fun getAuthenticatedUserId(): String? = withContext(Dispatchers.IO) {
        if (currentSession == null) return@withContext null
        if (!isJwtValid(currentSession?.accessToken)) {
            if (!refreshSession()) return@withContext null
        }
        currentSession?.user?.id
    }

    private fun refreshSessionBlocking(): String? {
        val refreshToken = currentSession?.refreshToken ?: return null
        if (refreshToken.isBlank()) return null
        synchronized(sessionRefreshLock) {
            val latest = currentSession
            if (latest != null && isJwtValid(latest.accessToken)) return latest.accessToken
            return try {
                val form = FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .build()
                val request = Request.Builder()
                    .url("${SupabaseConfig.AUTH_BASE_URL}/token?grant_type=refresh_token")
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .post(form)
                    .build()
                val response = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .build()
                    .newCall(request)
                    .execute()
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful || body.isBlank()) return null
                val refreshed = SupabaseAuthSession.fromJson(JSONObject(body))
                if (refreshed.accessToken.isBlank() || refreshed.user?.id.isNullOrBlank()) return null
                currentSession = refreshed
                persistSession(refreshed)
                refreshed.accessToken
            } catch (e: Exception) {
                Log.w(TAG, "Session refresh error: ${e.message}")
                null
            }
        }
    }

    private suspend fun refreshSession(): Boolean = withContext(Dispatchers.IO) {
        synchronized(sessionRefreshLock) {
            val latest = currentSession
            if (latest != null && isJwtValid(latest.accessToken)) {
                true
            } else {
                val refreshToken = latest?.refreshToken
                if (refreshToken.isNullOrBlank()) {
                    false
                } else {
                    try {
                        val form = FormBody.Builder()
                            .add("grant_type", "refresh_token")
                            .add("refresh_token", refreshToken)
                            .build()
                        val request = Request.Builder()
                            .url("${SupabaseConfig.AUTH_BASE_URL}/token?grant_type=refresh_token")
                            .addHeader("apikey", SupabaseConfig.ANON_KEY)
                            .addHeader("Content-Type", "application/x-www-form-urlencoded")
                            .post(form)
                            .build()
                        val response = OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(20, TimeUnit.SECONDS)
                            .writeTimeout(20, TimeUnit.SECONDS)
                            .build()
                            .newCall(request)
                            .execute()
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful || body.isBlank()) {
                            false
                        } else {
                            val refreshed = SupabaseAuthSession.fromJson(JSONObject(body))
                            if (refreshed.accessToken.isBlank() || refreshed.user?.id.isNullOrBlank()) {
                                false
                            } else {
                                currentSession = refreshed
                                persistSession(refreshed)
                                true
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Session refresh error: ${e.message}")
                        false
                    }
                }
            }
        }
    }

    // ==========================================
    // AUTH API
    // ==========================================

    private fun canonicalUsername(input: String): String {
        val base = input.trim()
            .removePrefix("@")
            .lowercase()
            .removeSuffix(".link")
            .removeSuffix(".bit")
            .removeSuffix(".chat")
            .filter { it in 'a'..'z' || it.isDigit() || it == '_' }
        return if (base.isBlank()) "" else "@$base.link"
    }

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        fullName: String = "",
        username: String = ""
    ): Result<SupabaseAuthSession> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.AUTH_BASE_URL}/signup"
            val bodyObj = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
                put("data", JSONObject().apply {
                    put("full_name", fullName)
                    put("username", username)
                })
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(resStr, "Sign up failed (${response.code})")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(resStr)

            val identities = json.optJSONArray("identities")
            if (json.has("id") && identities != null && identities.length() == 0) {
                return@withContext Result.failure(
                    Exception("This email is already registered. Please log in instead.")
                )
            }

            val session = if (json.has("access_token")) {
                SupabaseAuthSession.fromJson(json)
            } else {
                // If email confirmation is required, access_token might not be in root, but user object is
                val user = if (json.has("id")) SupabaseUser.fromJson(json) else null
                SupabaseAuthSession(
                    accessToken = SupabaseConfig.ANON_KEY,
                    user = user
                )
            }
            currentSession = session
            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Error in signUpWithEmail", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<SupabaseAuthSession> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.AUTH_BASE_URL}/token?grant_type=password"
            val bodyObj = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(resStr, "Invalid email or password")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(resStr)
            val session = SupabaseAuthSession.fromJson(json)
            currentSession = session
            persistSession(session)
            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Error in signInWithEmail", e)
            Result.failure(e)
        }
    }

    suspend fun verifyEmailOtp(
        email: String,
        token: String,
        type: String = "signup"
    ): Result<SupabaseAuthSession> = withContext(Dispatchers.IO) {
        val typesToTry = if (type == "signup") listOf("signup", "email", "magiclink", "recovery") else listOf("email", "signup", "magiclink", "recovery")
        var lastError: Exception? = null

        for (t in typesToTry) {
            try {
                val url = "${SupabaseConfig.AUTH_BASE_URL}/verify"
                val bodyObj = JSONObject().apply {
                    put("email", email.trim())
                    put("token", token.trim())
                    put("type", t)
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
                    .addHeader("Content-Type", "application/json")
                    .post(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val response = httpClient.newCall(request).execute()
                val resStr = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JSONObject(resStr)
                    val session = SupabaseAuthSession.fromJson(json)
                    currentSession = session
                    persistSession(session)
                    return@withContext Result.success(session)
                } else {
                    val errorMsg = parseErrorMessage(resStr, "Invalid verification code")
                    lastError = Exception(errorMsg)
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        Result.failure(lastError ?: Exception("Invalid or expired verification code"))
    }

    suspend fun sendOtpToEmail(
        email: String,
        type: String = "signup"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val typesToTry = when (type.lowercase()) {
                "recovery" -> listOf("recovery")
                "signup" -> listOf("signup")
                else -> listOf(type)
            }
            var lastErrStr = ""

            for (t in typesToTry) {
                try {
                    val resendUrl = "${SupabaseConfig.AUTH_BASE_URL}/resend"
                    val resendBody = JSONObject().apply {
                        put("email", cleanEmail)
                        put("type", t)
                    }
                    val resendReq = Request.Builder()
                        .url(resendUrl)
                        .addHeader("apikey", SupabaseConfig.ANON_KEY)
                        .addHeader("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
                        .addHeader("Content-Type", "application/json")
                        .post(resendBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                    val resendResp = httpClient.newCall(resendReq).execute()
                    val resendStr = resendResp.body?.string() ?: ""

                    if (resendResp.isSuccessful) {
                        Log.d(TAG, "OTP dispatched successfully via /resend ($t) to $cleanEmail")
                        return@withContext Result.success(true)
                    }

                    val err = parseErrorMessage(resendStr, "")
                    lastErrStr = err
                    val isRateLimited = resendResp.code == 429 || resendResp.code == 422 ||
                            err.contains("security", ignoreCase = true) ||
                            err.contains("rate", ignoreCase = true) ||
                            err.contains("seconds", ignoreCase = true) ||
                            err.contains("limit", ignoreCase = true) ||
                            err.contains("60", ignoreCase = true) ||
                            err.contains("already", ignoreCase = true)

                    if (isRateLimited) {
                        Log.d(TAG, "OTP active/rate limited for $cleanEmail ($err)")
                        return@withContext Result.success(true)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error trying /resend type=$t for $cleanEmail", e)
                }
            }

            if (type.equals("recovery", ignoreCase = true)) {
                return@withContext Result.failure(
                    Exception(lastErrStr.ifBlank { "No account found for this email. Please create an account first." })
                )
            }

            // Non-recovery OTP fallback may create/verify the signup flow.
            try {
                val otpUrl = "${SupabaseConfig.AUTH_BASE_URL}/otp"
                val otpBody = JSONObject().apply {
                    put("email", cleanEmail)
                    put("create_user", true)
                }
                val otpReq = Request.Builder()
                    .url(otpUrl)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
                    .addHeader("Content-Type", "application/json")
                    .post(otpBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val otpResp = httpClient.newCall(otpReq).execute()
                val otpStr = otpResp.body?.string() ?: ""
                if (otpResp.isSuccessful) return@withContext Result.success(true)

                lastErrStr = parseErrorMessage(otpStr, "Failed to send verification code")
            } catch (e: Exception) {
                Log.w(TAG, "Error trying /otp for $cleanEmail", e)
            }

            Result.failure(Exception(lastErrStr.ifBlank { "Failed to send verification code" }))
        } catch (e: Exception) {
            Log.e(TAG, "Error sending OTP to $email", e)
            Result.failure(e)
        }
    }

    suspend fun resendEmailOtp(
        email: String,
        type: String = "signup"
    ): Result<Boolean> {
        return sendOtpToEmail(email, type)
    }

    suspend fun signOut(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val token = currentSession?.accessToken
            if (token != null) {
                val url = "${SupabaseConfig.AUTH_BASE_URL}/logout"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer $token")
                    .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                httpClient.newCall(request).execute()
            }
            currentSession = null
            Result.success(true)
        } catch (e: Exception) {
            currentSession = null
            Result.success(true)
        }
    }

    // ==========================================
    // PROFILES API (REST)
    // ==========================================

    suspend fun upsertProfile(profile: SupabaseProfile): Result<SupabaseProfile> = withContext(Dispatchers.IO) {
        try {
            val username = canonicalUsername(profile.username)
            if (username.isBlank() || profile.fullName.isBlank() || profile.avatarUrl.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Profile requires username, full name and avatar"))
            }

            val body = JSONObject().apply {
                put("p_username", username)
                put("p_full_name", profile.fullName.trim())
                put("p_avatar_url", profile.avatarUrl)
                put("p_designation", profile.profession.takeIf { it.isNotBlank() })
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.REST_BASE_URL}/rpc/complete_profile")
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception(parseErrorMessage(responseBody, "Failed to complete profile (${response.code})"))
                    )
                }

                val obj = if (responseBody.trimStart().startsWith("[")) {
                    val arr = JSONArray(responseBody)
                    if (arr.length() == 0) null else arr.getJSONObject(0)
                } else {
                    JSONObject(responseBody)
                } ?: return@withContext Result.failure(Exception("Profile completion returned no profile"))

                val saved = SupabaseProfile.fromJson(obj)
                cacheProfile(saved)
                Result.success(saved)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in upsertProfile", e)
            Result.failure(e)
        }
    }

    suspend fun getProfile(userId: String): Result<SupabaseProfile?> = withContext(Dispatchers.IO) {
        try {
            val raw = userId.trim()
            if (raw.isBlank()) return@withContext Result.success(null)
            getCachedProfile(raw)?.let { return@withContext Result.success(it) }

            val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
            if (!raw.matches(uuidRegex)) {
                return@withContext getProfileByUsername(raw)
            }

            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?id=eq.$raw&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to load profile: ${response.code}"))
                }
                val arr = JSONArray(responseBody)
                if (arr.length() == 0) return@withContext Result.success(null)
                val profile = SupabaseProfile.fromJson(arr.getJSONObject(0))
                cacheProfile(profile)
                Result.success(profile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getProfile", e)
            Result.failure(e)
        }
    }

    suspend fun getProfileByEmail(email: String): Result<SupabaseProfile?> = withContext(Dispatchers.IO) {
        val current = currentSession?.user
        if (current != null && current.email.equals(email.trim(), ignoreCase = true)) {
            return@withContext getProfile(current.id)
        }
        Result.success(null)
    }

    suspend fun updateUserPassword(accessToken: String, newPass: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (newPass.isBlank()) return@withContext Result.success(true)
            val url = "${SupabaseConfig.AUTH_BASE_URL}/user"
            val bodyObj = JSONObject().apply {
                put("password", newPass)
            }
            val token = accessToken.ifBlank { getAccessToken() }
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .put(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d(TAG, "Successfully updated user password in Supabase Auth")
                Result.success(true)
            } else {
                val errStr = response.body?.string() ?: ""
                Log.w(TAG, "Failed to update user password: $errStr")
                Result.failure(Exception(parseErrorMessage(errStr, "Failed to set password")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user password", e)
            Result.failure(e)
        }
    }

    suspend fun getProfileByUsername(username: String): Result<SupabaseProfile?> = withContext(Dispatchers.IO) {
        try {
            val raw = username.trim().removePrefix("@").lowercase()
            if (raw.isBlank()) return@withContext Result.success(null)

            val genericPlaceholders = setOf(
                "user", "users", "contact", "contacts", "chat", "chat partner", "chat_partner",
                "someone", "me", "you", "admin", "null", "default", "undefined", "member",
                "chat member", "system", "anonymous", "assistant", "ai_assistant", "bitassistant"
            )
            if (genericPlaceholders.contains(raw)) return@withContext Result.success(null)

            val base = raw.removeSuffix(".link").removeSuffix(".bit").removeSuffix(".chat")
            val canonical = canonicalUsername(base)
            getCachedProfile(canonical)?.let { return@withContext Result.success(it) }

            val encoded = java.net.URLEncoder.encode(canonical, "UTF-8")
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?username=eq.$encoded&select=*&limit=1"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to load username: ${response.code}"))
                }
                val arr = JSONArray(responseBody)
                if (arr.length() == 0) return@withContext Result.success(null)
                val profile = SupabaseProfile.fromJson(arr.getJSONObject(0))
                cacheProfile(profile)
                Result.success(profile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getProfileByUsername", e)
            Result.failure(e)
        }
    }

    suspend fun searchProfiles(query: String): Result<List<SupabaseProfile>> = withContext(Dispatchers.IO) {
        try {
            val q = query.trim().removePrefix("@").lowercase()
            if (q.isBlank()) return@withContext Result.success(emptyList())

            val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
            val url = if (q.matches(uuidRegex)) {
                "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?id=eq.$q&select=*&limit=1"
            } else {
                val base = q.removeSuffix(".link")
                val encoded = java.net.URLEncoder.encode(base, "UTF-8")
                "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?or=(username.ilike.%25$encoded%25,full_name.ilike.%25$encoded%25)&select=*&limit=50"
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Profile search failed: ${response.code}"))
                }
                val arr = JSONArray(responseBody)
                val list = mutableListOf<SupabaseProfile>()
                val seen = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    val profile = SupabaseProfile.fromJson(arr.getJSONObject(i))
                    if (profile.id.isNotBlank() && seen.add(profile.id)) {
                        cacheProfile(profile)
                        list.add(profile)
                    }
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in searchProfiles", e)
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(userId: String, fcmToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val currentUid = getAuthenticatedUserId().orEmpty()
            val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
            if (!currentUid.matches(uuidRegex) || !currentUid.equals(userId.trim(), ignoreCase = true) || fcmToken.isBlank()) {
                return@withContext Result.failure(Exception("Push token registration requires the current user's UUID"))
            }

            val body = JSONObject().apply {
                put("user_id", currentUid)
                put("fcm_token", fcmToken.trim())
                put("platform", "android")
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PUSH_TOKENS}")
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val responseBody = response.body?.string().orEmpty()
                    Log.w(TAG, "Push token registration failed: ${response.code}: $responseBody")
                }
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token", e)
            Result.failure(e)
        }
    }

    suspend fun getFcmTokenForUser(targetKey: String): String? = null

    private val fcmTokenCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    suspend fun getFcmTokenForUser(targetKey: String): String? = withContext(Dispatchers.IO) {
        if (targetKey.isBlank()) return@withContext null

        val cleanKey = targetKey.removePrefix("chat_").removePrefix("user_").removePrefix("@").trim()

        val cached = fcmTokenCache[targetKey] ?: fcmTokenCache[cleanKey]
        if (!cached.isNullOrBlank()) {
            return@withContext cached
        }

        try {
            var prof = getProfile(targetKey).getOrNull()
                ?: getProfileByUsername(targetKey).getOrNull()
                ?: getProfileByEmail(targetKey).getOrNull()
                ?: (if (cleanKey.isNotBlank() && cleanKey != targetKey) {
                    getProfile(cleanKey).getOrNull()
                        ?: getProfileByUsername(cleanKey).getOrNull()
                        ?: getProfileByEmail(cleanKey).getOrNull()
                } else null)

            // Do not scan the entire profiles table for an FCM token.
            // If the targeted profile lookup has no token, return null and let the caller handle it.

            val token = prof?.fcmToken?.trim()
            if (!token.isNullOrBlank()) {
                fcmTokenCache[targetKey] = token
                fcmTokenCache[cleanKey] = token
                if (prof.id.isNotBlank()) fcmTokenCache[prof.id] = token
                if (prof.username.isNotBlank()) fcmTokenCache[prof.username] = token
                if (prof.email.isNotBlank()) fcmTokenCache[prof.email] = token
                return@withContext token
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching FCM token for $targetKey: ${e.message}")
        }
        return@withContext null
    }

    suspend fun fetchAllProfiles(): Result<List<SupabaseProfile>> = withContext(Dispatchers.IO) {
        try {
            val cached = allProfilesCache
            if (cached != null && System.currentTimeMillis() - allProfilesCacheAt < PROFILE_CACHE_TTL_MS) {
                return@withContext Result.success(cached)
            }

            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?select=*&limit=300"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""
            if (response.isSuccessful && resStr.isNotBlank()) {
                val arr = JSONArray(resStr)
                val list = mutableListOf<SupabaseProfile>()
                for (i in 0 until arr.length()) {
                    val profile = SupabaseProfile.fromJson(arr.getJSONObject(i))
                    list.add(profile)
                    cacheProfile(profile)
                }
                allProfilesCache = list
                allProfilesCacheAt = System.currentTimeMillis()
                Result.success(list)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isUsernameTaken(username: String, excludeUid: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val raw = username.trim().removePrefix("@").lowercase()
            if (raw.isBlank()) return@withContext Result.success(false)

            val base = raw.removeSuffix(".link").removeSuffix(".bit").removeSuffix(".chat")
            val canonical = canonicalUsername(base)
            val encCanonical = java.net.URLEncoder.encode(canonical, "UTF-8")

            // Availability checks must query only matching rows, never download all profiles.
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}" +
                "?username=eq.$encCanonical&select=id,username&limit=20"

            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()

            val resp = httpClient.newCall(req).execute()
            val str = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                Log.w(TAG, "Username availability HTTP ${resp.code}: $str")
                return@withContext Result.failure(
                    Exception("Could not verify username availability. Please try again.")
                )
            }
            if (str.isBlank()) {
                return@withContext Result.success(false)
            }

            val arr = JSONArray(str)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", "")
                if (excludeUid != null && id.equals(excludeUid, ignoreCase = true)) continue
                return@withContext Result.success(true)
            }

            Result.success(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error in isUsernameTaken", e)
            Result.failure(e)
        }
    }

    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?id=eq.$userId"
            val bodyObj = JSONObject().apply {
                put("is_online", isOnline)
                put("last_seen", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .patch(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(userId: String, updates: Map<String, Any?>): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?id=eq.$userId"
            val bodyObj = JSONObject()
            updates.forEach { (k, v) ->
                bodyObj.put(k, v ?: JSONObject.NULL)
            }
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .patch(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // ==========================================
    // CHATS & MESSAGES API (REST)
    // ==========================================

    suspend fun ensureChatExists(chatId: String, type: String = "DIRECT"): Result<Boolean> {
        // Legacy callers may still pass a local/hash chat ID. V2 creates direct chats
        // from the canonical recipient UUID inside sendMessage(), so this method is
        // intentionally a no-op for compatibility.
        return Result.success(chatId.isNotBlank())
    }

    suspend fun getOrCreateDirectChat(otherUserId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
            if (!otherUserId.trim().matches(uuidRegex)) {
                return@withContext Result.failure(Exception("Direct chat recipient must be a canonical UUID"))
            }

            val body = JSONObject().apply {
                put("other_user_id", otherUserId.trim())
            }
            val request = Request.Builder()
                .url("${SupabaseConfig.REST_BASE_URL}/rpc/get_or_create_direct_chat")
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception(parseErrorMessage(responseBody, "Failed to resolve direct chat (${response.code})"))
                    )
                }
                val chatId = responseBody.trim().trim('"')
                if (!chatId.matches(uuidRegex)) {
                    return@withContext Result.failure(Exception("Server returned an invalid direct chat UUID"))
                }
                Result.success(chatId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getOrCreateDirectChat", e)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(message: SupabaseMessage): Result<SupabaseMessage> = withContext(Dispatchers.IO) {
        try {
            val recipientId = message.receiverId.trim()
            val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
            if (!recipientId.matches(uuidRegex)) {
                return@withContext Result.failure(Exception("Message recipient must be a canonical UUID"))
            }

            val clientMessageId = (message.clientMsgId?.takeIf { it.matches(uuidRegex) } ?: java.util.UUID.randomUUID().toString())
            val messageType = message.messageType.lowercase().ifBlank { "text" }
            val body = JSONObject().apply {
                put("p_recipient_id", recipientId)
                put("p_message_type", messageType)
                if (message.text.isNotBlank()) put("p_body", message.text)
                if (!message.mediaUrl.isNullOrBlank()) put("p_media_url", message.mediaUrl)
                put("p_client_message_id", clientMessageId)
                val replyId = message.replyToMessageId ?: message.replyToId
                if (!replyId.isNullOrBlank() && replyId.matches(uuidRegex)) {
                    put("p_reply_to_message_id", replyId)
                }
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.REST_BASE_URL}/rpc/send_direct_message")
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception(parseErrorMessage(responseBody, "Failed to send message (${response.code})"))
                    )
                }

                val json = if (responseBody.trimStart().startsWith("[")) {
                    val arr = JSONArray(responseBody)
                    if (arr.length() == 0) null else arr.getJSONObject(0)
                } else {
                    JSONObject(responseBody)
                } ?: return@withContext Result.failure(Exception("Message RPC returned no message"))

                Result.success(SupabaseMessage.fromJson(json))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendMessage", e)
            Result.failure(e)
        }
    }

    suspend fun fetchMessagesSince(chatId: String, sinceTimestamp: Long, limit: Int = 100): Result<List<SupabaseMessage>> = withContext(Dispatchers.IO) {
        try {
            if (chatId.isBlank()) return@withContext Result.success(emptyList())
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_MESSAGES}?chat_id=eq.${java.net.URLEncoder.encode(chatId, "UTF-8")}&created_at=gt.$sinceTimestamp&order=created_at.asc&limit=$limit&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to fetch changed messages: ${response.code}"))
            val arr = JSONArray(body)
            val list = mutableListOf<SupabaseMessage>()
            for (i in 0 until arr.length()) list.add(SupabaseMessage.fromJson(arr.getJSONObject(i)))
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchMessagesSince", e)
            Result.failure(e)
        }
    }

    suspend fun fetchUserMessagesSince(
        userId: String,
        username: String? = null,
        email: String? = null,
        sinceTimestamp: Long,
        limit: Int = 200,
        offset: Int = 0
    ): Result<List<SupabaseMessage>> = withContext(Dispatchers.IO) {
        try {
            val ids = mutableListOf(userId)
            if (!username.isNullOrBlank()) {
                ids.add(username)
                ids.add(username.removePrefix("@"))
                ids.add(if (username.endsWith(".link")) username else "$username.link")
            }
            if (!email.isNullOrBlank()) ids.add(email)
            val orParts = ids.filter { it.isNotBlank() }.distinct().flatMap {
                val enc = java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20")
                listOf("recipient_id.eq.$enc", "sender_id.eq.$enc")
            }
            if (orParts.isEmpty()) return@withContext Result.success(emptyList())
            val safeOffset = offset.coerceAtLeast(0)
            val safeLimit = limit.coerceIn(1, 500)
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_MESSAGES}?or=(${orParts.joinToString(",")})&created_at=gt.$sinceTimestamp&order=created_at.asc&limit=$safeLimit&offset=$safeOffset&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to fetch changed user messages: ${response.code}"))
            val arr = JSONArray(body)
            val list = mutableListOf<SupabaseMessage>()
            for (i in 0 until arr.length()) list.add(SupabaseMessage.fromJson(arr.getJSONObject(i)))
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchUserMessagesSince", e)
            Result.failure(e)
        }
    }

    suspend fun fetchMessages(chatId: String, limit: Int = 50): Result<List<SupabaseMessage>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_MESSAGES}?chat_id=eq.$chatId&order=created_at.desc&limit=$limit&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch messages: ${response.code}"))
            }

            val jsonArray = JSONArray(resStr)
            val list = mutableListOf<SupabaseMessage>()
            for (i in 0 until jsonArray.length()) {
                list.add(SupabaseMessage.fromJson(jsonArray.getJSONObject(i)))
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchMessages", e)
            Result.failure(e)
        }
    }

    suspend fun fetchRecentGlobalMessages(limit: Int = 50): Result<List<SupabaseMessage>> = withContext(Dispatchers.IO) {
        // Disabled global message fetching to strictly prevent 3rd-party chat leakage
        Result.success(emptyList())
    }

    suspend fun fetchUserMessages(
        userId: String,
        username: String? = null,
        email: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<SupabaseMessage>> = withContext(Dispatchers.IO) {
        try {
            val ids = mutableListOf(userId)
            if (!username.isNullOrBlank()) {
                ids.add(username)
                ids.add(username.removePrefix("@"))
                ids.add(if (username.endsWith(".link")) username else "$username.link")
            }
            if (!email.isNullOrBlank()) {
                ids.add(email)
            }
            val orParts = mutableListOf<String>()
            for (id in ids.filter { it.isNotBlank() }.distinct()) {
                val enc = java.net.URLEncoder.encode(id, "UTF-8").replace("+", "%20")
                orParts.add("recipient_id.eq.$enc")
                orParts.add("sender_id.eq.$enc")
            }
            if (orParts.isEmpty()) return@withContext Result.success(emptyList())

            val safeOffset = offset.coerceAtLeast(0)
            val safeLimit = limit.coerceIn(1, 500)
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_MESSAGES}?or=(${orParts.joinToString(",")})&order=created_at.desc&limit=$safeLimit&offset=$safeOffset&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch user messages: ${response.code}"))
            }

            val jsonArray = JSONArray(resStr)
            val list = mutableListOf<SupabaseMessage>()
            for (i in 0 until jsonArray.length()) {
                list.add(SupabaseMessage.fromJson(jsonArray.getJSONObject(i)))
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchUserMessages", e)
            Result.failure(e)
        }
    }

    suspend fun markMessageDelivered(messageId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (messageId.isBlank()) return@withContext Result.success(true)
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_MESSAGES}?id=eq.$messageId&status=eq.SENT"
            val bodyObj = JSONObject().apply {
                put("status", "DELIVERED")
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .patch(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsRead(chatId: String, currentUserId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_MESSAGES}?chat_id=eq.$chatId&sender_id=neq.$currentUserId&status=neq.READ"
            val bodyObj = JSONObject().apply {
                put("status", "READ")
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .patch(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsRead(chatId: String, messageIds: List<String>): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (messageIds.isEmpty()) return@withContext Result.success(true)
            val idsIn = "in.(${messageIds.joinToString(",")})"
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_MESSAGES}?chat_id=eq.$chatId&id=$idsIn"
            val bodyObj = JSONObject().apply {
                put("status", "READ")
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .patch(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editMessage(messageId: String, newText: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_MESSAGES}?id=eq.$messageId"
            val bodyObj = JSONObject().apply {
                put("text", newText)
                put("is_edited", true)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMessagePinnedStatus(messageId: String, isPinned: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_MESSAGES}?id=eq.$messageId"
            val bodyObj = JSONObject().apply {
                put("is_pinned", isPinned)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessageForEveryone(messageId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_MESSAGES}?id=eq.$messageId"
            val bodyObj = JSONObject().apply {
                put("is_deleted_for_everyone", true)
                put("text", "")
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // TYPING INDICATOR API
    // ==========================================

    /**
     * Typing is transported through Supabase Realtime broadcast.
     * Keep this method for source compatibility, but never persist high-frequency
     * typing state in Postgres because that creates unnecessary REST traffic.
     */
    suspend fun sendTypingStatus(
        chatId: String,
        userId: String,
        userName: String,
        isTyping: Boolean
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        Result.success(true)
    }

    suspend fun getTypingUsers(chatId: String): Result<List<SupabaseTyping>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_TYPING_STATUS}?chat_id=eq.$chatId&is_typing=eq.true&select=*"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful || resStr.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            val jsonArray = JSONArray(resStr)
            val list = mutableListOf<SupabaseTyping>()
            val now = System.currentTimeMillis()
            for (i in 0 until jsonArray.length()) {
                val item = SupabaseTyping.fromJson(jsonArray.getJSONObject(i))
                if (now - item.updatedAt < 10000L) {
                    list.add(item)
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllRecentTypingUsers(): Result<List<SupabaseTyping>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_TYPING_STATUS}?is_typing=eq.true&select=*"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful || resStr.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            val jsonArray = JSONArray(resStr)
            val list = mutableListOf<SupabaseTyping>()
            val now = System.currentTimeMillis()
            for (i in 0 until jsonArray.length()) {
                val item = SupabaseTyping.fromJson(jsonArray.getJSONObject(i))
                if (now - item.updatedAt < 10000L) {
                    list.add(item)
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePresence(userId: String, isOnline: Boolean, force: Boolean = false): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank()) return@withContext Result.success(false)
            val now = System.currentTimeMillis()

            // Avoid repeated identical presence PATCHes from multiple lifecycle/realtime paths.
            // Online heartbeats are sent at most once every 15 seconds; offline transitions are immediate.
            if (isOnline) {
                val last = presenceUpdateTimes[userId] ?: 0L
                if (!force && now - last < PRESENCE_HEARTBEAT_TTL_MS) {
                    return@withContext Result.success(true)
                }
                presenceUpdateTimes[userId] = now
            } else {
                presenceUpdateTimes.remove(userId)
            }
            val bodyObj = JSONObject().apply {
                put("is_online", isOnline)
                put("last_seen", now)
            }
            val requestBody = bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE)

            val isUuid = userId.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
            val url = if (isUuid) {
                "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?id=eq.$userId"
            } else if (userId.contains("@")) {
                val enc = java.net.URLEncoder.encode(userId.trim(), "UTF-8")
                "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?email=ilike.$enc"
            } else {
                val clean = userId.trim().removePrefix("@").lowercase().removeSuffix(".link")
                val encClean = java.net.URLEncoder.encode(clean, "UTF-8")
                val encLink = java.net.URLEncoder.encode("$clean.link", "UTF-8")
                "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?or=(username.ilike.$encClean,username.ilike.$encLink)"
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserPresence(userId: String): Result<Pair<Boolean, Long>> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank()) return@withContext Result.success(Pair(false, 0L))
            val isUuid = userId.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
            val url = if (isUuid) {
                "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?id=eq.$userId&select=is_online,last_seen"
            } else if (userId.contains("@")) {
                val enc = java.net.URLEncoder.encode(userId.trim(), "UTF-8")
                "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?email=ilike.$enc&select=is_online,last_seen"
            } else {
                val clean = userId.trim().removePrefix("@").lowercase().removeSuffix(".link")
                val encClean = java.net.URLEncoder.encode(clean, "UTF-8")
                val encLink = java.net.URLEncoder.encode("$clean.link", "UTF-8")
                "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?or=(username.ilike.$encClean,username.ilike.$encLink)&select=is_online,last_seen"
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""
            if (!response.isSuccessful || resStr.isBlank()) {
                return@withContext Result.success(Pair(false, 0L))
            }
            val jsonArray = JSONArray(resStr)
            if (jsonArray.length() == 0) return@withContext Result.success(Pair(false, 0L))
            val obj = jsonArray.getJSONObject(0)
            val isOnline = obj.optBoolean("is_online", false)
            val lastSeen = obj.optLong("last_seen", 0L)
            val now = System.currentTimeMillis()
            val diff = if (lastSeen > 0L) Math.abs(now - lastSeen) else Long.MAX_VALUE
            // Strictly active if is_online is true, lastSeen is recorded and updated within the last 30 seconds
            val isRecentlyActive = isOnline && lastSeen > 0L && diff <= 30000L
            Result.success(Pair(isRecentlyActive, lastSeen))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUserPresence(): Result<Map<String, Pair<Boolean, Long>>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?select=id,username,email,full_name,is_online,last_seen"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""
            if (!response.isSuccessful || resStr.isBlank()) {
                return@withContext Result.success(emptyMap())
            }
            val jsonArray = JSONArray(resStr)
            val resultMap = mutableMapOf<String, Pair<Boolean, Long>>()
            val now = System.currentTimeMillis()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "").trim()
                val username = obj.optString("username", "").trim()
                val email = obj.optString("email", "").trim()
                val fullName = obj.optString("full_name", "").trim()
                val isOnline = obj.optBoolean("is_online", false)
                val lastSeen = obj.optLong("last_seen", 0L)
                val diff = if (lastSeen > 0L) Math.abs(now - lastSeen) else Long.MAX_VALUE
                // Strictly active if is_online is true, lastSeen is recorded and updated within the last 30 seconds
                val isRecentlyActive = isOnline && lastSeen > 0L && diff <= 30000L
                val presencePair = Pair(isRecentlyActive, lastSeen)

                if (id.isNotBlank()) {
                    resultMap[id] = presencePair
                    resultMap[id.lowercase()] = presencePair
                }
                if (username.isNotBlank()) {
                    resultMap[username] = presencePair
                    resultMap[username.lowercase()] = presencePair
                    val clean = username.lowercase().removePrefix("@").removeSuffix(".link")
                    resultMap[clean] = presencePair
                    resultMap["$clean.link"] = presencePair
                    resultMap["@$clean"] = presencePair
                    resultMap["@$clean.link"] = presencePair
                }
                if (email.isNotBlank()) {
                    resultMap[email] = presencePair
                    resultMap[email.lowercase()] = presencePair
                    val prefix = email.substringBefore("@").lowercase()
                    resultMap[prefix] = presencePair
                }
                if (fullName.isNotBlank()) {
                    resultMap[fullName] = presencePair
                    resultMap[fullName.lowercase()] = presencePair
                }
            }
            Result.success(resultMap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // CALL SESSIONS (VOICE & VIDEO CALL SIGNALS)
    // ==========================================

    suspend fun createCallSession(call: SupabaseCallSession): Result<SupabaseCallSession> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_CALL_SESSIONS}"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(call.toJson().toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to start call"))
            }

            val jsonArray = try { JSONArray(resStr) } catch (_: Throwable) { JSONArray() }
            val createdCall = if (jsonArray.length() > 0) {
                SupabaseCallSession.fromJson(jsonArray.getJSONObject(0))
            } else {
                call
            }

            // Send instant FCM High-Priority Push Notification for incoming call
            if (call.receiverId.isNotBlank()) {
                @Suppress("OPT_IN_USAGE")
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        com.example.util.FcmPushSender.sendPushToUser(
                            targetUserIdOrName = call.receiverId,
                            type = "call",
                            title = "Incoming ${call.callType} Call",
                            body = "${call.callerName} is calling you...",
                            senderName = call.callerName,
                            chatId = call.id,
                            callType = call.callType,
                            senderAvatar = call.callerAvatar,
                            senderId = call.callerId
                        )
                    } catch (e: Throwable) {
                        Log.w(TAG, "Error sending call FCM push: ${e.message}")
                    }
                }
            }

            Result.success(createdCall)
        } catch (e: Exception) {
            Log.e(TAG, "Error in createCallSession", e)
            Result.failure(e)
        }
    }

    suspend fun updateCallSessionStatus(callId: String, status: String, endedAt: Long? = null, connectedAt: Long? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (callId.isBlank() || status.isBlank()) return@withContext Result.success(false)

            // Terminal call states must never be overwritten by a late WebRTC callback.
            val normalizedStatus = status.uppercase()
            val allowedPreviousStates = when (normalizedStatus) {
                "ACCEPTED" -> "RINGING"
                "CONNECTED" -> "RINGING,ACCEPTED"
                "ENDED", "DECLINED", "CANCELLED" -> "RINGING,ACCEPTED,CONNECTED"
                else -> "RINGING,ACCEPTED,CONNECTED"
            }

            val encodedCallId = java.net.URLEncoder.encode(callId, "UTF-8")
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_CALL_SESSIONS}" +
                "?id=eq.$encodedCallId&status=in.($allowedPreviousStates)&select=id,status,ended_at,connected_at"

            val bodyObj = JSONObject().apply {
                put("status", normalizedStatus)
                if (connectedAt != null) put("connected_at", connectedAt)
                if (endedAt != null) put("ended_at", endedAt)
            }

            for (attempt in 1..3) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("apikey", SupabaseConfig.ANON_KEY)
                        .addHeader("Authorization", "Bearer ${getAccessToken()}")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .patch(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val responseBody = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        Log.w(TAG, "updateCallSessionStatus($callId,$normalizedStatus) HTTP ${response.code}: $responseBody")
                    } else {
                        val rows = try { JSONArray(responseBody).length() } catch (_: Throwable) { 0 }
                        if (rows > 0) {
                            Log.d(TAG, "Call session $callId transitioned to $normalizedStatus")
                            return@withContext Result.success(true)
                        }

                        // Zero rows means the session is already terminal or not eligible for this transition.
                        val current = getCallSession(callId).getOrNull()
                        if (current != null && current.status.equals(normalizedStatus, ignoreCase = true)) {
                            return@withContext Result.success(true)
                        }
                        if (current != null && current.status.uppercase() in setOf("ENDED", "DECLINED", "CANCELLED")) {
                            Log.d(TAG, "Ignoring stale $normalizedStatus transition; session is already ${current.status}")
                            return@withContext Result.success(false)
                        }
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "updateCallSessionStatus attempt $attempt failed: ${e.message}")
                }
                if (attempt < 3) delay(250L * attempt)
            }

            Result.success(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateCallSessionStatus", e)
            Result.failure(e)
        }
    }
    suspend fun getIncomingCalls(userId: String, username: String? = null, email: String? = null): Result<List<SupabaseCallSession>> = withContext(Dispatchers.IO) {
        try {
            val thirtySecondsAgo = System.currentTimeMillis() - 45000
            val ids = mutableListOf(userId)
            if (!username.isNullOrBlank()) {
                ids.add(username)
                ids.add(username.removePrefix("@"))
                ids.add(if (username.endsWith(".link")) username else "$username.link")
            }
            if (!email.isNullOrBlank()) {
                ids.add(email)
            }
            val orFilter = ids.filter { it.isNotBlank() }.distinct().joinToString(",") { 
                val enc = java.net.URLEncoder.encode(it, "UTF-8")
                "receiver_id.eq.$enc" 
            }
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_CALL_SESSIONS}?or=($orFilter)&status=eq.RINGING&started_at=gt.$thirtySecondsAgo&select=*"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch incoming calls"))
            }

            val jsonArray = JSONArray(resStr)
            val list = mutableListOf<SupabaseCallSession>()
            for (i in 0 until jsonArray.length()) {
                list.add(SupabaseCallSession.fromJson(jsonArray.getJSONObject(i)))
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserCallHistory(
        userId: String,
        username: String? = null,
        email: String? = null,
        limit: Int = 100
    ): Result<List<SupabaseCallSession>> = withContext(Dispatchers.IO) {
        try {
            val ids = mutableListOf(userId)
            if (!username.isNullOrBlank()) {
                ids.add(username)
                ids.add(username.removePrefix("@"))
                ids.add(if (username.endsWith(".link")) username else "$username.link")
            }
            if (!email.isNullOrBlank()) ids.add(email)

            val filters = ids.filter { it.isNotBlank() }.distinct().flatMap {
                val enc = java.net.URLEncoder.encode(it, "UTF-8")
                listOf("caller_id.eq.$" + enc, "receiver_id.eq.$" + enc)
            }
            if (filters.isEmpty()) return@withContext Result.success(emptyList())

            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_CALL_SESSIONS}" +
                "?or=(${filters.joinToString(",")})&order=started_at.desc&limit=${limit}&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch call history: ${response.code}"))
            }

            val arr = JSONArray(body)
            val list = mutableListOf<SupabaseCallSession>()
            for (i in 0 until arr.length()) {
                list.add(SupabaseCallSession.fromJson(arr.getJSONObject(i)))
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getUserCallHistory", e)
            Result.failure(e)
        }
    }

    suspend fun getCallSession(callId: String): Result<SupabaseCallSession> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_CALL_SESSIONS}?id=eq.$callId&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to get call session"))
            }
            val jsonArray = JSONArray(resStr)
            if (jsonArray.length() > 0) {
                Result.success(SupabaseCallSession.fromJson(jsonArray.getJSONObject(0)))
            } else {
                Result.failure(Exception("Call session not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setCallSdpOffer(callId: String, offerSdp: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_CALL_SESSIONS}?id=eq.$callId"
            val bodyObj = JSONObject().apply {
                put("sdp_offer", offerSdp)
            }
            var updated = false
            for (attempt in 1..4) {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer ${getAccessToken()}")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .patch(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    updated = true
                    Log.d(TAG, "setCallSdpOffer success on attempt $attempt")
                    break
                }
                Log.w(TAG, "setCallSdpOffer attempt $attempt failed. Retrying...")
                delay(300)
            }
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Error in setCallSdpOffer", e)
            Result.failure(e)
        }
    }

    suspend fun setCallSdpAnswer(callId: String, answerSdp: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val encodedCallId = java.net.URLEncoder.encode(callId, "UTF-8")
            // Do not accept an SDP answer after the call has already been declined/cancelled/ended.
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_CALL_SESSIONS}?id=eq.$encodedCallId&status=in.(RINGING,ACCEPTED)"
            val bodyObj = JSONObject().apply {
                put("sdp_answer", answerSdp)
                put("status", "ACCEPTED")
            }
            var updated = false
            for (attempt in 1..4) {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer ${getAccessToken()}")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .patch(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    updated = true
                    Log.d(TAG, "setCallSdpAnswer success on attempt $attempt")
                    break
                }
                Log.w(TAG, "setCallSdpAnswer attempt $attempt failed. Retrying...")
                delay(300)
            }
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Error in setCallSdpAnswer", e)
            Result.failure(e)
        }
    }

    suspend fun addCallIceCandidate(callId: String, candidateObj: JSONObject): Result<Boolean> = withContext(Dispatchers.IO) {
        val mutex = callCandidateMutexes.getOrPut(callId) { Mutex() }
        mutex.withLock {
            try {
                val currentCandidates = localCallCandidates.getOrPut(callId) {
                    try {
                        val currentSession = getCallSession(callId).getOrNull()
                        JSONArray(currentSession?.iceCandidates ?: "[]")
                    } catch (e: Exception) {
                        JSONArray()
                    }
                }
                currentCandidates.put(candidateObj)

                val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_CALL_SESSIONS}?id=eq.$callId"
                val bodyObj = JSONObject().apply {
                    put("ice_candidates", currentCandidates.toString())
                }
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer ${getAccessToken()}")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .patch(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val response = httpClient.newCall(request).execute()
                Result.success(response.isSuccessful)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ==========================================
    // STORAGE API (PROFILE PHOTOS & MEDIA)
    // ==========================================

    // ==========================================
    // MEDIA API (CLOUDFLARE R2)
    // ==========================================

    suspend fun uploadAvatar(
        fileName: String,
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): Result<String> = withContext(Dispatchers.IO) {
        com.example.data.cloudflare.CloudflareR2Service.uploadFile(
            bytes = imageBytes,
            fileName = fileName,
            mimeType = mimeType,
            folder = "avatars"
        )
    }

    suspend fun uploadAvatar(
        imageBytes: ByteArray,
        fileName: String,
        mimeType: String = "image/jpeg"
    ): Result<String> = uploadAvatar(fileName, imageBytes, mimeType)

    suspend fun uploadChatMedia(
        fileName: String,
        mediaBytes: ByteArray,
        mimeType: String
    ): Result<String> = withContext(Dispatchers.IO) {
        com.example.data.cloudflare.CloudflareR2Service.uploadFile(
            bytes = mediaBytes,
            fileName = fileName,
            mimeType = mimeType,
            folder = "chat_media"
        )
    }

    // ==========================================
    // STATUS / STORIES API
    // ==========================================

    suspend fun postStatusStory(story: SupabaseStatusStory): Result<SupabaseStatusStory> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_STATUS_STORIES}"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(story.toJson().toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to post status"))
            }

            val jsonArray = JSONArray(resStr)
            if (jsonArray.length() > 0) {
                Result.success(SupabaseStatusStory.fromJson(jsonArray.getJSONObject(0)))
            } else {
                Result.success(story)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchActiveStories(): Result<List<SupabaseStatusStory>> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_STATUS_STORIES}?expires_at=gt.$now&order=created_at.desc&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch stories"))
            }

            val jsonArray = JSONArray(resStr)
            val list = mutableListOf<SupabaseStatusStory>()
            for (i in 0 until jsonArray.length()) {
                list.add(SupabaseStatusStory.fromJson(jsonArray.getJSONObject(i)))
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(responseBody: String, defaultMsg: String): String {
        return try {
            val obj = JSONObject(responseBody)
            when {
                obj.has("msg") -> obj.getString("msg")
                obj.has("message") -> obj.getString("message")
                obj.has("error_description") -> obj.getString("error_description")
                obj.has("error") -> obj.getString("error")
                else -> defaultMsg
            }
        } catch (e: Exception) {
            defaultMsg
        }
    }
}
