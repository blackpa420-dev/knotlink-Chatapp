package com.example.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.util.PermissionUtils
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import com.example.ui.components.BitChatNavTab
import com.example.ui.components.DynamicIslandCallBanner
import com.example.ui.screens.AudioCallScreen
import com.example.ui.screens.BiometricLockOverlay

import com.example.ui.screens.CallsScreen
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatsScreen
import com.example.ui.screens.CreateGroupScreen
import com.example.ui.screens.GroupInfoScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.EmailAuthScreen
import com.example.ui.screens.NumberVerificationScreen
import com.example.ui.screens.RegisterIdentityScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StoreScreen
import com.example.ui.screens.VerifyOtpScreen
import com.example.ui.screens.VideoCallScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.viewmodel.BitChatViewModel
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

object BitChatRoutes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val NUMBER_VERIFICATION = "number_verification"
    const val VERIFY_OTP = "verify_otp"
    const val RESET_PASSWORD = "reset_password"
    const val REGISTER_IDENTITY = "register_identity"
    const val CHATS = "chats"
    const val CONTACTS = "contacts"
    const val CALLS = "calls"
    const val STORE = "store"
    const val SETTINGS = "settings"
    const val CHAT_DETAIL = "chat_detail/{chatId}/{chatName}"
    const val VIDEO_CALL = "video_call/{contactId}?name={contactName}"
    const val AUDIO_CALL = "audio_call/{contactId}?name={contactName}"
    const val QR_SCANNER = "qr_scanner"
    const val CREATE_GROUP = "create_group"
    const val GROUP_INFO = "group_info/{chatId}"

    fun chatDetail(chatId: String, chatName: String): String {
        val encId = android.net.Uri.encode(chatId.ifBlank { "chat" })
        val encName = android.net.Uri.encode(chatName.ifBlank { "User" })
        return "chat_detail/$encId/$encName"
    }
    fun videoCall(contactId: String, contactName: String = ""): String {
        val encId = android.net.Uri.encode(contactId.ifBlank { "user" })
        val encName = android.net.Uri.encode(contactName.ifBlank { "" })
        return "video_call/$encId?name=$encName"
    }
    fun audioCall(contactId: String, contactName: String = ""): String {
        val encId = android.net.Uri.encode(contactId.ifBlank { "user" })
        val encName = android.net.Uri.encode(contactName.ifBlank { "" })
        return "audio_call/$encId?name=$encName"
    }
    fun groupInfo(chatId: String): String {
        val encId = android.net.Uri.encode(chatId.ifBlank { "group" })
        return "group_info/$encId"
    }
}

@Composable
fun BitChatNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    bitChatViewModel: BitChatViewModel = viewModel(),
    isInPipMode: Boolean = false,
    onCallScreenVisibilityChanged: (Boolean) -> Unit = {},
    onVideoCallBackToPip: () -> Unit = {}
) {
    android.util.Log.d("BitChat_Debug", "BitChatNavHost composition started")
    val activeCallState by bitChatViewModel.activeCall.collectAsState()
    val incomingCallSession by bitChatViewModel.incomingCallSession.collectAsState()
    val activeCallBridge by com.example.call.ActiveCallBridge.state.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    val context = LocalContext.current
    var lastBackPressedTime by remember { mutableLongStateOf(0L) }
    android.util.Log.d("BitChat_Debug", "BitChatNavHost current route: $currentRoute")

    val isCallScreenVisible = currentRoute.startsWith("audio_call") || currentRoute.startsWith("video_call")
    androidx.compose.runtime.LaunchedEffect(isCallScreenVisible) { onCallScreenVisibilityChanged(isCallScreenVisible) }

    // A call route is entered before AudioCallScreen/VideoCallScreen has a
    // chance to initialize startCall(). Do NOT immediately pop a newly entered
    // route just because activeCallState is still false. Only pop after this
    // route has actually observed an active call and that call later terminates.
    var callRouteObservedActive by remember(currentRoute) { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(activeCallState.isActive, currentRoute) {
        val isCallRoute = currentRoute.startsWith("audio_call") || currentRoute.startsWith("video_call")
        if (!isCallRoute) {
            callRouteObservedActive = false
        } else if (activeCallState.isActive) {
            callRouteObservedActive = true
        } else if (callRouteObservedActive) {
            // Return to whatever screen launched the call (usually ChatDetail),
            // not the root Chats tab.
            navController.popBackStack()
        }
    }

    // Lazy Permission handling for Notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!PermissionUtils.hasNotificationPermission(context) && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Auto-navigate to Video Call Screen when system PiP activates so System PiP captures only the video stream
    androidx.compose.runtime.LaunchedEffect(isInPipMode, activeCallState.isActive, activeCallState.callType) {
        if (isInPipMode && activeCallState.isActive && activeCallState.callType == "VIDEO" && !currentRoute.startsWith("video_call")) {
            val route = BitChatRoutes.videoCall(activeCallState.contactId, activeCallState.contactName)
            navController.navigate(route)
        }
    }

    // Auto-navigate to Full Screen Call (Video/Audio) when call accepted from notification or overlay
    val pendingCallRoute by bitChatViewModel.pendingCallNavigationRoute.collectAsState()
    androidx.compose.runtime.LaunchedEffect(pendingCallRoute, currentRoute) {
        val route = pendingCallRoute
        if (!route.isNullOrBlank()) {
            val isAlreadyOnRequestedCallScreen =
                (route.startsWith("video_call") && currentRoute.startsWith("video_call")) ||
                (route.startsWith("audio_call") && currentRoute.startsWith("audio_call"))
            if (!isAlreadyOnRequestedCallScreen) {
                navController.navigate(route)
            }
            bitChatViewModel.clearPendingCallNavigationRoute()
        }
    }

    // Incoming Call Accept Permission launcher
    var pendingIncomingCallSession by remember { mutableStateOf<com.example.data.supabase.SupabaseCallSession?>(null) }
    val incomingCallPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val call = pendingIncomingCallSession
        pendingIncomingCallSession = null
        if (call != null) {
            val isVideo = call.callType.equals("VIDEO", ignoreCase = true)
            val granted = PermissionUtils.hasCallPermissions(context, isVideo)
            if (granted) {
                bitChatViewModel.acceptIncomingCall(call)
            } else {
                Toast.makeText(context, "Camera/microphone permission is required to answer the call.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Outgoing Call Permission Launcher
    var pendingOutgoingCallRoute by remember { mutableStateOf<String?>(null) }
    val outgoingCallPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val route = pendingOutgoingCallRoute
        pendingOutgoingCallRoute = null
        if (!route.isNullOrBlank()) {
            val isVideo = route.startsWith("video_call") || route.contains("/video/")
            if (PermissionUtils.hasCallPermissions(context, isVideo)) {
                navController.navigate(route)
            } else {
                Toast.makeText(context, "Permissions are required to place a call 📞", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val launchCallWithPermission = { contactId: String, contactName: String, isVideo: Boolean ->
        val route = if (isVideo) BitChatRoutes.videoCall(contactId, contactName) else BitChatRoutes.audioCall(contactId, contactName)
        if (PermissionUtils.hasCallPermissions(context, isVideo)) {
            navController.navigate(route)
        } else {
            pendingOutgoingCallRoute = route
            outgoingCallPermissionLauncher.launch(PermissionUtils.getCallPermissions(isVideo))
        }
    }

    // Central call-route Back handler. Keeping this at the NavHost level avoids
    // competing nested handlers from the WebRTC renderer/call screen on some devices.
    BackHandler(enabled = isCallScreenVisible) {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        } else {
            (context as? Activity)?.moveTaskToBack(true)
        }
    }

    // Double Back To Exit app on root screens (Welcome, Chats home tab, or root)
    val isRootDestination = currentRoute == BitChatRoutes.CHATS ||
            currentRoute == BitChatRoutes.WELCOME ||
            (navController.previousBackStackEntry == null && currentRoute.isNotEmpty())

    BackHandler(enabled = isRootDestination) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressedTime < 2000) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressedTime = currentTime
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    val openActiveAudioCall = {
        // Rehydrate the existing process-local WebRTC call first. This prevents
        // AudioCallScreen from interpreting the bulletin tap as a brand-new call.
        if (!activeCallState.isActive) {
            bitChatViewModel.resumeActiveCallFromBridge()
        }

        val contactId = if (activeCallState.isActive) {
            activeCallState.contactId
        } else {
            activeCallBridge?.peerId.orEmpty()
        }
        val contactName = if (activeCallState.isActive) {
            activeCallState.contactName
        } else {
            activeCallBridge?.peerName.orEmpty()
        }

        if (contactId.isNotBlank()) {
            navController.navigate(BitChatRoutes.audioCall(contactId, contactName))
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = BitChatRoutes.SPLASH,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            enterTransition = {
                fadeIn(animationSpec = tween(260)) + scaleIn(initialScale = 0.97f, animationSpec = tween(260))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(260)) + scaleIn(initialScale = 0.97f, animationSpec = tween(260))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(180))
            }
        ) {

        composable(BitChatRoutes.SPLASH) {
            com.example.ui.screens.SplashScreen(
                navController = navController,
                bitChatViewModel = bitChatViewModel
            )
        }

        composable(BitChatRoutes.WELCOME) {
            WelcomeScreen(
                onNavigateToLogin = {
                    bitChatViewModel.setLoginMode(true)
                    navController.navigate(BitChatRoutes.NUMBER_VERIFICATION)
                },
                onNavigateToRegister = {
                    bitChatViewModel.setLoginMode(false)
                    navController.navigate(BitChatRoutes.NUMBER_VERIFICATION)
                },
                onNavigateToForgotPassword = {
                    bitChatViewModel.setLoginMode(true)
                    navController.navigate(BitChatRoutes.NUMBER_VERIFICATION)
                }
            )
        }

        composable(BitChatRoutes.NUMBER_VERIFICATION) {
            val isLogin = bitChatViewModel.isLoginMode.collectAsState().value
            EmailAuthScreen(
                bitChatViewModel = bitChatViewModel,
                initialMode = if (isLogin) com.example.ui.screens.AuthMode.LOGIN else com.example.ui.screens.AuthMode.REGISTER,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOtp = { _ ->
                    navController.navigate(BitChatRoutes.VERIFY_OTP)
                },
                onAuthSuccess = {
                    // Email/password authentication is not enough to enter the app.
                    // An account without a completed public profile must resume the
                    // mandatory profile setup instead of being sent to Chats.
                    val identity = bitChatViewModel.userIdentity.value
                    val profileComplete = identity != null &&
                        identity.isVerified &&
                        identity.username.isNotBlank() &&
                        identity.fullName.isNotBlank() &&
                        !identity.avatarPath.isNullOrBlank()

                    if (profileComplete) {
                        navController.navigate(BitChatRoutes.CHATS) {
                            popUpTo(BitChatRoutes.WELCOME) { inclusive = true }
                        }
                    } else {
                        navController.navigate(BitChatRoutes.REGISTER_IDENTITY) {
                            popUpTo(BitChatRoutes.WELCOME) { inclusive = false }
                        }
                    }
                }
            )
        }

        composable(BitChatRoutes.VERIFY_OTP) {
            val email = bitChatViewModel.enteredEmail.collectAsState().value
            val isForgotPass = bitChatViewModel.isForgotPasswordMode.collectAsState().value
            VerifyOtpScreen(
                bitChatViewModel = bitChatViewModel,
                targetEmailOrNumber = email,
                onNavigateBack = { navController.popBackStack() },
                onOtpVerifiedSuccess = {
                    if (isForgotPass) {
                        navController.navigate(BitChatRoutes.RESET_PASSWORD)
                    } else {
                        val user = bitChatViewModel.userIdentity.value
                        if (user != null && user.username.isNotBlank() && user.isVerified) {
                            navController.navigate(BitChatRoutes.CHATS) {
                                popUpTo(BitChatRoutes.WELCOME) { inclusive = true }
                            }
                        } else {
                            navController.navigate(BitChatRoutes.REGISTER_IDENTITY)
                        }
                    }
                }
            )
        }

        composable(BitChatRoutes.RESET_PASSWORD) {
            EmailAuthScreen(
                bitChatViewModel = bitChatViewModel,
                initialMode = com.example.ui.screens.AuthMode.RESET_PASSWORD,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOtp = { _ -> },
                onAuthSuccess = {
                    navController.navigate(BitChatRoutes.NUMBER_VERIFICATION) {
                        popUpTo(BitChatRoutes.WELCOME) { inclusive = false }
                    }
                }
            )
        }

        composable(BitChatRoutes.REGISTER_IDENTITY) {
            RegisterIdentityScreen(
                bitChatViewModel = bitChatViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegistrationComplete = {
                    navController.navigate(BitChatRoutes.CHATS) {
                        popUpTo(BitChatRoutes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(BitChatRoutes.CHATS) {
            ChatsScreen(
                viewModel = bitChatViewModel,
                onChatClick = { chat ->
                    navController.navigate(BitChatRoutes.chatDetail(chat.id, chat.name))
                },
                onVideoCallClick = { contactId ->
                    val contactName = bitChatViewModel.allChats.value.find { it.id == contactId }?.name ?: ""
                    navController.navigate(BitChatRoutes.videoCall(contactId, contactName))
                },
                onOpenQrScanner = {
                    navController.navigate(BitChatRoutes.QR_SCANNER)
                },
                onActiveCallBannerClick = openActiveAudioCall,
                onTabSelected = { tab ->
                    handleTabNavigation(navController, tab)
                }
            )
        }

        composable(BitChatRoutes.QR_SCANNER) {
            QrScannerScreen(
                viewModel = bitChatViewModel,
                onBackClick = { navController.popBackStack() },
                onChatCreated = { chatId, chatName ->
                    navController.navigate(BitChatRoutes.chatDetail(chatId, chatName)) {
                        popUpTo(BitChatRoutes.CHATS)
                    }
                },
                onTabSelected = { tab ->
                    handleTabNavigation(navController, tab)
                }
            )
        }

        composable(BitChatRoutes.CREATE_GROUP) {
            CreateGroupScreen(
                viewModel = bitChatViewModel,
                onNavigateBack = { navController.popBackStack() },
                onGroupCreated = { chatId ->
                    navController.navigate(BitChatRoutes.chatDetail(chatId, "Group Chat")) {
                        popUpTo(BitChatRoutes.CHATS)
                    }
                }
            )
        }

        composable(
            route = BitChatRoutes.GROUP_INFO,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = android.net.Uri.decode(backStackEntry.arguments?.getString("chatId") ?: "")
            GroupInfoScreen(
                chatId = chatId,
                viewModel = bitChatViewModel,
                onNavigateBack = { navController.popBackStack() },
                onLeaveGroup = {
                    navController.popBackStack(BitChatRoutes.CHATS, false)
                },
                onActiveCallBannerClick = openActiveAudioCall
            )
        }

        composable(BitChatRoutes.CONTACTS) {
            ContactsScreen(
                viewModel = bitChatViewModel,
                onContactClick = { contact ->
                    navController.navigate(BitChatRoutes.chatDetail(contact.id, contact.name))
                },
                onVideoCallClick = { contactId ->
                    val contactName = bitChatViewModel.contacts.value.find { it.id == contactId }?.name ?: ""
                    launchCallWithPermission(contactId, contactName, true)
                },
                onTabSelected = { tab ->
                    handleTabNavigation(navController, tab)
                },
                onActiveCallBannerClick = openActiveAudioCall
            )
        }

        composable(BitChatRoutes.CALLS) {
            CallsScreen(
                viewModel = bitChatViewModel,
                onStartVideoCallClick = { contactId ->
                    val contactName = bitChatViewModel.contacts.value.find { it.id == contactId }?.name
                        ?: bitChatViewModel.allChats.value.find { it.id == contactId }?.name ?: ""
                    launchCallWithPermission(contactId, contactName, true)
                },
                onStartAudioCallClick = { contactId ->
                    val contactName = bitChatViewModel.contacts.value.find { it.id == contactId }?.name
                        ?: bitChatViewModel.allChats.value.find { it.id == contactId }?.name ?: ""
                    launchCallWithPermission(contactId, contactName, false)
                },
                onNavigateToChat = { chatId, chatName ->
                    navController.navigate(BitChatRoutes.chatDetail(chatId, chatName))
                },
                onTabSelected = { tab ->
                    handleTabNavigation(navController, tab)
                },
                onActiveCallBannerClick = openActiveAudioCall
            )
        }

        composable(BitChatRoutes.STORE) {
            StoreScreen(
                viewModel = bitChatViewModel,
                onTabSelected = { tab ->
                    handleTabNavigation(navController, tab)
                },
                onActiveCallBannerClick = openActiveAudioCall
            )
        }

        composable(BitChatRoutes.SETTINGS) {
            SettingsScreen(
                viewModel = bitChatViewModel,
                onTabSelected = { tab ->
                    handleTabNavigation(navController, tab)
                },
                onActiveCallBannerClick = openActiveAudioCall,
                onLogoutClick = {
                    navController.navigate(BitChatRoutes.WELCOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = BitChatRoutes.CHAT_DETAIL,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("chatName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = android.net.Uri.decode(backStackEntry.arguments?.getString("chatId") ?: "alex")
            val chatName = android.net.Uri.decode(backStackEntry.arguments?.getString("chatName") ?: "Alex Rivera")

            ChatDetailScreen(
                chatId = chatId,
                chatName = chatName,
                viewModel = bitChatViewModel,
                onBackClick = { navController.popBackStack() },
                onAudioCallClick = { id, name -> launchCallWithPermission(id, name, false) },
                onVideoCallClick = { id, name -> launchCallWithPermission(id, name, true) },
                onActiveCallBannerClick = openActiveAudioCall
            )
        }

        composable(
            route = BitChatRoutes.VIDEO_CALL,
            arguments = listOf(
                navArgument("contactId") { type = NavType.StringType },
                navArgument("contactName") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val contactId = android.net.Uri.decode(backStackEntry.arguments?.getString("contactId") ?: "user")
            val rawName = android.net.Uri.decode(backStackEntry.arguments?.getString("contactName") ?: "")
            val resolvedName = when {
                rawName.isNotBlank() && !rawName.startsWith("chat_", ignoreCase = true) && !rawName.startsWith("group_", ignoreCase = true) -> rawName
                else -> {
                    bitChatViewModel.contacts.value.find { it.id == contactId }?.name
                        ?: bitChatViewModel.allChats.value.find { it.id == contactId }?.name
                        ?: if (contactId.contains("alex", ignoreCase = true)) "Alex Rivera"
                        else if (contactId.contains("sarah", ignoreCase = true)) "Sarah Chen"
                        else if (contactId.contains("evelyn", ignoreCase = true)) "Evelyn Vance"
                        else contactId.removePrefix("chat_").removePrefix("group_").replace("_", " ")
                            .split(" ").filter { it.isNotBlank() }
                            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }.ifBlank { "BitChat User" }
                }
            }
            VideoCallScreen(
                contactId = contactId,
                contactName = resolvedName,
                viewModel = bitChatViewModel,
                isInPipMode = isInPipMode,
                onBackClick = {
                    onVideoCallBackToPip()
                },
                onEndCallClick = {
                    bitChatViewModel.endCall()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = BitChatRoutes.AUDIO_CALL,
            enterTransition = { androidx.compose.animation.EnterTransition.None },
            exitTransition = { androidx.compose.animation.ExitTransition.None },
            popEnterTransition = { androidx.compose.animation.EnterTransition.None },
            popExitTransition = { androidx.compose.animation.ExitTransition.None },
            arguments = listOf(
                navArgument("contactId") { type = NavType.StringType },
                navArgument("contactName") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val contactId = android.net.Uri.decode(backStackEntry.arguments?.getString("contactId") ?: "user")
            val rawName = android.net.Uri.decode(backStackEntry.arguments?.getString("contactName") ?: "")
            val resolvedName = when {
                rawName.isNotBlank() && !rawName.startsWith("chat_", ignoreCase = true) && !rawName.startsWith("group_", ignoreCase = true) -> rawName
                else -> {
                    bitChatViewModel.contacts.value.find { it.id == contactId }?.name
                        ?: bitChatViewModel.allChats.value.find { it.id == contactId }?.name
                        ?: if (contactId.contains("alex", ignoreCase = true)) "Alex Rivera"
                        else if (contactId.contains("sarah", ignoreCase = true)) "Sarah Chen"
                        else if (contactId.contains("evelyn", ignoreCase = true)) "Evelyn Vance"
                        else contactId.removePrefix("chat_").removePrefix("group_").replace("_", " ")
                            .split(" ").filter { it.isNotBlank() }
                            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }.ifBlank { "BitChat User" }
                }
            }
            AudioCallScreen(
                contactId = contactId,
                contactName = resolvedName,
                viewModel = bitChatViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onEndCallClick = {
                    bitChatViewModel.endCall()
                    navController.popBackStack()
                },
                onChatClick = {
                    navController.navigate(BitChatRoutes.chatDetail(contactId, resolvedName)) {
                        popUpTo(BitChatRoutes.CHATS)
                    }
                }
            )
        }
        } // NavHost
    } // Column

        if (!isInPipMode && !isCallScreenVisible && activeCallState.isActive && activeCallState.callType == "VIDEO") {
            com.example.ui.components.FloatingVideoCallPip(
                callState = activeCallState,
                viewModel = bitChatViewModel,
                onExpandClick = {
                    val route = BitChatRoutes.videoCall(activeCallState.contactId, activeCallState.contactName)
                    navController.navigate(route)
                }
            )
        }

        // Incoming Call Full-Screen Interactive Ringing Overlay
        incomingCallSession?.let { call ->
            IncomingCallOverlay(
                call = call,
                onAccept = {
                    val isVideo = call.callType.equals("VIDEO", ignoreCase = true)
                    if (!PermissionUtils.hasCallPermissions(context, isVideo)) {
                        pendingIncomingCallSession = call
                        incomingCallPermissionLauncher.launch(PermissionUtils.getCallPermissions(isVideo))
                    } else {
                        bitChatViewModel.acceptIncomingCall(call)
                    }
                },
                onDecline = {
                    bitChatViewModel.declineIncomingCall(call)
                }
            )
        }

        // Top-Level App Unlock Protection Biometric Overlay
        BiometricLockOverlay(
            viewModel = bitChatViewModel,
            bypassForCallUi = incomingCallSession != null || activeCallState.isActive
        )

        // Global Modern Premium Toast Banner Overlay
        GlobalModernToastOverlay(viewModel = bitChatViewModel)
    }
}

@Composable
fun GlobalModernToastOverlay(viewModel: BitChatViewModel) {
    val currentToast by viewModel.toastEvent.collectAsState()
    androidx.compose.animation.AnimatedVisibility(
        visible = currentToast != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 10.dp, start = 16.dp, end = 16.dp)
            .zIndex(200f)
    ) {
        currentToast?.let { toast ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (toast.isError) Color(0xFF881337) else Color(0xFF064E3B),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.2.dp,
                        color = if (toast.isError) Color(0xFFFDA4AF) else Color(0xFF6EE7B7),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (toast.isError) Icons.Default.Error else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = toast.message,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.dismissToast() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun handleTabNavigation(navController: NavHostController, tab: BitChatNavTab) {

    val targetRoute = when (tab) {
        BitChatNavTab.CHATS -> BitChatRoutes.CHATS
        BitChatNavTab.CALLS -> BitChatRoutes.CALLS
        BitChatNavTab.QR_SCAN -> BitChatRoutes.QR_SCANNER
        BitChatNavTab.SETTINGS -> BitChatRoutes.SETTINGS
    }
    if (navController.currentDestination?.route != targetRoute) {
        navController.navigate(targetRoute) {
            popUpTo(BitChatRoutes.CHATS) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

@Composable
fun IncomingCallOverlay(
    call: com.example.data.supabase.SupabaseCallSession,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "incoming_call_pulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.26f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_alpha"
    )

    val isVideo = call.callType.equals("VIDEO", ignoreCase = true)
    val haptic = LocalHapticFeedback.current
    val surface = Color(0xFF111827)
    val border = Color.White.copy(alpha = 0.10f)

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.97f, animationSpec = tween(320)),
        exit = fadeOut(tween(180)),
        modifier = Modifier.fillMaxSize().zIndex(250f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF050B16), Color(0xFF0B1324), Color(0xFF111827))
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF2563EB).copy(alpha = 0.18f), Color.Transparent),
                            radius = 720f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.055f))
                        .border(1.dp, border, RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isVideo) "Incoming video call" else "Incoming audio call",
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.weight(0.82f))

                Box(
                    modifier = Modifier.size(224.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(214.dp)
                            .scale(ringScale)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB).copy(alpha = glowAlpha))
                    )
                    Box(
                        modifier = Modifier
                            .size(176.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.045f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF4F46E5))))
                                .border(2.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val avatarUrl = call.callerAvatar
                            if (!avatarUrl.isNullOrBlank()) {
                                coil.compose.SubcomposeAsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Caller avatar",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    loading = {
                                        Text(call.callerName.take(1).uppercase().ifBlank { "U" }, color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Bold)
                                    },
                                    error = {
                                        Text(call.callerName.take(1).uppercase().ifBlank { "U" }, color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Bold)
                                    }
                                )
                            } else {
                                Text(call.callerName.take(1).uppercase().ifBlank { "U" }, color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = call.callerName.ifBlank { "Unknown Caller" },
                    color = Color.White,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isVideo) "Wants to start a video call" else "Wants to start an audio call",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(surface.copy(alpha = 0.88f))
                        .border(1.dp, border, RoundedCornerShape(28.dp))
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IncomingCallActionButton(
                        icon = Icons.Default.CallEnd,
                        label = "Decline",
                        background = Color(0xFFE11D48),
                        pressedBackground = Color(0xFFBE123C),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDecline()
                        }
                    )
                    IncomingCallActionButton(
                        icon = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                        label = "Answer",
                        background = Color(0xFF10B981),
                        pressedBackground = Color(0xFF059669),
                        emphasize = true,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAccept()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}


@Composable
private fun IncomingCallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    background: Color,
    pressedBackground: Color,
    emphasize: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(if (isPressed) 0.91f else 1f)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(if (emphasize) 78.dp else 70.dp)
                .clip(CircleShape)
                .background(if (isPressed) pressedBackground else background)
                .border(
                    if (emphasize) 2.dp else 1.dp,
                    Color.White.copy(alpha = if (emphasize) 0.28f else 0.16f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(if (emphasize) 34.dp else 30.dp)
            )
        }
        Spacer(modifier = Modifier.height(9.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

