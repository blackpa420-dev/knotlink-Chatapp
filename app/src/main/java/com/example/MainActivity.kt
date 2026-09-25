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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.navigation.BitChatNavHost
import com.example.ui.theme.BitChatTheme
import com.example.ui.viewmodel.BitChatViewModel
import com.example.util.NotificationHelper

class MainActivity : FragmentActivity() {

  private var isInPipModeState by mutableStateOf(false)
  private var bitChatViewModel: BitChatViewModel? = null
  private var wasCallActiveInSession = false
  private var currentIntentState = mutableStateOf<Intent?>(null)

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    permissions.forEach { (permission, isGranted) ->
      Log.d("BitChat_Debug", "Permission $permission granted: $isGranted")
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
      bitChatViewModel?.handleRemoteCallEnded(callerName)
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

        BitChatTheme {
          Surface(modifier = Modifier.fillMaxSize()) {
            BitChatNavHost(
              bitChatViewModel = vm,
              isInPipMode = isInPipModeState
            )
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
    val actionAcceptCall = intent.getBooleanExtra("action_accept_call", false)
    val actionIncomingCallScreen = intent.getBooleanExtra("action_incoming_call_screen", false) || intent.getBooleanExtra("is_incoming_call", false)
    val callId = intent.getStringExtra("call_id") ?: ""
    val callerId = intent.getStringExtra("caller_id") ?: ""
    val callerName = intent.getStringExtra("caller_name") ?: "Caller"
    val callType = intent.getStringExtra("call_type") ?: "AUDIO"

    val vm = bitChatViewModel ?: return
    if (actionAcceptCall) {
      val currentSession = vm.incomingCallSession.value
      if (currentSession != null) {
        vm.acceptIncomingCall(currentSession)
      } else {
        val finalCallId = callId.ifBlank { "call_${callerName.hashCode()}" }
        val finalCallerId = callerId.ifBlank { "caller_${callerName.hashCode()}" }
        val session = com.example.data.supabase.SupabaseCallSession(
          id = finalCallId,
          callId = finalCallId,
          callerId = finalCallerId,
          receiverId = "me",
          callerName = callerName,
          callType = callType,
          status = "RINGING"
        )
        vm.acceptIncomingCall(session)
      }
    } else if (actionIncomingCallScreen) {
      val finalCallId = callId.ifBlank { "call_${callerName.hashCode()}" }
      val finalCallerId = callerId.ifBlank { "caller_${callerName.hashCode()}" }
      val session = com.example.data.supabase.SupabaseCallSession(
        id = finalCallId,
        callId = finalCallId,
        callerId = finalCallerId,
        receiverId = "me",
        callerName = callerName,
        callType = callType,
        status = "RINGING"
      )
      vm.setIncomingCallSession(session)
    } else {
      val openChatId = intent.getStringExtra("open_chat_id") ?: intent.getStringExtra("chat_id")
      if (!openChatId.isNullOrBlank()) {
        vm.setActiveChatId(openChatId)
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
    try {
      com.example.data.supabase.SupabaseRealtimeManager.onAppBackground()
    } catch (_: Throwable) {}
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


