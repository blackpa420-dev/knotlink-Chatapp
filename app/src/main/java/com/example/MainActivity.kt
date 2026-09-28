package com.example

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.navigation.BitChatNavHost
import com.example.call.ActiveCallBridge
import com.example.ui.theme.BitChatTheme
import com.example.ui.viewmodel.BitChatViewModel
import com.example.util.NotificationHelper

class MainActivity : FragmentActivity() {

  private var isInPipModeState by mutableStateOf(false)
  private var bitChatViewModel: BitChatViewModel? = null
  private var wasCallActiveInSession = false
  private var currentIntentState = mutableStateOf<Intent?>(null)
  private var pendingAcceptedCall: PendingAcceptedCall? = null
  private var isCallScreenVisible by mutableStateOf(false)

  private data class PendingAcceptedCall(
    val callId: String,
    val callerId: String,
    val callerName: String,
    val callType: String
  )

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    permissions.forEach { (permission, isGranted) ->
      Log.d("BitChat_Debug", "Permission $permission granted: $isGranted")
    }

    val pending = pendingAcceptedCall
    if (pending != null) {
      pendingAcceptedCall = null
      val isVideo = pending.callType.equals("VIDEO", ignoreCase = true)
      if (com.example.util.PermissionUtils.hasCallPermissions(this, isVideo)) {
        bitChatViewModel?.handleIncomingCallIntent(
          pending.callId,
          pending.callerId,
          pending.callerName,
          pending.callType,
          autoAccept = true
        )
      } else {
        Log.w("BitChat_Debug", "Incoming call answer cancelled because required permissions were denied")
      }
    }
  }

  private val screenStateReceiver = object : android.content.BroadcastReceiver() {
    override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
      when (intent?.action) {
        android.content.Intent.ACTION_SCREEN_OFF -> {
          Log.d("BitChat_Debug", "Screen OFF: pausing video transmission")
          bitChatViewModel?.setVideoPausedByScreen(true)
        }
        android.content.Intent.ACTION_SCREEN_ON -> {
          Log.d("BitChat_Debug", "Screen ON: resuming video transmission")
          bitChatViewModel?.setVideoPausedByScreen(false)
        }
      }
    }
  }

  private val callEndedReceiver = object : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      val callerName = intent?.getStringExtra("caller_name")
      val callId = intent?.getStringExtra("call_id") ?: intent?.getStringExtra("chat_id")
      bitChatViewModel?.handleRemoteCallEnded(callerName = callerName, callId = callId)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d("BitChat_Debug", "MainActivity onCreate started")
    try {
      enableEdgeToEdge()
      
      val isCall = intent?.getBooleanExtra("action_incoming_call_screen", false) == true || 
                   intent?.getBooleanExtra("is_incoming_call", false) == true ||
                   intent?.getBooleanExtra("action_accept_call", false) == true
      
      updateLockScreenFlags(isCall)
      currentIntentState.value = intent

      com.example.data.supabase.SupabaseService.init(this)
      
      // Configure Coil ImageLoader with GIF support
      val imageLoader = coil.ImageLoader.Builder(this)
        .components {
          if (Build.VERSION.SDK_INT >= 28) {
            add(coil.decode.ImageDecoderDecoder.Factory())
          } else {
            add(coil.decode.GifDecoder.Factory())
          }
        }
        .build()
      coil.Coil.setImageLoader(imageLoader)

      NotificationHelper.createNotificationChannels(this)

      val filter = android.content.IntentFilter().apply {
        addAction(android.content.Intent.ACTION_SCREEN_OFF)
        addAction(android.content.Intent.ACTION_SCREEN_ON)
      }
      registerReceiver(screenStateReceiver, filter)

      try {
        val callEndedFilter = android.content.IntentFilter("com.knotlink.CALL_ENDED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          registerReceiver(callEndedReceiver, callEndedFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
          registerReceiver(callEndedReceiver, callEndedFilter)
        }
      } catch (_: Throwable) {}

      val permissionsToRequest = mutableListOf<String>()
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
          permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
      }
      if (permissionsToRequest.isNotEmpty()) {
        try {
          requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } catch (e: Throwable) {
          Log.w("BitChat_Debug", "Could not request notifications permission: ${e.message}")
        }
      }

      setContent {
        val vm: BitChatViewModel = viewModel()
        bitChatViewModel = vm

        val activeCallState by vm.activeCall.collectAsState()
        val isNightMode by vm.isNightMode.collectAsState()
        val bridgeCallState by ActiveCallBridge.state.collectAsState()
        androidx.compose.runtime.LaunchedEffect(activeCallState.isActive) {
          updateLockScreenFlags(activeCallState.isActive)
          if (activeCallState.isActive) {
            wasCallActiveInSession = true
          } else if (wasCallActiveInSession) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
            if (keyguardManager?.isKeyguardLocked == true) {
              Log.i("BitChat_Debug", "Call ended while keyguard is locked. Terminating MainActivity to prevent data exposure.")
              finishAndRemoveTask()
            }
          }
        }

        val currentIntent by currentIntentState
        androidx.compose.runtime.LaunchedEffect(currentIntent) {
          if (currentIntent != null) {
            handleNotificationIntent(currentIntent)
          }
        }

        BitChatTheme(darkTheme = isNightMode) {
          Surface(modifier = Modifier.fillMaxSize()) {
            val engineState by vm.callEngineState.collectAsState()

            Column(modifier = Modifier.fillMaxSize()) {
              // Do not render a global in-app "Ongoing call" bar here.
              // The compact Voice Call bulletin belongs to ChatDetailScreen,
              // where it does not shift the entire app layout.
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f)
              ) {
                BitChatNavHost(
                  bitChatViewModel = vm,
                  isInPipMode = isInPipModeState,
                  onCallScreenVisibilityChanged = { isCallScreenVisible = it },
                  onVideoCallBackToPip = { enterSystemPipMode() }
                )
              }
            }
          }
        }
      }
      Log.d("BitChat_Debug", "MainActivity setContent completed successfully")
    } catch (e: Exception) {
      Log.e("BitChat_Debug", "Crash in MainActivity onCreate: ${e.message}", e)
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    currentIntentState.value = intent
  }

  private fun handleNotificationIntent(intent: android.content.Intent?) {
    if (intent == null) return
    val returningFromActiveAudioCall = intent.getBooleanExtra("return_from_active_audio_call", false)
    if (returningFromActiveAudioCall) {
      bitChatViewModel?.resumeActiveCallFromBridge()
      return
    }

        val actionAcceptCall = intent.getBooleanExtra("action_accept_call", false)
    val actionIncomingCallScreen = intent.getBooleanExtra("action_incoming_call_screen", false) || intent.getBooleanExtra("is_incoming_call", false)
    val callId = intent.getStringExtra("call_id") ?: ""
    val callerId = intent.getStringExtra("caller_id") ?: ""
    val callerName = intent.getStringExtra("caller_name") ?: "Caller"
    val callType = intent.getStringExtra("call_type") ?: "AUDIO"

    val vm = bitChatViewModel ?: return
    when {
      actionAcceptCall -> {
        val isVideo = callType.equals("VIDEO", ignoreCase = true)
        if (com.example.util.PermissionUtils.hasCallPermissions(this, isVideo)) {
          vm.handleIncomingCallIntent(callId, callerId, callerName, callType, autoAccept = true)
        } else {
          pendingAcceptedCall = PendingAcceptedCall(callId, callerId, callerName, callType)
          requestPermissionLauncher.launch(com.example.util.PermissionUtils.getCallPermissions(isVideo))
        }
      }
      actionIncomingCallScreen -> vm.handleIncomingCallIntent(callId, callerId, callerName, callType, autoAccept = false)
      else -> {
        val openChatId = intent.getStringExtra("open_chat_id") ?: intent.getStringExtra("chat_id")
        if (!openChatId.isNullOrBlank()) vm.setActiveChatId(openChatId)
      }
    }
  }

  override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    val activeCall = bitChatViewModel?.activeCall?.value
    if (activeCall?.isActive == true && activeCall.callType == "VIDEO") {
      isInPipModeState = true
      enterSystemPipMode()
    }
  }

  private fun enterSystemPipMode() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val aspectRatio = Rational(9, 16)
      val params = PictureInPictureParams.Builder()
        .setAspectRatio(aspectRatio)
        .build()
      try {
        enterPictureInPictureMode(params)
      } catch (e: Exception) {
        Log.e("BitChat_Debug", "Error entering PiP mode: ${e.message}")
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      @Suppress("DEPRECATION")
      try {
        enterPictureInPictureMode()
      } catch (e: Exception) {
        Log.e("BitChat_Debug", "Error entering PiP mode: ${e.message}")
      }
    }
  }

  override fun onPictureInPictureModeChanged(
    isInPictureInPictureMode: Boolean,
    newConfig: Configuration
  ) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    isInPipModeState = isInPictureInPictureMode
  }

  override fun onStart() {
    super.onStart()
    try {
      com.example.data.supabase.SupabaseRealtimeManager.onAppForeground()
    } catch (_: Throwable) {}
  }

  override fun onStop() {
    super.onStop()
    // Keep realtime alive for an active call, including system PiP.
    // Otherwise remote hang-up/signaling can be delayed until the activity returns.
    val activeCall = bitChatViewModel?.activeCall?.value
    if (activeCall?.isActive != true) {
      try {
        com.example.data.supabase.SupabaseRealtimeManager.onAppBackground()
      } catch (_: Throwable) {}
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    try {
      com.example.data.supabase.SupabaseRealtimeManager.stopRealtime()
    } catch (_: Throwable) {}
    try {
      unregisterReceiver(screenStateReceiver)
    } catch (_: Throwable) {}
    try {
      unregisterReceiver(callEndedReceiver)
    } catch (_: Throwable) {}
  }

  private fun updateLockScreenFlags(showOverLockScreen: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(showOverLockScreen)
      setTurnScreenOn(showOverLockScreen)
    } else {
      @Suppress("DEPRECATION")
      if (showOverLockScreen) {
        window.addFlags(
          WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
          WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
          WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
          WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
      } else {
        window.clearFlags(
          WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
          WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
          WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
          WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
      }
    }
  }
}


