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

    private val remoteCallEndedReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            val endedId = intent?.getStringExtra("call_id") ?: intent?.getStringExtra("chat_id")
            if (endedId.isNullOrBlank() || endedId == callId) {
                if (::viewModel.isInitialized) {
                    viewModel.handleRemoteCallEnded(
                        callerName = intent?.getStringExtra("caller_name"),
                        callId = endedId
                    )
                }
                // The remote caller cancelled/ended the session. Do not leave the
                // dedicated full-screen CallActivity visible after the signaling
                // state has become terminal. This is especially important when the
                // receiver has not answered yet: there is no active WebRTC call,
                // so the Compose active-call observer alone cannot finish the task.
                finishCallTask()
            }
        }
    }

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
            if (!isTaskRoot) {
                // MainActivity is directly underneath CallActivity now. Finishing
                // this activity gives the receiver the exact same Back behavior
                // as the sender's normal NavController popBackStack().
                finish()
                overridePendingTransition(0, 0)
                return
            }

            // If the call was opened from a notification while the app task did
            // not exist, create the normal app task once and restore the call state.
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("return_from_active_audio_call", true)
                putExtra("call_id", callId)
                putExtra("caller_id", callerId)
                putExtra("caller_name", callerName)
                putExtra("call_type", "AUDIO")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
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
        window.setWindowAnimations(0)

        callId = intent.getStringExtra("call_id") ?: ""
        callerId = intent.getStringExtra("caller_id") ?: ""
        callerName = intent.getStringExtra("caller_name")?.ifBlank { "Caller" } ?: "Caller"
        callType = if (intent.getStringExtra("call_type")?.equals("VIDEO", ignoreCase = true) == true) {
            "VIDEO"
        } else {
            "AUDIO"
        }

        if (callType == "VIDEO") {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        try {
            SupabaseService.init(this)
            NotificationHelper.createNotificationChannels(this)
        } catch (_: Throwable) {
        }

        viewModel = ViewModelProvider(this)[BitChatViewModel::class.java]

        try {
            val filter = android.content.IntentFilter("com.knotlink.CALL_ENDED")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(remoteCallEndedReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(remoteCallEndedReceiver, filter)
            }
        } catch (_: Throwable) {}

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
                kotlinx.coroutines.delay(8_000L)
                if (!viewModel.activeCall.value.isActive &&
                    viewModel.incomingCallSession.value == null
                ) {
                    finishCallTask()
                }
            }

            LaunchedEffect(incomingCall) {
                if (incomingCall != null) {
                    hadIncomingSession = true
                // Do not finish the CallActivity merely because the incoming
                // session becomes null. Accepting a call intentionally clears
                // incomingCallSession before WebRTC flips activeCall to true;
                // finishing here races that transition and can kill the receiver's
                // call before the answer is sent. Remote cancellation is handled by
                // the dedicated CALL_ENDED receiver / active-call status observer.
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

    override fun onDestroy() {
        try { unregisterReceiver(remoteCallEndedReceiver) } catch (_: Throwable) {}
        super.onDestroy()
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
                // Clip the PiP content itself as well as the system PiP window.
                // This matters on OEMs where Surface/Texture content otherwise paints
                // square corners inside the rounded PiP container.
                window.decorView.outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, 18f * resources.displayMetrics.density)
                    }
                }
                window.decorView.clipToOutline = true
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
