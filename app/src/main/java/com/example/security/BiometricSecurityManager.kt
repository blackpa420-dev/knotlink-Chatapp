package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricStatus(val label: String) {
    AVAILABLE("Fingerprint / Biometric Available"),
    NOT_ENROLLED("No biometric enrolled"),
    NO_HARDWARE("No biometric hardware"),
    UNAVAILABLE("Biometric unavailable")
}

enum class LockTimeout(val displayName: String, val durationMillis: Long) {
    IMMEDIATELY("Immediately", 0L),
    ONE_MINUTE("After 1 minute", 60_000L),
    FIVE_MINUTES("After 5 minutes", 300_000L),
    FIFTEEN_MINUTES("After 15 minutes", 900_000L);

    companion object {
        fun fromDisplayName(name: String): LockTimeout {
            return values().find { it.displayName.equals(name, ignoreCase = true) } ?: IMMEDIATELY
        }
    }
}

class BiometricSecurityManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bitchat_biometric_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_APP_UNLOCK = "biometric_app_unlock_enabled"
        private const val KEY_CHAT_LOCK = "biometric_chat_lock_enabled"
        private const val KEY_ADVANCED_SETTINGS = "biometric_advanced_settings_enabled"
        private const val KEY_LOCK_TIMEOUT = "biometric_lock_timeout"
        private const val KEY_LOCKED_CHAT_IDS = "biometric_locked_chat_ids"
        private const val KEY_LAST_UNLOCKED_TIME = "biometric_last_unlocked_timestamp"
    }

    var isAppUnlockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_UNLOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_UNLOCK, value).apply()

    var isChatLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHAT_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_CHAT_LOCK, value).apply()

    var isAdvancedSettingsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ADVANCED_SETTINGS, false)
        set(value) = prefs.edit().putBoolean(KEY_ADVANCED_SETTINGS, value).apply()

    var lockTimeout: LockTimeout
        get() {
            val name = prefs.getString(KEY_LOCK_TIMEOUT, LockTimeout.IMMEDIATELY.displayName)
            return LockTimeout.fromDisplayName(name ?: LockTimeout.IMMEDIATELY.displayName)
        }
        set(value) = prefs.edit().putString(KEY_LOCK_TIMEOUT, value.displayName).apply()

    var lockedChatIds: Set<String>
        get() = prefs.getStringSet(KEY_LOCKED_CHAT_IDS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_LOCKED_CHAT_IDS, value).apply()

    var lastUnlockedTimestamp: Long
        get() = prefs.getLong(KEY_LAST_UNLOCKED_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UNLOCKED_TIME, value).apply()

    fun toggleChatLock(chatId: String): Boolean {
        val current = lockedChatIds.toMutableSet()
        val isLockedNow: Boolean
        if (current.contains(chatId)) {
            current.remove(chatId)
            isLockedNow = false
        } else {
            current.add(chatId)
            isLockedNow = true
        }
        lockedChatIds = current
        return isLockedNow
    }

    fun isChatLocked(chatId: String): Boolean {
        return isChatLockEnabled && lockedChatIds.contains(chatId)
    }

    fun getBiometricStatus(): BiometricStatus {
        return try {
            val biometricManager = BiometricManager.from(context)
            val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else {
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            }

            when (biometricManager.canAuthenticate(authenticators)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.UNAVAILABLE
                else -> {
                    try {
                        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
                            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
                            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
                            else -> BiometricStatus.UNAVAILABLE
                        }
                    } catch (e: Throwable) {
                        BiometricStatus.UNAVAILABLE
                    }
                }
            }
        } catch (e: Throwable) {
            BiometricStatus.UNAVAILABLE
        }
    }

    fun validateEnrollmentOrDisable() {
        try {
            val status = getBiometricStatus()
            if (status == BiometricStatus.NOT_ENROLLED || status == BiometricStatus.NO_HARDWARE) {
                isAppUnlockEnabled = false
                isChatLockEnabled = false
                isAdvancedSettingsEnabled = false
            }
        } catch (e: Throwable) {
            isAppUnlockEnabled = false
            isChatLockEnabled = false
            isAdvancedSettingsEnabled = false
        }
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        description: String = "Scan your fingerprint or enter device PIN to proceed",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                lastUnlockedTimestamp = System.currentTimeMillis()
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Biometric authentication failed. Please try again.")
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                promptInfoBuilder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                promptInfoBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                promptInfoBuilder.setNegativeButtonText("Cancel")
            }
        } catch (e: Exception) {
            promptInfoBuilder.setNegativeButtonText("Cancel")
        }

        try {
            biometricPrompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to initialize biometric prompt")
        }
    }

    fun isAppTimeoutExpired(): Boolean {
        if (!isAppUnlockEnabled) return false
        val elapsed = System.currentTimeMillis() - lastUnlockedTimestamp
        return elapsed >= lockTimeout.durationMillis
    }
}
