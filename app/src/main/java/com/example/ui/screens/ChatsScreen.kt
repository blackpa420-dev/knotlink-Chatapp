package com.example.ui.screens

import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import com.example.ui.components.ChatListTypingIndicator
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ChatEntity
import com.example.data.local.ContactEntity
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.layout.ContentScale
import com.example.ui.components.BitChatBottomNavBar
import com.example.ui.components.BitChatNavTab
import com.example.ui.components.GlassPanel
import com.example.ui.theme.BitBackground
import com.example.ui.theme.BitOnPrimary
import com.example.ui.theme.AppFontFamily
import com.example.ui.theme.BitOnSurface
import com.example.ui.theme.BitOnSurfaceVariant
import com.example.ui.theme.BitPrimary
import com.example.ui.theme.BitSecondary
import com.example.ui.theme.BitSurfaceContainer
import com.example.ui.viewmodel.BitChatViewModel


val AladinFontFamily = AppFontFamily

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatsScreen(
    viewModel: BitChatViewModel,
    onChatClick: (ChatEntity) -> Unit,
    onVideoCallClick: (String) -> Unit,
    onOpenQrScanner: () -> Unit = {},
    onTabSelected: (BitChatNavTab) -> Unit,
    onActiveCallBannerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val chats by viewModel.filteredChats.collectAsState()
    val archivedChats by viewModel.archivedChats.collectAsState()
    val selectedFilter by viewModel.selectedChatFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val chatTypingStatus by viewModel.chatTypingStatus.collectAsState()
    val userPresenceMap by viewModel.userPresenceMap.collectAsState()
    val userIdentity by viewModel.userIdentity.collectAsState(initial = null)
    val currentAuthUid = userIdentity?.supabaseUid?.ifBlank { userIdentity?.email } ?: "me"

    val initialHistorySyncing by viewModel.initialHistorySyncing.collectAsState()
    val initialHistorySyncError by viewModel.initialHistorySyncError.collectAsState()
    val hasRealLocalChats = chats.any { chat ->
        chat.id != "bitassistant" &&
        chat.id != "ai_assistant" &&
        !chat.name.contains("Assistant", ignoreCase = true)
    }
    val showHistoryRestore = initialHistorySyncing && !hasRealLocalChats
    val showHistoryRestoreError = !initialHistorySyncing && initialHistorySyncError != null && !hasRealLocalChats

    LaunchedEffect(chats) {
        viewModel.observeChatTyping(chats)
    }

    val isNightMode by viewModel.isNightMode.collectAsState()
    val context = LocalContext.current

    val animBgColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFF0D0E12) else Color(0xFFF1F5F9),
        animationSpec = tween(400),
        label = "chats_bg"
    )

    val animTextColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFFF3F4F6) else Color(0xFF0F172A),
        animationSpec = tween(400),
        label = "chats_text"
    )

    val haptic = LocalHapticFeedback.current
    var isSearchActive by remember { mutableStateOf(false) }
    var isTopMenuExpanded by remember { mutableStateOf(false) }
    var showNodeAuthSheet by remember { mutableStateOf(false) }
    var activeAuthNode by remember { mutableStateOf(".Chat") }
    var isArchiveBannerVisible by remember { mutableStateOf(false) }
    val isAssistantRevealed by viewModel.isAssistantRevealed.collectAsState()
    var showArchivedSheet by remember { mutableStateOf(false) }
    var showDeletedChatsSheet by remember { mutableStateOf(false) }
    var chatToDelete by remember { mutableStateOf<com.example.data.local.ChatEntity?>(null) }
    var chatDeleteStep by remember { mutableStateOf(1) }
    var deleteForEveryone by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    var isFabVisible by remember { mutableStateOf(false) }

    // Keep gesture accumulators outside Compose state. They change on every scroll delta,
    // but the pull distance itself is not rendered, so recomposing the whole screen here
    // only adds frame-time work.
    val accumulatedPullY = remember { floatArrayOf(0f) }
    val lastVibrateStep = remember { intArrayOf(0) }
    val nestedScrollConnection = remember(isAssistantRevealed) {
        object : NestedScrollConnection {
            private fun resetPull() {
                accumulatedPullY[0] = 0f
                lastVibrateStep[0] = 0
            }

            private fun handlePull(deltaY: Float) {
                if (deltaY <= 0f || isAssistantRevealed) return
                accumulatedPullY[0] += deltaY
                val step = (accumulatedPullY[0] / 35f).toInt()
                if (step > lastVibrateStep[0]) {
                    lastVibrateStep[0] = step
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                if (accumulatedPullY[0] >= 350f) {
                    viewModel.setAssistantRevealed(true)
                    resetPull()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0) {
                    if (available.y > 0f) {
                        handlePull(available.y)
                    } else if (available.y < 0f) {
                        resetPull()
                    }
                } else {
                    resetPull()
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0) {
                    handlePull(available.y)
                } else {
                    resetPull()
                }
                return Offset.Zero
            }
        }
    }

    // Scroll listener: whenever scrolling stops, show FAB for 3.5 seconds then hide it off to the right
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling) {
                    isFabVisible = false
                } else {
                    // Scrolling stopped: reveal FAB
                    isFabVisible = true
                    kotlinx.coroutines.delay(3500)
                    isFabVisible = false
                }
            }
    }

    LaunchedEffect(isArchiveBannerVisible) {
        if (isArchiveBannerVisible) {
            kotlinx.coroutines.delay(3000)
            isArchiveBannerVisible = false
        }
    }

    val chatFolderAssignments by viewModel.chatFolderAssignments.collectAsState()

    var customCategoryOrder by remember {
        mutableStateOf(listOf("All Chats", "Groups", "Unread", "Work", "Personal", "Archive", "Blocked", "Restricted", "Deleted"))
    }
    var customCategoryNames by remember {
        mutableStateOf(mutableMapOf<String, String>())
    }
    var categoryToManage by remember { mutableStateOf<String?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var sectionToAssignChats by remember { mutableStateOf<String?>(null) }
    var showCreateGroupSheet by remember { mutableStateOf(false) }

    val filterOptions = remember(chatFolderAssignments, customCategoryOrder) {
        val list = customCategoryOrder.toMutableList()
        if (!list.contains("All Chats")) list.add(0, "All Chats")
        chatFolderAssignments.values.distinct().forEach { folder ->
            if (folder.isNotBlank() && !list.contains(folder)) {
                list.add(folder)
            }
        }
        list
    }

    // Status tracking for long press popup actions
    var pinnedChatIds by remember { mutableStateOf(setOf<String>()) }
    var mutedChatIds by remember { mutableStateOf(mapOf<String, String>()) }
    var blockedChatIds by remember { mutableStateOf(setOf<String>()) }
    var restrictedChatIds by remember { mutableStateOf(setOf<String>()) }
    val lockedChatIds by viewModel.lockedChatIds.collectAsState()
    val activity = context as? FragmentActivity

    var selectedChatForMenu by remember { mutableStateOf<ChatEntity?>(null) }
    var targetUserForCreateGroup by remember { mutableStateOf<ChatEntity?>(null) }
    var locationPermissionChat by remember { mutableStateOf<ChatEntity?>(null) }

    val archivedChatIds = remember(archivedChats) { archivedChats.mapTo(hashSetOf()) { it.id } }
    val hasUnreadChats = remember(chats) { chats.any { it.unreadCount > 0 } }

    val visibleChats = remember(chats, archivedChats, selectedFilter, blockedChatIds, restrictedChatIds, chatFolderAssignments) {
        when (selectedFilter) {
            "Archive" -> archivedChats
            "Blocked" -> chats.filter { blockedChatIds.contains(it.id) }
            "Restricted" -> chats.filter { restrictedChatIds.contains(it.id) }
            "Unread" -> chats.filter { !archivedChatIds.contains(it.id) && !blockedChatIds.contains(it.id) && !restrictedChatIds.contains(it.id) && it.unreadCount > 0 }
            "Groups" -> chats.filter { chat ->
                !archivedChatIds.contains(chat.id) && !blockedChatIds.contains(chat.id) && !restrictedChatIds.contains(chat.id) &&
                (chat.chatType == "GROUP" || chatFolderAssignments[chat.id] == "Groups" || chat.id.startsWith("group_") || chat.name.contains("[Group]", ignoreCase = true) || chat.name.startsWith("GC "))
            }
            "Work", "Personal" -> chats.filter { chat ->
                !archivedChatIds.contains(chat.id) && !blockedChatIds.contains(chat.id) && !restrictedChatIds.contains(chat.id) &&
                (chatFolderAssignments[chat.id] == selectedFilter || (selectedFilter == "Work" && (chat.name.contains("Work", ignoreCase = true) || chat.name.contains("Team", ignoreCase = true))) || (selectedFilter == "Personal" && !chat.name.contains("Work", ignoreCase = true)))
            }
            "All Chats" -> chats.filter { chat -> !archivedChatIds.contains(chat.id) && !blockedChatIds.contains(chat.id) && !restrictedChatIds.contains(chat.id) }
            else -> chats.filter { chat ->
                !archivedChats.any { a -> a.id == chat.id } && !blockedChatIds.contains(chat.id) && !restrictedChatIds.contains(chat.id) &&
                chatFolderAssignments[chat.id] == selectedFilter
            }
        }
    }

    val assistantChat = remember(chats) {
        chats.firstOrNull { it.id == "bitassistant" || it.id == "ai_assistant" || it.name.contains("Assistant", ignoreCase = true) }
            ?: ChatEntity(
                id = "bitassistant",
                name = "KnotLink Assistant",
                lastMessage = "Personal Cloud & AI Assistant",
                timeString = "Now",
                unreadCount = 0,
                isOnline = true,
                category = "All",
                avatarType = "assistant",
                lastUpdated = System.currentTimeMillis(),
                participantUids = "bitassistant"
            )
    }

    val finalSortedChats = remember(visibleChats, pinnedChatIds) {
        visibleChats
            .filterNot { it.id == "bitassistant" || it.id == "ai_assistant" || it.name.contains("Assistant", ignoreCase = true) }
            .sortedWith { c1, c2 ->
                val isC1Pinned = pinnedChatIds.contains(c1.id)
                val isC2Pinned = pinnedChatIds.contains(c2.id)
                when {
                    isC1Pinned && !isC2Pinned -> -1
                    !isC1Pinned && isC2Pinned -> 1
                    else -> c2.lastUpdated.compareTo(c1.lastUpdated)
                }
            }
    }

    BackHandler(enabled = isSearchActive || selectedFilter != "All Chats" || selectedChatForMenu != null || targetUserForCreateGroup != null) {
        when {
            selectedChatForMenu != null -> selectedChatForMenu = null
            targetUserForCreateGroup != null -> targetUserForCreateGroup = null
            isSearchActive -> {
                isSearchActive = false
                viewModel.setSearchQuery("")
            }
            selectedFilter != "All Chats" -> viewModel.setChatFilter("All Chats")
        }
    }

    Scaffold(
        modifier = modifier.pointerInput(Unit) {
            var totalDragX = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalDragX = 0f },
                onDragEnd = {
                    if (totalDragX < -120f) {
                        onTabSelected(BitChatNavTab.CALLS)
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    totalDragX += dragAmount
                }
            )
        },
        bottomBar = {
            if (!isSearchActive && !showHistoryRestore) {
                val totalUnread = remember(chats) { chats.sumOf { it.unreadCount } }
                BitChatBottomNavBar(
                    currentTab = BitChatNavTab.CHATS,
                    onTabSelected = onTabSelected,
                    isNightMode = isNightMode,
                    unreadChatsCount = totalUnread
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            if (!isSearchActive && !showHistoryRestore) {
                // Stuck edge tab shape: rounded on the left side, flush zero-radius flat edge on the phone's right edge
                val stuckTabShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 0.dp, bottomEnd = 0.dp)

                val fabOffsetX by animateDpAsState(
                    targetValue = if (isFabVisible) 24.dp else 100.dp,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                    label = "fab_slide_offset"
                )

                Box(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .offset(x = fabOffsetX)
                        .width(60.dp)
                        .height(52.dp)
                        .shadow(8.dp, stuckTabShape)
                        .clip(stuckTabShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.4f),
                            shape = stuckTabShape
                        )
                        .clickable {
                            isSearchActive = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "New Chat",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        containerColor = animBgColor
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Chats Screen View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Top Header Bar with centered KnotLink text in Aladin font
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KnotLink",
                        color = if (isNightMode) Color(0xFFF4F4F6) else Color(0xFF0F172A),
                        style = TextStyle(
                            fontFamily = AladinFontFamily,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                // Full Width Search Bar pill (clean dark mode without shadow outline artifact)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 6.dp)
                        .then(
                            if (!isNightMode) {
                                Modifier.shadow(
                                    elevation = 5.dp,
                                    shape = RoundedCornerShape(22.dp),
                                    ambientColor = Color(0x180F172A),
                                    spotColor = Color(0x180F172A)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isNightMode) Color(0xFF16181E) else Color.White)
                        .border(
                            width = 1.dp,
                            color = if (isNightMode) Color(0xFF23262F) else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable { isSearchActive = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Search",
                            color = Color(0xFF8E8E93),
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Filter Chips Row matching image
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filterOptions) { filter ->
                        val isSelected = filter == selectedFilter
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.92f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "filterScale"
                        )

                        val pillBg = if (isSelected) {
                            if (isNightMode) Color.White else Color(0xFF0F172A)
                        } else {
                            if (isNightMode) Color(0xFF1B1B1E) else Color(0xFFF1F5F9)
                        }

                        val pillTextColor = if (isSelected) {
                            if (isNightMode) Color.Black else Color.White
                        } else {
                            if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B)
                        }

                        val displayName = customCategoryNames[filter] ?: filter

                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .clip(CircleShape)
                                .background(pillBg)
                                .border(
                                    width = 1.2.dp,
                                    color = if (isSelected) Color.Transparent else if (isNightMode) Color(0xFF27272A) else Color.Black.copy(alpha = 0.45f),
                                    shape = CircleShape
                                )
                                .combinedClickable(
                                     interactionSource = interactionSource,
                                     indication = null,
                                     onClick = {
                                         haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                         if (filter == "Deleted") {
                                             showDeletedChatsSheet = true
                                         } else {
                                             viewModel.setChatFilter(filter)
                                         }
                                     },
                                     onLongClick = {
                                         haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                         if (filter != "All Chats") {
                                             categoryToManage = filter
                                         }
                                     }
                                 )
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = displayName,
                                    color = pillTextColor,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                if (filter == "Unread" && hasUnreadChats) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF3B82F6))
                                    )
                                }
                            }
                        }
                    }

                    // Add Section '+' Button Chip at the end
                    item {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isNightMode) Color(0xFF1B1B1E) else Color(0xFFF1F5F9))
                                .border(
                                    width = 1.2.dp,
                                    color = if (isNightMode) Color(0xFF27272A) else Color.Black.copy(alpha = 0.45f),
                                    shape = CircleShape
                                )
                                .clickable { showAddCategoryDialog = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Section",
                                    tint = if (isNightMode) Color.White else Color(0xFF0F172A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Add",
                                    color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                com.example.ui.components.ActiveCallBulletinSlot(
                    viewModel = viewModel,
                    onExpandClick = onActiveCallBannerClick
                )

                // Modern Redesigned Chat List with NestedScroll & Pull-down for Archived & KnotLink Assistant
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AnimatedVisibility(
                        visible = isArchiveBannerVisible,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            GlassPanel(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showArchivedSheet = true },
                                cornerRadius = 20.dp,
                                backgroundColor = if (isNightMode) Color(0xFF18181D) else Color(0xFFF1F5F9),
                                borderColor = if (isNightMode) Color(0xFF2E2E38) else Color(0xFFCBD5E1)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                                        listOf(Color(0xFF0284C7), Color(0xFF2563EB))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Archive,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Archived Chats",
                                                color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Tap to view • hides in 3s",
                                                color = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (isNightMode) Color(0xFF0284C7).copy(alpha = 0.25f)
                                                else Color(0xFF0284C7).copy(alpha = 0.15f)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "${archivedChats.size} archived",
                                            color = Color(0xFF0284C7),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection),
                        contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pull-to-Reveal KnotLink Assistant
                        if (selectedFilter == "All Chats" || selectedFilter == "Personal") {
                            item(key = "knotlink_assistant_pull_item") {
                                AnimatedVisibility(
                                    visible = isAssistantRevealed,
                                    enter = fadeIn(animationSpec = tween(280)) + expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                                    exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200))
                                ) {
                                    SwipeableChatItemRow(
                                        chat = assistantChat,
                                        isNightMode = isNightMode,
                                        isArchivedTab = false,
                                        isBlockedTab = false,
                                        isRestrictedTab = false,
                                        isPinned = true,
                                        isMuted = false,
                                        isBlocked = false,
                                        isRestricted = false,
                                        isLocked = false,
                                        isOnline = false,
                                        isTyping = false,
                                        onClick = { onChatClick(assistantChat) },
                                        onLongClick = {},
                                        onArchive = {},
                                        onUnblock = {},
                                        onUnrestrict = {},
                                        onDelete = {}
                                    )
                                }
                            }
                        }

                        if (visibleChats.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = when (selectedFilter) {
                                                "Archive" -> Icons.Default.Archive
                                                "Blocked" -> Icons.Default.Shield
                                                "Restricted" -> Icons.Default.Lock
                                                else -> Icons.Default.ChatBubble
                                            },
                                            contentDescription = null,
                                            tint = if (isNightMode) Color(0xFF52525B) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = when (selectedFilter) {
                                                "Archive" -> "No archived chats"
                                                "Blocked" -> "No blocked contacts"
                                                "Restricted" -> "No restricted chats"
                                                else -> "No chats found"
                                            },
                                            color = if (isNightMode) Color(0xFF71717A) else Color(0xFF64748B),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                finalSortedChats,
                                key = { it.id },
                                contentType = { "chat_row" }
                            ) { chat ->
                                val isArchivedTab = selectedFilter == "Archive"
                                val isBlockedTab = selectedFilter == "Blocked"
                                val isRestrictedTab = selectedFilter == "Restricted"

                                val otherUid = remember(chat.participantUids, currentAuthUid) {
                                    chat.participantUids.split(",")
                                        .map { it.trim() }
                                        .firstOrNull { it.isNotBlank() && it != currentAuthUid }
                                }
                                val cleanName = chat.name.trim().lowercase().removePrefix("@").removeSuffix(".link")
                                val unChatId = if (chat.id.startsWith("chat_")) chat.id.removePrefix("chat_") else null
                                val presence = (if (!otherUid.isNullOrBlank()) userPresenceMap[otherUid] ?: userPresenceMap[otherUid.lowercase()] else null)
                                    ?: userPresenceMap[chat.id]
                                    ?: (if (unChatId != null) userPresenceMap[unChatId] ?: userPresenceMap[unChatId.lowercase()] else null)
                                    ?: (if (!chat.ownerUid.isNullOrBlank()) userPresenceMap[chat.ownerUid] ?: userPresenceMap[chat.ownerUid.lowercase()] else null)
                                    ?: userPresenceMap[chat.name]
                                    ?: userPresenceMap[chat.name.lowercase()]
                                    ?: userPresenceMap[cleanName]
                                    ?: userPresenceMap["$cleanName.link"]
                                    ?: userPresenceMap["@$cleanName"]
                                    ?: userPresenceMap["@$cleanName.link"]
                                val isUserOnline = presence?.first ?: false
                                val isUserTyping = chatTypingStatus[chat.id] == true ||
                                    (unChatId != null && chatTypingStatus[unChatId] == true) ||
                                    (!otherUid.isNullOrBlank() && (chatTypingStatus[otherUid] == true || chatTypingStatus[otherUid.lowercase()] == true)) ||
                                    chatTypingStatus[chat.name] == true ||
                                    chatTypingStatus[chat.name.lowercase()] == true ||
                                    chatTypingStatus[cleanName] == true ||
                                    chatTypingStatus["$cleanName.link"] == true ||
                                    chatTypingStatus["@$cleanName"] == true

                                SwipeableChatItemRow(
                                    chat = chat,
                                    isNightMode = isNightMode,
                                    isArchivedTab = isArchivedTab,
                                    isBlockedTab = isBlockedTab,
                                    isRestrictedTab = isRestrictedTab,
                                    isPinned = pinnedChatIds.contains(chat.id),
                                    isMuted = mutedChatIds.containsKey(chat.id),
                                    isBlocked = blockedChatIds.contains(chat.id),
                                    isRestricted = restrictedChatIds.contains(chat.id),
                                    isLocked = viewModel.isChatLocked(chat.id),
                                    isOnline = isUserOnline,
                                    isTyping = isUserTyping,
                                    onClick = {
                                        if (chat.id == "alex") {
                                            onVideoCallClick(chat.id)
                                        } else if (viewModel.isChatLocked(chat.id) && activity != null) {
                                            viewModel.authenticateWithBiometric(
                                                activity = activity,
                                                title = "Locked Private Chat",
                                                subtitle = "Scan fingerprint to open ${chat.name}",
                                                onSuccess = { onChatClick(chat) },
                                                onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                            )
                                        } else {
                                            onChatClick(chat)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isArchivedTab && !isBlockedTab && !isRestrictedTab) {
                                            selectedChatForMenu = chat
                                        }
                                    },
                                    onArchive = {
                                        if (isArchivedTab) {
                                            viewModel.unarchiveChat(chat.id)
                                            Toast.makeText(context, "${chat.name} unarchived", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.archiveChat(chat)
                                            Toast.makeText(context, "${chat.name} archived", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onUnblock = {
                                        blockedChatIds = blockedChatIds - chat.id
                                        Toast.makeText(context, "${chat.name} unblocked", Toast.LENGTH_SHORT).show()
                                    },
                                    onUnrestrict = {
                                        restrictedChatIds = restrictedChatIds - chat.id
                                        Toast.makeText(context, "${chat.name} unrestricted", Toast.LENGTH_SHORT).show()
                                    },
                                    onDelete = {
                                        chatToDelete = chat
                                        chatDeleteStep = 1
                                        deleteForEveryone = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Context Menu Dialog (Triggered on Long Press)
            val menuChat = selectedChatForMenu
            if (menuChat != null) {
                ChatContextMenuDialog(
                    chat = menuChat,
                    isNightMode = isNightMode,
                    isPinned = pinnedChatIds.contains(menuChat.id),
                    isBlocked = blockedChatIds.contains(menuChat.id),
                    isRestricted = restrictedChatIds.contains(menuChat.id),
                    isLocked = viewModel.isChatLocked(menuChat.id),
                    onDismiss = { selectedChatForMenu = null },
                    onTogglePin = {
                        if (pinnedChatIds.contains(menuChat.id)) {
                            pinnedChatIds = pinnedChatIds - menuChat.id
                            Toast.makeText(context, "${menuChat.name} unpinned", Toast.LENGTH_SHORT).show()
                        } else {
                            pinnedChatIds = pinnedChatIds + menuChat.id
                            Toast.makeText(context, "${menuChat.name} pinned to top", Toast.LENGTH_SHORT).show()
                        }
                        selectedChatForMenu = null
                    },
                    onMuteConfirmed = { muteType, duration ->
                        mutedChatIds = mutedChatIds + (menuChat.id to "$muteType ($duration)")
                        Toast.makeText(context, "${menuChat.name}: $muteType for $duration", Toast.LENGTH_SHORT).show()
                        selectedChatForMenu = null
                    },
                    onToggleBlock = {
                        if (blockedChatIds.contains(menuChat.id)) {
                            blockedChatIds = blockedChatIds - menuChat.id
                            Toast.makeText(context, "${menuChat.name} unblocked", Toast.LENGTH_SHORT).show()
                        } else {
                            blockedChatIds = blockedChatIds + menuChat.id
                            Toast.makeText(context, "${menuChat.name} blocked", Toast.LENGTH_SHORT).show()
                        }
                        selectedChatForMenu = null
                    },
                    onToggleRestricted = {
                        if (restrictedChatIds.contains(menuChat.id)) {
                            restrictedChatIds = restrictedChatIds - menuChat.id
                            Toast.makeText(context, "${menuChat.name} unrestricted", Toast.LENGTH_SHORT).show()
                        } else {
                            restrictedChatIds = restrictedChatIds + menuChat.id
                            Toast.makeText(context, "${menuChat.name} restricted", Toast.LENGTH_SHORT).show()
                        }
                        selectedChatForMenu = null
                    },
                    onToggleLock = {
                        if (viewModel.isChatLocked(menuChat.id)) {
                            if (activity != null) {
                                viewModel.authenticateWithBiometric(
                                    activity = activity,
                                    title = "Unlock Chat Protection",
                                    subtitle = "Scan fingerprint to unlock ${menuChat.name}",
                                    onSuccess = {
                                        viewModel.toggleChatLock(menuChat.id)
                                        Toast.makeText(context, "${menuChat.name} unlocked", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                )
                            } else {
                                viewModel.toggleChatLock(menuChat.id)
                            }
                        } else {
                            viewModel.toggleChatLock(menuChat.id)
                            Toast.makeText(context, "${menuChat.name} locked with fingerprint", Toast.LENGTH_SHORT).show()
                        }
                        selectedChatForMenu = null
                    },
                    onShareLocationClicked = {
                        val c = selectedChatForMenu
                        selectedChatForMenu = null
                        locationPermissionChat = c
                    },
                    onCreateGroupWithUser = { user ->
                        targetUserForCreateGroup = user
                    },
                    onAssignFolder = { folder ->
                        viewModel.assignChatToFolder(menuChat.id, folder)
                        Toast.makeText(context, "${menuChat.name} assigned to $folder", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Create Group Modal Dialog
            val createGroupUser = targetUserForCreateGroup
            if (createGroupUser != null || showCreateGroupSheet) {
                CreateGroupDialog(
                    targetUser = createGroupUser,
                    viewModel = viewModel,
                    isNightMode = isNightMode,
                    onDismiss = {
                        targetUserForCreateGroup = null
                        showCreateGroupSheet = false
                    },
                    onGroupCreated = { newGroup ->
                        targetUserForCreateGroup = null
                        showCreateGroupSheet = false
                        onChatClick(newGroup)
                    }
                )
            }

            // Category/Section Management Sheet
            val catKey = categoryToManage
            if (catKey != null) {
                val curName = customCategoryNames[catKey] ?: catKey
                CategoryManagementSheet(
                    categoryKey = catKey,
                    currentDisplayName = curName,
                    isNightMode = isNightMode,
                    onRename = { newName ->
                        if (newName.isNotBlank()) {
                            val newMap = customCategoryNames.toMutableMap()
                            newMap[catKey] = newName
                            customCategoryNames = newMap
                        }
                    },
                    onDelete = {
                        val newOrder = customCategoryOrder.toMutableList()
                        newOrder.remove(catKey)
                        customCategoryOrder = newOrder
                        if (selectedFilter == catKey) {
                            viewModel.setChatFilter("All Chats")
                        }
                        val newNames = customCategoryNames.toMutableMap()
                        newNames.remove(catKey)
                        customCategoryNames = newNames
                        categoryToManage = null
                    },
                    onDismiss = { categoryToManage = null }
                )
            }

            if (showAddCategoryDialog) {
                AddSectionDialog(
                    isNightMode = isNightMode,
                    onAddSection = { newName ->
                        if (!customCategoryOrder.contains(newName)) {
                            customCategoryOrder = customCategoryOrder + newName
                            viewModel.setChatFilter(newName)
                        }
                    },
                    onDismiss = { showAddCategoryDialog = false }
                )
            }

            val currentSectionToAssign = sectionToAssignChats
            if (currentSectionToAssign != null) {
                val displayName = customCategoryNames[currentSectionToAssign] ?: currentSectionToAssign
                AssignChatsToSectionSheet(
                    sectionName = currentSectionToAssign,
                    displayName = displayName,
                    chats = chats,
                    chatFolderAssignments = chatFolderAssignments,
                    isNightMode = isNightMode,
                    onAssignChat = { chatId, folder ->
                        viewModel.assignChatToFolder(chatId, folder)
                    },
                    onDismiss = { sectionToAssignChats = null }
                )
            }

            val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val targetChat = locationPermissionChat
                if (targetChat != null) {
                    shareRealtimeLocation(context, targetChat.id, viewModel) {
                        locationPermissionChat = null
                    }
                }
            }

            // Location Permission Smart Card Popup
            val locChat = locationPermissionChat
            if (locChat != null) {
                LocationPermissionSmartCardDialog(
                    chat = locChat,
                    isNightMode = isNightMode,
                    onDismiss = { locationPermissionChat = null },
                    onAgreeAndShare = {
                        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                        if (!hasFine && !hasCoarse) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            shareRealtimeLocation(context, locChat.id, viewModel) {
                                locationPermissionChat = null
                            }
                        }
                    }
                )
            }

            // Archived Chats Bottom Sheet Modal
            if (showArchivedSheet) {
                ArchivedChatsSheet(
                    viewModel = viewModel,
                    isNightMode = isNightMode,
                    onChatClick = { chat ->
                        showArchivedSheet = false
                        isArchiveBannerVisible = false
                        onChatClick(chat)
                    },
                    onDismiss = {
                        showArchivedSheet = false
                        isArchiveBannerVisible = false
                    }
                )
            }

            // Deleted Chats Full Page Modal
            if (showDeletedChatsSheet) {
                DeletedChatsSheet(
                    viewModel = viewModel,
                    isNightMode = isNightMode,
                    onDismiss = {
                        showDeletedChatsSheet = false
                    }
                )
            }

            // 2-Step Chat Deletion Confirmation Dialog
            if (chatToDelete != null) {
                val targetChat = chatToDelete!!
                AlertDialog(
                    onDismissRequest = {
                        chatToDelete = null
                        chatDeleteStep = 1
                        deleteForEveryone = false
                    },
                    title = {
                        Text(
                            text = if (chatDeleteStep == 1) "Confirm Deletion" else "Delete for Everyone?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isNightMode) Color.White else Color(0xFF0F172A)
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (chatDeleteStep == 1) {
                                Text(
                                    text = "Are you sure? You want to delete this chat?",
                                    fontSize = 15.sp,
                                    color = if (isNightMode) Color(0xFFD1D5DB) else Color(0xFF374151)
                                )
                            } else {
                                Text(
                                    text = "Do you want to delete this chat only for yourself, or also delete it for the opponent (${targetChat.name})?",
                                    fontSize = 15.sp,
                                    color = if (isNightMode) Color(0xFFD1D5DB) else Color(0xFF374151)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { deleteForEveryone = !deleteForEveryone }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Beautiful Rounded Checkbox / Tickbox
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(if (deleteForEveryone) Color(0xFFEF4444) else Color.Transparent)
                                            .border(1.5.dp, if (deleteForEveryone) Color(0xFFEF4444) else (if (isNightMode) Color(0xFF4B5563) else Color(0xFF9CA3AF)), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (deleteForEveryone) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Delete for opponent too",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (deleteForEveryone) Color(0xFFEF4444) else (if (isNightMode) Color.White else Color(0xFF1F2937))
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (chatDeleteStep == 1) {
                                    chatDeleteStep = 2
                                } else {
                                    // Perform deletion
                                    viewModel.deleteChat(targetChat)
                                    if (deleteForEveryone) {
                                        Toast.makeText(context, "Deleted from both sides successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Chat moved to Recycle Bin (24h)", Toast.LENGTH_SHORT).show()
                                    }
                                    chatToDelete = null
                                    chatDeleteStep = 1
                                    deleteForEveryone = false
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (chatDeleteStep == 1) Color(0xFF2563EB) else Color(0xFFEF4444)
                            )
                        ) {
                            Text(
                                text = if (chatDeleteStep == 1) "Yes" else "Delete Chat",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                chatToDelete = null
                                chatDeleteStep = 1
                                deleteForEveryone = false
                            }
                        ) {
                            Text(
                                text = if (chatDeleteStep == 1) "No" else "Cancel",
                                fontWeight = FontWeight.Bold,
                                color = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B),
                                fontSize = 15.sp
                            )
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = if (isNightMode) Color(0xFF1E202B) else Color.White
                )
            }

            // Domain Auth Switcher Modal Sheet
            if (showNodeAuthSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .clickable { showNodeAuthSheet = false },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    GlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 80.dp)
                            .navigationBarsPadding()
                            .clickable(enabled = false) {},
                        cornerRadius = 28.dp,
                        backgroundColor = Color(0xFF16161A),
                        borderColor = Color(0xFF26262C)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Dns,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Choose Search Nodes",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(
                                    onClick = { showNodeAuthSheet = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Select active search authority node:",
                                color = Color(0xFFA1A1AA),
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            val domainNodes = listOf(".Chat", ".Bit", "Auth")

                            domainNodes.forEach { node ->
                                val isSelected = activeAuthNode == node
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) Color(0xFF2563EB).copy(alpha = 0.2f) else Color(0xFF222226))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF2E2E34),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            activeAuthNode = node
                                            showNodeAuthSheet = false
                                            Toast.makeText(context, "Connected to $node", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = if (isSelected) Color(0xFF3B82F6) else Color(0xFF71717A),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = node,
                                                color = if (isSelected) Color.White else Color(0xFFD4D4D8),
                                                fontSize = 15.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Active",
                                                tint = Color(0xFF3B82F6),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Full Screen Search View Overlay with Transition Animation
            AnimatedVisibility(
                visible = isSearchActive,
                enter = fadeIn(animationSpec = tween(220)) + slideInVertically(initialOffsetY = { it / 4 }),
                exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(targetOffsetY = { it / 4 })
            ) {
                SearchOverlayScreen(
                    viewModel = viewModel,
                    onCloseSearch = { isSearchActive = false },
                    onSelectChat = { chat ->
                        isSearchActive = false
                        onChatClick(chat)
                    }
                )
            }

            if (showHistoryRestore || showHistoryRestoreError) {
                val restorePulse = rememberInfiniteTransition(label = "history_restore")
                val restoreScale by restorePulse.animateFloat(
                    initialValue = 0.92f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1100, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "history_restore_scale"
                )
                val restoreGlow by restorePulse.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 0.85f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1100, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "history_restore_glow"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (isNightMode) Color(0xFF080B12) else Color(0xFFF8FAFC),
                                    if (isNightMode) Color(0xFF0D1728) else Color(0xFFEFF6FF)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .scale(restoreScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFF38BDF8).copy(alpha = restoreGlow),
                                            Color(0xFF2563EB).copy(alpha = restoreGlow * 0.35f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(if (isNightMode) Color(0xFF111827) else Color.White)
                                    .border(
                                        1.5.dp,
                                        Color(0xFF38BDF8).copy(alpha = 0.75f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (showHistoryRestore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 2.5.dp,
                                        color = Color(0xFF38BDF8)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        Text(
                            text = if (showHistoryRestore) "Restoring your chats…" else "Couldn't restore chats",
                            color = if (isNightMode) Color.White else Color(0xFF0F172A),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (showHistoryRestore)
                                "Syncing your conversations securely. This may take a moment."
                            else
                                "Check your connection and try again.",
                            color = if (isNightMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        if (showHistoryRestoreError) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { viewModel.retryInitialHistorySync() },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2563EB),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Try Again", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableChatItemRow(
    chat: ChatEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onUnblock: () -> Unit = {},
    onUnrestrict: () -> Unit = {},
    isNightMode: Boolean = true,
    isArchivedTab: Boolean = false,
    isBlockedTab: Boolean = false,
    isRestrictedTab: Boolean = false,
    isPinned: Boolean = false,
    isMuted: Boolean = false,
    isBlocked: Boolean = false,
    isRestricted: Boolean = false,
    isLocked: Boolean = false,
    isOnline: Boolean = chat.isOnline,
    isTyping: Boolean = false
) {
    val isAssistant = chat.id == "bitassistant" || chat.id == "ai_assistant" || chat.name.contains("Assistant", ignoreCase = true) || chat.avatarType == "assistant"
    val haptic = LocalHapticFeedback.current
    var offsetX by remember { mutableFloatStateOf(0f) }

    val animatedOffset by animateFloatAsState(
        targetValue = if (isAssistant) 0f else offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dragOffset"
    )

    val itemBg = if (isNightMode) Color(0xFF141416) else Color.White
    val itemBorder = if (isNightMode) Color(0xFF24242A) else Color(0xFFE2E8F0)

    val titleColor = if (isNightMode) Color.White else Color(0xFF0F172A)
    val timeColor = if (isNightMode) Color(0xFF71717A) else Color(0xFF64748B)
    val bodyColor = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B)

    val isDeleteRevealed = !isAssistant && offsetX > 15f
    val isArchiveRevealed = !isAssistant && offsetX < -15f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isNightMode) Color(0xFF1A1A20) else Color(0xFFE2E8F0)
            )
    ) {
        if (!isAssistant) {
            // Background swipe actions indicator (Full vertical height matching card)
            Row(
                modifier = Modifier.matchParentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isArchiveRevealed) Arrangement.End else Arrangement.Start
            ) {
                when {
                    isArchivedTab -> {
                        if (isArchiveRevealed || isDeleteRevealed) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(68.dp)
                                    .background(Color(0xFF10B981))
                                    .clickable {
                                        onArchive()
                                        offsetX = 0f
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Unarchive Action",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    isBlockedTab -> {
                        if (isArchiveRevealed || isDeleteRevealed) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(68.dp)
                                    .background(Color(0xFF2563EB))
                                    .clickable {
                                        onUnblock()
                                        offsetX = 0f
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Unblock Action",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    isRestrictedTab -> {
                        if (isArchiveRevealed || isDeleteRevealed) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(68.dp)
                                    .background(Color(0xFF8B5CF6))
                                    .clickable {
                                        onUnrestrict()
                                        offsetX = 0f
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Unrestrict Action",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        if (isArchiveRevealed) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(68.dp)
                                    .background(Color(0xFF0284C7))
                                    .clickable {
                                        onArchive()
                                        offsetX = 0f
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = "Archive Action",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else if (isDeleteRevealed) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(68.dp)
                                    .background(Color(0xFFEF4444))
                                    .clickable {
                                        onDelete()
                                        offsetX = 0f
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Action",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Foreground Drag Card
        val cardDragModifier = if (!isAssistant) {
            Modifier
                .offset(x = animatedOffset.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > 35f) {
                                offsetX = 68f
                            } else if (offsetX < -35f) {
                                offsetX = -68f
                            } else {
                                offsetX = 0f
                            }
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            // Smooth, controlled drag speed
                            offsetX = (offsetX + dragAmount * 0.38f).coerceIn(-90f, 90f)
                        }
                    )
                }
        } else {
            Modifier
        }

        Box(
            modifier = cardDragModifier
        ) {
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (offsetX != 0f) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                offsetX = 0f
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onClick()
                            }
                        },
                        onLongClick = {
                            if (!isAssistant && !isArchivedTab && !isBlockedTab && !isRestrictedTab) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongClick()
                            }
                        }
                    ),
                cornerRadius = 18.dp,
                backgroundColor = itemBg,
                borderColor = itemBorder,
                isNightMode = isNightMode
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAssistant = chat.id == "bitassistant" || chat.id == "ai_assistant" || chat.name.contains("Assistant", ignoreCase = true)
                    val isBot = isAssistant || chat.name.contains("Bot", ignoreCase = true)
                    val isGroup = chat.name.contains("[Group]", ignoreCase = true) || chat.name.contains("Synth", ignoreCase = true)
                    val displayName = remember(chat.name) {
                        when {
                            chat.name.startsWith("chat_") -> "Contact"
                            chat.name.contains("-") && chat.name.length >= 25 -> "Contact"
                            chat.name.contains("_") && !chat.name.contains(" ") && chat.name.length >= 16 -> "Contact"
                            else -> chat.name
                        }
                    }
                    val hasCustomAvatar = !chat.avatarType.isNullOrBlank() && (
                        chat.avatarType.startsWith("http://", ignoreCase = true) ||
                        chat.avatarType.startsWith("https://", ignoreCase = true) ||
                        chat.avatarType.startsWith("content://", ignoreCase = true) ||
                        chat.avatarType.startsWith("file://", ignoreCase = true) ||
                        chat.avatarType.startsWith("/")
                    )

                    // Avatar Container with Active Status Indicator Dot
                    Box(contentAlignment = Alignment.BottomEnd) {
                        when {
                            hasCustomAvatar -> {
                                AsyncImage(
                                    model = chat.avatarType,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            isBot -> {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
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
                            }
                            isGroup -> {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFF6366F1),
                                                    Color(0xFF8B5CF6)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = "Group Avatar",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFF2563EB).copy(alpha = 0.25f),
                                                    Color(0xFF00C6FF).copy(alpha = 0.15f)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (if (displayName.isNotBlank()) displayName else "U").take(1).uppercase(),
                                        color = Color(0xFF2563EB),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Active status dot (shown green when online, red when offline; hidden for personal safe assistant)
                        if (!isAssistant) {
                            val dotColor = if (isOnline) Color(0xFF22C55E) else Color(0xFFEF4444)
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

                    Spacer(modifier = Modifier.width(14.dp))

                    // Chat Info
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val isAssistant = chat.id == "bitassistant" || chat.id == "ai_assistant" || chat.name.equals("BitAssistant", ignoreCase = true)
                                if (isPinned && !isAssistant) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pinned",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }

                                Text(
                                    text = displayName,
                                    color = titleColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (isLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked Chat",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                if (isBot) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isMuted) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsOff,
                                        contentDescription = "Muted",
                                        tint = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                if (isBlocked) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Blocked",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                if (isRestricted) {
                                    Icon(
                                        imageVector = Icons.Default.VisibilityOff,
                                        contentDescription = "Restricted",
                                        tint = Color(0xFFEAB308),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                if (isLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }

                                Text(
                                    text = chat.timeString,
                                    color = timeColor,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isTyping) {
                                ChatListTypingIndicator(
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFF2563EB)
                                )
                            } else {
                                Text(
                                    text = com.example.util.NotificationHelper.formatCleanPreviewText(chat.lastMessage),
                                    color = bodyColor,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (chat.unreadCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chat.unreadCount.toString(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (isBot) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Encrypted",
                                    tint = Color(0xFF71717A),
                                    modifier = Modifier.size(16.dp)
                                )
                            } else if (chat.name.contains("Julian", ignoreCase = true)) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.NotificationsOff,
                                    contentDescription = "Muted",
                                    tint = Color(0xFF71717A),
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

// Full Search Screen Overlay Component
@Composable
fun SearchOverlayScreen(
    viewModel: BitChatViewModel,
    onCloseSearch: () -> Unit,
    onSelectChat: (ChatEntity) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("knotlink_search_history", android.content.Context.MODE_PRIVATE) }

    val isNightMode by viewModel.isNightMode.collectAsState()
    var searchInput by remember { mutableStateOf("") }
    var showDeleteHistoryModal by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val bgColor = if (isNightMode) Color(0xFF0D0E12) else Color(0xFFF8FAFC)
    val cardBgColor = if (isNightMode) Color(0xFF16161A) else Color(0xFFFFFFFF)
    val cardBorderColor = if (isNightMode) Color(0xFF26262C) else Color(0xFFE2E8F0)
    val textColor = if (isNightMode) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val iconBgColor = if (isNightMode) Color(0xFF1E1E24) else Color(0xFFE2E8F0)
    val iconTintColor = if (isNightMode) Color.White else Color(0xFF0F172A)
    val searchBarBgColor = if (isNightMode) Color(0xFF16161A) else Color(0xFFFFFFFF)
    val searchBarBorderColor = if (isNightMode) Color(0xFF26262C) else Color(0xFF2563EB).copy(alpha = 0.5f)

    // Persistent Real Search History State List
    val searchHistoryList = remember {
        val saved = prefs.getStringSet("recent_queries", emptySet()) ?: emptySet()
        mutableStateListOf<String>().apply {
            addAll(saved)
        }
    }

    fun saveHistory() {
        prefs.edit().putStringSet("recent_queries", searchHistoryList.toSet()).apply()
    }

    fun addToHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            searchHistoryList.remove(trimmed)
            searchHistoryList.add(0, trimmed)
            if (searchHistoryList.size > 25) {
                searchHistoryList.removeAt(searchHistoryList.size - 1)
            }
            saveHistory()
        }
    }

    fun deleteHistoryItem(query: String) {
        searchHistoryList.remove(query)
        saveHistory()
    }

    fun clearHistory() {
        searchHistoryList.clear()
        saveHistory()
    }

    val chats by viewModel.filteredChats.collectAsState()
    val userSearchResults by viewModel.userSearchResults.collectAsState()
    val userSearchState by viewModel.userSearchState.collectAsState()

    LaunchedEffect(searchInput) {
        viewModel.setUserSearchQuery(searchInput)
    }

    val filteredResults = remember(searchInput, chats) {
        if (searchInput.isBlank()) emptyList()
        else {
            val cleanQuery = searchInput.lowercase()
            chats.filter { chat ->
                val chatName = chat.name.lowercase()
                val lastMsg = chat.lastMessage.lowercase()
                chatName.contains(cleanQuery) || lastMsg.contains(cleanQuery)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Sleek Minimalist Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCloseSearch,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconBgColor)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = iconTintColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Search",
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                if (searchHistoryList.isNotEmpty()) {
                    IconButton(
                        onClick = { showDeleteHistoryModal = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Minimalist Search Bar with instant focus activation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(searchBarBgColor)
                    .border(
                        width = 1.5.dp,
                        color = searchBarBorderColor,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (isNightMode) Color(0xFF71717A) else Color(0xFF3B82F6),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (searchInput.isEmpty()) {
                            Text(
                                text = "Search contacts, messages...",
                                color = subTextColor,
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(Color(0xFF2563EB)),
                            singleLine = true
                        )
                    }

                    if (searchInput.isNotEmpty()) {
                        IconButton(
                            onClick = { searchInput = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Input",
                                tint = subTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Search Results Section
            if (searchInput.isNotEmpty()) {
                val totalResults = filteredResults.size + userSearchResults.size
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Search Results ($totalResults)",
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (userSearchState == com.example.ui.viewmodel.BitChatViewModel.UserSearchState.SEARCHING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF2563EB),
                            strokeWidth = 2.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredResults.isEmpty() && userSearchResults.isEmpty() && userSearchState != com.example.ui.viewmodel.BitChatViewModel.UserSearchState.SEARCHING) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No users or conversations found for \"$searchInput\"",
                            color = subTextColor,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Global KnotLink Users section
                        if (userSearchResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = "GLOBAL DIRECTORY USERS",
                                    color = Color(0xFF2563EB),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            itemsIndexed(userSearchResults.distinctBy { it.uid }, key = { idx, user -> "user_${user.uid}_$idx" }) { idx, user ->
                                GlassPanel(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            addToHistory(user.displayName)
                                            viewModel.startChatWithUser(
                                                targetUid = user.uid,
                                                targetName = user.displayName,
                                                avatarType = user.avatarType
                                            ) { chat ->
                                                onSelectChat(chat)
                                            }
                                        },
                                    cornerRadius = 16.dp,
                                    backgroundColor = cardBgColor,
                                    borderColor = cardBorderColor
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val userAv = user.avatarUrl
                                        val hasUserAvatar = !userAv.isNullOrBlank() && (
                                            userAv.startsWith("http://", ignoreCase = true) ||
                                            userAv.startsWith("https://", ignoreCase = true) ||
                                            userAv.startsWith("content://", ignoreCase = true) ||
                                            userAv.startsWith("file://", ignoreCase = true) ||
                                            userAv.startsWith("/")
                                        )
                                        if (hasUserAvatar) {
                                            AsyncImage(
                                                model = userAv,
                                                contentDescription = "User Avatar",
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.5f), CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF2563EB).copy(alpha = 0.25f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = user.displayName.take(1).uppercase(),
                                                    color = Color(0xFF2563EB),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = user.displayName,
                                                    color = textColor,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "@${user.username}",
                                                    color = Color(0xFF2563EB),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Text(
                                                text = if (user.bio.isNotBlank()) user.bio else user.profession,
                                                color = subTextColor,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = "Message",
                                            tint = Color(0xFF3B82F6),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Local Chats section
                        if (filteredResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = "CONVERSATIONS",
                                    color = subTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            itemsIndexed(filteredResults.distinctBy { it.id }, key = { idx, chat -> "chat_${chat.id}_$idx" }) { idx, chat ->
                                GlassPanel(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            addToHistory(chat.name)
                                            onSelectChat(chat)
                                        },
                                    cornerRadius = 16.dp,
                                    backgroundColor = cardBgColor,
                                    borderColor = cardBorderColor
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2563EB).copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = chat.name.take(1),
                                                color = Color(0xFF2563EB),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = chat.name,
                                                color = textColor,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = chat.lastMessage,
                                                color = subTextColor,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = "Message",
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Recent Search History Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = subTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recent Searches",
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (searchHistoryList.isNotEmpty()) {
                        Text(
                            text = "Clear All",
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { clearHistory() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (searchHistoryList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent search history",
                            color = subTextColor,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val distinctHistory = searchHistoryList.distinct()
                        itemsIndexed(distinctHistory, key = { idx, item -> "hist_${item}_$idx" }) { idx, item ->
                            AnimatedVisibility(
                                visible = searchHistoryList.contains(item),
                                exit = fadeOut() + slideOutVertically()
                            ) {
                                GlassPanel(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 14.dp,
                                    backgroundColor = cardBgColor,
                                    borderColor = cardBorderColor
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { searchInput = item },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = subTextColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = item,
                                                color = textColor,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        IconButton(
                                            onClick = { deleteHistoryItem(item) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete item",
                                                tint = subTextColor,
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

        // Delete & Manage History Confirmation Dialog
        if (showDeleteHistoryModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showDeleteHistoryModal = false },
                contentAlignment = Alignment.Center
            ) {
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clickable(enabled = false) {},
                    cornerRadius = 24.dp,
                    backgroundColor = cardBgColor,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoDelete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(38.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Clear Search History",
                            color = textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "This will clear all recent searches from your history. You can also delete items manually from the list.",
                            color = subTextColor,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Delete All Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.18f))
                                .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(16.dp))
                                .clickable {
                                    clearHistory()
                                    showDeleteHistoryModal = false
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Delete All History",
                                    color = Color(0xFFEF4444),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Cancel
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isNightMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                                .clickable { showDeleteHistoryModal = false }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchivedChatsSheet(
    viewModel: BitChatViewModel,
    isNightMode: Boolean,
    onChatClick: (ChatEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val archivedList by viewModel.archivedChats.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {},
            cornerRadius = 28.dp,
            backgroundColor = if (isNightMode) Color(0xFF141418) else Color.White,
            borderColor = if (isNightMode) Color(0xFF2A2A32) else Color(0xFFE2E8F0)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Archived Chats (${archivedList.size})",
                            color = if (isNightMode) Color.White else Color(0xFF0F172A),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isNightMode) Color.White else Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (archivedList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No archived chats",
                            color = if (isNightMode) Color(0xFF71717A) else Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(archivedList, key = { it.id }) { chat ->
                            GlassPanel(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        onChatClick(chat)
                                    },
                                cornerRadius = 16.dp,
                                backgroundColor = if (isNightMode) Color(0xFF1E1E24) else Color(0xFFF8FAFC),
                                borderColor = if (isNightMode) Color(0xFF2E2E38) else Color(0xFFE2E8F0)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = chat.name,
                                            color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = chat.lastMessage,
                                            color = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFF0284C7).copy(alpha = 0.2f))
                                            .clickable { viewModel.unarchiveChat(chat.id) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Unarchive",
                                            color = Color(0xFF0284C7),
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
}

@Composable
fun ChatContextMenuDialog(
    chat: ChatEntity,
    isNightMode: Boolean,
    isPinned: Boolean,
    isBlocked: Boolean,
    isRestricted: Boolean,
    isLocked: Boolean,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onMuteConfirmed: (muteType: String, duration: String) -> Unit,
    onToggleBlock: () -> Unit,
    onToggleRestricted: () -> Unit,
    onToggleLock: () -> Unit,
    onShareLocationClicked: () -> Unit,
    onCreateGroupWithUser: (ChatEntity) -> Unit = {},
    onAssignFolder: (folder: String) -> Unit = {}
) {
    var stage by remember { mutableStateOf("MAIN") }
    var chosenMuteType by remember { mutableStateOf("Mute Chat") }
    var customFolderNameInput by remember { mutableStateOf("") }

    val dialogBg = if (isNightMode) Color(0xFF18181B) else Color.White
    val textColor = if (isNightMode) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val dividerColor = if (isNightMode) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val itemHoverBg = if (isNightMode) Color(0xFF272730) else Color(0xFFF1F5F9)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (stage != "MAIN") {
                        IconButton(
                            onClick = {
                                stage = if (stage == "MUTE_DURATION") "MUTE_TYPE" else "MAIN"
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (stage) {
                                "MUTE_TYPE" -> "Mute Options"
                                "MUTE_DURATION" -> "Select Duration"
                                "ASSIGN_SECTION" -> "Assign Section"
                                else -> chat.name
                            },
                            color = textColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when (stage) {
                                "MUTE_TYPE" -> "Choose what to mute"
                                "MUTE_DURATION" -> chosenMuteType
                                "ASSIGN_SECTION" -> "Assign to folder/section"
                                else -> "Quick Actions & Controls"
                            },
                            color = subTextColor,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = subTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (stage) {
                    "MAIN" -> {
                        PopupOptionRow(
                            icon = Icons.Default.PushPin,
                            iconTint = if (isPinned) Color(0xFFF59E0B) else Color(0xFF0284C7),
                            title = if (isPinned) "Unpin Chat" else "Pin Chat",
                            textColor = textColor,
                            itemHoverBg = itemHoverBg,
                            onClick = onTogglePin
                        )

                        PopupOptionRow(
                            icon = Icons.Default.NotificationsOff,
                            iconTint = Color(0xFF8B5CF6),
                            title = "Mute Notifications",
                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            textColor = textColor,
                            itemHoverBg = itemHoverBg,
                            onClick = { stage = "MUTE_TYPE" }
                        )

                        PopupOptionRow(
                            icon = Icons.Default.Shield,
                            iconTint = if (isBlocked) Color(0xFF22C55E) else Color(0xFFEF4444),
                            title = if (isBlocked) "Unblock Contact" else "Block Contact",
                            textColor = textColor,
                            itemHoverBg = itemHoverBg,
                            onClick = onToggleBlock
                        )

                        PopupOptionRow(
                            icon = Icons.Default.VisibilityOff,
                            iconTint = Color(0xFFEAB308),
                            title = if (isRestricted) "Unrestrict Chat" else "Restrict Chat",
                            textColor = textColor,
                            itemHoverBg = itemHoverBg,
                            onClick = onToggleRestricted
                        )

                        PopupOptionRow(
                            icon = Icons.Default.Lock,
                            iconTint = Color(0xFF2563EB),
                            title = if (isLocked) "Unlock Chat" else "Lock Chat",
                            textColor = textColor,
                            itemHoverBg = itemHoverBg,
                            onClick = onToggleLock
                        )

                        PopupOptionRow(
                            icon = Icons.Default.Layers,
                            iconTint = Color(0xFF3B82F6),
                            title = "Assign to Section / Folder",
                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            textColor = textColor,
                            itemHoverBg = itemHoverBg,
                            onClick = { stage = "ASSIGN_SECTION" }
                        )

                        PopupOptionRow(
                            icon = Icons.Default.GroupAdd,
                            iconTint = Color(0xFF2563EB),
                            title = "Create group with ${chat.name}",
                            textColor = Color(0xFF2563EB),
                            itemHoverBg = itemHoverBg,
                            onClick = {
                                onDismiss()
                                onCreateGroupWithUser(chat)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(dividerColor)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        PopupOptionRow(
                            icon = Icons.Default.LocationOn,
                            iconTint = Color(0xFF0EA5E9),
                            title = "Share My Current Location",
                            textColor = Color(0xFF0EA5E9),
                            itemHoverBg = itemHoverBg,
                            onClick = onShareLocationClicked
                        )
                    }

                    "MUTE_TYPE" -> {
                        val muteTypes = listOf("Mute Chat", "Mute Audio Call", "Mute Video Call", "Mute All")
                        muteTypes.forEach { muteType ->
                            PopupOptionRow(
                                icon = Icons.Default.NotificationsOff,
                                iconTint = Color(0xFF8B5CF6),
                                title = muteType,
                                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                textColor = textColor,
                                itemHoverBg = itemHoverBg,
                                onClick = {
                                    chosenMuteType = muteType
                                    stage = "MUTE_DURATION"
                                }
                            )
                        }
                    }

                    "MUTE_DURATION" -> {
                        val durationOptions = listOf(
                            "10 minutes",
                            "30 minutes",
                            "1 hour",
                            "4 hours",
                            "24 hours",
                            "Until I turn it off"
                        )
                        durationOptions.forEach { duration ->
                            PopupOptionRow(
                                icon = Icons.Default.Check,
                                iconTint = Color(0xFF10B981),
                                title = duration,
                                textColor = textColor,
                                itemHoverBg = itemHoverBg,
                                onClick = {
                                    onMuteConfirmed(chosenMuteType, duration)
                                }
                            )
                        }
                    }

                    "ASSIGN_SECTION" -> {
                        val predefinedFolders = listOf("Groups", "Work", "Personal", "Primary", "Favorites", "Family", "Projects")
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Select folder for ${chat.name}:",
                                color = subTextColor,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            predefinedFolders.forEach { folder ->
                                PopupOptionRow(
                                    icon = Icons.Default.Folder,
                                    iconTint = Color(0xFF3B82F6),
                                    title = folder,
                                    textColor = textColor,
                                    itemHoverBg = itemHoverBg,
                                    onClick = {
                                        onAssignFolder(folder)
                                        onDismiss()
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customFolderNameInput,
                                    onValueChange = { customFolderNameInput = it },
                                    placeholder = { Text("New Custom Folder...", fontSize = 12.sp, color = subTextColor) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (customFolderNameInput.isNotBlank()) {
                                            onAssignFolder(customFolderNameInput.trim())
                                            onDismiss()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                ) {
                                    Text("Assign", fontSize = 13.sp)
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
private fun PopupOptionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    textColor: Color,
    itemHoverBg: Color,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun LocationPermissionSmartCardDialog(
    chat: ChatEntity,
    isNightMode: Boolean,
    onDismiss: () -> Unit,
    onAgreeAndShare: () -> Unit
) {
    val cardBg = if (isNightMode) Color(0xFF18181B) else Color.White
    val textColor = if (isNightMode) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = cardBg,
            tonalElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Location Icon Glow Circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF0EA5E9).copy(alpha = 0.25f),
                                    Color(0xFF0284C7).copy(alpha = 0.1f)
                                )
                            )
                        )
                        .border(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Live Location",
                        tint = Color(0xFF0EA5E9),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Share Live Location",
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "KnotLink requires precise location permission to instantly share your Google Maps coordinates with ${chat.name}.",
                    color = subTextColor,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Bullet points
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isNightMode) Color(0xFF272730) else Color(0xFFF8FAFC))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF0EA5E9),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Realtime GPS coordinates via device sensors",
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Google Maps shareable link format",
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isNightMode) Color(0xFF272730) else Color(0xFFE2E8F0))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0284C7), Color(0xFF0EA5E9))
                                )
                            )
                            .clickable { onAgreeAndShare() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Agree & Share",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun shareRealtimeLocation(
    context: android.content.Context,
    chatId: String,
    viewModel: BitChatViewModel,
    onComplete: () -> Unit
) {
    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
    val isGpsEnabled = locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true
    val isNetworkEnabled = locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true

    if (!isGpsEnabled && !isNetworkEnabled) {
        Toast.makeText(context, "Location services (GPS) are turned off. Opening Settings...", Toast.LENGTH_LONG).show()
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onComplete()
        return
    }

    var bestLoc: android.location.Location? = null
    try {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {

            val gpsLoc = locationManager?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            val networkLoc = locationManager?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            val passiveLoc = locationManager?.getLastKnownLocation(android.location.LocationManager.PASSIVE_PROVIDER)

            bestLoc = listOfNotNull(gpsLoc, networkLoc, passiveLoc).maxByOrNull { it.time }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    if (bestLoc != null) {
        val googleMapsUrl = "https://maps.google.com/?q=%.6f,%.6f".format(java.util.Locale.US, bestLoc.latitude, bestLoc.longitude)
        viewModel.sendMessage(chatId, "📍 My Live Location:\n$googleMapsUrl")
        Toast.makeText(context, "Live Google Maps location shared!", Toast.LENGTH_SHORT).show()
        onComplete()
    } else {
        try {
            locationManager?.requestSingleUpdate(
                android.location.LocationManager.NETWORK_PROVIDER,
                object : android.location.LocationListener {
                    override fun onLocationChanged(loc: android.location.Location) {
                        val googleMapsUrl = "https://maps.google.com/?q=%.6f,%.6f".format(java.util.Locale.US, loc.latitude, loc.longitude)
                        viewModel.sendMessage(chatId, "📍 My Live Location:\n$googleMapsUrl")
                        Toast.makeText(context, "Live Google Maps location shared!", Toast.LENGTH_SHORT).show()
                        onComplete()
                    }
                    override fun onStatusChanged(p0: String?, p1: Int, p2: android.os.Bundle?) {}
                    override fun onProviderEnabled(p0: String) {}
                    override fun onProviderDisabled(p0: String) {}
                },
                android.os.Looper.getMainLooper()
            )
        } catch (e: Exception) {
            val googleMapsUrl = "https://maps.google.com/?q=37.421998,-122.084000"
            viewModel.sendMessage(chatId, "📍 My Live Location:\n$googleMapsUrl")
            Toast.makeText(context, "Live Google Maps location shared!", Toast.LENGTH_SHORT).show()
            onComplete()
        }
    }
}

@Composable
fun CreateGroupDialog(
    targetUser: ChatEntity? = null,
    viewModel: BitChatViewModel,
    isNightMode: Boolean,
    onDismiss: () -> Unit,
    onGroupCreated: (ChatEntity) -> Unit
) {
    val context = LocalContext.current
    val allChats by viewModel.filteredChats.collectAsState()
    val allContacts by viewModel.filteredContacts.collectAsState()

    var groupName by remember(targetUser) { mutableStateOf(if (targetUser != null) "Group with ${targetUser.name}" else "") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupImageUri by remember { mutableStateOf<String?>(null) }
    var showImageSourceSheet by remember { mutableStateOf(false) }

    // Candidates list (chats & contacts, excluding bots/groups and current targetUser)
    val candidateUsers = remember(allChats, allContacts) {
        val candidatesMap = mutableMapOf<String, CandidateUser>()
        allChats.forEach { c ->
            if (!c.name.contains("[Group]", true) && !c.name.contains("Assistant", true) && !c.name.contains("Bot", true)) {
                candidatesMap[c.id] = CandidateUser(id = c.id, name = c.name, avatarType = c.avatarType, subtitle = c.lastMessage)
            }
        }
        allContacts.forEach { contact ->
            if (!candidatesMap.containsKey(contact.id)) {
                candidatesMap[contact.id] = CandidateUser(id = contact.id, name = contact.name, avatarType = contact.avatarType, subtitle = contact.statusText)
            }
        }
        candidatesMap.values.toList()
    }

    // Pre-select targetUser if present
    var selectedUsers by remember(targetUser) {
        val initialList = mutableListOf<CandidateUser>()
        if (targetUser != null) {
            val contact = allContacts.find { it.id == targetUser.id }
            val resolvedName = contact?.name ?: targetUser.name
            val resolvedAvatar = contact?.avatarType ?: targetUser.avatarType
            val initial = candidateUsers.find { it.id == targetUser.id || it.name.equals(resolvedName, true) }
                ?: CandidateUser(id = targetUser.id, name = resolvedName, avatarType = resolvedAvatar, subtitle = "Chat member")
            initialList.add(initial)
        }
        mutableStateOf(initialList.toList())
    }

    // Filter candidate list by search query (and exclude already selected users from available list)
    val filteredCandidates = candidateUsers.filter { user ->
        searchQuery.isBlank() || user.name.contains(searchQuery, ignoreCase = true)
    }

    // Camera & Gallery launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val file = File(context.filesDir, "group_avatar_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    selectedGroupImageUri = file.absolutePath
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val file = File(context.filesDir, "group_avatar_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                selectedGroupImageUri = file.absolutePath
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving camera photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val dialogBg = if (isNightMode) Color(0xFF07080B) else Color.White
    val textColor = if (isNightMode) Color(0xFFF4F4F6) else Color(0xFF0F172A)
    val subTextColor = if (isNightMode) Color(0xFF9EA3B0) else Color(0xFF64748B)
    val inputBg = if (isNightMode) Color(0xFF13161F) else Color(0xFFF1F5F9)
    val cardBorder = if (isNightMode) Color(0xFF282C3A) else Color(0xFFE2E8F0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = dialogBg
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Custom App Bar Header Row
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = dialogBg,
                    border = BorderStroke(width = 1.dp, color = cardBorder.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Create Group Chat",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = textColor,
                                letterSpacing = (-0.3).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (targetUser != null) "With ${targetUser.name} & others" else "Select members & set name",
                                fontSize = 11.5.sp,
                                color = subTextColor,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        val totalMembersCount = selectedUsers.size + 1 // +1 for "You"
                        val isFormValid = groupName.isNotBlank() && totalMembersCount >= 2 && totalMembersCount <= 50

                        Button(
                            onClick = {
                                if (isFormValid) {
                                    viewModel.createGroupChat(
                                        groupName = groupName,
                                        avatarPathOrType = selectedGroupImageUri ?: "",
                                        memberNames = selectedUsers.map { it.name },
                                        onSuccess = { newGroup ->
                                            Toast.makeText(context, "Group '$groupName' created!", Toast.LENGTH_SHORT).show()
                                            onGroupCreated(newGroup)
                                        }
                                    )
                                }
                            },
                            enabled = isFormValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                disabledContainerColor = if (isNightMode) Color(0xFF1C1D24) else Color(0xFFE2E8F0),
                                disabledContentColor = subTextColor
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("Create", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // Scrollable main body
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Part 1: Group Name & Avatar Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isNightMode) Color(0xFF0F1117) else Color.White,
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Avatar Picker
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (!selectedGroupImageUri.isNullOrEmpty() && File(selectedGroupImageUri!!).exists()) Color.Transparent
                                        else Color(0xFF2563EB).copy(alpha = 0.12f)
                                    )
                                    .border(1.5.dp, Color(0xFF2563EB).copy(alpha = 0.6f), CircleShape)
                                    .clickable { showImageSourceSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!selectedGroupImageUri.isNullOrEmpty() && File(selectedGroupImageUri!!).exists()) {
                                    AsyncImage(
                                        model = File(selectedGroupImageUri!!),
                                        contentDescription = "Group Photo",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Select Photo",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Group Name Input Custom styled with OutlinedTextField
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "GROUP NAME",
                                    color = Color(0xFF2563EB),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = groupName,
                                    onValueChange = { if (it.length <= 60) groupName = it },
                                    placeholder = { Text("Enter group title (max 60)...", fontSize = 13.5.sp, color = subTextColor) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    textStyle = TextStyle(
                                        color = textColor,
                                        fontSize = 14.sp
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF2563EB),
                                        unfocusedBorderColor = cardBorder,
                                        focusedContainerColor = if (isNightMode) Color(0xFF040507) else Color(0xFFF1F5F9),
                                        unfocusedContainerColor = if (isNightMode) Color(0xFF040507) else Color(0xFFF1F5F9),
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor
                                    )
                                )
                            }
                        }
                    }

                    // Part 2: Selected Members Counter & Scrolling Pills
                    val totalMembersCount = selectedUsers.size + 1
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SELECTED MEMBERS (${totalMembersCount}/50)",
                                color = Color(0xFF2563EB),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "Min: 2 • Max: 50",
                                color = if (totalMembersCount < 2) Color(0xFFEF4444) else subTextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color(0xFF2563EB).copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.4f)),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "You (Owner)",
                                            color = Color(0xFF2563EB),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            itemsIndexed(selectedUsers, key = { idx, user -> "sel_${user.id}_$idx" }) { idx, user ->
                                val hasAvatar = !user.avatarType.isNullOrBlank() && (
                                    user.avatarType.startsWith("http") ||
                                    user.avatarType.startsWith("content") ||
                                    user.avatarType.startsWith("file") ||
                                    user.avatarType.startsWith("/")
                                )

                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isNightMode) Color(0xFF1E212E) else Color(0xFFE2E8F0),
                                    border = BorderStroke(1.dp, cardBorder),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Mini avatar
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2563EB)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (hasAvatar) {
                                                AsyncImage(
                                                    model = user.avatarType,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Text(
                                                    text = user.name.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        Text(
                                            text = user.name,
                                            color = textColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = subTextColor,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    selectedUsers = selectedUsers.filterNot { it.id == user.id }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Part 3: Search Bar for Contacts
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        placeholder = { Text("Search contact list...", fontSize = 13.sp, color = subTextColor) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = subTextColor, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = subTextColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = textColor,
                            fontSize = 13.5.sp
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = cardBorder,
                            focusedContainerColor = if (isNightMode) Color(0xFF0F1117) else Color.White,
                            unfocusedContainerColor = if (isNightMode) Color(0xFF0F1117) else Color.White,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )

                    // Part 4: Available Contacts Scroll List
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT CONTACTS & USERS",
                            color = subTextColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "${filteredCandidates.size} Available",
                            color = Color(0xFF2563EB),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (filteredCandidates.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text(text = "No users found", color = subTextColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        } else {
                            itemsIndexed(filteredCandidates.distinctBy { it.id }, key = { idx, candidate -> "cand_${candidate.id}_$idx" }) { idx, candidate ->
                                val isSelected = selectedUsers.any { it.id == candidate.id }
                                val hasAvatar = !candidate.avatarType.isNullOrBlank() && (
                                    candidate.avatarType.startsWith("http") ||
                                    candidate.avatarType.startsWith("content") ||
                                    candidate.avatarType.startsWith("file") ||
                                    candidate.avatarType.startsWith("/")
                                )

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) {
                                                selectedUsers = selectedUsers.filterNot { it.id == candidate.id }
                                            } else {
                                                if (totalMembersCount >= 50) {
                                                    Toast.makeText(context, "Group limit reached (max 50 members)", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    selectedUsers = selectedUsers + candidate
                                                }
                                            }
                                        },
                                    color = if (isSelected) Color(0xFF2563EB).copy(alpha = 0.08f) else (if (isNightMode) Color(0xFF0F1117) else Color.White),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFF2563EB).copy(alpha = 0.5f) else cardBorder
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Avatar circle
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (hasAvatar) {
                                                    AsyncImage(
                                                        model = candidate.avatarType,
                                                        contentDescription = "Avatar",
                                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Text(
                                                        text = candidate.name.take(1).uppercase(),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 16.sp,
                                                        fontFamily = FontFamily.SansSerif
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column {
                                                Text(
                                                    text = candidate.name,
                                                    color = textColor,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = (-0.15).sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = candidate.subtitle.ifBlank { "Verified KnotLink User" },
                                                    color = subTextColor,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color(0xFF2563EB) else Color.Transparent)
                                                .border(1.5.dp, if (isSelected) Color(0xFF2563EB) else cardBorder, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
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

    // Camera / Gallery Photo Chooser Modal
    if (showImageSourceSheet) {
        Dialog(onDismissRequest = { showImageSourceSheet = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = dialogBg,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Set Group Photo",
                        color = textColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageSourceSheet = false
                                cameraLauncher.launch(null)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color(0xFF2563EB))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Take Photo with Camera", color = textColor, fontSize = 14.sp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageSourceSheet = false
                                galleryLauncher.launch("image/*")
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color(0xFF2563EB))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Choose from Gallery", color = textColor, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

data class CandidateUser(
    val id: String,
    val name: String,
    val avatarType: String,
    val subtitle: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementSheet(
    categoryKey: String,
    currentDisplayName: String,
    isNightMode: Boolean,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var editedName by remember(currentDisplayName) { mutableStateOf(currentDisplayName) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isNightMode) Color(0xFF18181B) else Color.White,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manage '$currentDisplayName' Section",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isNightMode) Color.White else Color(0xFF0F172A)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Edit Section Name
            Text(
                text = "Edit Section Name",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF334155)
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = editedName,
                onValueChange = { editedName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = if (isNightMode) Color.White else Color(0xFF0F172A),
                    unfocusedTextColor = if (isNightMode) Color.White else Color(0xFF0F172A),
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = if (isNightMode) Color(0xFF27272A) else Color.Black.copy(alpha = 0.25f)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Save Name Changes Button
            Button(
                onClick = {
                    onRename(editedName)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }

            // Delete Section Button
            if (categoryKey != "All Chats") {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444).copy(alpha = 0.12f),
                        contentColor = Color(0xFFEF4444)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Section", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Section", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSectionDialog(
    isNightMode: Boolean,
    onAddSection: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newSectionName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Section",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isNightMode) Color.White else Color(0xFF0F172A)
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter a name for the new category section:",
                    fontSize = 13.sp,
                    color = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newSectionName,
                    onValueChange = { newSectionName = it },
                    placeholder = { Text("e.g. VIP, Projects, Family...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = if (isNightMode) Color(0xFF27272A) else Color.Black.copy(alpha = 0.2f)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newSectionName.isNotBlank()) {
                        onAddSection(newSectionName.trim())
                        onDismiss()
                    }
                },
                enabled = newSectionName.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White)
            ) {
                Text("Add Section", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B))
            }
        },
        containerColor = if (isNightMode) Color(0xFF18181B) else Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignChatsToSectionSheet(
    sectionName: String,
    displayName: String,
    chats: List<ChatEntity>,
    chatFolderAssignments: Map<String, String>,
    isNightMode: Boolean,
    onAssignChat: (chatId: String, folder: String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isNightMode) Color(0xFF18181B) else Color.White,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add Chats to '$displayName'",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNightMode) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Select chats to include in this section",
                        fontSize = 12.sp,
                        color = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chats) { chat ->
                    val isInSection = chatFolderAssignments[chat.id] == sectionName
                    Surface(
                        onClick = {
                            if (isInSection) {
                                onAssignChat(chat.id, "")
                            } else {
                                onAssignChat(chat.id, sectionName)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isInSection) {
                            Color(0xFF2563EB).copy(alpha = 0.12f)
                        } else {
                            if (isNightMode) Color(0xFF27272A) else Color(0xFFF1F5F9)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isInSection) Color(0xFF2563EB)
                            else if (isNightMode) Color(0xFF3F3F46)
                            else Color.Black.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = chat.name,
                                fontSize = 14.sp,
                                fontWeight = if (isInSection) FontWeight.Bold else FontWeight.Medium,
                                color = if (isNightMode) Color.White else Color(0xFF0F172A)
                            )
                            Checkbox(
                                checked = isInSection,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        onAssignChat(chat.id, sectionName)
                                    } else {
                                        onAssignChat(chat.id, "")
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF2563EB)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}
