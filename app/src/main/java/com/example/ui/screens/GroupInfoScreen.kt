package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ChatEntity
import com.example.data.local.GroupMemberEntity
import com.example.ui.viewmodel.BitChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    chatId: String,
    viewModel: BitChatViewModel,
    onNavigateBack: () -> Unit,
    onLeaveGroup: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userIdentity by viewModel.userIdentity.collectAsState(initial = null)
    val currentUid = userIdentity?.supabaseUid?.ifBlank { userIdentity?.email } ?: "current_user"

    var chatEntity by remember { mutableStateOf<ChatEntity?>(null) }
    var members by remember { mutableStateOf<List<GroupMemberEntity>>(emptyList()) }

    // Dialog & Sheet States
    var showEditSettingsDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var selectedMemberForAction by remember { mutableStateOf<GroupMemberEntity?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportingTargetUid by remember { mutableStateOf<String?>(null) }
    var reportReason by remember { mutableStateOf("Spam") }
    var reportDetails by remember { mutableStateOf("") }

    // Settings Form State
    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var onlyAdminsCanMessage by remember { mutableStateOf(false) }
    var onlyAdminsCanEditInfo by remember { mutableStateOf(false) }
    var onlyAdminsCanPin by remember { mutableStateOf(false) }

    var newMemberUidInput by remember { mutableStateOf("") }

    LaunchedEffect(chatId) {
        val chat = viewModel.getChatById(chatId)
        chatEntity = chat
        editTitle = chat?.name ?: ""
        editDescription = chat?.description ?: ""
        onlyAdminsCanMessage = chat?.permissions?.contains("onlyAdminsCanMessage=true") == true
        onlyAdminsCanEditInfo = chat?.permissions?.contains("onlyAdminsCanEditInfo=true") == true
        onlyAdminsCanPin = chat?.permissions?.contains("onlyAdminsCanPin=true") == true

        viewModel.observeGroupMembers(chatId).collect { list ->
            members = list
        }
    }

    val currentMember = members.find { it.uid == currentUid }
    val isOwner = chatEntity?.ownerUid == currentUid || currentMember?.role == "OWNER"
    val isAdmin = isOwner || currentMember?.role == "ADMIN"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Info", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { showEditSettingsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Group Settings")
                        }
                    }
                }
            )
        }
    ) { paddingVals ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!chatEntity?.avatarType.isNullOrBlank() && chatEntity?.avatarType != "default" && chatEntity?.avatarType != "group_default") {
                    AsyncImage(
                        model = chatEntity?.avatarType,
                        contentDescription = "Group Avatar",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = "Group Avatar",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = chatEntity?.name ?: "Group Chat",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            if (!chatEntity?.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = chatEntity?.description ?: "",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${members.size} participants",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Notifications Mute Switch Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (chatEntity?.isMuted == true) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Mute Notifications", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(
                                if (chatEntity?.isMuted == true) "Muted for this group" else "Sound and alerts enabled",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = chatEntity?.isMuted == true,
                        onCheckedChange = { isMuted ->
                            viewModel.toggleMuteChat(chatId, chatEntity?.isMuted == true)
                            chatEntity = chatEntity?.copy(isMuted = isMuted)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Members (${members.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (isAdmin) {
                    TextButton(onClick = { showAddMemberDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Member")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(members) { member ->
                    val isCurrentItemUser = member.uid == currentUid
                    val isItemOwner = member.role == "OWNER"
                    val isItemAdmin = member.role == "ADMIN"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMemberForAction = member },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isItemOwner -> Color(0xFFF59E0B)
                                            isItemAdmin -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.secondary
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        isItemOwner -> Icons.Default.Shield
                                        isItemAdmin -> Icons.Default.Star
                                        else -> Icons.Default.Person
                                    },
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isCurrentItemUser) "${member.uid} (You)" else member.uid,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when {
                                            isItemOwner -> Color(0xFFFEF3C7)
                                            isItemAdmin -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    ) {
                                        Text(
                                            text = when {
                                                isItemOwner -> "👑 Owner"
                                                isItemAdmin -> "⭐ Admin"
                                                else -> "Member"
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isItemOwner -> Color(0xFFB45309)
                                                isItemAdmin -> MaterialTheme.colorScheme.onPrimaryContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }

                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Member Actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showLeaveConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exit Group")
            }
        }
    }

    // Member Actions Modal Bottom Sheet / Dialog
    selectedMemberForAction?.let { targetMember ->
        val isTargetSelf = targetMember.uid == currentUid
        val isTargetOwner = targetMember.role == "OWNER"
        val isTargetAdmin = targetMember.role == "ADMIN"

        AlertDialog(
            onDismissRequest = { selectedMemberForAction = null },
            title = { Text(text = "Member: ${targetMember.uid}", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isOwner && !isTargetSelf) {
                        if (!isTargetAdmin) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            viewModel.promoteAdmin(chatId, targetMember.uid)
                                            Toast.makeText(context, "Promoted to Admin", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                                        }
                                        selectedMemberForAction = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Promote to Admin")
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            viewModel.demoteAdmin(chatId, targetMember.uid)
                                            Toast.makeText(context, "Demoted to Member", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                                        }
                                        selectedMemberForAction = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Demote to Member")
                            }
                        }

                        TextButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        viewModel.transferOwnership(chatId, targetMember.uid)
                                        Toast.makeText(context, "Ownership transferred", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                                    }
                                    selectedMemberForAction = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFF59E0B))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Transfer Ownership", color = Color(0xFFF59E0B))
                        }
                    }

                    if ((isOwner || (isAdmin && !isTargetAdmin && !isTargetOwner)) && !isTargetSelf) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        viewModel.removeMemberFromGroupChat(chatId, targetMember.uid)
                                        Toast.makeText(context, "Removed member", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                                    }
                                    selectedMemberForAction = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remove from Group", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    if (!isTargetSelf) {
                        TextButton(
                            onClick = {
                                reportingTargetUid = targetMember.uid
                                showReportDialog = true
                                selectedMemberForAction = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Report, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Report User", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMemberForAction = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Add Member Dialog
    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Add Group Member", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter the user ID or username to add to this group:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newMemberUidInput,
                        onValueChange = { newMemberUidInput = it },
                        label = { Text("User ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uidToAdd = newMemberUidInput.trim()
                        if (uidToAdd.isNotBlank()) {
                            scope.launch {
                                try {
                                    viewModel.addMemberToGroupChat(chatId, uidToAdd)
                                    Toast.makeText(context, "Added $uidToAdd", Toast.LENGTH_SHORT).show()
                                    showAddMemberDialog = false
                                    newMemberUidInput = ""
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.localizedMessage ?: "Failed to add member", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Group Settings & Permissions Dialog
    if (showEditSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showEditSettingsDialog = false },
            title = { Text("Group Settings & Permissions", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Group Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Permissions", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Only Admins Send Messages", fontSize = 13.sp)
                        Switch(
                            checked = onlyAdminsCanMessage,
                            onCheckedChange = { onlyAdminsCanMessage = it }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Only Admins Edit Group Info", fontSize = 13.sp)
                        Switch(
                            checked = onlyAdminsCanEditInfo,
                            onCheckedChange = { onlyAdminsCanEditInfo = it }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Only Admins Pin Messages", fontSize = 13.sp)
                        Switch(
                            checked = onlyAdminsCanPin,
                            onCheckedChange = { onlyAdminsCanPin = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val permissionsStr = "onlyAdminsCanMessage=$onlyAdminsCanMessage,onlyAdminsCanEditInfo=$onlyAdminsCanEditInfo,onlyAdminsCanPin=$onlyAdminsCanPin"
                        scope.launch {
                            try {
                                viewModel.updateGroupDetails(
                                    chatId = chatId,
                                    name = editTitle.ifBlank { "Group Chat" },
                                    description = editDescription,
                                    avatarUrl = chatEntity?.avatarType,
                                    permissionsJson = permissionsStr
                                )
                                chatEntity = chatEntity?.copy(
                                    name = editTitle.ifBlank { "Group Chat" },
                                    description = editDescription,
                                    permissions = permissionsStr
                                )
                                Toast.makeText(context, "Settings updated", Toast.LENGTH_SHORT).show()
                                showEditSettingsDialog = false
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Leave Confirm Dialog
    if (showLeaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = false },
            title = { Text("Exit Group", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isOwner && members.size > 1) {
                        "You are the group owner. Exiting will automatically transfer group ownership to another active participant."
                    } else {
                        "Are you sure you want to exit this group? You won't receive new messages."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirmDialog = false
                        scope.launch {
                            try {
                                viewModel.leaveGroup(chatId)
                                Toast.makeText(context, "You left the group", Toast.LENGTH_SHORT).show()
                                onLeaveGroup()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to leave group: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Report Dialog
    if (showReportDialog) {
        val reasons = listOf("Spam", "Harassment", "Inappropriate Content", "Other")
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Abuse", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select a reason for reporting ${reportingTargetUid ?: "this group"}:")
                    Spacer(modifier = Modifier.height(8.dp))
                    reasons.forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportReason = r }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = reportReason == r,
                                onClick = { reportReason = r }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(r)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { reportDetails = it },
                        label = { Text("Additional details (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                viewModel.reportMemberOrGroup(
                                    chatId = chatId,
                                    targetUid = reportingTargetUid,
                                    reason = reportReason,
                                    details = reportDetails
                                )
                                Toast.makeText(context, "Report submitted. Thank you.", Toast.LENGTH_SHORT).show()
                                showReportDialog = false
                                reportDetails = ""
                            } catch (e: Exception) {
                                Toast.makeText(context, "Report failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
