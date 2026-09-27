package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.app.PictureInPictureParams
import android.util.Rational
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.data.supabase.SupabaseService
import com.example.data.supabase.SupabaseCallSession
import com.example.navigation.IncomingCallOverlay
import com.example.ui.screens.AudioCallScreen
import com.example.ui.screens.VideoCallScreen
import com.example.ui.theme.BitChatTheme
import com.example.ui.viewmodel.BitChatViewModel
import com.example.util.NotificationHelper
import com.example.util.PermissionUtils

/**
 * Lock-screen-safe call task.
 *
 * This activity is intentionally separate from MainActivity so an incoming
 * call never has to create/reveal the normal KnotLink navigation stack.
 * It is launched by the full-screen call notification and uses its own task.
 */
class CallActivity : FragmentActivity() {

    private lateinit var viewModel: BitChatViewModel

    private var callId: String = ""
    private var callerId: String = ""
    private var callerName: String = "Caller"
    private var callType: String = "AUDIO"
    private var inPipMode by mutableStateOf(false)

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun returnToMainActivityForAudioCall() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
                putExtra("return_from_active_audio_call", true)
                putExtra("call_id", callId)
                putExtra("caller_id", callerId)
                putExtra("caller_name", callerName)
                putExtra("call_type", "AUDIO")
            }
            startActivity(intent)
            // Keep receiver Back transition consistent with the call route's
            // non-animated handoff and remove the dedicated call task.
            overridePendingTransition(0, 0)
            finishAndRemoveTask()
        } catch (_: Throwable) {
            moveTaskToBack(true)
        }
    }

    private fun finishCallTask() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(false)
                setTurnScreenOn(false)
            }
        } catch (_: Throwable) {
        }
        finishAndRemoveTask()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showOverLockScreen()

        callId = intent.getStringExtra("call_id") ?: ""
        callerId = intent.getStringExtra("caller_id") ?: ""
        callerName = intent.getStringExtra("caller_name")?.ifBlank { "Caller" } ?: "Caller"
        callType = if (intent.getStringExtra("call_type")?.equals("VIDEO", ignoreCase = true) == true) {
            "VIDEO"
        } else {
            "AUDIO"
        }

        try {
            SupabaseService.init(this)
            NotificationHelper.createNotificationChannels(this)
        } catch (_: Throwable) {
        }

        viewModel = ViewModelProvider(this)[BitChatViewModel::class.java]

        setContent {
            val activeCall by viewModel.activeCall.collectAsState()
            val incomingCall by viewModel.incomingCallSession.collectAsState()
            var accepting by remember { mutableStateOf(false) }
            var callWasActive by remember { mutableStateOf(false) }
            var hadIncomingSession by remember { mutableStateOf(false) }
            var permissionPending by remember { mutableStateOf<SupabaseCallSession?>(null) }

            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                val pending = permissionPending
                permissionPending = null
                if (pending != null && PermissionUtils.hasCallPermissions(this, callType.equals("VIDEO", true))) {
                    accepting = true
                    viewModel.acceptIncomingCall(pending)
                }
            }

            LaunchedEffect(Unit) {
                // Always resolve the live incoming session first. Even the
                // notification Answer action stays inside this call-only task.
                viewModel.handleIncomingCallIntent(
                    callId = callId,
                    callerId = callerId,
                    callerName = callerName,
                    callType = callType,
                    autoAccept = false
                )

                // Match the notification's lifetime. If the remote caller
                // cancels before the session reaches the UI, remove this task.
                kotlinx.coroutines.delay(65_000L)
                if (!viewModel.activeCall.value.isActive &&
                    viewModel.incomingCallSession.value == null
                ) {
                    finishCallTask()
                }
            }

            LaunchedEffect(incomingCall) {
                if (incomingCall != null) {
                    hadIncomingSession = true
                } else if (hadIncomingSession && !activeCall.isActive && !callWasActive) {
                    // Remote decline/cancel before answer: return directly to
                    // the underlying lock/home screen, never to MainActivity.
                    finishCallTask()
                }
                val session = incomingCall ?: return@LaunchedEffect
                if (intent.getBooleanExtra("action_accept_call", false) && !accepting) {
                    if (PermissionUtils.hasCallPermissions(this@CallActivity, callType.equals("VIDEO", true))) {
                        accepting = true
                        viewModel.acceptIncomingCall(session)
                    } else {
                        permissionPending = session
                        permissionLauncher.launch(PermissionUtils.getCallPermissions(callType.equals("VIDEO", true)))
                    }
                    intent.removeExtra("action_accept_call")
                }
            }

            LaunchedEffect(activeCall.isActive) {
                if (activeCall.isActive) {
                    callWasActive = true
                } else if (callWasActive) {
                    // Critical privacy rule: when the call ends, remove the
                    // dedicated call task instead of revealing MainActivity.
                    finishCallTask()
                }
            }

            BitChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    when {
                        activeCall.isActive -> {
                            if (callType.equals("VIDEO", true)) {
                                VideoCallScreen(
                                    contactId = callerId.ifBlank { callId },
                                    contactName = callerName,
                                    viewModel = viewModel,
                                    isInPipMode = inPipMode,
                                    onBackClick = { enterVideoPipOrBackground() },
                                    onEndCallClick = {
                                        viewModel.endCall()
                                        finishCallTask()
                                    }
                                )
                            } else {
                                AudioCallScreen(
                                    contactId = callerId.ifBlank { callId },
                                    contactName = callerName,
                                    viewModel = viewModel,
                                    onBackClick = { returnToMainActivityForAudioCall() },
                                    onEndCallClick = {
                                        viewModel.endCall()
                                        finishCallTask()
                                    },
                                    onChatClick = {
                                        // Chat is intentionally disabled here:
                                        // opening chat would expose app content
                                        // while the device may still be locked.
                                    }
                                )
                            }
                        }

                        incomingCall != null -> {
                            IncomingCallOverlay(
                                call = incomingCall!!,
                                onAccept = {
                                    val isVideo = callType.equals("VIDEO", true)
                                    if (PermissionUtils.hasCallPermissions(this@CallActivity, isVideo)) {
                                        accepting = true
                                        viewModel.acceptIncomingCall(incomingCall!!)
                                    } else {
                                        permissionPending = incomingCall
                                        permissionLauncher.launch(PermissionUtils.getCallPermissions(isVideo))
                                    }
                                },
                                onDecline = {
                                    viewModel.declineIncomingCall(incomingCall!!)
                                    finishCallTask()
                                }
                            )
                        }

                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF050B16)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF60A5FA))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun enterVideoPipOrBackground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE) &&
            callType.equals("VIDEO", ignoreCase = true) &&
            ::viewModel.isInitialized &&
            viewModel.activeCall.value.isActive &&
            !isInPictureInPictureMode
        ) {
            try {
                val builder = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(9, 16))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    builder.setAutoEnterEnabled(true)
                }
                setPictureInPictureParams(builder.build())
                inPipMode = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    window.setBackgroundDrawableResource(android.R.color.transparent)
                }
                enterPictureInPictureMode(builder.build())
                return
            } catch (_: Throwable) {
            }
        }
        moveTaskToBack(true)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPipMode = isInPictureInPictureMode
        if (!isInPictureInPictureMode) {
            window.setBackgroundDrawableResource(android.R.color.black)
        }
    }

    override fun onBackPressed() {
        if (::viewModel.isInitialized && viewModel.activeCall.value.isActive) {
            if (callType.equals("VIDEO", ignoreCase = true)) {
                enterVideoPipOrBackground()
            } else {
                returnToMainActivityForAudioCall()
            }
            return
        }
        finishCallTask()
    }
}
