package com.example.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
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
    isInPipMode: Boolean = false
) {
    android.util.Log.d("BitChat_Debug", "BitChatNavHost composition started")
    val activeCallState by bitChatViewModel.activeCall.collectAsState()
    val incomingCallSession by bitChatViewModel.incomingCallSession.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    val context = LocalContext.current
    var lastBackPressedTime by remember { mutableLongStateOf(0L) }
    android.util.Log.d("BitChat_Debug", "BitChatNavHost current route: $currentRoute")

    val isCallScreenVisible = currentRoute.startsWith("audio_call") || currentRoute.startsWith("video_call")

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
            navController.popBackStack(BitChatRoutes.CHATS, false)
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

    // Ongoing Audio Call notification update
    androidx.compose.runtime.LaunchedEffect(activeCallState.isActive, activeCallState.callType, activeCallState.secondsElapsed) {
        if (activeCallState.isActive && activeCallState.callType == "AUDIO") {
            com.example.util.NotificationHelper.showOngoingAudioCallNotification(
                context = context,
                callerName = activeCallState.contactName.ifBlank { "BitChat User" },
                secondsElapsed = activeCallState.secondsElapsed
            )
        }
    }

    // Auto-navigate to Video Call Screen when system PiP activates so System PiP captures only the video stream
    androidx.compose.runtime.LaunchedEffect(isInPipMode, activeCallState.isActive, activeCallState.callType) {
        if (isInPipMode && activeCallState.isActive && activeCallState.callType == "VIDEO" && !currentRoute.startsWith("video_call")) {
            val route = BitChatRoutes.videoCall(activeCallState.contactId, activeCallState.contactName)
            navController.navigate(route) {
                popUpTo(BitChatRoutes.CHATS)
            }
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
                navController.navigate(route) {
                    popUpTo(BitChatRoutes.CHATS)
                }
            }
            bitChatViewModel.clearPendingCallNavigationRoute()
        }
    }

    // Incoming Call Accept Permission launcher
    var pendingIncomingCallSession by remember { mutableStateOf<com.example.data.supabase.SupabaseCallSession?>(null) }
    val incomingCallPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val call = pendingIncomingCallSession
        if (call != null) {
            pendingIncomingCallSession = null
            bitChatViewModel.acceptIncomingCall(call)
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

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = BitChatRoutes.SPLASH,
            modifier = Modifier.fillMaxSize(),
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
                onCreateAccountClick = {
                    bitChatViewModel.setLoginMode(false)
                    navController.navigate(BitChatRoutes.NUMBER_VERIFICATION)
                },
                onLogInClick = {
                    bitChatViewModel.setLoginMode(true)
                    navController.navigate(BitChatRoutes.NUMBER_VERIFICATION)
                }
            )
        }

        composable(BitChatRoutes.NUMBER_VERIFICATION) {
            EmailAuthScreen(
                viewModel = bitChatViewModel,
                onBackClick = { navController.popBackStack() },
                onOtpSent = {
                    navController.navigate(BitChatRoutes.VERIFY_OTP)
                },
                onLoginSuccess = {
                    navController.navigate(BitChatRoutes.CHATS) {
                        popUpTo(BitChatRoutes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(BitChatRoutes.VERIFY_OTP) {
            VerifyOtpScreen(
                viewModel = bitChatViewModel,
                onVerifySuccess = {
                    val user = bitChatViewModel.userIdentity.value
                    if (user != null && user.username.isNotBlank() && user.isVerified) {
                        navController.navigate(BitChatRoutes.CHATS) {
                            popUpTo(BitChatRoutes.WELCOME) { inclusive = true }
                        }
                    } else {
                        navController.navigate(BitChatRoutes.REGISTER_IDENTITY)
                    }
                }
            )
        }

        composable(BitChatRoutes.REGISTER_IDENTITY) {
            RegisterIdentityScreen(
                viewModel = bitChatViewModel,
                onContinueClick = {
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
                }
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
                }
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
                }
            )
        }

        composable(BitChatRoutes.STORE) {
            StoreScreen(
                viewModel = bitChatViewModel,
                onTabSelected = { tab ->
                    handleTabNavigation(navController, tab)
                }
            )
        }

        composable(BitChatRoutes.SETTINGS) {
            SettingsScreen(
                viewModel = bitChatViewModel,
                onTabSelected = { tab ->
                    handleTabNavigation(navController, tab)
                },
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
                onVideoCallClick = { id, name -> launchCallWithPermission(id, name, true) }
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
                    navController.navigate(BitChatRoutes.chatDetail(contactId, resolvedName)) {
                        popUpTo(BitChatRoutes.CHATS)
                    }
                },
                onEndCallClick = {
                    bitChatViewModel.endCall()
                    navController.popBackStack(BitChatRoutes.CHATS, false)
                }
            )
        }

        composable(
            route = BitChatRoutes.AUDIO_CALL,
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
                    navController.navigate(BitChatRoutes.chatDetail(contactId, resolvedName)) {
                        popUpTo(BitChatRoutes.CHATS)
                    }
                },
                onEndCallClick = {
                    bitChatViewModel.endCall()
                    navController.popBackStack(BitChatRoutes.CHATS, false)
                },
                onChatClick = {
                    navController.navigate(BitChatRoutes.chatDetail(contactId, resolvedName)) {
                        popUpTo(BitChatRoutes.CHATS)
                    }
                }
            )
        }
    }

        if (!isInPipMode && !isCallScreenVisible && activeCallState.isActive && activeCallState.callType == "AUDIO") {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .zIndex(150f)
            ) {
                DynamicIslandCallBanner(
                    callState = activeCallState,
                    onExpandClick = {
                        val route = BitChatRoutes.audioCall(activeCallState.contactId, activeCallState.contactName)
                        navController.navigate(route)
                    }
                )
            }
        }

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
        BiometricLockOverlay(viewModel = bitChatViewModel)

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
    val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse1"
    )
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )

    val isVideo = call.callType.equals("VIDEO", ignoreCase = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF020617),
                        Color(0xFF000000)
                    )
                )
            )
            .zIndex(250f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 56.dp)
        ) {
            // Header Top Label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isVideo) "Incoming HD Video Call" else "Incoming Encrypted Audio Call",
                            color = Color(0xFF34D399),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Center Avatar with Concentric Pulse Rings
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    // Outer Pulse Ring 2
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .scale(pulseScale2)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.12f))
                    )
                    // Inner Pulse Ring 1
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(pulseScale1)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.25f))
                    )
                    // Main Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E293B), Color(0xFF334155))
                                )
                            )
                            .border(2.dp, Color(0xFF10B981), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarUrl = call.callerAvatar
                        if (!avatarUrl.isNullOrBlank()) {
                            coil.compose.SubcomposeAsyncImage(
                                model = avatarUrl,
                                contentDescription = "Caller Avatar",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                loading = {
                                    Text(
                                        text = call.callerName.take(1).uppercase().ifBlank { "U" },
                                        color = Color.White,
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                error = {
                                    Text(
                                        text = call.callerName.take(1).uppercase().ifBlank { "U" },
                                        color = Color.White,
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        } else {
                            Text(
                                text = call.callerName.take(1).uppercase().ifBlank { "U" },
                                color = Color.White,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = call.callerName.ifBlank { "Unknown Caller" },
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ringing...",
                    color = Color(0xFF94A3B8),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Bottom Action Controls (Decline / Accept)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline Call Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Decline Call",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Decline",
                        color = Color(0xFFF87171),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Accept Call Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier
                            .size(76.dp)
                            .scale(pulseScale1)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF10B981), Color(0xFF059669))
                                )
                            )
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = "Accept Call",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Accept",
                        color = Color(0xFF34D399),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
