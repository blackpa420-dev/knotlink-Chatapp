package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.Add
import com.example.ui.components.ChatInputTypingBanner
import com.example.ui.components.TopBarTypingIndicator
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import com.example.util.ImageDownloader
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.data.local.ContactEntity
import com.example.data.local.GroupMemberEntity
import com.example.data.local.PinnedMessageEntity
import com.example.data.local.ReactionEntity
import coil.compose.AsyncImage
import com.example.data.local.MessageEntity
import com.example.ui.components.GlassPanel
import com.example.ui.viewmodel.BitChatViewModel
import com.example.ui.viewmodel.GroupMember
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    chatName: String,
    viewModel: BitChatViewModel,
    onBackClick: () -> Unit,
    onAudioCallClick: (contactId: String, contactName: String) -> Unit = { _, _ -> },
    onVideoCallClick: (contactId: String, contactName: String) -> Unit,
    onActiveCallBannerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val localContext = LocalContext.current
    val messages by remember(chatId) { viewModel.getMessagesForChat(chatId) }.collectAsState()
    val isNightMode by viewModel.isNightMode.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val screenLaunchTime = remember { System.currentTimeMillis() }

    // Theme adaptive colors
    val animBgColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFF0D0E12) else Color(0xFFF8FAFC),
        animationSpec = tween(400),
        label = "chat_bg"
    )

    val animTextColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFFF3F4F6) else Color(0xFF0F172A),
        animationSpec = tween(400),
        label = "chat_text"
    )

    val animSubTextColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
        animationSpec = tween(400),
        label = "chat_subtext"
    )

    // Call Overlay & Menu States
    var isCallOverlayOpen by remember { mutableStateOf(false) }
    var isVideoCallMode by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isSideDrawerOpen by remember { mutableStateOf(false) }
    var isAttachmentMenuOpen by remember { mutableStateOf(false) }
    var showChatProfileModal by remember { mutableStateOf(false) }

    var showPollDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showChecklistDialog by remember { mutableStateOf(false) }
    var clickedMessageTimeId by remember { mutableStateOf<Long?>(null) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    // Voice Recording & View Once States
    var isViewOnce by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isRecordingPaused by remember { mutableStateOf(false) }
    var isRecordingLocked by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableStateOf(0) }
    var recordDragOffsetX by remember { mutableStateOf(0f) }
    var recordingAmplitude by remember { mutableStateOf(0f) }
    var recordingStartedAt by remember { mutableStateOf(0L) }
    var lastRecordingFinishedTime by remember { mutableStateOf(0L) }

    val audioRecordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(localContext, "Microphone permission is required to record voice notes 🎙️", Toast.LENGTH_SHORT).show()
        }
    }

    val view = LocalView.current
    val contacts by viewModel.contacts.collectAsState()
    val isPartnerTyping by viewModel.isPartnerTyping.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var pageLimit by remember { mutableStateOf(30) }

    val userIdentity by viewModel.userIdentity.collectAsState(initial = null)
    val currentAuthUid = userIdentity?.supabaseUid?.ifBlank { userIdentity?.email } ?: "me"
    val reactionsList by remember(chatId) { viewModel.observeReactions(chatId) }.collectAsState(initial = emptyList())
    val pinnedMessages by remember(chatId) { viewModel.observePinnedMessages(chatId) }.collectAsState(initial = emptyList())
    val groupMembers by remember(chatId) { viewModel.observeGroupMembers(chatId) }.collectAsState(initial = emptyList())
    val clipboardManager = LocalClipboardManager.current

    var selectedMessageForAction by remember { mutableStateOf<MessageEntity?>(null) }
    var activeReplyMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var editingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<MessageEntity?>(null) }
    var showForwardSheet by remember { mutableStateOf(false) }
    var messageToForward by remember { mutableStateOf<MessageEntity?>(null) }
    var showPinnedSheet by remember { mutableStateOf(false) }
    var showPinConfirmDialog by remember { mutableStateOf(false) }
    var messageToPin by remember { mutableStateOf<MessageEntity?>(null) }
    var showTranslateDialog by remember { mutableStateOf(false) }
    var messageToTranslate by remember { mutableStateOf<MessageEntity?>(null) }
    val selectedForwardChatIds = remember { mutableStateListOf<String>() }

    // Filter out messages deleted for me, guaranteed chronological ordering
    val validMessages = remember(messages) {
        messages
            .filter {
                !it.isDeletedForMe &&
                    !it.isDeletedForEveryone &&
                    !(it.messageType == "SYSTEM_EVENT" && it.text.startsWith("[REACTION:"))
            }
            .sortedWith(compareBy({ it.timestamp }, { it.id }))
    }

    // Precompute per-message display metadata once per message-list update.
    // Keep expensive date formatting and reaction matching out of LazyColumn rows.
    val messageDateHeaders = remember(validMessages) {
        val calendar = java.util.Calendar.getInstance()
        val todayDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val todayYear = calendar.get(java.util.Calendar.YEAR)
        val formatter = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault())

        buildMap<Long, String>(validMessages.size) {
            validMessages.forEach { message ->
                val label = if (message.timestamp <= 0L) {
                    "Today"
                } else {
                    calendar.timeInMillis = message.timestamp
                    val messageDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
                    val messageYear = calendar.get(java.util.Calendar.YEAR)
                    when {
                        todayYear == messageYear && todayDay == messageDay -> "Today"
                        todayYear == messageYear && todayDay - 1 == messageDay -> "Yesterday"
                        else -> formatter.format(java.util.Date(message.timestamp))
                    }
                }
                put(message.id, label)
            }
        }
    }

    val reactionsByLocalMessageId = remember(validMessages, reactionsList) {
        val messageKeys = HashMap<String, Long>(validMessages.size * 2)
        validMessages.forEach { message ->
            messageKeys[message.id.toString()] = message.id
            message.serverMessageId?.takeIf { it.isNotBlank() }?.let { serverId ->
                messageKeys[serverId] = message.id
            }
        }

        val grouped = HashMap<Long, MutableList<ReactionEntity>>()
        reactionsList.forEach { reaction ->
            messageKeys[reaction.messageId]?.let { localId ->
                grouped.getOrPut(localId) { mutableListOf() }.add(reaction)
            }
        }
        grouped.mapValues { (_, value) -> value.toList() }
    }

    // Display paginated messages (30 per page, latest at the end)
    val displayedMessages = remember(validMessages, pageLimit) {
        if (validMessages.size <= pageLimit) validMessages else validMessages.takeLast(pageLimit)
    }

    val isUserNearBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) true
            else {
                val lastVisibleItem = visibleItems.last()
                val totalItems = layoutInfo.totalItemsCount
                lastVisibleItem.index >= totalItems - 3
            }
        }
    }

    var unreadNewMessagesCount by remember { mutableStateOf(0) }
    var previousMessageCount by remember { mutableStateOf(0) }
    var hasInitialScrolled by remember { mutableStateOf(false) }

    DisposableEffect(chatId) {
        viewModel.setActiveChatId(chatId)
        onDispose {
            viewModel.setUserTyping(chatId, false)
            viewModel.setActiveChatId(null)
        }
    }

    LaunchedEffect(chatId) {
        viewModel.listenTypingStatusForChat(chatId)
        // Do not mark messages read merely because the chat route opened.
        // A message becomes READ only after the user has actually reached the
        // latest visible area of the conversation.
        pageLimit = 30
        unreadNewMessagesCount = 0
        hasInitialScrolled = false
    }

    LaunchedEffect(isUserNearBottom, hasInitialScrolled, messages.size, chatId) {
        if (!hasInitialScrolled || !isUserNearBottom) return@LaunchedEffect

        val unreadServerMessageIds = messages
            .filter { !it.isFromUser && (it.deliveryState != "READ" || !it.isRead) }
            .mapNotNull { it.serverMessageId?.takeIf { id -> id.isNotBlank() } }

        if (unreadServerMessageIds.isNotEmpty()) {
            viewModel.markMessagesAsRead(chatId, unreadServerMessageIds)
        }
        viewModel.markUserMessagesAsRead(chatId)
    }

    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)

    // Auto-scroll logic: Instant scroll to latest message without visible jump/sliding
    LaunchedEffect(displayedMessages.size, messages.lastOrNull()?.id) {
        if (displayedMessages.isNotEmpty()) {
            val targetIndex = displayedMessages.size
            listState.scrollToItem(targetIndex)
            hasInitialScrolled = true
            unreadNewMessagesCount = 0
            previousMessageCount = messages.size
        }
    }

    // Reset unread indicator when user scrolls to bottom
    LaunchedEffect(isUserNearBottom) {
        if (isUserNearBottom) {
            unreadNewMessagesCount = 0
        }
    }

    // Pagination: Only load previous 30 messages when user explicitly scrolls near top
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (hasInitialScrolled && listState.isScrollInProgress && listState.firstVisibleItemIndex == 0 && validMessages.size > pageLimit) {
            val prevTopIndex = listState.firstVisibleItemIndex
            val prevOffset = listState.firstVisibleItemScrollOffset
            pageLimit = minOf(pageLimit + 30, validMessages.size)
            delay(30)
            listState.scrollToItem(prevTopIndex + 30, prevOffset)
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingStartedAt = System.currentTimeMillis()
            recordSeconds = 0
            while (isRecording) {
                kotlinx.coroutines.delay(80L)
                if (!isRecordingPaused) {
                    recordSeconds = ((System.currentTimeMillis() - recordingStartedAt).coerceAtLeast(0L) / 1000L).toInt()
                    val raw = com.example.util.AudioRecorderManager.currentAmplitude()
                    recordingAmplitude = (raw / 32767f).coerceIn(0f, 1f)
                } else {
                    recordingAmplitude = 0f
                }
            }
        } else {
            recordingAmplitude = 0f
        }
    }

    val scope = rememberCoroutineScope()
    val mutedChatIds by viewModel.mutedChatIds.collectAsState()
    val isMuted = mutedChatIds.contains(chatId)

    // Multi-photo picker & pending attachments state
    val pendingSelectedImages = remember { mutableStateListOf<Uri>() }
    var isUploadingPendingImages by remember { mutableStateOf(false) }

    // Activity Result Launchers for attachments
    val multipleGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            pendingSelectedImages.addAll(uris)
        }
    }

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val total = uris.size
                Toast.makeText(localContext, "Uploading $total document${if (total > 1) "s" else ""}...", Toast.LENGTH_SHORT).show()
                uris.forEachIndexed { index, fileUri ->
                    try {
                        val details = com.example.util.FileHelper.getFileDetails(localContext, fileUri)
                        val mimeType = if (details.mimeType.isNotBlank()) details.mimeType else (localContext.contentResolver.getType(fileUri) ?: "application/octet-stream")
                        val downloadUrl = viewModel.uploadMedia(chatId, fileUri, mimeType, localContext) { _ -> }
                        val formattedMsg = "📄 [DOCUMENT_FILE|${details.name}|${details.formattedSize}|${details.mimeType}|$downloadUrl]"
                        viewModel.sendMessage(chatId, formattedMsg)
                    } catch (e: Exception) {
                        Toast.makeText(localContext, "Upload failed for item ${index + 1}: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
                Toast.makeText(localContext, "$total document${if (total > 1) "s" else ""} shared securely", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempPhotoUri != null) {
            val fileUri = tempPhotoUri!!
            pendingSelectedImages.add(fileUri)
        }
    }

    // Online status determination & RTDB presence/typing
    val allChats by viewModel.allChats.collectAsState()
    val currentChat = remember(allChats, chatId) { allChats.find { it.id == chatId } }
    val isGroupChat = currentChat?.chatType == "GROUP" || chatName.contains("[Group]", ignoreCase = true) || chatId.startsWith("group_")
    val currentUid = currentAuthUid
    val isOwner = currentChat?.ownerUid == currentUid || groupMembers.any { it.uid == currentUid && it.role == "OWNER" }
    val isAdmin = isOwner || groupMembers.any { it.uid == currentUid && it.role == "ADMIN" }
    val onlyAdminsCanSend = isGroupChat && currentChat?.permissions?.contains("onlyAdminsCanMessage=true") == true
    val isRestrictedFromSending = onlyAdminsCanSend && !isAdmin
    val partnerUid: String = remember(currentChat, chatId, currentUid, userIdentity, contacts) {
        val myKeys = setOfNotNull(
            currentUid,
            userIdentity?.supabaseUid,
            userIdentity?.email,
            userIdentity?.username,
            userIdentity?.username?.let { "$it.link" }
        ).map { it.trim().lowercase() }.filter { it.isNotBlank() }

        val parts: List<String> = currentChat?.participantUids?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val fromParts = parts.firstOrNull { part ->
            val pLower = part.lowercase()
            !myKeys.contains(pLower) && !myKeys.any { my -> pLower == my || pLower == "$my.link" || my == "$pLower.link" }
        }
        if (!fromParts.isNullOrBlank()) {
            fromParts
        } else {
            val name = currentChat?.name
            val contactMatch = contacts.firstOrNull { it.id == chatId || (name != null && it.name.equals(name, ignoreCase = true)) }
            val fallbackUid: String = contactMatch?.id
                ?: currentChat?.ownerUid?.takeIf { it != currentUid && it.isNotBlank() }
                ?: name?.takeIf { it.isNotBlank() && !it.contains("Group") }
                ?: if (chatId.startsWith("chat_")) chatId.removePrefix("chat_") else "uid_target"
            fallbackUid
        }
    }

    val partnerContact = remember(contacts, partnerUid, currentChat?.name) {
        contacts.firstOrNull { it.id == partnerUid || it.name.equals(currentChat?.name, ignoreCase = true) }
    }
    var remoteFullName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(chatId, partnerUid) {
        if (!isGroupChat && partnerUid.isNotBlank() && partnerUid != "uid_target") {
            // ViewModel owns the cached/single profile lookup.
            viewModel.refreshPartnerProfile(chatId, partnerUid)
        }
    }

    val groupNameMap by viewModel.groupNameMap.collectAsState()
    val membersMap by viewModel.groupMembersMap.collectAsState()
    val groupMembersList = membersMap[chatId] ?: emptyList()
    val activeMemberCount = if (groupMembersList.isNotEmpty()) groupMembersList.size else 4

    val displayHeaderName = remember(currentChat?.name, chatName, groupNameMap[chatId], isGroupChat, partnerContact, remoteFullName) {
        val resolvedName = remoteFullName 
            ?: (if (currentChat?.name?.isNotBlank() == true && currentChat.name != "User" && currentChat.name != "Contact") currentChat.name else null)
            ?: partnerContact?.name
            ?: currentChat?.name
            ?: (groupNameMap[chatId] ?: chatName)

        if (isGroupChat) {
            val clean = resolvedName.replace("[Group]", "", ignoreCase = true).trim()
            if (clean.startsWith("GC ", ignoreCase = true)) clean else "GC $clean"
        } else {
            resolvedName
        }
    }

    val presenceFlow = remember(partnerUid, currentChat?.name, partnerContact, remoteFullName, currentChat?.ownerUid) {
        val keys = listOfNotNull(
            partnerUid,
            currentChat?.name,
            currentChat?.ownerUid?.takeIf { it != currentUid },
            partnerContact?.id,
            partnerContact?.name,
            remoteFullName,
            if (chatId.startsWith("chat_")) chatId.removePrefix("chat_") else null
        ).filter { it.isNotBlank() && it != "uid_target" }.distinct()
        viewModel.observeMultiUserPresence(keys)
    }
    val presence by presenceFlow.collectAsState(initial = Pair(false, 0L))
    val isUserOnline = presence.first
    val lastSeenTimestamp = presence.second

    val typingFlow = remember(chatId, partnerUid) { viewModel.observeTyping(chatId, partnerUid) }
    val rtdbTyping by typingFlow.collectAsState(initial = false)
    val typingMap by com.example.data.supabase.SupabaseRealtimeManager.typingUsersByChat.collectAsState()
    val unChatId = remember(chatId) { if (chatId.startsWith("chat_")) chatId.removePrefix("chat_") else "chat_$chatId" }
    val isRealtimeTyping = remember(typingMap, chatId, unChatId) {
        val list1 = typingMap[chatId]
        val list2 = typingMap[unChatId]
        (!list1.isNullOrEmpty()) || (!list2.isNullOrEmpty())
    }
    val effectiveTyping = isPartnerTyping || rtdbTyping || isRealtimeTyping

    val lastSeenText = remember(isUserOnline, lastSeenTimestamp) {
        if (isUserOnline) "Online"
        else if (lastSeenTimestamp > 0L) {
            val diff = System.currentTimeMillis() - lastSeenTimestamp
            val mins = diff / (1000 * 60)
            if (mins < 1) "Last seen just now"
            else if (mins < 60) "Last seen $mins min${if (mins > 1) "s" else ""} ago"
            else {
                val hours = mins / 60
                if (hours < 24) "Last seen $hours hour${if (hours > 1) "s" else ""} ago"
                else "Offline"
            }
        } else "Offline"
    }

    BackHandler {
        when {
            isSideDrawerOpen -> isSideDrawerOpen = false
            showChatProfileModal -> showChatProfileModal = false
            isAttachmentMenuOpen -> isAttachmentMenuOpen = false
            isCallOverlayOpen -> isCallOverlayOpen = false
            else -> onBackClick()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(animBgColor)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
        ) {
            // Modern Curvy Header Bar with Drag Gesture for Options Drawer
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount > 15f) {
                                isSideDrawerOpen = true
                            }
                        }
                    },
                shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
                color = if (isNightMode) Color(0xFF14151C) else Color.White,
                shadowElevation = if (isNightMode) 0.dp else 4.dp
            ) {
                val isAssistant = chatId == "bitassistant" || displayHeaderName.contains("Assistant", ignoreCase = true) || currentChat?.avatarType == "assistant" || currentChat?.name?.contains("Assistant", ignoreCase = true) == true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showChatProfileModal = true }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = animTextColor
                            )
                        }

                        val chatAvatar = currentChat?.avatarType ?: ""
                        val hasCustomAvatar = chatAvatar.isNotBlank() && (
                            chatAvatar.startsWith("http://", ignoreCase = true) ||
                            chatAvatar.startsWith("https://", ignoreCase = true) ||
                            chatAvatar.startsWith("content://", ignoreCase = true) ||
                            chatAvatar.startsWith("file://", ignoreCase = true) ||
                            chatAvatar.startsWith("/")
                        )

                        Box(contentAlignment = Alignment.BottomEnd) {
                            if (isAssistant) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF222226))
                                        .border(1.dp, Color(0xFF3A3A3E), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = "Bot Avatar",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else if (hasCustomAvatar) {
                                AsyncImage(
                                    model = chatAvatar,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                if (isGroupChat)
                                                    listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                                                else
                                                    listOf(Color(0xFF2563EB), Color(0xFF00C6FF))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = displayHeaderName.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            if (!isGroupChat && !isAssistant) {
                                val dotColor = if (isUserOnline) Color(0xFF22C55E) else Color(0xFFEF4444)
                                Box(
                                    modifier = Modifier
                                        .size(13.dp)
                                        .offset(x = 1.dp, y = 1.dp)
                                        .clip(CircleShape)
                                        .background(if (isNightMode) Color(0xFF141416) else Color.White)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 2.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = displayHeaderName,
                                    color = animTextColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                if (isMuted) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.NotificationsOff,
                                        contentDescription = "Muted",
                                        tint = animSubTextColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            if (effectiveTyping || isGroupChat || isAssistant) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (effectiveTyping) {
                                        TopBarTypingIndicator(
                                            color = Color(0xFF2563EB),
                                            fontSize = 11.sp
                                        )
                                    } else if (isAssistant) {
                                        Text(
                                            text = "Personal Safe Storage",
                                            color = Color(0xFF10B981),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else if (isGroupChat) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$activeMemberCount members",
                                            color = Color(0xFF10B981),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Audio & Video Call Header Buttons (Dark Red Audio Call, Premium Blue Video Call)
                    if (!isAssistant) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            val audioInteraction = remember { MutableInteractionSource() }
                            val isAudioPressed by audioInteraction.collectIsPressedAsState()
                            val audioScale by animateFloatAsState(
                                targetValue = if (isAudioPressed) 0.90f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                label = "audio_press_scale"
                            )

                            // Audio Call Button - Dark Red Round Circle
                            Surface(
                                onClick = { onAudioCallClick(chatId, displayHeaderName) },
                                interactionSource = audioInteraction,
                                shape = CircleShape,
                                color = Color.Transparent,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .size(40.dp)
                                    .graphicsLayer {
                                        scaleX = audioScale
                                        scaleY = audioScale
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFFDC2626), Color(0xFF991B1B))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Audio Call",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            val videoInteraction = remember { MutableInteractionSource() }
                            val isVideoPressed by videoInteraction.collectIsPressedAsState()
                            val videoScale by animateFloatAsState(
                                targetValue = if (isVideoPressed) 0.90f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                label = "video_press_scale"
                            )

                            // Video Call Button - Premium Blue Round Circle (Voice Message Style)
                            Surface(
                                onClick = { onVideoCallClick(chatId, displayHeaderName) },
                                interactionSource = videoInteraction,
                                shape = CircleShape,
                                color = Color.Transparent,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .size(40.dp)
                                    .graphicsLayer {
                                        scaleX = videoScale
                                        scaleY = videoScale
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Video Call",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            com.example.ui.components.ActiveCallBulletinSlot(
                viewModel = viewModel,
                onExpandClick = onActiveCallBannerClick
            )

            // Sticky Pinned Message Banner
            androidx.compose.animation.AnimatedVisibility(
                visible = pinnedMessages.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val latestPinned = pinnedMessages.lastOrNull()
                val pinnedMsgEntity = validMessages.find { it.serverMessageId == latestPinned?.messageId || it.id.toString() == latestPinned?.messageId }
                Surface(
                    onClick = {
                        if (pinnedMessages.size > 1) {
                            showPinnedSheet = true
                        } else if (pinnedMsgEntity != null) {
                            val idx = displayedMessages.indexOfFirst { it.id == pinnedMsgEntity.id }
                            if (idx >= 0) {
                                coroutineScope.launch { listState.animateScrollToItem(idx) }
                            }
                        }
                    },
                    color = if (isNightMode) Color(0xFF1E202B) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isNightMode) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (pinnedMessages.size > 1) "Pinned Messages (${pinnedMessages.size})" else "Pinned Message",
                                color = Color(0xFFF59E0B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = pinnedMsgEntity?.text ?: "Pinned message",
                                color = animTextColor,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = { showPinnedSheet = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "View All",
                                color = Color(0xFF2563EB),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Chat Messages List with Floating New Message Indicator & Pagination
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        // Date Separator Badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isNightMode) Color.White.copy(alpha = 0.06f) else Color(0xFFE2E8F0)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Showing ${displayedMessages.size} of ${validMessages.size} messages • Scroll up for older",
                                    color = animSubTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    itemsIndexed(
                        displayedMessages,
                        key = { _, msg -> msg.id },
                        contentType = { _, _ -> "message_row" }
                    ) { index, msg ->
                        // Show date pill when day changes
                        val prevMsg = if (index > 0) displayedMessages[index - 1] else null
                        val currentHeader = messageDateHeaders[msg.id] ?: "Today"
                        val prevHeader = prevMsg?.let { messageDateHeaders[it.id] }

                        if (prevHeader == null || currentHeader != prevHeader) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isNightMode) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)
                                        )
                                        .padding(horizontal = 14.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = currentHeader,
                                        color = animSubTextColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        val msgReactions = reactionsByLocalMessageId[msg.id].orEmpty()
                        val isLastMsg = displayedMessages.lastOrNull()?.id == msg.id
                        val isTimeClicked = clickedMessageTimeId == msg.id
                        val currentAuthName = userIdentity?.fullName?.ifBlank { userIdentity?.username }?.ifBlank { "You" } ?: "You"
                        val currentAuthAvatar = userIdentity?.avatarPath ?: ""

                        val entryAlpha = remember(msg.id) { androidx.compose.animation.core.Animatable(0f) }
                        val entrySlide = remember(msg.id) { androidx.compose.animation.core.Animatable(32f) }

                        LaunchedEffect(msg.id) {
                            if (msg.timestamp > (screenLaunchTime - 3000L)) {
                                kotlinx.coroutines.joinAll(
                                    launch {
                                        entryAlpha.animateTo(
                                            targetValue = 1f,
                                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                        )
                                    },
                                    launch {
                                        entrySlide.animateTo(
                                            targetValue = 0f,
                                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                        )
                                    }
                                )
                            } else {
                                entryAlpha.snapTo(1f)
                                entrySlide.snapTo(0f)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = entryAlpha.value
                                    translationY = entrySlide.value
                                }
                        ) {
                            SwipeableMessageRow(
                                onSwipeReply = {
                                    activeReplyMessage = msg
                                },
                                onSwipeForward = {
                                    messageToForward = msg
                                    selectedForwardChatIds.clear()
                                    showForwardSheet = true
                                }
                            ) {
                                FigmaMessageBubbleRow(
                                    message = msg,
                                    isNightMode = isNightMode,
                                    animTextColor = animTextColor,
                                    reactions = msgReactions,
                                    currentUid = currentAuthUid,
                                    currentUserName = currentAuthName,
                                    currentUserAvatar = currentAuthAvatar,
                                    isLastMessage = isLastMsg,
                                    showDetails = isLastMsg || isTimeClicked,
                                    onClick = {
                                        clickedMessageTimeId = if (clickedMessageTimeId == msg.id) null else msg.id
                                    },
                                    // Read state is server-driven; tapping a bubble must
                                    // never toggle the sent-message status by itself.
                                    onToggleReadStatus = null,
                                    onLongClick = {
                                        selectedMessageForAction = msg
                                    },
                                    onReactionClick = { emoji ->
                                        viewModel.toggleReaction(chatId, msg, emoji)
                                    },
                                    onVote = { updatedPollText ->
                                        viewModel.editMessage(chatId, msg.id, msg.serverMessageId, updatedPollText)
                                    },
                                    onReplyQuoteClick = { targetMsgId ->
                                        val targetIndex = displayedMessages.indexOfFirst { it.serverMessageId == targetMsgId || it.id.toString() == targetMsgId }
                                        if (targetIndex >= 0) {
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(targetIndex)
                                            }
                                        }
                                    },
                                    onDeleteMessage = { message ->
                                        val rawText = message.text
                                        val parts = rawText.split("|")
                                        val dur = if (parts.size >= 3) parts[2].substringBefore("]") else "00:00"
                                        viewModel.editMessage(chatId, message.id, message.serverMessageId, "[VIEW_ONCE_OPENED|$dur]")
                                    }
                                )
                            }
                        }
                    }

                }

                // New Messages / Scroll to Bottom Floating Action Pill Button
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isUserNearBottom,
                    enter = fadeIn() + scaleIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + scaleOut() + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp)
                ) {
                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(displayedMessages.size)
                            }
                            unreadNewMessagesCount = 0
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF2563EB),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Scroll to bottom",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            if (unreadNewMessagesCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$unreadNewMessagesCount New Message${if (unreadNewMessagesCount > 1) "s" else ""}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Modern Horizontally Scrollable 6-Option Glass Attachment Menu
            AnimatedVisibility(
                visible = isAttachmentMenuOpen,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = if (isNightMode) Color(0xFF14141E).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.90f),
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, if (isNightMode) Color.White.copy(0.18f) else Color.Black.copy(0.08f))
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Camera
                        item {
                            ModernAttachmentChip(
                                label = "Camera",
                                icon = Icons.Default.CameraAlt,
                                gradientColors = listOf(Color(0xFFEC4899), Color(0xFFF43F5E)),
                                textColor = animTextColor,
                                onClick = {
                                    isAttachmentMenuOpen = false
                                    try {
                                        val photoFile = java.io.File(localContext.cacheDir, "camera_hd_${System.currentTimeMillis()}.jpg")
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            localContext,
                                            "${localContext.packageName}.fileprovider",
                                            photoFile
                                        )
                                        tempPhotoUri = uri
                                        cameraLauncher.launch(uri)
                                    } catch (e: Exception) {
                                        viewModel.sendMessage(chatId, "📸 [Camera Photo]")
                                    }
                                }
                            )
                        }

                        // 2. Gallery
                        item {
                            ModernAttachmentChip(
                                label = "Gallery",
                                icon = Icons.Default.PhotoLibrary,
                                gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)),
                                textColor = animTextColor,
                                onClick = {
                                    isAttachmentMenuOpen = false
                                    try {
                                        multipleGalleryLauncher.launch("image/*")
                                    } catch (e: Exception) {
                                        viewModel.sendMessage(chatId, "🖼️ [Gallery Image]")
                                    }
                                }
                            )
                        }

                        // 3. Documents
                        item {
                            ModernAttachmentChip(
                                label = "Documents",
                                icon = Icons.Default.AttachFile,
                                gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)),
                                textColor = animTextColor,
                                onClick = {
                                    isAttachmentMenuOpen = false
                                    try {
                                        documentLauncher.launch("*/*")
                                    } catch (e: Exception) {
                                        viewModel.sendMessage(chatId, "📄 [Document File]")
                                    }
                                }
                            )
                        }

                        // 4. Checklist
                        item {
                            ModernAttachmentChip(
                                label = "Checklist",
                                icon = Icons.Default.CheckCircle,
                                gradientColors = listOf(Color(0xFF06B6D4), Color(0xFF0D9488)),
                                textColor = animTextColor,
                                onClick = {
                                    isAttachmentMenuOpen = false
                                    showChecklistDialog = true
                                }
                            )
                        }

                        // 5. Polls
                        item {
                            ModernAttachmentChip(
                                label = "Polls",
                                icon = Icons.Default.BarChart,
                                gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                                textColor = animTextColor,
                                onClick = {
                                    isAttachmentMenuOpen = false
                                    showPollDialog = true
                                }
                            )
                        }

                        // 6. Contacts
                        item {
                            ModernAttachmentChip(
                                label = "Contacts",
                                icon = Icons.Default.PersonAdd,
                                gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669)),
                                textColor = animTextColor,
                                onClick = {
                                    isAttachmentMenuOpen = false
                                    showContactDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // Pending Selected Photos Preview Panel
            AnimatedVisibility(
                visible = pendingSelectedImages.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isNightMode) Color(0xFF1E202B) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isNightMode) Color.White.copy(0.12f) else Color(0xFFE2E8F0)),
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Selected ${pendingSelectedImages.size} Photo${if (pendingSelectedImages.size > 1) "s" else ""}",
                                    color = animTextColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { pendingSelectedImages.clear() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Clear", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = {
                                        if (!isUploadingPendingImages && pendingSelectedImages.isNotEmpty()) {
                                            isUploadingPendingImages = true
                                            val imagesToUpload = pendingSelectedImages.toList()
                                            scope.launch {
                                                try {
                                                    Toast.makeText(localContext, "Uploading ${imagesToUpload.size} photo(s)...", Toast.LENGTH_SHORT).show()
                                                    val uploadedUrls = mutableListOf<String>()
                                                    for (imgUri in imagesToUpload) {
                                                        val mime = localContext.contentResolver.getType(imgUri) ?: "image/jpeg"
                                                        val url = viewModel.uploadMedia(chatId, imgUri, mime, localContext) {}
                                                        uploadedUrls.add(url)
                                                    }
                                                    val captionText = inputText.trim()
                                                    if (captionText.isNotBlank()) {
                                                        inputText = ""
                                                    }
                                                    if (uploadedUrls.size == 1) {
                                                        val fullMsg = if (captionText.isNotBlank()) {
                                                            "[IMAGE_ATTACHMENT|${uploadedUrls[0]}] $captionText"
                                                        } else {
                                                            "[IMAGE_ATTACHMENT|${uploadedUrls[0]}] 🖼️ Photo"
                                                        }
                                                        viewModel.sendMessage(chatId, fullMsg)
                                                    } else {
                                                        val albumJoined = uploadedUrls.joinToString(",")
                                                        val fullMsg = if (captionText.isNotBlank()) {
                                                            "[IMAGE_ALBUM|$albumJoined] $captionText"
                                                        } else {
                                                            "[IMAGE_ALBUM|$albumJoined] 🖼️ ${uploadedUrls.size} Photos"
                                                        }
                                                        viewModel.sendMessage(chatId, fullMsg)
                                                    }
                                                    pendingSelectedImages.clear()
                                                    Toast.makeText(localContext, "Photos sent!", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(localContext, "Upload failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                } finally {
                                                    isUploadingPendingImages = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isUploadingPendingImages,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    if (isUploadingPendingImages) {
                                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                    } else {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Send", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(pendingSelectedImages) { index, uri ->
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Selected Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .clickable { pendingSelectedImages.removeAt(index) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            item {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isNightMode) Color.White.copy(0.06f) else Color(0xFFE2E8F0))
                                        .clickable {
                                            multipleGalleryLauncher.launch("image/*")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Add More",
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Add",
                                            color = Color(0xFF2563EB),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Active Reply Preview Bar
            AnimatedVisibility(
                visible = activeReplyMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                activeReplyMessage?.let { replyMsg ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isNightMode) Color(0xFF1E202B) else Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, if (isNightMode) Color.White.copy(alpha = 0.1f) else Color(0xFFBFDBFE)),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF2563EB))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Reply,
                                        contentDescription = "Replying",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Replying to ${replyMsg.senderName.ifBlank { "User" }}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when {
                                        replyMsg.text.contains("[DOCUMENT_FILE|") -> "📄 Document"
                                        replyMsg.text.contains("[IMAGE_BASE64|") || replyMsg.text.contains("[IMAGE_URL|") -> "🖼️ Photo"
                                        replyMsg.text.contains("[AUDIO_BASE64|") || replyMsg.text.contains("[AUDIO_FILE|") -> "🎙️ Voice message"
                                        replyMsg.text.contains("[VIDEO_BASE64|") || replyMsg.text.contains("[VIDEO_FILE|") -> "📹 Video message"
                                        replyMsg.text.startsWith("[POLL_JSON:") -> "📊 Live Poll"
                                        else -> replyMsg.text
                                    },
                                    color = animTextColor,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { activeReplyMessage = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Reply",
                                    tint = animSubTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Active Edit Preview Bar
            AnimatedVisibility(
                visible = editingMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                editingMessage?.let { editMsg ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isNightMode) Color(0xFF261D12) else Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, if (isNightMode) Color(0xFFD97706).copy(alpha = 0.3f) else Color(0xFFFDE68A)),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFD97706))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editing",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Editing message",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = editMsg.text,
                                    color = animTextColor,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    editingMessage = null
                                    inputText = ""
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Edit",
                                    tint = animSubTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Modern Animated Typing Indicator above the input box
            ChatInputTypingBanner(
                visible = effectiveTyping,
                partnerName = displayHeaderName,
                isNightMode = isNightMode
            )

            // Modern Chat Composer Bar
            if (isRestrictedFromSending) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = if (isNightMode) Color(0xFF1E202B) else Color(0xFFF1F5F9)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = animSubTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Only admins can send messages in this group",
                            fontSize = 13.sp,
                            color = animSubTextColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Modern Attached Icon Button / Dynamic Trash Delete Button when Recording
                    if (isRecording) {
                        val shakeTransition = rememberInfiniteTransition(label = "shake")
                        val shakeRotation by shakeTransition.animateFloat(
                            initialValue = -12f,
                            targetValue = 12f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(80, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "shakeRotation"
                        )
                        val rotation = if (recordDragOffsetX < -120f) shakeRotation else 0f

                        val deleteScale by animateFloatAsState(
                            targetValue = if (recordDragOffsetX < -80f) (1f + (kotlin.math.abs(recordDragOffsetX) / 220f)).coerceIn(1f, 1.7f) else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "deleteScale"
                        )
                        IconButton(
                            onClick = {
                                com.example.util.AudioRecorderManager.cancelRecording()
                                isRecording = false
                                isRecordingLocked = false
                                recordSeconds = 0
                                recordDragOffsetX = 0f
                                Toast.makeText(localContext, "Recording cancelled 🗑️", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size((38.dp * deleteScale).coerceAtMost(52.dp))
                                    .clip(CircleShape)
                                    .background(
                                        if (recordDragOffsetX < -220f)
                                            Color(0xFFEF4444)
                                        else
                                            Color(0xFFEF4444).copy(alpha = 0.18f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Cancel Recording",
                                    tint = if (recordDragOffsetX < -220f) Color.White else Color(0xFFEF4444),
                                    modifier = Modifier
                                        .size((22.dp * deleteScale).coerceAtMost(30.dp))
                                        .graphicsLayer { rotationZ = rotation }
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { isAttachmentMenuOpen = !isAttachmentMenuOpen },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Attach Options",
                                    tint = if (isAttachmentMenuOpen) Color(0xFF2563EB) else animSubTextColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Redesigned Spacious Typing / Recording Status Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .then(
                                if (isRecording) {
                                    Modifier
                                        .padding(bottom = 3.dp)
                                        .shadow(
                                            elevation = 14.dp,
                                            shape = RoundedCornerShape(18.dp),
                                            clip = false,
                                            ambientColor = Color(0xFF2563EB).copy(alpha = 0.5f),
                                            spotColor = Color(0xFF2563EB)
                                        )
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        GlassPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .then(
                                    if (isRecording) {
                                        Modifier.border(
                                            width = 1.5.dp,
                                            brush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6))),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            cornerRadius = 18.dp,
                            isNightMode = isNightMode
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isRecording) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        val mins = recordSeconds / 60
                                        val secs = recordSeconds % 60
                                        Text(
                                            text = String.format("%02d:%02d", mins, secs),
                                            color = Color(0xFFEF4444),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            for (index in 0 until 18) {
                                                val phase = kotlin.math.abs(kotlin.math.sin(index * 0.72))
                                                val speaking = recordingAmplitude > 0.035f
                                                val h = if (speaking) {
                                                    (5f + (recordingAmplitude * 24f * (0.35f + phase))).coerceIn(5f, 28f)
                                                } else 5f
                                                Box(
                                                    modifier = Modifier
                                                        .width(3.dp)
                                                        .height(h.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (recordDragOffsetX < -120f) Color(0xFFEF4444)
                                                            else Color(0xFF2563EB).copy(alpha = 0.9f)
                                                        )
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (recordDragOffsetX < -120f) "Release to delete" else "Slide ←",
                                            color = if (recordDragOffsetX < -120f) Color(0xFFEF4444) else animSubTextColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            keyboardController?.hide()
                                            showEmojiPicker = !showEmojiPicker
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showEmojiPicker) Icons.Filled.EmojiEmotions else Icons.Outlined.EmojiEmotions,
                                            contentDescription = "Emoji Picker",
                                            tint = if (showEmojiPicker) Color(0xFF2563EB) else animSubTextColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (inputText.isEmpty()) {
                                            Text(
                                                text = "Message...",
                                                color = animSubTextColor.copy(alpha = 0.7f),
                                                fontSize = 15.sp
                                            )
                                        }
                                        BasicTextField(
                                            value = inputText,
                                            onValueChange = { text ->
                                                inputText = text
                                                if (editingMessage != null) {
                                                    viewModel.setUserTyping(chatId, false)
                                                } else if (text.isNotEmpty()) {
                                                    viewModel.setUserTyping(chatId, true)
                                                } else {
                                                    viewModel.setUserTyping(chatId, false)
                                                }
                                            },
                                            textStyle = TextStyle(color = animTextColor, fontSize = 15.sp),
                                            cursorBrush = SolidColor(Color(0xFF2563EB)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onFocusChanged { focusState ->
                                                    if (focusState.isFocused) {
                                                        showEmojiPicker = false
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Controls on the Right: Send / View-Once / Hold-Mic Record
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.zIndex(100f)
                    ) {
                        // Hands-Free Recording Controls when recording
                        if (isRecording && isRecordingLocked) {
                            // Cancel / Trash Recording Button
                            IconButton(
                                onClick = {
                                    try { view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS) } catch (_: Exception) {}
                                    com.example.util.AudioRecorderManager.cancelRecording()
                                    isRecording = false
                                    isRecordingLocked = false
                                    isViewOnce = false
                                    recordSeconds = 0
                                    recordDragOffsetX = 0f
                                    Toast.makeText(localContext, "Recording cancelled 🗑️", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Cancel Recording",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // "1" View-Once Toggle Button
                            IconButton(
                                onClick = {
                                    isViewOnce = !isViewOnce
                                    try { view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
                                    Toast.makeText(
                                        localContext,
                                        if (isViewOnce) "View Once Enabled 1️⃣" else "View Once Disabled",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isViewOnce)
                                                Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF3B82F6)))
                                            else
                                                SolidColor(if (isNightMode) Color(0xFF27272A) else Color(0xFFE2E8F0))
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isViewOnce) Color.White else Color(0xFF94A3B8).copy(alpha = 0.5f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "1",
                                        color = if (isViewOnce) Color.White else (if (isNightMode) Color.White else Color(0xFF0F172A)),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                            }

                            // Send Voice Note Button
                            IconButton(
                                onClick = {
                                    try { view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
                                    val (recFile, secs) = com.example.util.AudioRecorderManager.stopRecording()
                                    if (recFile != null && recFile.exists() && secs >= 1) {
                                        val durStr = String.format("%02d:%02d", secs / 60, secs % 60)
                                        val base64Data = try {
                                            val bytes = recFile.readBytes()
                                            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                        } catch (_: Exception) { null }

                                        val textToSend = if (isViewOnce) {
                                            if (!base64Data.isNullOrBlank()) {
                                                "[VIEW_ONCE_AUDIO_BASE64|$base64Data|$durStr] 🎙️ 1-Time Voice Note ($durStr)"
                                            } else {
                                                "[VIEW_ONCE_AUDIO_FILE|${recFile.absolutePath}|$durStr] 🎙️ 1-Time Voice Note ($durStr)"
                                            }
                                        } else {
                                            if (!base64Data.isNullOrBlank()) {
                                                "[AUDIO_BASE64|$base64Data|$durStr] 🎙️ Voice Note ($durStr)"
                                            } else {
                                                "[AUDIO_FILE|${recFile.absolutePath}|$durStr] 🎙️ Voice Note ($durStr)"
                                            }
                                        }
                                        viewModel.sendMessage(chatId, textToSend)
                                        Toast.makeText(localContext, if (isViewOnce) "1-Time Voice note sent 1️⃣" else "Voice note sent 🎙️", Toast.LENGTH_SHORT).show()
                                    } else {
                                        com.example.util.AudioRecorderManager.cancelRecording()
                                        Toast.makeText(localContext, "Voice note must be at least 1 second ⏱️", Toast.LENGTH_SHORT).show()
                                    }
                                    isRecording = false
                                    isRecordingLocked = false
                                    isViewOnce = false
                                    recordSeconds = 0
                                    recordDragOffsetX = 0f
                                    lastRecordingFinishedTime = System.currentTimeMillis()
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send Voice Note",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else if (inputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        val textToSend = inputText.trim()
                                        if (editingMessage != null) {
                                            viewModel.editMessage(chatId, editingMessage!!.id, editingMessage!!.serverMessageId, textToSend)
                                            editingMessage = null
                                            inputText = ""
                                            Toast.makeText(localContext, "Message edited ✏️", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val replyTarget = activeReplyMessage
                                            inputText = ""
                                            activeReplyMessage = null
                                            viewModel.setUserTyping(chatId, false)
                                            viewModel.sendMessage(chatId, textToSend, replyToMessage = replyTarget)
                                        }
                                    }
                                },
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        } else {
                            // Responsive Voice Record Button with Hold & Drag-to-Delete
                            var isButtonHolding by remember { mutableStateOf(false) }
                            val buttonScale by animateFloatAsState(
                                targetValue = if (isButtonHolding || isRecording) 1.25f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "buttonScale"
                            )

                            val holdTransition = rememberInfiniteTransition(label = "holdPulse")
                            val holdPulseRadius by holdTransition.animateFloat(
                                initialValue = 0.9f,
                                targetValue = 1.45f,
                                animationSpec = infiniteRepeatable(tween(750, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                                label = "holdPulseRadius"
                            )
                            val holdPulseAlpha by holdTransition.animateFloat(
                                initialValue = 0.65f,
                                targetValue = 0f,
                                animationSpec = infiniteRepeatable(tween(750, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                                label = "holdPulseAlpha"
                            )

                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                if (System.currentTimeMillis() - lastRecordingFinishedTime < 1500L) {
                                                    try {
                                                         while (true) {
                                                             val ev = awaitPointerEvent()
                                                             if (ev.changes.all { !it.pressed }) break
                                                         }
                                                     } catch (_: Exception) {}
                                                    continue
                                                }
                                                val startTime = System.currentTimeMillis()
                                                var isHeldRecording = false
                                                var isCancelled = false

                                                // Check permission lazily
                                                val hasMicPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                                    localContext, android.Manifest.permission.RECORD_AUDIO
                                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                                if (!hasMicPermission) {
                                                    audioRecordPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                                    break
                                                }

                                                isButtonHolding = true
                                                try { view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}

                                                val holdJob = coroutineScope.launch {
                                                    delay(90L)
                                                    isHeldRecording = true
                                                    isRecording = true
                                                    isRecordingLocked = false
                                                    recordSeconds = 0
                                                    recordDragOffsetX = 0f
                                                    try { view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS) } catch (_: Exception) {}
                                                    com.example.util.AudioRecorderManager.startRecording(localContext)
                                                }

                                                var hasVibratedForDeleteZone = false

                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                    if (!change.pressed) {
                                                        break
                                                    }
                                                    val dragAmount = change.position - down.position

                                                    if (isHeldRecording) {
                                                        recordDragOffsetX = dragAmount.x
                                                        
                                                        // Tactile vibration when entering the shake/delete zone (-120f)
                                                        if (dragAmount.x < -120f) {
                                                            if (!isCancelled && !hasVibratedForDeleteZone) {
                                                                hasVibratedForDeleteZone = true
                                                                // Play a soft haptic trigger for entering the zone
                                                                try {
                                                                    val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                                        (localContext.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager)?.defaultVibrator
                                                                    } else {
                                                                        @Suppress("DEPRECATION")
                                                                        localContext.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                                                    }
                                                                    vibrator?.vibrate(android.os.VibrationEffect.createOneShot(40, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                                                } catch (_: Exception) {}
                                                            }
                                                        } else {
                                                            hasVibratedForDeleteZone = false
                                                        }

                                                        // Slide UP past threshold (-120f) to lock recording hands-free
                                                        if (dragAmount.y < -120f && !isRecordingLocked) {
                                                            isRecordingLocked = true
                                                            try {
                                                                val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                                    (localContext.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager)?.defaultVibrator
                                                                } else {
                                                                    @Suppress("DEPRECATION")
                                                                    localContext.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                                                }
                                                                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(60, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                                            } catch (_: Exception) {}
                                                            Toast.makeText(localContext, "Recording locked hands-free 🔒", Toast.LENGTH_SHORT).show()
                                                        }

                                                        // Drag left past threshold (-240f) to trigger instant cancellation
                                                        if (dragAmount.x < -240f && !isCancelled) {
                                                            isCancelled = true
                                                            try {
                                                                val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                                    (localContext.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager)?.defaultVibrator
                                                                } else {
                                                                    @Suppress("DEPRECATION")
                                                                    localContext.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                                                }
                                                                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(80, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                                            } catch (_: Exception) {}
                                                            com.example.util.AudioRecorderManager.cancelRecording()
                                                            isRecording = false
                                                            isButtonHolding = false
                                                            recordSeconds = 0
                                                            recordDragOffsetX = 0f
                                                            Toast.makeText(localContext, "Recording cancelled 🗑️", Toast.LENGTH_SHORT).show()
                                                            break
                                                        }
                                                    }
                                                }

                                                holdJob.cancel()
                                                isButtonHolding = false
                                                val holdDuration = System.currentTimeMillis() - startTime

                                                if (isCancelled) {
                                                    isRecording = false
                                                    isRecordingLocked = false
                                                    recordSeconds = 0
                                                    recordDragOffsetX = 0f
                                                    lastRecordingFinishedTime = System.currentTimeMillis()
                                                } else if (isHeldRecording) {
                                                    if (isRecordingLocked) {
                                                        // Let them record hands free
                                                    } else if (recordDragOffsetX < -120f) {
                                                        com.example.util.AudioRecorderManager.cancelRecording()
                                                        try {
                                                            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                                (localContext.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager)?.defaultVibrator
                                                            } else {
                                                                @Suppress("DEPRECATION")
                                                                localContext.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                                            }
                                                            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(80, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                                        } catch (_: Exception) {}
                                                        Toast.makeText(localContext, "Recording cancelled 🗑️", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        val (recFile, secs) = com.example.util.AudioRecorderManager.stopRecording()
                                                        if (recFile != null && recFile.exists() && secs >= 1) {
                                                            val durStr = String.format("%02d:%02d", secs / 60, secs % 60)
                                                            val base64Data = try {
                                                                val bytes = recFile.readBytes()
                                                                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                                            } catch (_: Exception) { null }

                                                            val textToSend = if (isViewOnce) {
                                                                if (!base64Data.isNullOrBlank()) {
                                                                    "[VIEW_ONCE_AUDIO_BASE64|$base64Data|$durStr] 🎙️ 1-Time Voice Note ($durStr)"
                                                                } else {
                                                                    "[VIEW_ONCE_AUDIO_FILE|${recFile.absolutePath}|$durStr] 🎙️ 1-Time Voice Note ($durStr)"
                                                                }
                                                            } else {
                                                                if (!base64Data.isNullOrBlank()) {
                                                                    "[AUDIO_BASE64|$base64Data|$durStr] 🎙️ Voice Note ($durStr)"
                                                                } else {
                                                                    "[AUDIO_FILE|${recFile.absolutePath}|$durStr] 🎙️ Voice Note ($durStr)"
                                                                }
                                                            }
                                                            viewModel.sendMessage(chatId, textToSend)
                                                            Toast.makeText(localContext, if (isViewOnce) "1-Time Voice note sent 1️⃣" else "Voice note sent 🎙️", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            com.example.util.AudioRecorderManager.cancelRecording()
                                                            if (secs > 0 || holdDuration > 800L) {
                                                                Toast.makeText(localContext, "Hold for at least 1 second ⏱️", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                    isRecording = false
                                                    isRecordingLocked = false
                                                    isViewOnce = false
                                                    recordSeconds = 0
                                                    recordDragOffsetX = 0f
                                                    lastRecordingFinishedTime = System.currentTimeMillis()
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                 // Animated Expanding Ripple Waves on Mic Button
                                if (isButtonHolding || isRecording) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp * holdPulseRadius)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2563EB).copy(alpha = holdPulseAlpha))
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .graphicsLayer {
                                            scaleX = buttonScale
                                            scaleY = buttonScale
                                        }
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Mic,
                                        contentDescription = "Voice Note",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Inline Section-Wise Emoji Picker (visible directly below typing box, stays open until toggled or keyboard focused)
                AnimatedVisibility(
                    visible = showEmojiPicker,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ModernEmojiPickerInline(
                        isNightMode = isNightMode,
                        onEmojiSelect = { emoji ->
                            inputText += emoji
                        },
                        onSendMedia = { mediaMsg ->
                            viewModel.sendMessage(chatId, mediaMsg)
                            showEmojiPicker = false
                        }
                    )
                }
        }
    }

        // Animated Slide-Over Side Options Drawer (Transferred 3-Dot Features)
        AnimatedVisibility(
            visible = isSideDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.zIndex(100f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { isSideDrawerOpen = false }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.82f)
                        .clickable(enabled = false) {},
                    color = if (isNightMode) Color(0xFF14151F) else Color.White,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .statusBarsPadding()
                    ) {
                        // Header with Animated Pulsing 3-Dots
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val infiniteTrans = rememberInfiniteTransition(label = "dots_drawer_anim")
                                val d1Alpha by infiniteTrans.animateFloat(
                                    initialValue = 0.25f, targetValue = 1f,
                                    animationSpec = infiniteRepeatable(tween(450), repeatMode = RepeatMode.Reverse),
                                    label = "d1"
                                )
                                val d2Alpha by infiniteTrans.animateFloat(
                                    initialValue = 0.25f, targetValue = 1f,
                                    animationSpec = infiniteRepeatable(tween(450, delayMillis = 150), repeatMode = RepeatMode.Reverse),
                                    label = "d2"
                                )
                                val d3Alpha by infiniteTrans.animateFloat(
                                    initialValue = 0.25f, targetValue = 1f,
                                    animationSpec = infiniteRepeatable(tween(450, delayMillis = 300), repeatMode = RepeatMode.Reverse),
                                    label = "d3"
                                )

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF2563EB).copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF2563EB).copy(alpha = d1Alpha)))
                                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF8B5CF6).copy(alpha = d2Alpha)))
                                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFEC4899).copy(alpha = d3Alpha)))
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Chat Options",
                                        color = animTextColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "3-Dot Features & Settings",
                                        color = animSubTextColor,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            IconButton(onClick = { isSideDrawerOpen = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Drawer",
                                    tint = animTextColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Contact Header Card inside Drawer
                        Surface(
                            onClick = {
                                isSideDrawerOpen = false
                                showChatProfileModal = true
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isNightMode) Color(0xFF1E202E) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isNightMode) Color.White.copy(0.08f) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                if (isGroupChat) listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                                                else listOf(Color(0xFF2563EB), Color(0xFF00C6FF))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chatName.take(1),
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayHeaderName,
                                        color = animTextColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isGroupChat) "Group • $activeMemberCount members" else if (isUserOnline) "Online • E2E Encrypted" else "Offline • Encrypted",
                                        color = if (isUserOnline || isGroupChat) Color(0xFF10B981) else animSubTextColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "View Profile",
                                    tint = animSubTextColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "ACTION MENU",
                            color = animSubTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                        )

                        // List of Transferred 3-Dot Options
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DrawerMenuItem(
                                icon = if (isGroupChat) Icons.Default.Group else Icons.Default.Person,
                                iconColor = Color(0xFF2563EB),
                                title = if (isGroupChat) "Group Info & Members" else "View User Profile",
                                subtitle = "Shared media, group members & bio",
                                animTextColor = animTextColor,
                                animSubTextColor = animSubTextColor,
                                isNightMode = isNightMode,
                                onClick = {
                                    isSideDrawerOpen = false
                                    showChatProfileModal = true
                                }
                            )

                            DrawerMenuItem(
                                icon = if (isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                iconColor = if (isMuted) Color(0xFF10B981) else Color(0xFFF59E0B),
                                title = if (isMuted) "Unmute Notifications" else "Mute Notifications",
                                subtitle = if (isMuted) "Enable sounds for this chat" else "Silence alerts for this chat",
                                animTextColor = animTextColor,
                                animSubTextColor = animSubTextColor,
                                isNightMode = isNightMode,
                                onClick = {
                                    isSideDrawerOpen = false
                                    viewModel.toggleMuteChat(chatId)
                                    Toast.makeText(localContext, if (isMuted) "Notifications unmuted" else "Notifications muted", Toast.LENGTH_SHORT).show()
                                }
                            )

                            DrawerMenuItem(
                                icon = Icons.Default.Image,
                                iconColor = Color(0xFF8B5CF6),
                                title = "Media, Links & Docs",
                                subtitle = "View all photos & attachments",
                                animTextColor = animTextColor,
                                animSubTextColor = animSubTextColor,
                                isNightMode = isNightMode,
                                onClick = {
                                    isSideDrawerOpen = false
                                    showChatProfileModal = true
                                }
                            )

                            DrawerMenuItem(
                                icon = Icons.Default.Search,
                                iconColor = Color(0xFF0284C7),
                                title = "Search in Chat",
                                subtitle = "Locate messages & history",
                                animTextColor = animTextColor,
                                animSubTextColor = animSubTextColor,
                                isNightMode = isNightMode,
                                onClick = {
                                    isSideDrawerOpen = false
                                    Toast.makeText(localContext, "Use search bar in chat list or top search", Toast.LENGTH_SHORT).show()
                                }
                            )

                            DrawerMenuItem(
                                icon = Icons.Default.Lock,
                                iconColor = Color(0xFF10B981),
                                title = "Lock Chat with PIN",
                                subtitle = "Keep conversation private",
                                animTextColor = animTextColor,
                                animSubTextColor = animSubTextColor,
                                isNightMode = isNightMode,
                                onClick = {
                                    isSideDrawerOpen = false
                                    Toast.makeText(localContext, "Chat lock toggled", Toast.LENGTH_SHORT).show()
                                }
                            )

                            DrawerMenuItem(
                                icon = Icons.Default.Delete,
                                iconColor = Color(0xFFEF4444),
                                title = "Clear Chat History",
                                subtitle = "Delete all messages in this chat",
                                animTextColor = animTextColor,
                                animSubTextColor = animSubTextColor,
                                isNightMode = isNightMode,
                                onClick = {
                                    isSideDrawerOpen = false
                                    viewModel.clearMessagesForChat(chatId)
                                    Toast.makeText(localContext, "Cleared chat history", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Interactive Audio & Video Calling Overlay Dialog
        if (isCallOverlayOpen) {
            InteractiveCallDialog(
                chatName = chatName,
                isVideo = isVideoCallMode,
                isUserOnline = isUserOnline,
                isNightMode = isNightMode,
                onDismiss = { isCallOverlayOpen = false },
                onLaunchFullVideo = {
                    isCallOverlayOpen = false
                    onVideoCallClick(chatId, chatName)
                },
                onSendVoiceNote = {
                    isCallOverlayOpen = false
                    viewModel.sendMessage(chatId, "🎙️ [Voice Message: Left while you were away]")
                }
            )
        }

        // Group / User Profile Details Full Page View
        if (showChatProfileModal) {
            ChatProfileDetailsPage(
                chatId = chatId,
                chatName = chatName,
                viewModel = viewModel,
                isNightMode = isNightMode,
                onDismiss = { showChatProfileModal = false },
                onBackClick = onBackClick
            )
        }

        if (showPollDialog) {
            com.example.ui.components.PollCreatorDialog(
                isNightMode = isNightMode,
                onDismiss = { showPollDialog = false },
                onCreatePoll = { pollSerializedString ->
                    showPollDialog = false
                    viewModel.sendMessage(chatId, pollSerializedString)
                    Toast.makeText(localContext, "Poll created & shared!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showContactDialog) {
            com.example.ui.components.ContactPickerFullPageSheet(
                isNightMode = isNightMode,
                onDismiss = { showContactDialog = false },
                onSelectContact = { name, phone ->
                    showContactDialog = false
                    viewModel.sendMessage(chatId, "👤 Contact: $name ($phone)")
                    Toast.makeText(localContext, "Shared contact: $name", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showChecklistDialog) {
            com.example.ui.components.ChecklistCreatorDialog(
                isNightMode = isNightMode,
                onDismiss = { showChecklistDialog = false },
                onCreateChecklist = { checklistPayload ->
                    showChecklistDialog = false
                    viewModel.sendMessage(chatId, checklistPayload)
                    Toast.makeText(localContext, "Shared Checklist! 📋", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 1. Telegram-Style Compact Message Context Menu Dialog
        if (selectedMessageForAction != null) {
            val msg = selectedMessageForAction!!
            Dialog(
                onDismissRequest = { selectedMessageForAction = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { selectedMessageForAction = null },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 270.dp)
                            .padding(16.dp)
                            .clickable(enabled = false) { },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Message Preview Snippet
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isNightMode) Color(0xFF181A24).copy(alpha = 0.85f) else Color(0xFFEFF6FF).copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, if (isNightMode) Color.White.copy(0.08f) else Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (msg.text.length > 70) msg.text.take(70) + "..." else msg.text.ifBlank { "Attachment" },
                                    fontSize = 12.sp,
                                    color = animTextColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Telegram Style Menu Card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isNightMode) Color(0xFF1E202B).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, if (isNightMode) Color.White.copy(0.12f) else Color(0xFFE2E8F0)),
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                // Reply
                                TelegramMenuItem(
                                    icon = Icons.AutoMirrored.Filled.Reply,
                                    tint = Color(0xFF2563EB),
                                    label = "Reply",
                                    isNightMode = isNightMode,
                                    onClick = {
                                        activeReplyMessage = msg
                                        selectedMessageForAction = null
                                    }
                                )

                                // Forward
                                TelegramMenuItem(
                                    icon = Icons.AutoMirrored.Filled.Forward,
                                    tint = Color(0xFF0284C7),
                                    label = "Forward",
                                    isNightMode = isNightMode,
                                    onClick = {
                                        messageToForward = msg
                                        selectedForwardChatIds.clear()
                                        showForwardSheet = true
                                        selectedMessageForAction = null
                                    }
                                )

                                // Pin
                                TelegramMenuItem(
                                    icon = Icons.Default.PushPin,
                                    tint = Color(0xFFF59E0B),
                                    label = if (msg.isPinned) "Unpin Message" else "Pin Message",
                                    isNightMode = isNightMode,
                                    onClick = {
                                        selectedMessageForAction = null
                                        messageToPin = msg
                                        showPinConfirmDialog = true
                                    }
                                )

                                // Translate
                                if (msg.text.isNotBlank()) {
                                    TelegramMenuItem(
                                        icon = Icons.Default.Translate,
                                        tint = Color(0xFF8B5CF6),
                                        label = "Translate",
                                        isNightMode = isNightMode,
                                        onClick = {
                                            selectedMessageForAction = null
                                            messageToTranslate = msg
                                            showTranslateDialog = true
                                        }
                                    )

                                    // Copy
                                    TelegramMenuItem(
                                        icon = Icons.Default.ContentCopy,
                                        tint = Color(0xFF10B981),
                                        label = "Copy Text",
                                        isNightMode = isNightMode,
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(msg.text))
                                            selectedMessageForAction = null
                                            Toast.makeText(localContext, "Copied to clipboard 📋", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }

                                // Edit
                                if (msg.isFromUser && !msg.isDeletedForEveryone) {
                                    TelegramMenuItem(
                                        icon = Icons.Default.Edit,
                                        tint = Color(0xFFD97706),
                                        label = "Edit Message",
                                        isNightMode = isNightMode,
                                        onClick = {
                                            viewModel.setUserTyping(chatId, false)
                                            editingMessage = msg
                                            inputText = msg.text
                                            selectedMessageForAction = null
                                        }
                                    )
                                }

                                // Delete
                                TelegramMenuItem(
                                    icon = Icons.Default.Delete,
                                    tint = Color(0xFFEF4444),
                                    label = "Delete Message",
                                    isNightMode = isNightMode,
                                    onClick = {
                                        messageToDelete = msg
                                        showDeleteDialog = true
                                        selectedMessageForAction = null
                                    }
                                )
                            }
                        }

                        // Quick Emoji Reaction Bar (Placed at the bottom)
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (isNightMode) Color(0xFF1E202B).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, if (isNightMode) Color.White.copy(0.12f) else Color(0xFFE2E8F0)),
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("👍", "❤️", "🔥", "😂", "😮", "😢", "🎉", "👏").forEach { emoji ->
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                viewModel.toggleReaction(chatId, msg, emoji)
                                                selectedMessageForAction = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 17.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Pin Message Confirmation Dialog (Telegram Style)
        if (showPinConfirmDialog && messageToPin != null) {
            val msgToPin = messageToPin!!
            var alsoPinForOpponent by remember { mutableStateOf(true) }
            val isUnpinning = msgToPin.isPinned

            AlertDialog(
                onDismissRequest = {
                    showPinConfirmDialog = false
                    messageToPin = null
                },
                title = {
                    Text(
                        text = if (isUnpinning) "Unpin Message?" else "Pin Message?",
                        fontWeight = FontWeight.Bold,
                        color = animTextColor
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (isUnpinning) "Are you sure you want to unpin this message?" else "Would you like to pin this message in this chat?",
                            color = animSubTextColor,
                            fontSize = 14.sp
                        )
                        if (!isUnpinning) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { alsoPinForOpponent = !alsoPinForOpponent }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = alsoPinForOpponent,
                                    onCheckedChange = { alsoPinForOpponent = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2563EB))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Also pin for $displayHeaderName",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = animTextColor
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.togglePinMessage(chatId, msgToPin, forEveryone = alsoPinForOpponent)
                            if (alsoPinForOpponent) {
                                val act = if (isUnpinning) "unpinned" else "pinned"
                                viewModel.sendSystemEvent(chatId, "MESSAGE_PINNED", "📌 A message was $act")
                            }
                            showPinConfirmDialog = false
                            messageToPin = null
                            Toast.makeText(localContext, if (isUnpinning) "Message unpinned" else "Message pinned for everyone", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isUnpinning) "Unpin" else "Pin", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPinConfirmDialog = false
                        messageToPin = null
                    }) {
                        Text("Cancel", color = animSubTextColor)
                    }
                },
                containerColor = if (isNightMode) Color(0xFF1E202B) else Color.White,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // 3. Delete Message Dialog (Telegram Style with Checkbox)
        if (showDeleteDialog && messageToDelete != null) {
            val msgToDelete = messageToDelete!!
            var deleteForOpponent by remember { mutableStateOf(msgToDelete.isFromUser || isGroupChat) }

            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    messageToDelete = null
                },
                title = {
                    Text(text = "Delete Message?", fontWeight = FontWeight.Bold, color = animTextColor)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Are you sure you want to delete this message?",
                            color = animSubTextColor,
                            fontSize = 14.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deleteForOpponent = !deleteForOpponent }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = deleteForOpponent,
                                onCheckedChange = { deleteForOpponent = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Also delete for $displayHeaderName",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = animTextColor
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteMessage(chatId, msgToDelete, deleteForEveryone = deleteForOpponent)
                            showDeleteDialog = false
                            messageToDelete = null
                            Toast.makeText(localContext, if (deleteForOpponent) "Deleted for everyone" else "Deleted for you", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        messageToDelete = null
                    }) {
                        Text("Cancel", color = animSubTextColor)
                    }
                },
                containerColor = if (isNightMode) Color(0xFF1E202B) else Color.White,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // 4. Gemini Translation Dialog (Telegram Style)
        if (showTranslateDialog && messageToTranslate != null) {
            TelegramTranslateDialog(
                message = messageToTranslate!!,
                isNightMode = isNightMode,
                onDismiss = {
                    showTranslateDialog = false
                    messageToTranslate = null
                }
            )
        }

        // 3. Full-Page Forward Message Screen
        if (showForwardSheet && messageToForward != null) {
            val forwardMsg = messageToForward!!
            var forwardSearchQuery by remember { mutableStateOf("") }
            val filteredForwardChats = remember(allChats, forwardSearchQuery) {
                if (forwardSearchQuery.isBlank()) allChats
                else allChats.filter {
                    it.name.contains(forwardSearchQuery, ignoreCase = true) ||
                            it.category.contains(forwardSearchQuery, ignoreCase = true)
                }
            }

            Dialog(
                onDismissRequest = {
                    showForwardSheet = false
                    messageToForward = null
                },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isNightMode) Color(0xFF0F1015) else Color(0xFFF8FAFC)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        // Top App Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                showForwardSheet = false
                                messageToForward = null
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = animTextColor
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Forward To",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = animTextColor
                                )
                                Text(
                                    text = if (selectedForwardChatIds.isNotEmpty()) "${selectedForwardChatIds.size} recipient(s) selected" else "Select one or more chats",
                                    fontSize = 12.sp,
                                    color = if (selectedForwardChatIds.isNotEmpty()) Color(0xFF2563EB) else animSubTextColor
                                )
                            }
                            if (selectedForwardChatIds.isNotEmpty()) {
                                TextButton(onClick = { selectedForwardChatIds.clear() }) {
                                    Text("Clear", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Message Preview Card
                        Surface(
                            color = if (isNightMode) Color(0xFF181A24) else Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isNightMode) Color.White.copy(alpha = 0.08f) else Color(0xFFBFDBFE)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Forward,
                                        contentDescription = "Forwarding",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Forwarding Message",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when {
                                            forwardMsg.text.contains("[DOCUMENT_FILE|") -> {
                                                val parts = forwardMsg.text.substringAfter("[DOCUMENT_FILE|").substringBefore("]").split("|")
                                                val docName = parts.getOrNull(0)?.trim()?.ifBlank { "Document" } ?: "Document"
                                                val docSize = parts.getOrNull(1)?.trim()?.ifBlank { "" } ?: ""
                                                if (docSize.isNotBlank()) "📄 $docName ($docSize)" else "📄 $docName"
                                            }
                                            forwardMsg.text.contains("[IMAGE_BASE64|") || forwardMsg.text.contains("[IMAGE_URL|") -> "🖼️ Photo attachment"
                                            forwardMsg.text.contains("[AUDIO_BASE64|") || forwardMsg.text.contains("[AUDIO_FILE|") -> "🎙️ Voice message"
                                            forwardMsg.text.contains("[VIDEO_BASE64|") || forwardMsg.text.contains("[VIDEO_FILE|") -> "📹 Video message"
                                            forwardMsg.text.startsWith("[POLL_JSON:") -> "📊 Live Poll"
                                            else -> forwardMsg.text
                                        },
                                        color = animTextColor,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Search Input
                        OutlinedTextField(
                            value = forwardSearchQuery,
                            onValueChange = { forwardSearchQuery = it },
                            placeholder = { Text("Search conversations...", color = animSubTextColor, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = animSubTextColor)
                            },
                            trailingIcon = {
                                if (forwardSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { forwardSearchQuery = "" }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = animSubTextColor)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = if (isNightMode) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1)
                            )
                        )

                        // Full Page Chats List
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredForwardChats, key = { it.id }) { targetChat ->
                                val isSelected = selectedForwardChatIds.contains(targetChat.id)
                                val hasCustomAvatar = targetChat.avatarType.isNotBlank() && targetChat.avatarType != "default" && targetChat.avatarType != "assistant"
                                val isAssistantChat = targetChat.avatarType.equals("assistant", ignoreCase = true) ||
                                    targetChat.name.equals("KnotLink Assistant", ignoreCase = true)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            if (isSelected) {
                                                selectedForwardChatIds.remove(targetChat.id)
                                            } else {
                                                selectedForwardChatIds.add(targetChat.id)
                                            }
                                        },
                                    color = if (isSelected) {
                                        if (isNightMode) Color(0xFF1E2640) else Color(0xFFEFF6FF)
                                    } else {
                                        if (isNightMode) Color(0xFF161822) else Color.White
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF2563EB) else (if (isNightMode) Color.White.copy(alpha = 0.05f) else Color(0xFFE2E8F0))
                                    ),
                                    shadowElevation = if (isNightMode) 0.dp else 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (hasCustomAvatar) {
                                            AsyncImage(
                                                model = if (targetChat.avatarType.startsWith("/")) File(targetChat.avatarType) else targetChat.avatarType,
                                                contentDescription = targetChat.name,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.linearGradient(
                                                            if (isAssistantChat)
                                                                listOf(Color(0xFF2563EB), Color(0xFF7C3AED))
                                                            else
                                                                listOf(Color(0xFF0EA5E9), Color(0xFF2563EB))
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isAssistantChat) {
                                                    Icon(
                                                        imageVector = Icons.Default.SmartToy,
                                                        contentDescription = "KnotLink Assistant",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                } else {
                                                    Text(
                                                        text = targetChat.name.take(1).uppercase(),
                                                        color = Color.White,
                                                        fontSize = 17.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = targetChat.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = animTextColor
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (targetChat.category.isNotBlank()) targetChat.category else "Conversation",
                                                fontSize = 12.sp,
                                                color = animSubTextColor
                                            )
                                        }
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                if (checked) selectedForwardChatIds.add(targetChat.id)
                                                else selectedForwardChatIds.remove(targetChat.id)
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF2563EB)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Action Forward Button
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isNightMode) Color(0xFF13141B) else Color.White,
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .navigationBarsPadding()
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.forwardMessage(forwardMsg, selectedForwardChatIds.toList())
                                        showForwardSheet = false
                                        messageToForward = null
                                        Toast.makeText(localContext, "Forwarded to ${selectedForwardChatIds.size} chat(s)", Toast.LENGTH_SHORT).show()
                                    },
                                    enabled = selectedForwardChatIds.isNotEmpty(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2563EB),
                                        disabledContainerColor = Color(0xFF2563EB).copy(alpha = 0.35f)
                                    )
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedForwardChatIds.isNotEmpty()) "Forward (${selectedForwardChatIds.size})" else "Select Chats to Forward",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showPinnedSheet) {
            val pinnedSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showPinnedSheet = false },
                sheetState = pinnedSheetState,
                containerColor = if (isNightMode) Color(0xFF1E202B) else Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pins",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pinned Messages (${pinnedMessages.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = animTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pinnedMessages, key = { it.messageId }) { pinnedItem ->
                            val msgEntity = validMessages.find { it.serverMessageId == pinnedItem.messageId || it.id.toString() == pinnedItem.messageId }
                            Surface(
                                onClick = {
                                    showPinnedSheet = false
                                    if (msgEntity != null) {
                                        val idx = displayedMessages.indexOfFirst { it.id == msgEntity.id }
                                        if (idx >= 0) {
                                            coroutineScope.launch { listState.animateScrollToItem(idx) }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isNightMode) Color(0xFF13141B) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isNightMode) Color.White.copy(alpha = 0.05f) else Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = msgEntity?.senderName ?: "Message",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF2563EB)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = msgEntity?.text ?: "Pinned message",
                                            fontSize = 13.sp,
                                            color = animTextColor,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            msgEntity?.let { viewModel.togglePinMessage(chatId, it) }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Unpin",
                                            tint = animSubTextColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (pinnedMessages.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.unpinAllMessages(chatId)
                                showPinnedSheet = false
                                Toast.makeText(localContext, "Unpinned all messages", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.PushPin, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Unpin All Messages", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // 5. Fullscreen Centered Voice Note Recording Overlay
        androidx.compose.animation.AnimatedVisibility(
            visible = isRecording,
            enter = androidx.compose.animation.fadeIn(tween(200)) + androidx.compose.animation.scaleIn(initialScale = 0.85f, animationSpec = spring()),
            exit = androidx.compose.animation.fadeOut(tween(200)) + androidx.compose.animation.scaleOut(targetScale = 0.85f, animationSpec = spring()),
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.70f)),
                contentAlignment = Alignment.Center
            ) {
                val waveTransition = rememberInfiniteTransition(label = "wave_ring_fullscreen")
                
                // Continuous rotating angle for gradient ring border
                val ringRotation by waveTransition.animateFloat(
                    initialValue = 0f, targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                    label = "ring_rotation"
                )

                // Animated Expanding Rings
                val ringPulse1 by waveTransition.animateFloat(
                    initialValue = 1f, targetValue = 1.6f,
                    animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                    label = "rp1"
                )
                val ringAlpha1 by waveTransition.animateFloat(
                    initialValue = 0.55f, targetValue = 0f,
                    animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                    label = "ra1"
                )
                val ringPulse2 by waveTransition.animateFloat(
                    initialValue = 1f, targetValue = 2.0f,
                    animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                    label = "rp2"
                )
                val ringAlpha2 by waveTransition.animateFloat(
                    initialValue = 0.42f, targetValue = 0f,
                    animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                    label = "ra2"
                )

                // Outer expanding pulsing waves
                Box(
                    modifier = Modifier
                        .size(195.dp * ringPulse2)
                        .clip(CircleShape)
                        .background(Color(0xFF8B5CF6).copy(alpha = ringAlpha2))
                )
                Box(
                    modifier = Modifier
                        .size(195.dp * ringPulse1)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB).copy(alpha = ringAlpha1))
                )

                // Side equalizer waveform bars (7 bars left, 7 bars right)
                Row(
                    modifier = Modifier.width(330.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Waveform Bars
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 0 until 7) {
                            val barH by waveTransition.animateFloat(
                                initialValue = 10f + i * 7f,
                                targetValue = 52f - i * 5f,
                                animationSpec = infiniteRepeatable(tween(200 + i * 40), repeatMode = RepeatMode.Reverse),
                                label = "fs_left_$i"
                            )
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .height(barH.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6))
                                        )
                                    )
                            )
                        }
                    }

                    // Right Waveform Bars
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 0 until 7) {
                            val barH by waveTransition.animateFloat(
                                initialValue = 52f - i * 6f,
                                targetValue = 10f + i * 6f,
                                animationSpec = infiniteRepeatable(tween(240 + i * 38), repeatMode = RepeatMode.Reverse),
                                label = "fs_right_$i"
                            )
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .height(barH.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6))
                                        )
                                    )
                            )
                        }
                    }
                }

                // Center Ring (Voice icon removed, Time at top, text below time)
                Surface(
                    modifier = Modifier
                        .size(195.dp)
                        .graphicsLayer { rotationZ = ringRotation }
                        .shadow(28.dp, CircleShape, spotColor = Color(0xFF2563EB)),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(
                        4.dp,
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFF2563EB),
                                Color(0xFF06B6D4),
                                Color(0xFFEC4899),
                                Color(0xFF8B5CF6),
                                Color(0xFF38BDF8),
                                Color(0xFF2563EB)
                            )
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = -ringRotation } // Keep inner text/timer upright
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // 1. Time Counter (Top)
                            val mins = recordSeconds / 60
                            val secs = recordSeconds % 60
                            Text(
                                text = String.format("%02d:%02d", mins, secs),
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 2. Recording Status text (Below Time) with pulsing REC dot
                            val recDotAlpha by waveTransition.animateFloat(
                                initialValue = 0.2f, targetValue = 1f,
                                animationSpec = infiniteRepeatable(tween(450), repeatMode = RepeatMode.Reverse),
                                label = "recDotAlpha"
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.20f))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.40f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444).copy(alpha = recDotAlpha))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Recording...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF87171)
                                )
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun ModernEmojiPickerInline(
    isNightMode: Boolean,
    onEmojiSelect: (String) -> Unit,
    onSendMedia: ((String) -> Unit)? = null
) {
    val emojiCategories = remember {
        listOf(
            "😀" to listOf("😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🥹", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🥸", "🤩", "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣", "😖", "😫", "😩", "🥺", "😢", "😭", "😮‍💨", "😤", "😠", "😡", "🤬", "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤗", "🤔", "🫣", "🤭", "🤫", "🤥", "😶", "😶‍🌫️", "😐", "😑", "😬", "🫠", "🙄", "😯", "😦", "😧", "😮", "😲", "🥱", "😴", "🤤", "😪", "😵", "😵‍💫", "🤐", "🥴", "🤢", "🤮", "🤧", "😷", "🤒", "🤕", "🤑", "🤠", "😈", "👿", "👹", "👺", "🤡", "💩", "👻", "💀", "☠️", "👽", "👾", "🤖", "🎃"),
            "👋" to listOf("👋", "🤚", "🖐️", "✋", "🖖", "🫱", "🫲", "🫳", "🫴", "👌", "🤌", "🤏", "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️", "🫵", "👍", "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "🫶", "👐", "🤲", "🤝", "🙏", "✍️", "💅", "🤳", "💪", "🦾", "🦿", "🦵", "🦶", "耳", "🦻", "鼻", "🫀", "🫁", "脑", "齿", "骨", "眼", "👁️", "舌", "嘴", "🫦", "吻", "🫂"),
            "❤️" to listOf("❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❤️‍🔥", "❤️‍🩹", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "☮️", "✝️", "☪️", "🕉️", "☸️", "✡️", "🔯", "🕎", "☯️", "☦️", "🛐", "⛎", "♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓", "🆔", "⚛️", "🉑", "☢️", "☣️", "📴", "📳", "🈶", "🈚", "🈸", "🈺", "🈷️", "✴️", "🆚", "💮", "🉐", "㊙️", "㊗️", "🈴", "🈵", "🈹", "🈲", "🅰️", "🅱️", "🆎", "🆑", "🅾️", "🆘", "❌", "⭕", "🛑", "⛔", "📛", "🚫", "💯", "💢", "♨️", "🚷", "🚯", "🚳", "🚱", "🔞", "📵", "🚭", "❗️", "❕", "❓", "❔", "‼️", "⁉️", "🔅", "🔆", "〽️", "⚠️", "🚸", "🔱", "⚜️", "🔰", "♻️", "✅", "🈯", "💹", "❇️", "✳️", "❎", "🌐", "💠", "Ⓜ️", "🌀", "💤", "🏧", "🚾", "♿", "🅿️", "🈳", "🈂️", "🛂", "🛃", "🛄", "🛅", "🚹", "🚺", "🚼", "⚧", "🚻", "🚮", "🎦", "📶", "🈁", "🆖", "🆗", "🆙", "🆒", "🆕", "🆓"),
            "🐶" to listOf("🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐻‍❄️", "🐨", "🐯", "🦁", "🐮", "🐷", "🐽", "🐸", "🐵", "🙈", "🙉", "🙊", "🐒", "🐔", "🐧", "🐦", "🐤", "🐣", "🐥", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🪱", "🐛", "🦋", "🐌", "🐞", "🐜", "🪰", "🪲", "🪳", "🦟", "🦗", "🕷️", "🕸️", "🦂", "🐢", "🐍", "🦎", "🦖", "🦕", "🐙", "🦑", "🦐", "🦞", "🦀", "🐡", "🐠", "🐟", "🐬", "🐳", "🐋", "🦈", "🦭", "🐊", "🐅", "🐆", "🦓", "🦍", "🦧", "🦣", "🐘", "🦛", "🦏", "🐪", "🐫", "🦒", "🦘", "🦬", "🐃", "🐂", "🐄", "🐎", "🐖", "🐏", "🐑", "🦙", "🐐", "🦌", "🐕", "🐩", "🦮", "🐕‍🦺", "🐈", "🐈‍⬛", "🪶", "🐓", "🦃", "🦤", "🦚", "🦜", "🦢", "🦩", "🕊️", "🐇", "🦝", "🦨", "🦡", "🦫", "🦦", "🦥", "🐁", "🐀", "🐿️", "🦔", "🐾", "🐉", "🐲", "🌵", "🎄", "🌲", "🌳", "🌴", "🪵", "🌱", "🌿", "☘️", "🍀", "🎍", "🪴", "🎋", "🍃", "🍂", "🍁", "🍄", "🐚", "🪨", "🌾", "💐", "🌷", "🌹", "🥀", "🌺", "🌸", "🌼", "🌻", "🌞", "🌝", "🌛", "🌜", "🌚", "🌕", "🌖", "🌗", "🌘", "🌑", "🌒", "🌓", "🌔", "🌙", "🌎", "🌍", "🌏", "🪐", "💫", "⭐️", "🌟", "✨", "⚡️", "☄️", "💥", "🔥", "🌪️", "🌈", "☀️", "🌤️", "⛅️", "🌥️", "☁️", "🌦️", "🌧️", "🌨️", "🌩️", "❄️", "☃️", "⛄️", "🌬️", "💨", "💧", "💦", "🫧", "☔️", "☂️", "🌊", "🌫️"),
            "🍕" to listOf("🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑", "🥦", "🥬", "🥒", "🌶️", "🫑", "🌽", "🥕", "🫒", "🧄", "🧅", "🥔", "🍠", "🥐", "🥯", "🍞", "🥖", "🥨", "🧀", "🥚", "🍳", "🧈", "🥞", "🧇", "🥓", "🥩", "🍗", "🍖", "🦴", "🌭", "🍔", "🍟", "🍕", "🫓", "🥪", "🥙", "🧆", "🌮", "🌯", "🫔", "🥗", "🥘", "🫕", "🥫", "🍝", "🍜", "🍲", "🍛", "🍣", "🍱", "🥟", "🦪", "🍤", "🍙", "🍚", "🍘", "🍥", "🥠", "🥮", "🍢", "🍡", "🍧", "🍨", "🍦", "🥧", "🧁", "🍰", "🎂", "🍮", "🍭", "🍬", "🍫", "🍿", "🍩", "🍪", "🌰", "🥜", "🍯", "🥛", "🍼", "🫖", "☕️", "🍵", "🧃", "🥤", "🧋", "🍶", "🍺", "🍻", "🥂", "🍷", "🥃", "🍸", "🍹", "🧉", "🍾", "🧊", "🥄", "🍴", "🍽️", "🥣", "🥡", "🥢", "🧂"),
            "⚽" to listOf("⚽️", "🏀", "🏈", "⚾️", "🥎", "🎾", "🏐", "🏉", "🥏", "🎱", "🪀", "🏓", "🏸", "🏒", "🏑", "🥍", "🏏", "🪃", "🥅", "⛳️", "🪁", "🏹", "🎣", "🤿", "🥊", "🥋", "🎽", "🛹", "🛼", "🛷", "⛸️", "🥌", "🎿", "⛷️", "🏂", "🪂", "🏋️", "🤼", "🤸", "🤺", "⛹️", "🤾", "🧗", "🧘", "🏇", "🚴", "🚵", "🎯", "🏆", "🥇", "🥈", "🥉", "🏅", "🎖️", "🎫", "🎟️", "🎪", "🤹", "🎭", "🩰", "🎨", "🎬", "🎤", "🎧", "🎼", "🎹", "🥁", "🪘", "🎷", "🎺", "🪗", "🎸", "🪕", "🎻", "🎲", "♟️", "🎯", "🎳", "🎮", "🎰", "🧩", "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "🚐", "🛻", "🚚", "🚛", "🚜", "🛵", "🏍️", "🛺", "🚲", "🛴", "🚀", "🛸", "🚁", "✈️", "🛫", "🛬", "⛵️", "🚤", "🛳️", "🚢", "⚓️", "📱", "💻", "⌨️", "🖥️", "🖨️", "🕹️", "💾", "💿", "📀", "📷", "📸", "📹", "🎥", "📽️", "🎞️", "📞", "☎️", "📟", "📠", "📺", "📻", "🎙️", "🎚️", "🎛️", "⏱️", "⏲️", "⏰", "🕰️", "⌛️", "⏳", "📡", "🔋", "🪫", "🔌", "💡", "🔦", "🕯️", "🧯", "🗑️", "🛢️", "💸", "💵", "💴", "💶", "💷", "🪙", "💰", "💳", "💎", "⚖️", "🪜", "🧰", "🪛", "🔧", "锤子", "⚒️", "🛠️", "⛏️", "🪚", "螺母螺栓", "齿轮", "捕鼠器", "砖块", "链条", "磁铁", "水枪", "炸弹", "鞭炮", "斧头", "菜刀", "匕首", "交叉剑", "盾牌", "香烟", "棺材", "墓碑", "骨灰盒", "花瓶", "水晶球", "魔杖", "邪眼", "念珠", "理发店", "炼金", "望远镜", "显微镜", "洞", "创可贴", "听诊器", "药丸", "注射器", "血液", "DNA", "微生物", "培养皿", "试管", "温度计", "扫帚", "马桶吸", "洗衣篮", "卫生纸", "马桶", "水龙头", "淋浴", "浴缸", "香皂", "牙刷", "剃须刀", "海绵", "水桶", "乳液", "按铃", "钥匙", "老钥匙", "门", "椅子", "沙发", "床", "睡觉", "相框", "购物袋", "购物车", "礼物", "气球", "鲤鱼旗", "缎带", "魔杖", "皮纳塔", "五彩纸屑", "礼花筒", "人偶", "灯笼", "风铃", "红包", "信", "带心信封", "收件", "邮件", "情书", "邮筒", "包裹", "标签", "告示牌", "邮箱", "邮箱关闭", "邮箱打开", "邮筒", "卷轴", "页", "文件", "书签", "收据", "图表", "趋势图", "下降图", "记事本", "日历", "撕历", "日程表", "名片盒", "文件盒", "投票箱", "文件柜", "剪贴板", "文件夹", "打开文件夹", "分隔卡", "报纸", "报纸", "笔记本", "红书", "绿书", "蓝书", "黄书", "书本", "打开的书", "书签", "安全别针", "回形针", "连结", "直尺", "三角尺", "算盘", "图钉", "大头针", "剪刀", "钢笔", "羽毛笔", "笔尖", "画笔", "蜡笔", "备忘录", "铅笔", "放大镜", "缩小镜", "带钥匙锁", "带密码锁", "锁", "开锁")
        )
    }

    var activeMainTab by remember { mutableStateOf(0) } // 0 = Emojis, 1 = Stickers, 2 = GIFs

    val mainTabIcons = listOf("😀", "🎨", "🎬")

    val stickerData = remember {
        listOf(
            "🐱" to listOf(
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExcDdtb2VybjRtNXBhbHkwdGNzeXZuYmFpdXByNDMzbWZkMjRwNTVjdiZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/CjmvTCZf2U3p09Cn0h/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExOHp1bjl2ZXR3MndmZ2xsaTRxbmsxdTJtNmYzcGlndmtobG8zZ214dSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/MLhyp4bB4RdtvE16vU/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExOWl1bnNuaHprcGsyMzJ2OHgwMmN3b2dmeWk2Nnp0cGxibDVvdGJzeSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/BzyTuYCmvSORqs1ABM/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExc2pxbWZhbndrdnpxcXV1NXRrOGk5NDUzdml0bTZ6aWhxbXBmNmN0MyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/JPbDhArCeRBDy/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNWV6OWd3NWprOXR4ZmVzYmVxZ2U4bWV5MXV0OTc1bGNvNmtycGZxeCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/vFKqnCdLPNOKc/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExazhyOHM3bnlhODlyMWFkcnU5eWg0OHpsMnkzbXk5MGJtbXlnaGpxdSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/BzyTuYCmvSORqs1ABM/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExbmQxdmF0OWRtcHBic3ByMjFxeGFkZmptbm42MXR2YzFxcDRtdGFudCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/CjmvTCZf2U3p09Cn0h/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExOHM3bnlhODlyMWFkcnU5eWg0OHpsMnkzbXk5MGJtbXlnaGpxdSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/3o7TKoWXm3okO1kgHC/giphy.gif"
            ),
            "😹" to listOf(
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExM3ZydnA5NDZqZW9jcXJxeW9vY3R5bnB4bnUzbWhjYW43OHd2MWdrcCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/3o7TKoWXm3okO1kgHC/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExaG9xdGl3NDYzaGFxdHRudTNzeGlxN3IwbXBubTh2M2dtcHljMjI1aCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/l0G17m03mSAIb248M/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExdmtuc251ZnkxbGk0Mmx4MjRxaGxseWZvZmdtbnM4MmxibThydmduZSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/3o6Zt62PeJedb4f6fC/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHpzNmFpNmRxc25rdjY3eXk0Y21kNzI3bmZicjFjczg5Y2EwcnJ3ciZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/l0HlHJGHe3yAMhdQY/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExYWQxcW4xazVsNHR4cXp1aGJlMHpxZnVxc3d4dTV3M25mdmE3NjdndCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/d3mlE7uhX8KFgEmY/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNmQ1cTlveWp6ZDFnNmU0NWpwbWFvYnR0dm43MGpxNTR1NnRldjFyeSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/g01ZnwAUvutuK8GIQn/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNzU2dWJpMHBwb2pxbjVsOTg2ejFybGpvMnFjcW42MmFmYnEydmJvbyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/94EQmVHkveNck/giphy.gif"
            ),
            "🎭" to listOf(
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExYWQxcW4xazVsNHR4cXp1aGJlMHpxZnVxc3d4dTV3M25mdmE3NjdndCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/d3mlE7uhX8KFgEmY/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNmQ1cTlveWp6ZDFnNmU0NWpwbWFvYnR0dm43MGpxNTR1NnRldjFyeSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/g01ZnwAUvutuK8GIQn/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExaGcxNGttMmRjNWxwbXpycGhhNDdzNGFtd2V6b2ptcTF6MjhwaXQxdSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/QMHoU66sBXCAU/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExZWx6MGJ1enpxNW1reWc1OGdrMXRzaXR4MDFvNGp6NHVvdTlzM3V4eiZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/g9582DNuQppxC/giphy.gif"
            ),
            "🎉" to listOf(
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExOTVsbGs2YmJhMGthNHB0OWJveXgxa3FiNWJycGl5c25oZzlza3N0eCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/l0HlBO7eyXzSZkJri/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNm00eHNsa3J2bDJrNmY1YmhqYTZmOTFlYjR1dGJnaHR1cDJ1Yzg3MyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/26ufdipQqU2lhNA4g/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExaGcxNGttMmRjNWxwbXpycGhhNDdzNGFtd2V6b2ptcTF6MjhwaXQxdSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/QMHoU66sBXCAU/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExZWx6MGJ1enpxNW1reWc1OGdrMXRzaXR4MDFvNGp6NHVvdTlzM3V4eiZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/g9582DNuQppxC/giphy.gif"
            )
        )
    }

    val gifData = remember {
        listOf(
            "🔥" to listOf(
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExOTVsbGs2YmJhMGthNHB0OWJveXgxa3FiNWJycGl5c25oZzlza3N0eCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/l0HlBO7eyXzSZkJri/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNm00eHNsa3J2bDJrNmY1YmhqYTZmOTFlYjR1dGJnaHR1cDJ1Yzg3MyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/26ufdipQqU2lhNA4g/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExbjIxcXNmNzBvaGFqNThud3BwcnJmd3ZpcnlyZDRsaHJ5MXM4MnJjNyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/3o7abKhOpu0NwenH3y/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExZnp4NGd2aDFtdXJjZWpydTVsbG1rdmdzeG03NXFjMXByajgyamptNSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/l3q2K5jinAlChoCLS/giphy.gif"
            ),
            "🐱" to listOf(
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExc2pxbWZhbndrdnpxcXV1NXRrOGk5NDUzdml0bTZ6aWhxbXBmNmN0MyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/JPbDhArCeRBDy/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExazhyOHM3bnlhODlyMWFkcnU5eWg0OHpsMnkzbXk5MGJtbXlnaGpxdSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/BzyTuYCmvSORqs1ABM/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExbmQxdmF0OWRtcHBic3ByMjFxeGFkZmptbm42MXR2YzFxcDRtdGFudCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/CjmvTCZf2U3p09Cn0h/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNWV6OWd3NWprOXR4ZmVzYmVxZ2U4bWV5MXV0OTc1bGNvNmtycGZxeCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/vFKqnCdLPNOKc/giphy.gif"
            ),
            "🎭" to listOf(
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExYWQxcW4xazVsNHR4cXp1aGJlMHpxZnVxc3d4dTV3M25mdmE3NjdndCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/d3mlE7uhX8KFgEmY/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNmQ1cTlveWp6ZDFnNmU0NWpwbWFvYnR0dm43MGpxNTR1NnRldjFyeSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/g01ZnwAUvutuK8GIQn/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNzU2dWJpMHBwb2pxbjVsOTg2ejFybGpvMnFjcW42MmFmYnEydmJvbyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/94EQmVHkveNck/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExaGcxNGttMmRjNWxwbXpycGhhNDdzNGFtd2V6b2ptcTF6MjhwaXQxdSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/QMHoU66sBXCAU/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExZWx6MGJ1enpxNW1reWc1OGdrMXRzaXR4MDFvNGp6NHVvdTlzM3V4eiZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/g9582DNuQppxC/giphy.gif"
            ),
            "❤️" to listOf(
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExZWx6MGJ1enpxNW1reWc1OGdrMXRzaXR4MDFvNGp6NHVvdTlzM3V4eiZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/g9582DNuQppxC/giphy.gif",
                "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExaGcxNGttMmRjNWxwbXpycGhhNDdzNGFtd2V6b2ptcTF6MjhwaXQxdSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/QMHoU66sBXCAU/giphy.gif"
            )
        )
    }

    var selectedEmojiCategoryIndex by remember { mutableStateOf(0) }
    var selectedStickerCategoryIndex by remember { mutableStateOf(0) }
    var selectedGifCategoryIndex by remember { mutableStateOf(0) }

    val pickerContext = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .background(if (isNightMode) Color(0xFF14151C) else Color(0xFFF8FAFC))
            .border(
                1.dp,
                if (isNightMode) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(vertical = 4.dp)
    ) {
        // Left Vertical Main Tabs Bar (Emojis, Stickers, GIFs as small icons on the left)
        Column(
            modifier = Modifier
                .width(44.dp)
                .fillMaxHeight()
                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            mainTabIcons.forEachIndexed { idx, icon ->
                val isSelected = activeMainTab == idx
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) Color(0xFF2563EB)
                            else if (isNightMode) Color.White.copy(alpha = 0.06f)
                            else Color(0xFFE2E8F0)
                        )
                        .clickable { activeMainTab = idx },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 18.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp)
                .background(if (isNightMode) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
        )

        // Right Content Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 6.dp)
        ) {
            when (activeMainTab) {
                0 -> {
                    // Emojis Section - Pure Icon Categories
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(emojiCategories.size) { idx ->
                            val isSelected = selectedEmojiCategoryIndex == idx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) Color(0xFF2563EB).copy(alpha = 0.2f)
                                        else if (isNightMode) Color.White.copy(alpha = 0.04f)
                                        else Color(0xFFF1F5F9)
                                    )
                                    .clickable { selectedEmojiCategoryIndex = idx }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = emojiCategories[idx].first,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val currentList = emojiCategories[selectedEmojiCategoryIndex].second
                    val chunkedEmojis = remember(selectedEmojiCategoryIndex) { currentList.chunked(5) }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(chunkedEmojis.size) { rowIdx ->
                            val rowItems = chunkedEmojis[rowIdx]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (emoji in rowItems) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .clickable { onEmojiSelect(emoji) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 28.sp)
                                    }
                                }
                                val emptySlots = 5 - rowItems.size
                                for (e in 0 until emptySlots) {
                                    Spacer(modifier = Modifier.size(48.dp))
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Stickers Section - Pure Icon Categories
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(stickerData.size) { idx ->
                            val isSelected = selectedStickerCategoryIndex == idx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) Color(0xFF2563EB).copy(alpha = 0.2f)
                                        else if (isNightMode) Color.White.copy(alpha = 0.04f)
                                        else Color(0xFFF1F5F9)
                                    )
                                    .clickable { selectedStickerCategoryIndex = idx }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stickerData[idx].first,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val currentStickers = stickerData[selectedStickerCategoryIndex].second
                    val chunkedStickers = remember(selectedStickerCategoryIndex) { currentStickers.chunked(3) }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chunkedStickers.size) { rowIdx ->
                            val rowItems = chunkedStickers[rowIdx]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (url in rowItems) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(75.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isNightMode) Color(0xFF1E202B) else Color(0xFFE2E8F0))
                                            .clickable {
                                                onSendMedia?.invoke("[IMAGE_ATTACHMENT|$url] 🎨 Sticker")
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val req = remember(url) {
                                            coil.request.ImageRequest.Builder(pickerContext)
                                                .data(url)
                                                .apply {
                                                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                                                        decoderFactory(coil.decode.ImageDecoderDecoder.Factory())
                                                    } else {
                                                        decoderFactory(coil.decode.GifDecoder.Factory())
                                                    }
                                                }
                                                .build()
                                        }
                                        AsyncImage(
                                            model = req,
                                            contentDescription = "Sticker",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                val emptySlots = 3 - rowItems.size
                                for (e in 0 until emptySlots) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // GIFs Section - Pure Icon Categories
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(gifData.size) { idx ->
                            val isSelected = selectedGifCategoryIndex == idx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) Color(0xFF2563EB).copy(alpha = 0.2f)
                                        else if (isNightMode) Color.White.copy(alpha = 0.04f)
                                        else Color(0xFFF1F5F9)
                                    )
                                    .clickable { selectedGifCategoryIndex = idx }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = gifData[idx].first,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val currentGifs = gifData[selectedGifCategoryIndex].second
                    val chunkedGifs = remember(selectedGifCategoryIndex) { currentGifs.chunked(2) }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chunkedGifs.size) { rowIdx ->
                            val rowItems = chunkedGifs[rowIdx]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (url in rowItems) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(85.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isNightMode) Color(0xFF1E202B) else Color(0xFFE2E8F0))
                                            .clickable {
                                                onSendMedia?.invoke("[IMAGE_ATTACHMENT|$url] 🎬 GIF")
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val req = remember(url) {
                                            coil.request.ImageRequest.Builder(pickerContext)
                                                .data(url)
                                                .apply {
                                                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                                                        decoderFactory(coil.decode.ImageDecoderDecoder.Factory())
                                                    } else {
                                                        decoderFactory(coil.decode.GifDecoder.Factory())
                                                    }
                                                }
                                                .build()
                                        }
                                        AsyncImage(
                                            model = req,
                                            contentDescription = "GIF",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                val emptySlots = 2 - rowItems.size
                                for (e in 0 until emptySlots) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernAttachmentChip(
    label: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    textColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ModernBouncingDotsTypingIndicator(
    isNightMode: Boolean,
    partnerName: String = "Partner"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots_bouncing")

    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, delayMillis = 120, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, delayMillis = 240, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1Alpha"
    )

    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, delayMillis = 120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2Alpha"
    )

    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, delayMillis = 240, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3Alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        val bubbleShape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
        Box(
            modifier = Modifier
                .clip(bubbleShape)
                .background(if (isNightMode) Color(0xFF1C1D27) else Color.White)
                .border(
                    width = 1.dp,
                    color = if (isNightMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0),
                    shape = bubbleShape
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Dot 1 (Cyan/Sky)
                Box(
                    modifier = Modifier
                        .offset(y = dot1Offset.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8).copy(alpha = dot1Alpha))
                )
                // Dot 2 (Royal Blue)
                Box(
                    modifier = Modifier
                        .offset(y = dot2Offset.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB).copy(alpha = dot2Alpha))
                )
                // Dot 3 (Violet/Indigo)
                Box(
                    modifier = Modifier
                        .offset(y = dot3Offset.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8B5CF6).copy(alpha = dot3Alpha))
                )
            }
        }
    }
}

@Composable
fun SwipeableMessageRow(
    onSwipeReply: () -> Unit,
    onSwipeForward: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "swipeOffset"
    )
    val view = LocalView.current
    var hasHapticReply by remember { mutableStateOf(false) }
    var hasHapticForward by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        hasHapticReply = false
                        hasHapticForward = false
                    },
                    onDragEnd = {
                        if (offsetX > 75f || hasHapticReply) {
                            onSwipeReply()
                        }
                        offsetX = 0f
                        hasHapticReply = false
                        hasHapticForward = false
                    },
                    onDragCancel = {
                        offsetX = 0f
                        hasHapticReply = false
                        hasHapticForward = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        // Only allow left-to-right swipe for reply (forward removed)
                        val newOffset = (offsetX + dragAmount * 0.85f).coerceIn(0f, 200f)
                        offsetX = newOffset
                        if (newOffset > 75f && !hasHapticReply) {
                            try { view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS) } catch (_: Exception) {}
                            hasHapticReply = true
                        }
                    }
                )
            }
    ) {
        // Left Action Indicator: Reply Icon (when swiping left-to-right)
        if (animatedOffsetX > 8f) {
            val replyProgress = (animatedOffsetX / 80f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .graphicsLayer {
                        scaleX = 0.5f + 0.5f * replyProgress
                        scaleY = 0.5f + 0.5f * replyProgress
                        alpha = replyProgress
                    }
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = "Reply",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Message content with animated offset
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .fillMaxWidth()
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FigmaMessageBubbleRow(
    message: MessageEntity,
    isNightMode: Boolean,
    animTextColor: Color,
    reactions: List<com.example.data.local.ReactionEntity> = emptyList(),
    currentUid: String = "",
    currentUserName: String = "You",
    currentUserAvatar: String = "",
    isLastMessage: Boolean = false,
    showDetails: Boolean = false,
    onClick: (() -> Unit)? = null,
    onToggleReadStatus: ((Long, Boolean) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onReactionClick: ((String) -> Unit)? = null,
    onVote: ((String) -> Unit)? = null,
    onReplyQuoteClick: ((String) -> Unit)? = null,
    onDeleteMessage: ((MessageEntity) -> Unit)? = null
) {
    val view = LocalView.current
    val isUser = message.isFromUser
    var rawText = message.text.trim()
    var displayReplySender = message.replySenderName
    var displayReplySnippet = message.replySnippet

    if (rawText.startsWith("[REPLY_QUOTE:")) {
        try {
            val meta = rawText.substringAfter("[REPLY_QUOTE:").substringBefore("]")
            val encSender = meta.substringBefore("|")
            val encSnippet = meta.substringAfter("|")
            if (displayReplySender.isNullOrBlank()) {
                displayReplySender = java.net.URLDecoder.decode(encSender, "UTF-8")
            }
            if (displayReplySnippet.isNullOrBlank()) {
                displayReplySnippet = java.net.URLDecoder.decode(encSnippet, "UTF-8")
            }
            rawText = rawText.substringAfter("]").trim()
        } catch (_: Exception) {}
    }
    val text = rawText

    // 1. System Events
    if (message.messageType == "SYSTEM_EVENT") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isNightMode) Color(0xFF1E202B).copy(alpha = 0.85f) else Color(0xFFE2E8F0).copy(alpha = 0.85f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (isNightMode) Color(0xFFD1D5DB) else Color(0xFF475569),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    // 2. Deleted For Everyone Messages
    if (message.isDeletedForEveryone) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isNightMode) Color(0xFF1A1C23).copy(alpha = 0.6f) else Color(0xFFF1F5F9))
                    .border(1.dp, if (isNightMode) Color.White.copy(alpha = 0.05f) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Deleted",
                        tint = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "This message was deleted",
                        color = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
        return
    }

    val isPhoto = text.contains("IMAGE_ATTACHMENT") || text.contains("IMAGE_ALBUM") || text.startsWith("🖼️") || text.startsWith("📸") || text.startsWith("http://") || text.startsWith("https://") || text.startsWith("content://") || text.startsWith("file://")
    val isPoll = text.contains("POLL:") || text.startsWith("📊") || text.contains("[POLL_DATA|")
    val isVoice = text.contains("Voice Note") || text.startsWith("🎙️") || text.contains("VIEW_ONCE_AUDIO") || text.contains("AUDIO_BASE64") || text.contains("AUDIO_FILE") || text.contains("VIEW_ONCE_OPENED")
    val isContact = text.contains("Contact:") || text.startsWith("👤")
    val isDocument = text.contains("DOCUMENT_FILE") || text.contains("[Document:") || text.startsWith("📄")
    val isChecklist = text.contains("[CHECKLIST_JSON|") || text.startsWith("📋 Checklist:")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        val bubbleShape = if (isUser) {
            RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
        } else {
            RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        onClick?.invoke()
                        if (isUser) onToggleReadStatus?.invoke(message.id, message.isRead)
                    },
                    onDoubleClick = {
                        try { view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
                        onReactionClick?.invoke("❤️")
                    },
                    onLongClick = {
                        try { view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
                        onLongClick?.invoke()
                    }
                )
        ) {
            val isCustomMediaBubble = isPhoto || isPoll || isVoice || isContact || isDocument || isChecklist

            if (isCustomMediaBubble) {
                Column {
                    // Forward Tag
                    if (message.isForwarded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Forward,
                                contentDescription = "Forwarded",
                                tint = if (isUser) Color.White.copy(alpha = 0.8f) else Color(0xFF2563EB),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Forwarded",
                                color = if (isUser) Color.White.copy(alpha = 0.8f) else Color(0xFF2563EB),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }

                    // Reply Quote Box
                    if (!displayReplySnippet.isNullOrBlank() || !displayReplySender.isNullOrBlank() || !message.replyToMessageId.isNullOrBlank()) {
                        Surface(
                            onClick = { message.replyToMessageId?.let { onReplyQuoteClick?.invoke(it) } },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isUser) Color.Black.copy(alpha = 0.18f) else (if (isNightMode) Color.White.copy(alpha = 0.06f) else Color(0xFFF1F5F9)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isUser) Color.White else Color(0xFF2563EB))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayReplySender ?: "Original Message",
                                        color = if (isUser) Color.White else Color(0xFF2563EB),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = displayReplySnippet ?: "Quoted text",
                                        color = if (isUser) Color.White.copy(alpha = 0.85f) else animTextColor.copy(alpha = 0.75f),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    when {
                        isPhoto -> RichImageMessageBubble(
                            imageUrlOrUri = text,
                            caption = text,
                            isUser = isUser,
                            isNightMode = isNightMode,
                            animTextColor = animTextColor,
                            isRead = message.isRead
                        )
                        isPoll -> com.example.ui.components.InteractivePollBubble(
                            rawPollText = text,
                            isUser = isUser,
                            isNightMode = isNightMode,
                            animTextColor = animTextColor,
                            currentUid = currentUid,
                            currentUserName = currentUserName,
                            currentUserAvatar = currentUserAvatar,
                            onVote = { updatedPollText ->
                                onVote?.invoke(updatedPollText)
                            }
                        )
                        isDocument -> com.example.ui.components.DocumentMessageBubble(
                            messageText = text,
                            isUser = isUser,
                            isNightMode = isNightMode,
                            animTextColor = animTextColor
                        )
                        isVoice -> VoiceNoteMessageBubble(
                            messageText = text,
                            isUser = isUser,
                            isNightMode = isNightMode,
                            onViewOncePlayed = {
                                onDeleteMessage?.invoke(message)
                            }
                        )
                        isContact -> SharedContactCardBubble(
                            messageText = text,
                            isUser = isUser,
                            isNightMode = isNightMode
                        )
                        isChecklist -> com.example.ui.components.InteractiveChecklistBubble(
                            rawChecklistText = text,
                            isUser = isUser,
                            isNightMode = isNightMode,
                            animTextColor = animTextColor,
                            currentUid = currentUid,
                            currentUserName = currentUserName,
                            currentUserAvatar = currentUserAvatar,
                            onToggleItem = { updatedChecklistPayload ->
                                onVote?.invoke(updatedChecklistPayload)
                            }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(bubbleShape)
                        .background(
                            if (isUser) {
                                Brush.linearGradient(
                                    listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                )
                            } else {
                                if (isNightMode) {
                                    SolidColor(Color(0xFF1A1C23))
                                } else {
                                    SolidColor(Color.White)
                                }
                            }
                        )
                        .border(
                            width = if (!isUser) 1.dp else 0.dp,
                            color = if (!isUser) {
                                if (isNightMode) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)
                            } else Color.Transparent,
                            shape = bubbleShape
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        // Forward Tag
                        if (message.isForwarded) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Forward,
                                    contentDescription = "Forwarded",
                                    tint = if (isUser) Color.White.copy(alpha = 0.8f) else Color(0xFF2563EB),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Forwarded",
                                    color = if (isUser) Color.White.copy(alpha = 0.8f) else Color(0xFF2563EB),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }

                        // Reply Quote Box Inside Bubble
                        if (!message.replySnippet.isNullOrBlank() || !message.replyToMessageId.isNullOrBlank()) {
                            Surface(
                                onClick = { message.replyToMessageId?.let { onReplyQuoteClick?.invoke(it) } },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isUser) Color.Black.copy(alpha = 0.18f) else (if (isNightMode) Color.White.copy(alpha = 0.06f) else Color(0xFFF1F5F9)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isUser) Color.White else Color(0xFF2563EB))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = message.replySenderName ?: "Original Message",
                                            color = if (isUser) Color.White else Color(0xFF2563EB),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = message.replySnippet ?: "Quoted text",
                                            color = if (isUser) Color.White.copy(alpha = 0.85f) else animTextColor.copy(alpha = 0.75f),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = message.text,
                            color = if (isUser) Color.White else animTextColor,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // Message Delivery Timestamp, Edit Status & Pin Checks (Only show on latest message or when clicked)
            val formattedTime = remember(message.timestamp, message.timestampString) {
                if (message.timestampString.isNotBlank()) {
                    message.timestampString
                } else if (message.timestamp > 0L) {
                    java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(message.timestamp))
                } else {
                    "Just now"
                }
            }

            if (showDetails || isLastMessage) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    if (message.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }

                    Text(
                        text = formattedTime,
                        color = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    if (message.isEdited) {
                        Text(
                            text = " • edited",
                            color = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }

                    if (isUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val isSeen = message.deliveryState == "READ" || message.isRead

                        // KnotLink message status:
                        // red = sent/delivered, green = seen. No gray state.
                        val dotColor = if (isSeen) {
                            Color(0xFF10B981)
                        } else {
                            Color(0xFFEF4444)
                        }
                        val animatedDotColor by animateColorAsState(
                            targetValue = dotColor,
                            animationSpec = tween(400),
                            label = "statusDotColor"
                        )
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(animatedDotColor)
                        )
                    }
                }
            }

            // Reactions Row Beneath Bubble
            if (reactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    val groupedReactions = reactions.groupBy { it.emoji }
                    groupedReactions.forEach { (emoji, list) ->
                        val hasUserReacted = list.any { it.uid == currentUid }
                        Surface(
                            onClick = { onReactionClick?.invoke(emoji) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (hasUserReacted) Color(0xFF2563EB).copy(alpha = 0.2f) else (if (isNightMode) Color(0xFF1E202B) else Color(0xFFE2E8F0)),
                            border = BorderStroke(
                                1.dp,
                                if (hasUserReacted) Color(0xFF2563EB) else (if (isNightMode) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = emoji, fontSize = 12.sp)
                                if (list.size > 1) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = list.size.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasUserReacted) Color(0xFF2563EB) else animTextColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RichImageMessageBubble(
    imageUrlOrUri: String,
    caption: String,
    isUser: Boolean,
    isNightMode: Boolean,
    animTextColor: Color,
    isRead: Boolean = false
) {
    var fullscreenInitialIndex by remember { mutableStateOf<Int?>(null) }

    val bubbleShape = if (isUser) {
        RoundedCornerShape(22.dp, 22.dp, 4.dp, 22.dp)
    } else {
        RoundedCornerShape(22.dp, 22.dp, 22.dp, 4.dp)
    }

    val imageUrls: List<String> = remember(imageUrlOrUri) {
        if (imageUrlOrUri.contains("[IMAGE_ALBUM|")) {
            val raw = imageUrlOrUri.substringAfter("[IMAGE_ALBUM|").substringBefore("]")
            raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else if (imageUrlOrUri.contains("[IMAGE_ATTACHMENT|")) {
            val raw = imageUrlOrUri.substringAfter("[IMAGE_ATTACHMENT|").substringBefore("]").substringBefore(" ").trim()
            if (raw.isNotBlank()) listOf(raw) else listOf(imageUrlOrUri)
        } else {
            val uriRegex = Regex("(content://\\S+|file://\\S+|https?://\\S+)")
            val matches = uriRegex.findAll(imageUrlOrUri).map { it.value }.toList()
            if (matches.isNotEmpty()) matches else listOf(imageUrlOrUri)
        }
    }

    val cleanCaption = remember(caption) {
        caption
            .replace(Regex("\\[IMAGE_ALBUM\\|.*?\\|"), "")
            .replace(Regex("\\[IMAGE_ALBUM\\|.*?\\]"), "")
            .replace(Regex("\\[IMAGE_ATTACHMENT\\|.*?\\|"), "")
            .replace(Regex("\\[IMAGE_ATTACHMENT\\|.*?\\]"), "")
            .replace(Regex("🖼️\\s*\\d*\\s*Photos?"), "")
            .replace("🖼️", "")
            .replace("📸", "")
            .trim()
    }

    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Surface(
            shape = bubbleShape,
            color = if (isNightMode) Color(0xFF1E202B) else Color.White,
            shadowElevation = 3.dp,
            border = BorderStroke(
                1.dp,
                if (isUser) Color(0xFF2563EB).copy(alpha = 0.5f)
                else if (isNightMode) Color.White.copy(0.12f) else Color(0xFFE2E8F0)
            ),
            modifier = Modifier
                .width(260.dp)
                .clip(bubbleShape)
        ) {
            Column {
                if (imageUrls.size <= 1) {
                    // Single Photo
                    val singleUrl = imageUrls.firstOrNull() ?: imageUrlOrUri
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clickable { fullscreenInitialIndex = 0 }
                    ) {
                        AsyncImage(
                            model = singleUrl,
                            contentDescription = "Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    // Album Grid of Multiple Photos
                    PhotoAlbumGrid(
                        images = imageUrls,
                        onImageClick = { idx -> fullscreenInitialIndex = idx }
                    )
                }

                if (cleanCaption.isNotBlank() && !cleanCaption.startsWith("[")) {
                    Text(
                        text = cleanCaption,
                        color = if (isNightMode) Color.White else Color(0xFF0F172A),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    // Full Screen Photo Gallery Modal
    fullscreenInitialIndex?.let { startIdx ->
        FullScreenPhotoGalleryModal(
            images = imageUrls,
            initialIndex = startIdx,
            onDismiss = { fullscreenInitialIndex = null }
        )
    }
}

@Composable
fun PhotoAlbumGrid(
    images: List<String>,
    onImageClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        when (images.size) {
            2 -> {
                Row(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClick(0) }
                    ) {
                        AsyncImage(
                            model = images[0],
                            contentDescription = "Photo 1",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClick(1) }
                    ) {
                        AsyncImage(
                            model = images[1],
                            contentDescription = "Photo 2",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            3 -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onImageClick(0) }
                ) {
                    AsyncImage(
                        model = images[0],
                        contentDescription = "Photo 1",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClick(1) }
                    ) {
                        AsyncImage(
                            model = images[1],
                            contentDescription = "Photo 2",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClick(2) }
                    ) {
                        AsyncImage(
                            model = images[2],
                            contentDescription = "Photo 3",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            else -> {
                // 4 or more photos (2x2 Grid with +N overlay on the 4th item)
                Row(
                    modifier = Modifier.fillMaxWidth().height(115.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClick(0) }
                    ) {
                        AsyncImage(
                            model = images[0],
                            contentDescription = "Photo 1",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClick(1) }
                    ) {
                        AsyncImage(
                            model = images[1],
                            contentDescription = "Photo 2",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(115.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClick(2) }
                    ) {
                        AsyncImage(
                            model = images[2],
                            contentDescription = "Photo 3",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClick(3) }
                    ) {
                        AsyncImage(
                            model = images[3],
                            contentDescription = "Photo 4",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (images.size > 4) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${images.size - 3}",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenPhotoGalleryModal(
    images: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0)),
        pageCount = { images.size }
    )
    var showDownloadDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Horizontal Pager for swiping between photos
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val imageUrl = images.getOrNull(page) ?: ""
                ZoomableImageViewer(
                    imageUrl = imageUrl,
                    onTapOutside = onDismiss
                )
            }

            // Transparent Navigation Chevron Arrows (Left & Right)
            if (images.size > 1) {
                if (pagerState.currentPage > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Photo",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (pagerState.currentPage < images.size - 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Photo",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Top Floating UI Matched Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // UI matched Cross / Close button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Photo Counter Indicator
                if (images.size > 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${images.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(42.dp))
                }

                // Download Button next to close/counter
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable {
                            if (images.size > 1) {
                                showDownloadDialog = true
                            } else {
                                val curImg = images.getOrNull(0) ?: ""
                                if (curImg.isNotBlank()) {
                                    isSaving = true
                                    scope.launch {
                                        val ok = ImageDownloader.saveImageToDevice(context, curImg)
                                        isSaving = false
                                        if (ok) {
                                            Toast.makeText(context, "Photo saved to gallery 📥", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to save photo", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Photo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    // Download Multiple Options Dialog
    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = {
                Text(
                    text = "Download Photos",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Choose whether to save the current photo or download all ${images.size} photos at full original resolution.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDownloadDialog = false
                        isSaving = true
                        scope.launch {
                            var savedCount = 0
                            for (img in images) {
                                val ok = ImageDownloader.saveImageToDevice(context, img)
                                if (ok) savedCount++
                            }
                            isSaving = false
                            Toast.makeText(context, "Saved $savedCount of ${images.size} photos to gallery 📥", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save All (${images.size})", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDownloadDialog = false
                        val curImg = images.getOrNull(pagerState.currentPage) ?: ""
                        if (curImg.isNotBlank()) {
                            isSaving = true
                            scope.launch {
                                val ok = ImageDownloader.saveImageToDevice(context, curImg)
                                isSaving = false
                                if (ok) {
                                    Toast.makeText(context, "Photo saved to gallery 📥", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to save photo", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Just This", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun ZoomableImageViewer(
    imageUrl: String,
    onTapOutside: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val rawUrl: Any = remember(imageUrl) {
        if (imageUrl.contains("[IMAGE_ATTACHMENT|")) {
            imageUrl.substringAfter("[IMAGE_ATTACHMENT|").substringBefore("]").substringBefore(" ").trim()
        } else if (imageUrl.contains("[IMAGE_ALBUM|")) {
            imageUrl.substringAfter("[IMAGE_ALBUM|").substringBefore("]").substringBefore(",").trim()
        } else imageUrl.trim()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                    },
                    onTap = {
                        if (scale == 1f) {
                            onTapOutside()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 4.5f)
                    scale = newScale
                    if (scale > 1f) {
                        val maxOffsetX = (scale - 1f) * 1000f
                        val maxOffsetY = (scale - 1f) * 1000f
                        offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                        offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = rawUrl,
            contentDescription = "Full Screen Photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}

@Composable
fun TelegramAudioWaveformBeats(
    progress: Float,
    isUser: Boolean,
    isNightMode: Boolean,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val barHeights = remember {
        listOf(
            8.dp, 14.dp, 22.dp, 12.dp, 26.dp, 30.dp, 18.dp, 24.dp,
            28.dp, 14.dp, 20.dp, 28.dp, 16.dp, 24.dp, 22.dp, 12.dp,
            26.dp, 18.dp, 10.dp, 6.dp
        )
    }

    Box(
        modifier = modifier
            .height(34.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeekFraction(frac)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeekFraction(frac)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            barHeights.forEachIndexed { idx, height ->
                val barFraction = idx / (barHeights.size - 1).toFloat()
                val isPlayed = barFraction <= progress
                val activeBrush = if (isUser) {
                    Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.85f)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFF2563EB), Color(0xFF06B6D4)))
                }
                val inactiveColor = if (isUser) Color.White.copy(alpha = 0.3f) else (if (isNightMode) Color.White.copy(alpha = 0.2f) else Color(0xFFCBD5E1))

                Box(
                    modifier = Modifier
                        .width(3.2.dp)
                        .height(height)
                        .clip(CircleShape)
                        .background(if (isPlayed) activeBrush else Brush.linearGradient(listOf(inactiveColor, inactiveColor)))
                )
            }
        }
    }
}

@Composable
fun VoiceNoteMessageBubble(
    messageText: String,
    isUser: Boolean,
    isNightMode: Boolean,
    onViewOncePlayed: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableStateOf(0f) }
    var totalDurationMs by remember { mutableStateOf(1000f) }
    var isDraggingSlider by remember { mutableStateOf(false) }

    val isViewOnce = remember(messageText) {
        messageText.contains("VIEW_ONCE_AUDIO") || messageText.contains("1-Time") || messageText.contains("VIEW_ONCE_OPENED")
    }
    val hasBeenPlayedOnce = remember(messageText) {
        messageText.contains("VIEW_ONCE_OPENED")
    }

    // Parse real audio file path or Base64 string e.g. [AUDIO_BASE64|data|dur] or [AUDIO_FILE|path|dur]
    val (audioData, isBase64) = remember(messageText) {
        if (messageText.contains("[AUDIO_BASE64|") || messageText.contains("[VIEW_ONCE_AUDIO_BASE64|")) {
            val parts = messageText.split("|")
            if (parts.size >= 2) Pair(parts[1].trim(), true) else Pair(null, false)
        } else if (messageText.contains("[AUDIO_FILE|") || messageText.contains("[VIEW_ONCE_AUDIO_FILE|")) {
            val parts = messageText.split("|")
            if (parts.size >= 2) Pair(parts[1].trim(), false) else Pair(null, false)
        } else if (messageText.contains("/")) {
            val idx = messageText.indexOf('/')
            val endIdx = messageText.indexOf(']', idx).let { if (it == -1) messageText.length else it }
            if (idx != -1) Pair(messageText.substring(idx, endIdx).trim(), false) else Pair(null, false)
        } else Pair(null, false)
    }

    val mediaPlayer = remember(audioData) { android.media.MediaPlayer() }

    DisposableEffect(audioData) {
        if (!audioData.isNullOrBlank()) {
            val targetFile: java.io.File? = if (isBase64) {
                try {
                    val bytes = android.util.Base64.decode(audioData, android.util.Base64.NO_WRAP)
                    val cacheFile = java.io.File(context.cacheDir, "recv_voice_${audioData.hashCode()}.m4a")
                    if (!cacheFile.exists() || cacheFile.length() == 0L) {
                        cacheFile.writeBytes(bytes)
                    }
                    cacheFile
                } catch (e: Exception) {
                    android.util.Log.e("VoiceNoteMessageBubble", "Error decoding audio base64: ${e.message}")
                    null
                }
            } else {
                val file = java.io.File(audioData)
                if (file.exists()) file else null
            }

            if (targetFile != null && targetFile.exists()) {
                try {
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(targetFile.absolutePath)
                    mediaPlayer.prepare()
                    val dur = mediaPlayer.duration.toFloat()
                    if (dur > 0f) totalDurationMs = dur
                } catch (e: Exception) {
                    android.util.Log.e("VoiceNoteMessageBubble", "Error preparing audio: ${e.message}")
                }
            }
        }
        mediaPlayer.setOnCompletionListener {
            isPlaying = false
            currentPositionMs = 0f
            if (isViewOnce) {
                onViewOncePlayed?.invoke()
            }
        }
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isPlaying, isDraggingSlider) {
        if (isPlaying && !isDraggingSlider) {
            while (isPlaying) {
                try {
                    if (mediaPlayer.isPlaying) {
                        currentPositionMs = mediaPlayer.currentPosition.toFloat()
                        val dur = mediaPlayer.duration.toFloat()
                        if (dur > 0f) totalDurationMs = dur
                    } else {
                        isPlaying = false
                    }
                } catch (_: Exception) {
                    isPlaying = false
                }
                kotlinx.coroutines.delay(100L)
            }
        }
    }

    val bubbleShape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    val parsedDurationStr = remember(messageText) {
        if (messageText.contains("|")) {
            val parts = messageText.split("|")
            val lastPart = parts.last().substringBefore("]")
            if (lastPart.contains(":")) lastPart else null
        } else {
            null
        }
    }
    val curSecs = (currentPositionMs / 1000f).toInt()
    val totSecs = (totalDurationMs / 1000f).toInt()
    val curTimeStr = String.format("%02d:%02d", curSecs / 60, curSecs % 60)
    val totTimeStr = parsedDurationStr ?: String.format("%02d:%02d", totSecs / 60, totSecs % 60)

    Surface(
        shape = bubbleShape,
        color = if (isUser) Color(0xFF2563EB) else if (isNightMode) Color(0xFF1E202B) else Color(0xFFF1F5F9),
        border = BorderStroke(
            1.dp,
            if (isUser) Color.Transparent else if (isNightMode) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1)
        ),
        modifier = Modifier
            .widthIn(min = 210.dp, max = 270.dp)
            .padding(vertical = 4.dp)
    ) {
        Column {
            if (isViewOnce) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isUser) Color.White.copy(alpha = 0.35f) else Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("1", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasBeenPlayedOnce) {
                            if (isUser) "Opened" else "Opened • Expired"
                        } else {
                            "1-Time Voice Note"
                        },
                        color = if (isUser) Color.White.copy(alpha = 0.9f) else (if (isNightMode) Color(0xFF94A3B8) else Color(0xFF475569)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play/Pause Button - prominent circular button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUser)
                                Color.White.copy(0.25f)
                            else if (isNightMode)
                                Color(0xFF2563EB)
                            else
                                Color(0xFF0F172A)
                        )
                        .clickable(enabled = !hasBeenPlayedOnce) {
                            if (isPlaying) {
                                try {
                                    mediaPlayer.pause()
                                } catch (_: Exception) {}
                                isPlaying = false
                            } else {
                                try {
                                    mediaPlayer.start()
                                    isPlaying = true
                                } catch (e: Exception) {
                                    isPlaying = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause Voice Note",
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .offset(x = if (isPlaying) 0.dp else 1.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Waveform Beats Bar in center
                val progress = if (totalDurationMs > 0f) (currentPositionMs / totalDurationMs).coerceIn(0f, 1f) else 0f
                TelegramAudioWaveformBeats(
                    progress = progress,
                    isUser = isUser,
                    isNightMode = isNightMode,
                    onSeekFraction = { frac ->
                        if (!hasBeenPlayedOnce) {
                            val targetMs = frac * totalDurationMs
                            currentPositionMs = targetMs
                            try {
                                mediaPlayer.seekTo(targetMs.toInt())
                            } catch (_: Exception) {}
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Time Count display with high contrast
                Text(
                    text = if (isPlaying) curTimeStr else totTimeStr,
                    color = if (isUser) Color.White else (if (isNightMode) Color.White else Color(0xFF0F172A)),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}


@Composable
fun SharedContactCardBubble(
    messageText: String,
    isUser: Boolean,
    isNightMode: Boolean
) {
    val context = LocalContext.current
    val cleanText = messageText.replace("👤", "").replace("Contact:", "").trim()
    val parts = cleanText.split("(", ")")
    val name = parts.firstOrNull()?.trim() ?: cleanText
    val phone = if (parts.size > 1) parts[1].trim() else ""

    val bubbleShape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Surface(
        shape = bubbleShape,
        color = if (isUser) Color(0xFF1E40AF) else (if (isNightMode) Color(0xFF1E202B) else Color(0xFFF1F5F9)),
        border = BorderStroke(1.dp, if (isUser) Color(0xFF3B82F6).copy(alpha = 0.5f) else (if (isNightMode) Color(0xFF272732) else Color(0xFFE2E8F0))),
        modifier = Modifier.widthIn(max = 280.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        color = if (isUser || isNightMode) Color.White else Color(0xFF0F172A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (phone.isNotEmpty()) {
                        Text(
                            text = phone,
                            color = if (isUser) Color(0xFF93C5FD) else (if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B)),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "Shared Contact Card 👤",
                        color = Color(0xFF10B981),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (phone.isNotEmpty()) {
                Button(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${phone.replace("[^0-9+]".toRegex(), "")}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Dialing $phone", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call $name", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun InteractiveCallDialog(
    chatName: String,
    isVideo: Boolean,
    isUserOnline: Boolean,
    isNightMode: Boolean,
    onDismiss: () -> Unit,
    onLaunchFullVideo: () -> Unit,
    onSendVoiceNote: () -> Unit
) {
    var callStage by remember { mutableStateOf("CONNECTING") } // "CONNECTING", "RINGING", "OFFLINE"
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1200)
        callStage = if (isUserOnline) "RINGING" else "OFFLINE"
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(if (isNightMode) Color(0xFF12141D) else Color.White)
                .border(1.dp, if (isNightMode) Color.White.copy(0.12f) else Color(0xFFCBD5E1), RoundedCornerShape(32.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Pulsing Animation Rings around Avatar
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.25f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (callStage == "CONNECTING" || callStage == "RINGING") {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB).copy(alpha = 0.15f))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF2563EB), Color(0xFF00C6FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chatName.take(1),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = chatName,
                    color = if (isNightMode) Color.White else Color(0xFF0F172A),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Call Status Text
                Text(
                    text = when (callStage) {
                        "CONNECTING" -> "Connecting to KnotLink Mesh..."
                        "RINGING" -> if (isVideo) "Ringing Video Call..." else "Ringing Audio Call..."
                        else -> "User is not online now"
                    },
                    color = when (callStage) {
                        "OFFLINE" -> Color(0xFFEF4444)
                        "RINGING" -> Color(0xFF10B981)
                        else -> Color(0xFF2563EB)
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Call Stage Actions
                when (callStage) {
                    "CONNECTING" -> {
                        Text(
                            text = "Establishing peer-to-peer encrypted channel...",
                            color = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }

                    "RINGING" -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mute
                            IconButton(onClick = { isMuted = !isMuted }) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(if (isMuted) Color(0xFFEF4444) else Color(0xFF2563EB).copy(0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = "Mute",
                                        tint = if (isMuted) Color.White else Color(0xFF2563EB)
                                    )
                                }
                            }

                            // Speaker
                            IconButton(onClick = { isSpeakerOn = !isSpeakerOn }) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(if (isSpeakerOn) Color(0xFF2563EB) else Color(0xFF2563EB).copy(0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Speaker",
                                        tint = if (isSpeakerOn) Color.White else Color(0xFF2563EB)
                                    )
                                }
                            }

                            // End Call Red Button
                            IconButton(onClick = onDismiss) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CallEnd,
                                        contentDescription = "End Call",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isVideo) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))))
                                    .clickable(onClick = onLaunchFullVideo)
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Open Full Video Stream 📹",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    "OFFLINE" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "$chatName is currently disconnected from the KnotLink network.",
                                color = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF2563EB))
                                        .clickable(onClick = onSendVoiceNote)
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Leave Voice Note 🎙️",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isNightMode) Color(0xFF1E202B) else Color(0xFFE2E8F0))
                                        .clickable(onClick = onDismiss)
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Close",
                                        color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatProfileDetailsPage(
    chatId: String,
    chatName: String,
    viewModel: BitChatViewModel,
    isNightMode: Boolean,
    onDismiss: () -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    val context = LocalContext.current
    val isGroupChat = chatName.contains("[Group]", ignoreCase = true) || chatId.startsWith("group_")

    val groupAvatarMap by viewModel.groupAvatarMap.collectAsState()
    val customAvatarPath = groupAvatarMap[chatId]
    val hasCustomAvatar = customAvatarPath != null && customAvatarPath.isNotEmpty() && File(customAvatarPath).exists()

    val groupNameMap by viewModel.groupNameMap.collectAsState()
    val currentName = groupNameMap[chatId] ?: chatName

    var editedName by remember(currentName) { mutableStateOf(currentName) }
    var isEditingName by remember { mutableStateOf(false) }

    val membersMap by viewModel.groupMembersMap.collectAsState()
    val members = membersMap[chatId] ?: emptyList()

    val groupApprovalMap by viewModel.groupAdminApprovalMap.collectAsState()
    val needsAdminApproval = groupApprovalMap[chatId] ?: false

    val contacts by viewModel.contacts.collectAsState()
    val mutedChatIds by viewModel.mutedChatIds.collectAsState()
    val isMuted = mutedChatIds.contains(chatId)

    var selectedMemberForAction by remember { mutableStateOf<GroupMember?>(null) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showBlockContactDialog by remember { mutableStateOf(false) }
    var showReportAbuseDialog by remember { mutableStateOf(false) }
    val blockedUsers by viewModel.blockedUsers.collectAsState()
    val allChats by viewModel.allChats.collectAsState()
    val userIdentity by viewModel.userIdentity.collectAsState(initial = null)
    val activeChatEntity = remember(allChats, chatId) { allChats.find { it.id == chatId } }
    val currentProfileUid = userIdentity?.supabaseUid?.ifBlank { userIdentity?.email } ?: "me"
    val myEmail = userIdentity?.email ?: ""
    val myUname = userIdentity?.username?.trim()?.lowercase()?.removePrefix("@")?.removeSuffix(".link") ?: ""

    val targetParticipantUid = remember(activeChatEntity, chatId, currentProfileUid, myEmail, myUname) {
        val parts = activeChatEntity?.participantUids?.split(",")?.map { it.trim() } ?: emptyList()
        val other = parts.firstOrNull { p ->
            val clean = p.lowercase().removePrefix("@").removeSuffix(".link")
            clean.isNotBlank() && clean != currentProfileUid.lowercase() && clean != myEmail.lowercase() && clean != myUname && clean != "me" && clean != "user_me"
        }
        if (other != null) {
            other
        } else if (chatId.startsWith("chat_")) {
            val unPrefixed = chatId.removePrefix("chat_")
            val subParts = unPrefixed.split("_")
            subParts.firstOrNull { s ->
                val clean = s.lowercase().removePrefix("@").removeSuffix(".link")
                clean.isNotBlank() && clean != currentProfileUid.lowercase() && clean != myEmail.lowercase() && clean != myUname && clean != "me" && clean != "user_me"
            } ?: unPrefixed
        } else {
            chatId
        }
    }

    var partnerProfile by remember { mutableStateOf<com.example.data.supabase.SupabaseProfile?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(targetParticipantUid) {
        if (!isGroupChat && targetParticipantUid.isNotBlank() && targetParticipantUid != "uid_target") {
            try {
                val p = com.example.data.supabase.SupabaseService.getProfile(targetParticipantUid).getOrNull()
                    ?: com.example.data.supabase.SupabaseService.getProfileByUsername(targetParticipantUid).getOrNull()
                partnerProfile = p
            } catch (e: Exception) {}
        }
    }

    val isContactCurrentlyBlocked = remember(blockedUsers, targetParticipantUid) {
        blockedUsers.any { it.targetUid == targetParticipantUid }
    }

    val directUserAvatar = remember(activeChatEntity, contacts, targetParticipantUid) {
        val contact = contacts.find {
            it.id.equals(targetParticipantUid, ignoreCase = true) ||
            (targetParticipantUid.isNotBlank() && it.name.equals(targetParticipantUid, ignoreCase = true))
        }
        val avatar = contact?.avatarType?.ifBlank { null } ?: activeChatEntity?.avatarType?.ifBlank { null }
        if (avatar != null && avatar != "default" && avatar != "assistant" && avatar != "group_default") avatar else null
    }

    // Group Avatar Image Launcher (Gallery picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val file = File(context.cacheDir, "group_avatar_${System.currentTimeMillis()}.png")
                val out = FileOutputStream(file)
                bitmap?.compress(Bitmap.CompressFormat.PNG, 90, out)
                out.flush()
                out.close()
                viewModel.updateGroupAvatar(chatId, file.absolutePath)
                Toast.makeText(context, "Group photo updated successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to update group photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isNightMode) Color(0xFF0F1015) else Color(0xFFF8FAFC))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Full Screen Top Navigation Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isNightMode) Color(0xFF14151C) else Color.White,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isNightMode) Color.White else Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isGroupChat) "Group Profile & Settings" else "User Details",
                            color = if (isNightMode) Color.White else Color(0xFF0F172A),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentName,
                            color = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(if (isNightMode) Color(0xFF1E202B) else Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mute Notification", color = if (isNightMode) Color.White else Color.Black) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.toggleMuteChat(chatId)
                                    Toast.makeText(context, if (isMuted) "Notifications unmuted" else "Notifications muted", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB)
                                    )
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Clear Chat History", color = if (isNightMode) Color.White else Color.Black) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.clearMessagesForChat(chatId)
                                    Toast.makeText(context, "Cleared messages", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            )

                            if (!isGroupChat) {
                                DropdownMenuItem(
                                    text = { Text(if (isContactCurrentlyBlocked) "Unblock Contact" else "Block Contact", color = if (isNightMode) Color.White else Color.Black) },
                                    onClick = {
                                        menuExpanded = false
                                        if (isContactCurrentlyBlocked) {
                                            viewModel.unblockUser(targetParticipantUid)
                                            Toast.makeText(context, "Contact unblocked", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showBlockContactDialog = true
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isContactCurrentlyBlocked) Icons.Default.Check else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (isContactCurrentlyBlocked) Color(0xFF10B981) else Color(0xFFEF4444)
                                        )
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("Send to Archive", color = if (isNightMode) Color.White else Color.Black) },
                                onClick = {
                                    menuExpanded = false
                                    activeChatEntity?.let {
                                        viewModel.archiveChat(it)
                                        Toast.makeText(context, "Chat archived successfully!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } ?: run {
                                        Toast.makeText(context, "Archive feature is only available for active chat threads", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Archive,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B)
                                    )
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Search", color = if (isNightMode) Color.White else Color.Black) },
                                onClick = {
                                    menuExpanded = false
                                    Toast.makeText(context, "Search enabled! Use the top search bar in chat to find items.", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB)
                                    )
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Create New Group", color = if (isNightMode) Color.White else Color.Black) },
                                onClick = {
                                    menuExpanded = false
                                    Toast.makeText(context, "Select 'New Group' from the main chat list screen to create a group with your contacts.", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981)
                                    )
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Add Shortcut", color = if (isNightMode) Color.White else Color.Black) },
                                onClick = {
                                    menuExpanded = false
                                    Toast.makeText(context, "Shortcut added to home screen successfully!", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Fully Scrollable Screen Body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Group / Profile Photo Area
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .clickable(enabled = isGroupChat) { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCustomAvatar) {
                        AsyncImage(
                            model = File(customAvatarPath!!),
                            contentDescription = "Group Avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else if (directUserAvatar != null) {
                        AsyncImage(
                            model = if (directUserAvatar.startsWith("/")) File(directUserAvatar) else directUserAvatar,
                            contentDescription = "User Avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        if (isGroupChat)
                                            listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                                        else
                                            listOf(Color(0xFF2563EB), Color(0xFF00C6FF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (isGroupChat) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB))
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Photo",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Name / Editable Title
                if (isGroupChat) {
                    if (isEditingName) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { if (it.length <= 60) editedName = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${editedName.length}/60 letters",
                                color = if (isNightMode) Color(0xFF71717A) else Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (editedName.isNotBlank()) {
                                        viewModel.updateGroupName(chatId, editedName.trim())
                                        isEditingName = false
                                        Toast.makeText(context, "Group name saved", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save Group Name", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = currentName,
                                color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { isEditingName = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Name",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${members.size} / 50 Members • Encrypted Group",
                        color = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = chatName,
                        color = if (isNightMode) Color.White else Color(0xFF0F172A),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val cleanHandle = chatName.trim().removePrefix("@").removeSuffix(".link").removeSuffix(".bit").removeSuffix(".chat").lowercase().replace(" ", "")
                    Text(
                        text = "@${cleanHandle.ifBlank { "user" }}.link",
                        color = Color(0xFF2563EB),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Encrypted Direct Peer • Active Node",
                        color = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Group Management Tabs & Sections
                if (isGroupChat) {
                    val joinRequestsMap by viewModel.groupJoinRequestsMap.collectAsState()
                    val joinRequests = joinRequestsMap[chatId] ?: emptyList()

                    val hiveMessagesMap by viewModel.hiveMessagesMap.collectAsState()
                    val hiveMessages = hiveMessagesMap[chatId] ?: emptyList()

                    val chatMessages by remember(chatId) { viewModel.getMessagesForChat(chatId) }.collectAsState(initial = emptyList())

                    var selectedGroupTab by remember { mutableStateOf("MEMBERS") }

                    // Horizontal Navigation Bar for Group Management
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tabs = listOf(
                            "MEMBERS" to "Members (${members.size})",
                            "ADMINS" to "Admins",
                            "REQUESTS" to "Req List (${joinRequests.size})",
                            "HIVE" to "Hive Msg (${hiveMessages.size})",
                            "MEDIA" to "Shared Media"
                        )
                        items(tabs) { (tabKey, tabLabel) ->
                            val isSelected = selectedGroupTab == tabKey
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFF2563EB) else (if (isNightMode) Color(0xFF1F202B) else Color(0xFFF1F5F9)))
                                    .clickable { selectedGroupTab = tabKey }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = tabLabel,
                                    color = if (isSelected) Color.White else (if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B)),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (selectedGroupTab) {
                        "MEMBERS" -> {
                            // Group Members Header & Add Member Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "All Members (${members.size})",
                                    color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (members.size < 50) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF2563EB).copy(alpha = 0.15f))
                                            .clickable { showAddMemberDialog = true }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.PersonAdd,
                                                contentDescription = "Add Member",
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Add Member",
                                                color = Color(0xFF2563EB),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                members.forEach { member ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedMemberForAction = member },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isNightMode) Color(0xFF1A1C25) else Color(0xFFF1F5F9)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (member.isOwner) Color(0xFFF59E0B)
                                                        else if (member.isAdmin) Color(0xFF8B5CF6)
                                                        else Color(0xFF2563EB)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = member.name.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = member.name,
                                                        color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (member.nickname.isNotBlank()) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "(${member.nickname})",
                                                            color = Color(0xFF2563EB),
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = when {
                                                        member.isOwner -> "Owner • Main Admin"
                                                        member.isAdmin -> "Sub-Admin / Admin"
                                                        else -> "Member"
                                                    },
                                                    color = if (member.isAdmin || member.isOwner) Color(0xFF8B5CF6) else (if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B)),
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Options",
                                                tint = if (isNightMode) Color(0xFF71717A) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        "ADMINS" -> {
                            val adminMembers = members.filter { it.isAdmin || it.isOwner }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Admins & Sub-Admins (${adminMembers.size})",
                                    color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    adminMembers.forEach { admin ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedMemberForAction = admin },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isNightMode) Color(0xFF1A1C25) else Color(0xFFF1F5F9)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(if (admin.isOwner) Color(0xFFF59E0B) else Color(0xFF8B5CF6)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (admin.isOwner) Icons.Default.Star else Icons.Default.Shield,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = admin.name,
                                                        color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = if (admin.isOwner) "Owner (Full Access)" else "Sub-Admin (Custom Access)",
                                                        color = Color(0xFF8B5CF6),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Text(
                                                    text = "Manage",
                                                    color = Color(0xFF2563EB),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "REQUESTS" -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Join Requests (${joinRequests.size})",
                                        color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Require Approval",
                                            color = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Switch(
                                            checked = needsAdminApproval,
                                            onCheckedChange = { viewModel.toggleGroupAdminApproval(chatId) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF8B5CF6))
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (joinRequests.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No pending join requests",
                                            color = if (isNightMode) Color(0xFF71717A) else Color(0xFF94A3B8),
                                            fontSize = 13.sp
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        joinRequests.forEach { req ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (isNightMode) Color(0xFF1A1C25) else Color(0xFFF1F5F9)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF0EA5E9)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = req.userName.take(1).uppercase(),
                                                            color = Color.White,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = req.userName,
                                                            color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "Requested ${req.requestTime}",
                                                            color = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        IconButton(
                                                            onClick = { viewModel.declineJoinRequest(chatId, req.id) },
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Decline",
                                                                tint = Color(0xFFEF4444),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = { viewModel.approveJoinRequest(chatId, req) },
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Approve",
                                                                tint = Color(0xFF10B981),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "HIVE" -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Hive Messages / Pinned List (${hiveMessages.size})",
                                    color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                if (hiveMessages.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No pinned messages in Hive yet",
                                            color = if (isNightMode) Color(0xFF71717A) else Color(0xFF94A3B8),
                                            fontSize = 13.sp
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        hiveMessages.forEach { msg ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (isNightMode) Color(0xFF1A1C25) else Color(0xFFF1F5F9)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PushPin,
                                                        contentDescription = "Pinned",
                                                        tint = Color(0xFFF59E0B),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = msg.senderName,
                                                            color = Color(0xFF8B5CF6),
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = msg.text,
                                                            color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                                            fontSize = 13.sp,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.unpinMessageFromHive(chatId, msg.id) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Unpin",
                                                            tint = if (isNightMode) Color(0xFF71717A) else Color(0xFF94A3B8),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "MEDIA" -> {
                            var mediaFilter by remember { mutableStateOf("Photos") }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val subFilters = listOf("Photos", "Videos", "Audio", "Files")
                                    subFilters.forEach { sf ->
                                        val isSfSelected = mediaFilter == sf
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSfSelected) (if (isNightMode) Color.White else Color.Black) else (if (isNightMode) Color(0xFF272730) else Color(0xFFE2E8F0)))
                                                .clickable { mediaFilter = sf }
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = sf,
                                                color = if (isSfSelected) (if (isNightMode) Color.Black else Color.White) else (if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B)),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val filteredMedia = remember(chatMessages, mediaFilter) {
                                    chatMessages.filter { msg ->
                                        when (mediaFilter) {
                                            "Photos" -> msg.text.contains("[Photo]", ignoreCase = true) || msg.text.contains("photo_", ignoreCase = true) || msg.text.contains("[Image]", ignoreCase = true)
                                            "Videos" -> msg.text.contains("[Video]", ignoreCase = true) || msg.text.contains("video_", ignoreCase = true)
                                            "Audio" -> msg.text.contains("[Voice]", ignoreCase = true) || msg.text.contains("[Audio]", ignoreCase = true) || msg.text.contains(".mp3", ignoreCase = true)
                                            else -> msg.text.contains("[File]", ignoreCase = true) || msg.text.contains("http", ignoreCase = true) || msg.text.contains(".pdf", ignoreCase = true)
                                        }
                                    }
                                }

                                if (filteredMedia.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No shared $mediaFilter found",
                                            color = if (isNightMode) Color(0xFF71717A) else Color(0xFF94A3B8),
                                            fontSize = 13.sp
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val limitedMedia = filteredMedia.take(20)
                                        limitedMedia.forEach { item ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isNightMode) Color(0xFF1A1C25) else Color(0xFFF1F5F9)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = when (mediaFilter) {
                                                            "Photos" -> Icons.Default.Image
                                                            "Videos" -> Icons.Default.Videocam
                                                            "Audio" -> Icons.Default.Mic
                                                            else -> Icons.Default.AttachFile
                                                        },
                                                        contentDescription = null,
                                                        tint = if (isNightMode) Color.White else Color.Black,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = item.text,
                                                            color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${item.senderName} • ${item.timestampString}",
                                                            color = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Danger Zone / Group Exit Container at Bottom
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFEF4444).copy(alpha = if (isNightMode) 0.12f else 0.06f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Group Exit & Danger Zone",
                                color = Color(0xFFEF4444),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Leaving will remove you from this group's encrypted channel.",
                                color = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B),
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.leaveOrDeleteGroup(chatId) {
                                        onDismiss()
                                        onBackClick()
                                    }
                                    Toast.makeText(context, "Left $currentName", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Leave Group",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Leave / Delete Group", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                } else {
                    // Actions for Direct Personal User Chat (Redesigned)
                    val chatMessages by remember(chatId) { viewModel.getMessagesForChat(chatId) }.collectAsState(initial = emptyList())
                    var selectedPillTab by remember { mutableStateOf("Photos") }
                    val pills = listOf("Photos", "Videos", "Audios", "Files", "Links")

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // User Profile Info Card
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isNightMode) Color(0xFF1E202C) else Color(0xFFF1F5F9)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Username
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AlternateEmail,
                                        contentDescription = null,
                                        tint = if (isNightMode) Color.White else Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Username",
                                            color = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = if (partnerProfile?.username?.isNotBlank() == true) "@${partnerProfile?.username}" else "@${chatName.lowercase().replace(" ", "")}.link",
                                            color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Designation
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Work,
                                        contentDescription = null,
                                        tint = if (isNightMode) Color.White else Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Designation",
                                            color = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = if (partnerProfile?.profession?.isNotBlank() == true) partnerProfile!!.profession else "Verified KnotLink User",
                                            color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Member Since
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = if (isNightMode) Color.White else Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Member Since",
                                            color = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        val joinedDateStr = if (partnerProfile?.joinedDate?.isNotBlank() == true) partnerProfile!!.joinedDate else "2026"
                                        Text(
                                            text = joinedDateStr,
                                            color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Media Filter Pill list
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pills) { pill ->
                                val isSelected = selectedPillTab == pill
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) {
                                                if (isNightMode) Color.White else Color.Black
                                            } else {
                                                if (isNightMode) Color(0xFF1E202B) else Color(0xFFE2E8F0)
                                            }
                                        )
                                        .clickable { selectedPillTab = pill }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = pill,
                                        color = if (isSelected) {
                                            if (isNightMode) Color.Black else Color.White
                                        } else {
                                            if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B)
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Shared Media List for Direct Chat
                        val directSharedItems = remember(chatMessages, selectedPillTab) {
                            chatMessages.filter { msg ->
                                val text = msg.text
                                when (selectedPillTab) {
                                    "Photos" -> text.contains("IMAGE_ATTACHMENT") || text.contains("IMAGE_ALBUM") || text.startsWith("🖼️") || text.startsWith("📸") || (text.startsWith("http") && (text.endsWith(".png") || text.endsWith(".jpg") || text.endsWith(".jpeg") || text.endsWith(".webp")))
                                    "Videos" -> text.contains("[Video]") || text.contains("video_") || text.contains(".mp4")
                                    "Audios" -> text.contains("Voice Note") || text.startsWith("🎙️") || text.contains("VIEW_ONCE_AUDIO") || text.contains("AUDIO_BASE64") || text.contains("AUDIO_FILE") || text.contains(".mp3")
                                    "Files" -> text.contains("DOCUMENT_FILE") || text.contains("[Document:") || text.startsWith("📄") || text.contains(".pdf") || text.contains(".zip")
                                    "Links" -> text.contains("http://") || text.contains("https://")
                                    else -> false
                                }
                            }
                        }

                        if (directSharedItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No shared $selectedPillTab in this chat",
                                    color = if (isNightMode) Color(0xFF71717A) else Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val limitedDirectItems = directSharedItems.take(20)
                                limitedDirectItems.forEach { item ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isNightMode) Color(0xFF161822) else Color.White
                                        ),
                                        border = BorderStroke(1.dp, if (isNightMode) Color.White.copy(0.08f) else Color(0xFFE2E8F0)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = when (selectedPillTab) {
                                                    "Photos" -> Icons.Default.Image
                                                    "Videos" -> Icons.Default.Videocam
                                                    "Audios" -> Icons.Default.Mic
                                                    "Files" -> Icons.Default.AttachFile
                                                    else -> Icons.Default.Link
                                                },
                                                contentDescription = null,
                                                tint = if (isNightMode) Color.White else Color.Black,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = getCleanMediaPreview(item.text, selectedPillTab),
                                                    color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${item.senderName} • ${item.timestampString}",
                                                    color = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            if (selectedPillTab == "Photos" || selectedPillTab == "Videos" || selectedPillTab == "Audios" || selectedPillTab == "Files") {
                                                IconButton(
                                                    onClick = {
                                                        Toast.makeText(context, "Downloading item to Downloads...", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Download,
                                                        contentDescription = "Download",
                                                        tint = if (isNightMode) Color.White else Color.Black
                                                    )
                                                }
                                            } else if (selectedPillTab == "Links") {
                                                val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                                                IconButton(
                                                    onClick = {
                                                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(item.text))
                                                        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copy Link",
                                                        tint = if (isNightMode) Color.White else Color.Black
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBlockContactDialog) {
        AlertDialog(
            onDismissRequest = { showBlockContactDialog = false },
            title = { Text("Block $currentName?") },
            text = { Text("Blocked contacts will no longer be able to send you messages or call you.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.blockUser(targetParticipantUid, currentName, currentName)
                    showBlockContactDialog = false
                    Toast.makeText(context, "Contact blocked", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Block", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockContactDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showReportAbuseDialog) {
        var selectedReason by remember { mutableStateOf("Spam or Advertising") }
        var reportDetails by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        val reportReasons = listOf(
            "Spam or Advertising",
            "Harassment or Bullying",
            "Impersonation",
            "Inappropriate Content",
            "Malware or Phishing",
            "Other"
        )

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showReportAbuseDialog = false },
            title = {
                Text(
                    text = if (isGroupChat) "Report Group" else "Report Contact",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Select a reason for this report:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    reportReasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedReason == reason) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (selectedReason == reason) Color(0xFF2563EB) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(reason, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { reportDetails = it },
                        label = { Text("Additional details (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.submitAbuseReport(
                            targetType = if (isGroupChat) "GROUP" else "USER",
                            targetId = if (isGroupChat) chatId else targetParticipantUid,
                            reason = selectedReason,
                            details = reportDetails
                        ) {
                            isSubmitting = false
                            showReportAbuseDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    enabled = !isSubmitting
                ) {
                    Text(if (isSubmitting) "Submitting..." else "Submit Report", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportAbuseDialog = false }, enabled = !isSubmitting) {
                    Text("Cancel")
                }
            }
        )
    }

    // Member Options Action Dialog (Nickname, Admin Role, Kick)
    selectedMemberForAction?.let { member ->
        var nicknameInput by remember { mutableStateOf(member.nickname) }

        Dialog(onDismissRequest = { selectedMemberForAction = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isNightMode) Color(0xFF1E202B) else Color.White)
                    .border(1.dp, if (isNightMode) Color(0xFF27272A) else Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Manage ${member.name}",
                        color = if (isNightMode) Color.White else Color(0xFF0F172A),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Nickname Input
                    Text(
                        text = "Group Nickname",
                        color = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B),
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = nicknameInput,
                        onValueChange = { nicknameInput = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = if (isNightMode) Color.White else Color(0xFF0F172A),
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.updateMemberNickname(chatId, member.id, nicknameInput.trim())
                            selectedMemberForAction = null
                            Toast.makeText(context, "Nickname updated", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Nickname", color = Color.White, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!member.isOwner) {
                        Button(
                            onClick = {
                                viewModel.toggleMemberAdminRole(chatId, member.id)
                                selectedMemberForAction = null
                                Toast.makeText(
                                    context,
                                    if (member.isAdmin) "Removed admin rights" else "Promoted to Group Admin",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Role",
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (member.isAdmin) "Dismiss as Admin" else "Make Group Admin",
                                color = Color(0xFF8B5CF6),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (member.isAdmin) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Sub-Admin Custom Feature Access",
                                color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val permissions = listOf(
                                Triple("deleteMessages", "Delete Messages", member.canDeleteMessages),
                                Triple("pinMessages", "Pin Messages", member.canPinMessages),
                                Triple("changeInfo", "Edit Group Info", member.canChangeGroupInfo),
                                Triple("inviteMembers", "Invite Members", member.canInviteMembers),
                                Triple("muteMembers", "Mute Members", member.canMuteMembers)
                            )

                            permissions.forEach { (permType, permLabel, isEnabled) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = permLabel,
                                        color = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF475569),
                                        fontSize = 12.sp
                                    )
                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { enabled ->
                                            viewModel.updateMemberPermission(chatId, member.id, permType, enabled)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF8B5CF6)
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.removeMemberFromGroup(chatId, member.id)
                                selectedMemberForAction = null
                                Toast.makeText(context, "${member.name} removed from group", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Kick Member",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kick Out from Group", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Add Member Dialog Launcher
    if (showAddMemberDialog) {
        Dialog(onDismissRequest = { showAddMemberDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isNightMode) Color(0xFF1E202B) else Color.White)
                    .border(1.dp, if (isNightMode) Color(0xFF27272A) else Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Add Member to Group",
                        color = if (isNightMode) Color.White else Color(0xFF0F172A),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(contacts) { contact ->
                            val isAlreadyInGroup = members.any { it.name.equals(contact.name, true) }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAlreadyInGroup) {
                                        viewModel.addMemberToGroup(chatId, contact.name)
                                        showAddMemberDialog = false
                                        Toast.makeText(context, "${contact.name} added to group", Toast.LENGTH_SHORT).show()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAlreadyInGroup) Color.Gray.copy(alpha = 0.1f) else (if (isNightMode) Color(0xFF14151C) else Color(0xFFF1F5F9))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = contact.name,
                                        color = if (isAlreadyInGroup) Color.Gray else (if (isNightMode) Color.White else Color(0xFF0F172A)),
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isAlreadyInGroup) {
                                        Text("Already added", color = Color.Gray, fontSize = 11.sp)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add",
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    animTextColor: Color,
    animSubTextColor: Color,
    isNightMode: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isNightMode) Color(0xFF1E202E) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isNightMode) Color.White.copy(0.08f) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = animTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = animSubTextColor,
                    fontSize = 11.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = animSubTextColor.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TelegramMenuItem(
    icon: ImageVector,
    tint: Color,
    label: String,
    isNightMode: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isNightMode) Color.White else Color(0xFF0F172A),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private private val translationCache = java.util.concurrent.ConcurrentHashMap<String, String>()

fun TelegramTranslateDialog(
    message: MessageEntity,
    isNightMode: Boolean,
    onDismiss: () -> Unit
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val languages = listOf(
        "English (US)", "Bengali (বাংলা)", "Spanish (Español)", 
        "French (Français)", "German (Deutsch)", "Hindi (हिंदी)", 
        "Arabic (العربية)", "Japanese (日本語)", "Chinese (中文)"
    )
    var selectedLanguage by remember { mutableStateOf("English (US)") }
    var translatedText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedLanguage, message.text) {
        isLoading = true
        translatedText = ""

        val cacheKey = selectedLanguage + "\u0000" + message.text.trim()
        val cached = translationCache[cacheKey]
        if (!cached.isNullOrBlank()) {
            translatedText = cached
            isLoading = false
            return@LaunchedEffect
        }

        translatedText = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val buildConfigKey = try {
                val field = com.example.BuildConfig::class.java.getField("GEMINI_API_KEY")
                field.get(null) as? String
            } catch (_: Exception) { null }

            val apiKey = buildConfigKey?.takeIf {
                it.isNotBlank() && it != "MY_GEMINI_API_KEY"
            }

            if (apiKey.isNullOrBlank()) {
                return@withContext "Translation unavailable. Gemini API key is not configured."
            }

            try {
                val modelName = "gemini-3.5-flash-lite"
                val urlString = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"
                val url = java.net.URL(urlString)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 5000
                conn.readTimeout = 8000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-goog-api-key", apiKey)
                conn.doOutput = true

                val prompt = "Translate into ${selectedLanguage}. Return only the translation, no notes, markdown, or quotes:\n${message.text.trim()}"
                val body = org.json.JSONObject().apply {
                    put("contents", org.json.JSONArray().apply {
                        put(org.json.JSONObject().apply {
                            put("parts", org.json.JSONArray().apply {
                                put(org.json.JSONObject().apply { put("text", prompt) })
                            })
                        })
                    })
                    put("generationConfig", org.json.JSONObject().apply {
                        put("temperature", 0.1)
                        put("maxOutputTokens", 256)
                    })
                }

                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

                if (conn.responseCode == 200) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(resp)
                    val text = json.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        ?.trim()

                    if (!text.isNullOrBlank()) {
                        translationCache[cacheKey] = text
                        text
                    } else {
                        "Translation unavailable."
                    }
                } else {
                    "Translation unavailable."
                }
            } catch (_: Exception) {
                "Translation unavailable. Please check internet connection."
            }
        }
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isNightMode) Color(0xFF181A24) else Color.White,
            border = BorderStroke(1.dp, if (isNightMode) Color.White.copy(0.12f) else Color(0xFFE2E8F0)),
            shadowElevation = 12.dp,
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Translate Message",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = if (isNightMode) Color.White else Color(0xFF0F172A)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                // Target Language Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = { dropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isNightMode) Color(0xFF232634) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedLanguage,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isNightMode) Color.White else Color(0xFF0F172A)
                                )
                            }
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                        }
                    }

                    androidx.compose.material3.DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(if (isNightMode) Color(0xFF1E202B) else Color.White)
                    ) {
                        languages.forEach { lang ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = {
                                    Text(
                                        text = lang,
                                        fontWeight = if (lang == selectedLanguage) FontWeight.Bold else FontWeight.Normal,
                                        color = if (lang == selectedLanguage) Color(0xFF8B5CF6) else (if (isNightMode) Color.White else Color.Black)
                                    )
                                },
                                onClick = {
                                    selectedLanguage = lang
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Original Snippet
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Original Text", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isNightMode) Color(0xFF13141B) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isNightMode) Color.White.copy(0.06f) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = message.text,
                            fontSize = 13.sp,
                            color = if (isNightMode) Color.LightGray else Color(0xFF334155),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Translation Result Card
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Translation ($selectedLanguage)", fontSize = 11.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isNightMode) Color(0xFF1E1B2E) else Color(0xFFF3E8FF),
                        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color(0xFF8B5CF6),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(text = "Translating with Gemini...", fontSize = 13.sp, color = Color(0xFF8B5CF6))
                                }
                            } else {
                                Text(
                                    text = translatedText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isNightMode) Color.White else Color(0xFF1E1B4B),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Copy Action Button
                if (!isLoading && translatedText.isNotBlank() && !translatedText.startsWith("Error") && !translatedText.startsWith("Translation unavailable")) {
                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(translatedText))
                            Toast.makeText(context, "Translation copied! 📋", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Copy Translation", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun getCleanMediaPreview(text: String, tab: String): String {
    val clean = text.trim()
    when (tab) {
        "Photos" -> return "🖼️ Photo Attachment"
        "Videos" -> return "🎥 Video Clip Attachment"
        "Audios" -> {
            if (clean.contains("base64", ignoreCase = true) || clean.contains("BASE64")) {
                val duration = clean.substringAfterLast("|", "").substringBefore("]", "Voice Note")
                return "🎙️ Voice Note (${duration.ifBlank { "Audio" }})"
            }
            if (clean.contains("AUDIO_FILE|")) {
                val parts = clean.substringAfter("AUDIO_FILE|").substringBefore("]").split("|")
                val dur = parts.getOrNull(1) ?: "Audio"
                return "🎙️ Voice Note ($dur)"
            }
            return "🎙️ Voice Note Recording"
        }
        "Files" -> {
            if (clean.contains("DOCUMENT_FILE|")) {
                val parts = clean.substringAfter("DOCUMENT_FILE|").substringBefore("]").split("|")
                val name = parts.getOrNull(0) ?: "Document"
                val size = parts.getOrNull(1) ?: ""
                return "📄 $name ($size)"
            }
            return "📄 Shared Document File"
        }
        "Links" -> {
            val url = clean.substringAfter("http://").substringAfter("https://")
            if (url.isNotBlank()) {
                return text
            }
            return "🔗 Shared Web Link"
        }
    }
    return text.substringBefore("\n")
}
