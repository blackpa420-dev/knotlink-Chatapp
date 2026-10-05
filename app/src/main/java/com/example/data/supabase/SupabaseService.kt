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
            val typesToTry = listOf(type, "signup", "email", "recovery").distinct()
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

            // Fallback to /otp endpoint
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

                if (otpResp.isSuccessful) {
                    Log.d(TAG, "OTP dispatched successfully via /otp to $cleanEmail")
                    return@withContext Result.success(true)
                }

                val otpErr = parseErrorMessage(otpStr, "Failed to send verification code")
                val isOtpRateLimited = otpResp.code == 429 || otpResp.code == 422 ||
                        otpErr.contains("security", ignoreCase = true) ||
                        otpErr.contains("rate", ignoreCase = true) ||
                        otpErr.contains("seconds", ignoreCase = true) ||
                        otpErr.contains("limit", ignoreCase = true) ||
                        otpErr.contains("60", ignoreCase = true) ||
                        otpErr.contains("already", ignoreCase = true)

                if (isOtpRateLimited) {
                    Log.d(TAG, "OTP active/rate limited for $cleanEmail ($otpErr)")
                    return@withContext Result.success(true)
                }
                lastErrStr = otpErr
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
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}"
            val bodyStr = profile.toJson().toString()
            Log.d(TAG, "upsertProfile sending payload: $bodyStr")

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(bodyStr.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "upsertProfile failed HTTP ${response.code}: $resStr")
                val errorMsg = parseErrorMessage(resStr, "Failed to save profile (${response.code})")
                return@withContext Result.failure(Exception(errorMsg))
            }

            Log.d(TAG, "upsertProfile success: $resStr")

            val jsonArray = JSONArray(resStr)
            if (jsonArray.length() > 0) {
                val profile = SupabaseProfile.fromJson(jsonArray.getJSONObject(0))
                cacheProfile(profile)
                Result.success(profile)
            } else {
                Result.success(profile)
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

            val isUuid = raw.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
            if (!isUuid) {
                // If not a UUID, delegate to username or email lookup to avoid Postgres 22P02 UUID syntax error
                val byUname = getProfileByUsername(raw).getOrNull()
                if (byUname != null) return@withContext Result.success(byUname)
                val byEmail = getProfileByEmail(raw).getOrNull()
                return@withContext Result.success(byEmail)
            }

            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?id=eq.$raw&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to load profile: ${response.code}"))
            }

            val jsonArray = JSONArray(resStr)
            if (jsonArray.length() > 0) {
                Result.success(SupabaseProfile.fromJson(jsonArray.getJSONObject(0)))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getProfile", e)
            Result.failure(e)
        }
    }

    suspend fun getProfileByEmail(email: String): Result<SupabaseProfile?> = withContext(Dispatchers.IO) {
        try {
            val clean = email.trim().lowercase()
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?or=(email.ilike.$clean,secondary_email.ilike.$clean)&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to check email: ${response.code}"))
            }

            val jsonArray = JSONArray(resStr)
            if (jsonArray.length() > 0) {
                Result.success(SupabaseProfile.fromJson(jsonArray.getJSONObject(0)))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getProfileByEmail", e)
            Result.failure(e)
        }
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
            
            // Guard against generic non-user placeholder strings that match random profiles
            val genericPlaceholders = setOf(
                "user", "users", "contact", "contacts", "chat", "chat partner", "chat_partner",
                "someone", "me", "you", "admin", "null", "default", "undefined", "member",
                "chat member", "system", "anonymous", "assistant", "ai_assistant", "bitassistant"
            )
            if (genericPlaceholders.contains(raw)) return@withContext Result.success(null)

            val base = raw.removeSuffix(".link").removeSuffix(".bit").removeSuffix(".chat")
            val withSuffix = "$base.link"
            if (genericPlaceholders.contains(base)) return@withContext Result.success(null)

            // Never download the entire profiles table for a single-user lookup.
            // Use the indexed/specific REST query first; cache only avoids repeated identical lookups.
            getCachedProfile(raw)?.let { return@withContext Result.success(it) }
            getCachedProfile(base)?.let { return@withContext Result.success(it) }
            getCachedProfile(withSuffix)?.let { return@withContext Result.success(it) }

            // Query by username or email
            val queryTerms = listOf(base, withSuffix, raw).distinct()
            for (term in queryTerms) {
                val enc = java.net.URLEncoder.encode(term, "UTF-8")
                val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?or=(username.eq.$enc,email.eq.$enc,public_id.eq.$enc,username.ilike.$enc,public_id.ilike.$enc)&select=*"
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
                    if (arr.length() > 0) {
                        val profile = SupabaseProfile.fromJson(arr.getJSONObject(0))
                        cacheProfile(profile)
                        return@withContext Result.success(profile)
                    }
                }
            }

            // 3. If it is a valid UUID, query by id
            val isUuid = raw.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
            if (isUuid) {
                val idUrl = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?id=eq.$raw&select=*"
                val idReq = Request.Builder()
                    .url(idUrl)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer ${getAccessToken()}")
                    .get()
                    .build()
                val idResp = httpClient.newCall(idReq).execute()
                val idBody = idResp.body?.string() ?: ""
                if (idResp.isSuccessful && idBody.isNotBlank()) {
                    val arr = JSONArray(idBody)
                    if (arr.length() > 0) {
                        val profile = SupabaseProfile.fromJson(arr.getJSONObject(0))
                        cacheProfile(profile)
                        return@withContext Result.success(profile)
                    }
                }
            }

            Result.success(null)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getProfileByUsername", e)
            Result.failure(e)
        }
    }

    suspend fun searchProfiles(query: String): Result<List<SupabaseProfile>> = withContext(Dispatchers.IO) {
        try {
            val q = query.trim().removePrefix("@").lowercase()
            if (q.isBlank()) return@withContext Result.success(emptyList())

            val base = q.removeSuffix(".link").removeSuffix(".bit").removeSuffix(".chat")
            val list = mutableListOf<SupabaseProfile>()
            val seenIds = mutableSetOf<String>()

            fun addProfiles(json: String) {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val profile = SupabaseProfile.fromJson(arr.getJSONObject(i))
                    if (profile.id.isNotBlank() && seenIds.add(profile.id)) {
                        list.add(profile)
                    }
                }
            }

            fun get(path: String): String {
                val request = Request.Builder()
                    .url(path)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer ${getAccessToken()}")
                    .get()
                    .build()
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.w(TAG, "Profile search HTTP ${response.code}: $body")
                    return ""
                }
                return body
            }

            val isUuid = q.matches(
                Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
            )

            // Canonical identifier lookup: QR UIDs are resolved directly by profiles.id.
            if (isUuid) {
                val encodedId = java.net.URLEncoder.encode(q, "UTF-8")
                val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}" +
                    "?id=eq.$encodedId&select=*&limit=1"
                val body = get(url)
                if (body.isNotBlank()) addProfiles(body)
                return@withContext Result.success(list)
            }

            // Canonical username lookup: the same endpoint is used by manual search and QR.
            val encodedBase = java.net.URLEncoder.encode(base, "UTF-8")
            val usernameUrl = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}" +
                "?or=(username.ilike.$encodedBase,username.ilike.${encodedBase}.link)&select=*&limit=50"
            val usernameBody = get(usernameUrl)
            if (usernameBody.isNotBlank()) addProfiles(usernameBody)

            // One canonical fallback for legacy public_id / display-name / email searches.
            if (list.isEmpty()) {
                val encodedQ = java.net.URLEncoder.encode(q, "UTF-8")
                val fallbackUrl = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}" +
                    "?or=(username.ilike.%25$encodedQ%25,full_name.ilike.%25$encodedQ%25,email.ilike.%25$encodedQ%25,public_id.ilike.%25$encodedQ%25)&select=*&limit=50"
                val body = get(fallbackUrl)
                if (body.isNotBlank()) addProfiles(body)
            }

            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error in searchProfiles", e)
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(userId: String, fcmToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank() || fcmToken.isBlank()) return@withContext Result.success(false)
            val clean = userId.removePrefix("chat_").removePrefix("user_").removePrefix("@").trim()
            val enc = java.net.URLEncoder.encode(clean, "UTF-8")
            val isUuid = clean.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))

            val bodyObj = JSONObject().apply {
                put("fcm_token", fcmToken)
            }
            val requestBody = bodyObj.toString().toRequestBody("application/json".toMediaType())

            fcmTokenCache[userId] = fcmToken
            fcmTokenCache[clean] = fcmToken

            // 1. If UUID, update directly by id
            if (isUuid) {
                val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?id=eq.$enc"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer ${getAccessToken()}")
                    .addHeader("Prefer", "return=minimal")
                    .patch(requestBody)
                    .build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.i(TAG, "FCM token updated successfully for UUID $clean")
                    return@withContext Result.success(true)
                }
            }

            // 2. If email or contains @, update by email
            if (clean.contains("@")) {
                val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?or=(email.ilike.$enc,secondary_email.ilike.$enc)"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer ${getAccessToken()}")
                    .addHeader("Prefer", "return=minimal")
                    .patch(requestBody)
                    .build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.i(TAG, "FCM token updated successfully for email $clean")
                    return@withContext Result.success(true)
                }
            }

            // 3. Otherwise update by username or fallback email
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}?or=(username.ilike.$enc,email.ilike.$enc)"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Prefer", "return=minimal")
                .patch(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Log.i(TAG, "FCM token updated successfully for username $clean")
            } else {
                Log.w(TAG, "FCM token update response: ${response.code} for $clean")
            }
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token: ${e.message}")
            Result.failure(e)
        }
    }

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
            val withSuffix = "$base.link"
            val encBase = java.net.URLEncoder.encode(base, "UTF-8")
            val encSuffix = java.net.URLEncoder.encode(withSuffix, "UTF-8")
            val encEmail = java.net.URLEncoder.encode("$base@%", "UTF-8")

            // Availability checks must query only matching rows, never download all profiles.
            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_PROFILES}" +
                "?or=(username.ilike.$encBase,username.ilike.$encSuffix,email.ilike.$encEmail)" +
                "&select=id,username,email&limit=20"

            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .get()
                .build()

            val resp = httpClient.newCall(req).execute()
            val str = resp.body?.string() ?: ""
            if (!resp.isSuccessful || str.isBlank()) {
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

    suspend fun ensureChatExists(chatId: String, type: String = "DIRECT"): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (chatId.isBlank()) return@withContext Result.failure(Exception("Chat ID cannot be blank"))
            val currentUid = getAuthenticatedUserId()?.trim().orEmpty()
            if (currentUid.isBlank()) {
                return@withContext Result.failure(Exception("No authenticated Supabase UUID"))
            }

            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_CHATS}"
            val body = JSONObject().apply {
                put("id", chatId)
                put("type", type)
            }
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val bodyText = response.body?.string().orEmpty()
                return@withContext Result.failure(
                    Exception(parseErrorMessage(bodyText, "Failed to create chat (${response.code})"))
                )
            }

            // Every authenticated chat operation starts by registering the current
            // user as a participant. The second participant is added only when the
            // canonical recipient is known.
            val participantUrl = "${SupabaseConfig.REST_BASE_URL}/chat_participants"
            val participantBody = JSONObject().apply {
                put("chat_id", chatId)
                put("user_id", currentUid)
            }
            val participantRequest = Request.Builder()
                .url(participantUrl)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(participantBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(participantRequest).execute().use { participantResponse ->
                if (!participantResponse.isSuccessful) {
                    val participantBody = participantResponse.body?.string().orEmpty()
                    return@withContext Result.failure(
                        Exception(parseErrorMessage(participantBody, "Failed to register chat participant (${participantResponse.code})"))
                    )
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error in ensureChatExists", e)
            Result.failure(e)
        }
    }

    private suspend fun ensureChatParticipants(
        chatId: String,
        senderId: String,
        recipientId: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (chatId.isBlank() || senderId.isBlank() || recipientId.isBlank()) {
                return@withContext Result.failure(Exception("Chat participants require canonical user IDs"))
            }

            val url = "${SupabaseConfig.REST_BASE_URL}/chat_participants"
            fun addParticipant(userId: String): Boolean {
                val body = JSONObject().apply {
                    put("chat_id", chatId)
                    put("user_id", userId)
                }
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer ${getAccessToken()}")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to add chat participant $userId: ${response.code}")
                    }
                    return response.isSuccessful
                }
            }

            if (!addParticipant(senderId)) return@withContext Result.failure(Exception("Could not register sender in chat"))
            if (recipientId != senderId && !addParticipant(recipientId)) {
                return@withContext Result.failure(Exception("Could not register recipient in chat"))
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error in ensureChatParticipants", e)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(message: SupabaseMessage): Result<SupabaseMessage> = withContext(Dispatchers.IO) {
        try {
            val senderId = message.senderId.trim()
            val recipientId = message.recipientId.trim()
            if (senderId.isBlank() || recipientId.isBlank()) {
                return@withContext Result.failure(Exception("Message sender and recipient must be canonical user IDs"))
            }

            val chatResult = ensureChatExists(message.chatId)
            if (chatResult.isFailure) return@withContext Result.failure(chatResult.exceptionOrNull() ?: Exception("Could not create chat"))

            val participantResult = ensureChatParticipants(message.chatId, senderId, recipientId)
            if (participantResult.isFailure) {
                return@withContext Result.failure(
                    participantResult.exceptionOrNull() ?: Exception("Could not register chat participants")
                )
            }

            val url = "${SupabaseConfig.REST_BASE_URL}/${SupabaseConfig.TABLE_MESSAGES}"
            val bodyStr = message.toJson().toString()

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(bodyStr.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(resStr, "Failed to send message (${response.code})")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val jsonArray = JSONArray(resStr)
            if (jsonArray.length() > 0) {
                Result.success(SupabaseMessage.fromJson(jsonArray.getJSONObject(0)))
            } else {
                Result.success(message)
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

    suspend fun uploadAvatar(
        fileName: String,
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): Result<String> = withContext(Dispatchers.IO) {
        // Direct zero-egress Cloudflare R2 upload
        val r2Result = com.example.data.cloudflare.CloudflareR2Service.uploadFile(
            bytes = imageBytes,
            fileName = fileName,
            mimeType = mimeType,
            folder = "avatars"
        )
        if (r2Result.isSuccess) {
            return@withContext r2Result
        }

        try {
            val url = "${SupabaseConfig.STORAGE_BASE_URL}/object/${SupabaseConfig.BUCKET_AVATARS}/$fileName"
            val mediaType = mimeType.toMediaType()

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", mimeType)
                .addHeader("x-upsert", "true")
                .post(imageBytes.toRequestBody(mediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorMsg = response.body?.string() ?: "Upload error"
                return@withContext Result.failure(Exception("Avatar upload failed: $errorMsg"))
            }

            val publicUrl = "${SupabaseConfig.STORAGE_BASE_URL}/object/public/${SupabaseConfig.BUCKET_AVATARS}/$fileName"
            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error in uploadAvatar", e)
            Result.failure(e)
        }
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
        // Direct zero-egress Cloudflare R2 upload
        val r2Result = com.example.data.cloudflare.CloudflareR2Service.uploadFile(
            bytes = mediaBytes,
            fileName = fileName,
            mimeType = mimeType,
            folder = "chat_media"
        )
        if (r2Result.isSuccess) {
            return@withContext r2Result
        }

        try {
            val url = "${SupabaseConfig.STORAGE_BASE_URL}/object/${SupabaseConfig.BUCKET_CHAT_MEDIA}/$fileName"
            val mediaType = mimeType.toMediaType()

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAccessToken()}")
                .addHeader("Content-Type", mimeType)
                .addHeader("x-upsert", "true")
                .post(mediaBytes.toRequestBody(mediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorMsg = response.body?.string() ?: "Upload error"
                return@withContext Result.failure(Exception("Media upload failed: $errorMsg"))
            }

            val publicUrl = "${SupabaseConfig.STORAGE_BASE_URL}/object/public/${SupabaseConfig.BUCKET_CHAT_MEDIA}/$fileName"
            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error in uploadChatMedia", e)
            Result.failure(e)
        }
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
