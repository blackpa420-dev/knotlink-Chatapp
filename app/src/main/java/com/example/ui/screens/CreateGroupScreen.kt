package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.viewmodel.BitChatViewModel
import kotlinx.coroutines.launch
import java.io.File
import com.example.ui.components.GlassPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    viewModel: BitChatViewModel,
    onNavigateBack: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contacts by viewModel.contacts.collectAsState()
    val isNightMode by viewModel.isNightMode.collectAsState()

    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedUids by remember { mutableStateOf(setOf<String>()) }
    var isCreating by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            avatarUri = uri
        }
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.statusText.contains(searchQuery, ignoreCase = true)
        }
    }

    val selectedContactsList = remember(contacts, selectedUids) {
        contacts.filter { selectedUids.contains(it.id) }
    }

    val isFormValid = groupName.isNotBlank() && selectedUids.isNotEmpty()

    val screenBg = if (isNightMode) Color(0xFF07080B) else Color(0xFFF8FAFC)
    val cardBg = if (isNightMode) Color(0xFF0F1117) else Color.White
    val borderCol = if (isNightMode) Color(0xFF1E212E) else Color(0xFFE2E8F0)
    val textCol = if (isNightMode) Color(0xFFF4F4F6) else Color(0xFF0F172A)
    val subTextCol = if (isNightMode) Color(0xFF9EA3B0) else Color(0xFF64748B)
    val brandBlue = Color(0xFF2563EB)
    val brandBlueGradient = Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = screenBg,
                border = BorderStroke(width = 1.dp, color = borderCol.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textCol,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Create Group Chat",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = textCol,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Select members & customize details",
                            fontSize = 11.5.sp,
                            color = subTextCol,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            if (groupName.isBlank()) {
                                Toast.makeText(context, "Please enter a group name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedUids.isEmpty()) {
                                Toast.makeText(context, "Please select at least one member", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isCreating = true
                            scope.launch {
                                try {
                                    var avatarUrl = ""
                                    if (avatarUri != null) {
                                        Toast.makeText(context, "Uploading group avatar...", Toast.LENGTH_SHORT).show()
                                        val tempChatId = "group_avatar_${System.currentTimeMillis()}"
                                        avatarUrl = viewModel.uploadMedia(tempChatId, avatarUri!!, "image/jpeg", context)
                                    }
                                    val memberList = selectedUids.toList()
                                    val chatId = viewModel.createGroupChat(
                                        title = groupName,
                                        description = groupDescription.ifBlank { null },
                                        avatarUrl = avatarUrl.ifBlank { null },
                                        memberUids = memberList
                                    )
                                    Toast.makeText(context, "Group created successfully!", Toast.LENGTH_SHORT).show()
                                    onGroupCreated(chatId)
                                } catch (e: Exception) {
                                    isCreating = false
                                    Toast.makeText(context, "Failed to create group: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isCreating && isFormValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brandBlue,
                            contentColor = Color.White,
                            disabledContainerColor = if (isNightMode) Color(0xFF1C1D24) else Color(0xFFE2E8F0),
                            disabledContentColor = subTextCol
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.5.dp, color = Color.White)
                        } else {
                            Text("Create", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        containerColor = screenBg
    ) { paddingVals ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card: Photo & Name Input wrapped in custom Flat Styled surface
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderCol)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Circular Group Avatar Selection Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                if (avatarUri != null) Color.Transparent
                                else brandBlue.copy(alpha = 0.12f)
                            )
                            .border(1.5.dp, brandBlue.copy(alpha = 0.6f), CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUri != null) {
                            AsyncImage(
                                model = avatarUri,
                                contentDescription = "Group Avatar",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Group Avatar",
                                modifier = Modifier.size(34.dp),
                                tint = brandBlue
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Avatar",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(bottom = 6.dp)
                                    .size(16.dp)
                            )
                        }
                    }

                    // Group Name Text Field Input
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GROUP NAME",
                            color = brandBlue,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { if (it.length <= 60) groupName = it },
                            placeholder = { Text("Set group title...", fontSize = 13.5.sp, color = subTextCol) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            textStyle = TextStyle(
                                color = textCol,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = brandBlue,
                                unfocusedBorderColor = borderCol,
                                focusedContainerColor = if (isNightMode) Color(0xFF040507) else Color(0xFFF1F5F9),
                                unfocusedContainerColor = if (isNightMode) Color(0xFF040507) else Color(0xFFF1F5F9),
                                focusedTextColor = textCol,
                                unfocusedTextColor = textCol
                            )
                        )
                    }
                }
            }

            // Description input field
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderCol)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "DESCRIPTION / BIO",
                        color = subTextCol,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = groupDescription,
                        onValueChange = { if (it.length <= 160) groupDescription = it },
                        placeholder = { Text("What is this group about? (Optional)", fontSize = 13.sp, color = subTextCol) },
                        maxLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        textStyle = TextStyle(
                            color = textCol,
                            fontSize = 13.5.sp
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandBlue,
                            unfocusedBorderColor = borderCol,
                            focusedContainerColor = if (isNightMode) Color(0xFF040507) else Color(0xFFF1F5F9),
                            unfocusedContainerColor = if (isNightMode) Color(0xFF040507) else Color(0xFFF1F5F9),
                            focusedTextColor = textCol,
                            unfocusedTextColor = textCol
                        )
                    )
                }
            }

            // Horizontally Scrollable Selected Members Chips Section
            AnimatedVisibility(
                visible = selectedContactsList.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SELECTED MEMBERS (${selectedUids.size})",
                        color = brandBlue,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(selectedContactsList, key = { "sel_${it.id}" }) { contact ->
                            val hasAvatar = !contact.avatarType.isNullOrBlank() && (
                                contact.avatarType.startsWith("http") ||
                                contact.avatarType.startsWith("content") ||
                                contact.avatarType.startsWith("file") ||
                                contact.avatarType.startsWith("/")
                            )

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isNightMode) Color(0xFF1E212E) else Color(0xFFE2E8F0),
                                border = BorderStroke(1.dp, brandBlue.copy(0.4f)),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Mini Circle Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (hasAvatar) Color.Transparent
                                                else brandBlue
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (hasAvatar) {
                                            AsyncImage(
                                                model = contact.avatarType,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = contact.name.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = contact.name,
                                        color = textCol,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove member",
                                        tint = subTextCol,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                selectedUids = selectedUids - contact.id
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar for Contacts Styled Premium exactly like the others
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                placeholder = { Text("Search contact list...", fontSize = 13.sp, color = subTextCol) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = subTextCol, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = subTextCol, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = textCol,
                    fontSize = 13.5.sp
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = brandBlue,
                    unfocusedBorderColor = borderCol,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedTextColor = textCol,
                    unfocusedTextColor = textCol
                )
            )

            // Contacts List Header
            Text(
                text = "AVAILABLE CONTACTS (${filteredContacts.size})",
                color = subTextCol,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            // Contacts Scrollable List Redesigned
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredContacts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No contacts found",
                                color = subTextCol,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    items(filteredContacts, key = { it.id }) { contact ->
                        val isSelected = selectedUids.contains(contact.id)
                        val hasAvatar = !contact.avatarType.isNullOrBlank() && (
                            contact.avatarType.startsWith("http") ||
                            contact.avatarType.startsWith("content") ||
                            contact.avatarType.startsWith("file") ||
                            contact.avatarType.startsWith("/")
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedUids = if (isSelected) selectedUids - contact.id else selectedUids + contact.id
                                },
                            color = if (isSelected) brandBlue.copy(alpha = 0.08f) else cardBg,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) brandBlue.copy(alpha = 0.5f) else borderCol
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
                                    // Circular Image/Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .then(
                                                if (hasAvatar) Modifier.background(Color.Transparent)
                                                else Modifier.background(brandBlueGradient)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (hasAvatar) {
                                            AsyncImage(
                                                model = contact.avatarType,
                                                contentDescription = "Avatar",
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = contact.name.take(1).uppercase(),
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
                                            text = contact.name,
                                            color = textCol,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.15).sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = contact.statusText.ifBlank { "Verified KnotLink User" },
                                            color = subTextCol,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedUids = if (checked) selectedUids + contact.id else selectedUids - contact.id
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = brandBlue,
                                        uncheckedColor = borderCol,
                                        checkmarkColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
