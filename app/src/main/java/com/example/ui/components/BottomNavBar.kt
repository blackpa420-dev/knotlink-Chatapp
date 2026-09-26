package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class BitChatNavTab(val route: String, val title: String) {
    CHATS("chats", "Chats"),
    CALLS("calls", "Calls"),
    QR_SCAN("qr_scanner", "QR Scan"),
    SETTINGS("settings", "Settings")
}

@Composable
fun BitChatBottomNavBar(
    currentTab: BitChatNavTab,
    onTabSelected: (BitChatNavTab) -> Unit,
    modifier: Modifier = Modifier,
    isNightMode: Boolean = true,
    unreadChatsCount: Int = 0
) {
    val haptic = LocalHapticFeedback.current

    // Dynamic container and chip styling for Night and Day modes
    val containerBg = if (isNightMode) Color(0xFF0F0F12) else Color(0xFFFFFFFF)
    val containerBorder = if (isNightMode) Color(0xFF26262E) else Color(0xFFE2E8F0)

    val activeChipBg = if (isNightMode) Color(0xFF24242C) else Color(0xFF2563EB)
    val activeIconBadgeBg = if (isNightMode) Color.White else Color.White
    val activeIconBadgeTint = if (isNightMode) Color(0xFF0F0F12) else Color(0xFF2563EB)
    val activeTextColor = Color.White

    val inactiveIconTint = if (isNightMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Floating Capsule Container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = if (isNightMode) 12.dp else 8.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = if (isNightMode) Color.Black else Color(0x1F000000),
                    spotColor = if (isNightMode) Color.Black else Color(0x1F000000)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(containerBg)
                .border(
                    width = 1.dp,
                    color = containerBorder,
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BitChatNavTab.entries.forEach { tab ->
                val isSelected = tab == currentTab

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val pressScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "tab_press_${tab.name}"
                )

                val activeIcon = when (tab) {
                    BitChatNavTab.CHATS -> Icons.AutoMirrored.Rounded.Chat
                    BitChatNavTab.CALLS -> Icons.Rounded.PhoneInTalk
                    BitChatNavTab.QR_SCAN -> Icons.Rounded.QrCodeScanner
                    BitChatNavTab.SETTINGS -> Icons.Rounded.Settings
                }

                val inactiveIcon = when (tab) {
                    BitChatNavTab.CHATS -> Icons.AutoMirrored.Outlined.Chat
                    BitChatNavTab.CALLS -> Icons.Outlined.PhoneInTalk
                    BitChatNavTab.QR_SCAN -> Icons.Outlined.QrCodeScanner
                    BitChatNavTab.SETTINGS -> Icons.Outlined.Settings
                }

                if (isSelected) {
                    // Active Expanded Pill Chip: [ (Icon / Unread Red Badge) Label ]
                    Row(
                        modifier = Modifier
                            .scale(pressScale)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(activeChipBg)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(tab)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (tab == BitChatNavTab.CHATS && unreadChatsCount > 0) {
                            // Modern Vibrant Red Badge displaying unread number inside icon spot
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadChatsCount > 99) "99+" else unreadChatsCount.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // Standard White Icon Circle Badge
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(activeIconBadgeBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = activeIcon,
                                    contentDescription = tab.title,
                                    tint = activeIconBadgeTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Text Label
                        Text(
                            text = tab.title,
                            color = activeTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.width(4.dp))
                    }
                } else {
                    // Inactive Icon Only or Unread Red Badge Container
                    Box(
                        modifier = Modifier
                            .scale(pressScale)
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(tab)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (tab == BitChatNavTab.CHATS && unreadChatsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadChatsCount > 99) "99+" else unreadChatsCount.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Icon(
                                imageVector = inactiveIcon,
                                contentDescription = tab.title,
                                tint = inactiveIconTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
