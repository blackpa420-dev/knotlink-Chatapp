package com.example.ui.screens

import android.widget.Toast
import java.io.File
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.BitChatBottomNavBar
import com.example.ui.components.BitChatNavTab
import com.example.ui.components.GlassPanel
import com.example.ui.viewmodel.BitChatViewModel
import com.example.ui.viewmodel.CallLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class CallFilterItem(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val count: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    viewModel: BitChatViewModel,
    onStartVideoCallClick: (String) -> Unit,
    onStartAudioCallClick: (String) -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    onTabSelected: (BitChatNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val isNightMode by viewModel.isNightMode.collectAsState()
    val callLogs by viewModel.callLogs.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val allChats by viewModel.allChats.collectAsState()
    val user by viewModel.userIdentity.collectAsState()

    var selectedContactForHistory by remember { mutableStateOf<CallLog?>(null) }
    var selectedContactForOptions by remember { mutableStateOf<CallLog?>(null) }
    var showStartCallDialog by remember { mutableStateOf(false) }
    var selectedCallFilter by remember { mutableStateOf("ALL") }
    var isCallsViewed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isCallsViewed = true
    }

    // Delete Animation & Option States
    var isDeleteProcessing by remember { mutableStateOf(false) }
    var isDeleteDone by remember { mutableStateOf(false) }
    var showDeleteOptionsDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteAllDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteSelectedDialog by remember { mutableStateOf(false) }
    var isSelectiveDeleteMode by remember { mutableStateOf(false) }
    var selectedLogIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Call Link Share States
    var showShareLinkSheet by remember { mutableStateOf(false) }
    var showConfirmSendAllChatsDialog by remember { mutableStateOf(false) }

    val filteredCallLogs = remember(callLogs, selectedCallFilter) {
        when (selectedCallFilter) {
            "INCOMING" -> callLogs.filter { it.direction.equals("INCOMING", ignoreCase = true) }
            "OUTGOING" -> callLogs.filter { it.direction.equals("OUTGOING", ignoreCase = true) }
            "MISSED" -> callLogs.filter { it.direction.equals("MISSED", ignoreCase = true) }
            else -> callLogs
        }
    }

    // Voice note recording overlay state
    var isRecordingVoiceNote by remember { mutableStateOf(false) }
    var recordingContactName by remember { mutableStateOf("") }
    var recordingContactId by remember { mutableStateOf("") }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    val animBgColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFF0D0E12) else Color(0xFFF1F5F9),
        animationSpec = tween(400),
        label = "calls_bg"
    )

    val animTextColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFFF3F4F6) else Color(0xFF0F172A),
        animationSpec = tween(400),
        label = "calls_text"
    )

    val animSubTextColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
        animationSpec = tween(400),
        label = "calls_subtext"
    )

    Scaffold(
        modifier = modifier.pointerInput(Unit) {
            var totalDragX = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalDragX = 0f },
                onDragEnd = {
                    if (totalDragX < -120f) {
                        onTabSelected(BitChatNavTab.QR_SCAN)
                    } else if (totalDragX > 120f) {
                        onTabSelected(BitChatNavTab.CHATS)
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    totalDragX += dragAmount
                }
            )
        },
        bottomBar = {
            BitChatBottomNavBar(
                currentTab = BitChatNavTab.CALLS,
                onTabSelected = onTabSelected,
                isNightMode = isNightMode
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showStartCallDialog = true },
                containerColor = Color(0xFF2563EB),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneInTalk,
                    contentDescription = "New Call"
                )
            }
        },
        containerColor = animBgColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Modern Centered TopBar Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Left: User Avatar
                    Box(
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        val avatarPath = user?.avatarPath
                        val hasAvatar = !avatarPath.isNullOrBlank()
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable { onTabSelected(BitChatNavTab.SETTINGS) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasAvatar) {
                                val model: Any = if (avatarPath!!.startsWith("http://") || avatarPath.startsWith("https://") || avatarPath.startsWith("content://")) {
                                    avatarPath
                                } else {
                                    val clean = avatarPath.removePrefix("file://")
                                    val f = File(clean)
                                    if (f.exists()) f else avatarPath
                                }
                                AsyncImage(
                                    model = model,
                                    contentDescription = "User Profile Photo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val initials = remember(user?.fullName, user?.username) {
                                    val name = user?.fullName?.ifBlank { user?.username } ?: "User"
                                    if (name.length >= 2) name.take(2).uppercase() else name.uppercase()
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF2563EB), Color(0xFF8B5CF6))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials.ifBlank { "U" },
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Center: "Call Logs" Title (Repositioned slightly lower, subtitle removed)
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Call Logs",
                            color = animTextColor,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Right: Clear History Animated Action Button
                    if (callLogs.isNotEmpty()) {
                        val spinTransition = rememberInfiniteTransition(label = "spin_del")
                        val spinAngle by spinTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing)),
                            label = "spin_angle"
                        )

                        IconButton(
                            onClick = {
                                if (isSelectiveDeleteMode) {
                                    if (selectedLogIds.isNotEmpty()) {
                                        showConfirmDeleteSelectedDialog = true
                                    } else {
                                        Toast.makeText(context, "Select call logs to delete", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    showDeleteOptionsDialog = true
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            when {
                                isDeleteProcessing -> {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Deleting...",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.graphicsLayer { rotationZ = spinAngle }
                                    )
                                }
                                isDeleteDone -> {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Deleted",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                isSelectiveDeleteMode -> {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Delete Options",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4 Section Filter Row (All, In, Out, Miss) with Continuous Animated Icons & Counts
                val inCount = remember(callLogs) { callLogs.count { it.direction.equals("INCOMING", ignoreCase = true) } }
                val outCount = remember(callLogs) { callLogs.count { it.direction.equals("OUTGOING", ignoreCase = true) } }
                val missCount = remember(callLogs) { callLogs.count { it.direction.equals("MISSED", ignoreCase = true) } }

                // Capsule / Pill Filter Chips Row fitting full screen width without scrolling
                val filterSections = remember(callLogs) {
                    listOf(
                        CallFilterItem("ALL", "All", Icons.Default.Call, callLogs.size),
                        CallFilterItem("INCOMING", "In", Icons.AutoMirrored.Filled.CallReceived, inCount),
                        CallFilterItem("OUTGOING", "Out", Icons.AutoMirrored.Filled.CallMade, outCount),
                        CallFilterItem("MISSED", "Miss", Icons.AutoMirrored.Filled.CallMissed, missCount)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    filterSections.forEach { (filterId, label, icon, count) ->
                        val isSelected = selectedCallFilter == filterId

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

                        val pillIconColor = if (isSelected) {
                            if (isNightMode) Color.Black else Color.White
                        } else {
                            when (filterId) {
                                "INCOMING" -> Color(0xFF10B981)
                                "OUTGOING" -> Color(0xFF6366F1)
                                "MISSED" -> Color(0xFFEF4444)
                                else -> Color(0xFF2563EB)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(pillBg)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else if (isNightMode) Color(0xFF27272A) else Color.Black.copy(alpha = 0.12f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedCallFilter = filterId
                                }
                                .padding(vertical = 8.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = pillIconColor,
                                    modifier = Modifier.size(13.dp)
                                )

                                Spacer(modifier = Modifier.width(3.dp))

                                Text(
                                    text = label,
                                    color = pillTextColor,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )

                                if (count > 0 && filterId == "MISSED" && !isCallsViewed) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                            .padding(horizontal = 4.dp, vertical = 1.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$count",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isSelectiveDeleteMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(if (isNightMode) Color(0xFF222432) else Color(0xFFEFF6FF))
                            .border(
                                width = 1.dp,
                                color = if (isNightMode) Color(0xFF33364A) else Color(0xFFBFDBFE),
                                shape = RoundedCornerShape(25.dp)
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    isSelectiveDeleteMode = false
                                    selectedLogIds = emptySet()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Selective Mode", tint = animTextColor, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2563EB).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${selectedLogIds.size} Selected",
                                    color = Color(0xFF2563EB),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (selectedLogIds.size == filteredCallLogs.size) {
                                        selectedLogIds = emptySet()
                                    } else {
                                        selectedLogIds = filteredCallLogs.map { it.id }.toSet()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color(0xFF2563EB)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(17.dp)
                            ) {
                                Text(
                                    text = if (selectedLogIds.size == filteredCallLogs.size) "Deselect All" else "Select All",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (selectedLogIds.isNotEmpty()) {
                                Button(
                                    onClick = { showConfirmDeleteSelectedDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.height(34.dp),
                                    shape = RoundedCornerShape(17.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Delete (${selectedLogIds.size})",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content Area: Empty State OR Filtered Calls
                if (callLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            if (isNightMode) listOf(Color(0xFF1E202E), Color(0xFF151622))
                                            else listOf(Color(0xFFDBEAFE), Color(0xFFEFF6FF))
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        if (isNightMode) Color.White.copy(0.1f) else Color(0xFF2563EB).copy(0.2f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "No Calls",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "No Recent Calls",
                                color = animTextColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Your incoming, outgoing, and missed encrypted audio & video calls will appear here.",
                                color = animSubTextColor,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { showStartCallDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Start Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Start a New Call",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else if (filteredCallLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val filterTitle = when (selectedCallFilter) {
                                "INCOMING" -> "No Incoming Calls"
                                "OUTGOING" -> "No Outgoing Calls"
                                "MISSED" -> "No Missed Calls"
                                else -> "No Calls Found"
                            }
                            Text(
                                text = filterTitle,
                                color = animTextColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Calls matching this filter will appear here.",
                                color = animSubTextColor,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // If selective delete mode is ON, show ALL individual logs so user can check specific ones
                        // Otherwise group by contact
                        val displayLogs = if (isSelectiveDeleteMode) filteredCallLogs else filteredCallLogs.distinctBy { it.contactId.ifBlank { it.contactName } }

                        items(displayLogs, key = { it.id }, contentType = { "call_log" }) { call ->
                            val isSelected = selectedLogIds.contains(call.id)
                            CallCardItem(
                                callLog = call,
                                isNightMode = isNightMode,
                                animTextColor = animTextColor,
                                animSubTextColor = animSubTextColor,
                                isSelectiveDeleteMode = isSelectiveDeleteMode,
                                isSelected = isSelected,
                                onToggleSelect = {
                                    selectedLogIds = if (isSelected) selectedLogIds - call.id else selectedLogIds + call.id
                                },
                                onClick = { selectedContactForHistory = call },
                                onLongClick = { selectedContactForOptions = call },
                                onQuickCallClick = {
                                    if (call.callType == "VIDEO") {
                                        viewModel.startCall(call.contactId, call.contactName, "VIDEO")
                                        onStartVideoCallClick(call.contactId)
                                    } else {
                                        viewModel.startCall(call.contactId, call.contactName, "AUDIO")
                                        onStartAudioCallClick(call.contactId)
                                    }
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }

            // Voice Note Recording Floating Overlay
            AnimatedVisibility(
                visible = isRecordingVoiceNote,
                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.85f, animationSpec = spring()),
                exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.85f, animationSpec = spring()),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.70f)),
                    contentAlignment = Alignment.Center
                ) {
                    val waveTransition = rememberInfiniteTransition(label = "calls_rec_fullscreen")
                    
                    val ringRotation by waveTransition.animateFloat(
                        initialValue = 0f, targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                        label = "cring_rotation"
                    )

                    val ringPulse1 by waveTransition.animateFloat(
                        initialValue = 1f, targetValue = 1.6f,
                        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                        label = "crp1"
                    )
                    val ringAlpha1 by waveTransition.animateFloat(
                        initialValue = 0.55f, targetValue = 0f,
                        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                        label = "cra1"
                    )
                    val ringPulse2 by waveTransition.animateFloat(
                        initialValue = 1f, targetValue = 2.0f,
                        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                        label = "crp2"
                    )
                    val ringAlpha2 by waveTransition.animateFloat(
                        initialValue = 0.42f, targetValue = 0f,
                        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                        label = "cra2"
                    )

                    // Outer expanding pulsing waves
                    Box(
                        modifier = Modifier
                            .size(195.dp * ringPulse2)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = ringAlpha2))
                    )
                    Box(
                        modifier = Modifier
                            .size(195.dp * ringPulse1)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6).copy(alpha = ringAlpha1))
                    )

                    // Side equalizer waveform bars
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
                                    label = "cleft_$i"
                                )
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .height(barH.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFFEF4444), Color(0xFF8B5CF6))
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
                                    label = "cright_$i"
                                )
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .height(barH.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFFEF4444), Color(0xFF8B5CF6))
                                            )
                                        )
                                )
                            }
                        }
                    }

                    // Center Ring (No mic icon, Time at top, text below time)
                    Surface(
                        modifier = Modifier
                            .size(195.dp)
                            .graphicsLayer { rotationZ = ringRotation }
                            .shadow(28.dp, CircleShape, spotColor = Color(0xFFEF4444)),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(
                            4.dp,
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFFEF4444),
                                    Color(0xFF8B5CF6),
                                    Color(0xFF2563EB),
                                    Color(0xFF06B6D4),
                                    Color(0xFFEF4444)
                                )
                            )
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationZ = -ringRotation } // Keep text upright
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF31121C), Color(0xFF0F172A))
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
                                val mins = recordingSeconds / 60
                                val secs = recordingSeconds % 60
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
                                    label = "crecDotAlpha"
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

                                if (recordingContactName.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "To: $recordingContactName",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Call History Details Bottom Sheet (Last 30 Days)
    if (selectedContactForHistory != null) {
        val callItem = selectedContactForHistory!!
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L
        val userCallHistory = callLogs.filter {
            it.contactId == callItem.contactId && it.timestampMillis >= thirtyDaysAgo
        }

        ModalBottomSheet(
            onDismissRequest = { selectedContactForHistory = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = if (isNightMode) Color(0xFF161822) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = callItem.contactName.take(1).uppercase(),
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = callItem.contactName,
                                color = animTextColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Last 30 Days Call Details",
                                color = animSubTextColor,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = { selectedContactForHistory = null }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = animTextColor
                        )
                    }
                }

                // Quick Action Buttons (Audio Call, Video Call, Chat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            selectedContactForHistory = null
                            viewModel.startCall(callItem.contactId, callItem.contactName, "AUDIO")
                            onStartAudioCallClick(callItem.contactId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Audio Call", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            selectedContactForHistory = null
                            viewModel.startCall(callItem.contactId, callItem.contactName, "VIDEO")
                            onStartVideoCallClick(callItem.contactId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Video Call", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                var historyTabSection by remember { mutableIntStateOf(0) } // 0 = Audio, 1 = Video
                val audioHistoryLogs = userCallHistory.filter { it.callType.equals("AUDIO", ignoreCase = true) }
                val videoHistoryLogs = userCallHistory.filter { it.callType.equals("VIDEO", ignoreCase = true) }

                // Audio vs Video Section Segmented Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isNightMode) Color(0xFF222432) else Color(0xFFF1F5F9))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (historyTabSection == 0) Color(0xFF10B981) else Color.Transparent)
                            .clickable { historyTabSection = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Audio Calls",
                                tint = if (historyTabSection == 0) Color.White else Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Audio Calls (${audioHistoryLogs.size})",
                                color = if (historyTabSection == 0) Color.White else animTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (historyTabSection == 1) Color(0xFF2563EB) else Color.Transparent)
                            .clickable { historyTabSection = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Video Calls",
                                tint = if (historyTabSection == 1) Color.White else Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Video Calls (${videoHistoryLogs.size})",
                                color = if (historyTabSection == 1) Color.White else animTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                val activeHistoryLogs = if (historyTabSection == 0) audioHistoryLogs else videoHistoryLogs

                if (activeHistoryLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (historyTabSection == 0) "No audio call history." else "No video call history.",
                            color = animSubTextColor,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeHistoryLogs, key = { it.id }, contentType = { "call_history" }) { history ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isNightMode) Color(0xFF222432) else Color(0xFFF8FAFC))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (history.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                                        contentDescription = null,
                                        tint = if (history.callType == "VIDEO") Color(0xFF2563EB) else Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${history.direction.lowercase().replaceFirstChar { it.uppercase() }} ${history.callType.lowercase().replaceFirstChar { it.uppercase() }} Call",
                                            color = animTextColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = history.timeString,
                                            color = animSubTextColor,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Text(
                                    text = if (history.durationSeconds > 0) {
                                        val m = history.durationSeconds / 60
                                        val s = history.durationSeconds % 60
                                        String.format("%02d:%02d", m, s)
                                    } else "Missed / Ended",
                                    color = if (history.durationSeconds > 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Long Press 4 Options Modal (Chat, Audio Call, Video Call, Leave Voice Note)
    if (selectedContactForOptions != null) {
        val contactLog = selectedContactForOptions!!

        ModalBottomSheet(
            onDismissRequest = { selectedContactForOptions = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = if (isNightMode) Color(0xFF161822) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Actions for ${contactLog.contactName}",
                    color = animTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Option 1: Chat
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isNightMode) Color(0xFF222432) else Color(0xFFF1F5F9))
                        .clickable {
                            val cId = contactLog.contactId
                            val cName = contactLog.contactName
                            selectedContactForOptions = null
                            onNavigateToChat(cId, cName)
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Open Direct Chat", color = animTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Send text, photos & media", color = animSubTextColor, fontSize = 12.sp)
                    }
                }

                // Option 2: Audio Call
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isNightMode) Color(0xFF222432) else Color(0xFFF1F5F9))
                        .clickable {
                            val cId = contactLog.contactId
                            val cName = contactLog.contactName
                            selectedContactForOptions = null
                            viewModel.startCall(cId, cName, "AUDIO")
                            onStartAudioCallClick(cId)
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Start Audio Call", color = animTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Encrypted HD voice call", color = animSubTextColor, fontSize = 12.sp)
                    }
                }

                // Option 3: Video Call
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isNightMode) Color(0xFF222432) else Color(0xFFF1F5F9))
                        .clickable {
                            val cId = contactLog.contactId
                            val cName = contactLog.contactName
                            selectedContactForOptions = null
                            viewModel.startCall(cId, cName, "VIDEO")
                            onStartVideoCallClick(cId)
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Start Video Call", color = animTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("HD 1080p peer-to-peer video", color = animSubTextColor, fontSize = 12.sp)
                    }
                }

                // Option 4: Leave a Voice Note (Hold or Tap to Record)
                var recJob by remember { mutableStateOf<Job?>(null) }
                val isThisContactRecording = isRecordingVoiceNote && recordingContactId == contactLog.contactId

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isThisContactRecording)
                                Brush.horizontalGradient(listOf(Color(0xFFEF4444).copy(alpha = 0.25f), Color(0xFF2563EB).copy(alpha = 0.25f)))
                            else
                                Brush.horizontalGradient(listOf(Color(0xFFEF4444).copy(alpha = 0.12f), Color(0xFF8B5CF6).copy(alpha = 0.12f)))
                        )
                        .border(
                            if (isThisContactRecording) 2.dp else 1.dp,
                            if (isThisContactRecording) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        )
                        .pointerInput(contactLog.contactId) {
                            detectTapGestures(
                                onPress = {
                                    isRecordingVoiceNote = true
                                    recordingContactName = contactLog.contactName
                                    recordingContactId = contactLog.contactId
                                    recordingSeconds = 0

                                    try {
                                        com.example.util.AudioRecorderManager.startRecording(context)
                                    } catch (_: Exception) {}

                                    recJob = coroutineScope.launch {
                                        while (isRecordingVoiceNote) {
                                            delay(1000)
                                            recordingSeconds++
                                        }
                                    }

                                    val released = tryAwaitRelease()
                                    recJob?.cancel()
                                    recJob = null

                                    if (isRecordingVoiceNote) {
                                        isRecordingVoiceNote = false
                                        val (recFile, audioSecs) = try {
                                            com.example.util.AudioRecorderManager.stopRecording()
                                        } catch (_: Exception) {
                                            Pair(null, 0)
                                        }
                                        val totalSecs = if (audioSecs > 0) audioSecs else recordingSeconds

                                        if (released && totalSecs >= 1) {
                                            val mins = totalSecs / 60
                                            val secs = totalSecs % 60
                                            val durStr = String.format("%02d:%02d", mins, secs)
                                            val textToSend = if (recFile != null && recFile.exists() && recFile.length() > 0) {
                                                "[AUDIO_FILE|${recFile.absolutePath}|$durStr] 🎙️ Voice Note ($durStr)"
                                            } else {
                                                "🎙️ [Voice Note ($durStr)]"
                                            }
                                            viewModel.sendMessage(contactLog.contactId, textToSend)
                                            Toast.makeText(context, "Voice note sent to ${contactLog.contactName} 🎙️", Toast.LENGTH_SHORT).show()
                                            selectedContactForOptions = null
                                        } else {
                                            try { com.example.util.AudioRecorderManager.cancelRecording() } catch (_: Exception) {}
                                            Toast.makeText(context, "Hold button longer to record voice note ⏱️", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                        .padding(14.dp)
                ) {
                    if (isThisContactRecording) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val pulseTransition = rememberInfiniteTransition(label = "pulse_sheet")
                                    val pulseAlpha by pulseTransition.animateFloat(
                                        initialValue = 0.2f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(tween(400), repeatMode = RepeatMode.Reverse),
                                        label = "pulse"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444).copy(alpha = pulseAlpha))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Recording Voice Note...",
                                        color = Color(0xFFEF4444),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                val mins = recordingSeconds / 60
                                val secs = recordingSeconds % 60
                                Text(
                                    text = String.format("%02d:%02d", mins, secs),
                                    color = Color(0xFFEF4444),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Live Audio Waveform Animation inside Option 4
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val waveformTrans = rememberInfiniteTransition(label = "wave_sheet")
                                for (i in 0 until 14) {
                                    val barH by waveformTrans.animateFloat(
                                        initialValue = 6f + (i % 4) * 5f,
                                        targetValue = 24f - (i % 3) * 6f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(220 + i * 35, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "bar_$i"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .width(4.dp)
                                            .height(barH.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFFEF4444), Color(0xFF8B5CF6))
                                                )
                                            )
                                    )
                                }
                            }

                            Text(
                                text = "Release finger or tap to Stop & Send Voice Note",
                                color = animTextColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Leave a Voice Note", color = animTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Hold or tap to record & send automatically", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Delete Options Selection Dialog
    if (showDeleteOptionsDialog) {
        Dialog(onDismissRequest = { showDeleteOptionsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isNightMode) Color(0xFF1E1F2B) else Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Call Log Delete Options",
                            color = animTextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showDeleteOptionsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = animTextColor)
                        }
                    }

                    // Option 1: Delete All
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                            .clickable {
                                showDeleteOptionsDialog = false
                                showConfirmDeleteAllDialog = true
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Delete All Call Logs", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Clear entire call history with confirmation", color = animSubTextColor, fontSize = 12.sp)
                        }
                    }

                    // Option 2: Selective Delete
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2563EB).copy(alpha = 0.12f))
                            .clickable {
                                showDeleteOptionsDialog = false
                                isSelectiveDeleteMode = true
                                selectedLogIds = emptySet()
                                Toast.makeText(context, "Select call logs to delete", Toast.LENGTH_SHORT).show()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SelectAll, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Selective Delete", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Select specific call logs to remove manually", color = animSubTextColor, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Delete All
    if (showConfirmDeleteAllDialog) {
        Dialog(onDismissRequest = { showConfirmDeleteAllDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isNightMode) Color(0xFF1E1F2B) else Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFEF4444).copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
                    }
                    Text("Delete All Call Logs?", color = animTextColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Are you sure you want to permanently clear all call history? This action cannot be undone.",
                        color = animSubTextColor,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showConfirmDeleteAllDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isNightMode) Color(0xFF2A2D3D) else Color(0xFFE2E8F0), contentColor = animTextColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                showConfirmDeleteAllDialog = false
                                coroutineScope.launch {
                                    isDeleteProcessing = true
                                    delay(600)
                                    viewModel.clearCallLogs()
                                    isDeleteProcessing = false
                                    isDeleteDone = true
                                    Toast.makeText(context, "All call history cleared", Toast.LENGTH_SHORT).show()
                                    delay(1200)
                                    isDeleteDone = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Yes, Delete All", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Selective Delete
    if (showConfirmDeleteSelectedDialog) {
        Dialog(onDismissRequest = { showConfirmDeleteSelectedDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isNightMode) Color(0xFF1E1F2B) else Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFEF4444).copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
                    }
                    Text("Delete ${selectedLogIds.size} Call Log(s)?", color = animTextColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Are you sure you want to delete the ${selectedLogIds.size} selected call log item(s)?",
                        color = animSubTextColor,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showConfirmDeleteSelectedDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isNightMode) Color(0xFF2A2D3D) else Color(0xFFE2E8F0), contentColor = animTextColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                showConfirmDeleteSelectedDialog = false
                                coroutineScope.launch {
                                    isDeleteProcessing = true
                                    delay(600)
                                    viewModel.deleteCallLogsByIds(selectedLogIds)
                                    val count = selectedLogIds.size
                                    selectedLogIds = emptySet()
                                    isSelectiveDeleteMode = false
                                    isDeleteProcessing = false
                                    isDeleteDone = true
                                    Toast.makeText(context, "$count call log(s) deleted", Toast.LENGTH_SHORT).show()
                                    delay(1200)
                                    isDeleteDone = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete (${selectedLogIds.size})", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Start New Call & Call Link Screen
    if (showStartCallDialog) {
        val generatedCallLink = remember { "https://bitchat.app/call/join-" + java.util.UUID.randomUUID().toString().take(6) }
        var hostApprovalRequired by remember { mutableStateOf(true) }
        var newCallSearchQuery by remember { mutableStateOf("") }

        Dialog(
            onDismissRequest = { showStartCallDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                color = if (isNightMode) Color(0xFF12131A) else Color(0xFFF8FAFC)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showStartCallDialog = false },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isNightMode) Color(0xFF1E202C) else Color(0xFFE2E8F0))
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = animTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "New Call & Call Link",
                                    color = animTextColor,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Start encrypted call or share link",
                                    color = animSubTextColor,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Compact Call Link Card Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isNightMode) Color(0xFF1C1E2B) else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isNightMode) Color(0xFF2A2D3E) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Call Link", color = animTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text("WhatsApp style", color = animSubTextColor, fontSize = 10.sp)
                        }

                        // Link Display Box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isNightMode) Color(0xFF262838) else Color(0xFFF1F5F9))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = generatedCallLink,
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(generatedCallLink))
                                    Toast.makeText(context, "Call link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            }
                        }

                        // Host Approval Required Switch Option
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = animSubTextColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Call Approval Needed", color = animTextColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Switch(
                                checked = hostApprovalRequired,
                                onCheckedChange = { hostApprovalRequired = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB)),
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        // Action Buttons: Copy Link | Share Link to People | Send to All Chats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(generatedCallLink))
                                    Toast.makeText(context, "Call link copied!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB).copy(alpha = 0.12f), contentColor = Color(0xFF2563EB)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showShareLinkSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981).copy(alpha = 0.12f), contentColor = Color(0xFF10B981)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.3f).height(36.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showConfirmSendAllChatsDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.12f), contentColor = Color(0xFFEF4444)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.3f).height(36.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Send to All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Compact Search Bar for Chat Persons
                    OutlinedTextField(
                        value = newCallSearchQuery,
                        onValueChange = { newCallSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        placeholder = { Text("Search chat list by name or @username...", fontSize = 12.sp, color = animSubTextColor) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = animSubTextColor, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (newCallSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { newCallSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = animSubTextColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = if (isNightMode) Color(0xFF2A2D3D) else Color(0xFFE2E8F0),
                            focusedContainerColor = if (isNightMode) Color(0xFF1C1E2B) else Color.White,
                            unfocusedContainerColor = if (isNightMode) Color(0xFF1C1E2B) else Color.White
                        )
                    )

                    // Contacts List Header
                    val chatPeopleList = remember(allChats, contacts, newCallSearchQuery) {
                        val itemsList = if (allChats.isNotEmpty()) {
                            allChats.map { chat -> Triple(chat.id, chat.name, chat.id) }
                        } else if (contacts.isNotEmpty()) {
                            contacts.map { c -> Triple(c.id, c.name, c.id) }
                        } else {
                            listOf(
                                Triple("alex", "Alex Rivera", "alex_rivera"),
                                Triple("sarah", "Sarah Chen", "sarah_c"),
                                Triple("evelyn", "Evelyn Vance", "evelyn_vance")
                            )
                        }

                        if (newCallSearchQuery.isBlank()) itemsList
                        else itemsList.filter {
                            it.second.contains(newCallSearchQuery, ignoreCase = true) ||
                            it.third.contains(newCallSearchQuery, ignoreCase = true)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "People in Your Chat List",
                            color = animSubTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${chatPeopleList.size} People",
                            color = Color(0xFF2563EB),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Full Screen Scrollable Chat List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chatPeopleList, key = { it.first }, contentType = { "person" }) { (personId, personName, handle) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isNightMode) Color(0xFF1C1E2B) else Color.White)
                                    .border(
                                        width = 1.dp,
                                        color = if (isNightMode) Color(0xFF282B3D) else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF2563EB), Color(0xFF8B5CF6))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = personName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = personName,
                                            color = animTextColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (handle.startsWith("@")) handle else "@$handle",
                                            color = animSubTextColor,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            showStartCallDialog = false
                                            viewModel.startCall(personId, personName, "AUDIO")
                                            onStartAudioCallClick(personId)
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    ) {
                                        Icon(
                                            Icons.Default.Call,
                                            contentDescription = "Audio Call",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            showStartCallDialog = false
                                            viewModel.startCall(personId, personName, "VIDEO")
                                            onStartVideoCallClick(personId)
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2563EB))
                                    ) {
                                        Icon(
                                            Icons.Default.Videocam,
                                            contentDescription = "Video Call",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
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

    // Share Link to Selected People Sheet (Expanded Full Screen Modal)
    if (showShareLinkSheet) {
        val linkToShare = remember { "https://bitchat.app/call/join-" + java.util.UUID.randomUUID().toString().take(6) }
        var selectedShareChatIds by remember { mutableStateOf<Set<String>>(emptySet()) }
        var shareSearchQuery by remember { mutableStateOf("") }

        Dialog(
            onDismissRequest = { showShareLinkSheet = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                color = if (isNightMode) Color(0xFF12131A) else Color(0xFFF8FAFC)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Bar with Back Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showShareLinkSheet = false },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isNightMode) Color(0xFF1E202C) else Color(0xFFE2E8F0))
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = animTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Share Call Link",
                                    color = animTextColor,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Select people in your chat list to share",
                                    color = animSubTextColor,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Search Input Bar
                    OutlinedTextField(
                        value = shareSearchQuery,
                        onValueChange = { shareSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        placeholder = { Text("Search contact name...", fontSize = 12.sp, color = animSubTextColor) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = animSubTextColor, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (shareSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { shareSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = animSubTextColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = if (isNightMode) Color(0xFF2A2D3D) else Color(0xFFE2E8F0),
                            focusedContainerColor = if (isNightMode) Color(0xFF1C1E2B) else Color.White,
                            unfocusedContainerColor = if (isNightMode) Color(0xFF1C1E2B) else Color.White
                        )
                    )

                    val sharePeople = remember(allChats, shareSearchQuery) {
                        val baseList = if (allChats.isNotEmpty()) {
                            allChats.map { Pair(it.id, it.name) }
                        } else {
                            listOf(Pair("alex", "Alex Rivera"), Pair("sarah", "Sarah Chen"), Pair("evelyn", "Evelyn Vance"))
                        }
                        if (shareSearchQuery.isBlank()) baseList
                        else baseList.filter { it.second.contains(shareSearchQuery, ignoreCase = true) }
                    }

                    // Select All Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Contacts (${selectedShareChatIds.size} Selected)",
                            color = animTextColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedShareChatIds.size == sharePeople.size) "Deselect All" else "Select All",
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedShareChatIds = if (selectedShareChatIds.size == sharePeople.size) {
                                        emptySet()
                                    } else {
                                        sharePeople.map { it.first }.toSet()
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Full Height Scrollable List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sharePeople, key = { it.first }, contentType = { "share_person" }) { (id, name) ->
                            val isChecked = selectedShareChatIds.contains(id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isNightMode) Color(0xFF1C1E2B) else Color.White)
                                    .border(
                                        width = 1.dp,
                                        color = if (isNightMode) Color(0xFF282B3D) else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        selectedShareChatIds = if (isChecked) selectedShareChatIds - id else selectedShareChatIds + id
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2563EB).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(name.take(1).uppercase(), color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(name, color = animTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        selectedShareChatIds = if (isChecked) selectedShareChatIds - id else selectedShareChatIds + id
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2563EB))
                                )
                            }
                        }
                    }

                    // Bottom Action Button
                    Button(
                        onClick = {
                            if (selectedShareChatIds.isEmpty()) {
                                Toast.makeText(context, "Select at least one contact", Toast.LENGTH_SHORT).show()
                            } else {
                                selectedShareChatIds.forEach { chatId ->
                                    viewModel.sendMessage(chatId, "📞 BitChat Encrypted Call Link: $linkToShare")
                                }
                                Toast.makeText(context, "Call link sent to ${selectedShareChatIds.size} chat(s)!", Toast.LENGTH_SHORT).show()
                                showShareLinkSheet = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Link to ${selectedShareChatIds.size} Contact(s)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Send Call Link to All Chats
    if (showConfirmSendAllChatsDialog) {
        val linkToBroadcast = remember { "https://bitchat.app/call/join-" + java.util.UUID.randomUUID().toString().take(6) }
        Dialog(onDismissRequest = { showConfirmSendAllChatsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isNightMode) Color(0xFF1E1F2B) else Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFEF4444).copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
                    }
                    Text("Send Call Link to ALL Chats?", color = animTextColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Are you sure you want to send this call link to EVERY active chat in your chat list? This will broadcast the link to all contacts.",
                        color = animSubTextColor,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showConfirmSendAllChatsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isNightMode) Color(0xFF2A2D3D) else Color(0xFFE2E8F0), contentColor = animTextColor),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text("Cancel", maxLines = 1)
                        }
                        Button(
                            onClick = {
                                showConfirmSendAllChatsDialog = false
                                val targets = if (allChats.isNotEmpty()) allChats.map { it.id } else listOf("alex", "sarah", "evelyn")
                                targets.forEach { chatId ->
                                    viewModel.sendMessage(chatId, "📞 BitChat Encrypted Call Link: $linkToBroadcast")
                                }
                                Toast.makeText(context, "Call link sent to ALL ${targets.size} chats!", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text(
                                text = "Yes, Send to All",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                fontSize = 12.sp,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallCardItem(
    callLog: CallLog,
    isNightMode: Boolean,
    animTextColor: Color,
    animSubTextColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onQuickCallClick: () -> Unit,
    isSelectiveDeleteMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {}
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(callLog.id, isSelectiveDeleteMode) {
                detectTapGestures(
                    onTap = {
                        if (isSelectiveDeleteMode) onToggleSelect() else onClick()
                    },
                    onLongPress = {
                        if (!isSelectiveDeleteMode) onLongClick()
                    }
                )
            },
        cornerRadius = 20.dp,
        isNightMode = isNightMode
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectiveDeleteMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFEF4444)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // User Profile Placeholder Avatar on Side (Left)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF2563EB), Color(0xFF8B5CF6))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val avatarPath = callLog.avatarType
                val hasAvatar = avatarPath.isNotBlank() && !avatarPath.equals("default", ignoreCase = true)
                if (hasAvatar) {
                    val model: Any = if (
                        avatarPath.startsWith("http://") ||
                        avatarPath.startsWith("https://") ||
                        avatarPath.startsWith("content://")
                    ) {
                        avatarPath
                    } else {
                        val clean = avatarPath.removePrefix("file://")
                        val file = File(clean)
                        if (file.exists()) file else avatarPath
                    }
                    AsyncImage(
                        model = model,
                        contentDescription = "Contact avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = callLog.contactName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details Column - Middle aligned next to profile avatar
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = callLog.contactName,
                    color = animTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Direction Arrow Icon
                    val (dirIcon, dirTint) = when (callLog.direction) {
                        "INCOMING" -> Icons.AutoMirrored.Filled.CallReceived to Color(0xFF10B981)
                        "MISSED" -> Icons.AutoMirrored.Filled.CallMissed to Color(0xFFEF4444)
                        else -> Icons.AutoMirrored.Filled.CallMade to Color(0xFF2563EB)
                    }
                    Icon(
                        imageVector = dirIcon,
                        contentDescription = callLog.direction,
                        tint = dirTint,
                        modifier = Modifier.size(13.dp)
                    )

                    Spacer(modifier = Modifier.width(3.dp))

                    // Audio vs Video Icon
                    Icon(
                        imageVector = if (callLog.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = callLog.callType,
                        tint = animSubTextColor,
                        modifier = Modifier.size(13.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Call Details Text (small size text below)
                    val durationText = if (callLog.durationSeconds > 0) {
                        val m = callLog.durationSeconds / 60
                        val s = callLog.durationSeconds % 60
                        " • ${String.format("%02d:%02d", m, s)}"
                    } else if (callLog.direction == "MISSED") " • Missed" else ""

                    Text(
                        text = "${callLog.timeString}$durationText",
                        color = animSubTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Quick Redial Call Button on Right
            IconButton(
                onClick = onQuickCallClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (callLog.callType == "VIDEO") Color(0xFF2563EB).copy(alpha = 0.12f)
                        else Color(0xFF10B981).copy(alpha = 0.12f)
                    )
            ) {
                Icon(
                    imageVector = if (callLog.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = "Quick Call",
                    tint = if (callLog.callType == "VIDEO") Color(0xFF2563EB) else Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
