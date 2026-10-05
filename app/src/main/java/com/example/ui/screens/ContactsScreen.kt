package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ContactEntity
import com.example.ui.components.BitChatBottomNavBar
import com.example.ui.components.BitChatNavTab
import com.example.ui.components.GlassPanel
import com.example.ui.theme.BitBackground
import com.example.ui.theme.BitOnPrimary
import com.example.ui.theme.BitOnSurface
import com.example.ui.theme.BitOnSurfaceVariant
import com.example.ui.theme.BitPrimary
import com.example.ui.theme.BitSecondary
import com.example.ui.theme.BitSurfaceContainer
import com.example.ui.viewmodel.BitChatViewModel

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween

@Composable
fun ContactsScreen(
    viewModel: BitChatViewModel,
    onContactClick: (ContactEntity) -> Unit,
    onVideoCallClick: (String) -> Unit,
    onTabSelected: (BitChatNavTab) -> Unit,
    onActiveCallBannerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.filteredContacts.collectAsState()
    val userPresenceMap by viewModel.userPresenceMap.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val isNightMode by viewModel.isNightMode.collectAsState()

    val animBgColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFF0D0E12) else Color(0xFFF1F5F9),
        animationSpec = tween(400),
        label = "contacts_bg"
    )

    val animTextColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFFF3F4F6) else Color(0xFF0F172A),
        animationSpec = tween(400),
        label = "contacts_text"
    )

    val animSubTextColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
        animationSpec = tween(400),
        label = "contacts_subtext"
    )

    val favorites = contacts.filter { it.isFavorite }
    val groupedContacts = contacts.groupBy { it.categoryLetter }

    Scaffold(
        bottomBar = {
            BitChatBottomNavBar(
                currentTab = BitChatNavTab.CHATS,
                onTabSelected = onTabSelected,
                isNightMode = isNightMode
            )
        },
        containerColor = animBgColor,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isNightMode) BitSurfaceContainer else Color.White)
                            .border(1.dp, if (isNightMode) Color.White.copy(0.1f) else Color(0xFFCBD5E1), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Avatar",
                            tint = Color(0xFF2563EB)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Contacts",
                        color = animTextColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = { /* Search */ }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = animTextColor
                    )
                }
            }

            com.example.ui.components.ActiveCallBulletinSlot(
                viewModel = viewModel,
                onExpandClick = onActiveCallBannerClick
            )

            // Search Bar
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                cornerRadius = 16.dp,
                isNightMode = isNightMode
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (isNightMode) Color(0xFF9EA3B0) else Color(0xFF2563EB),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "@username",
                                color = animSubTextColor.copy(alpha = 0.6f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                color = if (isNightMode) Color.White else Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(Color(0xFF2563EB)),
                            singleLine = true
                        )
                    }
                }
            }

            // Quick Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* New Contact */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BitPrimary,
                        contentColor = BitOnPrimary
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "New Contact", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                GlassPanel(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    cornerRadius = 24.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = BitOnSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Invite Friends", color = BitOnSurface, fontSize = 14.sp)
                    }
                }
            }

            // Favorites Row
            if (favorites.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Favorites",
                            color = BitOnSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "SEE ALL",
                            color = BitSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(favorites) { contact ->
                            val clean = contact.name.trim().lowercase().removePrefix("@").removeSuffix(".link")
                            val isContactOnline = (userPresenceMap[contact.id]?.first ?: false) ||
                                (userPresenceMap[contact.name]?.first ?: false) ||
                                (userPresenceMap[contact.name.lowercase()]?.first ?: false) ||
                                (userPresenceMap[clean]?.first ?: false) ||
                                (userPresenceMap["$clean.link"]?.first ?: false)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onContactClick(contact) }
                            ) {
                                Box(modifier = Modifier.size(56.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, BitSecondary.copy(alpha = 0.6f), CircleShape)
                                            .background(BitSurfaceContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = contact.name.take(1),
                                            color = BitPrimary,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (isContactOnline) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .align(Alignment.BottomEnd)
                                                .clip(CircleShape)
                                                .background(BitSecondary)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = contact.name,
                                    color = animTextColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Alphabetical Contact List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                groupedContacts.forEach { (letter, groupContacts) ->
                    item {
                        Text(
                            text = letter,
                            color = Color(0xFF2563EB),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                    items(groupContacts, key = { it.id }, contentType = { "contact" }) { contact ->
                        val clean = contact.name.trim().lowercase().removePrefix("@").removeSuffix(".link")
                        val isContactOnline = (userPresenceMap[contact.id]?.first ?: false) ||
                            (userPresenceMap[contact.name]?.first ?: false) ||
                            (userPresenceMap[contact.name.lowercase()]?.first ?: false) ||
                            (userPresenceMap[clean]?.first ?: false) ||
                            (userPresenceMap["$clean.link"]?.first ?: false)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (contact.id == "alex") {
                                        onVideoCallClick(contact.id)
                                    } else {
                                        onContactClick(contact)
                                    }
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(44.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (isNightMode) BitSurfaceContainer else Color.White)
                                        .border(1.dp, if (isNightMode) Color.White.copy(0.1f) else Color(0xFFCBD5E1), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.take(1),
                                        color = Color(0xFF2563EB),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (isContactOnline) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = contact.name,
                                    color = animTextColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = contact.statusText,
                                    color = animSubTextColor,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
